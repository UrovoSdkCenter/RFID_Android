package com.rfid.demo.utils;

import android.content.Context;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import com.ubx.usdk.RFIDSDKManager;
import com.ubx.usdk.bean.ExVersionInfo;
import com.ubx.usdk.bean.enums.ModuleType;

public class Util {

    public static boolean showWarning(Context context, int resRes) {
        Toast.makeText(context, resRes, Toast.LENGTH_LONG).show();
        return false;
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
                version =  version + " [" + ex10Fw + "]";
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
        if (ex10Ver != -1){
            return ex10Ver;
        }
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
//    public static String toVer(String version) {
//        if (!TextUtils.isEmpty(version)) {
//            int ReaderCode = -1;
//            if (RFIDSDKManager.getInstance().getRfidManager() != null) {
//                ReaderCode = RFIDSDKManager.getInstance().getRfidManager().getReaderType();
//            }
//            String ModuleInfo = version + " (" + Integer.toHexString(ReaderCode) + ")";
//            return ModuleInfo;
//        }
//        return "";
//    }


}
