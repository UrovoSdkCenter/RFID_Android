package com.rfid.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Toast;

import com.rfid.base.BaseActivity;
import com.rfid.base.utils.ToastUtils;
import com.rfid.base.utils.ViewHelper;
import com.rfid.demo.utils.Constant;
import com.rfid.demo.utils.ProgressDialogUtil;
import com.rfid.demo.utils.ReadFIleBinOrZip;
import com.rfid.demo.utils.SimpleTimer;
import com.demo.rfid.R;
import com.demo.rfid.databinding.ActivityRfidUpdateBinding;
import com.ubx.usdk.RFIDSDKManager;
import com.ubx.usdk.bean.ExVersionInfo;
import com.ubx.usdk.bean.enums.ReaderDeviceType;
import com.ubx.usdk.constant.UARTInitStatus;
import com.ubx.usdk.io.GripDeviceManager;
import com.ubx.usdk.rfid.RFUpgradeManager;
import com.ubx.usdk.rfid.rfupdate.callback.FirmwareUpgradeCallback;
import com.ubx.usdk.rfid.rfupdate.dialog.DialogRFUpgrade;
import com.ubx.usdk.rfid.rfupdate.dialog.FirmwareConfirmDialog;
import com.ubx.usdk.rfid.rfupdate.model.UpgradeCheckResult;
import com.ubx.usdk.rfid.rfupdate.model.UpgradeInfo;
import com.ubx.usdk.rfid.rfupdate.model.UpgradeStepEvent;
import com.ubx.usdk.util.SharedPreferencesUtils;

import java.io.File;

public class RFIDUpdateActivity extends BaseActivity implements OnClickListener, SimpleTimer.TimerCallback {

    private final static String TAG = "update";

    // 使用 DataBinding
    private ActivityRfidUpdateBinding binding;

    private Context mContext;
    private String mAddress = "";
    private int deviceReaderType = -1;

    private String mPathZipName = "";//ZIP文件名称  ZIP file name
    private String mPathZipPath = "";//ZIP文件路径  ZIP file path

    // 文件选择相关  File selection related
    private static final int REQUEST_CODE_SELECT_FILE = 1001;

    private SimpleTimer simpleTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 使用 DataBinding
        binding = ActivityRfidUpdateBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mContext = this;

        simpleTimer = new SimpleTimer();

        initView();

        if (binding.fwHeaderSection != null) {
            binding.fwHeaderSection.setPadding(
                    binding.fwHeaderSection.getPaddingLeft(),
                    binding.fwHeaderSection.getPaddingTop() + getStatusBarHeight(),
                    binding.fwHeaderSection.getPaddingRight(),
                    binding.fwHeaderSection.getPaddingBottom()
            );
        }

        mAddress = getIntent().getStringExtra("mac");
        if (RFIDSDKManager.getInstance().isConnected()) {

            deviceReaderType = RFIDSDKManager.getInstance().getRfidManager().getReaderDeviceType();

            if (UARTInitStatus.getInitUARTStatus()) {//是串口设备   UART Device
                if (deviceReaderType == ReaderDeviceType.PERIPHERAL_UART) {
                } else {
                    RFIDSDKManager.getInstance().getRfidManager().setBaudRate(115200);
                }
                binding.titleUpdate.setText(getResources().getString(R.string.intg_update));
            } else {//蓝牙设备  Bluetooth Sled
                mAddress = GripDeviceManager.getInstance().getBLEMac();
                binding.titleUpdate.setText(getResources().getString(R.string.ble_hand));
            }
        } else {
            binding.llUart.setVisibility(View.GONE);
        }

        String resStr = getResources().getString(R.string.no_choice_to_firmeare_file);

        mPathZipName = SharedPreferencesUtils.getString(mContext, Constant.KEY_PATH_ZIP_NAME, "");
        binding.tvFilePath.setText(mPathZipName.isEmpty() ? resStr : mPathZipName);

        mPathZipPath = SharedPreferencesUtils.getString(mContext, Constant.KEY_PATH_ZIP_PATH, "");
    }

    @Override
    public void onClick(View view) {
        try {
            if (view == binding.btStartUpdate) {
                simpleTimer.start(this);
                checkUpgrade();
            } else if (view == binding.btnSelectFile) {
                ReadFIleBinOrZip.openFileChooser(RFIDUpdateActivity.this, REQUEST_CODE_SELECT_FILE);
            }
        } catch (Exception ex) {
        }
    }

    // ==================== 升级回调  Upgrade callback ====================
    private FirmwareUpgradeCallback upgradeCallback;

    /**
     *  检测升级
     *  Upgrade of testing
     */
    private void checkUpgrade() {

        if (TextUtils.isEmpty(mPathZipPath)) {
            ToastUtils.show("LocalPath isEmpty.");
            showTvResult("LocalPath isEmpty.");
            return;
        }
        ProgressDialogUtil.startLoad(this, getString(R.string.check_update_ding));

        RFUpgradeManager.UpgradeSource source = RFUpgradeManager.UpgradeSource.fromLocalPath(mPathZipPath);

        upgradeCallback = new FirmwareUpgradeCallback() {
            @Override
            public void onCheckResult(UpgradeCheckResult result, UpgradeInfo info) {
                ProgressDialogUtil.stopLoad();

                    if (!result.checkPassed) {
                        Log.e(TAG, ">>checkUpdate failed: " + result.errorMessage);
                        showTvResult(result.errorMessage);
                        return;
                    }
                    if (!result.hasUpgrade) {
                        Log.i(TAG, ">>checkUpdate: firmware is up to date");
                        ToastUtils.show(getString(R.string.no_update_was_detected));
                        showTvResult(getString(R.string.no_update_was_detected));
                        return;
                    }
                    Log.i(TAG, ">>checkUpdate: has upgrade, upgradeCount="
                            + (info != null ? info.getUpgradeCount() : 0));

                    DialogRFUpgrade.showConfirmDialog(
                            RFIDUpdateActivity.this, info, "", "", new FirmwareConfirmDialog.OnConfirmListener() {
                                @Override
                                public void onConfirm() {
                                    RFUpgradeManager.getInstance().startUpgrade(
                                            RFIDUpdateActivity.this, true, upgradeCallback);
                                }

                                @Override
                                public void onCancel() {
                                }
                            });


            }

            @Override
            public void onStepChanged(UpgradeStepEvent event) {
                Log.d(TAG, ">>step=" + event.stepKey
                        + " status=" + event.status
                        + " progress=" + event.progress
                        + " total=" + event.totalProgress
                        + (event.message != null ? " msg=" + event.message : ""));
            }

            @Override
            public void onUpgradeFinished(boolean success, String message) {
                Log.i(TAG, ">>upgrade finished: success=" + success + " msg=" + message);

                if (success) {
                    showTvResult("Update Succcess. ");
                    initDataUI();
                } else {
                    showTvResult("Update fail: " + message);
                }
            }

            @Override
            public void onError(int errorCode, String message) {
                Log.e(TAG, ">>upgrade error: code=" + errorCode + " msg=" + message);
                showTvResult(errorCode + " " + message);

            }
        };
            RFUpgradeManager.getInstance().checkUpgrade(this, source, upgradeCallback);


    }

    private void showTvResult(String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                binding.paramResult.setText(message);
                stopTimer();
            }
        });
    }

    private void stopTimer() {
        if (simpleTimer != null) {
            simpleTimer.stop();
        }
    }

    private void initView() {
        // 使用 ViewHelper 批量设置点击监听器
        ViewHelper.setOnClickListener(this,
                binding.btStartUpdate,
                binding.btnSelectFile);
            binding.tvVer.setText(R.string.loading);
            initDataUI();
    }

    private String verMcu = "";//Sled Version
    private String verBle = "";//Sled BLE Version
    private String verRFIDMode = "";//RFID module version
    private String verRFIDModeDetails = "";//Detailed version of RFID module
    private String verRFIDChips = "";//RFID chip version

    private void initDataUI() {


        new Thread(new Runnable() {
            @Override
            public void run() {
                SystemClock.sleep(200);

                if (RFIDSDKManager.getInstance().getRfidManager() != null){
                for (int i = 0; i < 3; i++) {



                    if (RFIDSDKManager.getInstance().getRfidManager().getReaderDeviceType() != ReaderDeviceType.INTEGRATED) {

                        if (TextUtils.isEmpty(verMcu)) {
                            verMcu = GripDeviceManager.getInstance().getVersionSystem();
                            if (!TextUtils.isEmpty(verMcu)) {
                                updateVersionUI();
                            }
                        }
                        if (RFIDSDKManager.getInstance().getRfidManager().getReaderDeviceType() == ReaderDeviceType.BLE_DEVICE) {
                            if (TextUtils.isEmpty(verBle)) {
                                verBle = GripDeviceManager.getInstance().getVersionBLE();
                                if (!TextUtils.isEmpty(verBle)) {
                                    updateVersionUI();
                                }
                            }
                        }
                    }


                    if (TextUtils.isEmpty(verRFIDChips)) {
                        ExVersionInfo exVersionInfo = RFIDSDKManager.getInstance().getRfidManager().getEx10Version();
                        if (exVersionInfo != null) {
                            verRFIDChips = exVersionInfo.versionInfo;
                            if (!TextUtils.isEmpty(verRFIDChips)) {
                                updateVersionUI();
                            }
                        }
                    }

                    if (TextUtils.isEmpty(verRFIDModeDetails)) {
                        verRFIDModeDetails = RFIDSDKManager.getInstance().getRfidManager().getExFWDetails();
                        if (!TextUtils.isEmpty(verRFIDModeDetails)) {
                            updateVersionUI();
                        }
                    }

                    if (TextUtils.isEmpty(verRFIDModeDetails)) {
                        if (TextUtils.isEmpty(verRFIDMode)) {
                            verRFIDMode = RFIDSDKManager.getInstance().getRfidManager().getFirmwareVersion();
                            if (!TextUtils.isEmpty(verRFIDMode)) {
                                updateVersionUI();
                            }
                        }
                    }

                    SystemClock.sleep(200);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showRadioButtonUI();
                    }
                });
            }
            }
        }).start();

    }

    private void updateVersionUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String allVersion = "";

                String verRFIDModeTrue = "";
                if (!TextUtils.isEmpty(verRFIDModeDetails)) {
                    verRFIDModeTrue = verRFIDModeDetails;
                } else {
                    verRFIDModeTrue = verRFIDMode;
                }

                if (RFIDSDKManager.getInstance().getRfidManager().getReaderDeviceType() == ReaderDeviceType.INTEGRATED) {
                    if (TextUtils.isEmpty(verRFIDChips)) {
                        allVersion = String.format("%s%s",
                                getString(R.string.rfid_os), verRFIDModeTrue);
                    } else {
                        allVersion = String.format("%s%s      %s%s",
                                getString(R.string.rfid_os), verRFIDModeTrue,
                                getString(R.string.chips_os), verRFIDChips);
                    }
                } else {
                    if (RFIDSDKManager.getInstance().getRfidManager().getReaderDeviceType() == ReaderDeviceType.BLE_DEVICE) {
                        allVersion = String.format("%s%s%s%s%s%s      %s%s",
                                getString(R.string.mcu_os), verMcu,
                                getString(R.string.ble_os), verBle,
                                getString(R.string.rfid_os), verRFIDModeTrue,
                                getString(R.string.chips_os), verRFIDChips);
                    } else {
                        allVersion = String.format("%s%s%s%s      %s%s",
                                getString(R.string.mcu_os), verMcu,
                                getString(R.string.rfid_os), verRFIDModeTrue,
                                getString(R.string.chips_os), verRFIDChips);
                    }

                }

                binding.tvVer.setText(allVersion);

            }
        });
    }


    @Override
    public void onTick(String timeString, int elapsedSeconds) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                binding.timeTask.setText( getFormatTimeString(timeString));
                Log.d("SimpleTimer", "当前时间: " + timeString + ", 秒数: " + elapsedSeconds);
            }
        });
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (simpleTimer != null) {
            simpleTimer.release();
        }
        finish();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SELECT_FILE && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();

                // 优先通过文件名判断文件类型（更可靠）
                // First, determine the file type based on the file name (this is more reliable)
                String fileName = ReadFIleBinOrZip.getFileNameFromUri(RFIDUpdateActivity.this, uri);
                if (TextUtils.isEmpty(fileName)) {
                    // 如果无法获取文件名，尝试通过路径判断
                    //If the file name cannot be obtained, try to determine it based on the path.
                    String filePath = ReadFIleBinOrZip.getFileAbsolutePath(RFIDUpdateActivity.this, uri);
                    if (!TextUtils.isEmpty(filePath)) {
                        fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
                    }
                }
                mPathZipName = fileName;
                mPathZipPath = "";

                Log.d(TAG, "onActivityResult  fileName : " + fileName);

                if (!TextUtils.isEmpty(fileName)) {
                    String lowerFileName = fileName.toLowerCase();
                    // 验证文件扩展名并处理
                    //Verify the file extension and handle accordingly
                    if (lowerFileName.endsWith(".zip")) {
                        mPathZipPath = handleZipFile(uri, fileName);
                        showRadioButtonUI();
                    } else {
                        showRadioButtonUI();
                        Toast.makeText(this, getResources().getString(R.string.please_choice_zip), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String resStr = getResources().getString(R.string.no_found_to_firmeare_file);
                    mPathZipName = resStr;
                    showRadioButtonUI();
                    Toast.makeText(this, getResources().getString(R.string.not_get_file_path), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


    /**
     * 刷新显示 RadioButton 的UI
     * Refresh and update the UI of the RadioButton
     */
    private void showRadioButtonUI() {


        if (isFileExists(mPathZipPath)) {
            binding.tvFilePath.setText(mPathZipName);
            binding.tvFilePath.setTextColor(getResources().getColor(R.color.black));
            setSharedPreferences(Constant.KEY_PATH_ZIP_NAME, mPathZipName);
            setSharedPreferences(Constant.KEY_PATH_ZIP_PATH, mPathZipPath);
        } else {
            binding.tvFilePath.setText("");
            binding.tvFilePath.setTextColor(getResources().getColor(R.color.black));
            clearSharedPreferences(Constant.KEY_PATH_MCU);
        }

    }

    private void setSharedPreferences(String key, String value) {
        SharedPreferencesUtils.putString(mContext, key, value);
    }

    private void clearSharedPreferences(String key) {
        SharedPreferencesUtils.putString(mContext, key, "");
    }


    /**
     * 处理 ZIP 文件（解压并获取所有文件路径）
     * Process the ZIP file (unzip and obtain all file paths)
     * @param zipUri ZIP 文件 URI   ZIP file URI
     * @param fileName 文件名（用于显示）   File name (for display)
     */
    private String handleZipFile(Uri zipUri, String fileName) {
        // 优先尝试通过 URI 直接解压（适用于无法获取路径的情况）
        // First, try to extract directly via the URI (applicable when the path cannot be obtained)
        String zipFilePath = ReadFIleBinOrZip.getFileAbsolutePath(RFIDUpdateActivity.this, zipUri);
        Log.d(TAG, "onActivityResult  zipFilePath : " + zipFilePath);
        return zipFilePath;
    }

    /**
     * 获取状态栏高度
     * Obtain the height of the status bar
     * @return 状态栏高度（px） Status bar height (px)
     */
    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static boolean isFileExists(String path){
        File file = new File(path);
        if (file.exists()){
            return true;
        }
        return false;
    }
    public static  String getFormatTimeString(String timeString){
        return "("+timeString+")";
    }
}
