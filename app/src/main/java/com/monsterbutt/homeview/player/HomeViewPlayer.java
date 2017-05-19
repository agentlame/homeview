package com.monsterbutt.homeview.player;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.audio.AudioProcessor;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.text.SubtitleDecoder;
import com.google.android.exoplayer2.text.SubtitleDecoderFactory;
import com.google.android.exoplayer2.text.TextRenderer;
import com.google.android.exoplayer2.text.cea.Cea608Decoder;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import com.monsterbutt.homeview.player.renderers.DeviceAudioTrackRenderer;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;

import java.util.ArrayList;

public class HomeViewPlayer extends SimpleExoPlayer {

    static public HomeViewPlayer createPlayer(Context context, TrackSelector trackSelector, LoadControl loadControl) {
        return new HomeViewPlayer(new HomeViewRendererFactory(context), trackSelector, loadControl);
    }

    private HomeViewRendererFactory mFactory;

    private HomeViewPlayer(HomeViewRendererFactory renderersFactory, TrackSelector trackSelector,
                             LoadControl loadControl) {
        super(renderersFactory, trackSelector, loadControl);
        mFactory = renderersFactory;
    }

    public void prepare(PlexVideoItem item, PlexServer server, Context context, ExtractorMediaSource.EventListener listener) {
        prepare(item, server, context, listener, false, false);
    }

    public void prepare(PlexVideoItem item, PlexServer server, Context context, ExtractorMediaSource.EventListener listener,
                        boolean resetPosition, boolean resetState) {
        mFactory.mDeviceAudioRenderer.prepareVideo(item);
        super.prepare(new ExtractorMediaSource(Uri.parse(item.getVideoPath(server)),
         new DataSourceFactory(context),
         new DefaultExtractorsFactory(), null, listener)
         , resetPosition, resetState);
    }

    private class DataSourceFactory implements DataSource.Factory {

        private final Context context;
        private final String userAgent;

        DataSourceFactory(Context context) {
            this.context = context;
            this.userAgent = Util.getUserAgent(context, "HomeView");
        }

        @Override
        public DefaultDataSource createDataSource() {
            return new DefaultDataSource(context, null, userAgent, true);
        }
    }

    private static SubtitleDecoderFactory DEFAULT_SUBS = new SubtitleDecoderFactory() {

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
                if (clazz == Cea608Decoder.class) {
                    return clazz.asSubclass(SubtitleDecoder.class).getConstructor(String.class, Integer.TYPE)
                            .newInstance(format.sampleMimeType, format.accessibilityChannel);
                } else {
                    return clazz.asSubclass(SubtitleDecoder.class).getConstructor().newInstance();
                }
            } catch (Exception e) {
                throw new IllegalStateException("Unexpected error instantiating decoder", e);
            }
        }

        private Class<?> getDecoderClass(String mimeType) {
            if (mimeType == null) {
                return null;
            }
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
                    case MimeTypes.APPLICATION_MP4CEA608:
                        return Class.forName("com.google.android.exoplayer2.text.cea.Cea608Decoder");
                    case MimeTypes.APPLICATION_PGS:
                        return Class.forName("com.monsterbutt.homeview.player.text.pgs.PgsDecoder");
                    default:
                        return null;
                }
            } catch (ClassNotFoundException e) {
                return null;
            }
        }

    };

    private static class HomeViewRendererFactory extends DefaultRenderersFactory {

        DeviceAudioTrackRenderer mDeviceAudioRenderer;

        HomeViewRendererFactory(Context context) {
            super(context, null, EXTENSION_RENDERER_MODE_ON);
        }

        @Override
        protected void buildAudioRenderers(Context context,
                                           DrmSessionManager<FrameworkMediaCrypto> drmSessionManager,
                                           AudioProcessor[] audioProcessors, Handler eventHandler,
                                           AudioRendererEventListener eventListener, @ExtensionRendererMode int extensionRendererMode,
                                           ArrayList<Renderer> out) {

            super.buildAudioRenderers(context, drmSessionManager, audioProcessors, eventHandler,
             eventListener, extensionRendererMode, out);
            mDeviceAudioRenderer = new DeviceAudioTrackRenderer(MediaCodecSelector.DEFAULT,
             drmSessionManager, true, eventHandler, eventListener, MediaCodecCapabilities.getInstance(context));

            for(Renderer renderer : out) {

                if (renderer instanceof MediaCodecAudioRenderer) {
                    int index = out.indexOf(renderer);
                    out.remove(index);
                    out.add(index, mDeviceAudioRenderer);
                    break;
                }
            }
        }

        @Override
        protected void buildTextRenderers(Context context, TextRenderer.Output output,
                                          Looper outputLooper, @ExtensionRendererMode int extensionRendererMode,
                                          ArrayList<Renderer> out) {
            out.add(new TextRenderer(output, outputLooper, DEFAULT_SUBS));
        }
    }
}
