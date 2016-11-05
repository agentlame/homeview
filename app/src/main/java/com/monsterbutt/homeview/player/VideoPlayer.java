/*
 * Copyright (c) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//
//package com.monsterbutt.homeview.player;
//
//import android.media.MediaCodec.CryptoException;
//import android.media.MediaFormat;
//import android.os.Handler;
//import android.view.Surface;
//
//import com.google.android.exoplayer2.ExoPlayer;
//import com.google.android.exoplayer2.Format;
//import com.google.android.exoplayer2.audio.AudioTrack;
//import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer;
//import com.google.android.exoplayer2.text.Cue;
//import com.google.android.exoplayer2.text.TextRenderer;
//import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
//import com.google.android.exoplayer2.upstream.BandwidthMeter;
//import com.monsterbutt.homeview.R;
//import com.monsterbutt.homeview.player.ffmpeg.FfmpegTrackRenderer;
//import com.monsterbutt.homeview.plex.media.PlexVideoItem;
//
//import java.io.IOException;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.CopyOnWriteArrayList;
//
///**
// * A wrapper around {@link ExoPlayer} that provides a higher level interface. It can be prepared
// * with one of a number of {@link RendererBuilder} classes to suit different use cases (e.g. DASH,
// * SmoothStreaming and so on).
// * <p/>
// * This code was originally taken from the ExoPlayer demo application.
// */
//public class VideoPlayer {
//
//    // Constants pulled into this class for convenience.
//    public static final int STATE_IDLE = ExoPlayer.STATE_IDLE;
//    public static final int STATE_BUFFERING = ExoPlayer.STATE_BUFFERING;
//    public static final int STATE_READY = ExoPlayer.STATE_READY;
//    public static final int STATE_ENDED = ExoPlayer.STATE_ENDED;
//
//    public static final int RENDERER_COUNT = 5;
//    public static final int TYPE_VIDEO = 0;
//    public static final int TYPE_AUDIO = 1;
//    public static final int TYPE_TEXT = 2;
//    public static final int TYPE_METADATA = 3;
//    public static final int TYPE_AUDIO_FFMPEG = 4;
//
//    private static final int RENDERER_BUILDING_STATE_IDLE = 1;
//    private static final int RENDERER_BUILDING_STATE_BUILDING = 2;
//    private static final int RENDERER_BUILDING_STATE_BUILT = 3;
//
//    private final SimpleExoPlayerView player;
//
//    private RendererBuilder rendererBuilder;
//
//    private int rendererBuildingState;
//    private int lastReportedPlaybackState;
//    private boolean lastReportedPlayWhenReady;
//
//    private Surface surface;
//    private TrackRenderer videoRenderer;
//    private int videoTrackToRestore;
//
//    private PlexVideoItem mPreparedVideo;
//    private boolean backgrounded;
//
//    private final PlayerCon
//
//    public VideoPlayer(RendererBuilder rendererBuilder) {
//        this.rendererBuilder = rendererBuilder;
//
//
//        player = (SimpleExoPlayerView) findViewById(R.id.player_view);
//        player.setControllerVisibilityListener(this);
//        player.requestFocus();
//
//        playerControl = new PlayerControl(player);
//        lastReportedPlaybackState = STATE_IDLE;
//        rendererBuildingState = RENDERER_BUILDING_STATE_IDLE;
//        // Disable text initially.
//        player.setSelectedTrack(TYPE_TEXT, TRACK_DISABLED);
//    }
//
//    public int getTrackCount(int type) {
//        return player.getTrackCount(type);
//    }
//
//    public MediaFormat getTrackFormat(int type, int index) {
//        return player.getTrackFormat(type, index);
//    }
//
//    public int getSelectedTrack(int type) {
//        return player.getSelectedTrack(type);
//    }
//
//    public void setSelectedTrack(int type, int index) {
//        player.setSelectedTrack(type, index);
//        if (type == TYPE_TEXT && index < 0 && captionListener != null) {
//            captionListener.onCues(Collections.<Cue>emptyList());
//        }
//    }
//
//    public boolean getBackgrounded() {
//        return backgrounded;
//    }
//
//    public void setBackgrounded(boolean backgrounded) {
//        if (this.backgrounded == backgrounded) {
//            return;
//        }
//        this.backgrounded = backgrounded;
//        if (backgrounded) {
//            videoTrackToRestore = getSelectedTrack(TYPE_VIDEO);
//            setSelectedTrack(TYPE_VIDEO, TRACK_DISABLED);
//            blockingClearSurface();
//        } else {
//            setSelectedTrack(TYPE_VIDEO, videoTrackToRestore);
//        }
//    }
//
//    public void prepare(PlexVideoItem video) {
//        if (rendererBuildingState == RENDERER_BUILDING_STATE_BUILT) {
//            player.stop();
//        }
//        mPreparedVideo = video;
//        rendererBuilder.cancel();
//        videoFormat = null;
//        videoRenderer = null;
//        rendererBuildingState = RENDERER_BUILDING_STATE_BUILDING;
//        maybeReportPlayerState();
//        rendererBuilder.buildRenderers(this);
//    }
//
//  /* package */ void onRenderers(TrackRenderer[] renderers, BandwidthMeter bandwidthMeter) {
//        for (int i = 0; i < RENDERER_COUNT; i++) {
//            if (renderers[i] == null) {
//                // Convert a null renderer to a dummy renderer.
//                renderers[i] = new DummyTrackRenderer();
//            }
//        }
//        // Complete preparation.
//        this.videoRenderer = renderers[TYPE_VIDEO];
//        this.codecCounters = videoRenderer instanceof MediaCodecTrackRenderer
//                ? ((MediaCodecTrackRenderer) videoRenderer).codecCounters
//                : renderers[TYPE_AUDIO] instanceof MediaCodecTrackRenderer
//                ? ((MediaCodecTrackRenderer) renderers[TYPE_AUDIO]).codecCounters
//                : renderers[TYPE_AUDIO_FFMPEG] instanceof FfmpegTrackRenderer
//                ? ((FfmpegTrackRenderer) renderers[TYPE_AUDIO_FFMPEG]).codecCounters : null;
//        this.bandwidthMeter = bandwidthMeter;
//        pushSurface(false);
//        player.prepare(renderers);
//        rendererBuildingState = RENDERER_BUILDING_STATE_BUILT;
//    }
//
//    public void release() {
//        rendererBuilder.cancel();
//        rendererBuildingState = RENDERER_BUILDING_STATE_IDLE;
//        surface = null;
//        player.release();
//    }
//
//    public long getDuration() {
//        long duration = player.getDuration();
//        if (duration <= 0 && mPreparedVideo != null)
//            duration = mPreparedVideo.getDurationMs();
//        return duration;
//    }
///*
//    @Override
//    public void onDownstreamFormatChanged(int sourceId, Format format, int trigger,
//                                          long mediaTimeMs) {
//        if (infoListener == null) {
//            return;
//        }
//        if (sourceId == TYPE_VIDEO) {
//            videoFormat = format;
//            infoListener.onVideoFormatEnabled(format, trigger, mediaTimeMs);
//        } else if (sourceId == TYPE_AUDIO || sourceId == TYPE_AUDIO_FFMPEG) {
//            infoListener.onAudioFormatEnabled(format, trigger, mediaTimeMs);
//        }
//    }
//*/
//
//    public PlexVideoItem getPreparedVideo() { return mPreparedVideo; }
//}
//*/