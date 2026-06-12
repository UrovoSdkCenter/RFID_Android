package com.rfid.base.widget;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.rfid.base.R;

/**
 * Inventory Mode 选择弹窗
 * 每个 item 显示加粗标题 + 小字描述，替代原 Spinner 的单行文字显示
 */
public class InventoryModeDialog {

    public interface OnItemSelectedListener {
        void onItemSelected(int position);
    }

    /** 单个选项数据 */
    public static class ModeItem {
        public final String title;
        public final String description;

        public ModeItem(String title, String description) {
            this.title = title;
            this.description = description;
        }
    }

    private final Context mContext;
    private final String mDialogTitle;
    private final ModeItem[] mItems;
    private OnItemSelectedListener mListener;
    private int mSelectedPosition = -1;

    public InventoryModeDialog(Context context, String dialogTitle, ModeItem[] items) {
        mContext = context;
        mDialogTitle = dialogTitle;
        mItems = items;
    }

    public InventoryModeDialog setSelectedPosition(int position) {
        mSelectedPosition = position;
        return this;
    }

    public InventoryModeDialog setOnItemSelectedListener(OnItemSelectedListener listener) {
        mListener = listener;
        return this;
    }

    public void show() {
        final Dialog dialog = new Dialog(mContext, android.R.style.Theme_DeviceDefault_Light_Dialog_MinWidth);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_inventory_mode);

        // 设置弹窗宽度为屏幕宽度的 90%
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = (int) (mContext.getResources().getDisplayMetrics().widthPixels * 0.9f);
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(params);
        }

        TextView tvTitle = dialog.findViewById(R.id.tv_dialog_title);
        tvTitle.setText(mDialogTitle);

        LinearLayout container = dialog.findViewById(R.id.ll_items_container);
        LayoutInflater inflater = LayoutInflater.from(mContext);

        for (int i = 0; i < mItems.length; i++) {
            final int position = i;
            final ModeItem item = mItems[i];

            View itemView = inflater.inflate(R.layout.item_inventory_mode, container, false);
            TextView tvTitle2 = itemView.findViewById(R.id.tv_mode_title);
            TextView tvDesc = itemView.findViewById(R.id.tv_mode_desc);

            tvTitle2.setText(item.title);
            tvDesc.setText(item.description);

            // 选中项标题高亮
            if (position == mSelectedPosition) {
                tvTitle2.setTextColor(mContext.getResources().getColor(R.color.main_color));
            }

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    if (mListener != null) {
                        mListener.onItemSelected(position);
                    }
                }
            });

            container.addView(itemView);
        }

        dialog.show();
    }
}
