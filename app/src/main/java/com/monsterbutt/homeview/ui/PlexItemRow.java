package com.monsterbutt.homeview.ui;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.Presenter;

import com.monsterbutt.homeview.player.MediaTrackSelector;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.PlexContainerItem;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.presenters.CardObject;
import com.monsterbutt.homeview.presenters.CardPresenter;
import com.monsterbutt.homeview.presenters.CodecCard;
import com.monsterbutt.homeview.presenters.CodecPresenter;
import com.monsterbutt.homeview.presenters.PosterCard;
import com.monsterbutt.homeview.presenters.PosterCardExpanded;
import com.monsterbutt.homeview.presenters.SceneCard;
import com.monsterbutt.homeview.presenters.SceneCardExpanded;
import com.monsterbutt.homeview.presenters.SectionCard;
import com.monsterbutt.homeview.settings.SettingsManager;
import com.monsterbutt.homeview.ui.handler.CardSelectionHandler;
import com.monsterbutt.homeview.ui.handler.WatchedStatusHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import us.nineworlds.plex.rest.model.impl.Directory;
import us.nineworlds.plex.rest.model.impl.MediaContainer;
import us.nineworlds.plex.rest.model.impl.Video;

public class PlexItemRow extends ListRow implements WatchedStatusHandler.WatchStatusListener,
                                                    UILifecycleManager.LifecycleListener,
                                                    CardPresenter.LongClickWatchStatusCallback {

    public static final String SECTIONS_ROW_KEY = "sections";
    public static final String SETTINGS_ROW_KEY = "Settings";

    public interface RefreshAllCallback {
        void refresh();
    }

    private static class RowItem {

        PlexLibraryItem item;
        int index;

        RowItem(PlexLibraryItem item, int index) {
            this.item = item;
            this.index = index;
        }
    }

    private final PlexServer server;
    private final Context context;
    private final Map<String, RowItem> map = new HashMap<>();
    private final ArrayObjectAdapter adapter;
    private final RefreshAllCallback mRefreshCallback;
    private final boolean useScene;
    private final HubInfo hub;
    private WatchedStatusHandler watchedHandler = null;

    private PlexItemRow(Context context, PlexServer server, String header, int hash,
                        RefreshAllCallback callback, CardSelectionHandler listener,
                        boolean posterOnly, boolean useWatchedState, boolean useScene, HubInfo hub) {
        this(context, server, header, new CardPresenter(server, listener, posterOnly), callback, hash, useWatchedState, useScene, hub);
    }

    private PlexItemRow(Context context, PlexServer server, String header, Presenter presenter,
                        RefreshAllCallback callback, int hash, boolean useWatchedState,
                        boolean useScene, HubInfo hub) {
        super(hash, new HeaderItem(hash, header), new ArrayObjectAdapter(presenter));
        if (presenter instanceof CardPresenter)
            ((CardPresenter)presenter).setLongClickWatchStatusCallback(this);
        this.server = server;
        this.context = context;
        this.adapter = (ArrayObjectAdapter) getAdapter();
        this.mRefreshCallback = callback;
        this.useScene = useScene;
        this.hub = hub;
        if (useWatchedState)
            watchedHandler = new WatchedStatusHandler(server, this);
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

    public void update() {
        if (hub != null)
            new GetHubDataTask(server).execute(hub);
    }

    public void update(List<Directory> directories, List<Video> videos) {

        ArrayObjectAdapter  list = buildUpdateItem(directories, videos);
        for(int currIndex = adapter.size(); 0 != currIndex--; /**/) {

            CardObject item = (CardObject) adapter.get(currIndex);
            if (-1 == list.indexOf(item)) {

                map.remove(item.getRatingsKey());
                adapter.remove(item);
            }
        }

        for (int newIndex = 0; newIndex < list.size(); ++newIndex) {

            CardObject item = (CardObject) list.get(newIndex);
            int currIndex = adapter.indexOf(item);
            String key = item.getRatingsKey();
            RowItem ri = map.get(key);
            if (-1 == currIndex) {
                ri = new RowItem(((PosterCard)item).getItem(), newIndex);
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

    private ArrayObjectAdapter buildUpdateItem(List<Directory> directories, List<Video> videos) {

        ArrayObjectAdapter list = new ArrayObjectAdapter();
        Iterator<Directory> itDirs = directories != null ? directories.iterator() : null;
        Directory currDir = null;
        if (itDirs != null)
            currDir = itDirs.next();

        Iterator<Video> itVideos = videos != null ? videos.iterator() : null;
        Video currVid = null;
        if (itVideos != null)
            currVid = itVideos.next();

        while (currDir != null || currVid != null) {

            boolean useDir = false;
            if (currDir != null) {

                if (currVid != null) {
                    if (currDir.getUpdatedAt() > currVid.getTimeAdded())
                        useDir = true;
                }
                else
                    useDir = true;
            }

            PlexLibraryItem item;
            if (useDir) {

                item = PlexContainerItem.getItem(currDir);
                currDir = itDirs.hasNext() ? itDirs.next() : null;
            }
            else {

                item = PlexVideoItem.getItem(currVid);
                currVid = itVideos.hasNext() ? itVideos.next() : null;
            }

            if (item != null)
                list.add(useScene ? new SceneCardExpanded(context, item) : new PosterCardExpanded(context, item));
        }
        return list;
    }

    public static PlexItemRow buildCodecItemsRow(Context context, PlexServer server, String header,
                                                 MediaTrackSelector.StreamChoiceArrayAdapter choices,
                                                 int streamType) {

        PlexItemRow row = new PlexItemRow(context, server, header, new CodecPresenter(server), null,
                                            header.hashCode(), false, true, null);
        for (int i = 0; i < choices.getCount(); ++i)
            row.adapter.add(new CodecCard(context, choices.getItem(i), streamType));
        return row;
    }

    public static PlexItemRow buildChildItemsRow(Context context, PlexServer server, String header,
                                                 List<PlexLibraryItem> children,
                                                 boolean useWatchState, boolean useScene,
                                                 CardSelectionHandler listener) {

        boolean posterOnly = children != null && children.size() > 0 &&
         !(children.get(0) instanceof com.monsterbutt.homeview.plex.media.Chapter);
        PlexItemRow row = new PlexItemRow(context, server, header, header.hashCode(),
                                            null, listener, posterOnly,  useWatchState, useScene, null);
        if (children != null) {
            for (PlexLibraryItem item : children) {
                long ratingKey = item.getRatingKey();
                String key = ratingKey > 0 ? Long.toString(ratingKey) : item.getKey();
                if (!row.map.containsKey(key)) {

                    row.map.put(key, new RowItem(item, row.adapter.size()));
                    row.adapter.add(row.useScene ? new SceneCard(context, item) : new PosterCard(context, item));
                }
            }
        }
        return row;
    }

    static PlexItemRow buildItemsRow(Context context, PlexServer server, String header,
                                     int hash, RefreshAllCallback callback,
                                     CardSelectionHandler listener,
                                     boolean useScene, HubInfo hub) {
        return new PlexItemRow(context, server, header, hash, callback, listener, true, true, useScene, hub);
    }

    public static PlexItemRow buildSectionRow(Context context, MediaContainer sections, PlexServer server,
                                              String header, RefreshAllCallback callback,
                                              CardSelectionHandler listener) {

        if (sections == null)
            return null;
        PlexItemRow row = new PlexItemRow(context, server, header, header.hashCode(),
                                            callback, listener, false, false, true, null);
        for (Directory dir : sections.getDirectories()) {

            PlexContainerItem item = PlexContainerItem.getItem(dir);
            if (item != null) {
                long ratingKey = item.getRatingKey();
                String key = ratingKey > 0 ? Long.toString(ratingKey) : item.getKey();
                if (!row.map.containsKey(key)) {

                    row.map.put(key, new RowItem(item, row.adapter.size()));
                    row.adapter.add(new SectionCard(context, item));
                }
            }
        }
        return row;
    }

    private class GetHubDataTask extends AsyncTask<HubInfo, Void, MediaContainer> {

        private final PlexServer server;
        private final String maxCount;

        GetHubDataTask(PlexServer server) {
            this.server = server;
            this.maxCount = SettingsManager.getInstance(context).getString("preferences_navigation_hubsizelimit");
        }

        private final String recentlyAddedKey = "recentlyAdded?type=";
        private final String limitResultsKey = "&X-Plex-Container-Start=0&X-Plex-Container-Size=";
        @Override
        protected MediaContainer doInBackground(HubInfo[] params) {

            if (params != null && params.length > 0) {

                HubInfo hub = params[0];
                if (hub.path.contains(recentlyAddedKey) && Long.valueOf(maxCount) > 0)
                    hub = new HubInfo(hub.name, hub.key, hub.path + limitResultsKey + maxCount);
                return server.getHubsData(hub);
            }
            return null;
        }

        @Override
        protected void onPostExecute(MediaContainer item) {

            List<Directory> directories = null;
            List<Video> videos = null;
            if (item != null) {
                directories = item.getDirectories();
                videos = item.getVideos();
            }
            update(directories, videos);
        }
    }
}
