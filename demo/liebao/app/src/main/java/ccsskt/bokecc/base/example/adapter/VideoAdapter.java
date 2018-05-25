package ccsskt.bokecc.base.example.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bokecc.sskt.base.CCAtlasCallBack;
import com.bokecc.sskt.base.CCStream;
import com.bokecc.sskt.base.exception.StreamException;
import com.bokecc.sskt.base.renderer.CCSurfaceRenderer;

import ccsskt.bokecc.base.example.base.TitleActivity;
import ccsskt.bokecc.base.example.recycle.BaseRecycleAdapter;
import ccsskt.bokecc.base.example.util.DensityUtil;
import ccsskt.bokecc.base.example.view.VideoStreamView;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import butterknife.BindView;
import ccsskt.bokecc.base.example.R;

/**
 * 作者 ${王德惠}.<br/>
 */

public class VideoAdapter extends BaseRecycleAdapter<VideoAdapter.LittleVideoViewHolder, VideoStreamView> {

    private ConcurrentHashMap<CCSurfaceRenderer, RelativeLayout> views = new ConcurrentHashMap<>();
    private ConcurrentHashMap<RelativeLayout, CCSurfaceRenderer> mRootRenderers = new ConcurrentHashMap<>();

    public VideoAdapter(Context context) {
        super(context);
    }

    @Override
    public void onBindViewHolder(LittleVideoViewHolder holder, int position) {
        VideoStreamView videoStreamView = mDatas.get(position);
        CCSurfaceRenderer renderer = videoStreamView.getRenderer();
        RelativeLayout.LayoutParams params;
        params = new RelativeLayout.LayoutParams(DensityUtil.dp2px(mContext, 80),
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        renderer.setLayoutParams(params);
        renderer.setZOrderOnTop(true);
        renderer.setZOrderMediaOverlay(true);
        renderer.bringToFront();
        if (views.get(renderer) != null) { // 判断当前需要被添加的子布局是否有父布局
            views.get(renderer).removeView(renderer); // 找到该子布局的父布局，从父布局移除
        }
        if (mRootRenderers.get(holder.mLittleItemRoot) != null) { // 如果跟布局下面有渲染布局 移除该渲染布局
            holder.mLittleItemRoot.removeView(mRootRenderers.get(holder.mLittleItemRoot));
        }
        holder.mLittleItemRoot.addView(renderer,-1);
        views.put(renderer, holder.mLittleItemRoot);
        mRootRenderers.put(holder.mLittleItemRoot, renderer); // 存放根布局和渲染布局
    }

    @Override
    public int getItemView(int viewType) {
        return R.layout.little_item_video_layout;
    }

    @Override
    public LittleVideoViewHolder getViewHolder(View itemView, int viewType) {
        return new LittleVideoViewHolder(itemView);
    }

    final class LittleVideoViewHolder extends BaseRecycleAdapter.BaseViewHolder {

        @BindView(R.id.id_little_video_item_root)
        RelativeLayout mLittleItemRoot;

        LittleVideoViewHolder(View itemView) {
            super(itemView);
        }
    }

}
