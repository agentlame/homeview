package com.monsterbutt.homeview.player;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.SubtitleDecoder;
import com.google.android.exoplayer2.text.SubtitleDecoderFactory;
import com.google.android.exoplayer2.text.TextRenderer;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import com.monsterbutt.homeview.player.renderers.DeviceAudioTrackRenderer;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

public class HomeViewPlayer extends SimpleExoPlayer {

    private static final String TAG = "HomeViewPlayer";
    private static final long DEFAULT_ALLOWED_VIDEO_JOINING_TIME_MS = 5000;

    private DeviceAudioTrackRenderer mAudioRenderer = null;
    private TextRenderer.Output imageSubsHandler = null;
    private TextRenderer.Output textOutput = null;

    private PlexVideoItem currentVideo = null;

    public HomeViewPlayer(Context context,
                             com.google.android.exoplayer2.trackselection.TrackSelector trackSelector,
                             LoadControl loadControl) {
        super(context, trackSelector, loadControl, null, EXTENSION_RENDERER_MODE_ON, DEFAULT_ALLOWED_VIDEO_JOINING_TIME_MS);
        super.setTextOutput(new TextRenderer.Output() {
            @Override
            public void onCues(List<Cue> cues) {
                if (textOutput != null)
                    textOutput.onCues(cues);
                if (imageSubsHandler != null)
                    imageSubsHandler.onCues(cues);
            }
        });
    }

    public void prepare(PlexVideoItem item, PlexServer server, Context context, ExtractorMediaSource.EventListener listener) {

        this.currentVideo = item;
        if (mAudioRenderer != null)
            mAudioRenderer.prepareVideo(currentVideo);
        super.prepare(new ExtractorMediaSource(Uri.parse(item.getVideoPath(server)),
                new DataSourceFactory(context),
                new DefaultExtractorsFactory(), null, listener));
    }

    protected void buildAudioRenderers(Context context, Handler mainHandler,
                                       DrmSessionManager<FrameworkMediaCrypto> drmSessionManager,
                                       @ExtensionRendererMode int extensionRendererMode, AudioRendererEventListener eventListener,
                                       ArrayList<Renderer> out) {

        ComponentListener componentListener = getComponentListener();
        mAudioRenderer = new DeviceAudioTrackRenderer(MediaCodecSelector.DEFAULT,
                drmSessionManager, true, mainHandler, componentListener, MediaCodecCapabilities.getInstance(context));
        out.add(mAudioRenderer);
        if (currentVideo != null)
            mAudioRenderer.prepareVideo(currentVideo);

        if (extensionRendererMode == EXTENSION_RENDERER_MODE_OFF) {
            return;
        }
        int extensionRendererIndex = out.size();
        if (extensionRendererMode == EXTENSION_RENDERER_MODE_PREFER) {
            extensionRendererIndex--;
        }

        try {
            Class<?> clazz =
                    Class.forName("com.google.android.exoplayer2.ext.opus.LibopusAudioRenderer");
            Constructor<?> constructor = clazz.getConstructor(Handler.class,
                    AudioRendererEventListener.class);
            Renderer renderer = (Renderer) constructor.newInstance(mainHandler, componentListener);
            out.add(extensionRendererIndex++, renderer);
            Log.i(TAG, "Loaded LibopusAudioRenderer.");
        } catch (ClassNotFoundException e) {
            // Expected if the app was built without the extension.
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            Class<?> clazz =
                    Class.forName("com.google.android.exoplayer2.ext.flac.LibflacAudioRenderer");
            Constructor<?> constructor = clazz.getConstructor(Handler.class,
                    AudioRendererEventListener.class);
            Renderer renderer = (Renderer) constructor.newInstance(mainHandler, componentListener);
            out.add(extensionRendererIndex++, renderer);
            Log.i(TAG, "Loaded LibflacAudioRenderer.");
        } catch (ClassNotFoundException e) {
            // Expected if the app was built without the extension.
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            Class<?> clazz =
                    Class.forName("com.google.android.exoplayer2.ext.ffmpeg.FfmpegAudioRenderer");
            Constructor<?> constructor = clazz.getConstructor(Handler.class,
                    AudioRendererEventListener.class);
            Renderer renderer = (Renderer) constructor.newInstance(mainHandler, componentListener);
            out.add(extensionRendererIndex, renderer);
            Log.i(TAG, "Loaded FfmpegAudioRenderer.");
        } catch (ClassNotFoundException e) {
            // Expected if the app was built without the extension.
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void buildTextRenderers(Context context, Handler mainHandler,
                                      @ExtensionRendererMode int extensionRendererMode, TextRenderer.Output output,
                                      ArrayList<Renderer> out) {
        out.add(new TextRenderer(output, mainHandler.getLooper(), DEFAULT));
    }

    @Override
    public void setTextOutput(TextRenderer.Output output) {
        textOutput = output;
    }

    public void setImageSubsOutput(TextRenderer.Output output) { imageSubsHandler = output;}

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
