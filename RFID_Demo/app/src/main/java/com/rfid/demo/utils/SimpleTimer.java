package com.rfid.demo.utils;

import android.os.Handler;
import android.os.Looper;

/**
 * 单次计时工具类
 * 功能：从0开始每秒回调一次，格式为00:00:00
 */
public class SimpleTimer {

    private Handler handler;
    private Runnable timerRunnable;
    private int elapsedSeconds = 0;
    private boolean isRunning = false;
    private TimerCallback callback;

    public interface TimerCallback {
        /**
         * 每秒回调一次，返回格式化的时间字符串
         * @param timeString 格式为00:00:00的时间字符串
         * @param elapsedSeconds 经过的秒数
         */
        void onTick(String timeString, int elapsedSeconds);
    }

    public SimpleTimer() {
        handler = new Handler(Looper.getMainLooper());
    }

    /**
     * 开始计时
     * @param callback 计时回调接口
     */
    public void start(TimerCallback callback) {
        if (isRunning) {
            stop();
        }

        this.callback = callback;
        this.elapsedSeconds = 0;
        this.isRunning = true;

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isRunning) return;

                elapsedSeconds++;
                String timeString = formatTime(elapsedSeconds);

                if (callback != null) {
                    callback.onTick(timeString, elapsedSeconds);
                }

                // 每秒执行一次
                handler.postDelayed(this, 1000);
            }
        };

        // 立即执行第一次回调，显示00:00:00
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    callback.onTick("00:00:00", 0);
                }
                handler.postDelayed(timerRunnable, 1000);
            }
        });
    }

    /**
     * 停止计时
     */
    public void stop() {
        isRunning = false;
        if (handler != null && timerRunnable != null) {
            handler.removeCallbacks(timerRunnable);
        }
    }

    /**
     * 格式化时间为00:00:00格式
     * @param totalSeconds 总秒数
     * @return 格式化后的时间字符串
     */
    private String formatTime(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * 获取当前是否正在计时
     * @return 计时状态
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * 释放资源，避免内存泄漏
     */
    public void release() {
        stop();
        handler = null;
        callback = null;
    }
}