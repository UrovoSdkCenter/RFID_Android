package com.rfid.base;

/**
 * Tab 切换生命周期回调接口。
 * Tab switch lifecycle callback interface.
 *
 * 由于 LocalActivityManager 嵌入场景下，子 Activity 在 Tab 切换时
 * 不会触发标准的 onPause()/onResume()，通过此接口主动通知切换事件。
 * Due to the embedded scene of LocalActivityManager, when the sub Activity is switched in the Tab, it will not trigger the standard 。
 * onPause()/onResume() methods. Instead, this interface is used to actively notify the switching event.
 *
 * - onTabSelected()   : 对应 onResume()，Tab 被切换到前台时调用
 * - onTabUnselected() : 对应 onPause()，Tab 被切换离开时调用
 * - onTabSelected() : Corresponds to onResume(), called when the Tab is brought to the foreground
 * - onTabUnselected() : Corresponds to onPause(), called when the Tab is switched away
 *
 */
public interface TabLifecycleListener {

    /** Tab 切换到此界面（等效于 onResume）   Switch to this interface (equivalent to onResume) */
    void onTabSelected();

    /** Tab 从此界面切走（等效于 onPause）   Tab exits from this interface (equivalent to onPause) */
    void onTabUnselected();
}
