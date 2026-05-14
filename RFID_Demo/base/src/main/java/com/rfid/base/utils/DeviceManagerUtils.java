package com.rfid.base.utils;

import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class DeviceManagerUtils {
    private static final String TAG = DeviceManagerUtils.class.getSimpleName();
    private static final String CLASS_NAME = "android.device.DeviceManager";
    private static Class<?> deviceManagerClass;
    private static  Object deviceManager;

    static {
        try {
            deviceManagerClass = Class.forName(CLASS_NAME);
            // 2. 创建DeviceManager实例
            Constructor<?> constructor = deviceManagerClass.getConstructor();
             deviceManager = constructor.newInstance();

        } catch (Exception e) {
            Log.e(TAG, "Exception stub.");
        }
    }
    public static String getDeviceId() {
        if (deviceManagerClass == null || deviceManager == null) return "";
        try {
            // 5. 实现getDeviceId方法调用
            Method getDeviceIdMethod = deviceManagerClass.getMethod("getDeviceId");
            String SN = (String) getDeviceIdMethod.invoke(deviceManager);
            return SN;
        } catch (Exception e) {
            Log.e(TAG, "Failed to getDeviceId ");
            return "";
        }
    }
    public static String getSettingProperty(String key) {
        if (deviceManagerClass == null || deviceManager == null) return "";
        try {
            // 4. 实现getSettingProperty方法调用
            Method getSettingPropertyMethod = deviceManagerClass.getMethod(
                    "getSettingProperty", String.class);
            String property = (String) getSettingPropertyMethod.invoke(
                    deviceManager, key);
            return property;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get property: ");
            return "";
        }
    }


    public static boolean setSettingProperty(String key, String value) {
        if (deviceManagerClass == null || deviceManager == null) return false;
        try {
            Method setSettingPropertyMethod = deviceManagerClass.getMethod(
                    "setSettingProperty", String.class, String.class);
            boolean set = (boolean) setSettingPropertyMethod.invoke(
                    deviceManager, key,value);
            return set;
        } catch (Throwable e) {
            Log.e(TAG, "Failed to set property: ");
        }
        return false;
    }
}
