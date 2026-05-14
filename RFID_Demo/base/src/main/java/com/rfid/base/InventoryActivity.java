package com.rfid.base;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.text.TextWatcher;
import android.text.Editable;

import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.rfid.base.adapter.ScanTagAdapter;
import com.rfid.base.bean.ReadData;
import com.rfid.base.databinding.ActivityInventoryBinding;
import com.rfid.base.utils.DialogUtils;
import com.rfid.base.utils.FileImport;
import com.rfid.base.utils.SPUtils;
import com.rfid.base.utils.ToastUtils;
import com.rfid.base.utils.Util;
import com.rfid.base.utils.ViewHelper;
import com.ubx.usdk.RFIDSDKManager;
import com.ubx.usdk.bean.ReadTag;
import com.ubx.usdk.bean.RfidParameter;
import com.ubx.usdk.bean.enums.ModuleType;
import com.ubx.usdk.bean.enums.QueryMemBank;
import com.ubx.usdk.bean.enums.ReaderDeviceType;
import com.ubx.usdk.constant.BTKeyEvent;
import com.ubx.usdk.grip.ModelInfoCallback;
import com.ubx.usdk.io.GripDeviceManager;
import com.ubx.usdk.io.listener.BatteryGripListener;
import com.ubx.usdk.io.listener.KeyEventListener;
import com.ubx.usdk.listener.DataCallback;
import com.ubx.usdk.rfid.util.RfidErrorConstants;
import com.ubx.usdk.util.StringTool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class InventoryActivity extends BaseActivity implements OnClickListener, OnItemClickListener, TabLifecycleListener {

    private String TAG = InventoryActivity.class.getSimpleName();
    private int inventoryFlag = 1;
    private Handler handler;
    private ArrayList<ReadData> tagList;
    private ScanTagAdapter adapter;

    private ActivityInventoryBinding binding;

    public static String epc;
    public static String tid;
    public boolean isStopThread = false;
    private ScanCallback callback ;
    private static final int MSG_UPDATE_LISTVIEW = 0;
    private static final int MSG_UPDATE_TIME = 1;
    private static final int MSG_UPDATE_SPEED = 2;
    private static final int MSG_UPDATE_STOP = 3;
    private static final int MSG_UPDATE_LIST = 4;
    private static final int MSG_UPDATE_TV = 5;

    private final static int HANDLE_SEND_DEVICE_TYPE = 6;
    private final static int HANDLE_SEND_BATTERY_LEVEL = 7;
    private final static int HANDLE_SEND_IS_CHANGE = 8;
    private Timer timer;
    public long beginTime;
    public long CardNumber;
    public static List<String> mlist = new ArrayList<String>();
    public long lastTime = 0;
    public int lastCount = 0;
    private int isChange = -1;//0 - Not charged, 1 - Charging
    private int batteryLevel = -1;//Percentage of battery charge

    public static int timeout = 300;//300 seconds

    private Context mContext;

    // Filter 相关控件
    private CheckBox cbFilter;
    private LinearLayout llFilterPanel;
    private EditText etFilterData;
    private RadioGroup rgFilterType;
    private ImageView ivFilterArrow;

    private CheckBox cbTagFocus;
    private CheckBox cbFastId;
    private CheckBox cbAscii;
    private boolean isAsciiChecked = false;

    private int filterType = 1; // 0:EPC, 1:TID, 2:USER


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        try {
            binding = ActivityInventoryBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            tagList = new ArrayList<ReadData>();

            // 初始化Filter控件
            initFilterViews();

            // 2. 【关键】创建并设置布局管理器 (以最常用的垂直列表为例)
            binding.LvTags.setLayoutManager(new LinearLayoutManager(this));
            // 创建并配置一个垂直方向的分割线装饰
            DividerItemDecoration divider = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
            binding.LvTags.addItemDecoration(divider);
            adapter = new ScanTagAdapter(mContext, tagList);
            adapter.setOnClickItem(new ScanTagAdapter.OnClickItem() {
                @Override
                public void item(View view, int position) {
                    epc = tagList.get(position).epcId;
                    tid = tagList.get(position).memId;
                    itemLongClick(view, position);
                }
            });

            // 批量设置点击监听器
            ViewHelper.setOnClickListener(this, binding.BtClear, binding.BtImport, binding.BtInventory);
            binding.RgInventory.setOnCheckedChangeListener(new RgInventoryCheckedListener());

            RFIDSDKManager.getInstance().getRfidManager().getModuleType();


            if (RFIDSDKManager.getInstance().getRfidManager() != null) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        String fm = RFIDSDKManager.getInstance().getRfidManager().getFirmwareVersion();
                        binding.tvFm.setText(Util.toVer(fm));
                    }
                }, 1000);
            }
            listenerDeviceModeSwitch();
            binding.LvTags.setAdapter(adapter);
            clearData();

            initHandler();
        } catch (Exception e) {
            e.printStackTrace();
        }
        RFIDSDKManager.getInstance().getRfidManager().setBeepEnable(true);

        if (RFIDSDKManager.getInstance().getRfidManager().getReaderDeviceType() == ReaderDeviceType.BLE_DEVICE
                || RFIDSDKManager.getInstance().getRfidManager().getReaderDeviceType() == ReaderDeviceType.PERIPHERAL_UART) {
            MainBaseActivity.setIsSledVisibility(true);
            listenerGripBatteryInfo();
        } else {
            MainBaseActivity.setIsSledVisibility(false);
        }

//        // true: Displays the signal value ranging from 0 to 100; false: Displays the default value (not within the range of 0 to 100)
//        RFIDSDKManager.getInstance().getRfidManager().setRssiConvertEnabled(true);

    }


    /**
     * 初始化Filter相关控件
     */
    private void initFilterViews() {
        cbFilter = binding.cbFilter;
        llFilterPanel = binding.llFilterPanel;
        etFilterData = binding.etFilterData;
        rgFilterType = binding.rgFilterType;
        ivFilterArrow = binding.ivFilterArrow;

        // 初始化新增的CheckBox控件
        cbTagFocus = binding.cbTagFocus;
        cbFastId = binding.cbFastId;

        // 箭头图标点击监听
        ivFilterArrow.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // 切换面板状态
                boolean isCurrentlyVisible = llFilterPanel.getVisibility() == View.VISIBLE;
                toggleFilterPanel(!isCurrentlyVisible);
            }
        });

        // OK 按钮监听
        binding.btnFilterOk.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                applyFilter();
            }
        });
        // OK 按钮监听
        binding.btnFilterCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int ret = RFIDSDKManager.getInstance().getRfidManager().clearMask();
                if (ret == 0) {
                    toggleFilterPanel(false);
                    cbFilter.setChecked(false);
                }
            }
        });
        // Data输入框添加16进制验证
        etFilterData.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                validateHexInput(s, this);
            }
        });

        // 过滤类型选择监听
        rgFilterType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == binding.rbFilterEpc.getId()) {
                    filterType = 1; // EPC
                } else if (checkedId == binding.rbFilterTid.getId()) {
                    filterType = 2; // TID
                } else if (checkedId == binding.rbFilterUser.getId()) {
                    filterType = 3; // USER
                }
            }
        });

        // TagFocus CheckBox 监听
        cbTagFocus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // TODO: 添加TagFocus功能逻辑
                if (buttonView.isPressed()) {
                    if (isChecked) {

                        DialogUtils.getInstance().showDoubleButtonDialog(mContext, getResources().getString(R.string.tip), getString(R.string.tagfocus_session_s1), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                int ret = RFIDSDKManager.getInstance().getRfidManager().setTagFocus(1);
                                if (ret == RfidErrorConstants.SUCCESS) {
                                    RfidParameter rfidParameter = RFIDSDKManager.getInstance().getRfidManager().getInventoryParameter();
                                    rfidParameter.Session = 1;
                                    ret = RFIDSDKManager.getInstance().getRfidManager().setInventoryParameter(rfidParameter);
                                }
                                if (ret != RfidErrorConstants.SUCCESS) {
                                    ToastUtils.show(getResources().getString(R.string.set_failed) + " " + ret);
                                    cbTagFocus.setChecked(false);
                                }
                            }
                        }, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                cbTagFocus.setChecked(false);
                            }
                        });


                    } else {
                        int ret = RFIDSDKManager.getInstance().getRfidManager().setTagFocus(0);
                        if (ret != RfidErrorConstants.SUCCESS) {
                            ToastUtils.show(getResources().getString(R.string.set_failed) + " " + ret);
                            cbTagFocus.setChecked(true);
                        }
                    }
                }
            }
        });

        ModuleType moduleType = RFIDSDKManager.getInstance().getRfidManager().getModuleType();
        if (moduleType == ModuleType.MODULE_U3 || moduleType == ModuleType.MODULE_U4 || moduleType == ModuleType.MODULE_U5) {
            cbTagFocus.setEnabled(true);

            boolean isTagfocusEnable = false;
            int tagFocus = RFIDSDKManager.getInstance().getRfidManager().getTagFocus();
            if (tagFocus == 1) {
                int session = RFIDSDKManager.getInstance().getRfidManager().getInventoryWithSession();
                if (session == 1) {
                    isTagfocusEnable = true;
                } else {
                }
            }
            cbTagFocus.setChecked(isTagfocusEnable);
        } else {
            cbTagFocus.setEnabled(false);
        }


        // FastID CheckBox Listener
        cbFastId.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isPressed()) {
                    if (isChecked) {
                        int ret = RFIDSDKManager.getInstance().getRfidManager().setQueryMemoryBank(QueryMemBank.FASTTID, 0, 0);
                        if (ret != RfidErrorConstants.SUCCESS) {
                            ToastUtils.show(getResources().getString(R.string.set_failed) + " " + ret);
                            cbFastId.setChecked(false);
                        }
                    } else {
                        int ret = RFIDSDKManager.getInstance().getRfidManager().setQueryMemoryBank(QueryMemBank.EPC, 0, 0);
                        if (ret != RfidErrorConstants.SUCCESS) {
                            ToastUtils.show(getResources().getString(R.string.set_failed) + " " + ret);
                            cbFastId.setChecked(true);
                        }
                    }
                }
            }
        });

        // ASCII CheckBox
        cbAscii = binding.cbAscii;
        isAsciiChecked = SPUtils.getBoolean(SPUtils.KEY_ASCII_CHECKED, false);
        cbAscii.setChecked(isAsciiChecked);

        // ASCII CheckBox
        cbAscii.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isPressed()) {
                    isAsciiChecked = isChecked;
                    SPUtils.putBoolean(SPUtils.KEY_ASCII_CHECKED, isAsciiChecked);
                    clearData();
                }
            }
        });
    }

    /**
     * 切换Filter面板显示/隐藏
     */
    private void toggleFilterPanel(boolean show) {
        if (show) {
            llFilterPanel.setVisibility(View.VISIBLE);
            // 更新箭头图标为向上箭头
            ivFilterArrow.setImageResource(R.drawable.ic_arrow_up);
        } else {
            llFilterPanel.setVisibility(View.GONE);
            // 更新箭头图标为向下箭头
            ivFilterArrow.setImageResource(R.drawable.ic_arrow_down);
            // 清除过滤条件
//            clearFilter();
        }
    }

    /**
     * 应用过滤条件
     */
    private void applyFilter() {
        try {
            // 获取输入值
            String filterData = etFilterData.getText().toString().trim();

            if ((binding.etFilterPtr.getText() == null) || (binding.etFilterLen.getText() == null) || (filterData == null)) {
                return;
            }
            // 验证16进制数据
            if (!filterData.isEmpty() && !isValidHex(filterData)) {
                Toast.makeText(this, R.string.data_16_valid, Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                int maskAddr = (int) Integer.valueOf(binding.etFilterPtr.getText().toString());
                int maskMem = filterType;
                int maskLen = (int) Integer.valueOf(binding.etFilterLen.getText().toString());
                int ret = RFIDSDKManager.getInstance().getRfidManager().clearMask();
                ret = RFIDSDKManager.getInstance().getRfidManager().addMaskByBits(maskMem, maskAddr, maskLen, filterData);
                if (ret == 0) {
                    String temp = maskMem + "," + maskAddr + "," + maskLen + "," + filterData;
                    Log.d(TAG, "Filter: " + temp);
                    ToastUtils.show(getString(R.string.strsuccess));
                    cbFilter.setChecked(true);
                    // 收起面板
                    toggleFilterPanel(false);
                } else {
                    ToastUtils.show(getString(R.string.strfailed) + " " + ret);
                    cbFilter.setChecked(false);
                }
            } catch (Exception ex) {
                ToastUtils.show(getString(R.string.strfailed));
                cbFilter.setChecked(false);
            }


//            cbFilter.setChecked(false);

        } catch (NumberFormatException e) {
            ToastUtils.show(getResources().getString(R.string.ptr_len_number_must));
        }
    }


    /**
     * 验证16进制输入
     */
    private void validateHexInput(Editable s, TextWatcher watcher) {
        String input = s.toString().toUpperCase();
        StringBuilder validInput = new StringBuilder();

        for (char c : input.toCharArray()) {
            if ((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F')) {
                validInput.append(c);
            }
        }

        if (!input.equals(validInput.toString())) {
            etFilterData.removeTextChangedListener(watcher);
            etFilterData.setText(validInput.toString());
            etFilterData.setSelection(validInput.length());
            etFilterData.addTextChangedListener(watcher);
        }
    }

    /**
     * 检查是否为有效的16进制字符串
     */
    private boolean isValidHex(String hex) {
        if (hex == null || hex.isEmpty()) {
            return true;
        }
        return hex.matches("^[0-9A-Fa-f]+$");
    }

    public class RgInventoryCheckedListener implements RadioGroup.OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            if (checkedId == binding.RbInventorySingle.getId()) {
                inventoryFlag = 0;
            } else if (checkedId == binding.RbInventoryLoop.getId()) {
                inventoryFlag = 1;
            }
        }
    }

    private void setViewEnabled(boolean enabled) {
        binding.RbInventorySingle.setEnabled(enabled);
        binding.RbInventoryLoop.setEnabled(enabled);
        binding.BtClear.setEnabled(enabled);
        if (enabled) {
            binding.BtInventory.setEnabled(enabled);
        }
    }

    private void initHandler() {
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                try {
                    switch (msg.what) {
                        case MSG_UPDATE_LISTVIEW:
                            ReadTag readTag = (ReadTag) msg.obj;
                            addEPCToList(readTag);
                            break;
                        case MSG_UPDATE_TIME:
                            String ReadTime = msg.obj + "";
                            long CurTime = Integer.valueOf(ReadTime);
                            long hour = CurTime / (60 * 60 * 1000);
                            long min = (CurTime / 1000 - (hour * 60 * 60)) / 60;
                            long sec = (CurTime / 1000 - hour * 60 * 60 - min * 60);
                            String strHour = String.valueOf(hour);
                            if (strHour.length() < 2) strHour = "0" + strHour;
                            String strmin = String.valueOf(min);
                            if (strmin.length() < 2) strmin = "0" + strmin;
                            String strsec = String.valueOf(sec);
                            if (strsec.length() < 2) strsec = "0" + strsec;
                            binding.tvTimes.setText(strHour + ":" + strmin + ":" + strsec);
                            break;
                        case MSG_UPDATE_SPEED:
                            String readSpeed = msg.obj + "";
                            binding.tvTagspeed.setText(readSpeed);
                            break;
                        case MSG_UPDATE_STOP:
                            updateList();
                            if (timer != null) {
                                timer.cancel();
                                timer = null;
                                binding.BtInventory.setText(getString(R.string.btStoping));
                            }
                            setViewEnabled(true);
                            binding.BtInventory.setText(getString(R.string.btInventory));
                            break;
                        case MSG_UPDATE_LIST:
                            updateList();
                            break;
                        case MSG_UPDATE_TV:
                            updateTv();
                            break;
                        case HANDLE_SEND_DEVICE_TYPE:
                            boolean isRFG91 = (Boolean) msg.obj;
                            MainBaseActivity.setIsSledVisibility(isRFG91);
                            break;
                        case HANDLE_SEND_IS_CHANGE:
                            int isChange = msg.arg1;
                            MainBaseActivity.setIsSledChargingState(isChange);
                            break;
                        case HANDLE_SEND_BATTERY_LEVEL:
                            int level = msg.arg1;
                            MainBaseActivity.setIsSledBatteryLevel(level);
                            break;
                        default:
                            break;
                    }
                } catch (Exception ex) {
                    ex.toString();
                }
            }
        };
    }

    private HashMap<String, Integer> integerHashMap = new HashMap<>();

    public int checkIsExist(String strEPC) {
        int existFlag = -1;
        if (strEPC == null || strEPC.length() == 0) {
            return existFlag;
        }

        if (integerHashMap.containsKey(strEPC)) {
            return integerHashMap.get(strEPC);
        }
        return existFlag;
    }

    private void clearData() {
        binding.tvCount.setText("0");
        binding.tvTimes.setText("00:00:00");
        binding.tvAlltag.setText("0");
        binding.tvTagspeed.setText("0");
        tagList.clear();
        integerHashMap.clear();
        mlist.clear();
        CardNumber = 0;
        adapter.notifyDataSetChanged();

    }

    private void itemLongClick(View view, int position) {
        PopupMenu popupMenu = new PopupMenu(InventoryActivity.this, view);
        popupMenu.getMenuInflater().inflate(R.menu.item_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // 根据点击的菜单项ID执行不同操作
                if (item.getItemId() == R.id.action_edit) {
                    editItem(position);
                } else if (item.getItemId() == R.id.action_find) {
                    findItem(position);
                }
                return true;
            }
        });
        popupMenu.show();
    }

    private void editItem(int position) {
        MainBaseActivity.myTabHost.setCurrentTab(1);
    }

    private void findItem(int position) {
        MainBaseActivity.myTabHost.setCurrentTab(4);
    }

    /**
     * 添加EPC到列表中
     *
     * @param
     */
    private void addEPCToList(ReadTag readTag) {
//        long startLong = System.currentTimeMillis();
        if (readTag != null) {


            int index = checkIsExist(readTag.epcId);

            CardNumber++;
            if (index == -1) {

                String epc = readTag.epcId;
                String data = readTag.memId;

                if (isAsciiChecked) {
                    epc = StringTool.hexStringToASCIIString(epc);//TODO 默认的转ASCII码方法
                    if (!TextUtils.isEmpty(data) && !data.startsWith("E280")) {
                        data = StringTool.hexStringToASCIIString(data);
                    }

                }

                ReadData readData = new ReadData();
                readData.epcId = epc;
                readData.memId = data;
                readData.rssidBm = readTag.rssidBm;
                readData.BID = readTag.BID;
                readData.count = 1;

                tagList.add(readData);
//                binding.LvTags.setAdapter(adapter);
                binding.tvCount.setText("" + tagList.size());
                mlist.add(epc);
                integerHashMap.put(readTag.epcId, mlist.size() - 1);
                updateList();
//                handler.sendEmptyMessage(MSG_UPDATE_TV);
            } else {
                ReadData readData1 = tagList.get(index);
                int tagcount = readData1.count + 1;
                readData1.count = tagcount;
                readData1.rssidBm = readTag.rssidBm;
                tagList.set(index, readData1);
                updateList();
            }
        }
    }

    private void updateTv() {
        binding.tvCount.setText("" + tagList.size());
    }

    private void updateList() {
        binding.tvAlltag.setText(String.valueOf(CardNumber));
        adapter.notifyDataSetChanged();
    }


    private boolean isOnResume = false;

    @Override
    protected void onResume() {
        super.onResume();
        onTabSelected();
    }

    public void onTabSelected() {
        isOnResume = true;
        isStopThread = false;
        if (RFIDSDKManager.getInstance().getRfidManager() != null) {
            if (RFIDSDKManager.getInstance().getRfidManager().getReaderDeviceType() != ReaderDeviceType.INTEGRATED) {
                isVisableSettingTab(true);
                listenerGripStatus();
            } else {
                isVisableSettingTab(false);
            }
        }
        updateFastIDCheck();
    }

    @Override
    public void onTabUnselected() {
        isOnResume = false;
        cancelResultCallBack();
        stopInventory();
    }
    private void addResultCallBack(){
        if (callback == null){
            callback = new ScanCallback();
            RFIDSDKManager.getInstance().getRfidManager().addDataCallback(callback);
        }
    }
    private void cancelResultCallBack(){
        callback  = null;
    }

    private void updateFastIDCheck() {
        QueryMemBank queryMemBank = RFIDSDKManager.getInstance().getRfidManager().getQueryMemoryBank();
        if (queryMemBank == QueryMemBank.FASTTID) {
            cbFastId.setChecked(true);
        } else {
            cbFastId.setChecked(false);
        }
    }

    private void listenerDeviceModeSwitch() {

        RFIDSDKManager.getInstance().getRfidManager().setSwitchCallback(new ModelInfoCallback() {
            @Override
            public void onInfo(int type, String firmware, int power) {
                Log.d(TAG, "onInfo -->  type:" + type + ",firware:" + firmware + ",power:" + power);
                // type  0 : 自带模块（DT610自带模块） 1: 串口模块  （RFG91串口设备）
                if (type == 0) {
                    //不显示 DeviceInfoActivity 的Tab页
                    isVisableSettingTab(false);
                } else if (type == 1) {
                    //显示 DeviceInfoActivity 的Tab页
                    isVisableSettingTab(true);
                }
                binding.tvFm.setText(Util.toVer(firmware));

                isChangeDeviceType();
            }

            @Override
            public void onModuleStatus(int status) {

            }
        });
    }

    private void isChangeDeviceType() {
        int type = RFIDSDKManager.getInstance().getRfidManager().getReaderDeviceType();
        boolean isHandle = false;
        if (type == ReaderDeviceType.INTEGRATED) {
            isHandle = false;
        } else {
            listenerGripBatteryInfo();
            isHandle = true;
        }
        Message message = Message.obtain();
        message.obj = isHandle;
        message.what = HANDLE_SEND_DEVICE_TYPE;
        handler.sendMessage(message);
    }

    private void listenerGripBatteryInfo() {
        GripDeviceManager.getInstance().setBatteryGripListener(new BatteryGripListener() {
            @Override
            public void isChange(int i) {
                Log.e(TAG, "isChange: " + i);
                updateBatteryIsChange(i);
            }

            @Override
            public void level(int i) {
                updateBatteryLevel(i);
            }
        });
        threadGripBatteryInfo();
    }

    private void updateBatteryIsChange(int isChange) {
        Message message = Message.obtain();
        message.arg1 = isChange;
        message.what = HANDLE_SEND_IS_CHANGE;
        handler.sendMessage(message);
    }

    private void updateBatteryLevel(int level) {
        Message message = Message.obtain();
        message.arg1 = level;
        message.what = HANDLE_SEND_BATTERY_LEVEL;
        handler.sendMessage(message);
    }

    /**
     * 渲染数据，回到该界面可能有上一次操作后的数据
     */
    public void threadGripBatteryInfo() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 10; i++) {
                    Log.e(TAG, "getDatas  start ");
                    boolean isComplete = true;

                    if (batteryLevel < 0) {
                        batteryLevel = GripDeviceManager.getInstance().getElectricQuantity();
                        Log.e(TAG, "getDatas  for batteryLevel : " + batteryLevel);
                        if (batteryLevel >= 0) {
                            updateBatteryLevel(batteryLevel);
                        } else {
                            isComplete = false;
                        }
                        SystemClock.sleep(10);
                    }

                    if (isChange < 0) {
                        isChange = GripDeviceManager.getInstance().getIsChange();
                        Log.e(TAG, "getDatas  for isChange : " + isChange);
                        if (isChange >= 0) {
                            updateBatteryIsChange(isChange);
                        } else {
                            isComplete = false;
                        }
                        SystemClock.sleep(10);
                    }
                    Log.e(TAG, "getDatas  for isComplete : " + isComplete);
                    if (isComplete) {
                        break;
                    }
                }
            }
        }).start();
    }

    /**
     *
     * @param isVisible  false: 不显示 DeviceInfoActivity 的Tab页  true:显示 DeviceInfoActivity 的Tab页
     */
    private void isVisableSettingTab(boolean isVisible) {
        MainBaseActivity mainBaseActivity = MainBaseActivity.getInstance();
        if (mainBaseActivity != null) {
            mainBaseActivity.setDeviceInfoTabVisible(isVisible);
        }
    }

    private void listenerGripStatus() {
        GripDeviceManager.getInstance().setKeyEventListener(new KeyEventListener() {
            @Override
            public void event(int keyCode, boolean isDown) {
                //keycode == BTKeyEvent.BT_SCAN   The handle button was pressed
                //isDown  true: down   false: up
                if (isOnResume) {
                    if (keyCode == BTKeyEvent.BT_SCAN) {
                        if (isDown) {
                            readTag();
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onClick(View arg0) {
        try {
            if (arg0 == binding.BtInventory) {
                readTag();
            } else if (arg0 == binding.BtClear) {
                clearData();
            } else if (arg0 == binding.BtImport) {
                if (tagList.size() == 0) {
                    Toast.makeText(getApplicationContext(), getString(R.string.msgNodata), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!checkFilePermiss()){
                    return;
                }
                boolean re = FileImport.daochu("", tagList);
                if (re) {
                    Toast.makeText(getApplicationContext(), getString(R.string.msgImportsuc), Toast.LENGTH_SHORT).show();
                    //clearData();
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.msgImportfailed), Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            stopInventory();
        }
    }

    private void readTag() {
        epc = "";
        tid = "";
        Log.d(TAG, "readTag()   " + getString(R.string.btInventory) + "  " + inventoryFlag);

        addResultCallBack();

        if (binding.BtInventory.getText().equals(getString(R.string.btInventory)))// 识别标签
        {
            switch (inventoryFlag) {
                case 0:// Single
                {
                    RFIDSDKManager.getInstance().getRfidManager().inventorySingle();
                }
                break;
                case 1: {
//                    int result = RFIDSDKManager.getInstance().getRfidManager().startInventory();
                    int result = RFIDSDKManager.getInstance().getRfidManager().startInventoryWithTimeout(timeout);
                    if (result == 0) {
                        String content = "";
                        if (timeout == 0) {
                            content = getResources().getString(R.string.tips_inv);
                        } else {
                            content = String.format(getResources().getString(R.string.tips_inv_timeout), timeout);
                        }
                        binding.tvTips.setText(content);
                        lastTime = System.currentTimeMillis();
                        lastCount = 0;
                        binding.BtInventory.setText(getString(R.string.title_stop_Inventory));
                        setViewEnabled(false);
                        if (timer == null) {
                            beginTime = System.currentTimeMillis();
                            timer = new Timer();
                            timer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    long ReadTime = System.currentTimeMillis() - beginTime;
                                    Message msg = handler.obtainMessage();
                                    msg.what = MSG_UPDATE_TIME;
                                    msg.obj = String.valueOf(ReadTime);
                                    handler.sendMessage(msg);
                                }
                            }, 0, 200);
                        }
                    }
                }
                break;
                default:
                    break;
            }
        } else {// 停止识别
            Log.d(TAG, "readTag()   stopInventory()");
            stopInventory();
        }
    }

    private void stopInventory() {
        RFIDSDKManager.getInstance().getRfidManager().stopInventory();
        // View 可能尚未 attach（Activity 刚创建即被切走），需守卫判断
        if (binding != null && binding.tvTips.isAttachedToWindow()) {
            binding.tvTips.setText("");
        }
        delayStopInventoryUI(1000);
    }



    class ScanCallback implements DataCallback {
        @Override
        public void onInventoryTag(ReadTag readTag) {

            Message msg = handler.obtainMessage();
            msg.what = MSG_UPDATE_LISTVIEW;
            msg.obj = readTag;
            handler.sendMessage(msg);
            lastCount++;
            if (System.currentTimeMillis() - lastTime >= 1000) {
                msg = handler.obtainMessage();
                msg.what = MSG_UPDATE_SPEED;
                msg.obj = ((lastCount * 1000) / (System.currentTimeMillis() - lastTime)) + "";
                handler.sendMessage(msg);
                lastTime = System.currentTimeMillis();
                lastCount = 0;
            }
        }

        /**
         * 盘存结束回调(Inventory Command Operate End)
         */
        @Override
        public void onInventoryTagEnd() {
            Log.d(TAG, "onInventoryTagEnd()");
            delayStopInventoryUI(10);
        }
    }

    private void delayStopInventoryUI(int delaytime) {
        handler.removeMessages(MSG_UPDATE_STOP);
        handler.sendEmptyMessageDelayed(MSG_UPDATE_STOP, delaytime);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        epc = tagList.get(position).epcId;
        tid = tagList.get(position).memId;
    }


    @Override
    protected void onPause() {
        super.onPause();
        onTabUnselected();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * 按键扫描RFID
     **/
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == 515 || keyCode == 523) && event.getRepeatCount() == 0) {
            if (binding.BtInventory.getText().equals(getString(R.string.btInventory))) {// 识别标签
                readTag();
            }

        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 按键扫描RFID
     **/
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if ((keyCode == 515 || keyCode == 523) && event.getRepeatCount() == 0) {
            if (!binding.BtInventory.getText().equals(getString(R.string.btInventory))) {// 识别标签
                readTag();
            }
        }
        return super.onKeyUp(keyCode, event);
    }


}
