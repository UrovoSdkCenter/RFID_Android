package com.rfid.base.utils;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

/**
 * Spinner辅助类，用于简化Spinner的初始化和Adapter创建
 */
public class SpinnerHelper {

    /**
     * 创建并设置Spinner的Adapter
     * @param context 上下文
     * @param spinner Spinner控件
     * @param data 数据数组
     * @param defaultSelection 默认选中位置
     */
    public static void setupSpinner(Context context, Spinner spinner, String[] data, int defaultSelection) {
        if (spinner == null || data == null || context == null) {
            return;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, data);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        if (defaultSelection >= 0 && defaultSelection < data.length) {
            spinner.setSelection(defaultSelection, false);
        }
    }

    /**
     * 创建并设置Spinner的Adapter（使用资源ID）
     * @param context 上下文
     * @param spinner Spinner控件
     * @param arrayResId 字符串数组资源ID
     * @param defaultSelection 默认选中位置
     */
    public static void setupSpinner(Context context, Spinner spinner, int arrayResId, int defaultSelection) {
        if (spinner == null || context == null) {
            return;
        }
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context,
                arrayResId, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        if (defaultSelection >= 0) {
            spinner.setSelection(defaultSelection, false);
        }
    }

    /**
     * 创建数字范围的字符串数组
     * @param start 起始值
     * @param end 结束值（包含）
     * @param suffix 后缀（可为null）
     * @return 字符串数组
     */
    public static String[] createRangeArray(int start, int end, String suffix) {
        int size = end - start + 1;
        String[] array = new String[size];
        for (int i = 0; i < size; i++) {
            array[i] = String.valueOf(start + i) + (suffix != null ? suffix : "");
        }
        return array;
    }

    /**
     * 创建数字范围的字符串数组（无后缀）
     * @param start 起始值
     * @param end 结束值（包含）
     * @return 字符串数组
     */
    public static String[] createRangeArray(int start, int end) {
        return createRangeArray(start, end, null);
    }

    /**
     * 更新Spinner的数据
     * @param context 上下文
     * @param spinner Spinner控件
     * @param data 新的数据数组
     */
    public static void updateSpinnerData(Context context, Spinner spinner, String[] data) {
        if (spinner == null || data == null || context == null) {
            return;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, data);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }
}

