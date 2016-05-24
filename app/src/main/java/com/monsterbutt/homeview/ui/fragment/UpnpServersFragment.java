package com.monsterbutt.homeview.ui.fragment;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v17.leanback.app.VerticalGridFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.VerticalGridPresenter;
import android.view.View;
import android.widget.TextView;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.media.UpnpServer;
import com.monsterbutt.homeview.presenters.UpnpCardPresenter;
import com.monsterbutt.homeview.presenters.SceneCard;
import com.monsterbutt.homeview.services.UpnpService;
import com.monsterbutt.homeview.ui.handler.CardSelectionHandler;

import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.registry.DefaultRegistryListener;
import org.fourthline.cling.registry.Registry;

public class UpnpServersFragment extends VerticalGridFragment  {

    private ArrayObjectAdapter adapter;

    private BrowseRegistryListener registryListener = new BrowseRegistryListener();

    private AndroidUpnpService upnpService;

    private ServiceConnection serviceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            upnpService = (AndroidUpnpService) service;
            adapter.clear();
            upnpService.getRegistry().addListener(registryListener);
            for (Device device : upnpService.getRegistry().getDevices())
                registryListener.deviceAdded(device);
            startEntranceTransition();
            upnpService.getControlPoint().search();
        }

        public void onServiceDisconnected(ComponentName className) {
            upnpService = null;
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        if (null == savedInstanceState)
            prepareEntranceTransition();

        Activity act = getActivity();
        VerticalGridPresenter gridPresenter = new VerticalGridPresenter();
        String colCount = act.getString(R.string.gridview_scene_columns);

        setTitle(act.getString(R.string.mediaservertitle));
        gridPresenter.setNumberOfColumns(Integer.valueOf(colCount));
        setGridPresenter(gridPresenter);

        new CardSelectionHandler(this);
        adapter = new ArrayObjectAdapter(new UpnpCardPresenter());
        setAdapter(adapter);

        // This will start the UPnP service if it wasn't already started
        getActivity().getApplicationContext().bindService(
                new Intent(getActivity(), UpnpService.class),
                serviceConnection,
                Context.BIND_AUTO_CREATE
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        TextView text = (TextView) getActivity().findViewById(android.support.v17.leanback.R.id.title_text);
        text.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (upnpService != null)
            upnpService.getRegistry().removeListener(registryListener);
        getActivity().getApplicationContext().unbindService(serviceConnection);
    }

    protected class BrowseRegistryListener extends DefaultRegistryListener {

        @Override
        public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) {
            deviceAdded(device);
        }

        @Override
        public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
            deviceAdded(device);
        }

        @Override
        public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
            deviceRemoved(device);
        }

        @Override
        public void localDeviceAdded(Registry registry, LocalDevice device) {
            deviceAdded(device);
        }

        @Override
        public void localDeviceRemoved(Registry registry, LocalDevice device) {
            deviceRemoved(device);
        }

        public void deviceAdded(final Device device) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {

                    SceneCard card = new SceneCard(getActivity(), new UpnpServer(device));
                    int position = adapter.indexOf(card);
                    if (position >= 0)
                        adapter.replace(position, card);
                     else
                        adapter.add(card);
                }
            });
        }

        public void deviceRemoved(final Device device) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    adapter.remove(new SceneCard(getActivity(), new UpnpServer(device)));
                }
            });
        }
    }
}
