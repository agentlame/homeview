package com.monsterbutt;


import android.app.Application;
import android.content.Context;

public class Homeview extends Application {

  private static Homeview instance;

  @Override
  public void onCreate() {
    super.onCreate();
    instance = this;
  }

  public static Context getAppContext() {
    return instance != null ? instance.getApplicationContext() : null;
  }


  private static boolean mIsPIPActive = false;
  public static synchronized boolean isPIPActive() {
    return mIsPIPActive;
  }

  public static synchronized void isPIPActive(boolean active) {
    mIsPIPActive = active;
  }
}
