package com.rfid.base;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.rfid.base.utils.DialogUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import com.rfid.base.databinding.ActivityFindingBinding;
import com.rfid.base.utils.ViewHelper;
import com.ubx.usdk.RFIDSDKManager;
import com.ubx.usdk.bean.Tag6C;
import com.ubx.usdk.constant.BTKeyEvent;
import com.ubx.usdk.io.GripDeviceManager;
import com.ubx.usdk.io.listener.KeyEventListener;
import com.ubx.usdk.util.SoundTool;

public class FindingActivity extends BaseActivity implements View.OnClickListener, TabLifecycleListener {
    
    private ActivityFindingBinding binding;
    
    private volatile boolean mWorking = true;
    private volatile Thread mThread = null;
    Handler handler;
    int rssi = 0;
    public boolean keyPress = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 使用 DataBinding
        binding = ActivityFindingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // 批量设置点击监听器
        ViewHelper.setOnClickListener(this, binding.btfind);
        binding.btnEpcPicker.setOnClickListener(v -> showEpcPickerDialog());
        
        handler = new Handler() {
            @SuppressLint("HandlerLeak")
            @Override
            public void handleMessage(Message msg) {
                try {
                    switch (msg.what) {
                        case 0:
                            String rssistr = msg.obj + "";
                            int rssi = (int) Integer.valueOf(rssistr);
                            binding.circleProgress.setValue((float) rssi);
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

    @Override
    protected void onResume() {
        binding.epcText.setText(InventoryActivity.epc);
        binding.circleProgress.setValue(0.00f);
        super.onResume();
    }

    /**
     * 弹出 EPC 选择 Dialog，列表来自 InventoryActivity.mlist
     */
    private void showEpcPickerDialog() {
        if (InventoryActivity.mlist.isEmpty()) {
            Toast.makeText(this, R.string.please_choice_epc, Toast.LENGTH_SHORT).show();
            return;
        }
        String[] items = InventoryActivity.mlist.toArray(new String[0]);
        DialogUtils.getInstance().showEpcPickerDialog(this, items, selected -> {
            binding.epcText.setText(selected);
            InventoryActivity.epc = selected;
        });
    }
    private boolean isOnResume = false;
    public void onTabSelected() {
        isOnResume = true;
        binding.epcText.setText(InventoryActivity.epc);
        binding.circleProgress.setValue(0.00f);
        listenerGripStatus();
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
                            if (!keyPress) {
                                keyPress = true;
                                readTag();
                            }
                        }else {
                            keyPress = false;
                        }
                    }
                }
            }
        });
    }
    @Override
    public void onTabUnselected() {
        isOnResume = false;
        stopFind();
    }

    /** 停止查找线程，重置 UI 状态  Stop the search thread and reset the UI state */
    private void stopFind() {
        if (mThread != null) {
            mWorking = false;
            try {
                mThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mThread = null;
            Message msg = handler.obtainMessage();
            msg.what = 0;
            msg.obj = 0 + "";
            handler.sendMessage(msg);
            binding.btfind.setText(R.string.finding);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopFind();
    }

    @Override
    public void onClick(View v) {
        if (v == binding.btfind) {
            if (TextUtils.isEmpty(InventoryActivity.epc)) {
                Toast.makeText(this, R.string.please_choice_epc, Toast.LENGTH_SHORT).show();
                return;
            }

            readTag();
        }
    }

    private void readTag() {
        String input =  binding.epcText.getText().toString();

        if (TextUtils.isEmpty(input)) {
            Toast.makeText(this, R.string.please_choice_epc, Toast.LENGTH_SHORT).show();
            return;
        }


        if (binding.btfind.getText().toString().equals(getString(R.string.finding))) {
            if (mThread == null) {
                mWorking = true;
                binding.btfind.setText(R.string.btstop);

                mThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (mWorking) {
                            Tag6C mtag = RFIDSDKManager.getInstance().getRfidManager().findEpc(input);
                            if (mtag != null) {

                                rssi =  mtag.rssi;
                                SoundTool.getInstance().playSound(1);
                                Message msg = handler.obtainMessage();
                                msg.what = 0;
                                msg.obj = rssi + "";
                                handler.sendMessage(msg);
                                num = 0;
                            } else {
                                num = 0;
                                if (rssi > 0)
//                                    rssi -= 5;
                                    rssi -= 20;
                                if (rssi < 0)
                                    rssi = 0;
                                Message msg = handler.obtainMessage();
                                msg = handler.obtainMessage();
                                msg.what = 0;
                                msg.obj = rssi + "";
                                handler.sendMessage(msg);
                                num++;
                            }
                        }
                    }
                });
                mThread.start();
            }
        } else {
            stopFind();
        }
    }

    private int num = 0;

    /**
     * 按键扫描RFID
     * Press button to scan RFID
     **/
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 523 && !keyPress) {
            keyPress = true;
            readTag();
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 按键扫描RFID
     * Press button to scan RFID
     **/
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == 523) {
            keyPress = false;
        }
        return super.onKeyUp(keyCode, event);
    }

}
