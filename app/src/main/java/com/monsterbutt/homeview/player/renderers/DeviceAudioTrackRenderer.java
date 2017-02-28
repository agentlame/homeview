package com.monsterbutt.homeview.player.renderers;

import android.media.AudioFormat;
import android.os.Handler;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.audio.AudioCapabilities;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.util.MimeTypes;
import com.monsterbutt.homeview.player.MediaCodecCapabilities;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;


public class DeviceAudioTrackRenderer extends MediaCodecAudioRenderer {

    private PlexVideoItem mItem = null;
    private final AudioCapabilities mCaps;
    private final boolean usePassthroughAudio;

    public DeviceAudioTrackRenderer(MediaCodecSelector mediaCodecSelector,
                                     DrmSessionManager<FrameworkMediaCrypto> drmSessionManager,
                                     boolean playClearSamplesWithoutKeys, Handler eventHandler,
                                     AudioRendererEventListener eventListener,
                                     MediaCodecCapabilities mcc) {

        super(mediaCodecSelector, drmSessionManager, playClearSamplesWithoutKeys, eventHandler,
                eventListener, mcc.getSystemAudioCapabilities());
        usePassthroughAudio = mcc.usePassthroughAudioIfAvailable();
        mCaps = mcc.getSystemAudioCapabilities();
    }

    public void prepareVideo(PlexVideoItem item) {
        mItem = item;
    }

    protected boolean allowPassthrough(String mimeType) {
        return usePassthroughAudio && super.allowPassthrough(mimeType);
    }

    @Override
    protected void onInputFormatChanged(Format format) throws ExoPlaybackException {

        if (mItem != null &&
                format.sampleMimeType.equals(MimeTypes.AUDIO_DTS) && mItem.trackIsDtsHd(format.id)
                && mCaps.supportsEncoding(AudioFormat.ENCODING_DTS_HD)) {

            format = Format.createAudioSampleFormat(format.id, MimeTypes.AUDIO_DTS_HD,
                                            format.codecs, format.bitrate, format.maxInputSize,
                                            format.channelCount, format.sampleRate,
                                            format.initializationData, format.drmInitData,
                                            format.selectionFlags, format.language);
        }
        super.onInputFormatChanged(format);
    }
}
