package com.rfid.demo;

import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.rfid.demo.adapter.ScanTagAdapter;
import com.rfid.demo.bean.ReadData;
import com.rfid.demo.databinding.ActivityScanBinding;
import com.rfid.demo.utils.FileImport;
import com.rfid.demo.utils.Util;
import com.rfid.demo.utils.ViewHelper;
import com.ubx.usdk.RFIDSDKManager;
import com.ubx.usdk.bean.ReadTag;
import com.ubx.usdk.bean.enums.ReaderDeviceType;
import com.ubx.usdk.constant.BTKeyEvent;
import com.ubx.usdk.io.GripDeviceManager;
import com.ubx.usdk.io.listener.KeyEventListener;
import com.ubx.usdk.io.scan.BarcodeCallback;
import com.ubx.usdk.listener.DataCallback;
import com.ubx.usdk.util.SoundTool;
import com.ubx.usdk.util.log.LogUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ScanActivity extends Activity implements OnClickListener, OnItemClickListener{

    private String TAG = ScanActivity.class.getSimpleName();
    private int inventoryFlag = 1;
    Handler handler;
    private ArrayList<ReadData> tagList;
    ScanTagAdapter adapter;
    
    // 使用 DataBinding
    private ActivityScanBinding binding;
    
    String items[] = null;
    boolean chk[] = null;

    public static String epc;
    public static String tid;
    //private Button btnFilter;//过滤
    PopupWindow popFilter;
    public boolean isStopThread = false;
    ScanCallback callback = new ScanCallback();

    BarcodeScanCallback mBarcodeScanCallback = new BarcodeScanCallback();

    private static final int MSG_UPDATE_LISTVIEW = 0;
    private static final int MSG_UPDATE_TIME = 1;
    private static final int MSG_UPDATE_SPEED = 2;
    private static final int MSG_UPDATE_STOP = 3;
    private static final int MSG_UPDATE_LIST = 4;
    private static final int MSG_UPDATE_TV = 5;
    private Timer timer;
    public long beginTime;
    public long CardNumber;
    public static List<String> mlist = new ArrayList<String>();
    public long lastTime = 0;
    public int lastCount = 0;

    public static List<String> ledlist = new ArrayList<String>();
    public static int timeout = 300;//300 seconds

    private Context mContext;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        try {
            // 使用 DataBinding
            binding = ActivityScanBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            tagList = new ArrayList<ReadData>();
            

            // 2. 【关键】创建并设置布局管理器 (以最常用的垂直列表为例)
            binding.LvTags.setLayoutManager(new LinearLayoutManager(this));
            // 创建并配置一个垂直方向的分割线装饰
            DividerItemDecoration divider = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
            binding.LvTags.addItemDecoration(divider);
            adapter = new ScanTagAdapter(mContext,tagList);
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

            GripDeviceManager.getInstance().registerBarcodeCallback(mBarcodeScanCallback);

            if (RFIDSDKManager.getInstance().getRfidManager() != null) {
                RFIDSDKManager.getInstance().getRfidManager().addDataCallback(callback);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        String fm = RFIDSDKManager.getInstance().getRfidManager().getFirmwareVersion();
                        binding.tvFm.setText(Util.toVer(fm));
                    }
                }, 1000);

            }

            binding.LvTags.setAdapter(adapter);
            clearData();
            Log.i("MY", "UHFReadTagFragment.EtCountOfTags=" + binding.tvCount.getText());
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
                            default:
                                break;
                        }
                    } catch (Exception ex) {
                        ex.toString();
                    }
                }
            };
        } catch (Exception e) {

        }
        RFIDSDKManager.getInstance().getRfidManager().setBeepEnable(true);
    }
    public class RgInventoryCheckedListener implements RadioGroup.OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            if (checkedId == binding.RbInventorySingle.getId()) {
                inventoryFlag = 0;
            }  else if (checkedId == binding.RbInventoryLoop.getId()) {
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
    class BarcodeScanCallback implements BarcodeCallback {


        @Override
        public void onResult(byte[] barcodeByte ) {
            String barcode = new String(barcodeByte);

            ReadTag readTag = new ReadTag();
            readTag.epcId = barcode;
            readTag.rssi = 0;

            SoundTool.getInstance().playSound(1);

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

    private void itemLongClick(View view, int position){
        PopupMenu popupMenu = new PopupMenu(ScanActivity.this, view);
        popupMenu.getMenuInflater().inflate(R.menu.item_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // 根据点击的菜单项ID执行不同操作
                switch (item.getItemId()) {
                    case R.id.action_edit:
                        editItem(position);
                        return true;
                    case R.id.action_find:
                        findItem(position);
                        return true;
                    default:
                        return false;
                }
            }
        });
        popupMenu.show();
    }
    private void editItem(int position) {
            MainActivity.myTabHost.setCurrentTab(2);
    }

    private void findItem(int position) {
        MainActivity.myTabHost.setCurrentTab(1);
    }
    /**
     * 添加EPC到列表中
     *
     * @param
     */
    private void addEPCToList(ReadTag readTag) {
//        long startLong = System.currentTimeMillis();
        if (readTag != null) {

            int index = checkIsExist( readTag.epcId);


            CardNumber++;
            if (index == -1) {

                ReadData readData = new ReadData();
                readData.epcId = readTag.epcId;
                readData.memId = readTag.memId;
                readData.rssi = readTag.rssi;
                readData.BID = readTag.BID;
                readData.count = 1;

                tagList.add(readData);
//                binding.LvTags.setAdapter(adapter);
                binding.tvCount.setText("" + tagList.size());
                mlist.add(readData.epcId);
                integerHashMap.put(readTag.epcId,mlist.size()-1);
                updateList();
//                handler.sendEmptyMessage(MSG_UPDATE_TV);
            } else {
                ReadData readData1 = tagList.get(index);
                int tagcount =   readData1.count  + 1;
                readData1.count = tagcount;
                tagList.set(index, readData1);

//                long currUpdateListTime = System.currentTimeMillis();
//                if ((currUpdateListTime - lastUpdateListTime)>=200) {
//                    Log.i("MY", "notifyDataSetChanged();" + tagList.size());
//                    lastUpdateListTime = currUpdateListTime;
//                    updateList();
////                handler.sendEmptyMessage( MSG_UPDATE_LIST );
//                }

                updateList();

            }
        }
    }
    private void updateTv(){
        binding.tvCount.setText("" + tagList.size());
    }
    private void updateList(){
        binding.tvAlltag.setText(String.valueOf(CardNumber));
        adapter.notifyDataSetChanged();
    }


    private boolean isOnResume = false;
    @Override
    protected void onResume() {
        super.onResume();
        isOnResume = true;
        isStopThread = false;
        if (RFIDSDKManager.getInstance().getRfidManager().getReaderDeviceType() != ReaderDeviceType.INTEGRATED){
            listenerGripStatus();
        }
    }


    private void listenerGripStatus(){
        GripDeviceManager.getInstance().setKeyEventListener(new KeyEventListener() {
            @Override
            public void event(int keyCode, boolean isDown) {
                //keycode == BTKeyEvent.BT_SCAN   The handle button was pressed
                //isDown  true: down   false: up
                LogUtils.v(TAG,"isOnResume : "+isOnResume+"  keyCode : "+keyCode+"   isDown : "+isDown);
                if (isOnResume) {
                    if (keyCode ==  BTKeyEvent.BT_SCAN) {
                        if (isDown ) {
//                                keyPress = true;
                            readTag();
                        } else {
//                                keyPress = false;
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
            }   else if (arg0 == binding.BtImport) {
                if (tagList.size() == 0) {
                    Toast.makeText(getApplicationContext(), getString(R.string.msgNodata), Toast.LENGTH_SHORT).show();
                    return;
                }


                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
                        Environment.isExternalStorageManager()) {
                } else {
                    AlertDialog dialog = new AlertDialog.Builder(this)
                            .setTitle(R.string.tip)//设置标题
                            .setMessage(R.string.app_need_read_all_file)
                            .setPositiveButton(R.string.strok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                                    intent.setData(Uri.parse("package:" + getPackageName()));
                                    startActivityForResult(intent, 101);
                                }
                            }).create();
                    dialog.show();

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
        Log.d(TAG, "readTag()   "+getString(R.string.btInventory)+"  "+inventoryFlag);
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
                        if (timeout == 0){
                            content = getResources().getString(R.string.tips_inv);
                        }else {
                            content =String.format( getResources().getString(R.string.tips_inv_timeout),timeout);
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
            Log.d(TAG, "readTag()   stopInventory()" );
            stopInventory();
        }
    }

    private void stopInventory() {
           RFIDSDKManager.getInstance().getRfidManager().stopInventory();
            binding.tvTips.setText("");
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
            Message msg = handler.obtainMessage();
            msg.what = MSG_UPDATE_STOP;
            msg.obj = "";
            handler.sendMessage(msg);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        epc = tagList.get(position).epcId;
        tid = tagList.get(position).memId;
    }



    @Override
    protected void onPause() {
        super.onPause();
        isOnResume = false;
        stopInventory();

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
        if ( ( keyCode == 515 || keyCode == 523) && event.getRepeatCount()==0  ) {
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
        if  ( ( keyCode == 515 || keyCode == 523) && event.getRepeatCount()==0)  {
            if (!binding.BtInventory.getText().equals(getString(R.string.btInventory))) {// 识别标签
                readTag();
            }
        }
        return super.onKeyUp(keyCode, event);
    }


}
