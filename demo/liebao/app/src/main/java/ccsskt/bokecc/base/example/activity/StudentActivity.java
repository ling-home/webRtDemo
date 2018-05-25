package ccsskt.bokecc.base.example.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.bokecc.sskt.base.CCAtlasCallBack;
import com.bokecc.sskt.base.CCAtlasClient;
import com.bokecc.sskt.base.CCBaseBean;
import com.bokecc.sskt.base.CCStream;
import com.bokecc.sskt.base.LocalStreamConfig;
import com.bokecc.sskt.base.bean.LiveList;
import com.bokecc.sskt.base.exception.StreamException;
import com.bokecc.sskt.base.renderer.CCSurfaceRenderer;
import com.intel.webrtc.base.LocalCameraStream;
import com.intel.webrtc.base.LocalCustomizedStream;
import com.intel.webrtc.conference.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.RendererCommon;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.OnClick;
import ccsskt.bokecc.base.example.CCApplication;
import ccsskt.bokecc.base.example.Config;
import ccsskt.bokecc.base.example.R;
import ccsskt.bokecc.base.example.adapter.VideoAdapter;
import ccsskt.bokecc.base.example.base.BaseActivity;
import ccsskt.bokecc.base.example.base.TitleActivity;
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
public class StudentActivity extends BaseActivity implements DrawerLayout.DrawerListener {

    private static final String TAG = StudentActivity.class.getSimpleName();

    private static final String KEY_SESSION_ID = "session_id";
    private static final String KEY_USER_ACCOUNT = "user_account";
    private static final String KEY_ROOM_ID = "room_id";
    @BindView(R.id.id_main_video_layout)
    RelativeLayout mVideoLayout;
    @BindView(R.id.id_main_video_container)
    RelativeLayout mSurfaceContainer;
    @BindView(R.id.id_student_normal)
    ImageButton mlianmai_normal;
    @BindView(R.id.id_student_pressed)
    ImageButton mlianmai_pressed;
    @BindView(R.id.id_video_container)
    RelativeLayout mVideoSurface;
    @BindView(R.id.id_student_content)
    FrameLayout mstudent;

    private CCSurfaceRenderer mLocalRenderer, localCustomizedStreamRenderer;
    private CCStream mLocalStream, mStream;
    private CopyOnWriteArrayList<VideoStreamView> mVideoStreamViews = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<String> userIDs = new CopyOnWriteArrayList<>();
    private CCAtlasClient mAtlasClient;
    private boolean isFront;
    private VideoAdapter mVideoAdapter;
    private ArrayList<BaseFragment> mFragments;
    private BaseFragment mCurFragment;
    private boolean isLive = false;
    private boolean isRendererLocal = true;//判断是不是本地流
    private String userId = "";//判断是不是本地流ID
    //监听订阅流的添加与移除
    private CCAtlasClient.AtlasClientObserver mClientObserver = new CCAtlasClient.AtlasClientObserver() {
        @Override
        public void onServerDisconnected() {
            android.os.Process.killProcess(android.os.Process.myPid());
        }

        @Override
        public void onStreamAdded(final CCStream stream) {
            if (stream.isRemoteIsLocal()) { // 不订阅自己的本地流
                return;
            }

            Log.e(TAG, "onStreamAdded: [ " + stream.getStreamId() + " ]");
            if (stream.getStreamType() != CCStream.REMOTE_MIX && stream.getStreamType() != CCStream.REMOTE_SCREEN &&
                    stream.getStreamType() != CCStream.LOCAL && stream.getStreamType() != CCStream.REMOTE_CUSTOMIZED) {
                // 订阅流
                mStream = stream;
                mSubableRemoteStreams.add(mStream);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        addVideoView(stream);
                    }
                });
            }
        }

        @Override
        public void onStreamRemoved(final CCStream stream) {
            Log.e(TAG, "onStreamRemoved: [ " + stream.getStreamId() + " ]");
            if (stream.getStreamType() != CCStream.REMOTE_MIX && stream.getStreamType() != CCStream.REMOTE_SCREEN &&
                    stream.getStreamType() != CCStream.LOCAL) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        removeStreamView(stream);
                    }
                });
            }
            if (stream.getStream().getAttributes().get("role").equals("1")) {
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        }

        @Override
        public void onStreamError(String streamid, String errorMsg) {

        }
    };

    @Override
    protected int getLayoutId() {
        return R.layout.activity_student;
    }

    @Override
    protected void beforeSetContentView() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onViewCreated() {

        mAtlasClient = new CCAtlasClient(this, Config.PUBLIC_KEY);
        mAtlasClient.addAtlasObserver(mClientObserver);

        createLocalStream();
        mVideoStreamViews = new CopyOnWriteArrayList<>();

        mVideoAdapter = new VideoAdapter(this);
        mFragments = new ArrayList<>();
        mFragments.add(MainVideoFragment.newInstance(CCApplication.TALKER));
        for (BaseFragment fragment :
                mFragments) {
            fragment.setVideoAdapter(mVideoAdapter);
        }
        mCurFragment = mFragments.get(0);
        mCurFragment.addDatas(mVideoStreamViews);

        initRenderer();
        initLive();
        getSupportFragmentManager().beginTransaction().
                replace(R.id.id_student_content, mCurFragment)
                .commitAllowingStateLoss();
        initCustomizedStream();

    }


    private void initLive() {
        LiveList listLive = (LiveList) getIntent().getSerializableExtra("selected_Live");
        String live_id = listLive.getmLiveId();
        String live_name = listLive.getLiveName();
//        showProgress();
        final CCSurfaceRenderer localCustomizedStreamRenderer = new CCSurfaceRenderer(this);
        localCustomizedStreamRenderer.init(mAtlasClient.getEglBase().getEglBaseContext(), null);
        localCustomizedStreamRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);

        RelativeLayout.LayoutParams params;
//        params = new RelativeLayout.LayoutParams(DensityUtil.dp2px(this, 120),
//                DensityUtil.dp2px(this, 160));
        params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        localCustomizedStreamRenderer.setLayoutParams(params);
        mVideoSurface.addView(localCustomizedStreamRenderer);
        mAtlasClient.putClarity("hd");//设置标清分辨率 ld sd hd ud 流畅标清高清超清
        mAtlasClient.joinLive(live_id, live_name, new CCAtlasCallBack<CCStream>() {
            @Override
            public void onSuccess(CCStream ccStream) {
//                dismissProgress();
                try {
                    ccStream.attach(localCustomizedStreamRenderer);
                } catch (StreamException ignored) {
                }
            }

            @Override
            public void onFailure(int errCode, String errMsg) {
//                dismissProgress();
            }
        });
    }

    private void initRenderer() {

        mLocalRenderer = new CCSurfaceRenderer(this);
        mLocalRenderer.init(mAtlasClient.getEglBase().getEglBaseContext(), null);
        mLocalRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        mSurfaceContainer.addView(mLocalRenderer);
        RelativeLayout.LayoutParams params;
        params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        mLocalRenderer.setLayoutParams(params);
    }

    private void initCustomizedStream() {
        localCustomizedStreamRenderer = new CCSurfaceRenderer(this);
        localCustomizedStreamRenderer.init(mAtlasClient.getEglBase().getEglBaseContext(), null);
        localCustomizedStreamRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        mVideoSurface.addView(localCustomizedStreamRenderer);
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
        if (localCustomizedStreamRenderer != null) {
            localCustomizedStreamRenderer.cleanFrame();
            localCustomizedStreamRenderer.release();
        }
        mAtlasClient = null;
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (mStream != null) {
            if (mStream.getUserid() != null) {
                final JSONObject animation = new JSONObject();
                JSONObject phone = new JSONObject();
                try {
                    phone.put("role", "2");
                    phone.put("userid", mStream.getUserid());
                    animation.put("event", "leave");
                    animation.put("data", phone);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mAtlasClient.send(animation.toString(), null);
                try {
                    animation.put("phone", phone);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        if (isLive) {
            showProgress();
            mAtlasClient.leave(new CCAtlasCallBack<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    dismissProgress();
                    android.os.Process.killProcess(android.os.Process.myPid());
                }

                @Override
                public void onFailure(int errCode, String errMsg) {
                    dismissProgress();
                    showToast(errMsg);
                }
            });
        } else {
            mAtlasClient.leaveStopYUV(false);
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

    @OnClick(R.id.id_list_back)
    void onLeftClick() {
        if (mStream != null) {
            if (mStream.getUserid() != null) {
                final JSONObject animation = new JSONObject();
                JSONObject phone = new JSONObject();
                try {
                    phone.put("role", "2");
                    phone.put("userid", mStream.getUserid());
                    animation.put("event", "leave");
                    animation.put("data", phone);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mAtlasClient.send(animation.toString(), null);
            }
        }
        if (isLive) {
            showProgress();
            mAtlasClient.leave(new CCAtlasCallBack<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    dismissProgress();
                    android.os.Process.killProcess(android.os.Process.myPid());
                }

                @Override
                public void onFailure(int errCode, String errMsg) {
                    dismissProgress();
                    showToast(errMsg);
                }
            });
        } else {
            mAtlasClient.leaveStopYUV(false);
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

    private synchronized void addVideoView(final CCStream mStream) {
        if (mStream.getStreamType() != CCStream.REMOTE_MIX && mStream.getStreamType() != CCStream.REMOTE_SCREEN &&
                mStream.getStreamType() != CCStream.LOCAL && mStream.getStreamType() != CCStream.REMOTE_CUSTOMIZED
                && mStream != null) {
//            showProgress();
            final CCSurfaceRenderer mRemoteMixRenderer;
            mRemoteMixRenderer = new CCSurfaceRenderer(this);
            mRemoteMixRenderer.init(mAtlasClient.getEglBase().getEglBaseContext(), null);
            mRemoteMixRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
            Log.i("Hou", "DingYue1-" + mStream.getStreamId());
            Log.i("Hou", "DingYueUerID-" + mStream.getUserid());
            mAtlasClient.subscribe(mStream, new CCAtlasCallBack<CCStream>() {
                @Override
                public void onSuccess(final CCStream ccStream) {
//                    dismissProgress();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                int position;
                                if (mVideoStreamViews.size() == 0) {
                                    mStream.detach();
                                    mStream.attach(mLocalRenderer);
                                    Log.i("Hou", "DA-" + mStream.getStreamId());
                                    VideoStreamView videoRemoteStreamView = new VideoStreamView();
                                    videoRemoteStreamView.setRenderer(mRemoteMixRenderer);
                                    videoRemoteStreamView.setStream(mLocalStream);
                                    position = 0;
                                    mVideoStreamViews.add(videoRemoteStreamView);
                                    userIDs.add("0");
                                } else {
                                    VideoStreamView videoRemoteStreamView = new VideoStreamView();
                                    videoRemoteStreamView.setRenderer(mRemoteMixRenderer);
                                    videoRemoteStreamView.setStream(mStream);
                                    Log.i("Hou", "DingYue2-" + mStream.getStreamId());
                                    position = mVideoStreamViews.size();
                                    mVideoStreamViews.add(videoRemoteStreamView);
                                }
//                                if (isRendererLocal && videoRemoteStreamView.getStream().getStreamId().equals(streamID)) {
//                                    mLocalStream.attach(videoRemoteStreamView.getRenderer());
//                                    isRendererLocal = false;
//                                } else {
//                                    videoRemoteStreamView.getStream().attach(videoRemoteStreamView.getRenderer());
//                                }
                                for (int i = 0; i < mVideoStreamViews.size(); i++) {
                                    Log.i("Hou", "ADD" + i + "-" + mVideoStreamViews.get(i).getStream().getStreamId());
                                }
                                mVideoStreamViews.get(position).getStream().attach(mVideoStreamViews.get(position).getRenderer());
                                mCurFragment.notifyItemChanged(mVideoStreamViews.get(position), position, true);
                            } catch (StreamException e) {
                                e.printStackTrace();
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

    //
    private synchronized void removeStreamView(CCStream stream) {
        VideoStreamView tempView = null;
        try {
            int position = -1;
            for (int i = 0; i < mVideoStreamViews.size(); i++) {
                Log.i("Hou", "+Remove" + i + mVideoStreamViews.get(i).getStream().getStreamId());
                VideoStreamView streamView = mVideoStreamViews.get(i);
                if (streamView.getStream().getStreamId().equals(stream.getStreamId())) {
                    tempView = streamView;
                    position = i;
                    break;
                }
            }
            if (tempView == null) {
                stream.detach();
                if (mVideoStreamViews.size() > 1) {
                    mVideoStreamViews.get(1).getStream().detach();
                    mVideoStreamViews.get(1).getStream().attach(mLocalRenderer);
                    mCurFragment.notifyItemChanged(mVideoStreamViews.get(1), 1, false);
                    mVideoStreamViews.remove(mVideoStreamViews.get(1));
//                    mAtlasClient.unsubcribe(mVideoStreamViews.get(1).getStream(), null);
                } else if (mVideoStreamViews.size() > 0) {
                    mVideoStreamViews.remove(mVideoStreamViews.get(0));
                    mCurFragment.notifyItemChanged(mVideoStreamViews.get(0), 0, false);
                }
            } else {
                mVideoStreamViews.remove(tempView);
                mCurFragment.notifyItemChanged(tempView, position, false);
                stream.detach();
                if (!(stream.getStream() instanceof LocalCameraStream)) {
                    mAtlasClient.unsubcribe(stream, null);
                }
            }

        } catch (StreamException e) {
            e.printStackTrace();
        } finally {
            if (tempView != null) {
                tempView.getRenderer().cleanFrame();
            }
        }
    }

    boolean isVideo = false;
    boolean isVideoSurface = false;

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        StudentActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
//    }


    @NeedsPermission({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void showRationaleForCamera(PermissionRequest request) {
        showRationaleDialog(request);
    }

    @OnNeverAskAgain({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void onCameraNeverAskAgain() {
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

    private boolean status = false;

    @NeedsPermission({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    @OnClick(R.id.id_student_normal)
    void student_lianmai_start() {
        LiveList listLive = (LiveList) getIntent().getSerializableExtra("selected_Live");
        String live_id = listLive.getmLiveId();
        String live_name = listLive.getLiveName();
        mlianmai_normal.setEnabled(false);
        if (isVideoSurface) {
            RelativeLayout.LayoutParams params1;
            params1 = new RelativeLayout.LayoutParams(0,
                    0);
            params1.addRule(RelativeLayout.CENTER_IN_PARENT);
            mVideoSurface.setLayoutParams(params1);
        }
//        if (!mLocalStreamRenderer.isEmpty()) {
//            mLocalRenderer = mLocalStreamRenderer.remove(0); // 从池子中获取
//        } else {
//            mLocalRenderer = new CCSurfaceRenderer(this);
//            mLocalRenderer.init(mAtlasClient.getEglBase().getEglBaseContext(), null);
//            mSurfaceContainer.addView(mLocalRenderer);
//
//        }
        if (mLocalStream == null) {
            LocalStreamConfig config = new LocalStreamConfig.LocalStreamConfigBuilder().build();
            isFront = config.cameraType == LocalStreamConfig.CAMERA_FRONT;
            try {
                mLocalStream = mAtlasClient.createLocalStream(config);
            } catch (StreamException e) {
                showToast(e.getMessage());
            }
        }
        RelativeLayout.LayoutParams params;
        params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        params.addRule(RelativeLayout.ABOVE);
        mLocalRenderer.setLayoutParams(params);
        if (lianmaiStop != null) {
            try {
                lianmaiStop.detach(mLocalRenderer);
            } catch (StreamException e) {
                e.printStackTrace();
            }
        }
        showProgress();
//        try {
////            mLocalStream.attach(mLocalRenderer);
//        } catch (StreamException e) {
//            e.printStackTrace();
//        }
        mAtlasClient.publish(live_id, live_name, new CCAtlasCallBack<CCBaseBean>() {
            @Override
            public void onSuccess(CCBaseBean ccBaseBean) {
                dismissProgress();
                showToast("publish Success");
                if (mLocalStream != null) {
                    mAtlasClient.leaveStopYUV(false);

//                    CopyOnWriteArrayList<CCStream> streams = getSubscribeRemoteStreams();
//                    for (CCStream stream : streams) {
//                        try {
//                            stream.attach(mLocalRenderer);//放大
//                            lianmaiStop = stream;
//                            isRendererLocal = true;
//                            streamID = stream.getStreamId();
//                            addVideoView(stream);//本地老师
//                        } catch (StreamException e) {
//                            e.printStackTrace();
//                        }
//                    }
                    isVideo = true;
                }
                mlianmai_normal.setVisibility(View.GONE);
                mlianmai_pressed.setVisibility(View.VISIBLE);
                mlianmai_normal.setEnabled(true);
                isLive = true;
            }

            @Override
            public void onFailure(int errCode, String errMsg) {
                dismissProgress();
                showToast(errMsg);
                Log.i(TAG, "onFailure: ");
            }
        });
    }

    CCStream lianmaiStop;

    @NeedsPermission({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    @OnClick(R.id.id_student_pressed)
    void student_lianmai_stop() {
//        RelativeLayout.LayoutParams params;
//        params = new RelativeLayout.LayoutParams(0,
//                0);
//        params.addRule(RelativeLayout.CENTER_IN_PARENT);
//        mLocalRenderer.setLayoutParams(params);

        mlianmai_pressed.setEnabled(false);

        if (isVideo) {
            CopyOnWriteArrayList<CCStream> streams = getSubscribeRemoteStreams();
            for (VideoStreamView videoStreamView :
                    mVideoStreamViews) {
                try {
                    mLocalStream.detach(mLocalRenderer);

                } catch (StreamException e) {
                    e.printStackTrace();
                }
                removeStreamView(videoStreamView.getStream());
                getSubscribeRemoteStreams().remove(videoStreamView.getStream());
            }
        }
        RelativeLayout.LayoutParams Surfaceparams;
        Surfaceparams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        Surfaceparams.addRule(RelativeLayout.CENTER_IN_PARENT);
        mVideoSurface.setLayoutParams(Surfaceparams);

        RelativeLayout.LayoutParams Customizedparams;
        Customizedparams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        Customizedparams.addRule(RelativeLayout.CENTER_IN_PARENT);
        localCustomizedStreamRenderer.setLayoutParams(Customizedparams);

        showProgress();
        mAtlasClient.unpublish(new CCAtlasCallBack<CCStream>() {

            @Override
            public void onSuccess(CCStream ccStream) {
                dismissProgress();
                showToast("unpublish Success");
                try {
                    ccStream.attach(mLocalRenderer);
                    lianmaiStop = ccStream;
                } catch (StreamException e) {
                    e.printStackTrace();
                }
                isLive = false;
                mlianmai_normal.setVisibility(View.VISIBLE);
                mlianmai_pressed.setVisibility(View.GONE);
                mlianmai_pressed.setEnabled(true);
                isVideoSurface = true;
                status = true;
            }

            @Override
            public void onFailure(int errCode, String errMsg) {
                dismissProgress();
                showToast(errMsg);
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
