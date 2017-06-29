package com.monsterbutt.homeview.plex.media;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.ActivityOptionsCompat;
import android.view.View;

import com.monsterbutt.homeview.ui.activity.ContainerActivity;
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

    public long getSeasonNum() {
        return mDirectory.getIndex();
    }

    public String getShowTitle() {
        return mDirectory.getParentTitle();
    }

    public String getShowSummary() {
        return mDirectory.getParentSummary();
    }

    public String getShowThumbKey() {
        return mDirectory.getParentThumbKey();
    }

    public int getEpisodeCount() {
        return Integer.valueOf(mDirectory.getLeafCount());
    }

    public int getUnwatchedEpisodeCount() {
        return getEpisodeCount() - Integer.valueOf(mDirectory.getViewedLeafCount());
    }

    @Override
    public String getCardTitle(Context context) {
        return getTitle();
    }

    @Override
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

        Intent intent = new Intent(fragment.getActivity(), ContainerActivity.class);
        intent.putExtra(ContainerActivity.KEY, getKey());
        intent.putExtra(ContainerActivity.USE_SCENE, true);
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
    public String getHeaderForChildren(Context context) {
        return context.getString(R.string.detailview_header_season);
    }
}
