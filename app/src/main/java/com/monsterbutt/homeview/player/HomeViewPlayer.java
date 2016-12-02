package com.monsterbutt.homeview.player;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;

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
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.util.Util;
import com.monsterbutt.homeview.player.renderers.DeviceAudioTrackRenderer;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;

import java.util.ArrayList;

public class HomeViewPlayer extends SimpleExoPlayer {

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
        super.buildAudioRenderers(context, mainHandler, drmSessionManager, extensionRendererMode, eventListener, out);
        mAudioRenderer = new DeviceAudioTrackRenderer(MediaCodecSelector.DEFAULT,
                drmSessionManager, true, mainHandler, eventListener, MediaCodecCapabilities.getInstance(context));

        for(Renderer renderer : out) {

            if (renderer instanceof MediaCodecAudioRenderer) {
                int index = out.indexOf(renderer);
                out.remove(index);
                out.add(index, mAudioRenderer);
                break;
            }
        }
        if (currentVideo != null)
            mAudioRenderer.prepareVideo(currentVideo);
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
