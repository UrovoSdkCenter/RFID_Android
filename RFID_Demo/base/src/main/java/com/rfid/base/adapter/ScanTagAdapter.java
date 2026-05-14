package com.rfid.base.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.rfid.base.R;
import com.rfid.base.bean.ReadData;

import java.util.List;

public class ScanTagAdapter extends RecyclerAdapter<ReadData> {

    private OnClickItem mOnClickItem;


    public ScanTagAdapter(Context context, List<ReadData> datas) {
        super(context, R.layout.item_scan_tag, datas);
    }

    public void setOnClickItem(OnClickItem onClickItem){
        mOnClickItem = onClickItem;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void convert(RecyclerViewHolder hepler, ReadData readTag) {
        TextView tvRssi = hepler.getView(R.id.tv_rssi);
        TextView tvData = hepler.getView(R.id.tv_data);
        TextView tvCount = hepler.getView(R.id.tv_count);

        String epc = readTag.epcId;
        if ( !TextUtils.isEmpty(epc) && !TextUtils.isEmpty(readTag.BID)) {
            epc = "EPC:"+epc+"\r\nTID:"+readTag.memId+"\r\nBID:"+readTag.BID;
        } else if ( !TextUtils.isEmpty(epc) && !TextUtils.isEmpty(readTag.memId)) {
            epc = "EPC:"+epc+"\r\nMem:"+readTag.memId;
        } else  {
            epc = "EPC:"+epc;
        }

        tvData.setText(epc);
        tvCount.setText(readTag.count+"");
        tvRssi.setText(readTag.rssidBm+"");


        hepler.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOnClickItem.item(view,hepler.getAdapterPosition());
            }
        });


    }

    public interface OnClickItem {
        void item(View view,int position);
    }

}
