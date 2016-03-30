package com.monsterbutt.homeview.player;

import android.content.Context;
import android.media.AudioManager;

import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.SampleSource;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.monsterbutt.homeview.settings.SettingsManager;


public class AudioCodecTrackRenderer extends MediaCodecAudioTrackRenderer {

    private static boolean useAudioPassthrough;

    public static AudioCodecTrackRenderer getRenderer(Context context, VideoPlayer player, SampleSource sampleSource) {

        final boolean useAudioPassthrough = SettingsManager.getInstance(context).getBoolean("preferences_device_passthrough");
        AudioCapabilities audioCaps = useAudioPassthrough ? AudioCapabilities.getCapabilities(context)
                                                        : null;
        return new AudioCodecTrackRenderer(context, player, sampleSource, audioCaps, useAudioPassthrough);
    }

    private AudioCodecTrackRenderer(Context context, VideoPlayer player, SampleSource sampleSource,
                                    AudioCapabilities audioCapabilities, boolean useAudioPassthrough) {

        super(sampleSource, MediaCodecSelector.DEFAULT, null, true, player.getMainHandler(), player,
                audioCapabilities, AudioManager.STREAM_MUSIC);
        this.useAudioPassthrough = useAudioPassthrough;
    }
}
