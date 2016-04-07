package com.monsterbutt.homeview.player;

import android.content.Context;
import android.media.AudioManager;

import com.google.android.exoplayer.DecoderInfo;
import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.MediaCodecUtil;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.MediaFormatHolder;
import com.google.android.exoplayer.SampleSource;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.extractor.webm.WebmExtractor;
import com.google.android.exoplayer.util.MimeTypes;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.settings.SettingsManager;


public class AudioCodecTrackRenderer extends MediaCodecAudioTrackRenderer {

    private static boolean useAudioPassthrough;
    private PlexVideoItem mItem = null;
    private boolean mForcedDtsHd = false;

    public static AudioCodecTrackRenderer getRenderer(Context context, VideoPlayer player, SampleSource sampleSource) {

        final boolean useAudioPassthrough = SettingsManager.getInstance(context).getBoolean("preferences_device_passthrough");
        AudioCapabilities audioCaps = useAudioPassthrough ? AudioCapabilities.getCapabilities(context)
                                                        : null;
        return new AudioCodecTrackRenderer(context, player, sampleSource, audioCaps, useAudioPassthrough);
    }

    private AudioCodecTrackRenderer(Context context, VideoPlayer player, SampleSource sampleSource,
                                    AudioCapabilities audioCapabilities,  boolean useAudioPassthrough) {

        super(sampleSource, MediaCodecSelector.DEFAULT, null, true, player.getMainHandler(), player,
                audioCapabilities, AudioManager.STREAM_MUSIC);
        mItem = player.getPreparedVideo();
        this.useAudioPassthrough = useAudioPassthrough;
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
