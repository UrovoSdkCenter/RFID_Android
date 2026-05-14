package com.rfid.base;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;


public class BaseActivity extends AppCompatActivity {

		@Override
		protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
//			getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
//					WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


			setStatusBarColor(this,getResources().getColor(R.color.main_color));
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				Window window = getWindow();
				window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
			}
		}
	private void setStatusBarColor(Activity activity, int color){
		Window window=activity.getWindow();
		window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
		window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
		window.setStatusBarColor(color);
		window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
		ViewGroup mContentView = (ViewGroup) window.findViewById(Window.ID_ANDROID_CONTENT);
		View mChildView = mContentView.getChildAt(0);
		if (mChildView != null) {
			ViewCompat.setFitsSystemWindows(mChildView, false);
			ViewCompat.requestApplyInsets(mChildView);
		}
	}

	protected static boolean isGpsOpen(Context context) {

		boolean isGpsEnabled = false;
		try {
			LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

			isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		} catch (Exception e) {
			Log.e("Utils", "isGpsOpen error", e);
		}
		Log.v("Utils","isGpsOpen()   isGpsEnabled : "+isGpsEnabled);
		return isGpsEnabled;
	}
	public boolean checkFilePermiss(){
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
				Environment.isExternalStorageManager()) {
		} else {
			AlertDialog dialog = new AlertDialog.Builder(this)
					.setTitle(com.rfid.base.R.string.tip)
					.setMessage(com.rfid.base.R.string.app_need_read_all_file)
					.setPositiveButton(com.rfid.base.R.string.strok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
							intent.setData(Uri.parse("package:" + getPackageName()));
							startActivityForResult(intent, 101);
						}
					}).create();
			dialog.show();
			return false;
		}

		return true;
	}
}
