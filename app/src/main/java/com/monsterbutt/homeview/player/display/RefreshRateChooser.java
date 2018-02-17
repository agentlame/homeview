package com.monsterbutt.homeview.player.display;


import android.content.Context;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

class RefreshRateChooser {

  private static final String Tag = "HV_RefreshRateChooser";

  static final int DISPLAY_ID_UNKNOWN = -1;

  private static Display.Mode[] getModes(Context context) {
    WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    if (wm == null)
      return null;
    Display display = wm.getDefaultDisplay();
    return display.getSupportedModes();
  }

  static int getDefaultDisplayId(Context context) {
    WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    return manager != null && manager.getDefaultDisplay() != null ?
     manager.getDefaultDisplay().getDisplayId() : DISPLAY_ID_UNKNOWN;
  }

  static Display.Mode getCurrentMode(Context context) {
    WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    return manager != null && manager.getDefaultDisplay() != null ?
     manager.getDefaultDisplay().getMode() : null;
  }

  static Display.Mode getBestFitMode(Context context, DesiredVideoMode desired) {

    Display.Mode[] possible = getModes(context);
    if (possible == null || possible.length == 0) {
      Log.e(Tag, "Cannot find best fit because no modes");
      return null;
    }
    return desired.getBestFitMode(possible);
  }
}
