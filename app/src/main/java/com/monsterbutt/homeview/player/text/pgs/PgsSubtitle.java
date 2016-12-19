package com.monsterbutt.homeview.player.text.pgs;

import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.ImageCue;
import com.google.android.exoplayer2.text.Subtitle;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;

import java.util.Collections;
import java.util.List;

public class PgsSubtitle implements Subtitle {

  private final List<ImageCue> cues;
  private final long[] cueTimesUs;

  public PgsSubtitle(List<ImageCue> cues) {
    this.cues = cues;
    cueTimesUs = new long[cues != null ? cues.size() : 0];
    int index = 0;
    if (cues != null) {
      for (ImageCue cue : cues)
        cueTimesUs[index++] = cue.getStartDisplayTime();
    }
  }

  @Override
  public int getNextEventTimeIndex(long timeUs) {
    int index = Util.binarySearchCeil(cueTimesUs, timeUs, false, false);
    return index < cueTimesUs.length ? index : -1;
  }

  @Override
  public int getEventTimeCount() {
return cueTimesUs.length;
}

  @Override
  public long getEventTime(int index) {
    Assertions.checkArgument(index >= 0);
    Assertions.checkArgument(index < cueTimesUs.length);
    return cueTimesUs[index];
  }

  @Override
  public List<Cue> getCues(long timeUs) {
    int index = Util.binarySearchFloor(cueTimesUs, timeUs, true, false);
    if (index == -1 || cues.get(index) == null) {
      // timeUs is earlier than the start of the first cue, or we have an empty cue.
      return Collections.emptyList();
    }
    else
      return Collections.singletonList((Cue)cues.get(index));
  }
}
