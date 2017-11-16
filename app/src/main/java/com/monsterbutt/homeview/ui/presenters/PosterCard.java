package com.monsterbutt.homeview.ui.presenters;

import android.app.Fragment;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.Episode;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.ui.C;

public class PosterCard extends CardObject {

    protected PlexLibraryItem item;

    public PosterCard(Context context, PlexLibraryItem obj) {
        super(context);
        item = obj;
    }

    public final PlexLibraryItem getItem() { return item; }

    @Override
    public String getKey() { return item.getKey(); }

    @Override
    public String getParentKey() {
        return item.getParentKey();
    }

    @Override
    public String getRatingsKey() {
        if (item.getRatingKey() == 0)
            return getKey();
        return Long.toString(item.getRatingKey()); }

    @Override
    public String getTitle() {
        return item.getCardTitle(mContext);
    }

    @Override
    public String getSortTitle() { return item.getSortTitle(); }

    public String getSubtitle() {
        return item.getDetailSubtitle(mContext);
    }

    public String getDescription() {
        return item.getSummary();
    }

    @Override
    public String getContent() {
        return item.getCardContent(mContext);
    }

    @Override
    public String getImageUrl(PlexServer server) {

        String url = item.getCardImageURL();
        if (!TextUtils.isEmpty(url) && server != null)
            url = server.makeServerURL(url);
        return url;
    }

    @Override
    public String getBackgroundImageUrl() {
        return item.getBackgroundImageURL();
    }

    @Override
    public Drawable getImage(Context context) {
        if (context != null && item.getCardImageId() != 0)
            return context.getDrawable(item.getCardImageId());
        return null;
    }

    @Override
    public int getHeight() {
        return R.dimen.CARD_PORTRAIT_HEIGHT;
    }

    @Override
    public int getWidth() {
        return R.dimen.CARD_PORTRAIT_WIDTH;
    }

    @Override
    public PlexLibraryItem.WatchedState getWatchedState() {
        return item.getWatchedState();
    }

    @Override
    public void setWatchState(PlexLibraryItem.WatchedState updateStatus) {
        item.setStatus(updateStatus);
    }

    @Override
    public boolean updateStatus(C.StatusChanged status, int totalCount, int unwatchedCount) {
        boolean change = false;

        switch (status) {
            case SetDeleted:
                change = true;
                break;
            case SetWatched:
                if (getWatchedState() != PlexLibraryItem.WatchedState.Watched) {
                    item.toggleWatched();
                    change = true;
                }
                break;
            case SetUnwatched:
                if (getWatchedState() != PlexLibraryItem.WatchedState.Unwatched) {
                    item.toggleWatched();
                    change = true;
                }
                break;
            case Refresh:
                change = item.updateCounts(totalCount, unwatchedCount);
                break;
        }

        return change;
    }

    @Override
    public int getTotalLeaves() {
        return item.getTotalLeafCount();
    }

    @Override
    public int getUnwatchedLeaves() {
        return item.getUnwatchedCount();
    }

    @Override
    public int getUnwatchedCount() {
        return item.getUnwatchedCount();
    }

    @Override
    public int getViewedProgress() {
        return item.getViewedProgress();
    }

    public String getSeason() { return item instanceof Episode ? ((Episode) item).getSeasonNum() : ""; }

    public String getEpisode() { return item instanceof Episode ? ((Episode) item).getEpisodeNum() : ""; }

    @Override
    public boolean useItemBackgroundArt() { return item.useItemBackgroundArt(); }

    @Override
    public boolean onClicked(Fragment fragment, Bundle extras, View transitionView) {
        return item != null && item.onClicked(fragment, extras, transitionView);
    }

    @Override
    public boolean onPlayPressed(Fragment fragment, Bundle extras, View transitionView) {
        return item != null && item.onPlayPressed(fragment, extras, transitionView);
    }

    @Override
    public boolean onLongClicked(Fragment fragment, Bundle extras, View transitionView,
                                 CardPresenter.LongClickWatchStatusCallback callback) {
        return item != null && item.onLongClicked(this, fragment, extras, transitionView, callback);
    }

    String getDetailTitle() {
        return item == null ? "" : item.getDetailTitle(mContext);
    }

    String getDetailSubtitle() {
        return item == null ? "" : item.getDetailSubtitle(mContext);
    }

    String getSummary() {
        return item == null ? "" : item.getSummary();
    }
}
