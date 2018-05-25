package ccsskt.bokecc.base.example.adapter;

import android.content.Context;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bokecc.sskt.base.bean.LiveList;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import butterknife.BindViews;
import ccsskt.bokecc.base.example.recycle.BaseRecycleAdapter;
import ccsskt.bokecc.base.example.view.SwipeMenuLayout;
import ccsskt.bokecc.base.example.R;
import butterknife.BindView;

/**
 * 作者 ${王德惠}.<br/>
 */

public class ListAdapter extends BaseRecycleAdapter<ListAdapter.DocViewHolder, LiveList> {


    public ListAdapter(Context context) {
        super(context);
    }

    @Override
    public void onBindViewHolder(DocViewHolder holder, int position) {
        final LiveList list = mDatas.get(position);
        holder.mName.setText(list.getLiveName());
        Calendar calendar=Calendar.getInstance();
        calendar.setTimeInMillis(list.getmLiveTime() * 1000);
        SimpleDateFormat fmat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time=fmat.format(calendar.getTime());
        holder.mLiveTime.setText("Start time："+ time);
        if(list.getmLiveStatus() == 1){
            holder.mLiveStatus.setText("Live");
        } else if(list.getmLiveStatus() == 0){
            holder.mLiveStatus.setText("未直播");
        } else{
            holder.mLiveStatus.setText("直播结束");
        }
    }

    @Override
    public int getItemView(int viewType) {
        return R.layout.doc_item_layout;
    }

    @Override
    public DocViewHolder getViewHolder(View itemView, int viewType) {
        return new DocViewHolder(itemView);
    }

    final class DocViewHolder extends BaseRecycleAdapter.BaseViewHolder {
        @BindView(R.id.id_doc_item_name)
        TextView mName;
        @BindView(R.id.id_live_time)
        TextView mLiveTime;
        @BindView(R.id.id_live_status)
        TextView mLiveStatus;
        DocViewHolder(View itemView) {
            super(itemView);
        }
    }

}
