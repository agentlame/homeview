package com.monsterbutt.homeview.player.ffmpeg;

import android.os.Handler;
import android.os.SystemClock;

import com.google.android.exoplayer.CodecCounters;
import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.MediaCodecUtil;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.MediaFormatHolder;
import com.google.android.exoplayer.SampleSource;
import com.google.android.exoplayer.SampleSourceTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.util.TraceUtil;
import com.google.android.exoplayer.util.extensions.Buffer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;


public abstract class FfmpegTrackRenderer extends SampleSourceTrackRenderer {

    public interface EventListener {

        void onDecoderError(FfmpegDecoderException e);

        /**
         * Invoked when a decoder is successfully created.
         *
         * @param decoderName The decoder that was configured and created.
         * @param elapsedRealtimeMs {@code elapsedRealtime} timestamp of when the initialization
         *    finished.
         * @param initializationDurationMs Amount of time taken to initialize the decoder.
         */
        void onDecoderInitialized(String decoderName, long elapsedRealtimeMs,
                                  long initializationDurationMs);
    }

    protected static final int SOURCE_STATE_NOT_READY = 0;
    protected static final int SOURCE_STATE_READY = 1;
    protected static final int SOURCE_STATE_READY_READ_MAY_FAIL = 2;

    private static final long MAX_CODEC_HOTSWAP_TIME_MS = 1000;


    private static final int RECONFIGURATION_STATE_NONE = 0;
    private static final int RECONFIGURATION_STATE_WRITE_PENDING = 1;
    private static final int RECONFIGURATION_STATE_QUEUE_PENDING = 2;
    private static final int REINITIALIZATION_STATE_NONE = 0;
    private static final int REINITIALIZATION_STATE_SIGNAL_END_OF_STREAM = 1;
    private static final int REINITIALIZATION_STATE_WAIT_END_OF_STREAM = 2;

    private static final int NUM_BUFFERS = 16;
    private static final int INITIAL_INPUT_BUFFER_SIZE = 768 * 1024; // Value based on cs/SoftVpx.cpp.

    public final CodecCounters codecCounters;
    private final MediaCodecSelector mediaCodecSelector;
    protected final Handler eventHandler;
    private final EventListener eventListener;
    private final MediaFormatHolder formatHolder;

    private MediaFormat format;
    private FfmpegDecoder codec;
    private FfmpegInputBuffer inputBuffer;
    private FfmpegOutputBuffer outputBuffer;
    private final List<Long> decodeOnlyPresentationTimestamps;

    private long codecHotswapTimeMs;
    private boolean codecReconfigured;
    private int codecReconfigurationState;
    private int codecReinitializationState;
    private boolean codecReceivedBuffers;
    private boolean codecReceivedEos;

    private int sourceState;
    private boolean inputStreamEnded;
    private boolean outputStreamEnded;
    private boolean waitingForFirstSyncFrame;


    public FfmpegTrackRenderer(SampleSource source, MediaCodecSelector mediaCodecSelector) {
        this(source, mediaCodecSelector, null, null);
    }

    public FfmpegTrackRenderer(SampleSource source, MediaCodecSelector mediaCodecSelector,
                                    Handler eventHandler, EventListener eventListener) {
        super(source);
        this.mediaCodecSelector = mediaCodecSelector;
        this.eventHandler = eventHandler;
        this.eventListener = eventListener;
        formatHolder = new MediaFormatHolder();
        codecCounters = new CodecCounters();
        decodeOnlyPresentationTimestamps = new ArrayList<>();
        codecReconfigurationState = RECONFIGURATION_STATE_NONE;
        codecReinitializationState = REINITIALIZATION_STATE_NONE;
    }

    /**
     * Returns whether the underlying libvpx library is available.
     */
    public static boolean isLibFfmpegAvailable() {
        return FfmpegDecoder.IS_AVAILABLE;
    }

    /**
     * Returns the version of the underlying libvpx library if available, otherwise {@code null}.
     */
    public static String getLibFfmpegVersion() {
        return isLibFfmpegAvailable() ? FfmpegDecoder.getVersion() : null;
    }

    @Override
    protected final boolean handlesTrack(MediaFormat mediaFormat) throws MediaCodecUtil.DecoderQueryException {
        return handlesTrack(mediaCodecSelector, mediaFormat);
    }

    /**
     * Returns whether this renderer is capable of handling the provided track.
     *
     * @param mediaCodecSelector The decoder selector.
     * @param mediaFormat The format of the track.
     * @return True if the renderer can handle the track, false otherwise.
     * @throws MediaCodecUtil.DecoderQueryException Thrown if there was an error querying decoders.
     */
    protected abstract boolean handlesTrack(MediaCodecSelector mediaCodecSelector,
                                            MediaFormat mediaFormat) throws MediaCodecUtil.DecoderQueryException;

    protected abstract void configureCodec(FfmpegDecoder codec, MediaFormat format);

    protected final void maybeInitCodec() throws ExoPlaybackException {
        if (!shouldInitCodec()) {
            return;
        }

        String mimeType = format.mimeType;
       try {
            long codecInitializingTimestamp = SystemClock.elapsedRealtime();
            TraceUtil.beginSection("createByCodecName(" + mimeType + ")");// If we don't have a decoder yet, we need to instantiate one.
            codec = new FfmpegDecoder(NUM_BUFFERS, NUM_BUFFERS, INITIAL_INPUT_BUFFER_SIZE);
            TraceUtil.endSection();
            TraceUtil.beginSection("configureCodec");
            configureCodec(codec, format);
            TraceUtil.endSection();
            TraceUtil.beginSection("codec.start()");
            codec.start();
            TraceUtil.endSection();
            long codecInitializedTimestamp = SystemClock.elapsedRealtime();
            notifyDecoderInitialized(mimeType, codecInitializedTimestamp,
                    codecInitializedTimestamp - codecInitializingTimestamp);
        } catch (Exception e) {
            notifyDecoderError(new FfmpegDecoderException(mimeType + "init error: " + e.getMessage()));
        }
        codecHotswapTimeMs = getState() == TrackRenderer.STATE_STARTED ?
                SystemClock.elapsedRealtime() : -1;

        waitingForFirstSyncFrame = true;
        codecCounters.codecInitCount++;
    }

    @Override
    protected void onStarted() {
    }

    @Override
    protected void onStopped() {
    }


    protected boolean shouldInitCodec() {
        return codec == null && format != null;
    }

    protected final boolean codecInitialized() {
        return codec != null;
    }

    protected final boolean haveFormat() {
        return format != null;
    }

    @Override
    protected void onDisabled() throws ExoPlaybackException {
        inputBuffer = null;
        outputBuffer = null;
        format = null;
        try {
            if (codec != null) {
                codec.release();
                codec = null;
                codecCounters.codecReleaseCount++;
            }
        } finally {
            super.onDisabled();
        }
    }

    protected void releaseCodec() {

        if (codec != null) {

            codecHotswapTimeMs = -1;
            decodeOnlyPresentationTimestamps.clear();
            inputBuffer = null;
            if (outputBuffer != null) {
                outputBuffer.release();
                outputBuffer = null;
            }
            codecReconfigured = false;
            codecReceivedBuffers = false;
            codecReconfigurationState = RECONFIGURATION_STATE_NONE;
            codecReinitializationState = REINITIALIZATION_STATE_NONE;
            codecCounters.codecReleaseCount++;
            try {
                codec.stop();
            }
            catch(Exception e) {}
            finally {
                try {
                    codec.release();
                } finally {
                    codec = null;
                }
            }
        }
    }

    @Override
    protected void onDiscontinuity(long positionUs) throws ExoPlaybackException {
        sourceState = SOURCE_STATE_NOT_READY;
        inputStreamEnded = false;
        outputStreamEnded = false;
        if (codec != null) {
            flushCodec();
        }
    }

    @Override
    protected void doSomeWork(long positionUs, long elapsedRealtimeUs, boolean sourceIsReady)
            throws ExoPlaybackException {
        if (outputStreamEnded) {
            return;
        }
        sourceState = sourceIsReady
                ? (sourceState == SOURCE_STATE_NOT_READY ? SOURCE_STATE_READY : sourceState)
                : SOURCE_STATE_NOT_READY;

        // Try and read a format if we don't have one already.
        if (format == null && !readFormat(positionUs)) {
            // We can't make progress without one.
            return;
        }

        try {
            maybeInitCodec();
            if (codec != null) {
                TraceUtil.beginSection("drainAndFeed");
                while (drainOutputBuffer(positionUs, elapsedRealtimeUs)) {}
                if (feedInputBuffer(positionUs, true)) {
                    while (feedInputBuffer(positionUs, false)) {
                    }
                }
                TraceUtil.endSection();
            }
        } catch (ExoPlaybackException e) {
            notifyDecoderError(new FfmpegDecoderException(e.getMessage()));
            throw new ExoPlaybackException(e);
        }

        codecCounters.ensureUpdated();
    }

    protected void onInputFormatChanged(MediaFormatHolder formatHolder) throws ExoPlaybackException {

        MediaFormat oldFormat = format;
        format = formatHolder.format;
        if (codec != null && canReconfigureCodec(codec, oldFormat, format)) {
            codecReconfigured = true;
            codecReconfigurationState = RECONFIGURATION_STATE_WRITE_PENDING;
        } else {
            if (codecReceivedBuffers) {
                // Signal end of stream and wait for any final output buffers before re-initialization.
                codecReinitializationState = REINITIALIZATION_STATE_SIGNAL_END_OF_STREAM;
            } else {
                // There aren't any final output buffers, so perform re-initialization immediately.
                releaseCodec();
                maybeInitCodec();
            }
        }
    }

    protected void onOutputFormatChanged(android.media.MediaFormat outputFormat) {
    }

    protected abstract boolean processOutputBuffer(long positionUs, long elapsedRealtimeUs,
                                                   FfmpegOutputBuffer buffer, boolean shouldSkip)
            throws ExoPlaybackException;

    protected boolean canReconfigureCodec(FfmpegDecoder codec,
                                          MediaFormat oldFormat, MediaFormat newFormat) {
        return false;
    }

    protected abstract int getBufferSize();

    @Override
    protected boolean isEnded() {
        return outputStreamEnded;
    }

    @Override
    protected boolean isReady() {
        return format != null
                && (sourceState != SOURCE_STATE_NOT_READY || outputBuffer != null || isWithinHotswapPeriod());

    }

    protected final int getSourceState() {
        return sourceState;
    }

    private boolean isWithinHotswapPeriod() {
        return SystemClock.elapsedRealtime() < codecHotswapTimeMs + MAX_CODEC_HOTSWAP_TIME_MS;
    }

    private boolean drainOutputBuffer(long positionUs, long elapsedRealtimeUs)
            throws ExoPlaybackException {

        if (outputStreamEnded) {
            return false;
        }

        // Acquire outputBuffer either from nextOutputBuffer or from the decoder.
        if (outputBuffer == null) {

            try {
                outputBuffer = codec.dequeueOutputBuffer();
            } catch (FfmpegDecoderException e)  {}

            if (outputBuffer == null) {
                return false;
            }
        }

        /*if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            processOutputFormat();
            return true;
        } else if (outputIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            outputBuffers = codec.getOutputBuffers();
            codecCounters.outputBuffersChangedCount++;
            return true;
        }*/


        if (outputBuffer.getFlag(Buffer.FLAG_END_OF_STREAM)) {
            processEndOfStream();
            return false;
        }

        // Drop frame only if we have the next frame and that's also late, otherwise render whatever we
        // have.
        /*if (outputBuffer.timestampUs < positionUs) {
            // Drop frame if we are too late.
            codecCounters.droppedOutputBufferCount++;
            outputBuffer.release();
            outputBuffer = null;
            return true;
        }*/

        try {
            int decodeOnlyIndex = getDecodeOnlyIndex(outputBuffer.getPTS());
            if (processOutputBuffer(positionUs, elapsedRealtimeUs, outputBuffer, decodeOnlyIndex != -1)) {

                onProcessedOutputBuffer(outputBuffer.getPTS());
                if (decodeOnlyIndex != -1) {
                    decodeOnlyPresentationTimestamps.remove(decodeOnlyIndex);
                }

                outputBuffer = null;
                return true;
            }
        }
        catch (ExoPlaybackException e) {

            // handle exception

            outputBuffer.release();
            outputBuffer = null;
        }

        return false;
    }

    private boolean feedInputBuffer(long positionUs, boolean firstFeed) throws ExoPlaybackException {

        if (inputStreamEnded
                || codecReinitializationState == REINITIALIZATION_STATE_WAIT_END_OF_STREAM) {
            // The input stream has ended, or we need to re-initialize the codec but are still waiting
            // for the existing codec to output any final output buffers.
            return false;
        }

        if (inputBuffer == null) {
            try {
                inputBuffer = codec.dequeueInputBuffer();
            }
            catch (FfmpegDecoderException e) {
                throw new ExoPlaybackException(e.getMessage());
            }
            if (inputBuffer == null)
                return false;
        }

        if (codecReinitializationState == REINITIALIZATION_STATE_SIGNAL_END_OF_STREAM) {
            // We need to re-initialize the codec. Send an end of stream signal to the existing codec so
            // that it outputs any remaining buffers before we release it.

            codecReceivedEos = true;

            inputBuffer.setFlag(Buffer.FLAG_END_OF_STREAM);
            try {
                codec.queueInputBuffer(inputBuffer);
            }
            catch (FfmpegDecoderException e) {
                throw new ExoPlaybackException(e.getMessage());
            }
            codecReinitializationState = REINITIALIZATION_STATE_WAIT_END_OF_STREAM;
            inputBuffer = null;
            return false;
        }

        int result = readSource(positionUs, formatHolder, inputBuffer.sampleHolder);
        if (firstFeed && sourceState == SOURCE_STATE_READY && result == SampleSource.NOTHING_READ) {
            sourceState = SOURCE_STATE_READY_READ_MAY_FAIL;
        }
        if (result == SampleSource.NOTHING_READ) {
            flushCodec();
            inputBuffer = null;
            return false;
        }
        if (result == SampleSource.FORMAT_READ) {
            if (codecReconfigurationState == RECONFIGURATION_STATE_QUEUE_PENDING) {
                // We received two formats in a row. Clear the current buffer of any reconfiguration data
                // associated with the first format.
                inputBuffer.sampleHolder.clearData();
                codecReconfigurationState = RECONFIGURATION_STATE_WRITE_PENDING;
            }
            onInputFormatChanged(formatHolder);
            return true;
        }
        if (result == SampleSource.END_OF_STREAM) {
            if (codecReconfigurationState == RECONFIGURATION_STATE_QUEUE_PENDING) {
                // We received a new format immediately before the end of the stream. We need to clear
                // the corresponding reconfiguration data from the current buffer, but re-write it into
                // a subsequent buffer if there are any (e.g. if the user seeks backwards).
                inputBuffer.sampleHolder.clearData();
                codecReconfigurationState = RECONFIGURATION_STATE_WRITE_PENDING;
            }
            inputStreamEnded = true;
            if (!codecReceivedBuffers) {
                processEndOfStream();
                return false;
            }

            codecReceivedEos = true;

            inputBuffer.setFlag(Buffer.FLAG_END_OF_STREAM);
            try {
                codec.queueInputBuffer(inputBuffer);
            } catch (FfmpegDecoderException e)  { throw new ExoPlaybackException(e.getMessage()); }

            inputBuffer = null;
            return false;
        }
        if (waitingForFirstSyncFrame) {
            // TODO: Find out if it's possible to supply samples prior to the first sync
            // frame for HE-AAC.
            if (!inputBuffer.sampleHolder.isSyncFrame()) {
                inputBuffer.sampleHolder.clearData();
                if (codecReconfigurationState == RECONFIGURATION_STATE_QUEUE_PENDING) {
                    // The buffer we just cleared contained reconfiguration data. We need to re-write this
                    // data into a subsequent buffer (if there is one).
                    codecReconfigurationState = RECONFIGURATION_STATE_WRITE_PENDING;
                }
                return true;
            }
            waitingForFirstSyncFrame = false;
        }

        long presentationTimeUs = inputBuffer.sampleHolder.timeUs;
        if (inputBuffer.sampleHolder.isDecodeOnly()) {
            decodeOnlyPresentationTimestamps.add(presentationTimeUs);
        }

        onQueuedInputBuffer(presentationTimeUs, inputBuffer.sampleHolder.data,
                    inputBuffer.sampleHolder.data.position(), false);
        try {
            codec.queueInputBuffer(inputBuffer);
        }
        catch (FfmpegDecoderException e)  { throw new ExoPlaybackException(e.getMessage()); }
        inputBuffer = null;
        codecReceivedBuffers = true;
        codecReconfigurationState = RECONFIGURATION_STATE_NONE;
        codecCounters.inputBufferCount++;
        return true;
    }

    private void flushCodec() throws ExoPlaybackException {

        codecHotswapTimeMs = -1;
        waitingForFirstSyncFrame = true;
        decodeOnlyPresentationTimestamps.clear();

        if (codecReinitializationState != REINITIALIZATION_STATE_NONE) {
            // We're already waiting to release and re-initialize the codec. Since we're now flushing,
            // there's no need to wait any longer.
            releaseCodec();
            maybeInitCodec();
        } else {
            // We can flush and re-use the existing decoder.
            codec.flush();
            codecReceivedBuffers = false;
        }
        if (codecReconfigured && format != null) {
            // Any reconfiguration data that we send shortly before the flush may be discarded. We
            // avoid this issue by sending reconfiguration data following every flush.
            codecReconfigurationState = RECONFIGURATION_STATE_WRITE_PENDING;
        }
    }

    private boolean readFormat(long positionUs) throws ExoPlaybackException {
        int result = readSource(positionUs, formatHolder, null);
        if (result == SampleSource.FORMAT_READ) {
            onInputFormatChanged(formatHolder);
            return true;
        }
        return false;
    }

    private void processEndOfStream() throws ExoPlaybackException{

        if (codecReinitializationState == REINITIALIZATION_STATE_WAIT_END_OF_STREAM) {
            // We're waiting to re-initialize the codec, and have now processed all final buffers.
            releaseCodec();
            maybeInitCodec();
        } else {
            outputStreamEnded = true;
            onOutputStreamEnded();
        }
    }

    protected void onOutputStreamEnded() {
        // Do nothing.
    }

    protected void onProcessedOutputBuffer(long presentationTimeUs) {
        // Do nothing.
    }

    protected void onQueuedInputBuffer(
            long presentationTimeUs, ByteBuffer buffer, int bufferSize, boolean sampleEncrypted) {
        // Do nothing.
    }

    @Override
    public void handleMessage(int messageType, Object message) throws ExoPlaybackException {
        super.handleMessage(messageType, message);
    }

    private int getDecodeOnlyIndex(long presentationTimeUs) {
        final int size = decodeOnlyPresentationTimestamps.size();
        for (int i = 0; i < size; i++) {
            if (decodeOnlyPresentationTimestamps.get(i).longValue() == presentationTimeUs) {
                return i;
            }
        }
        return -1;
    }

    private void notifyDecoderError(final FfmpegDecoderException e) {
        if (eventHandler != null && eventListener != null) {
            eventHandler.post(new Runnable()  {
                @Override
                public void run() {
                    eventListener.onDecoderError(e);
                }
            });
        }
    }

    private void notifyDecoderInitialized(final String codecName,
            final long startElapsedRealtimeMs, final long finishElapsedRealtimeMs) {
        if (eventHandler != null && eventListener != null) {
            eventHandler.post(new Runnable() {
                @Override
                public void run() {
                    eventListener.onDecoderInitialized("libffmpeg" + getLibFfmpegVersion() + " - " + codecName,
                            finishElapsedRealtimeMs, finishElapsedRealtimeMs - startElapsedRealtimeMs);
                }
            });
        }
    }
}