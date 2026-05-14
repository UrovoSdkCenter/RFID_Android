package com.rfid.base;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.Ndef;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.journeyapps.barcodescanner.CaptureActivity;
import com.rfid.base.adapter.ScanAdapter;
import com.rfid.base.databinding.ActivityBleBinding;
import com.rfid.base.utils.LoadingDialogUtils;
import com.rfid.base.utils.M1CardUtils;
import com.rfid.base.utils.ToastUtils;
import com.rfid.base.utils.Util;
import com.rfid.base.utils.ViewHelper;
import com.ubx.usdk.RFIDSDKManager;
import com.ubx.usdk.listener.InitListener;
import com.ubx.usdk.util.conver.MacAddressFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class BleConnectBaseActivity extends BaseActivity {
    private String TAG = BleConnectBaseActivity.class.getSimpleName();
    private String mUuid = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    public static final int REQUEST_PERMISSION_LOCATION = 2;
    public static final int REQUEST_PERMISSION_WRITE = 3;
    public static final int REQUEST_GPS = 4;
    
    // 使用 DataBinding
    private ActivityBleBinding binding;

    private ScanAdapter adapter;
    private List<BluetoothDevice> bleRssiDevices;
    private boolean isFilter = false;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;

    private HashMap<String, String> hashMap = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 使用 DataBinding
        binding = ActivityBleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        Log.v(TAG, "onCreate()");
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Device does not support Bluetooth");
            return;
        }

        initAdapter();
        initLinsenter();
        requestPermission();


        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bluetoothLeScanner == null) {
            ToastUtils.show(getString(R.string.please_open_ble));
        } else {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkButtonScaning();
                }
            }, 1000);
        }
        initNfc();


    }

    private PendingIntent pIntent;
    private NfcAdapter mNfcAdapter;

    private void initNfc() {
        mNfcAdapter = M1CardUtils.isNfcAble(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pIntent = PendingIntent.getActivity(this, 0, //在Manifest里或者这里设置当前activity启动模式，否则每次响应NFC事件，activity会重复创建
                    new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_MUTABLE);
        } else {
            pIntent = PendingIntent.getActivity(this, 0, //在Manifest里或者这里设置当前activity启动模式，否则每次响应NFC事件，activity会重复创建
                    new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        }

    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        //这里必须setIntent，set  NFC事件响应后的intent才能拿到数据
        //Here, the intent must be set. Only after setting the intent corresponding to the NFC event response can the data be obtained.
        setIntent(intent);
        try {
            Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (detectedTag == null) {
                Log.e(TAG, "onNewIntent:  detectedTag == null ");
                return;
            }
            Ndef ndef = Ndef.get(detectedTag);
            if (ndef == null) {
                Log.e(TAG, "onNewIntent:  ndef == null ");
                return;
            }
            ndef.connect();
            NdefMessage ndefMessage = ndef.getNdefMessage();
            if (ndefMessage == null) {
                Log.e(TAG, "onNewIntent:  ndefMessage == null ");
                return;
            }
            String mac = MacAddressFormatter.formatMacAddressByNFC(ndefMessage);
            Toast.makeText(this, mac, Toast.LENGTH_SHORT).show();
            if (!checkBlueAndGPS()) {
                return;
            }
            toMACGetDevice(mac);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initAdapter() {
        bleRssiDevices = new ArrayList<>();
        adapter = new ScanAdapter(this, bleRssiDevices);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        binding.recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        binding.recyclerView.getItemAnimator().setChangeDuration(300);
        binding.recyclerView.getItemAnimator().setMoveDuration(300);
        binding.recyclerView.setAdapter(adapter);

        adapter.setOnClickItem(new ScanAdapter.OnClickItem() {
            @Override
            public void item(int position) {
                stopScan();

                try {
                    BluetoothDevice device = bleRssiDevices.get(position);
                    toDataAty(device.getAddress());
                } catch (Exception e) {
                    e.printStackTrace();
                    startScanning();
                }

//                toMACGetDevice("5F:7D:B8:4E:13:1A:E7");
            }
        });
    }



    private long lastScanTime = 0l;

    private void initLinsenter() {
        binding.tvAdapterStates.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View v) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        });
        
        ViewHelper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isGpsOpen(BleConnectBaseActivity.this);
                checkButtonScaning();
            }
        }, binding.btnScan);
        
        ViewHelper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(BleConnectBaseActivity.this, CaptureActivity.class);
                startActivityForResult(intent, REQUEST_SCAN_QR);
            }
        }, binding.buttonQr);
        
        binding.swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                binding.swipeLayout.setRefreshing(false);
                checkButtonScaning();
            }
        });
    }

    private void checkButtonScaning() {
        if (!checkBlueAndGPS()) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        if ((currentTime - lastScanTime) >= 5000) {
            lastScanTime = currentTime;
            startScanning();
        } else {
            ToastUtils.show(getString(R.string.scanning_too_frequently));
        }
    }

    private int REQUEST_SCAN_QR = 100;
    private int REQUEST_ENABLE_BT = 102;
    private int REQUEST_ENABLE_PERMISSION = 111;

    public void requestPermission() {
        // Android 6.0动态请求权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = null;
            //Android11  == 30
            Log.e(TAG, "Build.VERSION.SDK_INT :   " + Build.VERSION.SDK_INT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissions = new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION
                        , Manifest.permission.ACCESS_FINE_LOCATION
//                    , Manifest.permission.READ_EXTERNAL_STORAGE
//                    , Manifest.permission.WRITE_EXTERNAL_STORAGE
                        , Manifest.permission.BLUETOOTH_SCAN
                        , Manifest.permission.BLUETOOTH_ADMIN
                        , Manifest.permission.BLUETOOTH_ADVERTISE
                        , Manifest.permission.BLUETOOTH_CONNECT
                        , Manifest.permission.CAMERA};
            }else {
                permissions = new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION
                        , Manifest.permission.ACCESS_FINE_LOCATION
                        , Manifest.permission.CAMERA};
            }
            boolean isAllPermissions = true;
            for (String str : permissions) {
                if (checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "requestPermission:   " + str + "    not    permission");
                    requestPermissions(permissions, REQUEST_ENABLE_PERMISSION);
                    isAllPermissions = false;
                    break;
                }
            }
            if (isAllPermissions) {
                checkBlueStatus();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_ENABLE_PERMISSION) {
            boolean granted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
            if (granted) {
                if (checkBleScanPermission()){
                    startScanning();
                }
            } else {
            }
        }
    }


    private boolean checkBlueAndGPS() {
        boolean isOpenGPS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&   isGpsOpen(BleConnectBaseActivity.this);
        int bleStatus = checkBlueStatus();
        if (bleStatus != 0 && !isOpenGPS) {
            checkGpsStatus(getString(R.string.please_open_ble_and_gps));
            return false;
        } else if (bleStatus != 0) {
            checkGpsStatus(getString(R.string.please_open_ble_sysy));
            return false;
        } else if (!isOpenGPS) {
            checkGpsStatus(getString(R.string.please_open_gps_sys));
            return false;
        }
        return true;
    }

    //检查蓝牙是否支持及打开
    private int checkBlueStatus() {

        if (bluetoothAdapter == null) {
            // 设备不支持蓝牙   The device does not support Bluetooth.
            return -1;
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                // 蓝牙未打开   Bluetooth is not enabled.
                return -2;
            } else {
                // 蓝牙已打开  Bluetooth is turned on.
                return 0;
            }
        }

    }

    private AlertDialog mAlertDialog;

    private void checkGpsStatus(String msg) {

        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            mAlertDialog.dismiss();
            mAlertDialog = null;
        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
//                && !Utils.isGpsOpen(BleConnectActivity.this)){
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(BleConnectBaseActivity.this);
        mBuilder.setTitle(R.string.tips_status);
//                    .setMessage("为了更精确的扫描到Bluetooth LE设备,请打开GPS定位")
        mBuilder.setMessage(msg);
        mBuilder.setPositiveButton(R.string.confirm, (dialog, which) -> {
//                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//                        startActivityForResult(intent,REQUEST_GPS);
        });
        mBuilder.setNegativeButton(R.string.cancel, null);
        mAlertDialog = mBuilder.create();
        mAlertDialog.show();
//        }else {
//            startScanning();
//        }
    }

    @SuppressLint("MissingPermission")
    private void startScanning() {
        if (!checkBlueAndGPS()) {
            lastScanTime = 0;
            return;
        }
        if (!checkBleScanPermission()) {
            lastScanTime = 0;
            return;
        }

        hashMap.clear();
        bleRssiDevices.clear();
        adapter.notifyDataSetChanged();
        if (bluetoothLeScanner == null){
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        }
        if (bluetoothLeScanner != null) {
            bluetoothLeScanner.stopScan(scanCallback);
            bluetoothLeScanner.startScan(scanCallback);
        } else {
            ToastUtils.show(getString(R.string.please_check_whether_the_system_bluetooth_is_turned_on));
        }
    }

    private boolean checkBleScanPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions =null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissions = new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION
                        , Manifest.permission.ACCESS_FINE_LOCATION
                        , Manifest.permission.BLUETOOTH_SCAN
                        , Manifest.permission.BLUETOOTH_ADMIN
                        , Manifest.permission.BLUETOOTH_ADVERTISE
                        , Manifest.permission.BLUETOOTH_CONNECT};
            }else {
                permissions = new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION
                        , Manifest.permission.ACCESS_FINE_LOCATION
                         };
            }


            boolean isAllPermissions = true;
            for (String str : permissions) {
                if (checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "requestPermission:   " + str + "    not    permission");
                    isAllPermissions = false;
                    break;
                }
            }
            if (isAllPermissions) {
            }
            return isAllPermissions;
        }else {
            return true;
        }

    }


    private final Object locker = new Object();
    private final ScanCallback scanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (TextUtils.isEmpty(device.getName())) {
                return;
            }
            boolean isHave = false;
            List<ParcelUuid> parcelUuids = result.getScanRecord().getServiceUuids();
            if (parcelUuids!=null){
                for (int i = 0; i < parcelUuids.size(); i++) {
                    String uuid = parcelUuids.get(i).getUuid().toString();
                    if (uuid.equals(mUuid)){
                        isHave = true;
                    }
                }
            }
            if (!isHave){
                Log.v(TAG, "Found device: " + device.getName());
                return;
            }
            synchronized (locker) {
                if (!TextUtils.isEmpty(device.getName())) {
                    if (!hashMap.containsKey(device.getAddress())) {
                        hashMap.put(device.getAddress(), device.getName());
                        bleRssiDevices.add(device);
                        adapter.notifyDataSetChanged();
                    }
                }
            }

        }


        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "Scan failed: " + errorCode);
        }
    };

    @SuppressLint("MissingPermission")
    private void stopScan() {
        if (bluetoothLeScanner != null) {
            bluetoothLeScanner.stopScan(scanCallback);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
        } else if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_OK) {
            startScanning();
        } else if (requestCode == REQUEST_SCAN_QR && resultCode == Activity.RESULT_OK) {
            String result = data.getStringExtra("SCAN_RESULT");
            String macAddress = MacAddressFormatter.formatMacAddressByQRcode(result);
            Toast.makeText(this, macAddress, Toast.LENGTH_SHORT).show();
            toMACGetDevice(macAddress);
        } else if (requestCode == REQUEST_GPS) {

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy()");
    }

    private volatile boolean isShow = false;

    @Override
    public void onResume() {
        super.onResume();
        setAtyVisibility(true);
        Log.v(TAG, "onResume()");
//        BleUtils.getInstance().setGattListener(this);
        if (mNfcAdapter != null) {
            IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
            IntentFilter tag = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
            IntentFilter tech = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
            IntentFilter[] filters = new IntentFilter[]{ndef, tag, tech};
            String[][] techList = new String[][]{new String[]{
                    IsoDep.class.getName(),
                    "android.nfc.tech.Ndef",
                    "android.nfc.tech.NfcA",
                    "android.nfc.tech.NfcB",
                    "android.nfc.tech.NfcF",
                    "android.nfc.tech.NfcV",
                    "android.nfc.tech.NdefFormatable",
                    "android.nfc.tech.MifareClassic",
                    "android.nfc.tech.MifareUltralight",
                    "android.nfc.tech.NfcBarcode"
            }};
            mNfcAdapter.enableForegroundDispatch(this, pIntent, filters, techList);
            Log.v(TAG, "enableForegroundDispatch()");
        }
        checkBlueAndGPS();
    }

    @Override
    public void onPause() {
        super.onPause();
        setAtyVisibility(false);
        Log.v(TAG, "onPause()");
        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch(this);
            Log.v(TAG, "disableForegroundDispatch()");
        }
    }

    @SuppressLint("StringFormatMatches")
    private void toMACGetDevice(String deviceAddress) {
        int bleStatus = checkBlueStatus();
        if (bleStatus != 0) {
            checkGpsStatus(getString(R.string.please_open_ble_sysy));
            return ;
        }

        Log.i(TAG, "toMACGetDevice: deviceAddress : " + deviceAddress);
        BluetoothDevice device = null;
        try {
            device = bluetoothAdapter.getRemoteDevice(deviceAddress);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i(TAG, "toMACGetDevice: device : " + device);
        if (device != null) {
            stopScan();
            toDataAty(deviceAddress);
        } else {
            Toast.makeText(this, String.format(getString(R.string.blue_device_xx_not_found), deviceAddress), Toast.LENGTH_SHORT).show();
        }
    }

    private void setAtyVisibility(boolean show){
        isShow = show;
        Log.d(TAG, "setIsShow :   " + isShow);
    }

    /**
     * 跳转界面 to Activity
     * Navigation interface
     * @param mac BLE MAC address
     */
    private void toDataAty(String mac) {

        Log.i(TAG, "toDataAty:  mac : " + mac);
        LoadingDialogUtils.getInstance().show(BleConnectBaseActivity.this, R.string.sled_connecting);
        RFIDSDKManager.getInstance().initBTtoMac(BaseApplication.getContext(), mac, new InitListener() {
            @Override
            public void onStatus(boolean status) {
                Log.i(TAG, "onStatus:  isShow : "+isShow+"    status : " + status);
                LoadingDialogUtils.getInstance().dismiss();
//                if (isShow) {
                    if (status) {
//                    if (isShow) {
                        toMain();
                        Log.i(TAG, "onStatus: to Activity");
                        finish();
//                    }
                    } else {
                        ToastUtils.show(getString(R.string.bluetooth_connection_is_abnormal_abnormal_status_code) + "  " + status);
                    }
//                }
            }
        });
    }

    protected void toMain(){
    }


}
