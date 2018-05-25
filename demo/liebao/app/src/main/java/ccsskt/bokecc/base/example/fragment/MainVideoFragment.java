package ccsskt.bokecc.base.example.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.bokecc.sskt.base.exception.StreamException;
import com.bokecc.sskt.base.renderer.CCSurfaceRenderer;

import org.webrtc.RendererCommon;

import java.util.concurrent.CopyOnWriteArrayList;

import butterknife.BindView;
import butterknife.OnClick;
import ccsskt.bokecc.base.example.CCApplication;
import ccsskt.bokecc.base.example.R;
import ccsskt.bokecc.base.example.util.DensityUtil;
import ccsskt.bokecc.base.example.view.VideoStreamView;
import ccsskt.bokecc.base.example.recycle.RecycleViewDivider;
/**
 * 作者 ${王德惠}.<br/>
 */
public class MainVideoFragment extends BaseFragment {

    private static final String TAG = MainVideoFragment.class.getSimpleName();
    protected static final String KEY_PARAM_ROLE = "role";
    public MainVideoFragment() {
        super();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    protected int getContentViewId() {
        return R.layout.fragment_main_video;
    }

    @Override
    protected void transformData() { // 转换数据
    }

    @Override
    protected void setUpView() {
        mVideos.setLayoutManager(new LinearLayoutManager(mActivity));
        mVideos.setAdapter(mVideoAdapter);
        mVideos.addItemDecoration(new RecycleViewDivider(mActivity,
                LinearLayoutManager.HORIZONTAL, DensityUtil.dp2px(mActivity, 4), Color.parseColor("#00000000"),
                0, 0, RecycleViewDivider.TYPE_BETWEEN));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public RecyclerView getRecyclerView() {
        return mVideos;
    }


    @Override
    public synchronized void notifyItemChanged(VideoStreamView videoStreamView, int position, boolean isAdd) {
        if (mVideoStreamViews == null) {
            return;
        }
        if (isAdd) {
            mVideoStreamViews.add(position, videoStreamView);
        } else {
            mVideoStreamViews.remove(videoStreamView);
        }
        if (isAdd) {
            mVideoAdapter.notifyItemInserted(position);
        } else {
            mVideoAdapter.notifyItemRemoved(position);
        }
        if (position != mVideoStreamViews.size() - 1) {
            mVideoAdapter.notifyItemRangeChanged(position, mVideoStreamViews.size() - position);
        }
    }

    @Override
    public void notifyHandUp() {

    }

    private void displayMainVideo() {
    }

    public CopyOnWriteArrayList<VideoStreamView> getDatas() {
        return mVideoStreamViews;
    }

    @BindView(R.id.id_main_video_little_videos)
    RecyclerView mVideos;

    public static MainVideoFragment newInstance(int role) {
        Bundle args = new Bundle();
        args.putInt(KEY_PARAM_ROLE, role);
        MainVideoFragment fragment = new MainVideoFragment();
        fragment.setArguments(args);
        return fragment;
    }

}
