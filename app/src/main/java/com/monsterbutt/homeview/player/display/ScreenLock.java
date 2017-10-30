package com.monsterbutt.homeview.player.display;


import android.app.Activity;
import android.util.Log;
import android.view.WindowManager;

public class ScreenLock {

  private static final String Tag = "HV_ScreenLock";

  private boolean holdWake = false;
  private final Activity activity;

  public ScreenLock(Activity activity) {
    this.activity = activity;
  }

  public void obtain() {
      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          synchronized (this) {
            if (!holdWake) {
              activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
              holdWake = true;
              Log.d(Tag, "Stay Awake");
            }
          }
        }
      });
  }

  public synchronized void release() {
    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        synchronized (this) {
          if (holdWake){
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            holdWake = false;
            Log.d(Tag, "Clear Awake");
          }
        }
      }
    });
  }
}
