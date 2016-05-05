package com.monsterbutt.homeview.player.ffmpeg;

import android.util.Log;

import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.SampleHolder;
import com.google.android.exoplayer.util.extensions.Buffer;
import com.google.android.exoplayer.util.extensions.SimpleDecoder;

import java.nio.ByteBuffer;

public class FfmpegDecoder extends SimpleDecoder<FfmpegInputBuffer, FfmpegOutputBuffer, FfmpegDecoderException> {

    private long ffmpegDecContext;

    public FfmpegDecoder(int numInputBuffers, int numOutputBuffers, int initialInputBufferSize)
            throws FfmpegDecoderException {
        super(new FfmpegInputBuffer[numInputBuffers], new FfmpegOutputBuffer[numOutputBuffers]);

        ffmpegDecContext = init();
        if (ffmpegDecContext == 0) {
            throw new FfmpegDecoderException("Failed to initialize decoder");
        }
        setInitialInputBufferSize(initialInputBufferSize);
    }

    @Override
    protected FfmpegInputBuffer createInputBuffer() {
        return new FfmpegInputBuffer();
    }

    @Override
    protected FfmpegOutputBuffer createOutputBuffer() {
        return new FfmpegOutputBuffer(this);
    }

    @Override
    protected void releaseOutputBuffer(FfmpegOutputBuffer buffer) {
        super.releaseOutputBuffer(buffer);
    }

    @Override
    protected FfmpegDecoderException decode(FfmpegInputBuffer inputBuffer, FfmpegOutputBuffer outputBuffer, boolean reset) {

        SampleHolder sampleHolder = inputBuffer.sampleHolder;
        outputBuffer.timestampUs = sampleHolder.timeUs;
        sampleHolder.data.position(sampleHolder.data.position() - sampleHolder.size);

        int offset = 0;
        while (offset < sampleHolder.size) {

            int read = decode(ffmpegDecContext, sampleHolder.data, offset, sampleHolder.size);
            if (read < 0) {
                Log.e("FfmpegDecoder", "Decode error : " + getLastError(ffmpegDecContext));
                outputBuffer.setFlag(Buffer.FLAG_DECODE_ONLY);
                break;
            }
            offset += read;

            if (getFrame(ffmpegDecContext, outputBuffer) < 0) {
                Log.e("FfmpegDecoder", "GetFrame error : " + getLastError(ffmpegDecContext));
                outputBuffer.setFlag(Buffer.FLAG_DECODE_ONLY);
            }
            else
                outputBuffer.setFlag(0);
        }
        return null;
    }

    @Override
    public void release() {
        super.release();
        ffmpegDecContext = close(ffmpegDecContext);
    }

    public boolean configure(MediaFormat format, boolean use32BitEncoding) {
        return configure(ffmpegDecContext, format, use32BitEncoding);
    }

    public static final boolean IS_AVAILABLE;
    static {
        boolean isAvailable = true;
        try {
            System.loadLibrary("c++_shared");
            System.loadLibrary("homeviewJNI");
        } catch (UnsatisfiedLinkError exception) {
            isAvailable = false;
        }
        IS_AVAILABLE = isAvailable;
    }

    public static native String getVersion();

    private native long     init();
    private native long     close(long context);
    private native boolean  configure(long context, MediaFormat format, boolean use32bit);
    private native int      decode(long context, ByteBuffer encoded, int offset, int length);
    private native int      getFrame(long context, FfmpegOutputBuffer outputBuffer);
    private native String   getLastError(long context);

}
