package com.monsterbutt.homeview.player.ffmpeg;

import com.google.android.exoplayer.util.extensions.OutputBuffer;

import java.nio.ByteBuffer;

public class FfmpegOutputBuffer extends OutputBuffer {

    private final FfmpegDecoder owner;

    public ByteBuffer data = null;
    public int size = 0;
    private long ptsDiff = 0;

    /* package */ FfmpegOutputBuffer(FfmpegDecoder owner) {
        this.owner = owner;
    }

    /* package */ void setSize(int size) {

        if (data == null || data.capacity() < size) {

            this.size = 0;
            data = ByteBuffer.allocateDirect(size);
            data.position(0);
            data.limit(size);
        }
    }

    public long getPTS() {
        return ptsDiff + super.timestampUs;
    }

    @Override
    public void release() {
        owner.releaseOutputBuffer(this);
    }
}
