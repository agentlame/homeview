package com.monsterbutt.homeview.plex.media;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.ActivityOptionsCompat;
import android.text.TextUtils;
import android.view.View;

import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.settings.SettingsManager;
import com.monsterbutt.homeview.ui.activity.ContainerActivity;
import com.monsterbutt.homeview.ui.activity.DetailsActivity;
import com.monsterbutt.homeview.R;


import us.nineworlds.plex.rest.model.impl.Directory;
import us.nineworlds.plex.rest.model.impl.MediaContainer;


public class Show extends PlexContainerItem implements Parcelable {

    public static final String TYPE = "show";
    private static final String ALL_EPISODE = "All episodes";

    Show(Directory dir) {
        super(dir);
        removeAllSeasonFromLeafCount();
    }

    Show(MediaContainer mc) {
        super(mc);
        removeAllSeasonFromLeafCount();
    }

    protected Show(Parcel in) {
        super(in);
    }

    private void removeAllSeasonFromLeafCount() {

        if (mDirectories != null && !mDirectories.isEmpty()) {

            int viewedLeafCount = 0;
            int leafCount = 0;
            for (PlexLibraryItem dir : mDirectories) {

                if (!dir.getTitle().equals(ALL_EPISODE)) {
                    leafCount += Integer.valueOf(((PlexContainerItem) dir).getLeafCount());
                    viewedLeafCount += Integer.valueOf(((PlexContainerItem)dir).getViewedLeafCount());
                }
            }

            mDirectory.setLeafCount(Integer.toString(leafCount));
            mDirectory.setViewedLeafCount(Integer.toString(viewedLeafCount));
        }
    }

    public int getSeasonIndex(int offset) {

        int index = 0;
        int leafCount = 0;
        for (PlexLibraryItem dir : mDirectories) {

            int count = Integer.valueOf(((PlexContainerItem) dir).getLeafCount());
            if (leafCount >= offset && offset < (count + leafCount))
                break;
            leafCount += count;
            ++index;
        }
        return index;
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
            intent.putExtra(ContainerActivity.EPISODEIST, true);
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

    private int getEpisodeCount() {

        if (mDirectory != null && !TextUtils.isEmpty(mDirectory.getLeafCount()))
            return Integer.valueOf(mDirectory.getLeafCount());

        int count = 0;
        if (mDirectories != null) {
            for (PlexLibraryItem dir : mDirectories) {
                if (!dir.getTitle().equals(ALL_EPISODE))
                    count += Integer.valueOf(((PlexContainerItem) dir).getLeafCount());
            }
        }
        return count;
    }

    private int getUnwatchedEpisodeCount() {

        if (mDirectory != null && !TextUtils.isEmpty(mDirectory.getViewedLeafCount()) &&
            !TextUtils.isEmpty(mDirectory.getLeafCount()))
            return Integer.valueOf(mDirectory.getLeafCount()) - Integer.valueOf(mDirectory.getViewedLeafCount());

        int count = 0;
        if (mDirectories != null) {
            for (PlexLibraryItem dir : mDirectories) {
                if (!dir.getTitle().equals(ALL_EPISODE))
                    count += dir.getUnwatchedCount();
            }
        }
        return count;
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
        return String.format("%s %s", Integer.toString(count), context.getString(R.string.episodes));
    }

    @Override
    public String getDetailContent(Context context) {

        int count = getUnwatchedEpisodeCount();
        if (count == 0)
            return "";
        return String.format("%s %s %s", Integer.toString(count), context.getString(R.string.unwatched),
                                                context.getString(R.string.episodes));
    }

    @Override
    protected com.monsterbutt.homeview.model.Video.VideoBuilder toVideo(Context context,
                                                                        com.monsterbutt.homeview.model.Video.VideoBuilder builder,
                                                                        PlexServer server) {
        super.toVideo(context, builder, server);
        return builder
         .category(TYPE)
         .showTitle(getTitle())
         .thumbNail(getBackgroundImageURL())
         .cardImageUrl(getThumbnailKey());
    }
}
