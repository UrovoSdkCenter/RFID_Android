package com.rfid.base;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.LocalActivityManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;


import com.rfid.base.databinding.ActivityMainBinding;
import com.rfid.base.gen2x.ProtectedModeActivity;
import com.rfid.base.utils.SPUtils;
import com.rfid.base.utils.Util;
import com.ubx.usdk.RFIDSDKManager;
import com.ubx.usdk.bean.enums.ReaderDeviceType;
import com.ubx.usdk.bean.enums.RssiUnitType;
import com.ubx.usdk.io.GripDeviceManager;

@SuppressWarnings("deprecation")
public abstract class MainBaseActivity extends BaseMainActivity {

	// 使用 DataBinding
	public static ActivityMainBinding binding;
	
	public static TabHost myTabHost;
	public TabHost.TabSpec tabSpecSled; // TabSpec of SledActivity
	private boolean isDeviceInfoTabVisible = true; // 记录SledActivity Tab的显示状态 Record the display status of the SledActivity tab
	private static MainBaseActivity instance; // 单例引用，用于InventoryActivity调用   Singleton reference, used for InventoryActivity invocation
	private LocalActivityManager mLocalActivityManager; // 用于管理Activity  Used for managing Activities
	private String currentTabTag; // 手动维护当前 Tab tag，用于 onTabChanged 时获取上一个 tab  Manually maintain the current Tab tag, which is used to obtain the previous tab when onTabChanged occurs.

	public static Context mContext;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		instance = this;
		mContext = this.getApplicationContext();

		binding = ActivityMainBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		
		Toolbar toolbar = binding.toolbar;
		setTitleCustom();

//		setSupportActionBar(toolbar);
//
//		if (getSupportActionBar() != null) {
//			setTitleCustom();
//			getSupportActionBar().setDisplayShowTitleEnabled(true);
//		}
		
		mLocalActivityManager = new LocalActivityManager(this, false);
		mLocalActivityManager.dispatchCreate(savedInstanceState);
		
		myTabHost = (TabHost) findViewById(android.R.id.tabhost);
		myTabHost.setup(mLocalActivityManager);
		
		initTabHost();

		// 记录初始选中的 Tab tag   Record the initially selected Tab tag
		currentTabTag = myTabHost.getCurrentTabTag();

		// 设置 Tab 宽度自适应内容  Set the tab width to adapt to the content.
		adjustTabWidth(myTabHost);

		myTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
			@Override
			public void onTabChanged(String s) {
				// 通知上一个 Tab 对应的 Activity "被离开"（等效于 onPause）
				// 注意：回调触发时 getCurrentTabTag() 已是新 tag，必须用手动维护的 currentTabTag
				// Notify that the previous Tab has "been exited" (equivalent to onPause)
				// Note: When the callback is triggered, getCurrentTabTag() already returns the new tag. It is necessary to manually maintain the currentTabTag.
				if (currentTabTag != null && !currentTabTag.equals(s)) {
					android.app.Activity previousActivity = mLocalActivityManager.getActivity(currentTabTag);
					if (previousActivity instanceof TabLifecycleListener) {
						((TabLifecycleListener) previousActivity).onTabUnselected();
					}
				}
				currentTabTag = s;

				// 更新Tab颜色  Update the color of the Tab
				updateTabColors(myTabHost);
				// 让选中的 Tab 居中显示     Center the selected tab for display
				scrollToSelectedTab(myTabHost);

				// LocalActivityManager 嵌入场景下子 Activity 生命周期不可靠，
				// 切换到对应 Tab 时主动通知需要刷新的 Activity
				// In the embedded scene of LocalActivityManager, the lifecycle of the child Activity is unreliable.
				// When switching to the corresponding Tab, actively notify the Activity that needs to be refreshed.
				android.app.Activity currentActivity = mLocalActivityManager.getActivity(s);
				if (currentActivity instanceof TabLifecycleListener) {
					((TabLifecycleListener) currentActivity).onTabSelected();
				}

				monitorTabChanged(currentActivity, myTabHost.getCurrentTab(), s);
			}
		});


		onClickSledSpeaker();

		boolean iset = SPUtils.getBoolean("SetRssiTypeFleg",false);
		if (!iset){
			RFIDSDKManager.getInstance().getRfidManager().setRssiUnitType(RssiUnitType.PERCENTAGE);
			SPUtils.putBoolean("SetRssiTypeFleg",true);
		}
	}


	public abstract void initTabHost();
	public abstract void monitorTabChanged(Activity activity , int tabCurrent , String tabDes);

	public void setTitleCustom(){
		binding.appVer.setText( Util.getVersionName(mContext) );
	}




	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mLocalActivityManager != null) {
			mLocalActivityManager.dispatchResume();
		}
		// 关闭手柄按键处触发扫描头出光，长距设备，开启手柄按钮控制扫描头出光
		// （Long distance device, open the handle button to control the scanning head light）
		RFIDSDKManager.getInstance().enableScanHead(false);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mLocalActivityManager != null) {
			mLocalActivityManager.dispatchPause(isFinishing());
		}
		// 开启手柄按键处触发扫描头出光，长距设备，开启手柄按钮控制扫描头出光（Long distance device, open the handle button to control the scanning head light）
		RFIDSDKManager.getInstance().enableScanHead(true);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mLocalActivityManager != null) {
			mLocalActivityManager.dispatchDestroy(isFinishing());
		}
		// 清理单例引用，避免内存泄漏
		if (instance == this) {
			instance = null;
		}
	}

	/**
	 * Return to exit program twice
	 * 两次返回退出程序
	 */
	private long firstTime = 0;

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		long secondTime = System.currentTimeMillis();
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
			if (secondTime - firstTime < 2000) {
				RFIDSDKManager.getInstance().release();
				finish();
			} else {
				Toast.makeText(this, getString(R.string.press_again_exit_app), Toast.LENGTH_SHORT).show();
				firstTime = System.currentTimeMillis();
			}
			return true;
		}

		return super.dispatchKeyEvent(event);
	}


	/**
	 * 获取MainActivity实例
	 */
	public static MainBaseActivity getInstance() {
		return instance;
	}

	/**
	 * 获取指定 Tab tag 对应的 Activity 实例
	 * 供外部（如升级完成后刷新 UI）调用
	 */
	public Activity getTabActivity(String tabTag) {
		if (mLocalActivityManager == null) return null;
		return mLocalActivityManager.getActivity(tabTag);
	}

	/**
	 * 设置Sled标签页的显示状态    Set the display status of the Sled tab
	 * @param visible true显示，false隐藏  "True" indicates display, while "false" indicates hiding.
	 */
	public void setDeviceInfoTabVisible(boolean visible) {

		if (isDeviceInfoTabVisible == visible) {
			android.util.Log.d("MainActivity", ">>State unchanged, skipping");
			return; // 状态未改变，无需处理
		}
		
		isDeviceInfoTabVisible = visible;
		
		if (visible) {
			showDeviceInfoTab();
		} else {
			hideDeviceInfoTab();
		}
	}

	/**
	 * 显示Sled标签页
	 * Display the "Sled" tab page
	 */
	private void showDeviceInfoTab() {
		if (tabSpecSled != null && myTabHost != null) {
			// 检查是否已经存在该Tab
			//Check if the Tab already exists
			boolean tabExists = false;
			TabWidget tabWidget = myTabHost.getTabWidget();
			String sledInfoText = getString(R.string.sled_info);
			
			// 通过遍历所有Tab的文本内容来检查是否已存在
			//By traversing the text content of all Tabs to check if it already exists
			for (int i = 0; i < tabWidget.getTabCount(); i++) {
				View tabView = tabWidget.getChildTabViewAt(i);
				if (tabView != null) {
					String tabText = getTabText(tabView);
					if (sledInfoText.equals(tabText)) {
						tabExists = true;
						android.util.Log.d("MainActivity", ">>Sled tab already exists at index: " + i);
						break;
					}
				}
			}
			
			if (!tabExists) {
				myTabHost.addTab(tabSpecSled);
				// 重新设置Tab宽度和颜色
				// Reset the width and color of the tabs
				adjustTabWidth(myTabHost);
				updateTabColors(myTabHost);
			} else {
				android.util.Log.d("MainActivity", ">>Sled tab already exists, skipping add");
			}
		}
	}

	/**
	 * 隐藏Sled标签页
	 * Hide the "Sled" tab page
	 */
	private void hideDeviceInfoTab() {
		if (myTabHost != null) {
			TabWidget tabWidget = myTabHost.getTabWidget();
			
			// 查找Sled标签页的索引
			//Search for the index of the Sled tab
			int deviceInfoTabIndex = -1;
			String sledInfoText = getString(R.string.sled_info);
			
			// 通过遍历所有Tab的文本内容来查找DeviceInfo标签页
			//Search for the "DeviceInfo" tab by traversing all the text contents of the tabs.
			for (int i = 0; i < tabWidget.getTabCount(); i++) {
				View tabView = tabWidget.getChildTabViewAt(i);
				if (tabView != null) {
					// 查找Tab中的文本
					//Search for the text in the Tab.
					String tabText = getTabText(tabView);
					if (sledInfoText.equals(tabText)) {
						deviceInfoTabIndex = i;
						break;
					}
				}
			}
			
			if (deviceInfoTabIndex != -1) {
				// 如果当前选中的是DeviceInfo标签页，先切换到其他标签页
				//If the currently selected tab is the "DeviceInfo" tab, switch to another tab first.
				if (myTabHost.getCurrentTab() == deviceInfoTabIndex) {
					myTabHost.setCurrentTab(0); // 切换到第一个标签页  Switch to the first tab page
				}
				
				// 移除标签页  Remove tab page
				tabWidget.removeViewAt(deviceInfoTabIndex);
				
				// 重新设置Tab宽度和颜色  Reset the width and color of the tabs
				adjustTabWidth(myTabHost);
				updateTabColors(myTabHost);
			} else {
				android.util.Log.d("MainActivity", ">>Sled tab not found, nothing to remove");
			}
		}
	}

	/**
	 * 获取Tab的文本内容
	 * Obtain the text content of the Tab
	 */
	private String getTabText(View tabView) {
		if (tabView == null) {
			return null;
		}
		
		// 尝试多种方式获取Tab文本
		// 方式1: 查找android.R.id.title
		// Try various methods to obtain the text of the Tab
		// Method 1: Search for android.R.id.title
		TextView titleView = tabView.findViewById(android.R.id.title);
		if (titleView != null) {
			return titleView.getText().toString();
		}
		
		// 方式2: 如果Tab是TextView
		// Method 2: If the Tab is a TextView
		if (tabView instanceof TextView) {
			return ((TextView) tabView).getText().toString();
		}
		
		// 方式3: 递归查找第一个TextView
		// Method 3: Recursively search for the first TextView
		return findFirstTextView(tabView);
	}
	
	/**
	 * 递归查找第一个TextView的文本
	 * Recursively search for the text of the first TextView
	 */
	private String findFirstTextView(View view) {
		if (view instanceof TextView) {
			return ((TextView) view).getText().toString();
		} else if (view instanceof ViewGroup) {
			ViewGroup viewGroup = (ViewGroup) view;
			for (int i = 0; i < viewGroup.getChildCount(); i++) {
				String text = findFirstTextView(viewGroup.getChildAt(i));
				if (text != null && !text.isEmpty()) {
					return text;
				}
			}
		}
		return null;
	}


	public static void setIsSledVisibility(boolean isRFG91){
		if (isRFG91) {
			binding.rlRfg91.setVisibility(View.VISIBLE);
			showSledIcon();
		}else {
			binding.rlRfg91.setVisibility(View.INVISIBLE);
		}
	}

	public static void setIsSledChargingState(int isCharing){
		isSledChanging =  isCharing == 1 ;
		showSledBatteryImage();
	}
	public static void setIsSledBatteryLevel(int level){
		batreryLevelSled = level;
		showSledBatteryImage();
	}


	@SuppressLint("UseCompatLoadingForDrawables")
	private static void showSledBatteryImage(){
		Drawable drawable = null;
		if (!isSledChanging) {
			if (batreryLevelSled >= 0 && batreryLevelSled <= 9) {
				drawable = mContext.getResources().getDrawable(R.mipmap.icon_baty_0);
			} else if (batreryLevelSled <= 19) {
				drawable = mContext.getResources().getDrawable(R.mipmap.icon_baty_2);
			}else if (batreryLevelSled <= 59) {
				drawable = mContext.getResources().getDrawable(R.mipmap.icon_baty_3);
			}else if (batreryLevelSled <= 89) {
				drawable = mContext.getResources().getDrawable(R.mipmap.icon_baty_4);
			}else if (batreryLevelSled <= 100) {
				drawable = mContext.getResources().getDrawable(R.mipmap.icon_baty_5);
			}
		}else {
			drawable = mContext.getResources().getDrawable(R.mipmap.icon_baty_ischanging);
		}
		binding.rightIcon.setImageDrawable(drawable);
		binding.rightText.setText(batreryLevelSled +"%");
	}

	@SuppressLint("UseCompatLoadingForDrawables")
	private static void showSledIcon(){
		if (RFIDSDKManager.getInstance().getRfidManager()!=null){
			if (RFIDSDKManager.getInstance().getRfidManager().getReaderDeviceType() == ReaderDeviceType.BLE_DEVICE){
				binding.iconSled.setImageDrawable(mContext.getResources().getDrawable(R.mipmap.bluetooth_com));
				String mac = GripDeviceManager.getInstance().getBLEMac();
				if (!TextUtils.isEmpty(mac) && mac.length() == 17){
					binding.sledText.setText(mac.substring(12));
				}else {
					binding.sledText.setText("xx:xx");
				}
				binding.iconSpeaker.setVisibility(View.VISIBLE);
			}else if (RFIDSDKManager.getInstance().getRfidManager().getReaderDeviceType() == ReaderDeviceType.PERIPHERAL_UART){
				binding.iconSled.setImageDrawable(mContext.getResources().getDrawable(R.mipmap.icon_network));
				binding.sledText.setText("Pin");
				binding.iconSpeaker.setVisibility(View.GONE);
			}
		}

	}
	private void onClickSledSpeaker(){
		binding.iconSpeaker.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {

				long currentBeepPlayTime = System.currentTimeMillis();
				long cc = currentBeepPlayTime - beepSledPlayTime;
				if (cc < 1500){
					return;
				}
				beepSledPlayTime = currentBeepPlayTime;

				new Thread(new Runnable() {
					@Override
					public void run() {
						GripDeviceManager.getInstance().startBeep(true);
					}
				}).start();
			}
		});
	}

	private static int batreryLevelSled = 100;
	private static boolean isSledChanging = false;
	private long beepSledPlayTime = 0l;//播放Sled音频的间隔时间(The interval time for playing the Sled audio)

}
