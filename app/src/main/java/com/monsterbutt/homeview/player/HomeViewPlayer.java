package com.monsterbutt.homeview.player;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.util.Util;
import com.monsterbutt.homeview.player.renderers.DeviceAudioTrackRenderer;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;

import java.lang.reflect.Constructor;
import java.util.ArrayList;

public class HomeViewPlayer extends SimpleExoPlayer {

    private static final String TAG = "HomeViewPlayer";
    private static final long DEFAULT_ALLOWED_VIDEO_JOINING_TIME_MS = 5000;

    private DeviceAudioTrackRenderer mAudioRenderer = null;

    private PlexVideoItem currentVideo = null;

    public HomeViewPlayer(Context context,
                             com.google.android.exoplayer2.trackselection.TrackSelector trackSelector,
                             LoadControl loadControl) {
        super(context, trackSelector, loadControl, null, EXTENSION_RENDERER_MODE_ON, DEFAULT_ALLOWED_VIDEO_JOINING_TIME_MS);
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

        mAudioRenderer = new DeviceAudioTrackRenderer(MediaCodecSelector.DEFAULT,
                drmSessionManager, true, mainHandler, eventListener, MediaCodecCapabilities.getInstance(context));
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
            Renderer renderer = (Renderer) constructor.newInstance(mainHandler, eventListener);
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
            Renderer renderer = (Renderer) constructor.newInstance(mainHandler, eventListener);
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
            Renderer renderer = (Renderer) constructor.newInstance(mainHandler, eventListener);
            out.add(extensionRendererIndex, renderer);
            Log.i(TAG, "Loaded FfmpegAudioRenderer.");
        } catch (ClassNotFoundException e) {
            // Expected if the app was built without the extension.
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
}
