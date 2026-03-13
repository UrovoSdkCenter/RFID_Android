package com.rfid.demo;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;

import com.rfid.demo.databinding.ActivityDeviceInfoBinding;
import com.rfid.demo.utils.MsgShow;
import com.rfid.demo.utils.ViewHelper;
import com.ubx.usdk.RFIDSDKManager;
import com.ubx.usdk.constant.BTKeyEvent;
import com.ubx.usdk.io.GripDeviceManager;
import com.ubx.usdk.io.listener.BatteryGripListener;
import com.ubx.usdk.io.listener.KeyEventListener;

public class DeviceInfoActivity extends Activity implements OnClickListener {
	private static final String TAG = DeviceInfoActivity.class.getSimpleName();
	
	// 使用 DataBinding
	private ActivityDeviceInfoBinding binding;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		// 使用 DataBinding
		binding = ActivityDeviceInfoBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		initView();
	}

	@Override
	protected void onResume() {
		super.onResume();

		GripDeviceManager.getInstance().setKeyEventListener(new KeyEventListener() {
			@Override
			public void event(int keyCode, boolean isDown) {
				String msg = null;
				if (keyCode ==  BTKeyEvent.BT_SCAN) {
					if (isDown) {
						msg = getString(R.string.scan_key_down);
					} else {
						msg = getString(R.string.scan_key_up);
					}
				}
				binding.txtKeyboard.setText("" + msg);
			}
		});
	}

	private void initView() {
		// 批量设置点击监听器
		ViewHelper.setOnClickListener(this, 
				binding.btReadScanSN,
				binding.btReadDeviceVersion,
				binding.btReadDeviceSN,
				binding.btReadRfidSN,
				binding.btReadVerBle,
				binding.btReadVerMcu,
				binding.btReadVerRfid,
				binding.btReadBatteryValue,
				binding.btReadIsChange,
				binding.btMode);

		GripDeviceManager.getInstance().setBatteryGripListener(new BatteryGripListener() {
			@Override
			public void isChange(int change) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						readBatteryIsChange(change);
					}
				});
			}

			@Override
			public void level(int percent) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						readBatteryLevel(percent);
					}
				});
			}
		});
		readBatteryIsChange(GripDeviceManager.getInstance().getIsChange());
		readBatteryLevel(GripDeviceManager.getInstance().getElectricQuantity());
	}

	@Override
	public void onClick(View view) {
		try {
			if (view == binding.btReadScanSN) {
				getScanSN();
			} else if (view == binding.btReadDeviceSN) {
				getDeviceSN();
			} else if (view == binding.btReadRfidSN) {
				getRfidSN();
			} else if (view == binding.btReadVerBle) {
				getVersionBLE();
			} else if (view == binding.btReadVerMcu) {
				getVersionMcu();
			} else if (view == binding.btReadVerRfid) {
				getVersionRfid();
			} else if (view == binding.btReadDeviceVersion) {
				getScanVersion();
			} else if (view == binding.btReadBatteryValue) {
				getBatteryValue();
			} else if (view == binding.btReadIsChange) {
				getIsChange();
			} else if (view == binding.btMode) {
				getMode();
			}
		} catch (Exception ex) {
		}
	}

	private void getMode() {
		int scanMode = GripDeviceManager.getInstance().getScanMode();
		if (scanMode == 0) {
			binding.txtMode.setText(R.string.scan_mode);
		} else if (scanMode == 1) {
			binding.txtMode.setText(R.string.rfid_mode);
		} else if (scanMode == 2) {
			binding.txtMode.setText(R.string.rfid_and_scan_mode);
		}
	}

	private void getScanSN() {
		String scanSN = GripDeviceManager.getInstance().getScanSN();
		if (!TextUtils.isEmpty(scanSN)) {
			binding.txtScanSn.setText(scanSN);
			MsgShow.writelogSuc(getString(R.string.get_success), binding.paramResult);
		} else {
			binding.txtScanSn.setText(getResources().getString(R.string.get_failed));
			MsgShow.writelogErr(getString(R.string.get_failed), binding.paramResult);
		}
	}

	private void getScanVersion() {
		String scanVersion = GripDeviceManager.getInstance().getVersionScan();
		if (!TextUtils.isEmpty(scanVersion)) {
			binding.txtScanVersion.setText(scanVersion);
			MsgShow.writelogSuc(getString(R.string.get_success), binding.paramResult);
		} else {
			binding.txtScanVersion.setText(getResources().getString(R.string.get_failed));
			MsgShow.writelogErr(getString(R.string.get_failed), binding.paramResult);
		}
	}

	private void modeResetFactory() {
		int ret = GripDeviceManager.getInstance().modeResetFactory();
		if (ret == 0) {
			MsgShow.writelogSuc(getString(R.string.get_success), binding.paramResult);
		} else {
			MsgShow.writelogErr(getString(R.string.get_failed) + "  " + ret, binding.paramResult);
		}
	}

	private void getDeviceSN() {
		String deviceSN = GripDeviceManager.getInstance().getDeviceSN();
		if (!TextUtils.isEmpty(deviceSN)) {
			binding.txtDeviceSn.setText(deviceSN);
			MsgShow.writelogSuc(getString(R.string.get_success), binding.paramResult);
		} else {
			binding.txtDeviceSn.setText(getResources().getString(R.string.get_failed));
			MsgShow.writelogErr(getString(R.string.get_failed), binding.paramResult);
		}
	}

	private void getRfidSN() {
		String rfidSN = RFIDSDKManager.getInstance().getRfidManager().getDeviceId();
		if (!TextUtils.isEmpty(rfidSN)) {
			binding.txtRfidSn.setText(rfidSN);
			MsgShow.writelogSuc(getString(R.string.get_success), binding.paramResult);
		} else {
			binding.txtRfidSn.setText(getResources().getString(R.string.get_failed));
			MsgShow.writelogErr(getString(R.string.get_failed), binding.paramResult);
		}
	}

	private void getBatteryValue() {
		int batteryValue = GripDeviceManager.getInstance().getElectricQuantity();
		if (batteryValue >= 0) {
			binding.txtBatteryValue.setText(batteryValue + " %");
			MsgShow.writelogSuc(getString(R.string.get_success), binding.paramResult);
		} else {
			binding.txtBatteryValue.setText(getResources().getString(R.string.get_failed) + "  " + batteryValue);
			MsgShow.writelogErr(getString(R.string.get_failed) + "  " + batteryValue, binding.paramResult);
		}
	}

	private void getIsChange() {
		int isChange = GripDeviceManager.getInstance().getIsChange();
		if (isChange >= 0) {
			binding.txtIsChange.setText(isChange == 1 ? getString(R.string.isChangeing) : getString(R.string.not_change));
			MsgShow.writelogSuc(getString(R.string.get_success), binding.paramResult);
		} else {
			binding.txtIsChange.setText(getResources().getString(R.string.get_failed) + "  " + isChange);
			MsgShow.writelogErr(getString(R.string.get_failed) + "  " + isChange, binding.paramResult);
		}
	}

	private void getVersionBLE() {
		String verBle = GripDeviceManager.getInstance().getVersionBLE();
		if (!TextUtils.isEmpty(verBle)) {
			binding.txtVerBle.setText(verBle);
			MsgShow.writelogSuc(getString(R.string.get_success), binding.paramResult);
		} else {
			binding.txtVerBle.setText(getResources().getString(R.string.get_failed));
			MsgShow.writelogErr(getString(R.string.get_failed), binding.paramResult);
		}
	}

	private void getVersionMcu() {
		String verMcu = GripDeviceManager.getInstance().getVersionSystem();
		if (!TextUtils.isEmpty(verMcu)) {
			binding.txtVerMcu.setText(verMcu);
			MsgShow.writelogSuc(getString(R.string.get_success), binding.paramResult);
		} else {
			binding.txtVerMcu.setText(getResources().getString(R.string.get_failed));
			MsgShow.writelogErr(getString(R.string.get_failed), binding.paramResult);
		}
	}

	private void getVersionRfid() {
		String verRfid = RFIDSDKManager.getInstance().getRfidManager().getFirmwareVersion();
		if (!TextUtils.isEmpty(verRfid)) {
			binding.txtVerRfid.setText(verRfid);
			MsgShow.writelogSuc(getString(R.string.get_success), binding.paramResult);
		} else {
			binding.txtVerRfid.setText(getResources().getString(R.string.get_failed));
			MsgShow.writelogErr(getString(R.string.get_failed), binding.paramResult);
		}
	}

	private void readBatteryIsChange(int change) {
		if (change >= 0 || change <= 1) {
			binding.batteryIschange.setText(getString(R.string.battery_ischange) + (change == 1 ? getString(R.string.ischangeing) : getString(R.string.not_ischange)));
			binding.batteryIschange.setTextColor(change == 1 ? getColor(R.color.main_color) : getColor(R.color.grayslate));
		} else {
			binding.batteryIschange.setText(getString(R.string.battery_ischange) + "Unknown");
			binding.batteryIschange.setTextColor(getColor(R.color.red));
		}
	}

	private void readBatteryLevel(int percent) {
		if (percent >= 0 && percent <= 100) {
			binding.batteryLevel.setText(getString(R.string.battery_level) + percent + "%");
		} else {
			binding.batteryLevel.setText(getString(R.string.battery_level) + "**");
		}
	}
}
