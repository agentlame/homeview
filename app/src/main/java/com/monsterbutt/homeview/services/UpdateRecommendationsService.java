package com.monsterbutt.homeview.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.monsterbutt.homeview.ui.activity.PlaybackActivity;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.RecommendationBuilder;
import com.monsterbutt.homeview.Utils;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.PlexServerManager;
import com.monsterbutt.homeview.plex.media.Episode;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.provider.BackgroundContentProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import us.nineworlds.plex.rest.model.impl.Hub;
import us.nineworlds.plex.rest.model.impl.MediaContainer;
import us.nineworlds.plex.rest.model.impl.Video;


public class UpdateRecommendationsService extends IntentService {

    private static final String TAG = "RecommendationsService";

    public UpdateRecommendationsService() {
        super(TAG);
    }

    private class GroupedObject implements Comparable<GroupedObject>{

        public final String group;
        public final PlexVideoItem object;
        private final int index;
        public GroupedObject(String group, PlexVideoItem object, int index) {
            this.group = group;
            this.object = object;
            this.index = index;
        }

        @Override
        public int compareTo(@NonNull GroupedObject r) {

            if (this.index < r.index)
                return -1;
            else
                return 1;
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        Context context = getApplicationContext();
        PlexServer server = PlexServerManager.getInstance(context, null).getSelectedServer();
        if (server == null)
            return;

        MediaContainer container = server.getHubs();
        if (container == null || container.getHubs() == null)
            return;

        long excludeKey = server.getCurrentPlayingVideoRatingKey();

        int MAX_RECOMMENDATIONS = Integer.valueOf(getString(R.string.recommendations_max));
        int BASE_PRIORITY = Integer.valueOf(getString(R.string.recommendations_priority_base));
        int priority_span = Integer.valueOf(getString(R.string.recommendations_priority_span));

        Map<String, GroupedObject> shows = new HashMap<>();
        Map<String, GroupedObject> movies = new HashMap<>();

        Map<String, Boolean> checkedAlready = new HashMap<>();
        int sortOrder = 0;
        for (Hub hub : container.getHubs()) {

            String group = hub.getTitle();
            if (hub.getVideos() == null)
                continue;
            for (Video video : hub.getVideos()) {

                PlexVideoItem item = PlexVideoItem.getItem(video);
                if (item == null || item.getWatchedState() == PlexLibraryItem.WatchedState.Watched
                 || checkedAlready.containsKey(item.getKey()) || excludeKey == item.getRatingKey())
                    continue;

                GroupedObject go = new GroupedObject(group, item, sortOrder);
                if (item instanceof Episode) {

                    Episode ep = (Episode) item;
                    if (shows.containsKey(ep.getShowKey())) {

                        final Episode epPrev = (Episode) shows.get(ep.getShowKey()).object;
                        if ((Integer.valueOf(epPrev.getSeasonNum()) > Integer.valueOf(ep.getSeasonNum()))
                        ||  (epPrev.getSeasonNum().equals(ep.getSeasonNum()) &&
                                Integer.valueOf(epPrev.getEpisodeNum()) > Integer.valueOf(ep.getEpisodeNum())))
                            shows.put(ep.getShowKey(), go);
                    }
                    else
                        shows.put(ep.getShowKey(), go);
                }
                else
                    movies.put(item.getKey(), go);

                ++sortOrder;
                checkedAlready.put(item.getKey(), true);
            }
        }

        List<GroupedObject> recommends = new ArrayList<>();
        for (GroupedObject go : shows.values())
            recommends.add(go);
        for (GroupedObject go : movies.values())
            recommends.add(go);
        Collections.sort(recommends);
        if (!recommends.isEmpty())
            recommends = recommends.subList(0, Math.min(MAX_RECOMMENDATIONS, recommends.size()));

        Map<String, Integer> newNotifications = new HashMap<>();
        NotificationManager mgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        try {

            RecommendationBuilder builder = new RecommendationBuilder()
                    .setContext(getApplicationContext())
                    .setSmallIcon(R.drawable.small_icon);

            int priority = BASE_PRIORITY;
            for (GroupedObject go : recommends) {

                Resources res = context.getResources();
                int posterWidth = Utils.convertDpToPixel(context, res.getDimensionPixelSize(R.dimen.DETAIL_POSTER_WIDTH));
                int posterHeight = Utils.convertDpToPixel(context, res.getDimensionPixelSize(R.dimen.DETAIL_POSTER_HEIGHT));
                PlexVideoItem item = go.object;
                Bitmap poster = Glide.with(context)
                        .load(server.makeServerURL(item.getPlaybackImageURL())).asBitmap()
                        .into(posterWidth, posterHeight)
                        .get();
                builder.setGroup(go.group);
                Notification notification = builder
                    .setBackground(BackgroundContentProvider.CONTENT_URI + item.getBackgroundImageURL())
                    .setLargeIcon(poster)
                    .setPriority(priority)
                    .setProgress(item.getViewedProgress())
                    .setTitle(item.getCardTitle(context))
                    .setDescription(item.getCardContent(context))
                    .setGroup(go.group)
                    .setIntent(buildPendingIntent(item))
                    .build();
                mgr.notify(item.getKey(), (int) item.getRatingKey(), notification);
                newNotifications.put(item.getKey(), (int) item.getRatingKey());
                priority -= priority_span;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Map<String, Integer> oldIds = getNotifications();
        for (Map.Entry pair : oldIds.entrySet()) {
            String key = (String) pair.getKey();
            if (!newNotifications.containsKey(key))
                mgr.cancel(key, (Integer) pair.getValue());
        }
        setNotifications(newNotifications);
    }

    private static final String fileName = "notifications";
    @SuppressWarnings("unchecked")
    private Map<String, Integer> getNotifications() {

        Map<String,Integer> ret = new HashMap<>();
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new FileInputStream(new File(getCacheDir(), fileName)));
            ret = (HashMap) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return ret;
    }

    private void setNotifications(Map<String,Integer> ids) {

        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(new FileOutputStream(new File(getCacheDir(), fileName)));
            oos.writeObject(ids);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private PendingIntent buildPendingIntent(PlexVideoItem video) {

        Intent playbackIntent = new Intent(this, PlaybackActivity.class);
        playbackIntent.putExtra(PlaybackActivity.KEY, video.getKey());
        playbackIntent.putExtra(PlaybackActivity.VIDEO, video);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(PlaybackActivity.class);
        stackBuilder.addNextIntent(playbackIntent);
        // Ensure a unique PendingIntents, otherwise all
        // recommendations end up with the same PendingIntent
        playbackIntent.setAction(video.getKey());
        return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
