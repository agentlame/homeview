package com.monsterbutt.homeview.plex.media;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.ActivityOptionsCompat;
import android.text.TextUtils;
import android.view.View;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.Utils;
import com.monsterbutt.homeview.ui.activity.PlaybackActivity;

import java.util.List;


public class Chapter extends PlexLibraryItem implements Parcelable {

    final us.nineworlds.plex.rest.model.impl.Chapter mChapter;
    final String key;
    final PlexVideoItem mVideo;

    public Chapter(PlexVideoItem video, String parentKey, us.nineworlds.plex.rest.model.impl.Chapter chapter) {
        mChapter = chapter;
        key = parentKey;
        mVideo = video;
    }

    protected Chapter(Parcel in) {
        mChapter = in.readParcelable(us.nineworlds.plex.rest.model.impl.Chapter.class.getClassLoader());
        key = in.readString();
        mVideo = in.readParcelable(PlexVideoItem.class.getClassLoader());
    }

    public static final Creator<Chapter> CREATOR = new Creator<Chapter>() {
        @Override
        public Chapter createFromParcel(Parcel in) {
            return new Chapter(in);
        }

        @Override
        public Chapter[] newArray(int size) {
            return new Chapter[size];
        }
    };

    @Override
    public String getKey() {
        return key + "/" + Long.toString(mChapter.getindex());
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
        return "chapter";
    }

    @Override
    public String getTitle() {
        return mChapter.getTag();
    }

    @Override
    public String getSortTitle() {
        return null;
    }

    @Override
    public String getThumbnailKey() {
        return null;
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

        String title = mChapter.getTag();
        if (TextUtils.isEmpty(title))
            title = context.getString(R.string.chapter) + Long.toString(mChapter.getindex());

        return title;
    }

    public long getChapterStart() { return mChapter.getStartTimeOffset(); }

    @Override
    public String getCardContent(Context context) {
        return Utils.timeMStoString(context, mChapter.getStartTimeOffset());
    }

    @Override
    public String getCardImageURL() {
        return mChapter.getThumb();
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
        return mChapter.getThumb();
    }

    @Override
    public String getBackgroundImageURL() {
        return "";
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

        Intent intent = new Intent(fragment.getActivity(), PlaybackActivity.class);
        intent.putExtra(PlaybackActivity.KEY, key);
        intent.putExtra(PlaybackActivity.START_OFFSET, mChapter.getStartTimeOffset());
        intent.putExtra(PlaybackActivity.VIDEO, mVideo);
        if (extras != null)
            intent.putExtras(extras);

        Bundle bundle = null;
        if (transitionView != null) {

            bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    fragment.getActivity(),
                    transitionView,
                    PlaybackActivity.SHARED_ELEMENT_NAME).toBundle();
        }
        fragment.startActivity(intent, bundle);
        return true;
    }

    @Override
    public boolean onPlayPressed(Fragment fragment, Bundle extras, View transitionView) {
        return onClicked(fragment, extras, transitionView);
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
        dest.writeParcelable(mChapter, flags);
        dest.writeString(key);
        dest.writeParcelable(mVideo, flags);
    }
}
