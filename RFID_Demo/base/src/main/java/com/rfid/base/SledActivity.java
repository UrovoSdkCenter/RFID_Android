package com.rfid.base;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;

import com.rfid.base.custom.EnhancedItemSelectedListener;
import com.rfid.base.databinding.ActivitySledBinding;
import com.rfid.base.utils.SpinnerHelper;
import com.rfid.base.utils.ToastUtils;
import com.rfid.base.utils.ViewHelper;
import com.ubx.usdk.RFIDSDKManager;
import com.ubx.usdk.bean.enums.ReaderDeviceType;
import com.ubx.usdk.io.GripDeviceManager;
import com.ubx.usdk.io.scan.BarcodeCallback;
import com.ubx.usdk.rfid.util.RfidErrorConstants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class SledActivity extends BaseActivity implements OnClickListener {
	private static final String TAG = SledActivity.class.getSimpleName();
	
	private ActivitySledBinding binding;
	
	// 异步处理相关 Asynchronous processing related
	private ExecutorService executorService;
	private Handler mainHandler;
	
	// 加载状态管理  Loading state management
	private boolean isLoading = false;
	private AtomicInteger loadingProgress = new AtomicInteger(0);
	private static final int TOTAL_PARAMS = 10; // 总共需要获取的参数数量  The total number of parameters that need to be obtained

	// Beeper volume constants
	private static final int BEEPER_VOLUME_OFF = 0;
	private static final int BEEPER_VOLUME_LOW = 1;
	private static final int BEEPER_VOLUME_HIGH = 2;

	private String[] strPowerOffline = new String[361];
	private String[] strSleepTime = new String[361];
	// 标志位：是否正在加载设置（用于避免触发监听器回调）
	//Flag: Indicates whether the settings are currently being loaded (used to prevent triggering listener callbacks)
	private boolean isLoadingSettings = false;

	private Context mContext;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		mContext = this.getApplicationContext();
		binding = ActivitySledBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		// 初始化异步处理   Initialize asynchronous processing
		executorService = Executors.newCachedThreadPool();
		mainHandler = new Handler(Looper.getMainLooper());

		initView();
		setupListeners();
		listenerSledScannerResult();
	}

	private void listenerSledScannerResult(){
		GripDeviceManager.getInstance().registerBarcodeCallback(new BarcodeCallback() {
			@Override
			public void onResult(byte[] barcode) {
				binding.tvScannerResult.setText(new String(barcode));
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();

		Log.e(TAG, "onResume: "+GripDeviceManager.getInstance().isConnect()+" , "+RFIDSDKManager.getInstance().getRfidManager().getReaderDeviceType() );
		if (RFIDSDKManager.getInstance().getRfidManager().getReaderDeviceType() == ReaderDeviceType.BLE_DEVICE){
			binding.cardBeeperVolume.setVisibility(View.VISIBLE);
			binding.llScanner.setVisibility(View.VISIBLE);
		}else {
			binding.cardBeeperVolume.setVisibility(View.GONE);
			binding.llScanner.setVisibility(View.GONE);
		}
		// 自动加载所有设备信息   Automatically load all device information
		refreshAllDeviceInfo();
	}

	private void initView() {
		ViewHelper.setOnClickListener(this,
				binding.btRefreshAll,
				binding.btReadScanSN,
				binding.btReadDeviceVersion,
				binding.btReadDeviceSN,
				binding.btReadRfidSN,
				binding.btReadVerBle,
				binding.btReadVerMcu,
				binding.btReadVerRfid,
				binding.btStartDecode,
				binding.btStopDecode);
	}

	@Override
	public void onClick(View view) {
		try {
			if (view == binding.btRefreshAll) {
				refreshAllDeviceInfo();
			} else if (view == binding.btReadScanSN) {
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
			} else if (view == binding.btStartDecode) {
				startDecodeSled();
			} else if (view == binding.btStopDecode) {
				stopDecodeSled();
			}
		} catch (Exception ex) {
		}
	}

	private void startDecodeSled(){
		int ret = GripDeviceManager.getInstance().startScanBarcode(true);
		if (ret == RfidErrorConstants.SUCCESS) {
			ToastUtils.show(getString(R.string.start_success));
		} else {
			ToastUtils.show(getString(R.string.start_failed));
		}

	}
	private void stopDecodeSled(){
		int ret = GripDeviceManager.getInstance().startScanBarcode(false);
		if (ret == RfidErrorConstants.SUCCESS) {
			ToastUtils.show(getString(R.string.stop_success));
		} else {
			ToastUtils.show(getString(R.string.stop_failed));
		}
	}

	private void getScanSN() {
		String scanSN = GripDeviceManager.getInstance().getScanSN();
		if (!TextUtils.isEmpty(scanSN)) {
			binding.txtScanSn.setText(scanSN);
			ToastUtils.show(getString(R.string.get_success));
		} else {
			binding.txtScanSn.setText(getResources().getString(R.string.get_failed));
			ToastUtils.show(getString(R.string.get_failed));
		}
	}

	private void getScanVersion() {
		String scanVersion = GripDeviceManager.getInstance().getVersionScan();
		if (!TextUtils.isEmpty(scanVersion)) {
			binding.txtScanVersion.setText(scanVersion);
			ToastUtils.show(getString(R.string.get_success));
		} else {
			binding.txtScanVersion.setText(getResources().getString(R.string.get_failed));
			ToastUtils.show(getString(R.string.get_failed));
		}
	}


	private void getDeviceSN() {
		String deviceSN = GripDeviceManager.getInstance().getDeviceSN();
		if (!TextUtils.isEmpty(deviceSN)) {
			binding.txtDeviceSn.setText(deviceSN);
			ToastUtils.show(getString(R.string.get_success));
		} else {
			binding.txtDeviceSn.setText(getResources().getString(R.string.get_failed));
			ToastUtils.show(getString(R.string.get_failed));
		}
	}

	private void getRfidSN() {
		String rfidSN = RFIDSDKManager.getInstance().getRfidManager().getDeviceId();
		if (!TextUtils.isEmpty(rfidSN)) {
			binding.txtRfidSn.setText(rfidSN);
			ToastUtils.show(getString(R.string.get_success));
		} else {
			binding.txtRfidSn.setText(getResources().getString(R.string.get_failed));
			ToastUtils.show(getString(R.string.get_failed));
		}
	}

	private void getVersionBLE() {
		String verBle = GripDeviceManager.getInstance().getVersionBLE();
		if (!TextUtils.isEmpty(verBle)) {
			binding.txtVerBle.setText(verBle);
			ToastUtils.show(getString(R.string.get_success));
		} else {
			binding.txtVerBle.setText(getResources().getString(R.string.get_failed));
			ToastUtils.show(getString(R.string.get_failed));
		}
	}

	private void getVersionMcu() {
		String verMcu = GripDeviceManager.getInstance().getVersionSystem();
		if (!TextUtils.isEmpty(verMcu)) {
			binding.txtVerMcu.setText(verMcu);
			ToastUtils.show(getString(R.string.get_success));
		} else {
			binding.txtVerMcu.setText(getResources().getString(R.string.get_failed));
			ToastUtils.show(getString(R.string.get_failed));
		}
	}

	private void getVersionRfid() {
		String verRfid = RFIDSDKManager.getInstance().getRfidManager().getFirmwareVersion();
		if (!TextUtils.isEmpty(verRfid)) {
			binding.txtVerRfid.setText(verRfid);
			ToastUtils.show(getString(R.string.get_success));
		} else {
			binding.txtVerRfid.setText(getResources().getString(R.string.get_failed));
			ToastUtils.show(getString(R.string.get_failed));
		}
	}

	/**
	 * 刷新所有设备信息
	 * Refresh all device information
	 */
	private void refreshAllDeviceInfo() {
		if (isLoading) {
			return; // 防止重复加载  Prevent duplicate loading
		}

		isLoading = true;
		loadingProgress.set(0);
		
		// 显示加载状态   Display loading status
		mainHandler.post(new Runnable() {
			@Override
			public void run() {
				binding.progressLoading.setVisibility(View.VISIBLE);
				binding.progressLoading.setProgress(0);
				binding.btRefreshAll.setEnabled(false);
				ToastUtils.show(getString(R.string.loading_device_info));
			}
		});

		// 异步获取所有参数   Asynchronously obtain all parameters
		executorService.execute(new Runnable() {
			@Override
			public void run() {
				try {
					// 设置加载标志位，避免触发监听器回调  Set the loading flag to avoid triggering the listener callback.
					isLoadingSettings = true;
					// 获取各项参数  Get all the parameters
					getScanSNAsync();
					getScanVersionAsync();
					getDeviceSNAsync();
					getRfidSNAsync();
					getVersionBLEAsync();
					getVersionMcuAsync();
					getVersionRfidAsync();
					loadPowerOffTime();
					loadSleepTime();

					if (RFIDSDKManager.getInstance().getRfidManager().getReaderDeviceType() == ReaderDeviceType.BLE_DEVICE){
						// 4. 加载蜂鸣器音量设置(Load the volume setting for the buzzer)
						loadBeeperVolumeSetting();
					}
					isLoadingSettings = false;
					// 完成加载  Complete loading
					mainHandler.post(new Runnable() {
						@Override
						public void run() {
							finishLoading(true);
						}
					});
				} catch (Exception e) {
					mainHandler.post(new Runnable() {
						@Override
						public void run() {
							finishLoading(false);
						}
					});
				}
			}
		});
	}

	/**
	 * 完成加载状态处理
	 * Complete loading status processing
	 */
	private void finishLoading(boolean success) {
		isLoading = false;
		binding.progressLoading.setVisibility(View.GONE);
		binding.btRefreshAll.setEnabled(true);
		
		// 更新最后更新时间    Update Last Update Time
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
		String currentTime = sdf.format(new Date());
		binding.tvLastUpdateTime.setText(getString(R.string.last_update_time, currentTime));
		
		// 显示结果  Display results
		if (success) {
			ToastUtils.show(getString(R.string.refresh_success));
		} else {
			ToastUtils.show(getString(R.string.refresh_failed));
		}
	}

	/**
	 * 更新加载进度
	 * Update loading progress
	 */
	private void updateProgress() {
		int progress = loadingProgress.incrementAndGet();
		int percentage = (progress * 100) / TOTAL_PARAMS;
		
		mainHandler.post(new Runnable() {
			@Override
			public void run() {
				binding.progressLoading.setProgress(percentage);
			}
		});
	}

	// 异步版本的获取方法
	//The method for obtaining the asynchronous version
	private void getScanSNAsync() {
		try {
			String scanSN = GripDeviceManager.getInstance().getScanSN();
			mainHandler.post(new Runnable() {
				@Override
				public void run() {
					if (!TextUtils.isEmpty(scanSN)) {
						binding.txtScanSn.setText(scanSN);
					} else {
						binding.txtScanSn.setText(getResources().getString(R.string.get_failed));
					}
				}
			});
		} finally {
			updateProgress();
		}
	}

	private void getScanVersionAsync() {
		try {
			String scanVersion = GripDeviceManager.getInstance().getVersionScan();
			mainHandler.post(new Runnable() {
				@Override
				public void run() {
					if (!TextUtils.isEmpty(scanVersion)) {
						binding.txtScanVersion.setText(scanVersion);
					} else {
						binding.txtScanVersion.setText(getResources().getString(R.string.get_failed));
					}
				}
			});
		} finally {
			updateProgress();
		}
	}

	private void getDeviceSNAsync() {
		try {
			String deviceSN = GripDeviceManager.getInstance().getDeviceSN();
			mainHandler.post(new Runnable() {
				@Override
				public void run() {
					if (!TextUtils.isEmpty(deviceSN)) {
						binding.txtDeviceSn.setText(deviceSN);
					} else {
						binding.txtDeviceSn.setText(getResources().getString(R.string.get_failed));
					}
				}
			});
		} finally {
			updateProgress();
		}
	}

	private void getRfidSNAsync() {
		try {
			String rfidSN = RFIDSDKManager.getInstance().getRfidManager().getDeviceId();
			mainHandler.post(new Runnable() {
				@Override
				public void run() {
					if (!TextUtils.isEmpty(rfidSN)) {
						binding.txtRfidSn.setText(rfidSN);
					} else {
						binding.txtRfidSn.setText(getResources().getString(R.string.get_failed));
					}
				}
			});
		} finally {
			updateProgress();
		}
	}

	private void getVersionBLEAsync() {
		try {
			String verBle = GripDeviceManager.getInstance().getVersionBLE();
			mainHandler.post(new Runnable() {
				@Override
				public void run() {
					if (!TextUtils.isEmpty(verBle)) {
						binding.txtVerBle.setText(verBle);
					} else {
						binding.txtVerBle.setText(getResources().getString(R.string.get_failed));
					}
				}
			});
		} finally {
			updateProgress();
		}
	}

	private void getVersionMcuAsync() {
		try {
			String verMcu = GripDeviceManager.getInstance().getVersionSystem();
			mainHandler.post(new Runnable() {
				@Override
				public void run() {
					if (!TextUtils.isEmpty(verMcu)) {
						binding.txtVerMcu.setText(verMcu);
					} else {
						binding.txtVerMcu.setText(getResources().getString(R.string.get_failed));
					}
				}
			});
		} finally {
			updateProgress();
		}
	}

	private void getVersionRfidAsync() {
		try {
			String verRfid = RFIDSDKManager.getInstance().getRfidManager().getFirmwareVersion();
			mainHandler.post(new Runnable() {
				@Override
				public void run() {
					if (!TextUtils.isEmpty(verRfid)) {
						binding.txtVerRfid.setText(verRfid);
					} else {
						binding.txtVerRfid.setText(getResources().getString(R.string.get_failed));
					}
				}
			});
		} finally {
			updateProgress();
		}
	}

	/**
	 * 加载自动休眠时间   Load automatic sleep time
	 * @return 是否加载成功   Did the loading succeed
	 */
	private boolean loadSleepTime() {
		try {
			int time = GripDeviceManager.getInstance().getSleepTime();
			if (time >= 0) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						try {
							binding.sleepSpinner.setSelection((time / 10) - 1);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
				return true;
			} else {
				Log.w(TAG, ">>time is " + time);
				return false;
			}
		} catch (Exception e) {
			Log.e(TAG, ">>Failed to loadSleepTime: " + e.getMessage());
			return false;
		}
	}

	/**
	 * 加载Sled自动关机时间
	 * Load the automatic shutdown time for the Sled
	 * @return 是否加载成功  Did the loading succeed
	 */
	private boolean loadPowerOffTime() {
		try {
			int time = GripDeviceManager.getInstance().getPowerOffTime();
			if (time >= 0) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						try {
							binding.powerOffSpinner.setSelection((time / 10) - 1);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
				return true;
			} else {
				Log.w(TAG, ">>time is " + time);
				return false;
			}
		} catch (Exception e) {
			Log.e(TAG, ">>Failed to loadPowerOffTime: " + e.getMessage());
			return false;
		}
	}

	/**
	 * 加载蜂鸣器音量设置
	 * 根据设备连接状态显示或隐藏蜂鸣器音量卡片
	 * Load the buzzer volume settings
	 * Display or hide the buzzer volume card according to the device connection status
	 */
	private void loadBeeperVolumeSetting() {
		try {
			if (GripDeviceManager.getInstance().isConnect()) {
				int beepRange = GripDeviceManager.getInstance().getBeepRange();
				int volume = convertBeepRangeToVolume(beepRange);

				runOnUiThread(() -> {
					binding.cardBeeperVolume.setVisibility(View.VISIBLE);
					updateBeeperVolumeUI(volume);
				});

				Log.d(TAG, ">>Beeper volume loaded: " + volume + " (beepRange: " + beepRange + ")");
			} else {
				runOnUiThread(() ->
						binding.cardBeeperVolume.setVisibility(View.GONE)
				);
			}
		} catch (Exception e) {
			Log.e(TAG, ">>Failed to load beeper volume: " + e.getMessage());
			runOnUiThread(() ->
					binding.cardBeeperVolume.setVisibility(View.GONE)
			);
		}
	}

	/**
	 * 将 BeepRange 值转换为音量常量  Convert the BeepRange value to a volume constant
	 * @param beepRange BeepRange 值（0=关闭, 1=低音量, 其他=高音量）  BeepRange value (0 = off, 1 = low volume, others = high volume)
	 * @return 音量常量   Volume constant
	 */
	private int convertBeepRangeToVolume(int beepRange) {
		switch (beepRange) {
			case 0:
				return BEEPER_VOLUME_OFF;
			case 1:
				return BEEPER_VOLUME_LOW;
			default:
				return BEEPER_VOLUME_HIGH;
		}
	}

	/**
	 * 更新蜂鸣器音量 UI
	 * Update the UI for the buzzer volume
	 * @param volume 音量值  Volume level
	 */
	private void updateBeeperVolumeUI(int volume) {
		switch (volume) {
			case BEEPER_VOLUME_OFF:
				binding.rbSoundOff.setChecked(true);
				break;
			case BEEPER_VOLUME_LOW:
				binding.rbLowVolume.setChecked(true);
				break;
			case BEEPER_VOLUME_HIGH:
				binding.rbHighVolume.setChecked(true);
				break;
			default:
				binding.rbHighVolume.setChecked(true);
				break;
		}
	}

	private void setupListeners() {
		// 关机时间（Shutdown time） Spinner (1-360)
		for (int index = 0; index < 361; index++) {
			if (index == 0){
				strPowerOffline[index] = getString(R.string.always_on);
			}else {
				strPowerOffline[index] = String.valueOf(index);
			}

		}

		for (int index = 0; index < 361; index++) {
			if (index == 0){
				strSleepTime[index] = getString(R.string.keep_awake);
			}else {
				strSleepTime[index] = String.valueOf(index);
			}

		}

		int defIndex = 0;
		//PowerOffline time Spinner
		SpinnerHelper.setupSpinner(this, binding.powerOffSpinner, strPowerOffline, defIndex);
		//  SleepTime Spinner
		SpinnerHelper.setupSpinner(this, binding.sleepSpinner, strSleepTime, defIndex);

		// RFG91 shutdown time
		binding.powerOffSpinner.setOnItemSelectedListener(new EnhancedItemSelectedListener() {
			@Override
			public void onItemSelected(int position) {
				// 如果正在加载设置，忽略回调
				//If the settings are being loaded, ignore the callback.
				if (isLoadingSettings) {
					Log.d(TAG, ">>powerOffSpinner: isLoadingSettings");
					return;
				}

				int fCmdRet = GripDeviceManager.getInstance().setPowerOffTime( position  * 10);

				if (fCmdRet == 0) {
					ToastUtils.show(getString(R.string.set_success));
				} else {
					ToastUtils.show(getString(R.string.set_failed) + "  " + fCmdRet);
				}
			}
		});

		// RFG91 Sleep time
		binding.sleepSpinner.setOnItemSelectedListener(new EnhancedItemSelectedListener() {
			@Override
			public void onItemSelected(int position) {
				// 如果正在加载设置，忽略回调
				if (isLoadingSettings) {
					Log.d(TAG, ">>sleepSpinner: isLoadingSettings");
					return;
				}

				int fCmdRet = GripDeviceManager.getInstance().setSleepTime(  position   * 10);
				Log.e(TAG, "onItemSelected-sleepSpinner " + fCmdRet + "    position : " + position);
				if (fCmdRet == 0) {
					ToastUtils.show(getString(R.string.set_success));
				} else {
					ToastUtils.show(getString(R.string.set_failed) + "  " + fCmdRet);
				}
			}
		});

		// Beeper volume RadioGroup listener
		binding.rgBeeperVolume.setOnCheckedChangeListener((group, checkedId) -> {
			// 如果正在加载设置，忽略回调
			if (isLoadingSettings) {
				Log.d(TAG, ">>rgBeeperVolume: isLoadingSettings");
				return;
			}
			handleBeeperVolumeChange(checkedId);
		});

	}

	// ============================================================================
	// Beeper volume operation
	// ============================================================================

	/**
	 * Handle beeper volume change event
	 */
	private void handleBeeperVolumeChange(int checkedId) {
		int volume;

		if (checkedId == R.id.rb_sound_off) {
			volume = BEEPER_VOLUME_OFF;
		} else if (checkedId == R.id.rb_low_volume) {
			volume = BEEPER_VOLUME_LOW;
		} else if (checkedId == R.id.rb_high_volume) {
			volume = BEEPER_VOLUME_HIGH;
		} else {
			return;
		}

		if (!GripDeviceManager.getInstance().isConnect()) {
			ToastUtils.show(getString(R.string.please_first_connect_sled));
			return;
		}


		// Apply the volume setting
		int ret = applyBeeperVolume(volume);
		if (ret == RfidErrorConstants.SUCCESS) {
			// Show feedback
			ToastUtils.show(getString(R.string.set_success));
		} else {
			ToastUtils.show(getString(R.string.set_failed));
		}


		Log.d(TAG, ">>Beeper volume changed to: " + volume);
	}


	/**
	 * Apply beeper volume setting to the system
	 */
	private int applyBeeperVolume(int volume) {
		int ret = -1;
		try {
			switch (volume) {
				case BEEPER_VOLUME_OFF:
					// Turn off beeper sound
					ret = GripDeviceManager.getInstance().setBeepRange(0);
					Log.d(TAG, ">>Beeper volume set to OFF");
					break;
				case BEEPER_VOLUME_LOW:
					ret = GripDeviceManager.getInstance().setBeepRange(1);
					Log.d(TAG, ">>Beeper volume set to LOW (1)");
					break;
				case BEEPER_VOLUME_HIGH:
					ret = GripDeviceManager.getInstance().setBeepRange(10);
					Log.d(TAG, ">>Beeper volume set to HIGH (10)");
					break;
				default:
					ret = RfidErrorConstants.PARAM_ERROR;
					Log.e(TAG, ">>Invalid beeper volume value: " + volume);
					break;
			}
		} catch (Exception e) {
			Log.e(TAG, ">>Failed to apply beeper volume: " + e.getMessage());
			e.printStackTrace();
		}
		return ret;
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (executorService != null && !executorService.isShutdown()) {
			executorService.shutdown();
		}
	}
}
