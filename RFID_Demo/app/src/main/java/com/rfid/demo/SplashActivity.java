package com.rfid.demo;

import android.content.Intent;

import com.rfid.base.SplashBaseActivity;

public class SplashActivity extends SplashBaseActivity {
    @Override
    public void toMain() {
        Intent intent2 = new Intent().setClass(SplashActivity.this, MainActivity.class);
        startActivity(intent2);
    }
}
