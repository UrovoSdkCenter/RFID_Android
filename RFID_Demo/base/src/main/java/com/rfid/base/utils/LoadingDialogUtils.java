package com.rfid.base.utils;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.StringRes;

import com.rfid.base.R;

/**
 * Loading 对话框工具类（单例）
 * 提供转圈 + 文字提示的 Loading 弹窗，线程安全地显示/关闭
 */
public class LoadingDialogUtils {

    private static volatile LoadingDialogUtils instance;

    private Dialog currentDialog;

    private LoadingDialogUtils() {
    }

    public static LoadingDialogUtils getInstance() {
        if (instance == null) {
            synchronized (LoadingDialogUtils.class) {
                if (instance == null) {
                    instance = new LoadingDialogUtils();
                }
            }
        }
        return instance;
    }

    /**
     * 显示 Loading 对话框
     *
     * @param context 上下文（建议传 Activity）
     * @param message 提示文字
     */
    public void show(Context context, String message) {
        dismiss();
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null);
        TextView tvMessage = view.findViewById(R.id.tv_loading_message);
        tvMessage.setText(message);

        Dialog dialog = new Dialog(context, R.style.LoadingDialogStyle);
        dialog.setContentView(view);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        currentDialog = dialog;
        currentDialog.show();
    }

    /**
     * 显示 Loading 对话框（通过字符串资源 ID）
     *
     * @param context 上下文
     * @param resId   字符串资源 ID
     */
    public void show(Context context, @StringRes int resId) {
        show(context, context.getString(resId));
    }

    /**
     * 关闭当前 Loading 对话框
     */
    public void dismiss() {
        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
        }
        currentDialog = null;
    }

    /**
     * 是否正在显示
     */
    public boolean isShowing() {
        return currentDialog != null && currentDialog.isShowing();
    }
}
