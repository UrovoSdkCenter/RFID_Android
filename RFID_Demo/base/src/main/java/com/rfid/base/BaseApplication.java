package com.rfid.base;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.rfid.base.utils.CrashHandler;
import com.rfid.base.utils.SPUtils;
import com.rfid.base.utils.ToastUtils;


public class BaseApplication extends Application {
    private static Context mContext;
    private String TAG = BaseApplication.class.getSimpleName();


    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate() ");
        mContext = this;
        SPUtils.initContext(mContext);
        ToastUtils.init(mContext);

        CrashHandler.getInstance().init(this);

    }



    public static Context getContext() {
        return mContext;
    }


    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.i(TAG, "onTerminate()");

    }

}
