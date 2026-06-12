package com.rfid.base;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.rfid.base.databinding.ActivityConfigBinding;
import com.rfid.base.utils.DialogUtils;
import com.rfid.base.utils.SpinnerHelper;
import com.rfid.base.utils.ToastUtils;
import com.rfid.base.utils.ViewHelper;
import com.ubx.usdk.RFIDSDKManager;
import com.ubx.usdk.bean.RfidParameter;
import com.ubx.usdk.bean.enums.FrequencyRegion;
import com.ubx.usdk.bean.enums.InventorySceneMode;
import com.ubx.usdk.bean.enums.QueryMemBank;
import com.ubx.usdk.bean.enums.ReaderDeviceType;
import com.ubx.usdk.bean.enums.RfidProfile;
import com.ubx.usdk.bean.enums.RssiUnitType;
import com.ubx.usdk.bean.enums.Target;
import com.ubx.usdk.log.UlogManager;
import com.ubx.usdk.rfid.util.RfidErrorConstants;

import java.util.ArrayList;
import java.util.List;

public class ConfigActivity extends BaseActivity implements OnClickListener,TabLifecycleListener {

    protected static final String TAG = "ConfigActivity";

    // 使用 DataBinding
    protected ActivityConfigBinding binding;

    private String[] strBand = new String[12];
    private String[] strmaxFrm = null;
    private String[] strminFrm = null;
    protected String[] strProfile = new String[12];
    private String[] strRange = new String[109];
    private String[] strMemBankLength = new String[361];
    private String[] strBaudRate = new String[2];

    private List<RfidProfile> profileList;

    /** 当前选中的 Inventory Mode 位置（替代 Spinner.getSelectedItemPosition） */
    protected int mInventoryModePosition = 0;

    private List<FrequencyRegion> brandList;

    protected FrequencyRegion frequencyRegion;

    private int ReaderType = -1;
    private int ModuleType = -1; // -1:other chips 3:E310 5:E510 7:E710
    private int maxPower = 30;
    protected Context mContext;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityConfigBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mContext = this.getApplicationContext();

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

    }

    @Override
    public void onTabSelected() {

        Log.d(TAG, "onTabSelected: ");
        setupSwitchListener();
        initPowerAdapter();
        loadDeviceSettingsAsync();
    }

    @Override
    public void onTabUnselected() {

    }

    /**
     * 异步加载设备设置（功率、频段、盘点模式、蜂鸣器音量）
     * 最多重试3次，直到所有设置加载成功
     * Asynchronous loading of device settings (power, frequency band, inventory mode, buzzer volume)
     * Maximum of 3 retries, until all settings are loaded successfully
     */
    private void loadDeviceSettingsAsync() {
        new Thread(() -> {

            for (int attempt = 1; attempt <= 3; attempt++) {
                Log.d(TAG, ">>Loading device settings, attempt: " + attempt);

                boolean allSuccess = true;

                // 1. 加载功率设置   Load power setting
                allSuccess &= handleGetPower(false);

                // 2. 加载频段设置  Load frequency band settings
                allSuccess &= readFrequency(false);

                // 3. 加载盘点模式设置   Load inventory mode settings
                allSuccess &= handleGetMemoryBank(false);

                // 4. 加载盘点模式   Load inventory mode
                allSuccess &= handleGetEnergyMode(false);
                // 5. 加载盘点参数   Load inventory parameters
                allSuccess &= getInvertoryParm();

                if (allSuccess) {
                    Log.d(TAG, ">>All device settings loaded successfully");
                    break;
                }

                // 如果不是最后一次尝试，等待一小段时间再重试
                //If it's not the final attempt, wait for a short period of time before trying again.
                if (attempt < 3) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Log.e(TAG, ">>Thread interrupted: " + e.getMessage());
                        break;
                    }
                }
            }
        }).start();
    }

    private void initView() throws Throwable {
        initBasicConfig();

        setupClickListeners();

        setupSpinners();

        setupSwitchListener();

        binding.tvRuntime.setText(String.format("%d", InventoryActivity.timeout));

    }

    /**
     * Switch 监听方法
     * Switch listening method
     */
    private void setupSwitchListener() {

        boolean isEnable = RFIDSDKManager.getInstance().getRfidManager().getRssiUnitType() == RssiUnitType.PERCENTAGE;
        binding.switchRssi.setChecked(isEnable);

        binding.switchRssi.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    enableRssiRange();
                } else {
                    disableRssiRange();
                }
            }
        });


        boolean isEnableLog = UlogManager.isEnableLog();
        binding.switchLog.setChecked(isEnableLog);

        binding.switchLog.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (checkFilePermiss(ConfigActivity.this)){
                        enableLog();
                    }else {
                        binding.switchLog.setChecked(false);
                    }
                } else {
                    UlogManager.enableLog(false);
                }
            }
        });
    }
    private void enableLog(){
        DialogUtils.getInstance().showDoubleButtonDialog(ConfigActivity.this, getResources().getString(R.string.tip),getString(R.string.enabling_the_log_will_record_your_tag_operations), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                UlogManager.enableLog(true);
            }
        }, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                binding.switchLog.setChecked(false);
            }
        });

    }
    /**
     * Initialize basic configuration
     */
    private void initBasicConfig() {
        strBaudRate[0] = "115200bps";
        strBaudRate[1] = "230400bps";

        // Initialize the frequency band array
        brandList = RFIDSDKManager.getInstance().getRfidManager().getSupportFrequencyBandList();
        Log.e(TAG, "initModuleConfig: brandList.size(): " + brandList.size());
        if (!brandList.isEmpty()) {
            List<String> frequencyString = new ArrayList<>();
            for (int i = 0; i < brandList.size(); i++) {
                FrequencyRegion frequencyRegion = brandList.get(i);
                String result = String.format("%s", frequencyRegion.getRegionName());
                frequencyString.add(result);
            }
//            brandList.add(FrequencyRegion.FreshBand);
            brandList.add(FrequencyRegion.JAPANLBT1);
            brandList.add(FrequencyRegion.JAPANLBT2);

            strBand = frequencyString.toArray(new String[0]);
        }


        // Initialize the range array
        for (int index = 0; index <= 108; index++) {
            strRange[index] = String.valueOf(index);
        }

        // Initialize the MemBank Length array
        for (int index = 0; index <= 360; index++) {
            strMemBankLength[index] = String.valueOf(index);
        }


    }

    public boolean readProfileParm(FrequencyRegion frequencyRegion) {

        RfidProfile profile = RFIDSDKManager.getInstance().getRfidManager().getProfile();
        // 初始化Profile数组
        profileList = RFIDSDKManager.getInstance().getRfidManager().getSupportProfileList();
        Log.e(TAG, "initModuleConfig: profileList.size(): " + profileList.size() );
        if (!profileList.isEmpty()) {
            List<String> profileString = new ArrayList<>();
            for (int i = 0; i < profileList.size(); i++) {
                RfidProfile rfidProfile = profileList.get(i);
                String result = String.format("%s %s %.0fk Tari:%.1f PIE:%.0f M:%d",
                        rfidProfile.value(),
                        rfidProfile.getModulation(),
                        rfidProfile.getBLFKhz(),
                        rfidProfile.getTariUs(),
                        rfidProfile.getPie(),
                        rfidProfile.getMiller());
                profileString.add(result);
            }
            strProfile = profileString.toArray(new String[0]);

            // Profile Spinner
            SpinnerHelper.setupSpinner(this, binding.profSpinner, strProfile, 0);
            if (profile != null) {
                for (int i = 0; i < profileList.size(); i++) {
                    RfidProfile rfidProfile = profileList.get(i);
                    if (rfidProfile == profile) {
                        updateByProfile(profile.value());
                        Log.e(TAG, "initViewCustom: index : " + profile.name() + "   i : " + i);
                        binding.profSpinner.setSelection(i);
                        return true;
                    }
                }
            }
            binding.lineProfile.setVisibility(View.VISIBLE);
        } else {
            binding.lineProfile.setVisibility(View.GONE);
        }
        return false;
    }



    protected void handleSetProfile() {

        int result = RfidErrorConstants.PARAM_ERROR;
        int profileRes = 0;

        int index = binding.profSpinner.getSelectedItemPosition();
        RfidProfile Profile = getProfileValueByIndex(index);
        if (Profile != null) {
            profileRes = Profile.value();
            result = RFIDSDKManager.getInstance().getRfidManager().setProfile(Profile);
        }
        setProfileResult(result,profileRes);



    }

    protected void setProfileResult(int result, int profileRes) {
        if (result == 0) {
            updateByProfile(profileRes);
            ToastUtils.show(getString(R.string.set_success));
        } else {
            ToastUtils.show(getString(R.string.set_failed) + " " + result);
        }
    }

    protected void updateByProfile(int profile) {

    }

    protected RfidProfile getProfileValueByIndex(int index) {
        if (profileList != null && !profileList.isEmpty()) {
            try {
                return profileList.get(index);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Batch setting of click listener
     */
    protected void setupClickListeners() {
        ViewHelper.setOnClickListener(this,
                // 频段相关  Frequency band
                binding.proSetting, binding.proRead,
                // 功率相关 Power
                binding.btSetPower, binding.btGetPower,
                // Target
                binding.btSetAB, binding.btGetAB,
                //  MemBank
                binding.areaSetting, binding.areaRead,
                // Profile
                binding.btSetProfile, binding.btGetProfile,
                // Range
                binding.btGetRange, binding.btSetRange,
                // Q-Value
                binding.btSetQV, binding.btGetQV,
                // Session
                binding.btSetSS, binding.btGetSS,
                // Inventory mode
                binding.btSetEnergyMode, binding.btGetEnergyMode,
                // Timeout
                binding.btnTimeout,
                // Baud rate
                binding.btSetBdRate, binding.btGetBdRate
        );
    }
    protected void setupSpinners() {

        int defBrandIndex = 0;
        // Frequency band Spinner
        SpinnerHelper.setupSpinner(this, binding.bandSpinner, strBand, defBrandIndex);
        try {
            SetFre(brandList.get(defBrandIndex));
        } catch (Throwable e) {
            e.printStackTrace();
        }
        setupBandSpinnerListener();

        // QValue
        SpinnerHelper.setupSpinner(this, binding.qvalueSpinner, R.array.men_q, 6);

        // Target Spinner
        SpinnerHelper.setupSpinner(this, binding.abSpinner, R.array.ab_target, Target.TARGET_AB);

        // Session Spinner
        SpinnerHelper.setupSpinner(this, binding.sessionSpinner, R.array.men_s, 1);

        // TID地址和长度Spinner
        SpinnerHelper.setupSpinner(this, binding.tidptrSpinner, strMemBankLength, 0);
        SpinnerHelper.setupSpinner(this, binding.tidlenSpinner, R.array.men_tid, 6);

        // 查询模式Spinner  Query mode
        binding.spinnerInventoryMode.setSelection(0, false);
        setupQueryModeSpinnerListener();
        ViewHelper.setEnabled(false, binding.tidptrSpinner, binding.tidlenSpinner);

        //Inventory mode — 自定义弹窗替代 Spinner
        ReaderType = RFIDSDKManager.getInstance().getRfidManager().getReaderType();
        InventorySceneMode inventorySceneMode = RFIDSDKManager.getInstance()
                .getRfidManager().getInventorySceneMode();
        mInventoryModePosition = inventorySceneMode != null ? inventorySceneMode.getValue() : 0;
        updateInventoryModeText(mInventoryModePosition);
        binding.inventoryModeSpinner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInventoryModeDialog();
            }
        });
        setVisibilityParamterUI(inventorySceneMode == InventorySceneMode.CUSTOM_MODE);

        // Range Spinner
        SpinnerHelper.setupSpinner(this, binding.rangeSpinner, strRange, 0);

        // Baud rate Spinner
        SpinnerHelper.setupSpinner(this, binding.baudSpinner, strBaudRate, 0);


    }


    /**
     * Set the frequency band Spinner listener
     */
    private void setupBandSpinnerListener() {
        binding.bandSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> view, View arg1, int position, long arg3) {
                try {
                    view.setVisibility(View.VISIBLE);
                    FrequencyRegion frequencyRegion = getBandByPosition(position);
                    if (frequencyRegion != null) {
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
     * 根据Index获取频段值 Obtain the frequency band value based on the index
     */
    private FrequencyRegion getBandByPosition(int position) {

        if (brandList != null && !brandList.isEmpty()) {
            try {
                return brandList.get(position);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     *Set the query mode Spinner listener
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
        if (!Visible) {
            updateByProfile(-1);
        }
        binding.llInventoryParameters.setVisibility(Visible ? View.VISIBLE : View.GONE);
        binding.rangeSpinner.setEnabled(Visible);
        binding.btSetRange.setEnabled(Visible);
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
                setFrequency();
            } else if (id == R.id.pro_read) {
                readFrequency(true);
            } else if (id == R.id.bt_SetRange) {
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
                handleGetPower(true);
            } else if (id == R.id.area_setting) {
                handleSetMemoryBank();
            } else if (id == R.id.area_read) {
                handleGetMemoryBank(true);
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
                handleGetEnergyMode(true);
            } else if (id == R.id.bt_set_energy_mode) {
                handleSetEnergyMode();
            } else if (id == R.id.btn_timeout) {
                setInvTimeOut();
            } else if (id == R.id.bt_SetBdRate) {
                handleSetBaudRate();
            } else if (id == R.id.bt_GetBdRate) {
                handleGetBaudRate();
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }


    private void setFrequency() {
        int fband = binding.bandSpinner.getSelectedItemPosition();
        FrequencyRegion frequencyRegion1 = getBandByPosition(fband);
        int MinFre = binding.minSpinner.getSelectedItemPosition();
        int MaxFre = binding.maxSpinner.getSelectedItemPosition();

        Log.v(TAG, "frequencyRegion : " + frequencyRegion1.getRegion() + "   "
                + frequencyRegion1.getMinChannelIndex() + "   " + frequencyRegion1.getMaxChannelIndex());

        int result = RFIDSDKManager.getInstance().getRfidManager().setFrequencyRegion(frequencyRegion1, MinFre, MaxFre);

        if (result != 0) {


            ToastUtils.show(getString(R.string.frequent_error) + " " + result);
        } else {
            frequencyRegion = frequencyRegion1;
            readProfileParm(frequencyRegion);
            if (RFIDSDKManager.getInstance().getRfidManager().getReaderDeviceType() == ReaderDeviceType.INTEGRATED){
                initPowerAdapter();
                handleGetPower(false);
            }
            ToastUtils.show(getString(R.string.set_success));
        }
    }

    private void handleSetRange() {
        int index = binding.rangeSpinner.getSelectedItemPosition();
        int fCmdRet = RFIDSDKManager.getInstance().getRfidManager().setInventoryRssiLimit(index);

        if (fCmdRet == 0) {
            ToastUtils.show(getString(R.string.set_success));
        } else {
            ToastUtils.show(getString(R.string.set_failed));
        }
    }

    private void handleSetTarget() {
        int target = binding.abSpinner.getSelectedItemPosition();
        Log.v(TAG, "target : " + target);

        int fCmdRet = RFIDSDKManager.getInstance().getRfidManager().setInventoryWithTarget(target);

        if (fCmdRet == 0) {
            ToastUtils.show(getString(R.string.set_success));
        } else {
            ToastUtils.show(getString(R.string.set_failed));
        }
    }

    private void handleGetTarget() {
        int fCmdRet = RFIDSDKManager.getInstance().getRfidManager().getInventoryWithTarget();

        if (fCmdRet >= 0 && fCmdRet < 3) {
            binding.abSpinner.setSelection(fCmdRet);
            ToastUtils.show(getString(R.string.get_success));
        } else {
            ToastUtils.show(getString(R.string.get_failed) + " " + fCmdRet);
        }
    }

    private void handleSetPower() {
        int Power = binding.powerSpinner.getSelectedItemPosition();
        int result = RFIDSDKManager.getInstance().getRfidManager().setOutputPower(Power);

        if (result != 0) {
            ToastUtils.show(getString(R.string.power_error));
        } else {
            ToastUtils.show(getString(R.string.set_success));
        }
    }

    private boolean handleGetPower(boolean isToast) {
        int power = RFIDSDKManager.getInstance().getRfidManager().getOutputPower();

        if (power >= 0) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (power > maxPower) {
                        binding.powerSpinner.setSelection(maxPower, true);
                    } else {
                        binding.powerSpinner.setSelection(power, true);
                    }
                }
            });
            ToastUtils.show(isToast, getString(R.string.get_success));
            return true;
        } else {
            ToastUtils.show(isToast, getString(R.string.get_failed));
        }
        return false;
    }

    private void handleSetMemoryBank() {
        int length = binding.tidlenSpinner.getSelectedItemPosition();
        int startAddress = binding.tidptrSpinner.getSelectedItemPosition();
        int area = binding.spinnerInventoryMode.getSelectedItemPosition();

        int result = RFIDSDKManager.getInstance().getRfidManager()
                .setQueryMemoryBank(QueryMemBank.fromOrdinal(area), startAddress, length);

        if (result != 0) {
            ToastUtils.show(getString(R.string.set_failed));
        } else {
            ToastUtils.show(getString(R.string.set_success));
        }
    }

    private boolean handleGetMemoryBank(boolean isToast) {
        QueryMemBank queryMemBank = RFIDSDKManager.getInstance().getRfidManager().getQueryMemoryBank();

        if (queryMemBank != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    binding.spinnerInventoryMode.setSelection(queryMemBank.ordinal(), true);
                    binding.tidlenSpinner.setSelection(queryMemBank.getReadLength(), true);
                    binding.tidptrSpinner.setSelection(queryMemBank.getStartAddress(), true);
                    ToastUtils.show(isToast, getString(R.string.get_success));
                }
            });
            return true;
        } else {
            ToastUtils.show(isToast, getString(R.string.get_failed));
        }
        return false;
    }



    private void handleSetSession() {
        int index = binding.sessionSpinner.getSelectedItemPosition();
        int fCmdRet = RFIDSDKManager.getInstance().getRfidManager().setInventoryWithSession(index);

        if (fCmdRet == 0) {
            ToastUtils.show(getString(R.string.set_success));
        } else {
            ToastUtils.show(getString(R.string.set_failed));
        }
    }

    private void handleGetSession() {
        int value = RFIDSDKManager.getInstance().getRfidManager().getInventoryWithSession();

        if (value >= 0) {
            binding.sessionSpinner.setSelection(value, true);
            ToastUtils.show(getString(R.string.get_success));
        } else {
            ToastUtils.show(getString(R.string.get_failed) + "  " + value);
        }
    }

    private void handleSetQValue() {
        int index = binding.qvalueSpinner.getSelectedItemPosition();
        int fCmdRet = RFIDSDKManager.getInstance().getRfidManager().setInventoryWithStartQvalue(index);

        if (fCmdRet == 0) {
            ToastUtils.show(getString(R.string.set_success));
        } else {
            ToastUtils.show(getString(R.string.set_failed));
        }
    }

    private void handleGetQValue() {
        int value = RFIDSDKManager.getInstance().getRfidManager().getInventoryWithStartQvalue();

        if (value >= 0) {
            binding.qvalueSpinner.setSelection(value, true);
            ToastUtils.show(getString(R.string.get_success));
        } else {
            ToastUtils.show(getString(R.string.get_failed) + "  " + value);
        }
    }

    private boolean getInvertoryParm() {
        RfidParameter rfidParameter = RFIDSDKManager.getInstance().getRfidManager().getInventoryParameter();
        if (rfidParameter != null) {
            binding.abSpinner.setSelection(rfidParameter.Target, false);
            binding.sessionSpinner.setSelection(rfidParameter.Session, false);
            binding.qvalueSpinner.setSelection(rfidParameter.QValue, false);
            binding.rangeSpinner.setSelection(rfidParameter.Rssi, false);
            return true;
        }
        return false;

    }

    private boolean handleGetEnergyMode(boolean isToast) {
        InventorySceneMode inventorySceneMode = RFIDSDKManager.getInstance()
                .getRfidManager().getInventorySceneMode();

        if (inventorySceneMode != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mInventoryModePosition = inventorySceneMode.getValue();
                    updateInventoryModeText(mInventoryModePosition);
                    setVisibilityParamterUI(inventorySceneMode == InventorySceneMode.CUSTOM_MODE);
                }
            });
            ToastUtils.show(isToast, getString(R.string.get_success));
            return true;
        } else {
            ToastUtils.show(isToast, getString(R.string.get_failed) + "  ");
        }
        return false;
    }

    private void handleSetEnergyMode() {
        int index = mInventoryModePosition;
        int ret = RFIDSDKManager.getInstance().getRfidManager()
                .setInventorySceneMode(InventorySceneMode.fromValue(index));

        if (ret == 0) {
            setVisibilityParamterUI(InventorySceneMode.fromValue(index) == InventorySceneMode.CUSTOM_MODE);
            ToastUtils.show(getString(R.string.set_success));
            if (InventorySceneMode.fromValue(index) == InventorySceneMode.CUSTOM_MODE) {
                getInvertoryParm();
                readProfileParm(frequencyRegion);
            }
        } else {
            ToastUtils.show(getString(R.string.set_failed));
        }
    }

    /**
     * 更新 inventoryModeSpinner TextView 显示当前选中的模式名称
     */
    private void updateInventoryModeText(int position) {
        String[] titles = getResources().getStringArray(R.array.en_sp_inv_mode);
        if (position >= 0 && position < titles.length) {
            binding.inventoryModeSpinner.setText(titles[position]);
        }
    }

    /**
     * 弹出 Inventory Mode 选择弹窗
     * 每个 item 显示加粗标题 + 小字描述
     */
    private void showInventoryModeDialog() {
        com.rfid.base.widget.InventoryModeDialog.ModeItem[] items =
                new com.rfid.base.widget.InventoryModeDialog.ModeItem[]{
            new com.rfid.base.widget.InventoryModeDialog.ModeItem(
                getString(R.string.inv_mode_battery_life),
                getString(R.string.inv_mode_battery_life_desc)),
            new com.rfid.base.widget.InventoryModeDialog.ModeItem(
                getString(R.string.inv_mode_full_inventory),
                getString(R.string.inv_mode_full_inventory_desc)),
            new com.rfid.base.widget.InventoryModeDialog.ModeItem(
                getString(R.string.inv_mode_rapid_repeat),
                getString(R.string.inv_mode_rapid_repeat_desc)),
            new com.rfid.base.widget.InventoryModeDialog.ModeItem(
                getString(R.string.inv_mode_balanced),
                getString(R.string.inv_mode_balanced_desc)),
            new com.rfid.base.widget.InventoryModeDialog.ModeItem(
                getString(R.string.inv_mode_cycle_count),
                getString(R.string.inv_mode_cycle_count_desc)),
            new com.rfid.base.widget.InventoryModeDialog.ModeItem(
                getString(R.string.inv_mode_max_range),
                getString(R.string.inv_mode_max_range_desc)),
            new com.rfid.base.widget.InventoryModeDialog.ModeItem(
                getString(R.string.inv_mode_custom),
                getString(R.string.inv_mode_custom_desc)),
        };

        new com.rfid.base.widget.InventoryModeDialog(this,
                getString(R.string.inv_mode), items)
                .setSelectedPosition(mInventoryModePosition)
                .setOnItemSelectedListener(new com.rfid.base.widget.InventoryModeDialog.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(int position) {
                        mInventoryModePosition = position;
                        updateInventoryModeText(position);
                        setVisibilityParamterUI(
                                InventorySceneMode.fromValue(position) == InventorySceneMode.CUSTOM_MODE);
                    }
                })
                .show();
    }

    private void handleSetBaudRate() {
        int index = binding.baudSpinner.getSelectedItemPosition();
        int baudRate = (index == 0) ? 115200 : 230400;

        int result = RFIDSDKManager.getInstance().getRfidManager().setBaudRate(baudRate);

        if (result == 0) {
            ToastUtils.show(getString(R.string.set_success));
        } else {
            ToastUtils.show(getString(R.string.set_failed));
        }
    }

    private void handleGetBaudRate() {
        int baudRate = RFIDSDKManager.getInstance().getRfidManager().getBaudRate();

        if (baudRate >= 9600) {
            if (baudRate == 115200) {
                binding.baudSpinner.setSelection(0, false);
            } else if (baudRate == 230400) {
                binding.baudSpinner.setSelection(1, false);
            }
            ToastUtils.show(getString(R.string.get_success) + " " + baudRate);
        } else {
            ToastUtils.show(getString(R.string.get_failed) + " " + baudRate);
        }
    }
    // ==================== 其他方法 ====================

    private void setInvTimeOut() {
        String edTime = binding.tvRuntime.getText().toString();

        if (TextUtils.isEmpty(edTime)) {
            ToastUtils.show(getString(R.string.set_failed));
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
            InventoryActivity.timeout = timeout;
            ToastUtils.show(getString(R.string.set_success));
        } else {
            binding.tvRuntime.setText(String.valueOf(InventoryActivity.timeout));
            ToastUtils.show(getString(R.string.set_failed));
        }
    }

    private boolean readFrequency(boolean isToast) {
        frequencyRegion = RFIDSDKManager.getInstance().getRfidManager().getFrequencyRegion();

        if (frequencyRegion != null) {
            Log.v(TAG, "frequencyRegion : " + frequencyRegion.getRegion() + "   "
                    + frequencyRegion.getMinChannelIndex() + "   " + frequencyRegion.getMaxChannelIndex());

            int index = getBrandIndexByValue(frequencyRegion);
            if (index >= 0) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SetFre(frequencyRegion);
                        readProfileParm(frequencyRegion);
                        if (RFIDSDKManager.getInstance().getRfidManager().getReaderDeviceType() == ReaderDeviceType.INTEGRATED){
                            initPowerAdapter();
                            handleGetPower(false);
                        }
                        binding.bandSpinner.setSelection(index, true);
                        binding.minSpinner.setSelection(frequencyRegion.getMinChannelIndex(), true);
                        binding.maxSpinner.setSelection(frequencyRegion.getMaxChannelIndex(), true);
                        ToastUtils.show(isToast, getString(R.string.get_success));
                    }
                });
            } else {
                ToastUtils.show(isToast, getString(R.string.get_success) + " " + frequencyRegion.getRegionName());
            }
            return true;
        } else {
            ToastUtils.show(isToast, getString(R.string.get_failed));
        }
        return false;
    }


    private void getRangeControll() throws Throwable {
        int range = RFIDSDKManager.getInstance().getRfidManager().getInventoryRssiLimit();

        if (range >= 0) {
            binding.rangeSpinner.setSelection(range, true);
            ToastUtils.show(getString(R.string.get_success));
        } else {
            ToastUtils.show(getString(R.string.get_failed) + " " + range);
        }
    }

    private void ReadProfile() throws Throwable {
        boolean isSet = readProfileParm(frequencyRegion);
        if (isSet) {
            ToastUtils.show(getString(R.string.get_success));
        } else {
            ToastUtils.show(getString(R.string.get_failed));
        }
    }

    private int getBrandIndexByValue(FrequencyRegion frequencyRegion) {

        if (brandList != null && !brandList.isEmpty()) {
            try {
                for (int i = 0; i < brandList.size(); i++) {
                    if (brandList.get(i) == frequencyRegion) {
                        return i;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return -1;
    }

    private void SetFre(FrequencyRegion frequencyRegion) {
        try {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private float calculateFrequency(FrequencyRegion frequencyRegion, int index) {

        if (frequencyRegion == FrequencyRegion.BRAZIL) {
            if (index <= 9) {
                return formatFloatSmart(902.75f + index * 0.5f);
            } else {
                return formatFloatSmart(910.25f + index * 0.5f);
            }
        } else if (frequencyRegion == FrequencyRegion.INDONESIA) {
            if (index <= 0) {
                return formatFloatSmart(917.25f + index * 0.5f);
            } else {
                return formatFloatSmart(919.75f + index * 0.5f);
            }
        } else if (frequencyRegion == FrequencyRegion.FreshBand) {
            if (index <= 24) {
                return formatFloatSmart(902.75f + index * 0.5f);
            } else {
                return formatFloatSmart(879.25f + index * 1.5f);
            }
        } else if (frequencyRegion == FrequencyRegion.JAPANLBT1) {
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
     * 智能格式化浮点数    Intelligent formatting of floating-point numbers
     * @param originalNumber 原始浮点数     originalNumber The original floating-point number
     * @return 格式化后的浮点数，规则：若只有一位小数则保留一位，超过一位则保留两位（四舍五入）
     * @return The formatted floating-point number, following the rule: if there is only one decimal place, keep one digit; if more than one decimal place, keep two digits (rounded to the nearest whole number)
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

    private void enableRssiRange() {
        RFIDSDKManager.getInstance().getRfidManager().setRssiUnitType(RssiUnitType.PERCENTAGE);
    }

    private void disableRssiRange() {
        RFIDSDKManager.getInstance().getRfidManager().setRssiUnitType(RssiUnitType.RELATIVE);
    }


}
