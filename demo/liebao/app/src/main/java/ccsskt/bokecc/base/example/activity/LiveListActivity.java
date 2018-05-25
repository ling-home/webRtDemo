package ccsskt.bokecc.base.example.activity;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.bokecc.sskt.base.CCAtlasCallBack;
import com.bokecc.sskt.base.CCAtlasClient;
import com.bokecc.sskt.base.bean.LiveList;
import com.bokecc.sskt.base.bean.LiveListSet;

import butterknife.BindView;
import ccsskt.bokecc.base.example.CCApplication;
import ccsskt.bokecc.base.example.Config;
import ccsskt.bokecc.base.example.R;
import ccsskt.bokecc.base.example.adapter.ListAdapter;
import ccsskt.bokecc.base.example.base.TitleActivity;
import ccsskt.bokecc.base.example.base.TitleOptions;
import ccsskt.bokecc.base.example.recycle.BaseOnItemTouch;
import ccsskt.bokecc.base.example.recycle.OnClickListener;
import ccsskt.bokecc.base.example.recycle.RecycleViewDivider;
/**
 * 作者 ${王德惠}.<br/>
 */
public class LiveListActivity extends TitleActivity<LiveListActivity.ListViewHolder> {


    private ListAdapter mListAdapter;
    private CCAtlasClient mAtlasClient;

    @Override
    protected int getContentLayoutId() {
        return R.layout.list_layout;
    }

    @Override
    protected ListViewHolder getViewHolder(View contentView) {
        return new ListViewHolder(contentView);
    }

    @Override
    protected void onBindViewHolder(final ListViewHolder holder) {

        TitleOptions.Builder builder = new TitleOptions.Builder();
        TitleOptions options = builder.leftStatus(TitleOptions.VISIBLE).
                leftResId(R.drawable.title_back).
                rightStatus(TitleOptions.GONE).
                titleStatus(TitleOptions.VISIBLE).title("Live list").
                onTitleClickListener(new TitleOptions.OnLeftClickListener() {
                    @Override
                    public void onLeft() {
                        finish();
                    }
                }).
                build();
        setTitleOptions(options);
        mAtlasClient = new CCAtlasClient(this,Config.PUBLIC_KEY);

        mListAdapter = new ListAdapter(this);
//        holder.mList.setVisibility(View.GONE);
        holder.mList.setLayoutManager(new LinearLayoutManager(this));
        holder.mList.addItemDecoration(new RecycleViewDivider(this,
                LinearLayoutManager.HORIZONTAL, 1, Color.parseColor("#E8E8E8"),
                0, 0, RecycleViewDivider.TYPE_BOTTOM));
        holder.mList.setAdapter(mListAdapter);
        LiveRoomName();
        holder.mList.addOnItemTouchListener(new BaseOnItemTouch(holder.mList, new OnClickListener() {
            @Override
            public void onClick(RecyclerView.ViewHolder viewHolder) {
                int position = holder.mList.getChildAdapterPosition(viewHolder.itemView);
                selectLive(position);
            }
        }));

    }

    private void selectLive(int position) {
        LiveList mlivelist = mListAdapter.getDatas().get(position);
        Intent data = new Intent(this,StudentActivity.class);
        data.putExtra("selected_Live", mlivelist);
        startActivity(data);
    }
    /**
     * 获取直播间文档列表
     */
    private void LiveRoomName() {
        showProgress();
        mAtlasClient.liveList(1, new CCAtlasCallBack<LiveListSet>() {
            @Override
            public void onSuccess(final LiveListSet mLiveListSet) {
                dismissProgress();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mListAdapter.bindDatas(mLiveListSet.getLiveListSet());
                        mListAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onFailure(int errCode, String errMsg) {
                dismissProgress();
            }
        });
    }
    @Override
    protected void onDestroy() {
        mAtlasClient = null;
        mListAdapter = null;
        super.onDestroy();
    }


    @Override
    public void onBackPressed() {
        showProgress();
        finish();
    }
    final class ListViewHolder extends  TitleActivity.ViewHolder {

        @BindView(R.id.id_live_list)
        RecyclerView mList;

        public ListViewHolder(View view) {
            super(view);
        }

    }

}
