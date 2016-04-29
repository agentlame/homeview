#ifndef HOMEVIEW_JNI_AUDIOFRAMEBUFFER_H
#define HOMEVIEW_JNI_AUDIOFRAMEBUFFER_H

extern "C" {
#include "libavutil/opt.h"
#include "libswresample/swresample.h"
#include "libavcodec/avcodec.h"
}

#include <jni.h>

class SwrContext;
class AVCodecContext;
class AVFrame;

class AudioFrameBuffer
{

public:

    AudioFrameBuffer(AVCodecContext* ctx, bool use32bit);
    ~AudioFrameBuffer();

    int fill(JNIEnv * env, AVFrame* pFrame, jobject outputBuffer);

    bool hasValidSWR() const;
    const char* getLastError();

private:

    bool sourceAndroid16BitCompat() const;
    bool sourceAndroid32BitCompat() const;
    bool use32BitFloatOutput() const;

    int getFrameSize(int channelCount, int bytesPerChannel)  const;
    void copy(int bufferSize, int frameCount, int channelCount, const uint8_t** src, uint8_t* dst);
    int measurePTS(int frameCount) const;

private:

    SwrContext*             m_pSwr;

    const bool              m_Use32Bit;

    const AVSampleFormat    m_InputFormat;
    const bool              m_InputIsPlanar;
    const int               m_InputBPS;
    const int64_t           m_InputChannelLayout;
    const int               m_InputChannelCount;
    const int               m_InputSampleRate;

    const AVSampleFormat    m_OutputFormat;
    const int               m_OutputBPS;
    const int64_t           m_OutputChannelLayout;
    const int               m_OutputChannelCount;
    const int               m_OutputSampleRate;

    char m_Err[256];
};

#endif // HOMEVIEW_JNI_AUDIOFRAMEBUFFER_H