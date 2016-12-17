package com.monsterbutt.homeview.player.text.pgs;

import android.graphics.Bitmap;

import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.ImageCue;
import com.google.android.exoplayer2.text.Subtitle;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;

import java.util.Collections;
import java.util.List;

public class PgsSubtitle implements Subtitle {

  final private static byte PIXEL_ASHIFT = 24;
  final private static byte PIXEL_RSHIFT = 16;
  final private static byte PIXEL_GSHIFT = 8;
  //final private static byte PIXEL_BSHIFT = 0;

  public enum AVSubtitleType {
    SUBTITLE_NONE,
    SUBTITLE_BITMAP,
    SUBTITLE_TEXT,
    SUBTITLE_ASS
  }

  public static class AVPicture {

    // geared for pgs
    byte[] 	data = null;
    int[]   clut;
    int 	linesize;
  }

  public static class AVSubtitleRect {

    // copied/converted from libavcodec/avcodec.h
    public static int FLAG_FORCED = 0x40;

    public long start_display_time = 0;
    public long end_display_time = 0;
    public int x = 0;         ///< top left corner  of pict, undefined when pict is not set
    public int y = 0;         ///< top left corner  of pict, undefined when pict is not set
    public int w = 0;         ///< width            of pict, undefined when pict is not set
    public int h = 0;         ///< height           of pict, undefined when pict is not set
    public int nb_colors = 0; ///< number of colors in pict, undefined when pict is not set

    /**
    * can be set for text/ass as well once they where rendered
    * data+linesize for the bitmap of this subtitle.
    * can be set for text/ass as well once they where rendered
    */
    public AVPicture pict = new AVPicture();
    AVSubtitleType type = AVSubtitleType.SUBTITLE_NONE;
    public String text = "";                     ///< 0 terminated plain UTF-8 text

    /**
    * 0 terminated ASS/SSA compatible event line.
    * The presentation of this is unaffected by the other values in this
    * struct.
    */

    public int flags;
  }

  public static class PgsBuilder {

    AVSubtitleRect avSubtitleRect = new AVSubtitleRect();

    public PgsBuilder setStartTime(long startTime) {
      avSubtitleRect.start_display_time = startTime;
      return this;
    }

    public PgsBuilder setEndTime(long endTime) {
      avSubtitleRect.end_display_time = endTime;
      return this;
    }

    public PgsBuilder setX(int x) {
      avSubtitleRect.x = x;
      return this;
    }

    public PgsBuilder setY(int y) {
      avSubtitleRect.y = y;
      return this;
    }

    public PgsBuilder setWidth(int width) {
      avSubtitleRect.w = width;
      return this;
    }

    public PgsBuilder setHeight(int height) {
      avSubtitleRect.h = height;
      return this;
    }

    public PgsBuilder setType(AVSubtitleType type) {
      this.avSubtitleRect.type = type;
      return this;
    }

    public PgsBuilder setLineSize(int lineSize) {
      avSubtitleRect.pict.linesize = lineSize;
      return this;
    }

    public PgsBuilder setFlags(int flags) {
      avSubtitleRect.flags = flags;
      return this;
    }

    public byte[] initializeBitmap(int pixelCount) {
      avSubtitleRect.pict.data = new byte[pixelCount];
      return avSubtitleRect.pict.data;
    }

    public PgsBuilder setPictData(int[] pictData ) {
      avSubtitleRect.nb_colors = 256;
      avSubtitleRect.pict.clut = new int[256];
      System.arraycopy(pictData, 0, avSubtitleRect.pict.clut, 0, avSubtitleRect.nb_colors);
      return this;
    }

    public int getHeight() { return avSubtitleRect.h; }
    public int getWidth() { return avSubtitleRect.w; }

    public PgsSubtitle build() {
return new PgsSubtitle(buildCues());
}

    private List<ImageCue> buildCues() {
      return (Collections.singletonList(new ImageCue(buildBitmap(avSubtitleRect, false),
              avSubtitleRect.start_display_time, avSubtitleRect.x, avSubtitleRect.y,
              avSubtitleRect.w, avSubtitleRect.h,
              0 < (avSubtitleRect.flags & PgsSubtitle.AVSubtitleRect.FLAG_FORCED)
      )));
    }
  }

  private final List<ImageCue> cues;
  private final long[] cueTimesUs;

  private PgsSubtitle(List<ImageCue> cues) {
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

  private static Bitmap buildBitmap(PgsSubtitle.AVSubtitleRect  avSubtitleRect, boolean mergeAlpha) {
    final int height = avSubtitleRect.h;
    final int width = avSubtitleRect.w;

    int[] palette = new int[256];
    for(int i = 0; i < avSubtitleRect.nb_colors; ++i) {
      int palettePixel = avSubtitleRect.pict.clut[i];
      palette[i] = build_rgba((palettePixel >> PIXEL_ASHIFT) & 0xff
              , (palettePixel >> PIXEL_RSHIFT)    & 0xff
              , (palettePixel >> PIXEL_GSHIFT)    & 0xff
              , (palettePixel)/*>> PIXEL_BSHIFT)& 0xff */
              , mergeAlpha);
    }

    final byte[] data = avSubtitleRect.pict.data;
    final int lineSize = avSubtitleRect.pict.linesize;
    int[] argb = new int[height * width];
    for(int row = 0; row < height; ++row) {
      int rowStart = row * width;
      int dataStart = row * lineSize;
      for (int col = 0; col < width; ++col)
        argb[rowStart + col] = palette[(data[dataStart + col] & 0xFF)];
    }

    if (width == 0 || height == 0)
      return null;
    return Bitmap.createBitmap(argb, 0, width, width, height, Bitmap.Config.ARGB_8888);
  }

  static int build_rgba(int a, int r, int g, int b, boolean mergealpha) {
    if(mergealpha) {
      return a << PIXEL_ASHIFT
              | (r * a / 255) << PIXEL_RSHIFT
              | (g * a / 255) << PIXEL_GSHIFT
              | (b * a / 255);//<< PIXEL_BSHIFT;
    }
    else {
      return a << PIXEL_ASHIFT
              | r << PIXEL_RSHIFT
              | g << PIXEL_GSHIFT
              | b;//<< PIXEL_BSHIFT;
    }
  }
}
