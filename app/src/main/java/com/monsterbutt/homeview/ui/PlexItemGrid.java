package com.monsterbutt.homeview.ui;

import android.content.Context;
import android.support.v17.leanback.widget.ArrayObjectAdapter;

import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.presenters.CardPresenter;
import com.monsterbutt.homeview.presenters.PosterCard;
import com.monsterbutt.homeview.presenters.SceneCard;
import com.monsterbutt.homeview.ui.handler.WatchedStatusHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlexItemGrid implements WatchedStatusHandler.WatchStatusListener {

    private class GridItem {

        PlexLibraryItem item;
        int index;

        public GridItem(PlexLibraryItem item, int index) {
            this.item = item;
            this.index = index;
        }
    }

    Map<String, GridItem> map = new HashMap<>();
    ArrayObjectAdapter adapter = null;

    private WatchedStatusHandler watchedHandler = null;

    public static PlexItemGrid getGrid(PlexServer server) {

        return new PlexItemGrid(server, false);
    }

    public static PlexItemGrid getWatchedStateGrid(PlexServer server) {

        return new PlexItemGrid(server, true);
    }

    private PlexItemGrid(PlexServer server, boolean useWatchedState) {
        this.adapter = new ArrayObjectAdapter(new CardPresenter(server));
        if (useWatchedState)
            watchedHandler = new WatchedStatusHandler(server, this);
    }

    public boolean addItem(Context context, PlexLibraryItem item, boolean useScene) {

        String key = Long.toString(item.getRatingKey());
        if (!map.containsKey(key)) {

            map.put(key, new GridItem(item, adapter.size()));
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

            GridItem gridItem = map.get(update.key);
            gridItem.item.setStatus(update);
            indexList.add(gridItem.index);
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

    public ArrayObjectAdapter getAdapter() { return adapter; }

    public void updateItem(WatchedStatusHandler.UpdateStatus update) {

        if (map.containsKey(update.key)) {

            GridItem item = map.get(update.key);
            item.item.setStatus(update);
            adapter.notifyArrayItemRangeChanged(item.index, 1);
        }
    }
}
