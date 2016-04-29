/*
 *      Copyright (C) 2005-2013 Team XBMC
 *      http://xbmc.org
 *
 *  This Program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2, or (at your option)
 *  any later version.
 *
 *  This Program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with XBMC; see the file COPYING.  If not, see
 *  <http://www.gnu.org/licenses/>.
 *
 */

/*
 * Modified to fit with this application
 */

#include <stdint.h>
#include <string.h>
#include "AudioCodecFFmpeg.h"

extern "C" {
#include "libavcodec/avcodec.h"
}

#include "AudioFrameBuffer.h"

#include "util/Log.h"

AudioCodecFFmpeg::AudioCodecFFmpeg()
:   mUse32Bit(false)
,   m_pCodecContext(NULL)
,   m_pFrame1(NULL)
,   m_gotFrame(0)
,   m_Buffer(NULL)
{
    m_Err[0] = '\0';
}

AudioCodecFFmpeg::~AudioCodecFFmpeg()
{
    Dispose();
}

const char* AudioCodecFFmpeg::getLastError()
{
    return m_Err;
}

bool AudioCodecFFmpeg::Open(JNIEnv * env, jobject formatObj, bool use32Bit)
{
    mUse32Bit = use32Bit;

    AVCodec* pCodec = NULL;

    jclass claz = env->GetObjectClass(formatObj);
    jfieldID mimeFID = env->GetFieldID(claz, "mimeType", "Ljava/lang/String;");
    jfieldID bitrateFID = env->GetFieldID(claz, "bitrate", "I");
    jfieldID channelCountFID = env->GetFieldID(claz, "channelCount", "I");
    jfieldID sampleRateFID = env->GetFieldID(claz, "sampleRate", "I");
    //jfieldID bitDepthFID = env->GetFieldID(claz, "bitDepth", "I");

    int bitdepth = 16;//env->GetIntField(formatObj, bitDepthFID);

    jstring jMimeType = (jstring)env->GetObjectField(formatObj, mimeFID);
    const char *cMimeType = env->GetStringUTFChars(jMimeType, 0);
    mCodecID = AudioCodecFFmpeg::getAVCodecID(cMimeType, bitdepth);

    Log::Message(Debug, format("AudioCodecFFmpeg mime=%s,bitdepth=%d,codecID=%ld", cMimeType, bitdepth, (long)mCodecID));
    if (!pCodec)
        pCodec = avcodec_find_decoder(mCodecID);

    if (!pCodec)
    {
        strcpy(m_Err, "no codec found");
        return false;
    }

    m_pCodecContext = avcodec_alloc_context3(pCodec);
    if (!m_pCodecContext)
    {
        strcpy(m_Err, "bad context alloc");
        return false;
    }
    m_pCodecContext->debug_mv = 0;
    m_pCodecContext->debug = 0;
    m_pCodecContext->workaround_bugs = 1;

    if (pCodec->capabilities & CODEC_CAP_TRUNCATED)
        m_pCodecContext->flags |= CODEC_FLAG_TRUNCATED;

    m_pCodecContext->channels = env->GetIntField(formatObj, channelCountFID);
    m_pCodecContext->sample_rate = env->GetIntField(formatObj, sampleRateFID);
    m_pCodecContext->block_align = 0;
    m_pCodecContext->bit_rate = env->GetIntField(formatObj, bitrateFID);
    m_pCodecContext->bits_per_coded_sample = bitdepth;

    if(m_pCodecContext->bits_per_coded_sample == 0)
        m_pCodecContext->bits_per_coded_sample = 16;

    if (avcodec_open2(m_pCodecContext, pCodec, NULL) < 0)
    {
        strcpy(m_Err, "codec open failed");
        Dispose();
        return false;
    }

    m_pFrame1 = av_frame_alloc();
    if (!m_pFrame1)
    {
        strcpy(m_Err, "bad frame alloc");
        Dispose();
        return false;
    }

    return true;
}

void AudioCodecFFmpeg::Dispose()
{
    if (m_Buffer != NULL)
        delete m_Buffer;
    m_Buffer = NULL;

    av_frame_free(&m_pFrame1);
    avcodec_free_context(&m_pCodecContext);
}

int AudioCodecFFmpeg::Decode(uint8_t* pData, int iSize)
{
    int iBytesUsed;
    if (!m_pCodecContext)
    {
        strcpy(m_Err, "no context for decode");
        return -1;
    }

    AVPacket avpkt;
    av_init_packet(&avpkt);
    avpkt.data = pData;
    avpkt.size = iSize;
    iBytesUsed = avcodec_decode_audio4(m_pCodecContext, m_pFrame1, &m_gotFrame, &avpkt);
    if (iBytesUsed < 0 || !m_gotFrame)
    {
        av_strerror(iBytesUsed, m_Err, 255);
        return -1;
    }
    else if (iBytesUsed > iSize)
    {
        strcpy(m_Err, "decoder attempted to consume more data than given");
        iBytesUsed = iSize;
    }

    return iBytesUsed;
}

int AudioCodecFFmpeg::GetFrame(JNIEnv * env, jobject outputBuffer)
{
    if (m_Buffer == NULL)
    {
        m_Buffer = new AudioFrameBuffer(m_pCodecContext, mUse32Bit);
        if (!m_Buffer->hasValidSWR())
        {
            strcpy(m_Err, m_Buffer->getLastError());
            return -1;
        }
    }
    int ret = 0;
    if(m_gotFrame)
    {
        ret = m_Buffer->fill(env, m_pFrame1, outputBuffer);
        if (ret <= 0)
            strcpy(m_Err, m_Buffer->getLastError());
    }
    m_gotFrame = 0;

    return ret;
}

void AudioCodecFFmpeg::Reset()
{
    if (m_pCodecContext) avcodec_flush_buffers(m_pCodecContext);
        m_gotFrame = 0;
}

AVCodecID AudioCodecFFmpeg::getAVCodecID(const char* codec, int bitdepth)
{
    if (0 == strcmp(codec, "audio/mp4a-latm"))
        return AV_CODEC_ID_AAC;
    if (0 == strcmp(codec, "audio/raw"))
    {
        switch(bitdepth)
        {
            case 32:
                return AV_CODEC_ID_PCM_F32LE;
            case 24:
                return AV_CODEC_ID_PCM_S24LE;
            case 16:
                return AV_CODEC_ID_PCM_S16LE;
            case 8:
                return AV_CODEC_ID_PCM_S8;
            default:
                break;
        }
    }
    if (0 == strcmp(codec, "audio//mpeg"))
        return AV_CODEC_ID_MP3;
    if (0 == strcmp(codec, "audio/ac3"))
        return AV_CODEC_ID_AC3;
    if (0 == strcmp(codec, "audio/eac3"))
        return AV_CODEC_ID_EAC3;
    if (0 == strcmp(codec, "audio/true-hd"))
        return AV_CODEC_ID_TRUEHD;

    if (0 == strcmp(codec, "audio/vnd.dts") ||
        0 == strcmp(codec, "audio/vnd.dts.hd") ||
        0 == strcmp(codec, "audio/vnd.dts.hd;profile=lbr"))
        return AV_CODEC_ID_DTS;

    return AV_CODEC_ID_NONE;
}
