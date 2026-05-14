package com.rfid.base.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

/**
 * 一个Toast辅助类，确保在主线程显示Toast，并且新的Toast会立即覆盖上一个未完成的Toast。
 * 使用示例：
 * ToastHelper.showToast(context, "Hello World");
 * 或指定时长：
 * ToastHelper.showToast(context, "Hello World", Toast.LENGTH_LONG);
 */
public class ToastUtils {
    private static Toast currentToast; // 静态变量保存当前Toast实例
    private static Handler handler = new Handler(Looper.getMainLooper()); // 主线程Handler

    private static Context mContext;
    public static void init(Context context){
        mContext = context;

    }
    /**
     * 显示短时长的Toast（默认Toast.LENGTH_SHORT）。
     * @param message 要显示的文本内容。
     */
    public static void show(String message) {
        showToast(  message, Toast.LENGTH_SHORT);
    }
    /**
     * 显示短时长的Toast（默认Toast.LENGTH_SHORT）。
     * @param message 要显示的文本内容。
     */
    public static void show(boolean isToast ,String message) {
        if (isToast) {
            showToast(message, Toast.LENGTH_SHORT);
        }
    }
    /**
     * 显示指定时长的Toast。
     * @param message 要显示的文本内容。
     * @param duration 显示时长，如Toast.LENGTH_SHORT或Toast.LENGTH_LONG。
     */
    public static void showToast(  final String message, final int duration) {
        // 检查当前是否在主线程，如果不是则切换到主线程执行
        if (Looper.myLooper() != Looper.getMainLooper()) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    showToastInternal( message, duration);
                }
            });
        } else {
            showToastInternal( message, duration);
        }
    }

    /**
     * 内部方法，实际处理Toast的显示和覆盖逻辑。
     */
    private static void showToastInternal(  String message, int duration) {
        if (mContext == null) {
            return; // 上下文为空时直接返回
        }
        // 取消上一个正在显示的Toast
        if (currentToast != null) {
            currentToast.cancel();
        }
        // 创建新的Toast并显示
        currentToast = Toast.makeText(mContext.getApplicationContext(), message, duration);
        currentToast.show();
    }

}
