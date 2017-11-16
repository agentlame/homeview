package com.monsterbutt.homeview.plex.media;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.text.TextUtils;
import android.view.View;

import com.monsterbutt.homeview.ui.upnp.UpnpItemsActivity;
import com.monsterbutt.homeview.ui.upnp.UpnpItemsFragment;

import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.RemoteDeviceIdentity;

@SuppressLint("ParcelCreator")
public class UpnpServer extends PlexVideoItem {

    private final Device mDevice;

    public UpnpServer(Device device) {
        super();
        mDevice = device;
    }

    @Override
    public String getKey() { return mDevice.getIdentity().getUdn().toString(); }

    @Override
    public String getPlaybackTitle(Context context) {
        return getCardTitle(context);
    }

    @Override
    public String getPlaybackSubtitle(Context context, boolean includeMins) {
        return getCardContent(context);
    }

    @Override
    public String getPlaybackDescription(Context context) {
        return "";
    }

    @Override
    public String getCardImageURL() {

        String path = "";
        if (mDevice.getIcons() != null && mDevice.getIcons().length > 0) {

            String icon = mDevice.getIcons()[0].getUri().toString();
            path = String.format("%s%s%s", getServerURL(), !icon.startsWith("/") ? "/" : "", icon);
        }

        return  path;
    }

    @Override
    public String getPlaybackImageURL() {
        return getCardImageURL();
    }


    @Override
    public String getTitle() {
        return mDevice.getDetails().getFriendlyName();
    }

    @Override
    public String getCardTitle(Context context) {
        return getTitle();
    }

    @Override
    public String getCardContent(Context context) {
        return getHost();
    }

    private String getHost() {
        if (mDevice.getIdentity() instanceof RemoteDeviceIdentity) {

            RemoteDeviceIdentity rdi = (RemoteDeviceIdentity) mDevice.getIdentity();
            return rdi.getDescriptorURL().getHost();
        }
        return "";
    }

    private String getServerURL() {

        String host = getHost();
        if (!TextUtils.isEmpty(host)) {

            RemoteDeviceIdentity rdi = (RemoteDeviceIdentity) mDevice.getIdentity();
            host = rdi.getDescriptorURL().getProtocol() + "://" + host;
            int port = rdi.getDescriptorURL().getPort();
            if (port > 0)
                host += ":" + Integer.toString(port);
        }

        return host;
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
    public int getViewedProgress() {
        return 0;
    }

    @Override
    public boolean onClicked(Fragment fragment, Bundle extras, View transitionView) {

        Intent intent = new Intent(fragment.getActivity(), UpnpItemsActivity.class);
        intent.putExtra(UpnpItemsFragment.DEVICE_ID, mDevice.getIdentity().getUdn().getIdentifierString());
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
