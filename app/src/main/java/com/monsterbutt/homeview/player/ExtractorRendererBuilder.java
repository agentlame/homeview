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

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.net.Uri;
import android.util.Log;

import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.extractor.Extractor;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.text.TextTrackRenderer;
import com.google.android.exoplayer.upstream.Allocator;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;

/**
 * A {@link VideoPlayer.RendererBuilder} for streams that can be read using an {@link Extractor}.
 * <p/>
 * This code was originally taken from the ExoPlayer demo application.
 */
public class ExtractorRendererBuilder implements VideoPlayer.RendererBuilder {

    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int BUFFER_SEGMENT_COUNT = 256;

    private final Context context;
    private final String userAgent;
    private final Uri uri;

    public ExtractorRendererBuilder(Context context, String userAgent, Uri uri) {
        this.context = context;
        this.userAgent = userAgent;
        this.uri = uri;
    }

    private void outputCodecs() {

        final String TAG = "MediaCodecListTest";
        final String MEDIA_CODEC_XML_FILE = "/etc/media_codecs.xml";
        final MediaCodecList mRegularCodecs =
                new MediaCodecList(MediaCodecList.REGULAR_CODECS);
        final MediaCodecList mAllCodecs =
                new MediaCodecList(MediaCodecList.ALL_CODECS);
        final MediaCodecInfo[] mRegularInfos =
                mRegularCodecs.getCodecInfos();
        final MediaCodecInfo[] mAllInfos =
                mAllCodecs.getCodecInfos();

        Log.d(TAG, "RegularCodecs");
        for (MediaCodecInfo info : mRegularInfos)
            printCodec(TAG, info);
        Log.d(TAG, "AllCodecs");
        for (MediaCodecInfo info : mAllInfos)
            printCodec(TAG, info);
    }

    void printCodec(String TAG, MediaCodecInfo info) {

        String[] caps = info.getSupportedTypes();
        Log.d(TAG, String.format("Name: %s\n\tEnc : %s", info.getName(), info.isEncoder() ? "True" : "False"));
        for (String cap : caps) {

            MediaCodecInfo.CodecCapabilities codec = info.getCapabilitiesForType(cap);
            Log.d(TAG, String.format("\tMime: %s", codec.getMimeType()));
        }
    }

    @Override
    public void buildRenderers(VideoPlayer player) {
        Allocator allocator = new DefaultAllocator(BUFFER_SEGMENT_SIZE);

        //outputCodecs();
        // Build the video and audio renderers.
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter(player.getMainHandler(),
                null);
        DataSource dataSource = new DefaultUriDataSource(context, bandwidthMeter, userAgent);
        ExtractorSampleSource sampleSource = new ExtractorSampleSource(uri, dataSource, allocator,
                BUFFER_SEGMENT_COUNT * BUFFER_SEGMENT_SIZE);
        MediaCodecVideoTrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(context,
                sampleSource, MediaCodecSelector.DEFAULT,
                MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, 5000,
                player.getMainHandler(), player, 50);
        AudioCodecTrackRenderer audioRenderer = AudioCodecTrackRenderer.getRenderer(context, player, sampleSource);
        TrackRenderer textRenderer = new TextTrackRenderer(sampleSource, player,
                player.getMainHandler().getLooper());

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

}