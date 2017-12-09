package com.monsterbutt.homeview.ui.presenters;

import android.app.Fragment;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;

import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.StatusWatcher;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.ui.interfaces.IRegisteredMedia;
import com.monsterbutt.homeview.ui.interfaces.IRegisteredRow;


public abstract class CardObject implements IRegisteredMedia {

    Context mContext;
    protected IRegisteredRow registeredRow;

    protected CardObject() {

    }
    protected CardObject(Context context) {
        mContext = context;
    }

    public abstract String getKey();
    public abstract String getTitle();
    public abstract String getSortTitle();
    public abstract String getContent();

    public void setRegisteredRow(IRegisteredRow row) { registeredRow = row; }

    public abstract Drawable getImage(Context context);

    public abstract int getHeight();
    public abstract int getWidth();

    public String getImageUrl(PlexServer server) {
        return "";
    }

    public String getBackgroundImageUrl() {
        return "";
    }

    public PlexLibraryItem.WatchedState getWatchedState() {
        return PlexLibraryItem.WatchedState.Watched;
    }

    public int getUnwatchedCount() {
        return 0;
    }

    public int getViewedProgress() {
        return 0;
    }

    public long getViewedOffset() { return 0; }

    public boolean useItemBackgroundArt() { return false; }

    public abstract boolean onClicked(Fragment fragment, Bundle extras, View transitionView);

    public boolean onPlayPressed(Fragment fragment, Bundle extras, View transitionView) {
        return onClicked(fragment, extras, transitionView);
    }

    @Override
    public boolean equals(Object obj) {

        if (obj instanceof CardObject) {

            CardObject r = (CardObject) obj;
            return getKey().equals(r.getKey());
        }
        return false;
    }

    public boolean setWatchState(PlexLibraryItem.WatchedState updateStatus, long viewedProgress) { return false; }

    public boolean onLongClicked(StatusWatcher statusWatcher,
                                 Fragment fragment, Bundle extras, View transitionView) {
        return onClicked(fragment, extras, transitionView);
    }

    public Context getContext() { return mContext; }
}
