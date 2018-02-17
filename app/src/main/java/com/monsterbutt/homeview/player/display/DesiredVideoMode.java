package com.monsterbutt.homeview.player.display;


import android.text.TextUtils;
import android.view.Display;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import us.nineworlds.plex.rest.model.impl.Media;

public class DesiredVideoMode {

  private final int videoHeight;
  private final int videoWidth;
  private final String refreshRateFormat;
  private final String refreshRateNumber;
  private final boolean allowDeviceUpconvert;

  public DesiredVideoMode(Media media, String refreshRateNumber, boolean allowDeviceUpconvert) {
    videoHeight = Integer.parseInt(media.getHeight());
    videoWidth = Integer.parseInt(media.getWidth());
    refreshRateFormat = media.getVideoFrameRate();
    this.refreshRateNumber = refreshRateNumber;
    this.allowDeviceUpconvert = allowDeviceUpconvert;
  }

  Display.Mode getBestFitMode(Display.Mode[] possible) {

    if (TextUtils.isEmpty(refreshRateFormat) && TextUtils.isEmpty(refreshRateNumber))
      return null;

    Display.Mode ret = null;
    List<Display.Mode> matches = VideoModeDecider.getFrameRateMatches(possible,
     refreshRateFormat, refreshRateNumber);
    if (matches != null && !matches.isEmpty()) {
      List<Display.Mode> pruned = new ArrayList<>();
      for (Iterator<Display.Mode> iter = matches.iterator(); iter.hasNext(); /**/) {
        Display.Mode mode = iter.next();
        if ((!allowDeviceUpconvert &&
         frameIsLargerThanVideo(mode.getPhysicalWidth(), mode.getPhysicalHeight())) ||
         frameIsSmallerThanVideo(mode.getPhysicalWidth(), mode.getPhysicalHeight())) {
          pruned.add(mode);
          iter.remove();
        }
      }
      if (matches.isEmpty())
        matches.add(pruned.get(0));
    }
    if (matches != null && !matches.isEmpty())
      ret = matches.get(0);

    return ret;
  }
  private boolean frameIsLargerThanVideo(int frameWidth, int frameHeight) {
    return videoWidth < frameWidth && videoHeight < frameHeight;
  }

  private boolean frameIsSmallerThanVideo(int frameWidth, int frameHeight) {
    return videoWidth > frameWidth || videoHeight > frameHeight;
  }
}
