package com.rfid.base.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.rfid.base.R;

/**
 * Dialog工具类 - 单例模式
 * 提供单按钮和双按钮对话框功能，避免重复弹出
 */
public class DialogUtils {
    
    private static DialogUtils instance;
    private AlertDialog currentDialog;
    
    /**
     * 私有构造函数，防止外部实例化
     */
    private DialogUtils() {
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized DialogUtils getInstance() {
        if (instance == null) {
            instance = new DialogUtils();
        }
        return instance;
    }
    
    /**
     * 显示单按钮对话框
     * @param context 上下文
     * @param title 标题
     * @param message 消息内容
     * @param buttonText 按钮文字
     * @param listener 按钮点击监听器
     */
    public void showSingleButtonDialog(Context context, String title, String message, 
                                     String buttonText, DialogInterface.OnClickListener listener) {
        dismissCurrentDialog();
        
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title)
               .setMessage(message)
               .setPositiveButton(buttonText, listener)
               .setCancelable(false);
        
        currentDialog = builder.create();
        currentDialog.show();
    }
    
    /**
     * 显示单按钮对话框（使用默认确定按钮）
     * @param context 上下文
     * @param title 标题
     * @param message 消息内容
     * @param listener 按钮点击监听器
     */
    public void showSingleButtonDialog(Context context, String title, String message, 
                                     DialogInterface.OnClickListener listener) {
        showSingleButtonDialog(context, title, message, 
                             context.getString(R.string.strok), listener);
    }
    
    /**
     * 显示双按钮对话框（默认取消+确定）
     * @param context 上下文
     * @param title 标题
     * @param message 消息内容
     * @param positiveListener 确定按钮监听器
     * @param negativeListener 取消按钮监听器
     */
    public void showDoubleButtonDialog(Context context, String title, String message,
                                     DialogInterface.OnClickListener positiveListener,
                                     DialogInterface.OnClickListener negativeListener) {
        showDoubleButtonDialog(context, title, message,
                             context.getString(R.string.strcancle),
                             context.getString(R.string.strok),
                             negativeListener, positiveListener);
    }
    
    /**
     * 显示双按钮对话框（自定义按钮文字）
     * @param context 上下文
     * @param title 标题
     * @param message 消息内容
     * @param negativeText 左侧按钮文字
     * @param positiveText 右侧按钮文字
     * @param negativeListener 左侧按钮监听器
     * @param positiveListener 右侧按钮监听器
     */
    public void showDoubleButtonDialog(Context context, String title, String message,
                                     String negativeText, String positiveText,
                                     DialogInterface.OnClickListener negativeListener,
                                     DialogInterface.OnClickListener positiveListener) {
        dismissCurrentDialog();
        
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title)
               .setMessage(message)
               .setNegativeButton(negativeText, negativeListener)
               .setPositiveButton(positiveText, positiveListener)
               .setCancelable(false);
        
        currentDialog = builder.create();
        currentDialog.show();
    }
    
    /**
     * 显示确认对话框（带默认取消+确定，确定时执行操作）
     * @param context 上下文
     * @param title 标题
     * @param message 消息内容
     * @param confirmListener 确定按钮监听器
     */
    public void showConfirmDialog(Context context, String title, String message,
                                DialogInterface.OnClickListener confirmListener) {
        showDoubleButtonDialog(context, title, message,
                             confirmListener,
                             new DialogInterface.OnClickListener() {
                                 @Override
                                 public void onClick(DialogInterface dialog, int which) {
                                     dialog.dismiss();
                                 }
                             });
    }
    
    /**
     * 显示信息对话框（只有确定按钮）
     * @param context 上下文
     * @param title 标题
     * @param message 消息内容
     */
    public void showInfoDialog(Context context, String title, String message) {
        showSingleButtonDialog(context, title, message,
                             new DialogInterface.OnClickListener() {
                                 @Override
                                 public void onClick(DialogInterface dialog, int which) {
                                     dialog.dismiss();
                                 }
                             });
    }
    
    /**
     * 显示 EPC 选择列表对话框
     * @param context  上下文
     * @param items    EPC 列表
     * @param listener 选中回调，参数为选中的 EPC 字符串
     */
    public void showEpcPickerDialog(Context context, String[] items,
                                    OnItemSelectedListener listener) {
        dismissCurrentDialog();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.select_epc)
               .setItems(items, (dialog, which) -> listener.onSelected(items[which]));
        currentDialog = builder.create();
        currentDialog.show();
    }

    /**
     * EPC 选中回调接口
     */
    public interface OnItemSelectedListener {
        void onSelected(String item);
    }

    /**
     * 关闭当前对话框
     */
    public void dismissCurrentDialog() {
        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
            currentDialog = null;
        }
    }
    
    /**
     * 检查是否有对话框正在显示
     */
    public boolean isDialogShowing() {
        return currentDialog != null && currentDialog.isShowing();
    }
}