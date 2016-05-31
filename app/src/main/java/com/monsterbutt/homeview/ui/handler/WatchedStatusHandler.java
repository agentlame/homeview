package com.monsterbutt.homeview.ui.handler;

import android.os.AsyncTask;
import android.text.TextUtils;

import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.PlexContainerItem;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.ui.UILifecycleManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import us.nineworlds.plex.rest.model.impl.Directory;
import us.nineworlds.plex.rest.model.impl.MediaContainer;
import us.nineworlds.plex.rest.model.impl.Video;

public class WatchedStatusHandler implements UILifecycleManager.LifecycleListener {

    public static final String key = "watchedstatushandler";

    public static class UpdateStatus {

        public final PlexLibraryItem.WatchedState state;
        public final long viewedOffset;
        public final String key;

        public UpdateStatus(String key, long viewedOffset, PlexLibraryItem.WatchedState state) {

            this.key = key;
            this.viewedOffset = viewedOffset;
            this.state = state;
        }

        @Override
        public boolean equals(Object r) {

            if (r instanceof UpdateStatus) {

                UpdateStatus ro = (UpdateStatus) r;
                return this.key.equals(ro.key) && this.viewedOffset == ro.viewedOffset && this.state == ro.state;
            }
            return false;
        }
    }
    public static class UpdateStatusList extends ArrayList<UpdateStatus> {}

    public interface WatchStatusListener {

        UpdateStatusList getItemsToCheck();
        void    updatedItemsCallback(UpdateStatusList items);
    }

    private final PlexServer mServer;
    private boolean mIsPaused = false;
    private Map<WatchStatusListener, ListenerHandler> mListeners = new HashMap<>();

    public WatchedStatusHandler(PlexServer server) {
        mServer = server;
    }

    public WatchedStatusHandler(PlexServer server, WatchStatusListener listener) {
        this(server);
        registerListener(listener);
    }

    public void registerListener(WatchStatusListener listener) {

        if (!mListeners.containsKey(listener))
            mListeners.put(listener, new ListenerHandler(listener));
    }

    public void unregisterListener(WatchStatusListener listener) {

        if (mListeners.containsKey(listener))
            mListeners.remove(listener);
    }

    @Override
    public void onPause() {

        synchronized (this) {
            mIsPaused = true;
        }
    }

    @Override
    public void onResume() {

        boolean wasPaused;
        synchronized (this) {
            wasPaused = mIsPaused;
            mIsPaused = false;
        }

        if (wasPaused)
            checkStatus();
    }

    @Override
    public void onDestroyed() {}

    public void checkStatus() {

        for (WatchStatusListener listener : mListeners.keySet()) {

            if (listener != null)
                checkStatus(listener);
        }
    }

    public void checkStatus(WatchStatusListener listener) {

        ListenerHandler handler = mListeners.containsKey(listener) ? mListeners.get(listener) : null;
        if (handler != null)
            handler.checkStatus();
    }

    private class ListenerHandler {

        private final WatchStatusListener mListener;

        public ListenerHandler(WatchStatusListener listener) {
            mListener = listener;
        }

        public void checkStatus() {

            if (mListener != null)
                new Task().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mListener.getItemsToCheck());
        }

        private class Task extends AsyncTask<UpdateStatusList, Void, UpdateStatusList> {

            @Override
            protected UpdateStatusList doInBackground(UpdateStatusList... params) {

                if (params != null && params.length > 0) {

                    String keys = "";
                    Map<String, UpdateStatus> map = new HashMap<>();
                    for (UpdateStatus item : params[0]) {

                        if (!TextUtils.isEmpty(item.key)) {
                            if (!TextUtils.isEmpty(keys))
                                keys += ",";
                            keys += item.key;
                            map.put(item.key, item);
                        }
                    }
                    MediaContainer mc = mServer.getRelatedForKey("/library/metadata/" + keys);

                    if (mc != null) {

                        UpdateStatusList list = new UpdateStatusList();

                        if (mc.getVideos() != null && !mc.getVideos().isEmpty()) {
                            for (Video vid : mc.getVideos()) {

                                UpdateStatus update = new UpdateStatus(Long.toString(vid.getRatingKey()),
                                        vid.getViewOffset(),
                                        PlexVideoItem.getWatchedState(vid));
                                if (map.containsKey(update.key) && !update.equals(map.get(update.key)))
                                    list.add(update);
                            }
                        }
                        if (mc.getDirectories() != null && !mc.getDirectories().isEmpty()) {
                            for (Directory dir : mc.getDirectories()) {

                                UpdateStatus update = new UpdateStatus(Long.toString(dir.getRatingKey()),
                                        Long.valueOf(dir.getViewedLeafCount()),
                                        PlexContainerItem.getWatchedState(dir));
                                if (map.containsKey(update.key) && !update.equals(map.get(update.key)))
                                    list.add(update);
                            }
                        }
                        return list;
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(UpdateStatusList result) {
                mListener.updatedItemsCallback(result);
            }
        }
    }

}
