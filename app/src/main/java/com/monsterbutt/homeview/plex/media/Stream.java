package com.monsterbutt.homeview.plex.media;

import android.app.Fragment;
import android.content.Context;
import android.database.MatrixCursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.View;

import com.google.android.exoplayer.util.MimeTypes;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.player.MediaCodecCapabilities;
import com.monsterbutt.homeview.player.MediaTrackSelector;
import com.monsterbutt.homeview.plex.PlexServer;

import java.util.List;


public class Stream extends PlexLibraryItem implements Parcelable {

    public final static int Video_Stream = 1;
    public final static int Audio_Stream = 2;
    public final static int Subtitle_Stream = 3;

    public final static  String AudioCodec = "audioCodec";
    public final static  String AudioChannels = "audioChannels";

    private final static String Profile_DTS_MA = "ma";
    private final static String Profile_DTS_HR = "hra";


    final us.nineworlds.plex.rest.model.impl.Stream mStream;
    final private MediaCodecCapabilities.DecodeType mDecodeStatus;
    private int mTrackTypeIndex;
    public Stream (us.nineworlds.plex.rest.model.impl.Stream stream, int trackIndex, MediaCodecCapabilities capabilities) {
        mStream = stream;
        mTrackTypeIndex = trackIndex;
        if (mTrackTypeIndex == MediaTrackSelector.TrackTypeOff)
            mDecodeStatus = MediaCodecCapabilities.DecodeType.Hardware;
        else
            mDecodeStatus = capabilities.determineDecoderType(  getMimeTypeForTrackType(),
                                                                getCodec(),
                                                                getProfile(),
                                                                stream.getBitDepth());
    }

    protected Stream(Parcel in) {
        mStream = new us.nineworlds.plex.rest.model.impl.Stream(in);
        mDecodeStatus = MediaCodecCapabilities.DecodeType.valueOf(in.readString());
        mTrackTypeIndex = in.readInt();
    }

    public static final Creator<Stream> CREATOR = new Creator<Stream>() {
        @Override
        public Stream createFromParcel(Parcel in) {
            return new Stream(in);
        }

        @Override
        public Stream[] newArray(int size) {
            return new Stream[size];
        }
    };

    public long getId() { return mStream.getId(); }
    public String getIndex() { return mStream.getIndex(); }

    @Override
    public String getKey() {
        return Long.toString(mStream.getId());
    }

    @Override
    public long getRatingKey() {
        return 0;
    }

    @Override
    public String getSectionId() {
        return null;
    }

    @Override
    public String getSectionTitle() {
        return null;
    }

    @Override
    public String getType() { return Long.toString(mStream.getStreamType()); }

    public long getStreamType() { return mStream.getStreamType(); }

    @Override
    public String getTitle() {

        if (Audio_Stream == getStreamType()) {
                return mStream.getTitle();
        }
        if (Subtitle_Stream == getStreamType())
            return mStream.getLanguage();

        return "";
    }

    @Override
    public String getSortTitle() {
        return mStream.getIndex();
    }

    @Override
    public String getThumbnailKey() {
        return "";
    }

    @Override
    public String getArt() {
        return null;
    }

    @Override
    public long getAddedAt() {
        return 0;
    }

    @Override
    public long getUpdatedAt() {
        return 0;
    }

    @Override
    public String getSummary() {
        return null;
    }

    @Override
    public String getCardTitle(Context context) {
        return getTitle();
    }

    @Override
    public String getCardContent(Context context) {

        if( Audio_Stream == mStream.getStreamType()) {
                return mStream.getLanguage();
        }
        else if (Subtitle_Stream == mStream.getStreamType()) {

            if (mStream.getForced() > 0)
                return context.getString(R.string.Forced);
        }
        return "";
    }

    @Override
    public String getCardImageURL() {
        return getThumbnailKey();
    }

    @Override
    public String getWideCardTitle(Context context) {
        return getCardTitle(context);
    }

    @Override
    public String getWideCardContent(Context context) {
        return getCardContent(context);
    }

    @Override
    public String getWideCardImageURL() {
        return getCardImageURL();
    }

    @Override
    public String getBackgroundImageURL() {
        return null;
    }

    @Override
    public WatchedState getWatchedState() {
        return WatchedState.Watched;
    }

    @Override
    public int getUnwatchedCount() {
        return 0;
    }

    @Override
    public int getViewedProgress() {
        return 0;
    }

    @Override
    public boolean useItemBackgroundArt() {
        return false;
    }

    @Override
    public String getHeaderForChildren(Context context) {
        return null;
    }

    @Override
    public List<PlexLibraryItem> getChildrenItems() {
        return null;
    }

    @Override
    public boolean onClicked(Fragment fragment, Bundle extras, View transitionView) {
        return false;
    }

    @Override
    public boolean onPlayPressed(Fragment fragment, Bundle extras, View transitionView) {
        return false;
    }

    @Override
    public void fillQueryRow(MatrixCursor.RowBuilder row, Context context, String keyOverride, String yearOverride, boolean isStartOverride) {

    }

    public String getChannels() { return mStream.getChannels(); }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        mStream.writeToParcel(dest, flags);
        dest.writeString(mDecodeStatus.name());
        dest.writeInt(mTrackTypeIndex);
    }

    public static boolean profileIsDtsHdVariant(String profile) {
        return profile.equals(Profile_DTS_MA) || profile.equals(Profile_DTS_HR);
    }

    public int getTrackType() {
        return (int) mStream.getStreamType();
    }

    public boolean isForced() {
        return 0 < mStream.getForced();
    }

    public boolean isDefault() {
        return 0 < mStream.getDefault();
    }

    public MediaCodecCapabilities.DecodeType getDecodeStatus() {
        return mDecodeStatus;
    }

    public String getCodec() {
        return mStream.getCodec();
    }

    public String getCodecAndProfile() {

        String codec = getCodec();
        String profile = getProfile();
        if (codec.equals("pcm"))
            codec = profile;
        else if (!TextUtils.isEmpty(profile))
            codec += "-" + profile;
        return codec;
    }

    public String getProfile() { return mStream.getProfile(); }

    public String getHeight() {
        return mStream.getHeight();
    }

    public String getLanguage() {
        return mStream.getLanguage();
    }

    public String getLanguageCode() {
        return mStream.getLanguageCode();
    }

    public String getDecodeStatusText(Context context) {

        switch (getDecodeStatus()) {

            case Software:
                return context.getString(R.string.software);
            case Passthrough:
                return context != null ? context.getString(R.string.passthrough) : "Passthrough";
            case Unsupported:
                return context.getString(R.string.unsupported);
            case Hardware:
            default:
                break;
        }

        return "";
    }

    public int getTrackTypeIndex() {
        return mTrackTypeIndex;
    }

    public String getFrameRate () { return mStream.getFrameRate(); }

    public String getAudioChannelLayout() {

        String ret = mStream.getAudioChannelLayout();
        if (TextUtils.isEmpty(ret))
            ret = convertToLayout(mStream.getChannels());
        return ret;
    }

    private String convertToLayout(String channels) {

        if (TextUtils.isEmpty(channels))
            return "";
        int chnls = Integer.valueOf(channels);
        if (8 == chnls)
                return "7.1";
        if (7 == chnls)
                return "6.1";
        if (6 == chnls)
            return "5.1";
        if (5 == chnls)
            return "4.1";
        if (4 == chnls)
            return "4.0";
        if (3 == chnls)
            return "2.1";
        if (2 == chnls)
            return "2.0";
        if (1 == chnls)
            return "1.0";
         return "";
    }

    public static class StreamChoice {

        private final Stream stream;
        private final boolean isCurrent;
        private final Context context;

        public StreamChoice(Context context, boolean isCurrent, Stream stream) {

            this.context = context;
            this.isCurrent = isCurrent;
            this.stream = stream;
        }

        public Drawable getDrawable() {

            switch(stream.getTrackType()) {
                case Subtitle_Stream:
                    return context.getDrawable(R.drawable.ic_subtitles_white_48dp);
                case Video_Stream:
                    return context.getDrawable(R.drawable.ic_video_label_white_48dp);
                case Audio_Stream:
                    return context.getDrawable(R.drawable.ic_surround_sound_white_48dp);
                default:
                    return null;
            }
        }

        public String getCodecImage(PlexServer server) {

            String codecType;
            switch(stream.getTrackType()) {
                case Video_Stream:
                    codecType = "videoCodec";
                    break;
                case Audio_Stream:
                    codecType = "audioCodec";
                    break;
                default:
                    return "";
            }
            return server.makeServerURLForCodec(codecType, stream.getCodecAndProfile());
        }

        public boolean isCurrentSelection() { return isCurrent; }

        public String getDecoderStatus() { return stream.getDecodeStatusText(context); }

        @Override
        public String toString() {

            String desc;
            switch (stream.getTrackType()) {

                case Video_Stream:
                    return String.format("%s (%s)", stream.getCodecAndProfile(), stream.getFrameRate());

                case Audio_Stream:
                    String lang = stream.getLanguage();
                    String title = stream.getTitle();
                    desc = String.format("%s, %s, %s", lang, stream.getCodecAndProfile(), stream.getAudioChannelLayout());
                    if (TextUtils.isEmpty(title)) {
                        title = lang;
                        desc = String.format("%s, %s", stream.getCodecAndProfile(), stream.getAudioChannelLayout());
                    }
                    return String.format("%s (%s)", title, desc);

                case Subtitle_Stream:
                    desc = stream.getCodec();
                    if (stream.isForced())
                        desc += " , " + context.getString(R.string.Forced);
                    if (stream.isDefault())
                        desc += ", "  + context.getString(R.string.Default);
                    if (!TextUtils.isEmpty(desc))
                        return String.format("%s (%s)", stream.getLanguage(), desc);
                    return stream.getLanguage();

                default:
                    return String.format("%s (%s)", stream.getLanguage(), stream.getCodecAndProfile());

            }
        }
    }

    private String getMimeTypeForTrackType() {

        switch (getTrackType()) {

            case Stream.Video_Stream:
                return MimeTypes.BASE_TYPE_VIDEO;
            case Stream.Audio_Stream:
                return MimeTypes.BASE_TYPE_AUDIO;
            case Stream.Subtitle_Stream:
                return MimeTypes.BASE_TYPE_TEXT;
            default:
                return "";
        }

    }
}
