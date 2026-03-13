package com.rfid.demo;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;

import com.rfid.demo.databinding.ActivitySettingBinding;
import com.rfid.demo.utils.MsgShow;
import com.rfid.demo.utils.SpinnerHelper;
import com.rfid.demo.utils.ToastUtils;
import com.rfid.demo.utils.ViewHelper;
import com.ubx.usdk.RFIDSDKManager;
import com.ubx.usdk.bean.enums.FrequencyRegion;
import com.ubx.usdk.bean.enums.InventorySceneMode;
import com.ubx.usdk.bean.enums.QueryMemBank;
import com.ubx.usdk.bean.enums.RfidProfile;
import com.ubx.usdk.bean.enums.Target;
import com.ubx.usdk.rfid.util.RfidErrorConstants;
import com.ubx.usdk.util.log.LogUtils;

import java.util.ArrayList;
import java.util.List;

public class SettingActivity extends Activity implements OnClickListener {
	
	private static final String TAG = "SettingActivity";
	
	// 使用 DataBinding
	private ActivitySettingBinding binding;
	
	private String[] strBand = new String[12];
	private String[] strmaxFrm = null; 
	private String[] strminFrm = null;
	private String[] strProfile = new String[12];
	private String[] strRange = new String[109];
	private String[] strBaudRate = new String[2];

	private List<RfidProfile> profileList ;

	private  List<FrequencyRegion> brandList ;
	
	private int ReaderType = -1;
	private int ModuleType = -1; // -1:other chips 3:E310 5:E510 7:E710
	private int maxPower = 30;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// 使用 DataBinding
		binding = ActivitySettingBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		ModuleType = RFIDSDKManager.getInstance().getRfidManager().getEx10Version().type;
		Log.i(TAG, "ModuleType : " + ModuleType);

		try {
			initView();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		initPowerAdapter();
	}

	private void initView() throws Throwable {
		// 初始化基本配置
		initBasicConfig();
		
		// 批量设置点击监听器
		setupClickListeners();
		
		// 初始化所有Spinner
		setupSpinners();
		
		// 设置默认值
		binding.tvRuntime.setText(String.format("%d", ScanActivity.timeout));
	}

	/**
	 * 初始化基本配置
	 */
	private void initBasicConfig() {
		strBaudRate[0] = "115200bps";
		strBaudRate[1] = "230400bps";

		// 初始化频段数组
		brandList = RFIDSDKManager.getInstance().getRfidManager().getSupportFrequencyBandList();
		Log.e(TAG, "initModuleConfig: brandList.size(): " +brandList.size());
		if (!brandList.isEmpty()) {
			List<String> frequencyString = new ArrayList<>();
			for (int i = 0; i < brandList.size(); i++) {
				FrequencyRegion frequencyRegion = brandList.get(i);
				String result = String.format("%s",frequencyRegion.getRegionName());
				frequencyString.add(result);
			}
			brandList.add(FrequencyRegion.FreshBand);
			brandList.add(FrequencyRegion.JAPANLBT1);
			brandList.add(FrequencyRegion.JAPANLBT2);

			strBand = frequencyString.toArray(new String[0]);
		}

		// 初始化Profile数组
		profileList = RFIDSDKManager.getInstance().getRfidManager().getSupportProfileList();
		Log.e(TAG, "initModuleConfig: profileList.size(): " +profileList.size());
		if (!profileList.isEmpty()) {
			List<String> profileString = new ArrayList<>();
			for (int i = 0; i < profileList.size(); i++) {
				RfidProfile rfidProfile = profileList.get(i);
				String result = String.format("%s: BLF:%.2f Miller:%d Tari:%.2f PIE:%.2f",
						rfidProfile.value(),
						rfidProfile.getBLFKhz(),
						rfidProfile.getMiller(),
						rfidProfile.getTariUs(),
						rfidProfile.getPie());
				profileString.add(result);
			}
			strProfile = profileString.toArray(new String[0]);
			binding.lineProfile.setVisibility(View.VISIBLE);
		}  else {
			binding.lineProfile.setVisibility(View.GONE);
		}

		// 初始化范围数组
		for (int index = 0; index <= 108; index++) {
			strRange[index] = String.valueOf(index);
		}
	}

	/**
	 * 批量设置点击监听器
	 */
	private void setupClickListeners() {
		// 使用 ViewHelper 批量设置点击监听器
		ViewHelper.setOnClickListener(this,
			// 频段相关
			binding.proSetting, binding.proRead,
			// 功率相关
			binding.btSetPower, binding.btGetPower,
			// Target相关
			binding.btSetAB, binding.btGetAB,
			// 区域相关
			binding.areaSetting, binding.areaRead,
			// Profile相关
			binding.btSetProfile, binding.btGetProfile,
			// Range相关
			binding.btGetRange, binding.btSetDelay,
			// Q值相关
			binding.btSetQV, binding.btGetQV,
			// Session相关
			binding.btSetSS, binding.btGetSS,
			// 能量模式相关
			binding.btSetEnergyMode, binding.btGetEnergyMode,
			// 超时相关
			binding.btnTimeout,
			// 波特率相关
			binding.btSetBdRate,binding.btGetBdRate
		);
	}

	/**
	 * 初始化所有Spinner
	 */
	private void setupSpinners() {
		// 功率Spinner在onResume中初始化

		// Profile Spinner
		SpinnerHelper.setupSpinner(this, binding.profSpinner, strProfile, 0);

		int defBrandIndex = 0;
		// 频段Spinner
		SpinnerHelper.setupSpinner(this, binding.bandSpinner, strBand, defBrandIndex);
		try {
			SetFre(brandList.get(defBrandIndex));
		} catch (Throwable e) {
			e.printStackTrace();
		}
		setupBandSpinnerListener();

		// Q值Spinner
		SpinnerHelper.setupSpinner(this, binding.qvalueSpinner, R.array.men_q, 6);

		// Target Spinner
		SpinnerHelper.setupSpinner(this, binding.abSpinner, R.array.ab_target, Target.TARGET_AB);

		// Session Spinner
		SpinnerHelper.setupSpinner(this, binding.sessionSpinner, R.array.men_s, 1);

		// TID地址和长度Spinner
		SpinnerHelper.setupSpinner(this, binding.tidptrSpinner, R.array.men_tid, 0);
		SpinnerHelper.setupSpinner(this, binding.tidlenSpinner, R.array.men_tid, 6);

		// 查询模式Spinner
		binding.spinnerInventoryMode.setSelection(0, false);
		setupQueryModeSpinnerListener();
		ViewHelper.setEnabled(false, binding.tidptrSpinner, binding.tidlenSpinner);

		// 盘存模式Spinner
		ReaderType = RFIDSDKManager.getInstance().getRfidManager().getReaderType();
		InventorySceneMode inventorySceneMode = RFIDSDKManager.getInstance()
			.getRfidManager().getInventorySceneMode();
		SpinnerHelper.setupSpinner(this, binding.inventoryModeSpinner, 
			R.array.en_sp_inv_mode, inventorySceneMode.getValue());
		setVisibilityParamterUI(inventorySceneMode == InventorySceneMode.CUSTOM_MODE);

		// 范围Spinner
		SpinnerHelper.setupSpinner(this, binding.rangeSpinner, strRange, 0);

		// 波特率Spinner
		SpinnerHelper.setupSpinner(this, binding.baudSpinner, strBaudRate, 0);
	}

	/**
	 * 设置频段Spinner监听器
	 */
	private void setupBandSpinnerListener() {
		binding.bandSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> view, View arg1, int position, long arg3) {
				try {
					view.setVisibility(View.VISIBLE);
					FrequencyRegion frequencyRegion = getBandByPosition(position);
					if(frequencyRegion!=null) {
						SetFre(frequencyRegion);
					}
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
	}


	/**
	 * 根据位置获取频段值
	 */
	private FrequencyRegion getBandByPosition(int position) {

		if (brandList!=null && !brandList.isEmpty()){
			try {
				return brandList.get(position);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * 设置查询模式Spinner监听器
	 */
	private void setupQueryModeSpinnerListener() {
		binding.spinnerInventoryMode.setOnItemSelectedListener(
			new AdapterView.OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
					Log.i(TAG, "onItemSelected: position : " + position);
					QueryMemBank queryMemBank = QueryMemBank.fromOrdinal(position);
					boolean enabled = !(queryMemBank == QueryMemBank.EPC
						|| queryMemBank == QueryMemBank.FASTTID
						|| queryMemBank == QueryMemBank.BID);
					ViewHelper.setEnabled(enabled, binding.tidptrSpinner, binding.tidlenSpinner);
				}

				@Override
				public void onNothingSelected(AdapterView<?> parent) {
				}
			}
		);
	}


	private void setVisibilityParamterUI(boolean Visible) {
		binding.llInventoryParameters.setVisibility(Visible ? View.VISIBLE : View.GONE);
	}

	private void initPowerAdapter() {
		maxPower = RFIDSDKManager.getInstance().getRfidManager().getSupportMaxOutputPower();
		Log.e(TAG, "initView: maxPower : " + maxPower);
		
		String[] pwArray = SpinnerHelper.createRangeArray(0, maxPower);
		SpinnerHelper.setupSpinner(this, binding.powerSpinner, pwArray, 
			maxPower < 30 ? 24 : maxPower);
	}

	@Override
	public void onClick(View view) {
		try {
			int id = view.getId();
			
			if (id == R.id.pro_setting) {
				handleSetFrequency();
			} else if (id == R.id.pro_read) {
				ReadInformation();
			} else if (id == R.id.bt_SetDelay) {
				handleSetRange();
			} else if (id == R.id.bt_GetRange) {
				getRangeControll();
			} else if (id == R.id.bt_SetAB) {
				handleSetTarget();
			} else if (id == R.id.bt_GetAB) {
				handleGetTarget();
			} else if (id == R.id.bt_SetPower) {
				handleSetPower();
			} else if (id == R.id.bt_GetPower) {
				handleGetPower();
			} else if (id == R.id.area_setting) {
				handleSetArea();
			} else if (id == R.id.area_read) {
				handleGetArea();
			} else if (id == R.id.bt_GetProfile) {
				ReadProfile();
			} else if (id == R.id.bt_SetProfile) {
				handleSetProfile();
			} else if (id == R.id.bt_SetSS) {
				handleSetSession();
			} else if (id == R.id.bt_GetSS) {
				handleGetSession();
			} else if (id == R.id.bt_SetQV) {
				handleSetQValue();
			} else if (id == R.id.bt_GetQV) {
				handleGetQValue();
			} else if (id == R.id.bt_get_energy_mode) {
				handleGetEnergyMode();
			} else if (id == R.id.bt_set_energy_mode) {
				handleSetEnergyMode();
			} else if (id == R.id.btn_timeout) {
				setInvTimeOut();
			} else if (id == R.id.bt_SetBdRate) {
				handleSetBaudRate();
			}else if (id == R.id.bt_GetBdRate) {
				handleGetBaudRate();
			}
		} catch (Throwable ex) {
			ex.printStackTrace();
		}
	}

	// ==================== 处理方法 ====================

	private void handleSetFrequency() {
		int fband = binding.bandSpinner.getSelectedItemPosition();
		FrequencyRegion frequencyRegion = getBandByPosition(fband);
		int MinFre = binding.minSpinner.getSelectedItemPosition();
		int MaxFre = binding.maxSpinner.getSelectedItemPosition();

		LogUtils.v(TAG, "frequencyRegion : " + frequencyRegion.getRegion() + "   "
				+ frequencyRegion.getMinChannelIndex() + "   " + frequencyRegion.getMaxChannelIndex());

		int result = RFIDSDKManager.getInstance().getRfidManager().setFrequencyRegion(frequencyRegion, MinFre, MaxFre);

		if (result != 0) {
			MsgShow.writelogErr(getString(R.string.frequent_error) + " " + result, binding.paramResult);
		} else {
			MsgShow.writelogSuc(getString(R.string.set_success), binding.paramResult);
		}
	}

	private void handleSetRange() {
		int index = binding.rangeSpinner.getSelectedItemPosition();
		int fCmdRet = RFIDSDKManager.getInstance().getRfidManager().setInventoryRssiLimit(index);
		
		if (fCmdRet == 0) {
			MsgShow.writelogSuc(getString(R.string.set_success), binding.paramResult);
		} else {
			MsgShow.writelogErr(getString(R.string.set_failed), binding.paramResult);
		}
	}

	private void handleSetTarget() {
		int target = binding.abSpinner.getSelectedItemPosition();
		Log.v(TAG, "target : " + target);
		
		int fCmdRet = RFIDSDKManager.getInstance().getRfidManager().setInventoryWithTarget(target);
		
		if (fCmdRet == 0) {
			MsgShow.writelogSuc(getString(R.string.set_success), binding.paramResult);
		} else {
			MsgShow.writelogErr(getString(R.string.set_failed), binding.paramResult);
		}
	}

	private void handleGetTarget() {
		int fCmdRet = RFIDSDKManager.getInstance().getRfidManager().getInventoryWithTarget();
		
		if (fCmdRet >= 0 && fCmdRet < 3) {
			binding.abSpinner.setSelection(fCmdRet);
			MsgShow.writelogSuc(getString(R.string.get_success), binding.paramResult);
		} else {
			MsgShow.writelogErr(getString(R.string.get_failed) + " " + fCmdRet, binding.paramResult);
		}
	}

	private void handleSetPower() {
		int Power = binding.powerSpinner.getSelectedItemPosition();
		int result = RFIDSDKManager.getInstance().getRfidManager().setOutputPower(Power);
		
		if (result != 0) {
			MsgShow.writelogErr(getString(R.string.power_error), binding.paramResult);
		} else {
			MsgShow.writelogSuc(getString(R.string.set_success), binding.paramResult);
		}
	}

	private void handleGetPower() {
		int power = RFIDSDKManager.getInstance().getRfidManager().getOutputPower();
		
		if (power >= 0) {
			if (power > maxPower) {
				binding.powerSpinner.setSelection(maxPower, true);
				ToastUtils.show("Power " + maxPower);
			} else {
				binding.powerSpinner.setSelection(power, true);
			}
			MsgShow.writelogSuc(getString(R.string.get_success), binding.paramResult);
		} else {
			MsgShow.writelogErr(getString(R.string.get_failed), binding.paramResult);
		}
	}

	private void handleSetArea() {
		int length = binding.tidlenSpinner.getSelectedItemPosition();
		int startAddress = binding.tidptrSpinner.getSelectedItemPosition();
		int area = binding.spinnerInventoryMode.getSelectedItemPosition();
		
		int result = RFIDSDKManager.getInstance().getRfidManager()
			.setQueryMemoryBank(QueryMemBank.fromOrdinal(area), startAddress, length);
		
		if (result != 0) {
			MsgShow.writelogErr(getString(R.string.set_failed), binding.paramResult);
		} else {
			MsgShow.writelogSuc(getString(R.string.set_success), binding.paramResult);
		}
		setInvTimeOut();
	}

	private void handleGetArea() {
		QueryMemBank queryMemBank = RFIDSDKManager.getInstance().getRfidManager().getQueryMemoryBank();
		
		if (queryMemBank != null) {
			binding.spinnerInventoryMode.setSelection(queryMemBank.ordinal(), true);
			binding.tidlenSpinner.setSelection(queryMemBank.getReadLength(), true);
			binding.tidptrSpinner.setSelection(queryMemBank.getStartAddress(), true);
			MsgShow.writelogSuc(getString(R.string.get_success), binding.paramResult);
		} else {
			MsgShow.writelogErr(getString(R.string.get_failed), binding.paramResult);
		}
	}

	private void handleSetProfile() {
		int index = binding.profSpinner.getSelectedItemPosition();
		RfidProfile Profile = getProfileValueByIndex(index);
		int result = RfidErrorConstants.PARAM_ERROR;
		if (Profile != null) {
			result = RFIDSDKManager.getInstance().getRfidManager().setProfile(Profile);
		}
		if (result == 0) {
			MsgShow.writelogSuc(getString(R.string.set_success), binding.paramResult);
		} else {
			MsgShow.writelogErr(getString(R.string.set_failed) + " " + result, binding.paramResult);
		}
	}

	private RfidProfile getProfileValueByIndex(int index) {
		if (profileList!=null && !profileList.isEmpty()){
			try {
				return profileList.get(index);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private void handleSetSession() {
		int index = binding.sessionSpinner.getSelectedItemPosition();
		int fCmdRet = RFIDSDKManager.getInstance().getRfidManager().setInventoryWithSession(index);
		
		if (fCmdRet == 0) {
			MsgShow.writelogSuc(getString(R.string.set_success), binding.paramResult);
		} else {
			MsgShow.writelogErr(getString(R.string.set_failed), binding.paramResult);
		}
	}

	private void handleGetSession() {
		int value = RFIDSDKManager.getInstance().getRfidManager().getInventoryWithSession();
		
		if (value >= 0) {
			binding.sessionSpinner.setSelection(value, true);
			MsgShow.writelogSuc(getString(R.string.get_success), binding.paramResult);
		} else {
			MsgShow.writelogErr(getString(R.string.get_failed) + "  " + value, binding.paramResult);
		}
	}

	private void handleSetQValue() {
		int index = binding.qvalueSpinner.getSelectedItemPosition();
		int fCmdRet = RFIDSDKManager.getInstance().getRfidManager().setInventoryWithStartQvalue(index);
		
		if (fCmdRet == 0) {
			MsgShow.writelogSuc(getString(R.string.set_success), binding.paramResult);
		} else {
			MsgShow.writelogErr(getString(R.string.set_failed), binding.paramResult);
		}
	}

	private void handleGetQValue() {
		int value = RFIDSDKManager.getInstance().getRfidManager().getInventoryWithStartQvalue();
		
		if (value >= 0) {
			binding.qvalueSpinner.setSelection(value, true);
			MsgShow.writelogSuc(getString(R.string.get_success), binding.paramResult);
		} else {
			MsgShow.writelogErr(getString(R.string.get_failed) + "  " + value, binding.paramResult);
		}
	}

	private void handleGetEnergyMode() {
		InventorySceneMode inventorySceneMode = RFIDSDKManager.getInstance()
			.getRfidManager().getInventorySceneMode();
		
		if (inventorySceneMode != null) {
			binding.inventoryModeSpinner.setSelection(inventorySceneMode.getValue(), true);
			setVisibilityParamterUI(inventorySceneMode == InventorySceneMode.CUSTOM_MODE);
			MsgShow.writelogSuc(getString(R.string.get_success), binding.paramResult);
		} else {
			MsgShow.writelogErr(getString(R.string.get_failed) + "  ", binding.paramResult);
		}
	}

	private void handleSetEnergyMode() {
		int index = binding.inventoryModeSpinner.getSelectedItemPosition();
		int ret = RFIDSDKManager.getInstance().getRfidManager()
			.setInventorySceneMode(InventorySceneMode.fromValue(index));
		
		if (ret == 0) {
			setVisibilityParamterUI(InventorySceneMode.fromValue(index) == InventorySceneMode.CUSTOM_MODE);
			MsgShow.writelogSuc(getString(R.string.set_success), binding.paramResult);
		} else {
			MsgShow.writelogErr(getString(R.string.set_failed), binding.paramResult);
		}
	}

	private void handleSetBaudRate() {
		int index = binding.baudSpinner.getSelectedItemPosition();
		int baudRate = (index == 0) ? 115200 : 230400;
		
		int result = RFIDSDKManager.getInstance().getRfidManager().setBaudRate(baudRate);
		
		if (result == 0) {
			MsgShow.writelogSuc(getString(R.string.set_success), binding.paramResult);
		} else {
			MsgShow.writelogErr(getString(R.string.set_failed), binding.paramResult);
		}
	}
	private void handleGetBaudRate() {
		int baudRate = RFIDSDKManager.getInstance().getRfidManager().getBaudRate();

		if (baudRate >= 9600) {
			if (baudRate == 115200){
				binding.baudSpinner.setSelection(0,false);
			}else if (baudRate == 230400){
				binding.baudSpinner.setSelection(1,false);
			}
			MsgShow.writelogSuc(getString(R.string.get_success) + " " + baudRate, binding.paramResult);
		} else {
			MsgShow.writelogErr(getString(R.string.get_failed) + " " + baudRate, binding.paramResult);
		}
	}
	// ==================== 其他方法 ====================

	private void setInvTimeOut() {
		String edTime = binding.tvRuntime.getText().toString();
		
		if (TextUtils.isEmpty(edTime)) {
			MsgShow.writelogErr(getString(R.string.set_failed), binding.paramResult);
			return;
		}
		
		int timeout = 0;
		try {
			timeout = Integer.valueOf(edTime);
		} catch (Exception e) {
			e.printStackTrace();
			timeout = -1;
		}
		
		if (timeout >= 0) {
			ScanActivity.timeout = timeout;
			MsgShow.writelogSuc(getString(R.string.set_success), binding.paramResult);
		} else {
			binding.tvRuntime.setText(String.valueOf(ScanActivity.timeout));
			MsgShow.writelogErr(getString(R.string.set_failed), binding.paramResult);
		}
	}

	private void ReadInformation() throws Throwable {
		FrequencyRegion frequencyRegion = RFIDSDKManager.getInstance().getRfidManager().getFrequencyRegion();

		if (frequencyRegion != null) {
			LogUtils.v(TAG, "frequencyRegion : " + frequencyRegion.getRegion() + "   "
					+ frequencyRegion.getMinChannelIndex() + "   " + frequencyRegion.getMaxChannelIndex());

			int index = getBrandIndexByValue(frequencyRegion);
			if (index >=0){
				binding.bandSpinner.setSelection(index, true);
				binding.minSpinner.setSelection(frequencyRegion.getMinChannelIndex(), true);
				binding.maxSpinner.setSelection(frequencyRegion.getMaxChannelIndex(), true);
				MsgShow.writelogSuc(getString(R.string.get_success), binding.paramResult);
			}else {
				MsgShow.writelogSuc(getString(R.string.get_success)+" "+frequencyRegion.getRegionName(), binding.paramResult);
			}
		} else {
			MsgShow.writelogErr(getString(R.string.get_failed), binding.paramResult);
		}
	}


	private void getRangeControll() throws Throwable {
		int range = RFIDSDKManager.getInstance().getRfidManager().getInventoryRssiLimit();
		
		if (range >= 0) {
			binding.rangeSpinner.setSelection(range, true);
			MsgShow.writelogSuc(getString(R.string.get_success), binding.paramResult);
		} else {
			MsgShow.writelogErr(getString(R.string.get_failed) + " " + range, binding.paramResult);
		}
	}

	private void ReadProfile() throws Throwable {
		RfidProfile profile = RFIDSDKManager.getInstance().getRfidManager().getProfile();

		if (profile != null) {
			Log.d(TAG, "ReadProfile: " + profile.value());
			int index = getProfileIndexByValue(profile);
			if (index >=0){
				binding.profSpinner.setSelection(index, true);
				MsgShow.writelogSuc(getString(R.string.get_success), binding.paramResult);
			}else {
				MsgShow.writelogSuc(getString(R.string.get_success)+" "+profile.name(), binding.paramResult);
			}
		} else {
			MsgShow.writelogErr(getString(R.string.get_failed), binding.paramResult);
		}
	}

	private int getProfileIndexByValue(RfidProfile rfidProfile) {

		if (profileList!=null && !profileList.isEmpty()){
			try {
				for (int i = 0; i < profileList.size(); i++) {
					if (profileList.get(i) == rfidProfile){
						return i;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return -1;
	}
	private int getBrandIndexByValue(FrequencyRegion frequencyRegion) {

		if (brandList!=null && !brandList.isEmpty()){
			try {
				for (int i = 0; i < brandList.size(); i++) {
					if (brandList.get(i) == frequencyRegion){
						return i;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return -1;
	}

	private void SetFre(FrequencyRegion frequencyRegion) throws Throwable {
		strmaxFrm = new String[frequencyRegion.getChannelCount()];
		strminFrm = new String[frequencyRegion.getChannelCount()];

		for (int i = 0; i < frequencyRegion.getChannelCount(); i++) {
			float values = calculateFrequency(frequencyRegion, i);
			String temp = values + "MHz";
			strminFrm[i] = temp;
			strmaxFrm[i] = temp;
		}

		SpinnerHelper.updateSpinnerData(this, binding.maxSpinner, strmaxFrm);
		binding.maxSpinner.setSelection(frequencyRegion.getChannelCount() - 1, false);

		SpinnerHelper.updateSpinnerData(this, binding.minSpinner, strminFrm);
		binding.minSpinner.setSelection(0, false);
	}

	private float calculateFrequency(FrequencyRegion frequencyRegion, int index) {

		if ( frequencyRegion == FrequencyRegion.BRAZIL) {
			if (index <= 9) {
				return formatFloatSmart(902.75f + index * 0.5f);
			} else {
				return formatFloatSmart(910.25f + index * 0.5f);
			}
		}else if ( frequencyRegion == FrequencyRegion.INDONESIA) {
			if (index <= 0) {
				return formatFloatSmart(917.25f + index * 0.5f);
			} else {
				return formatFloatSmart(919.75f + index * 0.5f);
			}
		}else if ( frequencyRegion == FrequencyRegion.FreshBand) {
			if (index <= 24) {
				return formatFloatSmart(902.75f + index * 0.5f);
			} else {
				return formatFloatSmart(879.25f + index * 1.5f);
			}
		} else if (frequencyRegion == FrequencyRegion.JAPANLBT1 ) {
			if (index <= 2) {
				return formatFloatSmart(916.8f + index * 1.2f);
			} else {
				return formatFloatSmart(919.8f + index * 0.2f);
			}
		} else if (frequencyRegion == FrequencyRegion.JAPANLBT2) {
			if (index <= 3) {
				return formatFloatSmart(916.8f + index * 1.2f);
			} else {
				return formatFloatSmart(919.8f + index * 0.2f);
			}
		} else {
			return (float) (frequencyRegion.getStartFreqMHz() + index * frequencyRegion.getStepSizeMHz());
		}
	}

	/**
	 * 智能格式化浮点数
	 * @param originalNumber 原始浮点数
	 * @return 格式化后的浮点数，规则：若只有一位小数则保留一位，超过一位则保留两位（四舍五入）
	 */
	public static float formatFloatSmart(float originalNumber) {
		String numberStr = String.valueOf(originalNumber);
		int decimalIndex = numberStr.indexOf('.');
		int decimalPlaces = (decimalIndex != -1) ? numberStr.length() - decimalIndex - 1 : 0;

		float result;
		if (decimalPlaces <= 1) {
			result = Math.round(originalNumber * 10) / 10.0f;
		} else {
			result = Math.round(originalNumber * 100) / 100.0f;
		}
		return result;
	}
}
