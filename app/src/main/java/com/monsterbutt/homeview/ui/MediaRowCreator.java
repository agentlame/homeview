package com.monsterbutt.homeview.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v17.leanback.widget.ListRow;

import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.PlexContainerItem;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.presenters.CardPresenter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import us.nineworlds.plex.rest.model.impl.Directory;
import us.nineworlds.plex.rest.model.impl.Hub;
import us.nineworlds.plex.rest.model.impl.MediaContainer;
import us.nineworlds.plex.rest.model.impl.Video;


public class MediaRowCreator {

    public static class RowData implements Comparable<RowData> {

        public final String id;
        public final ListRow data;
        public int currentIndex;

        public RowData(String id, int index, ListRow data) {
            this.id = id;
            currentIndex = index;
            this.data = data;
        }

        @Override
        public int compareTo(@NonNull RowData row) {

            // reverse order
            if (this.currentIndex < row.currentIndex)
                return 1;
            return -1;
        }
    }


    public static class MediaRow {

        public final String title;
        public final String key;
        public final List<Directory> directories;
        public final List<Video> videos;

        public MediaRow(String title, String key, List<Directory> dirs, List<Video> vids) {

            this.title = title;
            this.key = key;
            directories = dirs;
            videos = vids;
        }
    }

    public static List<MediaRow> buildRowList(MediaContainer sections, MediaContainer hubs) {

        List<MediaRow> rows = new ArrayList<>();
        if (sections != null)
            rows.add(new MediaRow(sections.getTitle1(), "sections", sections.getDirectories(), sections.getVideos()));
        if (hubs != null) {
            for (Hub hub : hubs.getHubs()) {

                if ((hub.getVideos() == null || hub.getVideos().isEmpty())
                        && (hub.getDirectories() == null || hub.getDirectories().isEmpty()))
                    continue;
                rows.add(new MediaRow(hub.getTitle(), hub.getHubIdentifier(), hub.getDirectories(), hub.getVideos()));
            }
        }
        return rows;
    }

    public static  PlexItemRow fillAdapterForWatchedRow(Context context, PlexServer server, MediaRow row,
                                                        String title, int hash, boolean useLandscape,
                                                        CardPresenter.CardPresenterLongClickListener listener) {
        return MediaRowCreator.fillAdapterForRow(context, server, row, title, hash, useLandscape, true, listener);
    }

    public static  PlexItemRow fillAdapterForWatchedRow(Context context, PlexServer server,
                                                        MediaRow row, boolean useLandscape,
                                                        CardPresenter.CardPresenterLongClickListener listener) {
        return MediaRowCreator.fillAdapterForRow(context, server, row, row.title, row.title.hashCode(), useLandscape, true, listener);
    }

    public static  PlexItemRow fillAdapterForRow(Context context, PlexServer server, MediaRow row,
                                                 boolean useLandscape, CardPresenter.CardPresenterLongClickListener listener) {
        return MediaRowCreator.fillAdapterForRow(context, server, row, row.title, row.title.hashCode(), useLandscape, false, listener);
    }

    public static  PlexItemRow fillAdapterForRow(Context context, PlexServer server, MediaRow row,
                                                 String title, int hash, boolean useLandscape,
                                                 CardPresenter.CardPresenterLongClickListener listener) {
        return MediaRowCreator.fillAdapterForRow(context, server, row, title, hash, useLandscape, false, listener);
    }

    private static  PlexItemRow fillAdapterForRow(Context context, PlexServer server, MediaRow row,
                                                 String title, int hash, boolean useLandscape,
                                                 boolean watched, CardPresenter.CardPresenterLongClickListener listener) {

        PlexItemRow gridRow = watched ? PlexItemRow.getWatchedStateRow(server, title, hash, listener)
                                      : PlexItemRow.getRow(server, title, hash, listener);

        Iterator<Directory> itDirs = row.directories != null ? row.directories.iterator() : null;
        Directory currDir = null;
        if (itDirs != null)
            currDir = itDirs.next();

        Iterator<Video> itVideos = row.videos != null ? row.videos.iterator() : null;
        Video currVid = null;
        if (itVideos != null)
            currVid = itVideos.next();

        while (currDir != null || currVid != null) {

            boolean useDir = false;
            PlexLibraryItem item;
            if (currDir != null) {

                if (currVid != null) {
                    if (currDir.getUpdatedAt() > currVid.getTimeAdded())
                        useDir = true;
                }
                else
                    useDir = true;
            }

            if (useDir) {

                item = PlexContainerItem.getItem(currDir);
                currDir = itDirs.hasNext() ? itDirs.next() : null;
            }
            else {

                item = PlexVideoItem.getItem(currVid);
                currVid = itVideos.hasNext() ? itVideos.next() : null;
            }

            if (item != null)
                gridRow.addItem(context, item, useLandscape);
        }
        return gridRow;
    }
}
