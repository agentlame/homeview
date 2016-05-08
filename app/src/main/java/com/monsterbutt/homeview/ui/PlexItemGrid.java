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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlexItemGrid implements WatchedStatusHandler.WatchStatusListener {

    public enum ItemSort {

        DateAdded,
        Duration,
        LastViewed,
        Rating,
        ReleaseDate,
        Title
    }

    private class GridItem {

        PlexLibraryItem item;
        int index;
        final boolean useScene;

        public GridItem(PlexLibraryItem item, int index, boolean useScene) {
            this.item = item;
            this.index = index;
            this.useScene = useScene;
        }
    }

    Map<String, GridItem> map = new HashMap<>();
    ArrayObjectAdapter adapter = null;

    private WatchedStatusHandler watchedHandler = null;

    public static PlexItemGrid getGrid(PlexServer server, CardPresenter.CardPresenterLongClickListener listener) {

        return new PlexItemGrid(server, false, listener);
    }

    public static PlexItemGrid getWatchedStateGrid(PlexServer server, CardPresenter.CardPresenterLongClickListener listener) {

        return new PlexItemGrid(server, true, listener);
    }

    private PlexItemGrid(PlexServer server, boolean useWatchedState, CardPresenter.CardPresenterLongClickListener listener) {
        this.adapter = new ArrayObjectAdapter(new CardPresenter(server, listener));
        if (useWatchedState)
            watchedHandler = new WatchedStatusHandler(server, this);
    }

    public boolean addItem(Context context, PlexLibraryItem item, boolean useScene) {

        String key = Long.toString(item.getRatingKey());
        if (!map.containsKey(key)) {

            map.put(key, new GridItem(item, adapter.size(), useScene));
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

    public void sort(Context context, ItemSort sortType, boolean ascending) {

        List<GridItem> items = new ArrayList<>(map.values());
        Collections.sort(items, getComparator(sortType, ascending));

        adapter.clear();
        int index = 0;
        for (GridItem item : items) {

            item.index = index++;
            adapter.add(item.useScene ? new SceneCard(context, item.item) : new PosterCard(context, item.item));
        }
    }

    private Comparator<GridItem> getComparator(ItemSort sortType, boolean ascending) {

        switch (sortType) {

            case DateAdded:
                return ascending ? SortDateAddedAsc : SortDateAddedDesc;
            case Duration:
                return ascending ? SortDurationAsc : SortDurationDesc;
            case LastViewed:
                return ascending ? SortLastViewedAsc : SortLastViewedDesc;
            case Rating:
                return ascending ? SortRatingAsc : SortRatingDesc;
            case ReleaseDate:
                return ascending ? SortReleaseDateAsc : SortReleaseDateDesc;
            case Title:
                return ascending ? SortTitleAsc : SortTitleDesc;
            default:
                return null;
        }
    }

    private static Comparator<GridItem> SortDateAddedAsc = new Comparator<GridItem>() {
        @Override
        public int compare(GridItem lhs, GridItem rhs) {
            if (lhs.item.getAddedAt() > rhs.item.getAddedAt())
                return 1;
            return -1;
        }
    };

    private static Comparator<GridItem> SortDateAddedDesc = new Comparator<GridItem>() {
        @Override
        public int compare(GridItem lhs, GridItem rhs) {
            if (rhs.item.getAddedAt() > lhs.item.getAddedAt())
                return 1;
            return -1;
        }
    };

    private static Comparator<GridItem> SortDurationAsc = new Comparator<GridItem>() {
        @Override
        public int compare(GridItem lhs, GridItem rhs) {
            if (lhs.item.getDuration() > rhs.item.getDuration())
                return 1;
            return -1;
        }
    };

    private static Comparator<GridItem> SortDurationDesc = new Comparator<GridItem>() {
        @Override
        public int compare(GridItem lhs, GridItem rhs) {
            if (rhs.item.getDuration() > lhs.item.getDuration())
                return 1;
            return -1;
        }
    };

    private static Comparator<GridItem> SortLastViewedAsc = new Comparator<GridItem>() {
        @Override
        public int compare(GridItem lhs, GridItem rhs) {
            if (lhs.item.getLastViewedAt() > rhs.item.getLastViewedAt())
                return 1;
            return -1;
        }
    };

    private static Comparator<GridItem> SortLastViewedDesc = new Comparator<GridItem>() {
        @Override
        public int compare(GridItem lhs, GridItem rhs) {
            if (rhs.item.getLastViewedAt() > lhs.item.getLastViewedAt())
                return 1;
            return -1;
        }
    };

    private static Comparator<GridItem> SortRatingAsc = new Comparator<GridItem>() {
        @Override
        public int compare(GridItem lhs, GridItem rhs) {
            return lhs.item.getRating().compareTo(rhs.item.getRating());
        }
    };

    private static Comparator<GridItem> SortRatingDesc = new Comparator<GridItem>() {
        @Override
        public int compare(GridItem lhs, GridItem rhs) {
            return rhs.item.getRating().compareTo(lhs.item.getRating());
        }
    };

    private static Comparator<GridItem> SortReleaseDateAsc = new Comparator<GridItem>() {
        @Override
        public int compare(GridItem lhs, GridItem rhs) {
            return lhs.item.getOriginalAvailableDate().compareTo(rhs.item.getOriginalAvailableDate());
        }
    };

    private static Comparator<GridItem> SortReleaseDateDesc = new Comparator<GridItem>() {
        @Override
        public int compare(GridItem lhs, GridItem rhs) {
            return rhs.item.getOriginalAvailableDate().compareTo(lhs.item.getOriginalAvailableDate());
        }
    };

    private static Comparator<GridItem> SortTitleAsc = new Comparator<GridItem>() {
        @Override
        public int compare(GridItem lhs, GridItem rhs) {
            return lhs.item.getSortTitle().compareTo(rhs.item.getSortTitle());
        }
    };

    private static Comparator<GridItem> SortTitleDesc = new Comparator<GridItem>() {
        @Override
        public int compare(GridItem lhs, GridItem rhs) {
            return rhs.item.getSortTitle().compareTo(lhs.item.getSortTitle());
        }
    };
}
