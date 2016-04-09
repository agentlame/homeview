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

package com.monsterbutt.homeview.player;

import android.app.Activity;
import android.content.Context;
import android.media.MediaCodec;
import android.net.Uri;
import android.text.TextUtils;
import android.view.Display;
import android.view.WindowManager;

import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.extractor.Extractor;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.text.TextTrackRenderer;
import com.google.android.exoplayer.upstream.Allocator;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.monsterbutt.homeview.player.parser.PgsParser;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.settings.SettingsManager;

import java.util.Timer;
import java.util.TimerTask;

/**
 * A {@link VideoPlayer.RendererBuilder} for streams that can be read using an {@link Extractor}.
 * <p/>
 * This code was originally taken from the ExoPlayer demo application.
 */
public class ExtractorRendererBuilder implements VideoPlayer.RendererBuilder {

    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int BUFFER_SEGMENT_COUNT = 256;

    private static final float UNKNOWN = (float)0.0;
    private static final float FILM = (float)23.976;
    private static final float PAL_FILM = (float)25.0;
    private static final float NTSC_INTERLACED = (float)29.97;
    private static final float DIGITAL_30 = (float)30.0;
    private static final float PAL = (float)50.0;
    private static final float NTSC = (float)59.94;
    private static final float DIGITAL_60 = (float) 60.0;

    private static final int   RefreshRateSwitchDelay = 3000;

    private final Activity activity;
    private final String userAgent;
    private final Uri uri;

    public ExtractorRendererBuilder(Activity activity, String userAgent, Uri uri) {
        this.activity = activity;
        this.userAgent = userAgent;
        this.uri = uri;
    }

    @Override
    public void buildRenderers(final VideoPlayer player) {

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setupRenderers(player);
                    }
                });
            }
        }, setDisplayRefreshRate(player.getPreparedVideo()) ? RefreshRateSwitchDelay : 0);
    }

    private void setupRenderers(VideoPlayer player) {
        // Build the video and audio renderers.
        Allocator allocator = new DefaultAllocator(BUFFER_SEGMENT_SIZE);
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter(player.getMainHandler(),
                null);
        DataSource dataSource = new DefaultUriDataSource(activity, bandwidthMeter, userAgent);
        ExtractorSampleSource sampleSource = new ExtractorSampleSource(uri, dataSource, allocator,
                BUFFER_SEGMENT_COUNT * BUFFER_SEGMENT_SIZE);
        MediaCodecVideoTrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(activity,
                sampleSource, MediaCodecSelector.DEFAULT,
                MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, 5000,
                player.getMainHandler(), player, 50);
        AudioCodecTrackRenderer audioRenderer = AudioCodecTrackRenderer.getRenderer(activity, player, sampleSource);
        TrackRenderer textRenderer = new TextTrackRenderer(sampleSource, player,
                player.getMainHandler().getLooper(), new PgsParser());

        // Invoke the callback.
        TrackRenderer[] renderers = new TrackRenderer[VideoPlayer.RENDERER_COUNT];
        renderers[VideoPlayer.TYPE_VIDEO] = videoRenderer;
        renderers[VideoPlayer.TYPE_AUDIO] = audioRenderer;
        renderers[VideoPlayer.TYPE_TEXT] = textRenderer;
        player.onRenderers(renderers, bandwidthMeter);
    }

    @Override
    public void cancel() {
        // Do nothing.
    }

    private static final int NO_MATCH = -1;
    private int hasFrameRate(Display.Mode[] possible, double desired) {

        int match = NO_MATCH;
        double matchDiff = 10000;
        for (int i = 0; i < possible.length; ++i) {

            double curr = possible[i].getRefreshRate();
            if (curr == desired)
                return i;

            if (Math.floor(desired) == Math.floor(curr)) {

                double discrepency = getFrameDiff(desired, curr);
                if (matchDiff > discrepency) {

                    matchDiff = discrepency;
                    match = i;
                }
            }
            else if (Math.ceil(desired) == Math.floor(curr)) {

                double discrepency = getFrameDiff(desired, curr);
                if (matchDiff > discrepency) {

                    matchDiff = discrepency;
                    match = i;
                }
            }

        }
        return match;
    }

    private int findBestForTwoPossibleFrames(Display.Mode[] possible, double desired, double backup) {

        int matchA = hasFrameRate(possible, desired);
        if (NO_MATCH != matchA && desired == possible[matchA].getRefreshRate())
            return matchA;
        int matchB = hasFrameRate(possible, backup);
        if (UNKNOWN != backup && NO_MATCH != matchB) {
            if (NO_MATCH != matchA) {

                double discrepencyA = getFrameDiff(desired, possible[matchA].getRefreshRate());
                double discrepencyB = getFrameDiff(desired, possible[matchB].getRefreshRate());
                if (discrepencyA < discrepencyB)
                    return matchA;
                return matchB;
            }
            else
                return matchB;
        }
        else if (NO_MATCH != matchA)
            return matchA;
        return -1;
    }

    private int getBestFrameRate(Display.Mode[] possible, double desired) {

        int ret = -1;
        if (desired == DIGITAL_60)
            ret = findBestForTwoPossibleFrames(possible, DIGITAL_60, NTSC);
        else if (desired == NTSC)
            ret = findBestForTwoPossibleFrames(possible, NTSC, DIGITAL_60);
        else if (desired == PAL)
            ret = findBestForTwoPossibleFrames(possible, PAL, UNKNOWN);
        else if (desired == DIGITAL_30) {
            ret = findBestForTwoPossibleFrames(possible, DIGITAL_30, DIGITAL_60);
            if (ret == -1)
                ret =findBestForTwoPossibleFrames(possible, NTSC_INTERLACED, NTSC);
        }
        else if (desired == NTSC_INTERLACED) {
            ret = findBestForTwoPossibleFrames(possible, NTSC_INTERLACED, NTSC);
            if (ret == -1)
                ret = findBestForTwoPossibleFrames(possible, DIGITAL_30, DIGITAL_60);
        }
        else if (desired == PAL_FILM)
            ret = findBestForTwoPossibleFrames(possible, PAL_FILM, PAL);
        else if (desired == FILM)
            return findBestForTwoPossibleFrames(possible, FILM, UNKNOWN);

        return ret;
    }

    private float convertFrameRate(String frameRate) {

        float ret = UNKNOWN;
        if (TextUtils.isEmpty(frameRate))
            return UNKNOWN;
        if (frameRate.equals("PAL") || frameRate.startsWith("50"))
            ret = PAL;
        else if (frameRate.equals("24p") || frameRate.startsWith("23"))
            ret = FILM;
        else if (frameRate.equals("NTSC") || frameRate.startsWith("59"))
            ret = NTSC;
        else if (frameRate.startsWith("25"))
            ret = PAL_FILM;
        else if (frameRate.startsWith("29"))
            ret = NTSC_INTERLACED;
        else if (frameRate.startsWith("30"))
            ret = DIGITAL_30;
        else if (frameRate.startsWith("60"))
            ret = DIGITAL_60;
        return ret;
    }

    private double getFrameDiff(double a, double b) {
        return  Math.abs(a - b);
    }

    private boolean setDisplayRefreshRate(PlexVideoItem video) {

        boolean ret = false;
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        SettingsManager mgr = SettingsManager.getInstance(activity);
        if (mgr.getBoolean("preferences_device_refreshrate")) {

            WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            final Display.Mode[] modes = display.getSupportedModes();
            final int requestMode = getBestFrameRate(modes, convertFrameRate(video.getMedia().get(0).getVideoFrameRate()));
            if (requestMode != -1 && display.getMode().getRefreshRate() != modes[requestMode].getRefreshRate()) {

                WindowManager.LayoutParams params = activity.getWindow().getAttributes();
                params.preferredDisplayModeId = modes[requestMode].getModeId();
                activity.getWindow().setAttributes(params);
                ret = true;
            }
        }
        return ret;
    }

}