package com.monsterbutt.homeview.player;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.text.SubtitleDecoder;
import com.google.android.exoplayer2.text.SubtitleDecoderFactory;
import com.google.android.exoplayer2.util.MimeTypes;

public class HomeViewSubtitleDecoderFactory {

    public static SubtitleDecoderFactory DEFAULT = new SubtitleDecoderFactory() {

        @Override
        public boolean supportsFormat(Format format) {
            return getDecoderClass(format.sampleMimeType) != null;
        }

        @Override
        public SubtitleDecoder createDecoder(Format format) {
            try {
                Class<?> clazz = getDecoderClass(format.sampleMimeType);
                if (clazz == null) {
                    throw new IllegalArgumentException("Attempted to create decoder for unsupported format");
                }
                return clazz.asSubclass(SubtitleDecoder.class).getConstructor().newInstance();
            } catch (Exception e) {
                throw new IllegalStateException("Unexpected error instantiating decoder", e);
            }
        }

        private Class<?> getDecoderClass(String mimeType) {
            try {
                switch (mimeType) {
                    case MimeTypes.TEXT_VTT:
                        return Class.forName("com.google.android.exoplayer2.text.webvtt.WebvttDecoder");
                    case MimeTypes.APPLICATION_TTML:
                        return Class.forName("com.google.android.exoplayer2.text.ttml.TtmlDecoder");
                    case MimeTypes.APPLICATION_MP4VTT:
                        return Class.forName("com.google.android.exoplayer2.text.webvtt.Mp4WebvttDecoder");
                    case MimeTypes.APPLICATION_SUBRIP:
                        return Class.forName("com.google.android.exoplayer2.text.subrip.SubripDecoder");
                    case MimeTypes.APPLICATION_TX3G:
                        return Class.forName("com.google.android.exoplayer2.text.tx3g.Tx3gDecoder");
                    case MimeTypes.APPLICATION_CEA608:
                        return Class.forName("com.google.android.exoplayer2.text.cea.Cea608Decoder");
                    case MimeTypes.APPLICATION_PGS:
                        return Class.forName("com.monsterbutt.homeview.player.parser.PgsDecoder");
                    default:
                        return null;
                }
            } catch (ClassNotFoundException e) {
                return null;
            }
        }

    };
}
