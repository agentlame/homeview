package com.monsterbutt.homeview.player.text.pgs;

import com.google.android.exoplayer2.text.SimpleSubtitleDecoder;
import com.google.android.exoplayer2.text.Subtitle;
import com.google.android.exoplayer2.text.SubtitleDecoderException;
import com.google.android.exoplayer2.util.ParsableByteArray;

@SuppressWarnings("unused")
public class PgsDecoder extends SimpleSubtitleDecoder {

  @SuppressWarnings("unused")
  public PgsDecoder() {
    super("PgsDecoder");
  }

  @Override
  protected Subtitle decode(byte[] data, int size) throws SubtitleDecoderException {
    ParsableByteArray buffer = new ParsableByteArray(data, size);
    PgsBuilder builder = new PgsBuilder();
    boolean hasMore;
    do {
      hasMore = builder.readNextSection(buffer);
    } while (hasMore);
    return builder.build();
  }
}
