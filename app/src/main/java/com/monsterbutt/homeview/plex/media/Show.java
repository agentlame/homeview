package com.monsterbutt.homeview.plex.media;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.ActivityOptionsCompat;
import android.view.View;

import com.monsterbutt.homeview.settings.SettingsManager;
import com.monsterbutt.homeview.ui.activity.ContainerActivity;
import com.monsterbutt.homeview.ui.activity.DetailsActivity;
import com.monsterbutt.homeview.R;

import us.nineworlds.plex.rest.model.impl.Directory;
import us.nineworlds.plex.rest.model.impl.MediaContainer;


public class Show extends PlexContainerItem implements Parcelable {

    public static final String TYPE = "show";
    public static final String ALL_EPISODE = "All episodes";

    Show(Directory dir) {
        super(dir);
    }

    Show(MediaContainer mc) {
        super(mc);
    }

    protected Show(Parcel in) {
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

    public static final Creator<Show> CREATOR = new Creator<Show>() {
        @Override
        public Show createFromParcel(Parcel in) {
            return new Show(in);
        }

        @Override
        public Show[] newArray(int size) {
            return new Show[size];
        }
    };

    @Override
    public boolean onClicked(Fragment fragment, Bundle extras, View transitionView) {

        boolean collapseSingleSeason = SettingsManager.getInstance(fragment.getActivity())
                                            .getBoolean("preferences_navigation_collapsesingleseason");
        if (collapseSingleSeason && mDirectory.getChildCount() == 1) {

            Intent intent = new Intent(fragment.getActivity(), ContainerActivity.class);
            intent.putExtra(ContainerActivity.KEY, getKey().replace(Season.CHILDREN, Season.ALL_SEASONS));
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
        }
        else {

            Intent intent = new Intent(fragment.getActivity(), DetailsActivity.class);
            intent.putExtra(DetailsActivity.ITEM, this);
            intent.putExtra(DetailsActivity.BACKGROUND, getBackgroundImageURL());
            if (extras != null)
                intent.putExtras(extras);

            Bundle bundle = null;
            if (transitionView != null) {

                bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        fragment.getActivity(),
                        transitionView,
                        DetailsActivity.SHARED_ELEMENT_NAME).toBundle();
            }
            fragment.startActivity(intent, bundle);
        }
        return true;
    }

    private PlexContainerItem getAllEpisodes() {

        PlexContainerItem ret = null;
        for (PlexLibraryItem dir : mDirectories) {
            if (dir.getTitle().equals(ALL_EPISODE))
                ret = (PlexContainerItem) dir;
        }
        return ret;
    }

    public int getEpisodeCount() {

        PlexContainerItem all = getAllEpisodes();
        if (all != null)
            return Integer.valueOf(all.getLeafCount());
        return 0;
    }

    public int getUnwatchedEpisodeCount() {

        PlexContainerItem all = getAllEpisodes();
        if (all != null)
            return getEpisodeCount() - Integer.valueOf(all.getViewedLeafCount());
        return super.getUnwatchedCount();
    }

    @Override
    public String getCardContent(Context context) {

        String ret = mDirectory.getParentYear();
        if (ret == null)
            ret = mDirectory.getYear();
        return ret;
    }

    @Override
    public String getHeaderForChildren(Context context) {
        return context.getString(R.string.detailview_header_show);
    }

    @Override
    public String getDetailDuration(Context context) {
        int count = getEpisodeCount();
        if (count == 0)
            return "";
        return String.format("%d %s", count, context.getString(R.string.episodes));
    }

    @Override
    public String getDetailContent(Context context) {

        int count = getUnwatchedEpisodeCount();
        if (count == 0)
            return "";
        return String.format("%d %s %s", count, context.getString(R.string.unwatched),
                                                context.getString(R.string.episodes));
    }
}
