package com.monsterbutt.homeview.ui.interfaces;


public interface ILifecycleListener {

    void onResume();
    void onPause();
    void onDestroyed();
    void release();
}
