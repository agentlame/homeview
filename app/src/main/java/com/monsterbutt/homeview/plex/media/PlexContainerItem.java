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

import com.monsterbutt.homeview.data.VideoContract;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.provider.MediaContentProvider;
import com.monsterbutt.homeview.provider.SearchImagesProvider;
import com.monsterbutt.homeview.ui.activity.ContainerActivity;
import com.monsterbutt.homeview.ui.activity.PlaybackActivity;
import com.monsterbutt.homeview.ui.handler.WatchedStatusHandler;

import java.util.ArrayList;
import java.util.List;

import us.nineworlds.plex.rest.model.impl.Directory;
import us.nineworlds.plex.rest.model.impl.Genre;
import us.nineworlds.plex.rest.model.impl.MediaContainer;
import us.nineworlds.plex.rest.model.impl.Video;


public class PlexContainerItem extends PlexLibraryItem implements Parcelable {

    public static String CHILDREN = "children";
    public static String ALL = "all";

    protected Directory mDirectory = null;
    protected List<PlexLibraryItem> mVideos = new ArrayList<>();
    protected List<PlexLibraryItem> mDirectories = new ArrayList<>();

    protected PlexContainerItem(Parcel in) {
        mDirectory = in.readParcelable(Directory.class.getClassLoader());
        mCurrFilter = in.readString();
    }

    public static final Creator<PlexContainerItem> CREATOR = new Creator<PlexContainerItem>() {
        @Override
        public PlexContainerItem createFromParcel(Parcel in) {
            return new PlexContainerItem(in);
        }

        @Override
        public PlexContainerItem[] newArray(int size) {
            return new PlexContainerItem[size];
        }
    };

    static public PlexContainerItem getItem(Directory directory) {

        String type = directory.getType() != null ? directory.getType() : "";
        if (type.equalsIgnoreCase(Shows.TYPE)) {

            if (directory.getScanner() != null && !directory.getScanner().isEmpty())
                return new Shows(directory);
            return new Show(directory);
        }
        else if (type.equalsIgnoreCase(Movies.TYPE)) {

            if (directory.getScanner().equalsIgnoreCase(Videos.SCANNER))
                return new Videos(directory);
            return new Movies(directory);
        }
        else if (TextUtils.isEmpty(type)) {

            return new Folder(directory);
        }
        return null;
    }

    static public PlexContainerItem getItem(MediaContainer mc) {

        String type = mc.getViewGroup();
        if (type.equalsIgnoreCase(Season.TYPE))
            return new Show(mc);
        else if (type.equalsIgnoreCase(Episode.TYPE))
            return new Season(mc);
        return null;
    }

    protected PlexContainerItem(Directory directory) {

        mCurrFilter = ALL;
        mDirectory = directory;
    }

    protected PlexContainerItem(MediaContainer mc) {

        mCurrFilter = ALL;
        mDirectory = new Directory();
        mDirectory.setLibrarySectionID(mc.getLibrarySectionID());
        mDirectory.setLibrarySectionTitle(mc.getLibrarySectionTitle());
        mDirectory.setRatingKey(mc.getKey());
        mDirectory.setParentRatingKey(mc.getGrandparentRatingKey());
        mDirectory.setParentTitle(mc.getGrandparentTitle());
        mDirectory.setParentThumbKey(mc.getGrandparentThumb());
        mDirectory.setParentThemeKey(mc.getGrandparentTheme());
        mDirectory.setParentYear(mc.getParentYear());
        mDirectory.setContentRating(mc.getGrandparentContentRating());
        mDirectory.setStudio(mc.getGrandparentStudio());
        mDirectory.setBanner(mc.getBannerKey());
        mDirectory.setArt(mc.getArt());
        mDirectory.setTitle(mc.getTitle2());
        mDirectory.setThumb(mc.getParentPosterURL());
        mDirectory.setThemeKey(mc.getThemeKey());
        mDirectory.setIndex(mc.getParentIndex());
        mDirectory.setViewGroup(mc.getViewGroup());
        mDirectory.setViewMode(mc.getViewMode());
        mDirectory.setSummary(mc.getSummary());

        List<Video> videos = mc.getVideos();
        if (videos != null) {

            int viewed = 0;
            mDirectory.setLeafCount(Integer.toString(videos.size()));
            for (Video video  : videos) {

                PlexVideoItem vid = PlexVideoItem.getItem(video);
                if (vid != null) {

                    if (vid.getWatchedState() == PlexVideoItem.WatchedState.Watched)
                        ++viewed;
                    mVideos.add(vid);
                }
            }
            mDirectory.setViewedLeafCount(Integer.toString(viewed));
        }

        List<Directory> directories = mc.getDirectories();
        if (directories != null) {

            int viewed = 0;
            int total = 0;
            for (Directory directory : directories) {

                PlexContainerItem dir = null;
                if (this instanceof Show)
                    dir = new Season(directory);
                else if (this instanceof Shows)
                    dir = new Show(directory);
                else if (this instanceof Videos)
                    dir = new Videos(directory);

                if (dir != null) {

                    viewed += Integer.valueOf(directory.getViewedLeafCount());
                    total += Integer.valueOf(directory.getLeafCount());
                    mDirectories.add(dir);
                }
            }
            mDirectory.setLeafCount(Integer.toString(total));
            mDirectory.setViewedLeafCount(Integer.toString(viewed));
        }
    }

    private String mCurrFilter = "";
    public String getBaseFilter() { return ALL; }
    public String getCurrentFilter() {
        return mCurrFilter.isEmpty() ? getBaseFilter() : mCurrFilter;
    }
    public void setCurrentFilter(String filter) {
        mCurrFilter = filter;
    }

    @Override
    public String getKey() {
        return mDirectory.getKey();
    }

    @Override
    public long getRatingKey() {
        return mDirectory.getRatingKey();
    }

    @Override
    public String getSectionId() {
        if (null == mDirectory.getLibrarySectionID())
            return mDirectory.getKey();
        return mDirectory.getLibrarySectionID();
    }

    @Override
    public String getSectionTitle() {
        return mDirectory.getLibrarySectionTitle();
    }

    @Override
    public String getType() {
        return mDirectory.getType();
    }

    @Override
    public String getTitle() {
        return mDirectory.getTitle();
    }

    @Override
    public String getSortTitle() {
        return mDirectory.getTitle();
    }

    @Override
    public String getThumbnailKey() {
        return mDirectory.getThumb();
    }

    @Override
    public String getArt() {
        return mDirectory.getArt();
    }

    @Override
    public long getAddedAt() {
        return mDirectory.getCreatedAt();
    }

    @Override
    public long getUpdatedAt() {
        return mDirectory.getUpdatedAt();
    }

    @Override
    public String getSummary() {
        return mDirectory.getSummary();
    }

    @Override
    public String getThemeKey(PlexServer server) {
        String ret = mDirectory.getThemeKey();
        if (TextUtils.isEmpty(ret))
            ret = mDirectory.getParentThemeKey();
        if (!TextUtils.isEmpty(ret))
            ret = server.makeServerURL(ret);
        return ret;
    }

    @Override
    public String getCardTitle(Context context) {
        return getTitle();
    }

    @Override
    public String getCardContent(Context context) {
        return "";
    }

    public long getChildCount() {
        return mDirectory.getChildCount();
    }

    @Override
    public String getCardImageURL() {

        if (mDirectory.getThumb() != null && !mDirectory.getThumb().isEmpty())
            return mDirectory.getThumb();
        return mDirectory.getParentThumbKey();
    }

    @Override
    public String getWideCardTitle(Context context) {
        return getCardTitle(context);
    }

    @Override
    public String getWideCardContent(Context context) {
        return getCardContent(context);
    }

    public String getWideCardImageURL() {
        return mDirectory.getComposite(COMPOSITE_COLS,
                COMPOSITE_ROW,
                COMPOSITE_HEIGHT,
                COMPOSITE_WIDTH);
    }

    @Override
    public String getBackgroundImageURL() {
        return mDirectory.getArt();
    }

    @Override
    public void toggleWatched() {

        if (null == mDirectory || null == mDirectory.getLeafCount() || null == mDirectory.getViewedLeafCount())
            return;

        final boolean isWatched = getWatchedState() == WatchedState.Watched;
        if (isWatched)
            mDirectory.setViewedLeafCount("0");
        else
            mDirectory.setViewedLeafCount(mDirectory.getLeafCount());
    }

    @Override
    public WatchedState getWatchedState() {

        return PlexContainerItem.getWatchedState(mDirectory);
    }

    public static WatchedState getWatchedState(Directory directory) {

        if (null == directory || null == directory.getLeafCount() || null == directory.getViewedLeafCount())
            return WatchedState.Watched;

        int unwatched = getUnwatchedCount(directory);
        if (unwatched == 0)
            return WatchedState.Watched;

        int watched = getWatchedCount(directory);
        if (watched == 0)
            return WatchedState.Unwatched;
        return WatchedState.PartialWatched;
    }

    @Override
    public long getViewedOffset() {

        if (null == mDirectory || null == mDirectory.getLeafCount() || null == mDirectory.getViewedLeafCount())
            return 0;
        return Long.valueOf(mDirectory.getViewedLeafCount());
    }

    public int getWatchedCount() {

        return PlexContainerItem.getWatchedCount(mDirectory);
    }

    public static int getWatchedCount(Directory directory) {

        if (null == directory || null == directory.getLeafCount() || null == directory.getViewedLeafCount())
            return 0;
        return Integer.valueOf(directory.getViewedLeafCount());
    }

    @Override
    public void setStatus(WatchedStatusHandler.UpdateStatus status) {

        if (null == mDirectory || null == mDirectory.getLeafCount() || null == mDirectory.getViewedLeafCount())
            return;
        mDirectory.setViewedLeafCount(Long.toString(status.viewedOffset));
    }

    @Override
    public int getUnwatchedCount() {

        return PlexContainerItem.getUnwatchedCount(mDirectory);
    }

    public static int getUnwatchedCount(Directory directory) {

        if (null == directory || null == directory.getLeafCount() || null == directory.getViewedLeafCount())
            return 0;

        return  Integer.valueOf(directory.getLeafCount()) - Integer.valueOf(directory.getViewedLeafCount());
    }

    @Override
    public int getViewedProgress() {

        int ret = 0;
        int unwatched = getUnwatchedCount();
        if (unwatched > 0)
            ret = 100 - (int)  (100 * ((double) unwatched / Double.valueOf(mDirectory.getLeafCount())));
        return ret;
    }

    @Override
    public boolean useItemBackgroundArt() {

        return mDirectory.getComposite(0, 0, 0, 0).isEmpty();
    }


    @Override
    public String getHeaderForChildren(Context context) { return ""; }

    @Override
    public boolean shouldChildRowWatchState() {
        return mVideos.isEmpty();
    }

    @Override
    public List<PlexLibraryItem> getChildrenItems() {

        if (!mVideos.isEmpty())
            return mVideos;
        return mDirectories;
    }

    public String getLeafCount() {
        return mDirectory.getLeafCount();
    }

    public String getYear() {

        String ret = mDirectory.getYear();
        if (TextUtils.isEmpty(ret))
            ret = mDirectory.getParentYear();
        return ret;
    }

    public String getStudio() {
        return mDirectory.getStudio();
    }

    public String getViewedLeafCount() {
        return mDirectory.getViewedLeafCount();
    }

    @Override
    public boolean onClicked(Fragment fragment, Bundle extras, View transitionView) {

        Intent intent = new Intent(fragment.getActivity(), ContainerActivity.class);
        intent.putExtra(ContainerActivity.KEY, getKey());
        intent.putExtra(ContainerActivity.BACKGROUND, getBackgroundImageURL());
        if (extras != null)
            intent.putExtras(extras);

        Bundle bundle = null;
        if (transitionView != null) {

            bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    fragment.getActivity(),
                    transitionView,
                    ContainerActivity.SHARED_ELEMENT_NAME).toBundle();
        }
        fragment.startActivity(intent, bundle);

        return true;
    }

    @Override
    public boolean onPlayPressed(Fragment fragment, Bundle extras, View transitionView) {

        Intent intent = new Intent(fragment.getActivity(), PlaybackActivity.class);
        intent.putExtra(PlaybackActivity.KEY, getKey());
        intent.putExtra(PlaybackActivity.FILTER, getCurrentFilter());
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
    public void fillQueryRow(MatrixCursor.RowBuilder row, Context context, String keyOverride, String yearOverride, boolean isStartOverride) {

        row.add(VideoContract.VideoEntry._ID, getRatingKey());
        row.add(VideoContract.VideoEntry.COLUMN_CONTENT_TYPE, "video/*");
        row.add(VideoContract.VideoEntry.COLUMN_NAME, getTitle());
        row.add(VideoContract.VideoEntry.COLUMN_SUBTITLE, getCardContent(context));
        row.add(VideoContract.VideoEntry.COLUMN_DESC, getSummary());
        row.add(VideoContract.VideoEntry.COLUMN_STUDIO, getStudio());
        row.add(VideoContract.VideoEntry.COLUMN_VIDEO_URL, getKey());
        row.add(VideoContract.VideoEntry.COLUMN_IS_LIVE, "false");
        row.add(VideoContract.VideoEntry.COLUMN_PRODUCTION_YEAR, getYear());
        row.add(VideoContract.VideoEntry.COLUMN_DURATION, getLeafCount());
        row.add(VideoContract.VideoEntry.COLUMN_BG_IMAGE_URL, SearchImagesProvider.CONTENT_URI + getBackgroundImageURL());
        row.add(VideoContract.VideoEntry.COLUMN_CARD_IMG, SearchImagesProvider.CONTENT_URI + getThumbnailKey());
        row.add(VideoContract.VideoEntry.COLUMN_DATA_ID, MediaContentProvider.ID_DETAIL);
        row.add(VideoContract.VideoEntry.COLUMN_EXTRA, getKey());
        row.add(VideoContract.VideoEntry.COLUMN_KEY, getKey());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mDirectory, flags);
        dest.writeString(mCurrFilter);
    }

    @Override
    public String getDetailTitle(Context context) { return getCardTitle(context); }

    @Override
    public String getDetailSubtitle(Context context) { return ""; }

    @Override
    public String getDetailContent(Context context) { return ""; }

    @Override
    public String getDetailYear(Context context) { return getYear(); }

    @Override
    public String getDetailDuration(Context context) { return ""; }

    @Override
    public String getDetailStudioPath(PlexServer server) { return server.makeServerURLForCodec("studio", getStudio()); }

    @Override
    public String getDetailRatingPath(PlexServer server) { return server.makeServerURLForCodec("contentRating", mDirectory.getContentRating()); }

    @Override
    public String getDetailGenre(Context context) {

        String ret = "";
        if (mDirectory.getGenres() != null) {
            for (Genre genre : mDirectory.getGenres()) {
                if (!TextUtils.isEmpty(ret))
                    ret += ", ";
                ret += genre.getTag();
            }
        }
        return ret;
    }
}
