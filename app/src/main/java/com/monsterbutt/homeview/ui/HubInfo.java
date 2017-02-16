package com.monsterbutt.homeview.ui;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v17.leanback.widget.ArrayObjectAdapter;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.presenters.CardPresenter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import us.nineworlds.plex.rest.model.impl.Hub;
import us.nineworlds.plex.rest.model.impl.MediaContainer;

public class HubInfo implements Parcelable {

    final public String name;
    final public String key;
    final public String path;

    public HubInfo(String name, String key, String path) {
        this.name = name;
        this.key = key;
        this.path = path;
    }

    protected HubInfo(Parcel in) {
        name = in.readString();
        key = in.readString();
        path = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(key);
        dest.writeString(path);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<HubInfo> CREATOR = new Creator<HubInfo>() {
        @Override
        public HubInfo createFromParcel(Parcel in) {
            return new HubInfo(in);
        }

        @Override
        public HubInfo[] newArray(int size) {
            return new HubInfo[size];
        }
    };

    public static void handleHubRows(Context context, PlexServer server,
                                    List<HubInfo> hubs, HashMap<String, Integer> landscape,
                                    Map<String, RowData> map, ArrayObjectAdapter rowsAdapter,
                                    UILifecycleManager lifeCycleMgr, PlexItemRow.RefreshAllCallback callback,
                                    CardPresenter.CardPresenterLongClickListener listener) {

        List<RowData> currentRows = new ArrayList<>();
        currentRows.addAll(map.values());
        Collections.sort(currentRows);
        // remove old rows that aren't there anymore
        for (RowData row : currentRows) {

            if (row.id.equals(PlexItemRow.SETTINGS_ROW_KEY) ||
                    row.id.equals(PlexItemRow.SECTIONS_ROW_KEY))
                continue;
            // we are reversing through the list
            boolean found = false;
            if (hubs != null) {
                for (HubInfo hub : hubs) {

                    if (hub.key.equals(row.id)) {

                        found = true;
                        break;
                    }
                }
            }

            if (!found) {
                map.remove(row.id);
                rowsAdapter.removeItems(row.currentIndex, 1);
            }
        }

        RowData settings = map.get(PlexItemRow.SETTINGS_ROW_KEY);
        if (hubs != null) {

            int addIndex = rowsAdapter.size() > 0 ? 1 : 0;
            for (HubInfo hub : hubs) {

                RowData row = map.get(hub.key);
                if (row == null) {
                    String header = hub.name;
                    int hash = header.hashCode();
                    for (String sub : context.getString(R.string.main_rows_header_strip).split(";"))
                        header = header.replace(sub, "").trim();
                    PlexItemRow item = PlexItemRow.buildItemsRow(context, server, header, hash,
                            callback, listener, landscape != null && landscape.containsKey(hub.key), hub);
                    row = new RowData(hub.key, addIndex, item);
                    map.put(hub.key, row);
                    lifeCycleMgr.put(hub.key, item);
                    rowsAdapter.add(addIndex, item);
                }
                else
                    row.currentIndex = addIndex;
                ((PlexItemRow)row.data).update();
                ++addIndex;
            }
        }
    }

    public static HubInfo getHub(Hub hub) {
        if (hub == null)
            return null;
        return new HubInfo(hub.getTitle(), hub.getHubIdentifier(), hub.getKey());
    }

    public static ArrayList<HubInfo> getHubs(MediaContainer mc) {
        return HubInfo.getHubs(mc.getHubs());
    }

    public static ArrayList<HubInfo> getHubs(List<Hub> hubs) {

        ArrayList<HubInfo> list = new ArrayList<>();
        if (hubs != null) {
            for (Hub hub : hubs)
                list.add(HubInfo.getHub(hub));
        }
        return list;
    }
}
