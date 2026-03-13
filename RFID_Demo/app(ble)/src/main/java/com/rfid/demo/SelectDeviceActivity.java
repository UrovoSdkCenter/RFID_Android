package com.rfid.demo;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.ViewCompat;

import com.rfid.demo.databinding.ActivitySelectDeviceBinding;
import com.rfid.demo.utils.ViewHelper;

public class SelectDeviceActivity extends AppCompatActivity {

	// 使用 DataBinding
	private ActivitySelectDeviceBinding binding;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// 使用 DataBinding
		binding = ActivitySelectDeviceBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		setStatusBarColor(this, getResources().getColor(R.color.white_title_status));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			Window window = getWindow();
			window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
		}

		// 批量设置点击监听器
		ViewHelper.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (view == binding.cardMachine) {
					toMachine();
				}else if (view == binding.cardBle) {
					toBle();
				}
			}
		}, binding.cardMachine,binding.cardBle);
		
	}

	private static final int REQUEST_EXTERNAL_STORAGE = 1;
	private static String[] PERMISSIONS_STORAGE = {
			Manifest.permission.READ_EXTERNAL_STORAGE,
			Manifest.permission.WRITE_EXTERNAL_STORAGE
	};

	public static void verifyStoragePermissions(Activity activity) {
		// Check if we have write permission
		int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

		if (permission != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(
					activity,
					PERMISSIONS_STORAGE,
					REQUEST_EXTERNAL_STORAGE
			);
		}
	}

	private int REQUEST_CODE_PERMISSION = 100;

	@Override
	protected void onResume() {
		super.onResume();
		// getStorgePermission();
	}

	/**
	 * 获取所有文件权限
	 */
	private void getStorgePermission() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
				Environment.isExternalStorageManager()) {
			// Toast.makeText(this, getString(R.string.already_all_storage_premission), Toast.LENGTH_SHORT).show();
		} else {
			AlertDialog dialog = new AlertDialog.Builder(this)
					.setTitle(R.string.tips)//设置标题
					.setMessage(R.string.need_all_permisssion)
					.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
							intent.setData(Uri.parse("package:" + getPackageName()));
							startActivityForResult(intent, REQUEST_CODE_PERMISSION);
						}
					}).create();
			dialog.show();
		}
	}

	/**
	 * Return to exit program twice
	 * 两次返回退出程序
	 */
	private long firstTime = 0;

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		long secondTime = System.currentTimeMillis();
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (secondTime - firstTime < 2000) {
				System.exit(0);
			} else {
				Toast.makeText(this, getString(R.string.press_again_exit_app), Toast.LENGTH_SHORT).show();
				firstTime = System.currentTimeMillis();
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	private void toMachine() {
		Intent intent2 = new Intent().setClass(SelectDeviceActivity.this, SplashActivity.class);
		startActivity(intent2);
	}

	private void toBle() {
		Intent intent2 = new Intent().setClass(SelectDeviceActivity.this, BleConnectActivity.class);
		startActivity(intent2);
	}

	/**
	 * 设置状态栏颜色
	 * 
	 * @param activity
	 * @param color
	 */
	private void setStatusBarColor(Activity activity, int color) {
		Window window = activity.getWindow();
		// 取消状态栏透明
		window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
		// 添加Flag把状态栏设为可绘制模式
		window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
		// 设置状态栏颜色
		window.setStatusBarColor(color);
		// 设置系统状态栏处于可见状态
		window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
		// 让view不根据系统窗口来调整自己的布局
		ViewGroup mContentView = (ViewGroup) window.findViewById(Window.ID_ANDROID_CONTENT);
		View mChildView = mContentView.getChildAt(0);
		if (mChildView != null) {
			ViewCompat.setFitsSystemWindows(mChildView, false);
			ViewCompat.requestApplyInsets(mChildView);
		}
	}
}
