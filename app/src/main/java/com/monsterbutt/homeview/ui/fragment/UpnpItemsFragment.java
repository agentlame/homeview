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
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.media.UpnpContainer;
import com.monsterbutt.homeview.plex.media.UpnpItem;
import com.monsterbutt.homeview.presenters.CardObject;
import com.monsterbutt.homeview.presenters.UpnpCardPresenter;
import com.monsterbutt.homeview.presenters.PosterCard;
import com.monsterbutt.homeview.services.UpnpService;
import com.monsterbutt.homeview.ui.handler.CardSelectionHandler;

import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.support.contentdirectory.callback.Browse;
import org.fourthline.cling.support.model.BrowseFlag;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.item.Item;

import java.util.ArrayList;
import java.util.List;

public class UpnpItemsFragment extends VerticalGridFragment implements CardSelectionHandler.CardSelectionListener {

    public static final String SHARED_ELEMENT_NAME = "SHARED";
    public static final String DEVICE_ID = "deviceID";
    public static final String PATH_ID = "pathID";
    public static final String PATH_NAME = "pathName";

    private static final String ROOT_PATH_ID = "0";
    private static final String ROOT_PATH_NAME = "/";

    private ArrayObjectAdapter adapter;

    private AndroidUpnpService upnpService;

    private String mPathId = "";
    private String mDeviceId = "";
    private Device mDevice = null;
    private String mPathName = "";
    private CardSelectionHandler mSelectionHandler;

    private boolean transitioned = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            upnpService = (AndroidUpnpService) service;
            adapter.clear();

            for (Device device : upnpService.getRegistry().getDevices()) {

                if (device.getIdentity().getUdn().getIdentifierString().equals(mDeviceId)) {

                    mDevice = device;
                    break;
                }
            }
            if (mDevice != null && mDevice.getType().getType().equals("MediaServer")) {
                for (Service remoteservice : mDevice.getServices()) {
                    if (remoteservice.getServiceType().getType().equals("ContentDirectory")) {
                        upnpService.getControlPoint().execute(new BrowseCallback(remoteservice, mPathId, BrowseFlag.DIRECT_CHILDREN));
                        return;
                    }
                }
            }
            Toast.makeText(getActivity(), getActivity().getString(R.string.upnpbaddevice), Toast.LENGTH_LONG).show();
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
        Intent intent = act.getIntent();
        mDeviceId = intent.getStringExtra(DEVICE_ID);
        mPathId = intent.getStringExtra(PATH_ID);
        if (TextUtils.isEmpty(mPathId))
            mPathId = ROOT_PATH_ID;
        mPathName = intent.getStringExtra(PATH_NAME);
        if (TextUtils.isEmpty(mPathName))
            mPathName = ROOT_PATH_NAME;

        setTitle(mPathName);
        mSelectionHandler = new CardSelectionHandler(this, this);
        VerticalGridPresenter gridPresenter = new VerticalGridPresenter();
        String colCount = act.getString(R.string.gridview_poster_columns);

        gridPresenter.setNumberOfColumns(Integer.valueOf(colCount));
        setGridPresenter(gridPresenter);

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
    public Bundle getPlaySelectionBundle(boolean cardWasScene) {

        Bundle extras = new Bundle();
        extras.putString(PATH_NAME, String.format("%s/%s", !mPathName.equals(ROOT_PATH_NAME) ? mPathName : "",
                                                    mSelectionHandler.getSelection().getTitle()));
        extras.putString(DEVICE_ID, mDevice.getIdentity().getUdn().getIdentifierString());
        return extras;
    }

    private class BrowseCallback extends Browse {

        public BrowseCallback(Service service, String containerId, BrowseFlag flag) {
            super(service, containerId, flag);
        }

        @Override
        public void received(ActionInvocation actionInvocation, DIDLContent didl) {

            adapter.clear();
            List<CardObject> list = new ArrayList<>();
            Activity act = getActivity();
            List<Container> containers = didl.getContainers();
            if (containers != null && !containers.isEmpty()) {

                for(Container container : containers)
                    list.add(new PosterCard(act, new UpnpContainer(container)));
            }
            List<Item> items = didl.getItems();
            if (items != null && !items.isEmpty()) {

                for (Item item : items)
                    list.add(new PosterCard(act, new UpnpItem(item)));
            }
            adapter.addAll(0, list);

            synchronized (UpnpItemsFragment.this) {
                if (!transitioned) {
                    transitioned = true;
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            startEntranceTransition();
                        }
                    });
                }
            }
        }

        @Override
        public void updateStatus(Status status) {
        }

        @Override
        public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
            Toast.makeText(getActivity(), getActivity().getString(R.string.upnprequestfail), Toast.LENGTH_LONG).show();
        }
    }

}
