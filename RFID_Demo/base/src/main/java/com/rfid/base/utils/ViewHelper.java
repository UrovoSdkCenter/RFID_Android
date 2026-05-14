package com.rfid.base.utils;

import android.view.View;
import java.util.Arrays;
import java.util.List;

/**
 * View辅助类，用于简化View的批量操作
 */
public class ViewHelper {

    /**
     * 为多个View设置相同的点击监听器
     * @param listener 点击监听器
     * @param views 需要设置监听器的View列表
     */
    public static void setOnClickListener(View.OnClickListener listener, View... views) {
        if (views == null || listener == null) {
            return;
        }
        for (View view : views) {
            if (view != null) {
                view.setOnClickListener(listener);
            }
        }
    }

    /**
     * 为多个View设置相同的点击监听器
     * @param listener 点击监听器
     * @param views 需要设置监听器的View列表
     */
    public static void setOnClickListener(View.OnClickListener listener, List<View> views) {
        if (views == null || listener == null) {
            return;
        }
        for (View view : views) {
            if (view != null) {
                view.setOnClickListener(listener);
            }
        }
    }

    /**
     * 设置多个View的可见性
     * @param visibility 可见性值（View.VISIBLE, View.GONE, View.INVISIBLE）
     * @param views 需要设置可见性的View列表
     */
    public static void setVisibility(int visibility, View... views) {
        if (views == null) {
            return;
        }
        for (View view : views) {
            if (view != null) {
                view.setVisibility(visibility);
            }
        }
    }

    /**
     * 设置多个View的启用状态
     * @param enabled 是否启用
     * @param views 需要设置启用状态的View列表
     */
    public static void setEnabled(boolean enabled, View... views) {
        if (views == null) {
            return;
        }
        for (View view : views) {
            if (view != null) {
                view.setEnabled(enabled);
            }
        }
    }
}

