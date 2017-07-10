package com.monsterbutt.homeview.player;

import android.content.Context;
import android.media.AudioFormat;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.text.TextUtils;

import com.google.android.exoplayer2.audio.AudioCapabilities;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.C;
import com.monsterbutt.homeview.settings.SettingsManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MediaCodecCapabilities {

    public enum DecodeType {
        Hardware,
        Passthrough,
        Software,
        Unsupported
    }


    private static final int NO_ENCODING = -1;

    private static MediaCodecCapabilities gInstance = null;
    public static MediaCodecCapabilities getInstance(Context context) {

        if (gInstance == null)
            gInstance = new MediaCodecCapabilities(context);
        return gInstance;
    }

    private final Context context;
    private final AudioCapabilities audioCapabilities;
    private final MediaCodecInfo[] regularCodecs;
    private final Map<String, List<MediaCodecInfo>> videoDecoderCodecs = new HashMap<>();
    private final Map<String, List<MediaCodecInfo>> audioDecoderCodecs = new HashMap<>();
    private final Map<String, DecodeType>           subtitleDecoderCodecs = new HashMap<>();

    private static final Map<String,String> codecTranslations;
    static
    {
        codecTranslations = new HashMap<>();
        codecTranslations.put("video/h263",     MimeTypes.VIDEO_MPEG2);
        codecTranslations.put("video/mpeg2v",   MimeTypes.VIDEO_MPEG2);
        codecTranslations.put("video/mpeg2video",MimeTypes.VIDEO_MPEG2);
        codecTranslations.put("video/mpeg4",    MimeTypes.VIDEO_MP4V);
        codecTranslations.put("video/h264",     MimeTypes.VIDEO_H264);
        codecTranslations.put("video/h265",     MimeTypes.VIDEO_H265);
        codecTranslations.put("video/vc1",      MimeTypes.VIDEO_VC1);

        codecTranslations.put("audio/pcm",      MimeTypes.AUDIO_RAW);
        codecTranslations.put("audio/truehd",   MimeTypes.AUDIO_TRUEHD);
        codecTranslations.put("audio/dca",      MimeTypes.AUDIO_DTS);
        codecTranslations.put("audio/dca-hra",  MimeTypes.AUDIO_DTS_HD);
        codecTranslations.put("audio/dca-ma",   MimeTypes.AUDIO_DTS_HD);
        codecTranslations.put("audio/aac-lc",   MimeTypes.AUDIO_AAC);
        codecTranslations.put("audio/mp3",      MimeTypes.AUDIO_MPEG);

        codecTranslations.put("text/pgs",       MimeTypes.APPLICATION_PGS);
        codecTranslations.put("text/srt",       MimeTypes.APPLICATION_SUBRIP);
    }

    private MediaCodecCapabilities(Context context) {

        this.context = context;
        audioCapabilities = AudioCapabilities.getCapabilities(context);
        regularCodecs = (new MediaCodecList(MediaCodecList.REGULAR_CODECS)).getCodecInfos();
        fillCodecs();
    }

    private boolean isVideoCodec(String mimeType) {
        return mimeType.startsWith(MimeTypes.BASE_TYPE_VIDEO);
    }

    private boolean isAudioCodec(String mimeType) {
        return mimeType.startsWith(MimeTypes.BASE_TYPE_AUDIO);
    }

    private boolean isSubtitleCodec(String mimeType) {
        return mimeType.startsWith(MimeTypes.BASE_TYPE_TEXT)
                || mimeType.startsWith(MimeTypes.BASE_TYPE_APPLICATION);
    }

    private void fillCodecs() {

        for (MediaCodecInfo codec : regularCodecs) {

            if (codec.isEncoder())
                continue;
            for (String mimeType : codec.getSupportedTypes()) {

                if (isVideoCodec(mimeType))
                    fillCodec(videoDecoderCodecs, mimeType, codec);
                else if (isAudioCodec(mimeType))
                    fillCodec(audioDecoderCodecs, mimeType, codec);
            }
        }

        // use hardware for subs to keep the label from displaying, it's really software
        subtitleDecoderCodecs.put(MimeTypes.APPLICATION_PGS, DecodeType.Hardware);
        subtitleDecoderCodecs.put(MimeTypes.APPLICATION_SUBRIP, DecodeType.Hardware);
        subtitleDecoderCodecs.put(MimeTypes.APPLICATION_TTML, DecodeType.Hardware);
    }

    private void fillCodec(Map<String, List<MediaCodecInfo>> map, String mimeType, MediaCodecInfo codec) {

        List<MediaCodecInfo> list = map.get(mimeType);
        if (list == null) {

            list = new ArrayList<>();
            map.put(mimeType, list);
        }
        list.add(codec);
    }

    private int hasPassthroughForAudio(String mimeType, long bitDepth) {

        switch (mimeType) {
            case MimeTypes.AUDIO_AC3:
                if (audioCapabilities.supportsEncoding(AudioFormat.ENCODING_AC3))
                    return AudioFormat.ENCODING_AC3;
                break;
            case MimeTypes.AUDIO_E_AC3:
                if (audioCapabilities.supportsEncoding(AudioFormat.ENCODING_E_AC3))
                    return AudioFormat.ENCODING_E_AC3;
                break;
            case MimeTypes.AUDIO_TRUEHD:
                if (audioCapabilities.supportsEncoding(AudioFormat.ENCODING_DOLBY_TRUEHD))
                    return AudioFormat.ENCODING_DOLBY_TRUEHD;
                break;
            case MimeTypes.AUDIO_DTS:
                if (audioCapabilities.supportsEncoding(AudioFormat.ENCODING_DTS))
                    return AudioFormat.ENCODING_DTS;
                break;
            case MimeTypes.AUDIO_DTS_HD:
                if (audioCapabilities.supportsEncoding(AudioFormat.ENCODING_DTS_HD))
                    return AudioFormat.ENCODING_DTS_HD;
                break;
            case MimeTypes.AUDIO_RAW:
                if (bitDepth == 8)
                    return AudioFormat.ENCODING_PCM_8BIT;
                else if (bitDepth == 16)
                    return AudioFormat.ENCODING_PCM_16BIT;
                else if (bitDepth == 24)
                    return C.ENCODING_PCM_24BIT;
                else if (bitDepth == 32)
                    return C.ENCODING_PCM_32BIT;
                break;
            default:
                return NO_ENCODING;
        }
        return NO_ENCODING;
    }

    public boolean usePassthroughAudioIfAvailable() {

        return SettingsManager.getInstance(context).getBoolean("preferences_device_passthrough");
    }

    public DecodeType determineDecoderType(String trackType, String codec, String profile, long bitDepth) {

        String mimeType = !TextUtils.isEmpty(trackType) ? String.format("%s/%s", trackType, codec) : codec;

        if (isVideoCodec(mimeType))
            return determineVideoDecoderType(mimeType);
        if (isAudioCodec(mimeType))
            return determineAudioDecoderType(mimeType, profile, bitDepth);
        if (isSubtitleCodec(mimeType))
            return determinSubtitleDecoderType(mimeType);

        return DecodeType.Unsupported;
    }

    private DecodeType determineVideoDecoderType(String mimeType) {

        DecodeType ret = DecodeType.Unsupported;
        if (checkForDecoder(videoDecoderCodecs, mimeType))
            ret = DecodeType.Hardware;
        else {

            String alt = codecTranslations.get(mimeType);
            if (!TextUtils.isEmpty(alt) && checkForDecoder(videoDecoderCodecs, alt))
                ret = DecodeType.Hardware;
        }

        return ret;
    }

    private DecodeType determineAudioDecoderType(String mimeType, String profile, long bitDepth) {

        String mimeTypeProfile = String.format("%s-%s", mimeType, profile);

        DecodeType ret = !TextUtils.isEmpty(profile)
                        ?   determineAudioDecoderType(mimeTypeProfile, bitDepth)
                        :   DecodeType.Software
                ;
        if (ret != DecodeType.Passthrough && ret != DecodeType.Hardware) {

            // this can be changed when we can decode HD formats in software
            ret = determineAudioDecoderType(mimeType, bitDepth);
        }
        return ret;
    }

    private DecodeType determineAudioDecoderType(String mimeType, long bitDepth) {

        DecodeType ret = DecodeType.Software;
        if (determineAudioPassthrough(mimeType, bitDepth))
            ret = DecodeType.Passthrough;
        else {

            String alt = codecTranslations.get(mimeType);
            if (!TextUtils.isEmpty(alt) && determineAudioPassthrough(alt, bitDepth))
                ret = DecodeType.Passthrough;
            else if (checkForDecoder(audioDecoderCodecs, mimeType))
                ret = DecodeType.Hardware;
            else if (!TextUtils.isEmpty(alt) && checkForDecoder(audioDecoderCodecs,alt))
                    ret = DecodeType.Hardware;
        }
        return ret;
    }

    private boolean checkForDecoder(Map<String, List<MediaCodecInfo>> decoders, String mimeType) {

        if (decoders.get(mimeType) != null)
            return true;
        for (List<MediaCodecInfo> codecs : decoders.values()) {

            if (codecs == null || codecs.isEmpty())
                continue;

            for (MediaCodecInfo codec : codecs) {
                try {
                    if (mimeType.equals(codec.getName()) || codec.getCapabilitiesForType(mimeType) != null)
                        return true;
                }
                catch (IllegalArgumentException e) {}
                String[] types = codec.getSupportedTypes();
                if (types == null || types.length == 0)
                    continue;
                for (String type : types) {

                    if (type.equals(mimeType))
                        return true;
                }
            }
        }
        return false;
    }

    private boolean determineAudioPassthrough(String mimeType, long bitDepth) {

        boolean ret = false;
        int encoding = hasPassthroughForAudio(mimeType, bitDepth);
        if (encoding != NO_ENCODING)
            ret = audioCapabilities.supportsEncoding(encoding);
        return ret;
    }

    private DecodeType determinSubtitleDecoderType(String mimeType) {

        DecodeType ret = subtitleDecoderCodecs.get(mimeType);
        if (ret != DecodeType.Hardware) {

            String alt = codecTranslations.get(mimeType);
            if (!TextUtils.isEmpty(alt))
                ret = subtitleDecoderCodecs.get(alt);
            if (ret == null)
                ret = DecodeType.Unsupported;
        }
        return ret;
    }

    public AudioCapabilities getSystemAudioCapabilities() {

        return audioCapabilities;
    }
}
