package com.rfid.base.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;


/**
 * SharedPreferences 工具类
 */
public class SPUtils {
    private static final String TAG = SPUtils.class.getSimpleName();
    private static String name = "uhf_sp";

    private static Context mContext;
    private static SharedPreferences sharedPreference ;

    public static String KEY_INTEGRATED_INVMODE = "KEY_INTEGRATED_INVMODE";
    public static String KEY_NO_INTEGRATED_INVMODE = "KEY_NO_INTEGRATED_INVMODE";
    public static String KEY_ASCII_CHECKED = "KEY_ASCII_CHECKED";

    public static void initContext(Context context){
        mContext = context.getApplicationContext();
         getSharedPreference(mContext);
    }

    /**
     * 获取SharedPreferences实例对象
     *
     * @return
     */
    private static SharedPreferences getSharedPreference(Context context)  {
        try {
            sharedPreference = context.getSharedPreferences(name, Context.MODE_MULTI_PROCESS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sharedPreference;
    }

    /**
     * 保存一个Boolean类型的值！
     *
     * @param key
     * @param value
     * @return
     */
    public static boolean putBoolean(String key, Boolean value) {
        boolean editorStatus = false;
        try {
            SharedPreferences.Editor editor = sharedPreference.edit();
            editor.putBoolean(key, value);
            editorStatus = editor.commit();
        } catch (Exception e) {
            Log.e(TAG, "putBoolean error: " + key, e);
        }
        return editorStatus;
    }

    /**
     * 保存一个int类型的值！
     *
     * @param key
     * @param value
     * @return
     */
    public static boolean putInt(String key, int value) {
        boolean editorStatus = false;
        try {
            SharedPreferences.Editor editor = sharedPreference.edit();
            editor.putInt(key, value);
            editorStatus = editor.commit();
        } catch (Exception e) {
            Log.e(TAG, "putInt error: " + key, e);
        }
        return editorStatus;
    }

    /**
     * 保存一个float类型的值！
     *
     * @param key
     * @param value
     * @return
     */
    public static boolean putFloat(String key, float value) {
        boolean editorStatus = false;
        try {
            SharedPreferences.Editor editor = sharedPreference.edit();
            editor.putFloat(key, value);
            editorStatus = editor.commit();
        } catch (Exception e) {
            Log.e(TAG, "putFloat error: " + key, e);
        }
        return editorStatus;
    }


    /**
     * 保存一个long类型的值！
     *
     * @param key
     * @param value
     * @return
     */
    public static boolean putLong(String key, long value) {
        boolean editorStatus = false;
        try {
            SharedPreferences.Editor editor = sharedPreference.edit();
            editor.putLong(key, value);
            editorStatus = editor.commit();
        } catch (Exception e) {
            Log.e(TAG, "putLong error: " + key, e);
        }
        return editorStatus;
    }

    /**
     * 保存一个String类型的值！
     *
     * @param key
     * @param value
     * @return
     */
    public static boolean putString(String key, String value) {
        boolean editorStatus = false;
        try {
            SharedPreferences.Editor editor = sharedPreference.edit();
            editor.putString(key, value);
            editorStatus = editor.commit();
        } catch (Exception e) {
            Log.e(TAG, "putString error: " + key, e);
        }
        return editorStatus;
    }

    /**
     * 获取String的value
     *
     * @param key      名字
     * @param defValue 默认值
     * @return
     */
    public static String getString(String key, String defValue) {
        String result = null;
        try {
            result = sharedPreference.getString(key, defValue);
        } catch (Exception e) {
            Log.e(TAG, "getString error: " + key, e);
            result = defValue;
        }
        return result;
    }

    /**
     * 获取int的value
     *
     * @param key      名字
     * @param defValue 默认值
     * @return
     */
    public static int getInt(String key, int defValue) {
        int result = 0;
        try {
            result = sharedPreference.getInt(key, defValue);
        } catch (Exception e) {
            Log.e(TAG, "getInt error: " + key, e);
            result = defValue;
        }
        return result;
    }

    /**
     * 获取float的value
     *
     * @param key      名字
     * @param defValue 默认值
     * @return
     */
    public static float getFloat(String key, Float defValue) {
        float result = 0;
        try {
            result = sharedPreference.getFloat(key, defValue);
        } catch (Exception e) {
            Log.e(TAG, "getFloat error: " + key, e);
            result = defValue;
        }
        return result;
    }

    /**
     * 获取boolean的value
     *
     * @param key      名字
     * @param defValue 默认值
     * @return
     */
    public static boolean getBoolean(String key, Boolean defValue) {
        boolean result = false;
        try {
            result = sharedPreference.getBoolean(key, defValue);
        } catch (Exception e) {
            Log.e(TAG, "getBoolean error: " + key, e);
            result = defValue;
        }
        return result;
    }

    /**
     * 获取long的value
     *
     * @param key      名字
     * @param defValue 默认值
     * @return
     */
    public static long getLong(String key, long defValue) {
        long result = 0;
        try {
            result = sharedPreference.getLong(key, defValue);
        } catch (Exception e) {
            Log.e(TAG, "getLong error: " + key, e);
            result = defValue;
        }
        return result;
    }

    /**
     * 清除所有
     */
    public static void clearAll() {
        try {
            SharedPreferences.Editor editor = sharedPreference.edit();
            editor.clear();
            editor.apply();
        } catch (Exception e) {
            Log.e(TAG, "clearAll error", e);
        }
    }
}
