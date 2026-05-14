package com.rfid.demo;

import android.content.Intent;

import com.rfid.base.BleConnectBaseActivity;

public class BleConnectActivity extends BleConnectBaseActivity {

    @Override
    protected void toMain() {
        Intent intent = new Intent(BleConnectActivity.this, MainActivity.class);
        startActivity(intent);
    }
}
