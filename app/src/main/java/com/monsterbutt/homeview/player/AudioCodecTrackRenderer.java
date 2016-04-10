package com.monsterbutt.homeview.player;

import android.content.Context;
import android.media.AudioManager;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.MediaFormatHolder;
import com.google.android.exoplayer.SampleSource;
import com.google.android.exoplayer.util.MimeTypes;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;


public class AudioCodecTrackRenderer extends MediaCodecAudioTrackRenderer {

    private final PlexVideoItem mItem;

    public static AudioCodecTrackRenderer getRenderer(Context context, VideoPlayer player, SampleSource sampleSource) {

        return new AudioCodecTrackRenderer(player, sampleSource, MediaCodecCapabilities.getInstance(context));
    }

    private AudioCodecTrackRenderer(VideoPlayer player, SampleSource sampleSource, MediaCodecCapabilities mcc) {

        super(sampleSource, MediaCodecSelector.DEFAULT, null, true, player.getMainHandler(), player,
                mcc.usePassthroughAudioIfAvailable() ? mcc.getSystemAudioCapabilities() : null
                , AudioManager.STREAM_MUSIC);
        mItem = player.getPreparedVideo();
    }

    @Override
    protected void onInputFormatChanged(MediaFormatHolder holder) throws ExoPlaybackException {

        MediaFormat format = holder.format;
        if (format.mimeType.equals(MimeTypes.AUDIO_DTS)&& mItem.trackIsDtsHd(format.trackId))
            holder.format = setMediaFormatToDtsHd(format);
        super.onInputFormatChanged(holder);
    }
    
    private MediaFormat setMediaFormatToDtsHd(MediaFormat mediaFormat) {
        return MediaFormat.createAudioFormat(mediaFormat.trackId, MimeTypes.AUDIO_DTS_HD,
                mediaFormat.bitrate, mediaFormat.maxInputSize, mediaFormat.durationUs,
                mediaFormat.channelCount, mediaFormat.sampleRate,
                mediaFormat.initializationData, mediaFormat.language);
    }
}
