package com.monsterbutt.homeview.player;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;

import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
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


    private DeviceAudioTrackRenderer mDeviceAudioRenderer;

    public HomeViewPlayer(Context context,
                             com.google.android.exoplayer2.trackselection.TrackSelector trackSelector,
                             LoadControl loadControl) {
        super(context, trackSelector, loadControl, null,
                EXTENSION_RENDERER_MODE_ON, ExoPlayerFactory.DEFAULT_ALLOWED_VIDEO_JOINING_TIME_MS);
    }

    public void prepare(PlexVideoItem item, PlexServer server, Context context, ExtractorMediaSource.EventListener listener) {

        mDeviceAudioRenderer.prepareVideo(item);
        super.prepare(new ExtractorMediaSource(Uri.parse(item.getVideoPath(server)),
                new DataSourceFactory(context),
                new DefaultExtractorsFactory(), null, listener));
    }

    @Override
    protected void buildAudioRenderers(Context context, Handler mainHandler,
                                       DrmSessionManager<FrameworkMediaCrypto> drmSessionManager,
                                       @ExtensionRendererMode int extensionRendererMode, AudioRendererEventListener eventListener,
                                       ArrayList<Renderer> out) {
        super.buildAudioRenderers(context, mainHandler, drmSessionManager, extensionRendererMode, eventListener, out);
        mDeviceAudioRenderer = new DeviceAudioTrackRenderer(MediaCodecSelector.DEFAULT,
                drmSessionManager, true, mainHandler, eventListener, MediaCodecCapabilities.getInstance(context));

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
    protected void buildTextRenderers(Context context, Handler mainHandler,
                                                @ExtensionRendererMode int extensionRendererMode, TextRenderer.Output output,
                                                ArrayList<Renderer> out) {
        out.add(new TextRenderer(output, mainHandler.getLooper(), DEFAULT_SUBS));
    }

    private class DataSourceFactory implements DataSource.Factory {

        private final Context context;
        private final String userAgent;
        public DataSourceFactory(Context context) {
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
}
