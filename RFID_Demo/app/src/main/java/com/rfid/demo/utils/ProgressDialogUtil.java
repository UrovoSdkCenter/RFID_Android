package com.rfid.demo.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.demo.rfid.R;

import java.util.Iterator;


public class ProgressDialogUtil {
    private static final int START_DIALOG = 0;
    private static final int STOP_DIALOG = 2;
    private static final int UPDATE_DIALOG = 1;
    private static Context context;
    private static AlertDialog dialog;
    private static Handler handler = new Handler(Looper.getMainLooper()) { // from class: com.ex.admin.ble.utils.1

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            try {
                int i = message.what;
                if (i == 0) {
                    String str = (String) message.obj;
                    if (dialog == null) {
                        init(str);
                        isTouchDismiss(false);
                        return;
                    } else {
                        stopLoad();
                        startLoad(context, str);
                        return;
                    }
                }
                if (i != 1) {
                    if (i == 2 && dialog != null) {
                        dialog.dismiss();
                        dialog.cancel();
                        dialog = null;
                        title = null;
                        return;
                    }
                    return;
                }
                String str2 = (String) message.obj;
                if (!TextUtils.isEmpty(str2)) {
                    title.setText(str2);
                } else {
                    title.setText("");
                }
            } catch (Exception e) {
                e.getMessage();
            }
        }
    };
    private static TextView title;


    public static void init(String str) {
        Context context2;
        if  (isBackground(context) || (context2 = context) == null) {
            return;
        }
        View inflate = LayoutInflater.from(context2).inflate(R.layout.loading, (ViewGroup) null);
        dialog = new AlertDialog.Builder(context, com.rfid.base.R.style.dialog).create();
        dialog.setCancelable(mCancelable);
        dialog.setCanceledOnTouchOutside(mCanceledOnTouchOutside);
        dialog.show();
        title = (TextView) inflate.findViewById(R.id.loading_title);
        title.setText(str + "");
        Window window = dialog.getWindow();
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.width = -2;
        attributes.height = -2;
        attributes.gravity = Gravity.CENTER;
        window.setAttributes(attributes);
        dialog.getWindow().setContentView(inflate);
    }

    public static void startLoad(Context context2, String str) {
        context = context2;
        if ( isBackground(context)) {
            return;
        }
        Message message = new Message();
        message.what = 0;
        message.obj = str;
        handler.sendMessage(message);
    }

    public static void UpdateMsg(String str) {
        Message message = new Message();
        message.what = 1;
        message.obj = str;
        handler.sendMessage(message);
    }
    private static boolean mCancelable = true;
    public static void openCancelable(boolean cancelable) {
        mCancelable = cancelable;
        if (dialog != null) {
            dialog.setCancelable(mCancelable);
        }
    }

    private static boolean mCanceledOnTouchOutside = true;
    public static void isTouchDismiss(boolean canceledOnTouchOutside) {
        mCanceledOnTouchOutside = canceledOnTouchOutside;
        if (dialog != null) {
            dialog.setCanceledOnTouchOutside(mCanceledOnTouchOutside);
        }
    }

    public static void stopLoad() {
        handler.sendEmptyMessage(2);
    }
    public static boolean isBackground(Context context) {
        Iterator<ActivityManager.RunningAppProcessInfo> it = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getRunningAppProcesses().iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            ActivityManager.RunningAppProcessInfo next = it.next();
            if (next.processName.equals(context.getPackageName())) {
                if (next.importance == 400) {
                    return true;
                }
            }
        }
        return false;
    }
}
