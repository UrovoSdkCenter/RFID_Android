package com.rfid.demo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TabHost;
import android.widget.TextView;

import com.rfid.base.AccessActivity;
import com.rfid.base.FixedActivity;
import com.rfid.base.ConfigActivity;
import com.rfid.base.FindingActivity;
import com.rfid.base.InventoryActivity;
import com.rfid.base.LedActivity;
import com.rfid.base.MainBaseActivity;
import com.rfid.base.SledActivity;
import com.rfid.base.gen2x.ProtectedModeActivity;
import com.demo.rfid.R;
import com.ubx.usdk.RFIDSDKManager;
import com.ubx.usdk.bean.enums.ModuleType;
import com.ubx.usdk.bean.enums.ReaderDeviceType;

@SuppressWarnings("deprecation")
public class MainActivity extends MainBaseActivity {

	private boolean isFixedDevice = false;
	/** 三个点菜单按钮容器
	 * Three point menu button container  */
	private FrameLayout mMenuBtnContainer;
	@Override
	public void initTabHost() {
		Intent intent0 = new Intent(this, InventoryActivity.class);
		Intent intent1 = new Intent(this, AccessActivity.class);
		Intent intent2 = new Intent(this, ConfigActivity.class);
		Intent intent3 = new Intent(this, ProtectedModeActivity.class); // 内部根据profileExt决定显示内容
		Intent intent4 = new Intent(this, FindingActivity.class);
		if (RFIDSDKManager.getInstance().isConnected()){
			if (RFIDSDKManager.getInstance().getRfidManager().getReaderDeviceType() == ReaderDeviceType.FIXED_INTEGRATED_DEVICE){
				isFixedDevice = true;
				intent4 = new Intent(this, FixedActivity.class);
			}
		}
		Intent intent5 = new Intent(this, SledActivity.class);
		Intent intent6 = new Intent(this, LedActivity.class);



		TabHost.TabSpec tabSpec0 = myTabHost.newTabSpec(getString(com.rfid.base.R.string.tab_scan)).setIndicator(getString(com.rfid.base.R.string.tab_scan)).setContent(intent0);
		TabHost.TabSpec tabSpec1 = myTabHost.newTabSpec(getString(com.rfid.base.R.string.tab_rw)).setIndicator(getString(com.rfid.base.R.string.tab_rw)).setContent(intent1);
		TabHost.TabSpec tabSpec2 = myTabHost.newTabSpec(getString(com.rfid.base.R.string.tab_param)).setIndicator(getString(com.rfid.base.R.string.tab_param)).setContent(intent2);
		TabHost.TabSpec tabSpec3 = myTabHost.newTabSpec(getString(com.rfid.base.R.string.tab_gen2x)).setIndicator(getString(com.rfid.base.R.string.tab_gen2x)).setContent(intent3);
		TabHost.TabSpec tabSpec4 ;
		if (isFixedDevice){
			 tabSpec4 = myTabHost.newTabSpec(getString(com.rfid.base.R.string.fixed)).setIndicator(getString(com.rfid.base.R.string.fixed)).setContent(intent4);
		}else {
			 tabSpec4 = myTabHost.newTabSpec(getString(com.rfid.base.R.string.finding)).setIndicator(getString(com.rfid.base.R.string.finding)).setContent(intent4);
		}
		tabSpecSled = myTabHost.newTabSpec(getString(com.rfid.base.R.string.sled_info)).setIndicator(getString(com.rfid.base.R.string.sled_info)).setContent(intent5);
		TabHost.TabSpec tabSpecLed = myTabHost.newTabSpec(getString(com.rfid.base.R.string.led_aty)).setIndicator(getString(com.rfid.base.R.string.led_aty)).setContent(intent6);

		myTabHost.addTab(tabSpec0);
		myTabHost.addTab(tabSpec1);
		myTabHost.addTab(tabSpec3);
		myTabHost.addTab(tabSpec2);
		myTabHost.addTab(tabSpec4);

		ModuleType moduleType = RFIDSDKManager.getInstance().getRfidManager().getModuleType();
		if (moduleType == ModuleType.MODULE_U3 || moduleType == ModuleType.MODULE_U4 || moduleType == ModuleType.MODULE_U5  ){
			myTabHost.addTab(tabSpecLed);
		}
		myTabHost.addTab(tabSpecSled);
		myTabHost.setCurrentTab(0);

		addMenuButtonToToolbar();
	}
// ==================== 动态添加菜单 ====================

	/**
	 * 在 ll_toolbar 末尾动态插入 菜单按钮
	 * ll_toolbar 是水平 LinearLayout，直接 addView 到最后即可靠右显示
	 */
	private void addMenuButtonToToolbar() {
		LinearLayout llToolbar = (LinearLayout) binding.getRoot()
				.findViewById(com.rfid.base.R.id.ll_toolbar);
		if (llToolbar == null) {
			Log.e("TAG", ">>addMenuButtonToToolbar: ll_toolbar not found");
			return;
		}

		// rl_rfg91 是 ll_toolbar 的第二个子 View（index=1），wrap_content
		// 给它设置 weight=1 让它弹性占用中间空间，三个点按钮才能显示在右侧
		// 注意：只改 width 和 weight，不改其他属性
		if (llToolbar.getChildCount() >= 2) {
			View rlRfg91 = llToolbar.getChildAt(1);
			LinearLayout.LayoutParams p = (LinearLayout.LayoutParams) rlRfg91.getLayoutParams();
			p.width  = 0;
			p.weight = 1;
			rlRfg91.setLayoutParams(p);
		}

		// 构建三个点按钮容器（FrameLayout，支持右上角红点）
		mMenuBtnContainer = new FrameLayout(this);
		int btnSize = dp2px(44);
		LinearLayout.LayoutParams containerParams =
				new LinearLayout.LayoutParams(btnSize, LinearLayout.LayoutParams.MATCH_PARENT);
		mMenuBtnContainer.setLayoutParams(containerParams);

		//  图标
		TextView tvDots = new TextView(this);
		tvDots.setText("⋮");
		tvDots.setTextSize(22f);
		tvDots.setTextColor(Color.WHITE);
		tvDots.setGravity(Gravity.CENTER);
		FrameLayout.LayoutParams dotsParams = new FrameLayout.LayoutParams(
				FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
		tvDots.setLayoutParams(dotsParams);
		mMenuBtnContainer.addView(tvDots);

		mMenuBtnContainer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showUpgradePopupMenu(v);
			}
		});
		llToolbar.addView(mMenuBtnContainer);
	}

	/** 弹出升级菜单 */
	private void showUpgradePopupMenu(View anchor) {
		PopupMenu popup = new PopupMenu(this, anchor);

		String upgradeTag = "  \u2B06";  // ⬆ 实心粗上箭头
		// 用 SpannableString 给箭头单独设置红色
//		String fwBase  = "固件版本升级";
//		String appBase = "软件版本升级";
//
//		if (mFirmwareHasUpdate) {
//			android.text.SpannableString fwSpan = new android.text.SpannableString(fwBase + upgradeTag);
//			fwSpan.setSpan(new android.text.style.ForegroundColorSpan(Color.parseColor("#E53935")),
//					fwBase.length(), fwBase.length() + upgradeTag.length(),
//					android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//			popup.getMenu().add(0, 1, 0, fwSpan);
//		} else {
//			popup.getMenu().add(0, 1, 0, fwBase);
//		}
//
//		if (mAppHasUpdate) {
//			android.text.SpannableString appSpan = new android.text.SpannableString(appBase + upgradeTag);
//			appSpan.setSpan(new android.text.style.ForegroundColorSpan(Color.parseColor("#E53935")),
//					appBase.length(), appBase.length() + upgradeTag.length(),
//					android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//			popup.getMenu().add(0, 2, 1, appSpan);
//		} else {
//			popup.getMenu().add(0, 2, 1, appBase);
//		}
//		popup.getMenu().add(0, 3, 2, getString(R.string.menu_about));

		popup.getMenu().add(0, 1, 0, getString(R.string.fw_update));

		popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(android.view.MenuItem item) {
				startActivity(new Intent(MainActivity.this, RFIDUpdateActivity.class));
				return false;
			}
		});
		popup.show();
	}
	private int dp2px(int dp) {
		float density = getResources().getDisplayMetrics().density;
		return (int) (dp * density + 0.5f);
	}
	@Override
	public void monitorTabChanged(Activity currentActivity , int tabCurrent, String tabDes) {

	}
}
