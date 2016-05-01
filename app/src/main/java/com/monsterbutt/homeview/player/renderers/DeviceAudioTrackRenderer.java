package com.monsterbutt.homeview.player.renderers;

import android.content.Context;
import android.media.AudioManager;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.MediaFormatHolder;
import com.google.android.exoplayer.SampleSource;
import com.google.android.exoplayer.util.MimeTypes;
import com.monsterbutt.homeview.player.MediaCodecCapabilities;
import com.monsterbutt.homeview.player.VideoPlayer;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;


public class DeviceAudioTrackRenderer extends MediaCodecAudioTrackRenderer {

    private final PlexVideoItem mItem;

    public static DeviceAudioTrackRenderer getRenderer(Context context, VideoPlayer player, SampleSource sampleSource) {

        return new DeviceAudioTrackRenderer(player, sampleSource, MediaCodecCapabilities.getInstance(context));
    }

    private DeviceAudioTrackRenderer(VideoPlayer player, SampleSource sampleSource, MediaCodecCapabilities mcc) {

        super(sampleSource, MediaCodecSelector.DEFAULT, null, true, player.getMainHandler(), player,
                mcc.usePassthroughAudioIfAvailable() ? mcc.getSystemAudioCapabilities() : null
                , AudioManager.STREAM_MUSIC);
        mItem = player.getPreparedVideo();
    }

    @Override
    protected void onInputFormatChanged(MediaFormatHolder holder) throws ExoPlaybackException {

        MediaFormat format = holder.format;
        if (format.mimeType.equals(MimeTypes.AUDIO_DTS) && mItem.trackIsDtsHd(format.trackId)) {

            holder.format = MediaFormat.createAudioFormat(format.trackId, MimeTypes.AUDIO_DTS_HD,
                                            format.NO_VALUE, format.bitrate, format.maxInputSize,
                                            format.durationUs, format.channelCount, format.sampleRate,
                                            format.initializationData, format.language);
        }
        super.onInputFormatChanged(holder);
    }
}
