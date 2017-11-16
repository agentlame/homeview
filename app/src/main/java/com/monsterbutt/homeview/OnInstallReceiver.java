package com.monsterbutt.homeview;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class OnInstallReceiver extends BroadcastReceiver {

  private final static String TAG = "HV_OnInstallReceiver";

  @Override
  public void onReceive(Context context, Intent intent) {

    Log.d(TAG, "onReceive(): " + intent);

    TvUtil.scheduleSyncingChannel(context);
  }
}
