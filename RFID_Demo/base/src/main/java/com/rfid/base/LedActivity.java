package com.rfid.base;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.rfid.base.adapter.ScanTagAdapter;
import com.rfid.base.bean.FilterLed;
import com.rfid.base.bean.ReadData;
import com.rfid.base.databinding.ActivityLedBinding;
import com.rfid.base.utils.ViewHelper;
import com.ubx.usdk.RFIDSDKManager;
import com.ubx.usdk.bean.ReadTag;
import com.ubx.usdk.bean.enums.ReaderDeviceType;
import com.ubx.usdk.constant.BTKeyEvent;
import com.ubx.usdk.io.GripDeviceManager;
import com.ubx.usdk.io.listener.KeyEventListener;
import com.ubx.usdk.listener.DataCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class LedActivity extends BaseActivity implements OnClickListener, OnItemClickListener, TabLifecycleListener {

    private String TAG = LedActivity.class.getSimpleName();
    private Handler handler;
    private ArrayList<ReadData> tagList;
    private ScanTagAdapter adapter;

    private ActivityLedBinding binding;

    private String items[] = null;
    boolean chk[] = null;

    public static String epc;
    public static String tid;
    public boolean isStopThread = false;
    private ScanCallback callback = new ScanCallback();
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

    public static List<String> ledlist = new ArrayList<String>();
    public static int timeout = 300;//300 seconds

    private Context mContext;

    private CheckBox cbLed;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        try {
            binding = ActivityLedBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            tagList = new ArrayList<ReadData>();

            initFilterViews();

            binding.LvTags.setLayoutManager(new LinearLayoutManager(this));
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

            ViewHelper.setOnClickListener(this, binding.BtClear, binding.Btfilter, binding.BtInventory);

            RFIDSDKManager.getInstance().getRfidManager().getModuleType();


            if (RFIDSDKManager.getInstance().getRfidManager() != null) {
                RFIDSDKManager.getInstance().getRfidManager().addDataCallback(callback);
            }
            binding.LvTags.setAdapter(adapter);
            clearData();

            initHandler();
        } catch (Exception e) {
            e.printStackTrace();
        }
        RFIDSDKManager.getInstance().getRfidManager().setBeepEnable(true);

    }


    private void initFilterViews() {
        cbLed = binding.cbLed;


        cbLed.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isPressed()) {
                    if (isChecked) {
                        binding.llFilterTag.setVisibility(View.VISIBLE);
                    } else {
                        binding.llFilterTag.setVisibility(View.GONE);
                    }
                }
            }
        });
    }


    private void setViewEnabled(boolean enabled) {
        binding.BtClear.setEnabled(enabled);
        cbLed.setEnabled(enabled);
        if (enabled) {
            binding.Btfilter.setEnabled(enabled);
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

                            if (ledlist.size() == 0) {
                                items = new String[tagList.size()];
                                chk = new boolean[tagList.size()];
                                for (int i = 0; i < tagList.size(); i++) {
                                    items[i] = tagList.get(i).epcId;
                                    chk[i] = false;
                                }
                            }

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
        items = null;
        chk = null;
        ledlist.clear();
        Log.i("MY", "tagList.size " + tagList.size());
        adapter.notifyDataSetChanged();

    }

    private void itemLongClick(View view, int position) {
        PopupMenu popupMenu = new PopupMenu(LedActivity.this, view);
        popupMenu.getMenuInflater().inflate(R.menu.item_menu_led, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // 根据点击的菜单项ID执行不同操作
                if (item.getItemId() == R.id.action_led) {
                    String epc = tagList.get(position).epcId;
                    int retLedSingle = RFIDSDKManager.getInstance().getRfidManager().lightUpLedTag(epc, "00000000", 25500);
                    Log.e(TAG, "ItemClick   retLedSingle : " + retLedSingle);
                }
                return true;
            }
        });
        popupMenu.show();
    }


    private void addEPCToList(ReadTag readTag) {
//        long startLong = System.currentTimeMillis();
        if (readTag != null) {


            int index = checkIsExist(readTag.epcId);

            CardNumber++;
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
                listenerGripStatus();
            }
        }
    }

    @Override
    public void onTabUnselected() {
        isOnResume = false;
        cancelResultCallBack();
        stopInventory();
    }

    private void addResultCallBack(){
        if (callback == null){
            callback = new  ScanCallback();
            RFIDSDKManager.getInstance().getRfidManager().addDataCallback(callback);
        }
    }
    private void cancelResultCallBack(){
        callback  = null;
    }


    private void listenerGripStatus() {
        GripDeviceManager.getInstance().setKeyEventListener(new KeyEventListener() {
            @Override
            public void event(int keyCode, boolean isDown) {
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
            } else if (arg0 == binding.Btfilter) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                dialog.setTitle(getString(R.string.strselecttag));
                dialog.setPositiveButton(getString(R.string.strcancle), null);
                dialog.setPositiveButton(getString(R.string.strok), null);


                dialog.setMultiChoiceItems(items, chk, new DialogInterface.OnMultiChoiceClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        FilterLed led = new FilterLed();
                        led.epc = items[which];
                        led.isChedk = isChecked;
                        if (isChecked) {
                            if (ledlist.indexOf(led.epc) == -1) {
                                ledlist.add(led.epc);
                            }
                        } else {
                            if (ledlist.indexOf(led.epc) != -1)
                                ledlist.remove(led.epc);
                        }
                        chk[which] = isChecked;
                    }
                }).create();
                dialog.show();
            }
        } catch (Exception e) {
            stopInventory();
        }
    }

    private void readTag() {
        epc = "";
        tid = "";
        if (binding.BtInventory.getText().equals(getString(R.string.btInventory)))// 识别标签
        {
            addResultCallBack();

            int result = -1;
            if (cbLed.isChecked()) {
                int mtype = binding.spfactory.getSelectedItemPosition();
                result = RFIDSDKManager.getInstance().getRfidManager().startInventoryLed(mtype, ledlist);
            } else {
                result = RFIDSDKManager.getInstance().getRfidManager().startInventory();
            }

            if (result == 0) {
                String content = getResources().getString(R.string.tips_inv);
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
        } else {// 停止识别
            Log.d(TAG, "readTag()   stopInventory()");
            stopInventory();
        }
    }

    private void stopInventory() {
        if (binding != null && binding.tvTips.isAttachedToWindow()) {
            if (cbLed.isChecked()) {
                RFIDSDKManager.getInstance().getRfidManager().stopInventoryLed();
            } else {
                RFIDSDKManager.getInstance().getRfidManager().stopInventory();
            }
            binding.tvTips.setText("");
        } else {
            RFIDSDKManager.getInstance().getRfidManager().stopInventory();
        }
        delayStopInventoryUI(1000);
    }


    private long lastSoundTime = 0l;

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

    private void delayStopInventoryUI(int delaytime){
        handler.removeMessages(MSG_UPDATE_STOP);
        handler.sendEmptyMessageDelayed(MSG_UPDATE_STOP,delaytime);
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == 515 || keyCode == 523) && event.getRepeatCount() == 0) {
            if (binding.BtInventory.getText().equals(getString(R.string.btInventory))) {// 识别标签
                readTag();
            }

        }
        return super.onKeyDown(keyCode, event);
    }

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
