package com.rfid.base.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

/**
 * Tab切换广播管理工具类
 * 用于处理MainActivity和子Activity之间的Tab切换事件通信
 */
public class TabBroadcastManager {
    
    private static final String TAG = "TabBroadcastManager";
    
    // 广播Action常量
    public static final String ACTION_TAB_CHANGED = "com.rfid.demo.TAB_CHANGED";
    
    // 广播Extra键名常量
    public static final String EXTRA_TAB_TAG = "tab_tag";
    public static final String EXTRA_TAB_INDEX = "tab_index";
    
    /**
     * 发送Tab切换广播
     * @param context 上下文
     * @param tabTag Tab标签
     * @param tabIndex Tab索引
     */
    public static void sendTabChangedBroadcast(Context context, String tabTag, int tabIndex) {
        try {
            Intent broadcastIntent = new Intent(ACTION_TAB_CHANGED);
            broadcastIntent.putExtra(EXTRA_TAB_TAG, tabTag);
            broadcastIntent.putExtra(EXTRA_TAB_INDEX, tabIndex);
            context.sendBroadcast(broadcastIntent);
        } catch (Exception e) {
            Log.e(TAG, ">>Failed to send tab changed broadcast", e);
        }
    }
    
    /**
     * 注册Tab切换广播接收器
     * @param context 上下文
     * @param listener Tab切换监听器
     * @return 广播接收器实例
     */
    public static BroadcastReceiver registerTabChangeReceiver(Context context, TabChangeListener listener) {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ACTION_TAB_CHANGED.equals(intent.getAction())) {
                    String tabTag = intent.getStringExtra(EXTRA_TAB_TAG);
                    int tabIndex = intent.getIntExtra(EXTRA_TAB_INDEX, -1);
                    if (listener != null) {
                        listener.onTabChanged(tabTag, tabIndex);
                    }
                }
            }
        };
        
        try {
            IntentFilter filter = new IntentFilter(ACTION_TAB_CHANGED);
            context.registerReceiver(receiver, filter,Context.RECEIVER_EXPORTED);
        } catch (Exception e) {
            Log.e(TAG, ">>Failed to register tab change receiver", e);
        }
        
        return receiver;
    }
    
    /**
     * 取消注册广播接收器
     * @param context 上下文
     * @param receiver 广播接收器
     */
    public static void unregisterTabChangeReceiver(Context context, BroadcastReceiver receiver) {
        if (receiver != null) {
            try {
                context.unregisterReceiver(receiver);
            } catch (Exception e) {
                Log.e(TAG, ">>Failed to unregister tab change receiver", e);
            }
        }
    }
    
    /**
     * Tab切换监听器接口
     */
    public interface TabChangeListener {
        /**
         * Tab切换回调
         * @param tabTag Tab标签
         * @param tabIndex Tab索引
         */
        void onTabChanged(String tabTag, int tabIndex);
    }
}