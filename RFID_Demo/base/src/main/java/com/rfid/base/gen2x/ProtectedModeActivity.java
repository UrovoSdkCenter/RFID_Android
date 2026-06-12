package com.rfid.base.gen2x;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.rfid.base.R;
import com.rfid.base.TabLifecycleListener;
import com.rfid.base.adapter.ScanTagAdapter;
import com.rfid.base.bean.ReadData;
import com.rfid.base.databinding.ActivityProtectedModeBinding;
import com.rfid.base.utils.GlobalData;
import com.rfid.base.utils.TipsMessageGen2XPeofile;
import com.rfid.base.utils.ToastUtils;
import com.rfid.base.utils.Util;
import com.ubx.usdk.RFIDSDKManager;
import com.ubx.usdk.bean.ReadTag;
import com.ubx.usdk.bean.enums.InventorySceneMode;
import com.ubx.usdk.listener.DataCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 标签保护模式 Activity
 * 支持标签保护模式的启用/禁用和盘点功能
 *
 * Tag protection mode Activity
 * Supports the enable/disable and inventory functions of the tag protection mode
 */
public class ProtectedModeActivity extends Activity implements View.OnClickListener, TabLifecycleListener {

    private static final String TAG = "ProtectedModeActivity";
    
    private static final int MSG_UPDATE_LISTVIEW = 0;
    private static final int MSG_UPDATE_TIME = 1;
    private static final int MSG_UPDATE_SPEED = 2;
    private static final int MSG_UPDATE_STOP = 3;
    
    // 硬件按键码  Hardware key codes
    private static final int KEY_CODE_RFID = 523;
    
    // 密码长度    Password length
    private static final int PASSWORD_LENGTH = 8;
    
    // 内存区域   Memory area
    private static final byte MEM_RESERVED = 0;
    private static final byte WORD_PTR_PASSWORD = 2;

    // UI 绑定   UI Binding
    private ActivityProtectedModeBinding binding;
    
    // 数据适配器
    private ScanTagAdapter adapter;
    
    // 数据集合   Data set
    private final ArrayList<ReadData> tagList = new ArrayList<>();
    private static final List<String> mlist = new ArrayList<>();
    
    // 状态变量   State variable
    private boolean isReading = false;
    private boolean keyPressed = false;
    private long cardNumber = 0;
    private long beginTime = 0;
    private long lastTime = 0;
    private int lastCount = 0;
    
    private Timer timer;
    
    // RFID callback
    private   RfidCallback rfidCallback  ;
    
    // Handler
    private Handler handler;

    public Context mContext;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        binding = ActivityProtectedModeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mContext = this.getApplicationContext();

        initHandler();
        initAdapters();
        initListeners();
        initRadioGroup();
        clearData();

        RFIDSDKManager.getInstance().getRfidManager().setInventorySceneMode(InventorySceneMode.CUSTOM_MODE);
    }

    /**
     * profileExt <= 4000 时显示 Guide 覆盖层，隐藏所有功能控件
     * When profileExt is less than or equal to 4000, display the Guide overlay and hide all function controls.
     */
    private void showGuideOverlay(int visibility) {
        binding.layoutGuideOverlay.setVisibility(visibility);
        binding.tvGuideContent.setText(isZHLanguage() ? TipsMessageGen2XPeofile.getZHDes() : TipsMessageGen2XPeofile.getENDes());
    }

    private boolean isZHLanguage() {
        return "zh".equals(getResources().getConfiguration().locale.getLanguage());
    }


    /**
     * init Handler
     */
    private void initHandler() {
        handler = new Handler(Looper.getMainLooper()) {
            @SuppressLint("HandlerLeak")
            @Override
            public void handleMessage(@NonNull Message msg) {
                handleMessageInternal(msg);
            }
        };
    }

    /**
     * Handle Handler messages
     */
    private void handleMessageInternal(Message msg) {
        try {
            switch (msg.what) {
                case MSG_UPDATE_LISTVIEW:
                    ReadTag readTag = (ReadTag) msg.obj;
                    addTagToList(readTag);
                    break;
                    
                case MSG_UPDATE_TIME:
                    updateTimeDisplay(Long.parseLong(msg.obj.toString()));
                    break;
                    
                case MSG_UPDATE_SPEED:
                    binding.tvTagspeed.setText(msg.obj.toString());
                    break;
                    
                case MSG_UPDATE_STOP:
                    handleInventoryStop();
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, ">> Handler messages Exception: " + e.getMessage());
        }
    }


    /**
     * 更新时间显示
     * Update time display
     */
    private void updateTimeDisplay(long timeMillis) {
        long hours = timeMillis / (60 * 60 * 1000);
        long minutes = (timeMillis / 1000 - hours * 60 * 60) / 60;
        long seconds = timeMillis / 1000 - hours * 60 * 60 - minutes * 60;
        
        String timeStr = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        binding.tvTimes.setText(timeStr);
    }

    /**
     * 处理盘点停止
     * Stop the inventory processing
     */
    private void handleInventoryStop() {
        stopTimer();
        setViewEnabled(true);
        binding.BtInventory.setText(getString(R.string.btStart));
    }

    private void initRadioGroup() {
// 2. 设置监听器
        binding.radioGroupMode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // checkedId 是被选中的那个 RadioButton 的 ID

                if (checkedId == R.id.radio_protect) {
                    // 选中了 Protect
                   binding.cardReaderMode.setVisibility(View.VISIBLE);
                    binding.cardTagMode.setVisibility(View.VISIBLE);
                    binding.cardShortRange.setVisibility(View.GONE);
                } else if (checkedId == R.id.radio_short_range) {
                    // 选中了 ShortRange
                    binding.cardReaderMode.setVisibility(View.GONE);
                    binding.cardTagMode.setVisibility(View.GONE);
                    binding.cardShortRange.setVisibility(View.VISIBLE);
                }
            }
        });

    }
    /**
     * 初始化适配器
     * Initialize adapter
     */
    private void initAdapters() {

        binding.LvTags.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration divider = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        binding.LvTags.addItemDecoration(divider);
        adapter = new ScanTagAdapter(mContext,tagList);
        adapter.setOnClickItem(new ScanTagAdapter.OnClickItem() {
            @Override
            public void item(View view, int position) {
                itemLongClick(view, position);
            }
        });
        binding.LvTags.setAdapter(adapter);
        
        // 开关适配器
        ArrayAdapter<CharSequence> switchAdapter = ArrayAdapter.createFromResource(
                this, R.array.en_select, android.R.layout.simple_spinner_item);
        switchAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.switchSpinner.setAdapter(switchAdapter);
        binding.switchSpinner.setSelection(0, false);
    }

    /**
     * 初始化监听器
     * Initialize the listener
     */
    private void initListeners() {
        binding.BtClear.setOnClickListener(this);
        binding.BtInventory.setOnClickListener(this);
        binding.btnGetreadermode.setOnClickListener(this);
        binding.btnSetreadermode.setOnClickListener(this);
        binding.buttonEnableprotected.setOnClickListener(this);
        binding.buttonDisableprotected.setOnClickListener(this);
        binding.buttonEnableShort.setOnClickListener(this);
        binding.buttonDisableShort.setOnClickListener(this);
        
    }



    /**
     * 设置控件启用状态
     */
    private void setViewEnabled(boolean enabled) {
        binding.BtClear.setEnabled(enabled);
        binding.btnGetreadermode.setEnabled(enabled);
        binding.btnSetreadermode.setEnabled(enabled);
        binding.buttonEnableprotected.setEnabled(enabled);
        binding.buttonDisableprotected.setEnabled(enabled);
        binding.buttonEnableShort.setEnabled(enabled);
        binding.buttonDisableShort.setEnabled(enabled);
        if (enabled) {
            binding.BtInventory.setEnabled(true);
        }
    }

    /**
     * clear Data
     */
    private void clearData() {
        binding.tvCount.setText("0");
        binding.tvTimes.setText("00:00:00");
        binding.tvAlltag.setText("0");
        binding.tvTagspeed.setText("0");
        
        tagList.clear();
        mlist.clear();
        integerHashMap.clear();
        cardNumber = 0;
        
        adapter.notifyDataSetChanged();
    }

    /**
     * 添加标签到列表
     * Add tags to the list
     */
    private void addTagToList(ReadTag readTag) {
        if (readTag == null) {
            return;
        }
        
        int index = checkIsExist( readTag.epcId);
        
        cardNumber++;

        if (index == -1) {
            String epc = readTag.epcId;
            String data = readTag.memId;

            ReadData readData = new ReadData();
            readData.epcId = epc;
            readData.memId = data;
            readData.rssidBm = readTag.rssidBm;
            readData.BID = readTag.BID;
            readData.count = 1;

            tagList.add(readData);

            binding.tvCount.setText(String.valueOf(tagList.size()));
            mlist.add(epc);
            integerHashMap.put(readTag.epcId,mlist.size()-1);
            updateList();
        } else {
            // 已存在标签，更新计数   Existing tag, updated count
            ReadData readData1 = tagList.get(index);
            int tagcount =   readData1.count  + 1;
            readData1.count = tagcount;
            readData1.rssidBm = readTag.rssidBm;
            tagList.set(index, readData1);
            updateList();
        }
    }

    private void updateList(){
        binding.tvAlltag.setText(String.valueOf(cardNumber));
        adapter.notifyDataSetChanged();
    }

    private HashMap<String,Integer> integerHashMap = new HashMap<>();
    public int checkIsExist(String strEPC) {
        int existFlag = -1;
        if (strEPC == null || strEPC.length() == 0) {
            return existFlag;
        }

        if (integerHashMap.containsKey(strEPC)){
            return integerHashMap.get(strEPC);
        }
        return existFlag;
    }
    /**
     * RFID 回调实现  RFID callback implementation
     */
    private class RfidCallback implements DataCallback {
        @Override
        public void onInventoryTag(ReadTag readTag) {
            Log.d(TAG, "onInventoryTag()  "+readTag.epcId);
            Message msg = handler.obtainMessage();
            msg.what = MSG_UPDATE_LISTVIEW;
            msg.obj = readTag;
            handler.sendMessage(msg);
            
            // 更新速度
            lastCount++;
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastTime >= 1000) {
                int speed = (int) ((lastCount * 1000) / (currentTime - lastTime));
                Message speedMsg = handler.obtainMessage(MSG_UPDATE_SPEED, String.valueOf(speed));
                handler.sendMessage(speedMsg);
                lastTime = currentTime;
                lastCount = 0;
            }
        }

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
    private void itemLongClick(View view, int position){
        PopupMenu popupMenu = new PopupMenu(ProtectedModeActivity.this, view);
        popupMenu.getMenuInflater().inflate(R.menu.item_protect, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                GlobalData.epc = tagList.get(position).epcId;
                binding.epcText.setText(GlobalData.epc);
                binding.epcTextShort.setText(GlobalData.epc);
                return true;
            }
        });
        popupMenu.show();
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KEY_CODE_RFID && !keyPressed) {
            keyPressed = true;
            toggleInventory();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KEY_CODE_RFID) {
            keyPressed = false;
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // LocalActivityManager 首次启动时触发，刷新一次界面状态
        //It is triggered when LocalActivityManager is first launched and refreshes the interface status once.
        onTabSelected();
    }

    /**
     * Tab 切换回本页时由宿主 Activity 主动调用。
     * LocalActivityManager 嵌入场景下生命周期/焦点事件不可靠，改为主动通知。
     * When Tab switches back to this page, it is actively called by the host Activity.
     * In the embedded scene, the lifecycle/focus events are unreliable; instead, they are replaced by active notifications.
     */
    public void onTabSelected() {

        int profileExt = RFIDSDKManager.getInstance().getRfidManager().getExtProfile();
        Log.d(TAG, ">>onTabSelected profileExt: " + profileExt);
        if (profileExt <= 4000) {
            showGuideOverlay(View.VISIBLE);
            return;
        }
        showGuideOverlay(View.GONE);
        binding.epcText.setText(GlobalData.epc);
        getReaderProtectedMode(false);
    }

    @Override
    public void onTabUnselected() {
        handleLeave();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handleLeave();
    }

    /** Tab 离开或 Activity 暂停时的统一处理逻辑
     * The unified processing logic for when a Tab is closed or an Activity is paused.*/
    private void handleLeave() {
        cancelResultCallBack();
        stopInventory();
        // Guide 模式下不执行保存，避免用空密码覆盖设备配置
        //In the Guide mode, saving is not performed to avoid overwriting the device configuration with an empty password.
        int profileExt = RFIDSDKManager.getInstance().getRfidManager().getExtProfile();
        if (profileExt > 4000) {
//            saveReaderProtectedMode();
        }
    }
    private void addResultCallBack(){
        Log.d(TAG, "addResultCallBack()  "+rfidCallback);
        if (rfidCallback == null){
            rfidCallback = new RfidCallback();
            RFIDSDKManager.getInstance().getRfidManager().addDataCallback(rfidCallback);
        }
    }
    private void cancelResultCallBack(){
        rfidCallback  = null;
    }
    /**
     * 保存读写器保护模式设置
     * Save the protection mode settings of the reader
     */
    private void saveReaderProtectedMode() {
        int enableFlag = 0;
        String password = binding.etPwd.getText().toString();
        
        if (TextUtils.isEmpty(password) || password.length() != PASSWORD_LENGTH) {
            password = "00000000";
        }
        
        byte[] passwordBytes = Util.hexStringToBytes(password);
        byte[] data = new byte[6];
        data[0] = (byte) enableFlag;
        System.arraycopy(passwordBytes, 0, data, 1, 4);
        data[5] = (byte) (data[0] ^ data[1] ^ data[2] ^ data[3] ^ data[4]);
        
        int result = RFIDSDKManager.getInstance().getRfidManager()
                .setCfgParameter((byte) 1, (byte) 14, Util.bytesToHexString(data, 0, 6), 6);
        
        if (result == 0) {
             ToastUtils.show(getString(R.string.set_success));
        } else {
             ToastUtils.show(getString(R.string.set_failed));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        
        if (id == R.id.BtInventory) {
            toggleInventory();
        } else if (id == R.id.BtClear) {
            clearData();
        }  else if (id == R.id.btn_getreadermode) {
            getReaderProtectedMode(true);
        } else if (id == R.id.btn_setreadermode) {
            setReaderProtectedMode();
        } else if (id == R.id.button_enableprotected) {
            enableTagProtection();
        } else if (id == R.id.button_disableprotected) {
            disableTagProtection();
        }else if (id == R.id.button_enable_short) {
            enableTagShortRange();
        } else if (id == R.id.button_disable_short) {
            disableTagShortRange();
        }
    }

    /**
     * 切换盘点状态
     * Switch the inventory status
     */
    private void toggleInventory() {
        if (binding.BtInventory.getText().equals(getString(R.string.btStart))) {
            startInventory();
        } else {
            stopInventory();
        }
    }

    private void startInventory() {

        addResultCallBack();
        GlobalData.epc = "";
        int result = RFIDSDKManager.getInstance().getRfidManager().startInventory();
        
        if (result == 0) {
            isReading = true;
            lastTime = System.currentTimeMillis();
            lastCount = 0;
            binding.BtInventory.setText(getString(R.string.btStop));
            setViewEnabled(false);
            startTimer();
        }
    }

    private void stopInventory() {
        if (isReading) {
            RFIDSDKManager.getInstance().getRfidManager().stopInventory();
            isReading = false;
        }
        delayStopInventoryUI(1000);
    }

    /**
     * 启动定时器
     * Switch the inventory status
     */
    private void startTimer() {
        if (timer == null) {
            beginTime = System.currentTimeMillis();
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    long readTime = System.currentTimeMillis() - beginTime;
                    Message msg = handler.obtainMessage(MSG_UPDATE_TIME, String.valueOf(readTime));
                    handler.sendMessage(msg);
                }
            }, 0, 200);
        }
    }

    /**
     * 停止定时器
     * Stop the timer
     */
    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
            binding.BtInventory.setText(getString(R.string.btStoping));
        }
    }



    /**
     * 获取读写器保护模式
     * Get the reader protection mode
     */
    private void getReaderProtectedMode(boolean isToast) {
        String[] arr = RFIDSDKManager.getInstance().getRfidManager().getReaderProtectedMode();
        if (arr != null && arr.length >= 2) {
            int index = Integer.parseInt(arr[0]);
            binding.switchSpinner.setSelection(index, true);
            binding.etPwd.setText(arr[1]);
            if (isToast) {
                ToastUtils.show(getString(R.string.get_success));
            }
        } else {
            if (isToast) {
                ToastUtils.show(getString(R.string.get_failed));
            }
        }
    }

    /**
     * 设置读写器保护模式
     * Set the reader protection mode
     */
    private void setReaderProtectedMode() {
        int enableFlag = binding.switchSpinner.getSelectedItemPosition();
        String password = binding.etPwd.getText().toString();
        
        if (TextUtils.isEmpty(password) || password.length() != PASSWORD_LENGTH) {
             ToastUtils.show(getString(R.string.set_failed));
            return;
        }
        
        int result = RFIDSDKManager.getInstance().getRfidManager()
                .setReaderProtectedMode(1, enableFlag, password);
        
        if (result == 0) {
             ToastUtils.show(getString(R.string.set_success));
        } else {
             ToastUtils.show(getString(R.string.set_failed));
        }
    }

    /**
     * 启用标签保护模式
     * Enable tag protection mode
     */
    private void enableTagProtection() {
        String password = binding.tagPwd.getText().toString();
        if (TextUtils.isEmpty(password) || password.length() != PASSWORD_LENGTH) {
            return;
        }
        
        String epc = binding.epcText.getText().toString();
        byte[] zeroPassword = new byte[4];
        
        // 先写入密码  Enter the password first
        int result = RFIDSDKManager.getInstance().getRfidManager()
                .writeTag(epc, Util.bytesToHexString(zeroPassword, 0, 4), 
                        MEM_RESERVED, WORD_PTR_PASSWORD, password);
        
        if (result == 0 || result == 0x05) {
            // 启用保护模式
            byte enableFlag = 1;
            for (int i = 0; i < 2; i++) {
                result = RFIDSDKManager.getInstance().getRfidManager()
                        .protectedMode(epc, enableFlag, password);
                if (result == 0) {
                    break;
                }
            }
            
            if (result == 0) {
                 ToastUtils.show(getString(R.string.set_success));
            } else {
                 ToastUtils.show(getString(R.string.set_failed));
            }
        } else {
             ToastUtils.show(getString(R.string.set_failed));
        }
    }

    /**
     * 禁用标签保护模式
     * Disable Tag protection mode
     */
    private void disableTagProtection() {
        String password = binding.tagPwd.getText().toString();
        if (TextUtils.isEmpty(password) || password.length() != PASSWORD_LENGTH) {
            return;
        }
        
        String epc = binding.epcText.getText().toString();
        byte enableFlag = 0;
        
        int result = RFIDSDKManager.getInstance().getRfidManager()
                .protectedMode(epc, enableFlag, password);
        
        if (result == 0) {
             ToastUtils.show(getString(R.string.set_success));
        } else {
             ToastUtils.show(getString(R.string.set_failed));
        }
    }



    /**
     * 启用标签保护模式
     * Enable tag protection mode
     */
    private void enableTagShortRange() {
        String password = binding.tagPwdShort.getText().toString();
        if (TextUtils.isEmpty(password) || password.length() != PASSWORD_LENGTH) {
            return;
        }

        String epc = binding.epcTextShort.getText().toString();
        byte[] zeroPassword = new byte[4];

        // 先写入密码  Enter the password first
        int result = RFIDSDKManager.getInstance().getRfidManager()
                .writeTag(epc, Util.bytesToHexString(zeroPassword, 0, 4),
                        MEM_RESERVED, WORD_PTR_PASSWORD, password);

        int epcNum = epc.length()/4 ;

        if (result == 0 || result == 0x05) {
            for (int i = 0; i < 2; i++) {
                result = RFIDSDKManager.getInstance().getRfidManager().
                        setShortRangeFlag(epcNum,epc, password,0,0,0,"",4,1);
                if (result == 0) {
                    break;
                }
            }

            if (result == 0) {
                ToastUtils.show(getString(R.string.set_success));
            } else {
                ToastUtils.show(getString(R.string.set_failed));
            }
        } else {
            ToastUtils.show(getString(R.string.set_failed));
        }
    }

    /**
     * 禁用标签保护模式
     * Disable Tag protection mode
     */
    private void disableTagShortRange() {
        String password = binding.tagPwdShort.getText().toString();
        if (TextUtils.isEmpty(password) || password.length() != PASSWORD_LENGTH) {
            return;
        }

        String epc = binding.epcTextShort.getText().toString();

        int epcNum = epc.length()/4 ;
        int result = RFIDSDKManager.getInstance().getRfidManager().
                setShortRangeFlag(epcNum,epc, password,0,0,0,"",4,0);

        if (result == 0) {
            ToastUtils.show(getString(R.string.set_success));
        } else {
            ToastUtils.show(getString(R.string.set_failed));
        }
    }
}

