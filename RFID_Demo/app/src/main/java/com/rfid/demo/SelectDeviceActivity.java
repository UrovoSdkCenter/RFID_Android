package com.rfid.demo;

import android.content.Intent;
import android.os.Bundle;

import com.rfid.base.SelectDeviceBaseActivity;
import com.ubx.usdk.log.UlogManager;

public class SelectDeviceActivity extends SelectDeviceBaseActivity {



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}


	@Override
	protected void toMachine() {
		Intent intent2 = new Intent().setClass(SelectDeviceActivity.this, SplashActivity.class);
		intent2.putExtra("pin",toPinDevice);
		startActivity(intent2);
	}

	@Override
	protected void toBle() {
		Intent intent2 = new Intent().setClass(SelectDeviceActivity.this, BleConnectActivity.class);
		startActivity(intent2);
	}

}
