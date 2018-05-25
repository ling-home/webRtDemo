package ccsskt.bokecc.base.example.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.bokecc.sskt.base.CCAtlasCallBack;
import com.bokecc.sskt.base.CCAtlasClient;
import com.bokecc.sskt.base.CCBaseBean;
import com.bokecc.sskt.base.CCStream;
import com.bokecc.sskt.base.ConnectionStatsWrapper;
import com.bokecc.sskt.base.LocalStreamConfig;
import com.bokecc.sskt.base.exception.StreamException;
import com.bokecc.sskt.base.renderer.CCSurfaceRenderer;
import com.intel.webrtc.base.LocalCameraStreamParameters;
import com.intel.webrtc.base.LocalCustomizedStream;
import com.intel.webrtc.conference.User;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.RendererCommon;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import butterknife.BindView;
import butterknife.OnClick;
import ccsskt.bokecc.base.example.CCApplication;
import ccsskt.bokecc.base.example.Config;
import ccsskt.bokecc.base.example.R;
import ccsskt.bokecc.base.example.adapter.VideoAdapter;
import ccsskt.bokecc.base.example.base.BaseActivity;
import ccsskt.bokecc.base.example.fragment.BaseFragment;
import ccsskt.bokecc.base.example.fragment.MainVideoFragment;
import ccsskt.bokecc.base.example.recycle.RecycleViewDivider;
import ccsskt.bokecc.base.example.util.DensityUtil;
import ccsskt.bokecc.base.example.view.VideoStreamView;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;
/**
 * 作者 ${王德惠}.<br/>
 */
@RuntimePermissions
public class MainActivity extends BaseActivity implements DrawerLayout.DrawerListener{

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String KEY_SESSION_ID = "session_id";
    private static final String KEY_USER_ACCOUNT = "user_account";
    private static final String KEY_ROOM_ID = "room_id";
    @BindView(R.id.id_main_video_container)
    RelativeLayout mSurfaceContainer;
    @BindView(R.id.id_teacher_start)
    ImageButton mStartClss;
    @BindView(R.id.id_teacher_stop)
    ImageButton mStopClass;

    private CCSurfaceRenderer mLocalRenderer;
    private CCStream mLocalStream, mStream;
    private CopyOnWriteArrayList<VideoStreamView> mVideoStreamViews = new CopyOnWriteArrayList<>();

    private CCAtlasClient mAtlasClient;
    private boolean isFront;
    private ArrayList<BaseFragment> mFragments;

    private BaseFragment mCurFragment;
    private VideoAdapter mVideoAdapter;
    private Handler handler;

    private CCAtlasClient.AtlasClientObserver mClientObserver = new CCAtlasClient.AtlasClientObserver() {
        @Override
        public void onServerDisconnected() {
            android.os.Process.killProcess(android.os.Process.myPid());
        }

        @Override
        public void onStreamAdded(CCStream stream) {
            if (stream.isRemoteIsLocal()) { // 不订阅自己的本地流
                return;
            }

            Log.e(TAG, "onStreamAdded: [ " + stream.getStreamId() + " ]");
            if (stream.getStreamType() != CCStream.REMOTE_MIX && stream.getStreamType() != CCStream.REMOTE_SCREEN &&
                    stream.getStreamType() != CCStream.LOCAL && stream.getStreamType() != CCStream.REMOTE_CUSTOMIZED) {
                // 订阅
                mStream = stream;
                mSubableRemoteStreams.add(mStream);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        addVideoView(mStream);
                    }
                });
            }
//            Timer timer = new Timer(true);

//            //任务
//            TimerTask task = new TimerTask() {
//                public void run() {
//                    Message msg = new Message();
//                    msg.what = 1;
//                    handler.sendMessage(msg);
//                }
//            };
//
//            timer.schedule(task, 0, 5000);
        }

        @Override
        public void onStreamRemoved(final CCStream stream) {
            Log.e(TAG, "onStreamRemoved: [ " + stream.getStreamId() + " ]");
            if (stream.getStreamType() != CCStream.REMOTE_MIX && stream.getStreamType() != CCStream.REMOTE_SCREEN &&
                    stream.getStreamType() != CCStream.LOCAL && stream.getStreamType() != CCStream.REMOTE_CUSTOMIZED) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        removeStreamView(stream);
                    }
                }, 2000);
            }

        }

        @Override
        public void onStreamError(String streamid, String errorMsg) {

        }
    };

    @Override
    protected int getLayoutId() {
        return R.layout.activity_teacher;
    }

    @Override
    protected void beforeSetContentView() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onViewCreated() {

        mAtlasClient = new CCAtlasClient(this,Config.PUBLIC_KEY);
        mAtlasClient.addAtlasObserver(mClientObserver);

        handler = new Handler();
        createLocalStream();

        mVideoAdapter = new VideoAdapter(this);
        mFragments = new ArrayList<>();
        mFragments.add(MainVideoFragment.newInstance(CCApplication.PRESENTER));
        for (BaseFragment fragment :
                mFragments) {
            fragment.setVideoAdapter(mVideoAdapter);
        }
        mCurFragment = mFragments.get(0);
        mCurFragment.addDatas(mVideoStreamViews);
        getSupportFragmentManager().beginTransaction().
                replace(R.id.id_teacher_content, mCurFragment)
                .commitAllowingStateLoss();
        initRenderer();
        MainActivityPermissionsDispatcher.localStreamWithCheck(this);

    }

    @NeedsPermission({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void localStream() {
        if (mLocalStream != null) {
            try {
                mLocalStream.attach(mLocalRenderer);
            } catch (StreamException ignored) {
            }
        }
    }

    private void initRenderer() {
        mLocalRenderer = new CCSurfaceRenderer(this);
        mLocalRenderer.init(mAtlasClient.getEglBase().getEglBaseContext(), null);
        mLocalRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        RelativeLayout.LayoutParams params;
        params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        mLocalRenderer.setLayoutParams(params);
        mSurfaceContainer.addView(mLocalRenderer);
    }

    private void createLocalStream() {
        LocalStreamConfig config = new LocalStreamConfig.LocalStreamConfigBuilder().build();
        isFront = config.cameraType == LocalStreamConfig.CAMERA_FRONT;
        try {
            mLocalStream = mAtlasClient.createLocalStream(config);
        } catch (StreamException e) {
            Log.e("111", e.getLocalizedMessage());
            showToast(e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        if (mLocalRenderer != null) {
            mLocalRenderer.cleanFrame();
            mLocalRenderer.release();
        }
        if (mStream != null) {
            mStream.detach();
            mStream = null;
        }
        if (mLocalStream != null) {
            mLocalStream.detach();
            mAtlasClient.destoryLocalStream();
        }
        mAtlasClient = null;
        super.onDestroy();

    }





    @Override
    public void onBackPressed() {
        showProgress();
        if(StopStatus) {
            finish();
        } else {
            mAtlasClient.stopLive(new CCAtlasCallBack<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    dismissProgress();
                    showToast("leave room success");
                    if (mVideoStreamViews.size() != 0) {
                        CopyOnWriteArrayList<CCStream> streams = getSubscribeRemoteStreams();
                        for (CCStream stream :
                                streams) {
                            removeStreamView(stream);
                        }
                    }
                    finish();
                }

                @Override
                public void onFailure(int errCode, String errMsg) {
                    dismissProgress();
                    showToast(errMsg);
                }
            });
        }
    }

    private synchronized void addVideoView(CCStream mStream) {
        if (mStream.getStreamType() != CCStream.REMOTE_MIX && mStream.getStreamType() != CCStream.REMOTE_SCREEN &&
                mStream.getStreamType() != CCStream.LOCAL && mStream != null && mStream.getStreamType() != CCStream.REMOTE_CUSTOMIZED) {
//            showProgress();
            final CCSurfaceRenderer mRemoteMixRenderer = new CCSurfaceRenderer(this);
            mRemoteMixRenderer.init(mAtlasClient.getEglBase().getEglBaseContext(), null);
            mRemoteMixRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
            final VideoStreamView videoRemoteStreamView = new VideoStreamView();
            videoRemoteStreamView.setRenderer(mRemoteMixRenderer);
            videoRemoteStreamView.setStream(mStream);
            videoRemoteStreamView.setUserId(mStream.getUserid());
            mAtlasClient.subscribe(mStream, new CCAtlasCallBack<CCStream>() {
                @Override
                public void onSuccess(final CCStream ccStream) {
//                    dismissProgress();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                videoRemoteStreamView.getStream().attach(videoRemoteStreamView.getRenderer());
                                int position;
                                position = mVideoStreamViews.size();
                                mVideoStreamViews.add(position, videoRemoteStreamView);
                                mCurFragment.notifyItemChanged(videoRemoteStreamView, mVideoStreamViews.size() - 1, true);
                            } catch (StreamException e) {
                                showToast(e.getMessage());
                            }
                        }
                    });
                }

                @Override
                public void onFailure(int errCode, String errMsg) {
//                    dismissProgress();
                }
            });
        }
    }

    private synchronized void removeStreamView(CCStream stream) {
        int position = -1;
        VideoStreamView tempView = null;
        for (int i = 0; i < mVideoStreamViews.size(); i++) {
            VideoStreamView streamView = mVideoStreamViews.get(i);
            if (streamView.getStream().getStreamId().equals(stream.getStreamId())) {
                tempView = streamView;
                position = i;
                break;
            }
        }
        if (tempView == null)
            return;
//        mStreamViewMap.remove(stream);
        mVideoStreamViews.remove(tempView);
        mCurFragment.notifyItemChanged(tempView, position, false);
        mCurFragment.notifySelfRemove(tempView);
        stream.detach();
        mAtlasClient.unsubcribe(stream, null);
    }
    @OnClick(R.id.id_list_back)
    void onLeftClick() {
        showProgress();
        if(StopStatus) {
            finish();
        } else {
            mAtlasClient.stopLive(new CCAtlasCallBack<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    dismissProgress();
                    showToast("leave room success");
                    if (mVideoStreamViews.size() != 0) {
                        CopyOnWriteArrayList<CCStream> streams = getSubscribeRemoteStreams();
                        for (CCStream stream :
                                streams) {
                            removeStreamView(stream);
                        }
                    }
                    finish();
                }

                @Override
                public void onFailure(int errCode, String errMsg) {
                    dismissProgress();
                    showToast(errMsg);
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // NOTE: delegate the permission handling to generated method
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @NeedsPermission({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void showRationale(PermissionRequest request) {
        // NOTE: Show a rationale to explain why the permission is needed, e.g. with a dialog.
        // Call proceed() or cancel() on the provided PermissionRequest to continue or abort
        showRationaleDialog(request);
    }

    @OnNeverAskAgain({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void onNeverAskAgain() {
        Toast.makeText(this, "相机或录音权限被拒绝，并且不会再次询问", Toast.LENGTH_SHORT).show();
    }

    private void showRationaleDialog(final PermissionRequest request) {
        new AlertDialog.Builder(this)
                .setPositiveButton("允许", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(@NonNull DialogInterface dialog, int which) {
                        request.proceed();
                    }
                })
                .setNegativeButton("禁止", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(@NonNull DialogInterface dialog, int which) {
                        request.cancel();
                    }
                })
                .setCancelable(false)
                .setMessage("当前应用需要开启相机和录音进行推流")
                .show();
    }
//    private Handler handler  = new Handler(){
//        public void handleMessage(Message msg) {
//            super.handleMessage(msg);
//            if(msg.what == 1){
//                mAtlasClient.getConnectionStats(mStream, new CCAtlasCallBack<ConnectionStatsWrapper>() {
//                    @Override
//                    public void onSuccess(ConnectionStatsWrapper StatsWrapper) {
//                        Log.i(TAG, "wdh---->---<onSuccess: " + StatsWrapper.bytesReceived + " 1 " + StatsWrapper.packetsReceived
//                                + " 2 " + StatsWrapper.frameHeightReceived + " 3 " + StatsWrapper.frameWidthReceived + " 4 " +StatsWrapper.frameRateReceived
//                                + " 5 " + StatsWrapper.firsSent + " 6 " + StatsWrapper.frameRateOutput + " 7 " + StatsWrapper.plisSent);
//                        if(StatsWrapper.frameRateReceived == 0&&StatsWrapper.frameRateOutput == 0){
//                            removeStreamView(mStream);
//                        }
//                    }
//
//                    @Override
//                    public void onFailure(int errCode, String errMsg) {
//
//                    }
//                });
//            }
//        }
//    };




    @NeedsPermission({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    @OnClick(R.id.id_teacher_start)
    void preview() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// HH:mm:ss
        Date date = new Date(System.currentTimeMillis());
        String name = simpleDateFormat.format(date);
        final String publisher_name = "cc";
        showProgress();
        int max_publishers = 10;
        //启动定时器
        mStartClss.setEnabled(false);
        mAtlasClient.createLive(name, publisher_name, max_publishers,new CCAtlasCallBack<CCBaseBean>() {
            @Override
            public void onSuccess(CCBaseBean ccBaseBean) {
                dismissProgress();
                showToast("join room success");
                mStartClss.setVisibility(View.GONE);
                mStopClass.setVisibility(View.VISIBLE);
                StopStatus = false;
                mStartClss.setEnabled(true);
            }

            @Override
            public void onFailure(int errCode, String errMsg) {
                dismissProgress();
                showToast(errMsg);
                mStartClss.setEnabled(true);
            }
        });

    }
    private boolean StopStatus = true;
    @NeedsPermission({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    @OnClick(R.id.id_teacher_stop)
    void StopClass() {
        showProgress();
        mStopClass.setEnabled(false);
        mAtlasClient.stopLive(new CCAtlasCallBack<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                dismissProgress();
                showToast("leave room success");
                mAtlasClient.leaveStopYUV(false);
                if (mVideoStreamViews.size() != 0) {
                    CopyOnWriteArrayList<CCStream> streams = getSubscribeRemoteStreams();
                    for (CCStream stream :
                            streams) {
                        removeStreamView(stream);
                    }
                }
                mStopClass.setVisibility(View.GONE);
                mStartClss.setVisibility(View.VISIBLE);
                StopStatus = true;
                mStopClass.setEnabled(true);

            }

            @Override
            public void onFailure(int errCode, String errMsg) {
                dismissProgress();
                showToast(errMsg);
                mStopClass.setEnabled(true);
            }
        });
    }

    @Override
    public void onDrawerSlide(View drawerView, float slideOffset) {

    }

    @Override
    public void onDrawerOpened(View drawerView) {

    }

    @Override
    public void onDrawerClosed(View drawerView) {

    }

    @Override
    public void onDrawerStateChanged(int newState) {

    }

}
