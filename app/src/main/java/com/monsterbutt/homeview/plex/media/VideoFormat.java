package com.monsterbutt.homeview.plex.media;

import android.app.Fragment;
import android.content.Context;
import android.database.MatrixCursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

import java.util.List;

import us.nineworlds.plex.rest.model.impl.Media;


public class VideoFormat extends PlexLibraryItem implements Parcelable {

    private final Media mMedia;
    private final int mType;

    public final static int VideoFormat_Type_Codec = 1;
    public final static int VideoFormat_Type_Resolution = 2;
    public final static int VideoFormat_Type_FrameRate = 3;
    public final static int VideoFormat_Type_AspectRatio = 4;

    public final static String VideoCodec = "videoCodec";
    public final static String VideoResolution = "videoResolution";
    public final static String VideoFrameRate = "videoFrameRate";
    public final static String VideoAspectRatio = "aspectRatio";

    public VideoFormat(Media media, int type) {
        mMedia = media;
        mType = type;
    }

    protected VideoFormat(Parcel in) {
        mMedia = in.readParcelable(Media.class.getClassLoader());
        mType = in.readInt();
    }

    public static final Creator<VideoFormat> CREATOR = new Creator<VideoFormat>() {
        @Override
        public VideoFormat createFromParcel(Parcel in) {
            return new VideoFormat(in);
        }

        @Override
        public VideoFormat[] newArray(int size) {
            return new VideoFormat[size];
        }
    };

    @Override
    public String getKey() {
        return "";
    }

    @Override
    public long getRatingKey() {
        return 0;
    }

    @Override
    public String getSectionId() {
        return "";
    }

    @Override
    public String getSectionTitle() {
        return "";
    }

    @Override
    public String getType() {
        return Integer.toString(mType);
    }

    @Override
    public String getTitle() {
        return "";
    }

    @Override
    public String getSortTitle() {
        return "";
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
        return "";
    }

    @Override
    public String getCardImageURL() {
        return getThumbnailKey();
    }

    @Override
    public String getWideCardTitle(Context context) {
        return getCardContent(context);
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        mMedia.writeToParcel(dest, flags);
        dest.writeInt(mType);
    }
}
