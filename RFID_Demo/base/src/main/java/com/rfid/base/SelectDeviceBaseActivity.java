package com.rfid.base;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.databinding.ViewDataBinding;

import com.rfid.base.databinding.ActivitySelectDeviceBinding;
import com.rfid.base.utils.DeviceManagerUtils;
import com.rfid.base.utils.ViewHelper;

public abstract class SelectDeviceBaseActivity extends BaseActivity {

	/** 子类通过 initRootView() 赋值，类型为各模块自己的 Binding */
	protected ViewDataBinding binding;


	private boolean isPhone = false;
	private boolean isSupportPIN = false;

	protected boolean toPinDevice = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initRootView();
		String ProjectName = DeviceManagerUtils.getSettingProperty("pwv.project");
		String SN = DeviceManagerUtils.getDeviceId();
		Log.d("TAG", "onCreate:   SN : "+SN+"  ProjectName : "+ProjectName);
		if (TextUtils.isEmpty(ProjectName)){
			isPhone = true;
		}
		if (!TextUtils.isEmpty(ProjectName) && ProjectName.toUpperCase().contains("SQ610")){
			isSupportPIN = true;
		}


		CardView cardMachine = binding.getRoot().findViewById(R.id.card_machine);
		CardView cardPin    = binding.getRoot().findViewById(R.id.card_pin);
		CardView cardBle    = binding.getRoot().findViewById(R.id.card_ble);

		if (isSupportPIN){
			cardPin.setVisibility(View.VISIBLE);
		}else {
			cardPin.setVisibility(View.GONE);
		}

		// 批量设置点击监听器
		ViewHelper.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (view == cardMachine) {
					toPinDevice = false;
					toMachine();
				} else if (view == cardPin) {
					toPinDevice = true;
					toMachine();
				} else if (view == cardBle) {
					toBle();
				}
			}
		}, cardMachine, cardPin, cardBle);


		if (isPhone){
			isPhoneToBLE();
			return;
		}
	}

	protected void isPhoneToBLE(){
		toBle();
		finish();
	}

	protected void initRootView(){
		binding = ActivitySelectDeviceBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
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

	protected void toMachine() {
		Intent intent2 = new Intent().setClass(SelectDeviceBaseActivity.this, SplashBaseActivity.class);
		intent2.putExtra("pin",toPinDevice);
		startActivity(intent2);
	}


	protected void toBle() {
		Intent intent2 = new Intent().setClass(SelectDeviceBaseActivity.this, BleConnectBaseActivity.class);
		startActivity(intent2);
	}

}
