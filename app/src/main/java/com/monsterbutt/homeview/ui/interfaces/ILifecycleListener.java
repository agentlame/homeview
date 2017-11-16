package com.monsterbutt.homeview.ui.interfaces;


import com.monsterbutt.homeview.ui.UILifecycleManager;

public interface ILifecycleListener {

    void onResume(UILifecycleManager lifecycleMgr);
    void onPause(UILifecycleManager lifecycleMgr);
    void onDestroyed(UILifecycleManager lifecycleMgr);
}
