package com.monsterbutt.homeview.player.display;


import android.view.Display;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class VideoModeDecider {

  private static final float UNKNOWN = (float)0.0;
  private static final float FILM = (float)23.976;
  private static final float DIGITAL_FILM = (float) 24.000;
  private static final float PAL_FILM = (float)25.000;
  private static final float NTSC_INTERLACED = (float)29.970;
  private static final float DIGITAL_30 = (float)30.000;
  private static final float PAL = (float)50.000;
  private static final float NTSC = (float)59.940;
  private static final float DIGITAL_60 = (float) 60.000;

  private static List<Display.Mode> hasFrameRate(Display.Mode[] possible, double desired) {

    List<Display.Mode> match = new ArrayList<>();
    for (Display.Mode aPossible : possible) {
      double curr = aPossible.getRefreshRate();
      if (curr == desired) {
        match.add(aPossible);
      }
      else if (Math.floor(desired) == Math.floor(curr) || Math.ceil(desired) == Math.floor(curr)) {
        double discrepancy = getFrameDiff(desired, curr);
        if (.25 > discrepancy) {
          match.add(aPossible);
        }
      }
    }
    if (match.isEmpty())
      return null;
    Collections.sort(match, new CompareModesByFrameRate(desired));
    return match;
  }

  private static double getFrameDiff(double a, double b) {
    return  Math.abs(a - b);
  }

  private static List<Display.Mode> findBestForTwoPossibleFrames(Display.Mode[] possible, double desired, double backup) {

    List<Display.Mode> matchA = hasFrameRate(possible, desired);
    if (null != matchA && desired == matchA.get(0).getRefreshRate())
      return matchA;
    List<Display.Mode> matchB = hasFrameRate(possible, backup);
    if (UNKNOWN != backup && null != matchB) {
      if (null != matchA) {

        double discrepencyA = getFrameDiff(desired, matchA.get(0).getRefreshRate());
        double discrepencyB = getFrameDiff(desired, matchB.get(0).getRefreshRate());
        if (discrepencyA < discrepencyB)
          return matchA;
        return matchB;
      }
      else
        return matchB;
    }
    else if (null != matchA)
      return matchA;
    return null;
  }

  private static float convertFrameRate(String frameRateFormat, String desiredFrameRateNumber) {

    if (desiredFrameRateNumber.startsWith("50") ||
     frameRateFormat.equals("PAL") || frameRateFormat.startsWith("50"))
      return PAL;
    else if (desiredFrameRateNumber.startsWith("23") ||
     frameRateFormat.startsWith("23") || frameRateFormat.equals("24p"))
      return FILM;
    else if (desiredFrameRateNumber.startsWith("24"))
      return DIGITAL_FILM;
    else if (desiredFrameRateNumber.startsWith("29") || frameRateFormat.startsWith("29"))
      return NTSC_INTERLACED;
    else if (desiredFrameRateNumber.startsWith("59") ||
     frameRateFormat.equals("NTSC") || frameRateFormat.startsWith("59"))
      return NTSC;
    else if (desiredFrameRateNumber.startsWith("25") || frameRateFormat.startsWith("25"))
      return PAL_FILM;
    else if (desiredFrameRateNumber.startsWith("30") || frameRateFormat.startsWith("30"))
      return DIGITAL_30;
    else if (desiredFrameRateNumber.startsWith("60") || frameRateFormat.startsWith("60"))
      return DIGITAL_60;
    return UNKNOWN;
  }

  static List<Display.Mode> getFrameRateMatches(Display.Mode[] modes,
                                                   String refreshRateFormat,
                                                   String refreshRateNumber) {
    double desired = convertFrameRate(refreshRateFormat, refreshRateNumber);
    List<Display.Mode> ret = null;
    if (desired == DIGITAL_60)
      ret = findBestForTwoPossibleFrames(modes, DIGITAL_60, NTSC);
    else if (desired == NTSC)
      ret = findBestForTwoPossibleFrames(modes, NTSC, DIGITAL_60);
    else if (desired == PAL)
      ret = findBestForTwoPossibleFrames(modes, PAL, UNKNOWN);
    else if (desired == DIGITAL_30) {
      ret = findBestForTwoPossibleFrames(modes, DIGITAL_30, DIGITAL_60);
      if (ret == null)
        ret = findBestForTwoPossibleFrames(modes, NTSC, NTSC_INTERLACED);
    }
    else if (desired == NTSC_INTERLACED) {
      ret = findBestForTwoPossibleFrames(modes, NTSC, NTSC_INTERLACED);
      if (ret == null)
        ret = findBestForTwoPossibleFrames(modes, DIGITAL_30, DIGITAL_60);
    }
    else if (desired == PAL_FILM)
      ret = findBestForTwoPossibleFrames(modes, PAL_FILM, PAL);
    else if (desired == FILM)
      ret = findBestForTwoPossibleFrames(modes, FILM, DIGITAL_FILM);
    else if (desired == DIGITAL_FILM)
      ret = findBestForTwoPossibleFrames(modes, DIGITAL_FILM, FILM);

    return ret;
  }

  private static class CompareModesByFrameRate implements Comparator<Display.Mode> {

    private final double desiredRate;

    CompareModesByFrameRate(double desiredRate) {
      this.desiredRate = desiredRate;
    }
    @Override
    public int compare(Display.Mode a, Display.Mode b) {

      double diffA = getFrameDiff(desiredRate, a.getRefreshRate());
      double diffB = getFrameDiff(desiredRate, b.getRefreshRate());
      if (diffA < diffB)
        return -1;
      else if (diffB < diffA)
        return 1;
      if (a.getPhysicalWidth() > b.getPhysicalWidth())
        return -1;
      if (b.getPhysicalWidth() > a.getPhysicalWidth())
        return 1;
      if (a.getPhysicalHeight() > b.getPhysicalHeight())
        return -1;
      if (b.getPhysicalHeight() > a.getPhysicalHeight())
        return 1;
      return 0;
    }
  }
}
