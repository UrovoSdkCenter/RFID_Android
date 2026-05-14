package com.rfid.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.TabHost;

import com.rfid.base.AccessActivity;
import com.rfid.base.ConfigActivity;
import com.rfid.base.FindingActivity;
import com.rfid.base.InventoryActivity;
import com.rfid.base.LedActivity;
import com.rfid.base.MainBaseActivity;
import com.rfid.base.SledActivity;
import com.rfid.base.gen2x.ProtectedModeActivity;
import com.ubx.usdk.RFIDSDKManager;
import com.ubx.usdk.bean.enums.ModuleType;
import com.ubx.usdk.listener.InitListener;

@SuppressWarnings("deprecation")
public class MainActivity extends MainBaseActivity {

	@Override
	public void initTabHost() {
		Intent intent0 = new Intent(this, InventoryActivity.class);
		Intent intent1 = new Intent(this, AccessActivity.class);
		Intent intent2 = new Intent(this, ConfigActivity.class);
		Intent intent3 = new Intent(this, ProtectedModeActivity.class); // 内部根据profileExt决定显示内容
		Intent intent4 = new Intent(this, FindingActivity.class);
		Intent intent5 = new Intent(this, SledActivity.class);
		Intent intent6 = new Intent(this, LedActivity.class);



		TabHost.TabSpec tabSpec0 = myTabHost.newTabSpec(getString(com.rfid.base.R.string.tab_scan)).setIndicator(getString(com.rfid.base.R.string.tab_scan)).setContent(intent0);
		TabHost.TabSpec tabSpec1 = myTabHost.newTabSpec(getString(com.rfid.base.R.string.tab_rw)).setIndicator(getString(com.rfid.base.R.string.tab_rw)).setContent(intent1);
		TabHost.TabSpec tabSpec2 = myTabHost.newTabSpec(getString(com.rfid.base.R.string.tab_param)).setIndicator(getString(com.rfid.base.R.string.tab_param)).setContent(intent2);
		TabHost.TabSpec tabSpec3 = myTabHost.newTabSpec(getString(com.rfid.base.R.string.tab_gen2x)).setIndicator(getString(com.rfid.base.R.string.tab_gen2x)).setContent(intent3);
		TabHost.TabSpec tabSpec4 = myTabHost.newTabSpec(getString(com.rfid.base.R.string.finding)).setIndicator(getString(com.rfid.base.R.string.finding)).setContent(intent4);
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

	}

	@Override
	public void monitorTabChanged(Activity currentActivity , int tabCurrent, String tabDes) {

	}
}
