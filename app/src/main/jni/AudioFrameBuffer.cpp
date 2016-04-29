#include "AudioFrameBuffer.h"

#include "util/Log.h"

AudioFrameBuffer::AudioFrameBuffer(AVCodecContext* ctx, bool use32bit)
:   m_pSwr(NULL)
,   m_Use32Bit(use32bit)
,   m_InputFormat(ctx->sample_fmt)
,   m_InputIsPlanar(av_sample_fmt_is_planar(ctx->sample_fmt))
,   m_InputBPS(av_get_bytes_per_sample(ctx->sample_fmt))
,   m_InputChannelLayout(ctx->channel_layout)
,   m_InputChannelCount(av_get_channel_layout_nb_channels(m_InputChannelLayout))
,   m_InputSampleRate(ctx->sample_rate)
,   m_OutputFormat(use32BitFloatOutput() ? AV_SAMPLE_FMT_FLT : AV_SAMPLE_FMT_S16)
,   m_OutputBPS(av_get_bytes_per_sample(m_OutputFormat))
,   m_OutputChannelLayout(ctx->channel_layout)
,   m_OutputChannelCount(av_get_channel_layout_nb_channels(m_OutputChannelLayout))
,   m_OutputSampleRate(ctx->sample_rate)

{
    m_Err[0] = '/0';
    if (m_OutputChannelLayout != m_InputChannelLayout
     || m_OutputSampleRate != m_InputSampleRate
     || m_OutputFormat != m_InputFormat)
    {
        Log::Message(Debug, format("AudioFrameBuffer prep convert = channels (%" PRIx64 " : %" PRIx64 "), rate (%ld : %ld), format (%ld : %ld)",
            m_InputChannelLayout, m_OutputChannelLayout,
            m_InputSampleRate, m_OutputSampleRate,
            m_InputFormat, m_OutputFormat));
        m_pSwr = swr_alloc();
        av_opt_set_int(m_pSwr, "in_channel_layout",     m_InputChannelLayout, 0);
        av_opt_set_int(m_pSwr, "out_channel_layout",    m_OutputChannelLayout,  0);
        av_opt_set_int(m_pSwr, "in_sample_rate",        m_InputSampleRate, 0);
        av_opt_set_int(m_pSwr, "out_sample_rate",       m_OutputSampleRate, 0);
        av_opt_set_sample_fmt(m_pSwr, "in_sample_fmt",  m_InputFormat, 0);
        av_opt_set_sample_fmt(m_pSwr, "out_sample_fmt", m_OutputFormat,  0);
        if (0 > swr_init(m_pSwr))
            strcpy(m_Err, "Bad SWR init");
    }
    else
        Log::Message(Debug, "AudioFrameBuffer pret copy");
}

AudioFrameBuffer::~AudioFrameBuffer()
{
    if (m_pSwr != NULL)
        swr_free(&m_pSwr);
}

const char* AudioFrameBuffer::getLastError()
{
    return m_Err;
}

bool AudioFrameBuffer::hasValidSWR() const
{
    return m_pSwr != NULL;
}

int AudioFrameBuffer::fill(JNIEnv * env, AVFrame* pFrame, jobject outputBuffer)
{
    const int frames = pFrame->nb_samples;
    const int bufferSize = frames * getFrameSize(m_OutputChannelCount, m_OutputBPS);

    jclass claz = env->GetObjectClass(outputBuffer);
    jfieldID fidSize = env->GetFieldID(claz, "size", "I");
    env->SetIntField(outputBuffer, fidSize, bufferSize);

    int pts = measurePTS(frames);
    jfieldID fidPTS = env->GetFieldID(claz, "ptsDiff", "J");
    env->SetLongField(outputBuffer, fidPTS, pts);

    env->CallVoidMethod(outputBuffer, env->GetMethodID(claz, "setSize","(I)V"), bufferSize);

    jfieldID fidData = env->GetFieldID(claz, "data", "Ljava/nio/ByteBuffer;");
    jobject data = env->GetObjectField(outputBuffer, fidData);
    uint8_t* dst = reinterpret_cast<uint8_t*>(env->GetDirectBufferAddress(data));
    int ret = 0;
    if(m_pSwr != NULL)
    {
        ret = swr_convert(m_pSwr, &dst, frames, (const uint8_t**) pFrame->extended_data, frames);
        if (0 > ret)
            av_strerror(ret, m_Err, 255);
    }
    else
        copy(bufferSize, frames, pFrame->channels, (const uint8_t**) pFrame->extended_data, dst);

    return ret;
}

void AudioFrameBuffer::copy(int bufferSize, int frameCount, int channelCount, const uint8_t** src, uint8_t* dst)
{
    if (m_InputIsPlanar && channelCount > 1)
    {
        // interleave channels
        int offset = 0;
        for (int frame = 0; frame < frameCount; ++frame)
        {
            for (int channel = 0; channel < channelCount; ++channel)
                dst[offset++] = src[channel][frame];
        }
    }
    else
        memcpy(dst, src, bufferSize);
}

int AudioFrameBuffer::measurePTS(int frameCount) const
{
    return (frameCount * AV_TIME_BASE) / m_InputSampleRate;
}

bool AudioFrameBuffer::sourceAndroid16BitCompat() const
{
    return (AV_SAMPLE_FMT_S16 == m_InputFormat ||
            AV_SAMPLE_FMT_S16P == m_InputFormat);
}

bool AudioFrameBuffer::sourceAndroid32BitCompat() const
{
    return (AV_SAMPLE_FMT_FLT == m_InputFormat);
}

bool AudioFrameBuffer::use32BitFloatOutput() const
{
    return m_Use32Bit;
}

int AudioFrameBuffer::getFrameSize(int channelCount, int bytesPerChannel) const
{
    return channelCount * bytesPerChannel;
}
