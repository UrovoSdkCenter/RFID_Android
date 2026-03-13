package com.rfid.demo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.rfid.demo.databinding.ActivityMaskBinding;
import com.rfid.demo.utils.SpinnerHelper;
import com.rfid.demo.utils.ViewHelper;
import com.ubx.usdk.RFIDSDKManager;

public class MaskActivity extends Activity implements View.OnClickListener {
    private String TAG = MaskActivity.class.getSimpleName();
    
    // 使用 DataBinding
    private ActivityMaskBinding binding;

    private String[] strMem = new String[3];
    String MaskData = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 使用 DataBinding
        binding = ActivityMaskBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        strMem[0] = "EPC";
        strMem[1] = "TID";
        strMem[2] = "USER";
        
        // 使用 SpinnerHelper 设置 Spinner
        SpinnerHelper.setupSpinner(this, binding.memSpinner, strMem, 0);

        // 批量设置点击监听器
        ViewHelper.setOnClickListener(this, binding.buttonAdd, binding.buttonClear);
    }

    @Override
    public void onClick(View v) {
        if (v == binding.buttonAdd) {
            if ((binding.etAddr.getText() == null) || (binding.etLen.getText() == null) || (binding.etData.getText() == null))
                return;
            try {
                int maskAddr = (int) Integer.valueOf(binding.etAddr.getText().toString());
                int maskMem = binding.memSpinner.getSelectedItemPosition() + 1;
                int maskLen = (int) Integer.valueOf(binding.etLen.getText().toString());
                String strData = binding.etData.getText().toString();
                int ret = RFIDSDKManager.getInstance().getRfidManager().addMaskByBits(maskMem, maskAddr, maskLen, strData);
                if (ret == 0) {
                    String temp = maskMem + "," + maskAddr + "," + maskLen + "," + strData;
                    MaskData += (temp + "\r\n");
                    binding.txtMask.setText(MaskData);
                    Toast.makeText(
                            getApplicationContext(),
                            getString(R.string.strsuccess),
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(
                            getApplicationContext(),
                            getString(R.string.strfailed) + " " + ret,
                            Toast.LENGTH_SHORT).show();
                }
            } catch (Exception ex) {
                Toast.makeText(
                        getApplicationContext(),
                        getString(R.string.strfailed),
                        Toast.LENGTH_SHORT).show();
            }

        } else if (v == binding.buttonClear) {
            MaskData = "";
            binding.txtMask.setText("");
            RFIDSDKManager.getInstance().getRfidManager().clearMask();
            Toast.makeText(
                    getApplicationContext(),
                    getString(R.string.strsuccess),
                    Toast.LENGTH_SHORT).show();
        }
    }
}
