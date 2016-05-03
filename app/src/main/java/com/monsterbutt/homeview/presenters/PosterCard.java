package com.monsterbutt.homeview.presenters;

import android.app.Fragment;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.ui.handler.WatchedStatusHandler;


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
    public String getRatingsKey() {
        if (item.getRatingKey() == 0)
            return getKey();
        return Long.toString(item.getRatingKey()); }

    @Override
    public WatchedStatusHandler.UpdateStatus getUpdateStatus() {

        return new WatchedStatusHandler.UpdateStatus(Long.toString(item.getRatingKey()),
                                                     item.getViewedOffset(),
                                                     item.getWatchedState());
    }

    @Override
    public String getTitle() {
        return item.getCardTitle(mContext);
    }

    @Override
    public String getContent() {
        return item.getCardContent(mContext);
    }

    @Override
    public String getImageUrl(PlexServer server) {

        String url = item.getCardImageURL();
        if (!TextUtils.isEmpty(url))
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
    public int getUnwatchedCount() {
        return item.getUnwatchedCount();
    }

    @Override
    public int getViewedProgress() {
        return item.getViewedProgress();
    }

    public long getViewedOffset() { return item.getViewedOffset(); }

    @Override
    public boolean useItemBackgroundArt() { return item.useItemBackgroundArt(); }

    @Override
    public boolean onClicked(Fragment fragment, Bundle extras, View transitionView) {
        return item.onClicked(fragment, extras, transitionView); }

    public boolean onPlayPressed(Fragment fragment, Bundle extras, View transitionView) {
        return item.onPlayPressed(fragment, extras, transitionView);
    }
}
