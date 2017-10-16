package com.monsterbutt.homeview.player.display;


import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

class RefreshRateChooser {

  private static final String Tag = "HV_RefreshRateChooser";

  static final int DISPLAY_ID_UNKNOWN = -1;

  private static final float UNKNOWN = (float)0.0;
  private static final float FILM = (float)23.976;
  private static final float PAL_FILM = (float)25.0;
  private static final float NTSC_INTERLACED = (float)29.97;
  private static final float DIGITAL_30 = (float)30.0;
  private static final float PAL = (float)50.0;
  private static final float NTSC = (float)59.94;
  private static final float DIGITAL_60 = (float) 60.0;

  private static final int NO_MATCH = -1;

  private static int hasFrameRate(Display.Mode[] possible, double desired) {

    int match = NO_MATCH;
    double matchDiff = 10000;
    for (int i = 0; i < possible.length; ++i) {

      double curr = possible[i].getRefreshRate();
      if (curr == desired)
        return i;

      if (Math.floor(desired) == Math.floor(curr)) {

        double discrepency = getFrameDiff(desired, curr);
        if (matchDiff > discrepency) {

          matchDiff = discrepency;
          match = i;
        }
      }
      else if (Math.ceil(desired) == Math.floor(curr)) {

        double discrepency = getFrameDiff(desired, curr);
        if (matchDiff > discrepency) {

          matchDiff = discrepency;
          match = i;
        }
      }

    }
    return match;
  }

  private static int findBestForTwoPossibleFrames(Display.Mode[] possible, double desired, double backup) {

    int matchA = hasFrameRate(possible, desired);
    if (NO_MATCH != matchA && desired == possible[matchA].getRefreshRate())
      return matchA;
    int matchB = hasFrameRate(possible, backup);
    if (UNKNOWN != backup && NO_MATCH != matchB) {
      if (NO_MATCH != matchA) {

        double discrepencyA = getFrameDiff(desired, possible[matchA].getRefreshRate());
        double discrepencyB = getFrameDiff(desired, possible[matchB].getRefreshRate());
        if (discrepencyA < discrepencyB)
          return matchA;
        return matchB;
      }
      else
        return matchB;
    }
    else if (NO_MATCH != matchA)
      return matchA;
    return -1;
  }


  private static float convertFrameRate(String frameRate) {

    float ret = UNKNOWN;
    if (TextUtils.isEmpty(frameRate))
      return UNKNOWN;
    if (frameRate.equals("PAL") || frameRate.startsWith("50"))
      ret = PAL;
    else if (frameRate.equals("24p") || frameRate.startsWith("23"))
      ret = FILM;
    else if (frameRate.equals("NTSC") || frameRate.startsWith("59"))
      ret = NTSC;
    else if (frameRate.startsWith("25"))
      ret = PAL_FILM;
    else if (frameRate.startsWith("29"))
      ret = NTSC_INTERLACED;
    else if (frameRate.startsWith("30"))
      ret = DIGITAL_30;
    else if (frameRate.startsWith("60"))
      ret = DIGITAL_60;
    return ret;
  }

  private static double getFrameDiff(double a, double b) {
    return  Math.abs(a - b);
  }

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

  static Display.Mode getBestFitMode(Context context, String desiredFrameRate) {

    double desired = convertFrameRate(desiredFrameRate);
    Display.Mode[] possible = getModes(context);
    if (possible == null || possible.length == 0) {
      Log.e(Tag, "Cannot find best fit because no modes");
      return null;
    }

    int ret = -1;
    if (desired == DIGITAL_60)
      ret = findBestForTwoPossibleFrames(possible, DIGITAL_60, NTSC);
    else if (desired == NTSC)
      ret = findBestForTwoPossibleFrames(possible, NTSC, DIGITAL_60);
    else if (desired == PAL)
      ret = findBestForTwoPossibleFrames(possible, PAL, UNKNOWN);
    else if (desired == DIGITAL_30) {
      ret = findBestForTwoPossibleFrames(possible, DIGITAL_30, DIGITAL_60);
      if (ret == -1)
        ret =findBestForTwoPossibleFrames(possible, NTSC_INTERLACED, NTSC);
    }
    else if (desired == NTSC_INTERLACED) {
      ret = findBestForTwoPossibleFrames(possible, NTSC_INTERLACED, NTSC);
      if (ret == -1)
        ret = findBestForTwoPossibleFrames(possible, DIGITAL_30, DIGITAL_60);
    }
    else if (desired == PAL_FILM)
      ret = findBestForTwoPossibleFrames(possible, PAL_FILM, PAL);
    else if (desired == FILM)
      ret = findBestForTwoPossibleFrames(possible, FILM, UNKNOWN);

    return ret != -1 ? possible[ret] : null;
  }
}
