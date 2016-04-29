#include <jni.h>

#include <string>

#include <unistd.h>

#include "libavcodec/version.h"
#include "libavdevice/version.h"
#include "libavfilter/version.h"
#include "libavformat/version.h"
#include "libavutil/version.h"
#include "libpostproc/version.h"
#include "libswresample/version.h"
#include "libswscale/version.h"


#include "util/Log.h"
#include "AudioCodecFFmpeg.h"


extern "C" {
#include "libavcodec/avcodec.h"
}
extern "C"
JNIEXPORT jstring JNICALL Java_com_monsterbutt_homeview_player_ffmpeg_FfmpegDecoder_getVersion
        (JNIEnv *env, jobject )
{
    std::string version = std::string("AVCodec    : ") + LIBAVCODEC_IDENT + "\n" +
                          std::string("AVDevice   : ") + LIBAVDEVICE_IDENT + "\n" +
                          std::string("AVFilter   : ") + LIBAVFILTER_IDENT + "\n" +
                          std::string("AVFormat   : ") + LIBAVFORMAT_IDENT + "\n" +
                          std::string("AVUtil     : ") + LIBAVUTIL_IDENT + "\n" +
                          std::string("AVPostProc : ") + LIBPOSTPROC_IDENT + "\n" +
                          std::string("AVResample : ") + LIBSWRESAMPLE_IDENT + "\n" +
                          std::string("AVSwScale  : ") + LIBSWSCALE_IDENT + "\n";

    return env->NewStringUTF(version.c_str());
}

extern "C"
JNIEXPORT jlong JNICALL Java_com_monsterbutt_homeview_player_ffmpeg_FfmpegDecoder_init
        (JNIEnv* env, jobject )
{
    AudioCodecFFmpeg* decoder = new AudioCodecFFmpeg();
    return (jlong)decoder;
}

extern "C"
JNIEXPORT jlong JNICALL Java_com_monsterbutt_homeview_player_ffmpeg_FfmpegDecoder_close
        (JNIEnv* , jobject , jlong context)
{
    AudioCodecFFmpeg* decoder = (AudioCodecFFmpeg*) context;
    if (decoder != NULL)
        delete decoder;
    return (jlong)0;
}

extern "C"
JNIEXPORT jboolean JNICALL Java_com_monsterbutt_homeview_player_ffmpeg_FfmpegDecoder_configure
        (JNIEnv* env, jobject , jlong context, jobject format, jboolean use32bit)
{
    AudioCodecFFmpeg* decoder = (AudioCodecFFmpeg*) context;
    return decoder != NULL
           ? (jboolean) decoder->Open(env, format, (bool)use32bit)
           : (jboolean) false;
}

extern "C"
JNIEXPORT jint JNICALL Java_com_monsterbutt_homeview_player_ffmpeg_FfmpegDecoder_decode
        (JNIEnv* env, jobject , jlong context, jobject buffer, jint offset, jint length)
{
    uint8_t* const directBuffer = reinterpret_cast<uint8_t*>(env->GetDirectBufferAddress(buffer));
    AudioCodecFFmpeg* decoder = (AudioCodecFFmpeg*) context;
    return  decoder != NULL
            ? (jint) decoder->Decode(directBuffer+offset, (int)(length-offset))
            : (jint) -1;
}

extern "C"
JNIEXPORT jint JNICALL Java_com_monsterbutt_homeview_player_ffmpeg_FfmpegDecoder_getFrame
        (JNIEnv * env, jobject obj, jlong context, jobject outputBuffer)
{
    AudioCodecFFmpeg* decoder = (AudioCodecFFmpeg*) context;
    return  decoder != NULL
            ? (jint) decoder->GetFrame(env, outputBuffer)
            : (jint) -1;
}

extern "C"
JNIEXPORT jstring JNICALL Java_com_monsterbutt_homeview_player_ffmpeg_FfmpegDecoder_getLastError
        (JNIEnv * env, jobject obj, jlong context)
{
    const char* err = NULL;
    AudioCodecFFmpeg* decoder = (AudioCodecFFmpeg*) context;
    if (decoder != NULL)
        err = decoder->getLastError();

    jstring result =env->NewStringUTF(err);
    return result;
}

JavaVM* g_vm = NULL;
extern "C" jint JNI_OnLoad(JavaVM *vm,  void *)
{
    g_vm = vm;
    Log::Message(Debug, "JNI_OnLoad");
    JNIEnv *env = NULL;
    if (g_vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK)
        return -1;

    avcodec_register_all();
    // Get jclass with env->FindClass.
    // Register methods with env->RegisterNatives

    return JNI_VERSION_1_6;
}

extern "C" void JNI_OnUnload(JavaVM *, void *)
{
    JNIEnv *env = NULL;
    if (g_vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK)
        return;
}
