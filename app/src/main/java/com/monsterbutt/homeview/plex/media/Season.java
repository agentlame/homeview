package com.monsterbutt.homeview.plex.media;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.ActivityOptionsCompat;
import android.view.View;

import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.ui.C;
import com.monsterbutt.homeview.ui.grid.GridActivity;
import com.monsterbutt.homeview.R;

import us.nineworlds.plex.rest.model.impl.Directory;
import us.nineworlds.plex.rest.model.impl.MediaContainer;


public class Season extends PlexContainerItem implements Parcelable {

    public static final String TYPE = "season";
    public static final String ALL_SEASONS = "allLeaves";

    public Season(Directory directory) {

        super(directory);
        mDirectory.setType(TYPE);
    }

    public Season(MediaContainer container) {

        super(container);
        mDirectory.setType(TYPE);
    }

    protected Season(Parcel in) {
        super(in);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Season> CREATOR = new Creator<Season>() {
        @Override
        public Season createFromParcel(Parcel in) {
            return new Season(in);
        }

        @Override
        public Season[] newArray(int size) {
            return new Season[size];
        }
    };

    private long getSeasonNum() {
        return mDirectory.getIndex();
    }

    public String getShowTitle() {
        return mDirectory.getParentTitle();
    }

  private String getShowSummary() {
        return mDirectory.getParentSummary();
    }

  private String getShowThumbKey() {
        return mDirectory.getParentThumbKey();
    }

  private int getEpisodeCount() {
        return Integer.valueOf(mDirectory.getLeafCount());
    }

  private int getUnwatchedEpisodeCount() {
        return getEpisodeCount() - Integer.valueOf(mDirectory.getViewedLeafCount());
    }

    @Override
    public String getCardTitle(Context context) {
        return getTitle();
    }

    @Override
    @SuppressLint("DefaultLocale")
    public String getCardContent(Context context) {

        String ret = String.format("%d %s", getEpisodeCount(),
                                            context.getString(R.string.episodes));
        if (getUnwatchedEpisodeCount() > 0)
            ret += String.format(" (%d)",   getUnwatchedEpisodeCount());
        return ret;
    }

    @Override
    public String getDetailTitle(Context context) {
        return getShowTitle();
    }

    @Override
    @SuppressLint("DefaultLocale")
    public String getDetailSubtitle(Context context) {
        String ret = String.format("%s %s %d %s", getTitle(), context.getString(R.string.mid_dot), getEpisodeCount(),
         context.getString(R.string.episodes));
        if (getUnwatchedEpisodeCount() > 0)
            ret += String.format(" (%d %s)",   getUnwatchedEpisodeCount(), context.getString(R.string.unwatched));
        return ret;
    }

    @Override
    public String getSummary() {
        return getShowSummary();
    }

    @Override
    public boolean onClicked(Fragment fragment, Bundle extras, View transitionView) {

        Intent intent = new Intent(fragment.getActivity(), GridActivity.class);
        intent.putExtra(C.KEY, getKey());
        intent.putExtra(C.EPISODEIST, true);
        intent.putExtra(C.BACKGROUND, getBackgroundImageURL());
        if (extras != null)
            intent.putExtras(extras);

        Bundle bundle = null;
        if (transitionView != null) {

            bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    fragment.getActivity(),
                    transitionView,
                    GridActivity.SHARED_ELEMENT_NAME).toBundle();
        }
        fragment.startActivity(intent, bundle);

        return true;
    }

    @Override
    public String getHeaderForChildren(Context context) {
        return context.getString(R.string.detailview_header_season);
    }

    @Override
    protected com.monsterbutt.homeview.model.Video.VideoBuilder toVideo(Context context,
                                                                        com.monsterbutt.homeview.model.Video.VideoBuilder builder,
                                                                        PlexServer server) {
        super.toVideo(context, builder, server);
        return builder
         .category(TYPE)
         .showTitle(getShowTitle())
         .seasonNum((int) getSeasonNum())
         .thumbNail(getBackgroundImageURL())
         .cardImageUrl(getShowThumbKey());
    }
}
