package com.monsterbutt.homeview.plex.media;

import android.app.Fragment;
import android.content.Context;
import android.database.MatrixCursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

import com.monsterbutt.homeview.R;

import java.util.List;


public class Stream extends PlexLibraryItem implements Parcelable {

    public final static int Audio_Stream = 2;
    public final static int Subtitle_Stream = 3;

    public final static  String SubtitleCodec = "textCodec";

    public final static  String AudioCodec = "audioCodec";
    public final static  String AudioChannels = "audioChannels";

    final us.nineworlds.plex.rest.model.impl.Stream mStream;
    final boolean modeA;
    public Stream (us.nineworlds.plex.rest.model.impl.Stream stream, boolean modeA) {
        mStream = stream;
        this.modeA = modeA;
    }

    protected Stream(Parcel in) {
        mStream = in.readParcelable(us.nineworlds.plex.rest.model.impl.Stream.class.getClassLoader());
        modeA = in.readByte() != 0;
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
            if (modeA)
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
            if (modeA)
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
    public boolean onClicked(Fragment fragment, View transitionView) {
        return false;
    }

    @Override
    public boolean onPlayPressed(Fragment fragment, View transitionView) {
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
        dest.writeInt(modeA ? 1 : 0);
    }
}
