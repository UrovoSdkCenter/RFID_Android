package com.rfid.base.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.pm.PackageInfoCompat;

import com.ubx.usdk.RFIDSDKManager;
import com.ubx.usdk.bean.ExVersionInfo;
import com.ubx.usdk.bean.enums.InventorySceneMode;
import com.ubx.usdk.bean.enums.ModuleType;
import com.ubx.usdk.bean.enums.ReaderDeviceType;
import com.ubx.usdk.rfid.update.FirmwareManager;
import com.ubx.usdk.rfid.util.RfidErrorConstants;

public class Util {

    public static boolean showWarning(Context context, int resRes) {
        Toast.makeText(context, resRes, Toast.LENGTH_LONG).show();
        return false;
    }
    public static boolean isTvEmpty(TextView textView) {
        String str = textView.getText().toString();
        return str == null || str.equals("");
    }
    public static boolean isEtEmpty(EditText editText) {
        String str = editText.getText().toString();
        return str == null || str.equals("");
    }

    public static boolean isLenLegal(EditText editText) {
        if (isEtEmpty(editText)) return false;
        String str = editText.getText().toString();
        return str != null && str.length() % 2 == 0;
    }

    public static boolean isEtsLegal(EditText[] ets) {
        for (EditText et : ets) {
            if (isLenLegal(et)) return true;
        }
        return false;
    }

    public static boolean isHexNumber(String str) {
        boolean flag = false;
        for (int i = 0; i < str.length(); i++) {
            char cc = str.charAt(i);
            if (cc == '0' || cc == '1' || cc == '2' || cc == '3' || cc == '4'
                    || cc == '5' || cc == '6' || cc == '7' || cc == '8'
                    || cc == '9' || cc == 'A' || cc == 'B' || cc == 'C'
                    || cc == 'D' || cc == 'E' || cc == 'F' || cc == 'a'
                    || cc == 'b' || cc == 'c' || cc == 'c' || cc == 'd'
                    || cc == 'e' || cc == 'f') {
                flag = true;
            } else {
                flag = false;
                break;
            }
        }
        return flag;
    }

    public static String bytesToHexString(byte[] src, int offset, int length) {
        StringBuilder stringBuilder = new StringBuilder("");

        try {
            if (src != null && src.length > 0) {
                for (int i = offset; i < length; ++i) {
                    int v = src[i] & 255;
                    String hv = Integer.toHexString(v);
                    if (hv.length() == 1) {
                        stringBuilder.append(0);
                    }

                    stringBuilder.append(hv);
                }

                return stringBuilder.toString().toUpperCase();
            } else {
                return null;
            }
        } catch (Exception var8) {
            return null;
        }
    }

    public static byte[] hexStringToBytes(String hexString) {
        try {
            if (hexString != null && !hexString.equals("")) {
                hexString = hexString.toUpperCase();
                int length = hexString.length() / 2;
                char[] hexChars = hexString.toCharArray();
                byte[] d = new byte[length];

                for (int i = 0; i < length; ++i) {
                    int pos = i * 2;
                    d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
                }

                return d;
            } else {
                return null;
            }
        } catch (Exception var7) {
            return null;
        }
    }

    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }


    public static String toVer(String version) {
        if (!TextUtils.isEmpty(version)) {
            ModuleType moduleType = null;
            if (RFIDSDKManager.getInstance().getRfidManager() != null) {
                moduleType = RFIDSDKManager.getInstance().getRfidManager().getModuleType();
            }

            String Type = "";
            if (moduleType != null ) {
                Type = moduleType.getDescription();
            }

            String ModuleInfo = "";
            double ex10Fw = getEx10Ver();
            if (ex10Fw!=-1){
                version =  version + "\n[" + ex10Fw + "]";
            }
            if (!TextUtils.isEmpty(Type)) {
                ModuleInfo = version + " (" + Type + ")";
            } else {
                ModuleInfo = version;
            }
            return ModuleInfo;
        }
        return "";
    }
    private static double ex10Ver = -1;
    public static double getEx10Ver() {
//        if (ex10Ver != -1){
//            return ex10Ver;
//        }
        ExVersionInfo exVersionInfo = RFIDSDKManager.getInstance().getRfidManager().getEx10Version();
        if (exVersionInfo!=null && exVersionInfo.code == 0){
            try {
                ex10Ver = Double.parseDouble(exVersionInfo.versionInfo);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ex10Ver;
    }

    /**
     * 获取当前应用的 versionName (例如: "1.0.0")
     */
    public static String getVersionName(Context context) {
        if (context == null) return "";

        try {
            PackageManager packageManager = context.getPackageManager();
            // 获取当前应用的包名
            String packageName = context.getPackageName();

            // 获取 PackageInfo，0 表示获取基本信息
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);

            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 获取当前应用的 versionCode (兼容 Android 12+ 的 Long 类型)
     */
    public static long getVersionCode(Context context) {
        if (context == null) return 0;

        try {
            PackageManager packageManager = context.getPackageManager();
            String packageName = context.getPackageName();
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);

            // 使用 PackageInfoCompat 自动处理 Android 12+ (API 31+) 的 Long 类型变更
            return PackageInfoCompat.getLongVersionCode(packageInfo);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return 0;
        }
    }

}
