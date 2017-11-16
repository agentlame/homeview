package com.monsterbutt.homeview.plex.media;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.view.View;

import com.monsterbutt.homeview.ui.upnp.UpnpItemsActivity;
import com.monsterbutt.homeview.ui.upnp.UpnpItemsFragment;

import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.container.Container;

import java.util.List;

@SuppressLint("ParcelCreator")
public class UpnpContainer extends PlexVideoItem {

    private final Container mContainer;

    public UpnpContainer(Container container) {
        super();
        mContainer = container;
    }

    @Override
    public String getKey() { return mContainer.getId(); }

    @Override
    public String getPlaybackTitle(Context context) {
        return getCardTitle(context);
    }

    @Override
    public String getPlaybackSubtitle(Context context, boolean includeMins) {
        return getCardContent(context);
    }

    @Override
    public String getPlaybackDescription(Context context) { return ""; }

    @Override
    public String getCardImageURL() {

        List<DIDLObject.Property> properties = mContainer.getProperties();
        if (properties != null && !properties.isEmpty()) {

            for (DIDLObject.Property property : properties) {

                if (property.getDescriptorName().equals("albumArtURI")) {

                    String img = property.getValue().toString();

                    int scale = img.indexOf("?scale=");
                    if (-1 < scale)
                        img = img.substring(0, scale) + "scale=480x480";
                    return img;
                }
            }
        }
        return  "";
    }

    @Override
    public String getPlaybackImageURL() {
        return getCardImageURL();
    }

    @Override
    public String getTitle() {
        return mContainer.getTitle();
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
    public String getWideCardTitle(Context context) {
        return getCardTitle(context);
    }

    @Override
    public String getWideCardContent(Context context) {
        return getPlaybackSubtitle(context, false);
    }

    @Override
    public String getSectionId() {
        return "0";
    }

    @Override
    public int getUnwatchedCount() {
        return mContainer.getChildCount() > 1 ? mContainer.getChildCount() : 0;
    }

    @Override
    public WatchedState getWatchedState() {

        return (mContainer.getChildCount() > 1) ? WatchedState.Unwatched : WatchedState.Watched;
    }

    @Override
    public int getViewedProgress() {
        return 0;
    }

    @Override
    public boolean onClicked(Fragment fragment, Bundle extras, View transitionView) {

        Intent intent = new Intent(fragment.getActivity(), UpnpItemsActivity.class);
        intent.putExtra(UpnpItemsFragment.PATH_ID, getKey());
        if (extras != null)
            intent.putExtras(extras);

        Bundle bundle = null;
        if (transitionView != null) {

            bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    fragment.getActivity(),
                    transitionView,
                    UpnpItemsFragment.SHARED_ELEMENT_NAME).toBundle();
        }
        fragment.startActivity(intent, bundle);
        return true;
    }

    public boolean onPlayPressed(Fragment fragment, Bundle extras, View transitionView) {
        return onClicked(fragment, extras, transitionView);
    }
}
