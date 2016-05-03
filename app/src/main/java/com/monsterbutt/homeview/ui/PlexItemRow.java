package com.monsterbutt.homeview.ui;

import android.content.Context;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;

import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.presenters.CardObject;
import com.monsterbutt.homeview.presenters.CardPresenter;
import com.monsterbutt.homeview.presenters.PosterCard;
import com.monsterbutt.homeview.presenters.SceneCard;
import com.monsterbutt.homeview.ui.handler.WatchedStatusHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlexItemRow extends ListRow implements WatchedStatusHandler.WatchStatusListener {

    private class RowItem {

        PlexLibraryItem item;
        int index;

        public RowItem(PlexLibraryItem item, int index) {
            this.item = item;
            this.index = index;
        }
    }

    Map<String, RowItem> map = new HashMap<>();
    ArrayObjectAdapter adapter = null;

    private WatchedStatusHandler watchedHandler = null;

    public static PlexItemRow getRow(PlexServer server, String header) {

        return new PlexItemRow(server, header, new ArrayObjectAdapter(new CardPresenter(server)), false);
    }

    public static PlexItemRow getWatchedStateRow(PlexServer server, String header) {

        return new PlexItemRow(server, header, new ArrayObjectAdapter(new CardPresenter(server)), true);
    }

    public static PlexItemRow getRow(PlexServer server, String header, int hash) {

        return new PlexItemRow(server, header, new ArrayObjectAdapter(new CardPresenter(server)), hash, false);
    }

    public static PlexItemRow getWatchedStateRow(PlexServer server, String header, int hash) {

        return new PlexItemRow(server, header, new ArrayObjectAdapter(new CardPresenter(server)), hash, true);
    }

    private PlexItemRow(PlexServer server, String header, ArrayObjectAdapter adapter, boolean useWatchedState) {
        this(server, header, adapter, 0, useWatchedState);
    }

    private PlexItemRow(PlexServer server, String header, ArrayObjectAdapter adapter, int hash, boolean useWatchedState) {
        super(new HeaderItem(hash, header),adapter);
        this.adapter = adapter;
        if (useWatchedState)
            watchedHandler = new WatchedStatusHandler(server, this);
    }

    public boolean addItem(Context context, PlexLibraryItem item, boolean useScene) {

        long ratingKey = item.getRatingKey();
        String key = ratingKey > 0 ? Long.toString(ratingKey) : item.getKey();
        if (!map.containsKey(key)) {

            map.put(key, new RowItem(item, adapter.size()));
            adapter.add(useScene ? new SceneCard(context, item) : new PosterCard(context, item));
            return true;
        }
        return false;
    }

    public void resume() {

        if (watchedHandler != null)
            watchedHandler.resume();
    }

    public void pause() {

        if (watchedHandler != null)
            watchedHandler.pause();
    }

    public WatchedStatusHandler.UpdateStatusList getItemsToCheck() {

        WatchedStatusHandler.UpdateStatusList list = null;

        if (!map.isEmpty()) {

            list = new WatchedStatusHandler.UpdateStatusList();
            for (int i = 0; i < adapter.size(); ++i) {

                PosterCard card = (PosterCard) adapter.get(i);
                list.add(card.getUpdateStatus());
            }
        }

        return list;
    }

    public void updatedItemsCallback(WatchedStatusHandler.UpdateStatusList items) {

        List<Integer> indexList = new ArrayList<>();
        for(WatchedStatusHandler.UpdateStatus update : items) {

            if (!map.containsKey(update.key))
                continue;

            RowItem rowItem = map.get(update.key);
            rowItem.item.setStatus(update);
            indexList.add(rowItem.index);
        }

        if (!indexList.isEmpty()) {

            Collections.sort(indexList);
            int startIndex =-2;
            int indexCount = 0;
            for (Integer index : indexList) {

                if (startIndex + indexCount != index) {

                    if (indexCount > 0)
                        adapter.notifyArrayItemRangeChanged(startIndex, indexCount);
                    startIndex = index;
                    indexCount = 1;
                }
                else
                    ++indexCount;
            }

            adapter.notifyArrayItemRangeChanged(startIndex, indexCount);
        }
    }

    public void updateRow(PlexItemRow row) {

        for(int currIndex = adapter.size(); 0 != currIndex--; /**/) {

            CardObject item = (CardObject) adapter.get(currIndex);
            if (-1 == row.adapter.indexOf(item)) {

                map.remove(item.getRatingsKey());
                adapter.remove(item);
            }
        }

        for (int newIndex = 0; newIndex < row.adapter.size(); ++newIndex) {

            CardObject item = (CardObject) row.adapter.get(newIndex);
            int currIndex = adapter.indexOf(item);
            String key = item.getRatingsKey();
            RowItem ri = row.map.get(key);
            if (-1 == currIndex) {

                ri.index = newIndex;
                adapter.add(newIndex, item);
            }
            else {
                ri.index = currIndex;
                adapter.replace(currIndex, item);
            }
            map.put(key, ri);
        }
    }
}
