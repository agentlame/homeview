package com.monsterbutt.homeview.player.renderers;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.PlaybackParams;
import android.os.Handler;
import android.os.SystemClock;

import com.google.android.exoplayer.C;
import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.MediaClock;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.MediaCodecUtil;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.SampleSource;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.audio.AudioTrack;
import com.google.android.exoplayer.util.MimeTypes;
import com.google.android.exoplayer.util.extensions.Buffer;
import com.monsterbutt.homeview.player.MediaCodecCapabilities;
import com.monsterbutt.homeview.player.VideoPlayer;
import com.monsterbutt.homeview.player.ffmpeg.FfmpegDecoder;
import com.monsterbutt.homeview.player.ffmpeg.FfmpegOutputBuffer;
import com.monsterbutt.homeview.player.ffmpeg.FfmpegTrackRenderer;

public class FfmpegAudioTrackRenderer extends FfmpegTrackRenderer implements MediaClock {

    public interface EventListener extends FfmpegTrackRenderer.EventListener {

        void onAudioTrackInitializationError(AudioTrack.InitializationException e);
        void onAudioTrackWriteError(AudioTrack.WriteException e);
        void onAudioTrackUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs);
    }

    public static final int MSG_SET_VOLUME = 1;
    public static final int MSG_SET_PLAYBACK_PARAMS = 2;

    private final EventListener eventListener;
    private final AudioTrack audioTrack;
    private AudioCapabilities audioCapabilities;

    private int audioSessionId;
    private long currentPositionUs;
    private boolean allowPositionDiscontinuity;

    private boolean audioTrackHasData;
    private long lastFeedElapsedRealtimeMs;

    public static FfmpegAudioTrackRenderer getRenderer(Context context, VideoPlayer player, SampleSource sampleSource) {

        MediaCodecCapabilities mcc = MediaCodecCapabilities.getInstance(context);
        return new FfmpegAudioTrackRenderer(sampleSource, MediaCodecSelector.DEFAULT,
                                            player.getMainHandler(), player,
                                            mcc.usePassthroughAudioIfAvailable() ?
                                                    mcc.getSystemAudioCapabilities() : null);
    }

    public FfmpegAudioTrackRenderer(SampleSource source, MediaCodecSelector mediaCodecSelector,
                                    Handler eventHandler, EventListener eventListener,
                                    AudioCapabilities audioCapabilities) {
        this(source, mediaCodecSelector, eventHandler,
                eventListener, audioCapabilities, AudioManager.STREAM_MUSIC);
    }

    public FfmpegAudioTrackRenderer(SampleSource sources, MediaCodecSelector mediaCodecSelector,
                                    Handler eventHandler, EventListener eventListener, AudioCapabilities audioCapabilities,
                                    int streamType) {
        super(sources, mediaCodecSelector, eventHandler, eventListener);
        this.eventListener = eventListener;
        this.audioSessionId = AudioTrack.SESSION_ID_NOT_SET;
        this.audioCapabilities = audioCapabilities;
        this.audioTrack = new AudioTrack(audioCapabilities, streamType);
    }

    @Override
    protected boolean handlesTrack(MediaCodecSelector mediaCodecSelector, MediaFormat mediaFormat)
            throws MediaCodecUtil.DecoderQueryException {
        String mimeType = mediaFormat.mimeType;
        if (!MimeTypes.isAudio(mimeType) || MimeTypes.AUDIO_UNKNOWN.equals(mimeType))
            return false;

        boolean hasPass = allowPassthrough(mimeType) && mediaCodecSelector.getPassthroughDecoderName() != null;
        boolean hasDecode = mediaCodecSelector.getDecoderInfo(mimeType, false) != null;
        return !hasPass && !hasDecode;
    }

    @Override
    protected int getBufferSize() {
        return audioTrack.getBufferSize();
    }

    protected boolean allowPassthrough(String mimeType) {
        return audioTrack.isPassthroughSupported(mimeType);
    }

    @Override
    protected void configureCodec(FfmpegDecoder codec, MediaFormat format) {

        boolean use32BitEncoding = false; /*format.channelCount == 2 && audioCapabilities.supportsEncoding(AudioFormat.ENCODING_PCM_FLOAT);
        if (use32BitEncoding)
            audioTrack.setFallbackEncoding(AudioFormat.ENCODING_PCM_FLOAT);*/
        onOutputFormatChanged(format.getFrameworkMediaFormatV16());
        codec.configure(format, use32BitEncoding);
    }

    @Override
    protected MediaClock getMediaClock() {
        return this;
    }

    @Override
    protected void onOutputFormatChanged(android.media.MediaFormat format) {

        String mimeType = MimeTypes.AUDIO_RAW;
        int channelCount = format.getInteger(android.media.MediaFormat.KEY_CHANNEL_COUNT);
        int sampleRate = format.getInteger(android.media.MediaFormat.KEY_SAMPLE_RATE);
        audioTrack.configure(mimeType, channelCount, sampleRate, AudioFormat.ENCODING_PCM_16BIT);
    }

    @Override
    protected void onStarted() {
        super.onStarted();
        audioTrack.play();
    }

    @Override
    protected void onStopped() {
        audioTrack.pause();
        super.onStopped();
    }

    @Override
    protected boolean isEnded() {
        return super.isEnded() && !audioTrack.hasPendingData();
    }

    @Override
    protected boolean isReady() {
        return audioTrack.hasPendingData() || super.isReady();
    }

    @Override
    public long getPositionUs() {
        long newCurrentPositionUs = audioTrack.getCurrentPositionUs(isEnded());
        if (newCurrentPositionUs != AudioTrack.CURRENT_POSITION_NOT_SET) {
            currentPositionUs = allowPositionDiscontinuity ? newCurrentPositionUs
                    : Math.max(currentPositionUs, newCurrentPositionUs);
            allowPositionDiscontinuity = false;
        }
        return currentPositionUs;
    }

    @Override
    protected void onDisabled() throws ExoPlaybackException {
        audioSessionId = AudioTrack.SESSION_ID_NOT_SET;
        try {
            audioTrack.release();
        } finally {
            super.onDisabled();
        }
    }

    @Override
    protected void onDiscontinuity(long positionUs) throws ExoPlaybackException {
        super.onDiscontinuity(positionUs);
        audioTrack.reset();
        currentPositionUs = positionUs;
        allowPositionDiscontinuity = true;
    }

    @Override
    protected boolean processOutputBuffer(long positionUs, long elapsedRealtimeUs, FfmpegOutputBuffer buffer, boolean shouldSkip)
    throws ExoPlaybackException {

        if (shouldSkip || buffer.getFlag(Buffer.FLAG_DECODE_ONLY) || buffer.data == null || buffer.size == 0) {
            buffer.release();
            codecCounters.skippedOutputBufferCount++;
            audioTrack.handleDiscontinuity();
            return true;
        }

        if (!audioTrack.isInitialized()) {
            // Initialize the AudioTrack now.
            try {
                if (audioSessionId != AudioTrack.SESSION_ID_NOT_SET) {
                    audioTrack.initialize(audioSessionId);
                } else {
                    audioSessionId = audioTrack.initialize();
                }
                audioTrackHasData = false;
            } catch (AudioTrack.InitializationException e) {
                notifyAudioTrackInitializationError(e);
                throw new ExoPlaybackException(e);
            }
            if (getState() == TrackRenderer.STATE_STARTED) {
                audioTrack.play();
            }
        } else {
            // Check for AudioTrack underrun.
            boolean audioTrackHadData = audioTrackHasData;
            audioTrackHasData = audioTrack.hasPendingData();
            if (audioTrackHadData && !audioTrackHasData && getState() == TrackRenderer.STATE_STARTED) {
                long elapsedSinceLastFeedMs = SystemClock.elapsedRealtime() - lastFeedElapsedRealtimeMs;
                long bufferSizeUs = audioTrack.getBufferSizeUs();
                long bufferSizeMs = bufferSizeUs == C.UNKNOWN_TIME_US ? -1 : bufferSizeUs / 1000;
                notifyAudioTrackUnderrun(audioTrack.getBufferSize(), bufferSizeMs, elapsedSinceLastFeedMs);
            }
        }

        int handleBufferResult;
        try {
            handleBufferResult = audioTrack.handleBuffer(buffer.data, 0, buffer.size, buffer.getPTS());
            lastFeedElapsedRealtimeMs = SystemClock.elapsedRealtime();
        } catch (AudioTrack.WriteException e) {
            notifyAudioTrackWriteError(e);
            throw new ExoPlaybackException(e);
        }

        // If we are out of sync, allow currentPositionUs to jump backwards.
        if ((handleBufferResult & AudioTrack.RESULT_POSITION_DISCONTINUITY) != 0) {
            handleAudioTrackDiscontinuity();
            allowPositionDiscontinuity = true;
        }

        // Release the buffer if it was consumed.
        if ((handleBufferResult & AudioTrack.RESULT_BUFFER_CONSUMED) != 0) {
            buffer.release();
            codecCounters.renderedOutputBufferCount++;
            return true;
        }

        return false;
    }

    @Override
    protected void onOutputStreamEnded() {
        audioTrack.handleEndOfStream();
    }

    protected void handleAudioTrackDiscontinuity() {
        // Do nothing
    }

    @Override
    public void handleMessage(int messageType, Object message) throws ExoPlaybackException {
        switch (messageType) {
            case MSG_SET_VOLUME:
                audioTrack.setVolume((Float) message);
                break;
            case MSG_SET_PLAYBACK_PARAMS:
                audioTrack.setPlaybackParams((PlaybackParams) message);
                break;
            default:
                super.handleMessage(messageType, message);
                break;
        }
    }

    private void notifyAudioTrackInitializationError(final AudioTrack.InitializationException e) {
        if (eventHandler != null && eventListener != null) {
            eventHandler.post(new Runnable() {
                @Override
                public void run() {
                    eventListener.onAudioTrackInitializationError(e);
                }
            });
        }
    }

    private void notifyAudioTrackWriteError(final AudioTrack.WriteException e) {
        if (eventHandler != null && eventListener != null) {
            eventHandler.post(new Runnable()  {
                @Override
                public void run() {
                    eventListener.onAudioTrackWriteError(e);
                }
            });
        }
    }

    private void notifyAudioTrackUnderrun(final int bufferSize, final long bufferSizeMs,
                                          final long elapsedSinceLastFeedMs) {
        if (eventHandler != null && eventListener != null) {
            eventHandler.post(new Runnable()  {
                @Override
                public void run() {
                    eventListener.onAudioTrackUnderrun(bufferSize, bufferSizeMs, elapsedSinceLastFeedMs);
                }
            });
        }
    }

}