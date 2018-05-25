package ccsskt.bokecc.base.example.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.concurrent.CopyOnWriteArrayList;

import butterknife.ButterKnife;
import butterknife.Unbinder;
import ccsskt.bokecc.base.example.adapter.VideoAdapter;
import ccsskt.bokecc.base.example.recycle.MyItemAnimator;
import ccsskt.bokecc.base.example.view.VideoStreamView;
/**
 * 作者 ${王德惠}.<br/>
 */
public abstract class BaseFragment extends Fragment {

    protected Activity mActivity;
    protected View mRoot;
    protected boolean isViewInitialize = false;

    protected Handler mHandler;

    private Unbinder mUnbinder;
    protected VideoAdapter mVideoAdapter;
    protected CopyOnWriteArrayList<VideoStreamView> mVideoStreamViews;
    protected View mContentView;
    protected int mRole;

    protected static final String KEY_PARAM_ROLE = "role";

    protected BaseFragment() {
        // Required empty public constructor
        mHandler = new Handler(Looper.getMainLooper());
        mVideoStreamViews = new CopyOnWriteArrayList<>();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() == null || !getArguments().keySet().contains(KEY_PARAM_ROLE)) {
            throw new NullPointerException();
        }
        mRole = getArguments().getInt(KEY_PARAM_ROLE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mContentView = inflater.inflate(getContentViewId(), container, false);
        mUnbinder = ButterKnife.bind(this, mContentView);
        mRoot = mContentView;
        return mContentView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        isViewInitialize = true;
        transformData();
        mVideoAdapter.bindDatas(mVideoStreamViews);
        setUpView();
        getRecyclerView().setItemAnimator(new MyItemAnimator());
        ((SimpleItemAnimator)getRecyclerView().getItemAnimator()).setSupportsChangeAnimations(false);
    }

    protected void transformData() {
        // Ignore 子类需要的时候进行实现
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
        clearDatas();
        isViewInitialize = false;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (Activity) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity = null;
    }

    protected View findViewById(int id) {
        return mContentView.findViewById(id);
    }

    protected void showToast(String msg) {
        if (TextUtils.isEmpty(msg)) {
            return;
        }
        Toast.makeText(mActivity, msg, Toast.LENGTH_SHORT).show();
    }

    protected void toastOnUiThread(final String msg) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                showToast(msg);
            }
        });
    }

    protected abstract int getContentViewId();

    protected abstract void setUpView();

    public abstract RecyclerView getRecyclerView();

    public abstract void notifyItemChanged(VideoStreamView videoStreamView, int position, boolean isAdd);

    public abstract void notifyHandUp();

    public void setVideoAdapter(VideoAdapter videoAdapter) {
        mVideoAdapter = videoAdapter;
    }

    public void clearDatas() {
        if (mVideoStreamViews != null) {
            mVideoStreamViews.clear();
            mVideoAdapter.clear();
        }
    }

    public void addDatas(CopyOnWriteArrayList<VideoStreamView> datas) {
        mVideoStreamViews.addAll(datas);
    }

    public void notifySelfRemove(VideoStreamView selfView) {
        mVideoStreamViews.remove(selfView);
        mVideoAdapter.notifyDataSetChanged();
    }
}
