#pragma once

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


extern "C" {
#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
#include "libavutil/avutil.h"
#include "libswresample/swresample.h"
}

#include <jni.h>

class AudioFrameBuffer;

class AudioCodecFFmpeg
{
public:

    AudioCodecFFmpeg();
    ~AudioCodecFFmpeg();
    bool Open(JNIEnv * env, jobject format, bool use32Bit);
    void Dispose();
    int Decode(uint8_t* pData, int iSize);
    int GetFrame(JNIEnv * env, jobject outputBuffer);
    void Reset();

    const char* getLastError();

protected:

    static AVCodecID getAVCodecID(const char* codec, int bitdepth);

    AVCodecID mCodecID;
    bool      mUse32Bit;

    AVCodecContext* m_pCodecContext;
    AVFrame* m_pFrame1;
    int m_gotFrame;

    AudioFrameBuffer* m_Buffer;

    char m_Err[256];
};
