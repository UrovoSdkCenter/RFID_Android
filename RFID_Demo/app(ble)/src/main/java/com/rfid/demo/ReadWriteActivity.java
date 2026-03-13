package com.rfid.demo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;

import com.rfid.demo.databinding.ActivityReadWriteBinding;
import com.rfid.demo.utils.MsgShow;
import com.rfid.demo.utils.SpinnerHelper;
import com.rfid.demo.utils.ToastUtils;
import com.rfid.demo.utils.Util;
import com.rfid.demo.utils.ViewHelper;
import com.ubx.usdk.RFIDSDKManager;
import com.ubx.usdk.bean.TagResult;
import com.ubx.usdk.rfid.util.RfidErrorConstants;

public class ReadWriteActivity extends Activity implements OnClickListener, OnItemSelectedListener {

	private String TAG = ReadWriteActivity.class.getSimpleName();
	
	// 使用 DataBinding
	private ActivityReadWriteBinding binding;
	
	private int mode;
	int selectedEd = 3;
	int selectedWhenPause = 0;
	
	private static final int CHECK_W_6B = 0;
	private static final int CHECK_R_6B = 1;
	private static final int CHECK_W_6C = 2;
	private static final int CHECK_R_6C = 3;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// 使用 DataBinding
		binding = ActivityReadWriteBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		
		initView();
	}
	
	@Override
	protected void onResume() {
		binding.epcText.setText(ScanActivity.epc);
		binding.tidText.setText(ScanActivity.tid);
		super.onResume();
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
		// 设置默认值
		binding.etWordptr.setText("2");
		binding.etLength.setText("6");
		binding.etPwd.setText("00000000");
		
		// 初始化 Spinner
		setupSpinners();
		
		// 批量设置点击监听器
		setupClickListeners();
	}

	/**
	 * 初始化所有 Spinner
	 */
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
	 */
	private void setupClickListeners() {
		ViewHelper.setOnClickListener(this,
			binding.buttonRead6c,
			binding.buttonWrite6c,
			binding.buttonWriteEpc,
			binding.buttonKill,
			binding.buttonLock,
			binding.buttonLed,
			binding.buttonErase
		);
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
		} else if (id == R.id.button_led) {
			handleLed();
		} else if (id == R.id.button_erase) {
			// Erase功能暂未实现
		}
	}

	/**
	 * 处理写入操作
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
			int Num = Integer.valueOf(binding.etLength.getText().toString());
			String Password = binding.etPwd.getText().toString();
			String strData = binding.etContent6c.getText().toString();
			Log.i(TAG, "onClick: strEPC : " + strEPC + "   Password : " + Password + "  strData : " + strData);

			result = RFIDSDKManager.getInstance().getRfidManager().writeTag(strEPC, Password, Mem, WordPtr, strData);
			Log.i(TAG, "writeTag: " + strData);

			if (result != 0) {
				MsgShow.writelogErr(getString(R.string.write_failed) + " " + result, binding.rwResult);
				ToastUtils.show("" + RfidErrorConstants.getDescription(result));
			} else {
				MsgShow.writelogSuc(getString(R.string.write_success), binding.rwResult);
			}
		} catch (Exception ex) {
			MsgShow.writelogErr(getString(R.string.write_failed), binding.rwResult);
		}
	}

	/**
	 * 处理读取操作
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

			TagResult tagResult = RFIDSDKManager.getInstance().getRfidManager()
				.readTag(strEPC, memBank, startAdd, wordCnt, password);
			Log.i(TAG, "readTag: " + tagResult.code);

			if (tagResult.code != 0) {
				binding.etRead6c.setText("");
				MsgShow.writelogErr(getString(R.string.get_failed) + " " + tagResult.code, binding.rwResult);
				ToastUtils.show("" + RfidErrorConstants.getDescription(tagResult.code));
			} else {
				binding.etRead6c.setText(tagResult.data + "");
				MsgShow.writelogSuc(getString(R.string.get_success), binding.rwResult);
			}
		} catch (Exception ex) {
			MsgShow.writelogErr(getString(R.string.get_failed), binding.rwResult);
		}
	}

	/**
	 * 处理写入EPC操作
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

			result = RFIDSDKManager.getInstance().getRfidManager().writeTagEpc(strEPC, Password, strData);

			if (result != 0) {
				MsgShow.writelogErr(getString(R.string.write_failed) + " " + result, binding.rwResult);
				ToastUtils.show("" + RfidErrorConstants.getDescription(result));
			} else {
				MsgShow.writelogSuc(getString(R.string.write_success), binding.rwResult);
			}
		} catch (Exception ex) {
			MsgShow.writelogErr(getString(R.string.write_failed), binding.rwResult);
		}
	}

	/**
	 * 处理Kill操作
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
				MsgShow.writelogErr(getString(R.string.kill_failed) + " " + result, binding.rwResult);
				ToastUtils.show("" + RfidErrorConstants.getDescription(result));
			} else {
				MsgShow.writelogSuc(getString(R.string.kill_success), binding.rwResult);
			}
		} catch (Exception ex) {
			MsgShow.writelogErr(getString(R.string.kill_failed), binding.rwResult);
		}
	}

	/**
	 * 处理Lock操作
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
				MsgShow.writelogErr(getString(R.string.lock_failed) + " " + result, binding.rwResult);
				ToastUtils.show("" + RfidErrorConstants.getDescription(result));
			} else {
				MsgShow.writelogSuc(getString(R.string.lock_success), binding.rwResult);
			}
		} catch (Exception ex) {
			MsgShow.writelogErr(getString(R.string.lock_failed), binding.rwResult);
		}
	}

	/**
	 * 处理LED操作
	 */
	private void handleLed() {
		try {
			int result = 0x30;
			String PasswordStr = binding.etPwd.getText().toString();
			String strEPC = "";
			if (binding.epcText.getText() != null) {
				strEPC = binding.epcText.getText().toString();
			}
			
			result = RFIDSDKManager.getInstance().getRfidManager()
				.lightUpLedTag(strEPC, PasswordStr, 25500); // 最大 25500ms
			
			if (result != 0) {
				MsgShow.writelogErr(getString(R.string.Optfailed) + " " + result, binding.rwResult);
			} else {
				MsgShow.writelogSuc(getString(R.string.Optsuccess), binding.rwResult);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			MsgShow.writelogErr(getString(R.string.Optfailed), binding.rwResult);
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
				if ((binding.etContent6c.getText().toString().length() % 4) != 0)
					return Util.showWarning(this, R.string.length_content_warning);
				if (!(Util.isLenLegal(binding.etContent6c)))
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
