package com.rfid.base;

import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;


public class BaseMainActivity extends BaseActivity {

    /**
     * 调整 Tab 宽度以自适应内容
     * Adjust the width of the Tab to adapt to the content
     */
    public void adjustTabWidth(TabHost myTabHost) {
        TabWidget tabWidget = myTabHost.getTabWidget();
        for (int i = 0; i < tabWidget.getChildCount(); i++) {
            View tabView = tabWidget.getChildAt(i);
            // 设置每个 Tab 的宽度为包裹内容  Set the width of each tab to wrap the content.
            tabView.setLayoutParams(new TabWidget.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
            // 添加适当的左右内边距，让标签不会太挤   Add appropriate left and right margins to ensure that the labels are not too crowded.
            tabView.setPadding(15, tabView.getPaddingTop(), 15, tabView.getPaddingBottom());

            // 禁用Tab文本的大写转换  Disable the capitalization conversion for Tab text
            if (tabView instanceof TextView) {
                ((TextView) tabView).setAllCaps(false);
            } else {
                // 如果Tab包含TextView子控件，递归查找并设置   If the Tab contains a TextView sub-control, recursively search for it and set it.
                disableAllCapsRecursively(tabView);
            }
        }

        // 设置初始选中状态的颜色
        updateTabColors(myTabHost);
    }

    /**
     * 递归禁用ViewGroup中所有TextView的大写转换
     * Disable the capitalization conversion for all TextViews in the ViewGroup recursively
     */
    public void disableAllCapsRecursively(View view) {
        if (view instanceof TextView) {
            ((TextView) view).setAllCaps(false);
        } else if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                disableAllCapsRecursively(viewGroup.getChildAt(i));
            }
        }
    }

    /**
     * 更新Tab文字颜色，选中的显示主题色，未选中的显示默认色
     * Update the text color of the tab. Selected items will display the theme color, while unselected items will display the default color.
     */
    public void updateTabColors(TabHost myTabHost) {
        TabWidget tabWidget = myTabHost.getTabWidget();
        int currentTab = myTabHost.getCurrentTab();

        for (int i = 0; i < tabWidget.getChildCount(); i++) {
            View tabView = tabWidget.getChildAt(i);
            boolean isSelected = (i == currentTab);

            if (tabView instanceof TextView) {
                TextView textView = (TextView) tabView;
                if (isSelected) {
                    textView.setTextColor(getResources().getColor(R.color.main_color));
                } else {
                    // 设置为默认颜色（通常是黑色或系统默认）  Set as the default color (usually black or the system default)
                    textView.setTextColor(getResources().getColor(android.R.color.tab_indicator_text));
                }
            } else {
                // 如果Tab包含TextView子控件，递归查找并设置   If the Tab contains a TextView sub-control, recursively search for it and set it.
                setTabColorRecursively(tabView, isSelected);
            }
        }
    }

    /**
     * 递归设置Tab中TextView的颜色
     * Set the color of the TextViews in the Recursion tab
     */
    public void setTabColorRecursively(View view, boolean isSelected) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            if (isSelected) {
                textView.setTextColor(getResources().getColor(R.color.main_color));
            } else {
                textView.setTextColor(getResources().getColor(android.R.color.tab_indicator_text));
            }
        } else if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                setTabColorRecursively(viewGroup.getChildAt(i), isSelected);
            }
        }
    }

    /**
     * 滚动到选中的 Tab，使其居中显示
     * Scroll to the selected tab and center it for display.
     */
    public void scrollToSelectedTab(TabHost myTabHost) {
        int currentTab = myTabHost.getCurrentTab();
        TabWidget tabWidget = myTabHost.getTabWidget();

        if (currentTab < 0 || currentTab >= tabWidget.getChildCount()) {
            return;
        }

        View selectedTab = tabWidget.getChildAt(currentTab);
        HorizontalScrollView scrollView = (HorizontalScrollView) tabWidget.getParent();

        if (scrollView != null && selectedTab != null) {
            // 计算选中 Tab 的中心位置   Calculate the center position of the selected tab
            int tabCenter = selectedTab.getLeft() + selectedTab.getWidth() / 2;
            // 计算 ScrollView 的中心位置   Calculate the center position of the ScrollView
            int scrollViewCenter = scrollView.getWidth() / 2;
            // 计算需要滚动的距离，使 Tab 居中   Calculate the distance that needs to be scrolled so that the Tab key is centered.
            int scrollX = tabCenter - scrollViewCenter;

            // 平滑滚动到目标位置    Smoothly scroll to the target position
            scrollView.smoothScrollTo(scrollX, 0);
        }
    }



}
