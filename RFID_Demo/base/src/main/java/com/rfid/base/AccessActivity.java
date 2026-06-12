package com.rfid.base;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.rfid.base.custom.HexInputFilter;
import com.rfid.base.databinding.ActivityAccessBinding;
import com.rfid.base.utils.DialogUtils;
import com.rfid.base.utils.SPUtils;
import com.rfid.base.utils.SpinnerHelper;
import com.rfid.base.utils.ToastUtils;
import com.rfid.base.utils.Util;
import com.rfid.base.utils.ViewHelper;
import com.ubx.usdk.RFIDSDKManager;
import com.ubx.usdk.bean.TagResult;
import com.ubx.usdk.bean.enums.MemoryBank;
import com.ubx.usdk.log.UlogManager;
import com.ubx.usdk.rfid.util.RfidErrorConstants;
import com.ubx.usdk.util.StringTool;

public class AccessActivity extends Activity implements OnClickListener, OnItemSelectedListener, TabLifecycleListener {

	private String TAG = AccessActivity.class.getSimpleName();
	
	// 使用 DataBinding
	private ActivityAccessBinding binding;
	
	private int mode;
	int selectedEd = 3;
	int selectedWhenPause = 0;
	
	private static final int CHECK_W_6B = 0;
	private static final int CHECK_R_6B = 1;
	private static final int CHECK_W_6C = 2;
	private static final int CHECK_R_6C = 3;

	private CheckBox cbAscii;
	public boolean isAsciiChecked = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// 使用 DataBinding
		binding = ActivityAccessBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		
		initView();
	}
	
	@Override
	protected void onResume() {
		binding.epcText.setText(InventoryActivity.epc);
		binding.tidText.setText(InventoryActivity.tid);
		super.onResume();
	}

	public void onTabSelected() {
		binding.epcText.setText(InventoryActivity.epc);
		binding.tidText.setText(InventoryActivity.tid);
	}

	@Override
	public void onTabUnselected() {
	}

    /**
     * 弹出 EPC 选择 Dialog，列表来自 InventoryActivity.mlist
	 * Pop up the EPC selection dialog, and the list is derived from InventoryActivity.mlist
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
			binding.etContent6c.setText(selected);
        });
    }
	
	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
	private void initView() {
		binding.etWordptr.setText("2");
		binding.etLength.setText("6");
		binding.etPwd.setText("00000000");
		
		setupSpinners();
		
		setupClickListeners();

		cbAscii = binding.cbAscii;
		cbAscii.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (buttonView.isPressed()) {
					isAsciiChecked = isChecked;
					if (isAsciiChecked){
						binding.etContent6c.setHint(R.string.write_input_ascii);
						binding.etContent6c.setInputType(InputType.TYPE_CLASS_TEXT);
						binding.etContent6c.setFilters(new InputFilter[]{});
					} else {
						binding.etContent6c.setHint(R.string.write_input_hex);
						binding.etContent6c.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
						binding.etContent6c.setFilters(new InputFilter[]{new HexInputFilter()});
					}
					binding.etContent6c.setText("");
				}
			}
		});
	}

	private void setupSpinners() {
		// 存储区 Spinner
		SpinnerHelper.setupSpinner(this, binding.memSpinner, R.array.men_select, 1);
		binding.memSpinner.setOnItemSelectedListener(this);

		// 锁定存储区 Spinner
		SpinnerHelper.setupSpinner(this, binding.lockmemSpinner, R.array.arrayLockMem, 4);

		// 锁定类型 Spinner
		SpinnerHelper.setupSpinner(this, binding.locktypeSpinner, R.array.arrayLock, 2);
	}

	/**
	 * 批量设置点击监听器
	 * Batch setting of click listener
	 */
	private void setupClickListeners() {
		ViewHelper.setOnClickListener(this,
			binding.buttonRead6c,
			binding.buttonWrite6c,
			binding.buttonWriteEpc,
			binding.buttonKill,
			binding.buttonLock,
			binding.buttonErase
		);
		binding.btnEpcPicker.setOnClickListener(v -> showEpcPickerDialog());
	}

	@SuppressLint("ResourceAsColor")
	@Override
	public void onClick(View view) {
		String tidStr = binding.tidText.getText().toString();
		int id = view.getId();
		
		if (id == R.id.button_write_6c) {
			handleWrite();
		} else if (id == R.id.button_read_6c) {
			handleRead();
		} else if (id == R.id.button_write_epc) {
			handleWriteEPC();
		} else if (id == R.id.button_kill) {
			handleKill();
		} else if (id == R.id.button_lock) {
			handleLock();
		}  else if (id == R.id.button_erase) {
			// Erase功能暂未实现
		}
	}

	/**
	 * 处理写入操作
	 * Handling write operations
	 */
	private void handleWrite() {
		if (!checkContent(CHECK_W_6C)) return;
		
		try {
			int result = 0x30;
			String strEPC = "";
			if (binding.epcText.getText() != null) {
				strEPC = binding.epcText.getText().toString();
			}

			if (binding.etContent6c.getText() == null) return;
			
			int Mem = binding.memSpinner.getSelectedItemPosition();
			int WordPtr = Integer.valueOf(binding.etWordptr.getText().toString());
			String Password = binding.etPwd.getText().toString();
			String strData = binding.etContent6c.getText().toString();
			Log.i(TAG, "onClick: strEPC : " + strEPC + "   Password : " + Password + "  strData : " + strData);

			strEPC = getEPCHex(strEPC);

			strData = getWriteHex(strData);

			if (TextUtils.isEmpty(strData)){
				ToastUtils.show(  getResources().getString(R.string.data_exchange_error ));
				return ;
			}

			result = RFIDSDKManager.getInstance().getRfidManager().writeTag(strEPC, Password, Mem, WordPtr, strData);
			Log.i(TAG, "writeTag: " + strData);

			if (result != 0) {
				ToastUtils.show(getString(R.string.write_failed) + " " + result);
				ToastUtils.show("" + RfidErrorConstants.getDescription(result));
			} else {
				ToastUtils.show(getString(R.string.write_success));
			}
		} catch (Exception ex) {
			ToastUtils.show(getString(R.string.write_failed));
		}
	}

	private String getEPCHex(String strEPC){
		boolean isAsciiCheckedInventory = SPUtils.getBoolean(SPUtils.KEY_ASCII_CHECKED, false);
		if (isAsciiCheckedInventory){
			strEPC = StringTool.asciiStringToHexString(strEPC);
			Log.i(TAG, "strEPC: " + strEPC);
		}
		return strEPC;
	}

	private String getWriteHex(String strWriteData){

		if (isAsciiChecked){
			strWriteData = StringTool.asciiStringToHexString(strWriteData);
			if (TextUtils.isEmpty(strWriteData)){
				ToastUtils.show(  getResources().getString(R.string.data_exchange_error ));
				return "";
			}
			//data 长度不够4的倍数，后面自动补0
 			//The data length is not a multiple of 4. The remaining part will be automatically padded with zeros.
			if (strWriteData.length() % 4 != 0) {
				int less = strWriteData.length() % 4;
				for (int  k = 0; k < 4 - less; k++) {
					strWriteData = "0"+strWriteData  ;
				}
			}
			Log.i(TAG, "strWriteData: " + strWriteData);
		}
			return strWriteData;
	}

	/**
	 * 处理读取操作
	 * Handling read operations
	 */
	private void handleRead() {
		if (!checkContent(CHECK_R_6C)) return;
		
		try {
			String strEPC = "";
			if (binding.epcText.getText() != null) {
				strEPC = binding.epcText.getText().toString();
			}

			int memBank = (byte) binding.memSpinner.getSelectedItemPosition();
			int wordCnt = Integer.valueOf(binding.etLength.getText().toString());
			int startAdd = Integer.valueOf(binding.etWordptr.getText().toString());
			String password = binding.etPwd.getText().toString();

			strEPC = getEPCHex(strEPC);

			TagResult tagResult = RFIDSDKManager.getInstance().getRfidManager().readTag(strEPC, memBank, startAdd, wordCnt, password);
			Log.i(TAG, "readTag: " + tagResult.code);
			if (tagResult.code != 0) {
				binding.etRead6c.setText("");
				ToastUtils.show(getString(R.string.get_failed) + " " + tagResult.code);
			} else {
				binding.etRead6c.setText(tagResult.data + "");
				ToastUtils.show(getString(R.string.get_success));
			}
		} catch (Exception ex) {
			ToastUtils.show(getString(R.string.get_failed));
		}
	}

	/**
	 * 处理写入EPC操作
	 * Handling the write operation to EPC
	 */
	private void handleWriteEPC() {
		if (!checkContent(CHECK_W_6C)) return;
		
		try {
			int result = 0x30;
			String Password = binding.etPwd.getText().toString();
			String strData = binding.etContent6c.getText().toString();

			String strEPC = "";
			if (binding.epcText.getText() != null) {
				strEPC = binding.epcText.getText().toString();
			}


			strEPC = getEPCHex(strEPC);

			strData = getWriteHex(strData);

			if (TextUtils.isEmpty(strData)){
				ToastUtils.show(  getResources().getString(R.string.data_exchange_error ));
				return ;
			}

			result = RFIDSDKManager.getInstance().getRfidManager().writeTagEpc(strEPC, Password, strData);

			if (result != 0) {
				ToastUtils.show(getString(R.string.write_failed) + " " + result);
				ToastUtils.show("" + RfidErrorConstants.getDescription(result));
			} else {
				ToastUtils.show(getString(R.string.write_success));
			}
		} catch (Exception ex) {
			ToastUtils.show(getString(R.string.write_failed));
		}
	}

	/**
	 * 处理Kill操作
	 * Handling the Kill operation
	 */
	private void handleKill() {
		try {
			int result = 0x30;
			String Password = binding.etKwd.getText().toString();
			if (Password == null || Password.length() != 8) return;
			
			String strEPC = "";
			if (binding.epcText.getText() != null) {
				strEPC = binding.epcText.getText().toString();
			}

			result = RFIDSDKManager.getInstance().getRfidManager().killTag(strEPC, Password);
			Log.i(TAG, "killTag: " + result);

			if (result != 0) {
				ToastUtils.show(getString(R.string.kill_failed) + " " + result);
				ToastUtils.show("" + RfidErrorConstants.getDescription(result));
			} else {
				ToastUtils.show(getString(R.string.kill_success));
			}
		} catch (Exception ex) {
			ToastUtils.show(getString(R.string.kill_failed));
		}
	}

	/**
	 * 处理Lock操作
	 * Handling Lock operations
	 */
	private void handleLock() {
		try {
			int result = 0x30;
			String PasswordStr = binding.etPwd.getText().toString();
			String strEPC = "";
			if (binding.epcText.getText() != null) {
				strEPC = binding.epcText.getText().toString();
			}
			
			int select = binding.lockmemSpinner.getSelectedItemPosition();
			int setprotect = binding.locktypeSpinner.getSelectedItemPosition();
			
			if (binding.checku9.isChecked()) {
				Log.i(TAG, "lockTag  U9 ");
				select = 255;
				setprotect = 255;
			}
			
			result = RFIDSDKManager.getInstance().getRfidManager().lockTag(strEPC, PasswordStr, select, setprotect);
			Log.i(TAG, "lockTag: " + result);

			if (result != 0) {
				ToastUtils.show(getString(R.string.lock_failed) + " " + result);
			} else {
				ToastUtils.show(getString(R.string.lock_success));
			}
		} catch (Exception ex) {
			ToastUtils.show(getString(R.string.lock_failed));
		}
	}


	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
		selectedEd = position;
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
	}

	private boolean checkContent(int check) {
		switch (check) {
			case CHECK_W_6C:
				if (Util.isEtEmpty(binding.etContent6c)) 
					return Util.showWarning(this, R.string.content_empty_warning);
				if (!isAsciiChecked && (binding.etContent6c.getText().toString().length() % 4) != 0)
					return Util.showWarning(this, R.string.length_content_warning);
				if (!isAsciiChecked && !(Util.isLenLegal(binding.etContent6c)))
					return Util.showWarning(this, R.string.str_lenght_odd_warning);
				if (!(Util.isLenLegal(binding.etPwd)))
					return Util.showWarning(this, R.string.str_lenght_odd_warning);
			case CHECK_R_6C:
				if (Util.isEtEmpty(binding.etWordptr)) 
					return Util.showWarning(this, R.string.wordptr_empty_warning);
				if (Util.isEtEmpty(binding.etLength)) 
					return Util.showWarning(this, R.string.length_empty_warning);
				if (Util.isEtEmpty(binding.etPwd)) 
					return Util.showWarning(this, R.string.pwd_empty_warning);
				
				if (!(Util.isLenLegal(binding.etPwd)))
					return Util.showWarning(this, R.string.str_lenght_odd_warning);
				
				break;
			default:
				break;
		}
		return true;
	}
	
	public String bytesToHexString(byte[] src, int offset, int length) {
		StringBuilder stringBuilder = new StringBuilder("");
		if (src == null || src.length <= 0) {
			return null;
		}
		for (int i = offset; i < length; i++) {
			int v = src[i] & 0xFF;
			String hv = Integer.toHexString(v);
			if (hv.length() == 1) {
				stringBuilder.append(0);
			}
			stringBuilder.append(hv);
		}
		return stringBuilder.toString();
	}

	public byte[] hexStringToBytes(String hexString) {
		if (hexString == null || hexString.equals("")) {
			return null;
		}
		hexString = hexString.toUpperCase();
		int length = hexString.length() / 2;
		char[] hexChars = hexString.toCharArray();
		byte[] d = new byte[length];
		for (int i = 0; i < length; i++) {
			int pos = i * 2;
			d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
		}
		return d;
	}
	
	private byte charToByte(char c) {
		return (byte) "0123456789ABCDEF".indexOf(c);
	}

}
