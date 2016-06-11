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

public class PlexItemRow extends ListRow implements WatchedStatusHandler.WatchStatusListener,
                                                    UILifecycleManager.LifecycleListener,
                                                    CardPresenter.LongClickWatchStatusCallback {

    public interface RefreshAllCallback {
        void refresh();
    }

    private class RowItem {

        PlexLibraryItem item;
        int index;

        public RowItem(PlexLibraryItem item, int index) {
            this.item = item;
            this.index = index;
        }
    }

    final Map<String, RowItem> map = new HashMap<>();
    final ArrayObjectAdapter adapter;
    final RefreshAllCallback mRefreshCallback;

    private WatchedStatusHandler watchedHandler = null;

    public static PlexItemRow getRow(PlexServer server, String header, RefreshAllCallback callback,
                                     CardPresenter.CardPresenterLongClickListener listener) {
        return new PlexItemRow(server, header, callback, listener, false);
    }

    public static PlexItemRow getWatchedStateRow(PlexServer server, String header, RefreshAllCallback callback,
                                                 CardPresenter.CardPresenterLongClickListener listener) {
        return new PlexItemRow(server, header, callback, listener, true);
    }

    public static PlexItemRow getRow(PlexServer server, String header, int hash,
                                     RefreshAllCallback callback, CardPresenter.CardPresenterLongClickListener listener) {
        return new PlexItemRow(server, header, new CardPresenter(server, listener), callback, hash, false);
    }

    public static PlexItemRow getWatchedStateRow(PlexServer server, String header, int hash, RefreshAllCallback callback,
                                                 CardPresenter.CardPresenterLongClickListener listener) {
        return new PlexItemRow(server, header, new CardPresenter(server, listener), callback, hash, true);
    }

    private PlexItemRow(PlexServer server, String header, RefreshAllCallback callback,
                        CardPresenter.CardPresenterLongClickListener listener, boolean useWatchedState) {
        this(server, header, new CardPresenter(server, listener), callback, 0, useWatchedState);
    }

    private PlexItemRow(PlexServer server, String header, CardPresenter presenter,
                        RefreshAllCallback callback, int hash, boolean useWatchedState) {
        super(new HeaderItem(hash, header), new ArrayObjectAdapter(presenter));
        presenter.setLongClickWatchStatusCallback(this);
        this.adapter = (ArrayObjectAdapter) getAdapter();
        this.mRefreshCallback = callback;
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

    @Override
    public void onResume() {

        if (watchedHandler != null)
            watchedHandler.onResume();
    }

    @Override
    public void onPause() {

        if (watchedHandler != null)
            watchedHandler.onPause();
    }

    @Override
    public void onDestroyed() {

        if (watchedHandler != null)
            watchedHandler.onDestroyed();
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
        if (items != null) {
            for (WatchedStatusHandler.UpdateStatus update : items) {

                if (!map.containsKey(update.key))
                    continue;

                RowItem rowItem = map.get(update.key);
                if (rowItem != null) {

                    Object obj = adapter.get(rowItem.index);
                    if (obj != null && obj instanceof PosterCard && ((PosterCard) obj).getRatingsKey().equals(update.key)) {
                        ((PosterCard) obj).setUpdateStatus(update);
                        rowItem.item.setStatus(update);
                        indexList.add(rowItem.index);
                    }
                }
            }
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
                CardObject oldItem = (CardObject) adapter.get(currIndex);
                ri.index = currIndex;
                oldItem.setUpdateStatus(item.getUpdateStatus());
            }
            map.put(key, ri);
        }
    }

    @Override
    public void resetSelected(CardObject obj) {

        if (obj != null) {

            int index = adapter.indexOf(obj);
            if (index != -1)
                adapter.notifyArrayItemRangeChanged(index, 1);
        }

        if (mRefreshCallback != null)
            mRefreshCallback.refresh();
    }
}
