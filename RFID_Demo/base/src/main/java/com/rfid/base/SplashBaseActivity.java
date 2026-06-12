package com.rfid.base;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;


import com.rfid.base.databinding.ActivitySplashBinding;
import com.rfid.base.utils.ViewHelper;
import com.ubx.usdk.RFIDSDKManager;
import com.ubx.usdk.bean.enums.ReaderDeviceType;
import com.ubx.usdk.listener.InitListener;

public class SplashBaseActivity extends BaseActivity {

    protected static String TAG = SplashBaseActivity.class.getSimpleName();
    
    // 使用 DataBinding
    private ActivitySplashBinding binding;
    private boolean isPIN = false;
    protected int connFailCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 使用 DataBinding
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        isPIN = getIntent().getBooleanExtra("pin",false);

        binding.BtConnect.setVisibility(View.GONE);
        
        // 使用 ViewHelper 设置点击监听器
        ViewHelper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initRFID();
            }
        }, binding.BtConnect);

        initRFID();
    }

    private void initRFID() {
        binding.progress.setVisibility(View.VISIBLE);
        binding.textviewShow.setVisibility(View.INVISIBLE);

        initRfid();

//        if (RFIDSDKManager.getInstance().getRfidManager() != null && RFIDSDKManager.getInstance().getRfidManager().isConnected()) {
//            toMain();
//            finish();
//        }
    }

    public void initRfid() {
        RFIDSDKManager.getInstance().init(SplashBaseActivity.this.getApplicationContext(), new InitListener() {
            @Override
            public void onStatus(boolean status) {
                Log.i(TAG, "initRfid()  status : " + status);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {

                        if (status) {
                            connFailCount = 0;
                            int deviceType = RFIDSDKManager.getInstance().getRfidManager().getReaderDeviceType();
                            Log.i(TAG, "initRfid()  deviceType : " + deviceType+"  isPIN : "+isPIN);
                            if (isPIN && deviceType != ReaderDeviceType.PERIPHERAL_UART){
                                initFail( getString(R.string.pin_check_fail));
                                RFIDSDKManager.getInstance().release();
                                return;
                            }else  if (!isPIN && (deviceType != ReaderDeviceType.INTEGRATED && deviceType != ReaderDeviceType.FIXED_INTEGRATED_DEVICE )){
                                initFail( getString(R.string.please_disconnect_pin_device));
                                RFIDSDKManager.getInstance().release();
                                return;
                            }
                            toMain();
                            finish();
                        } else {
                            connFailCount ++;
                            initFail(getString(R.string.openport_failed));
                        }

                        Toast.makeText(SplashBaseActivity.this, " " + (status ? getResources().getString(R.string.rfid_successful_initialization) : getResources().getString(R.string.rfid_initialization_failure)),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    public void initFail(String errMsg){
        binding.textviewShow.setVisibility(View.VISIBLE);
        binding.textviewShow.setText(errMsg);
        binding.textviewShow.setTextColor(getResources().getColor(R.color.red));
        binding.progress.setVisibility(View.INVISIBLE);
        binding.BtConnect.setVisibility(View.VISIBLE);

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * Return to exit program twice
     * 两次返回退出程序
     */
    private long firstTime = 0;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        long secondTime = System.currentTimeMillis();
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (secondTime - firstTime < 2000) {
                System.exit(0);
            } else {
                Toast.makeText(this, getString(R.string.press_again_exit_app), Toast.LENGTH_SHORT).show();
                firstTime = System.currentTimeMillis();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void toMain() {
        Intent intent2 = new Intent().setClass(SplashBaseActivity.this, MainBaseActivity.class);
        startActivity(intent2);
    }

}
