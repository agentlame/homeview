package com.monsterbutt.homeview.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.media.tv.BasePreviewProgram;
import android.support.media.tv.Channel;
import android.support.media.tv.PreviewProgram;
import android.support.media.tv.TvContractCompat;
import android.support.media.tv.WatchNextProgram;
import android.text.TextUtils;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.monsterbutt.homeview.TvUtil;
import com.monsterbutt.homeview.model.MockDatabase;
import com.monsterbutt.homeview.model.Subscription;
import com.monsterbutt.homeview.model.Video;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.RecommendationBuilder;
import com.monsterbutt.homeview.Utils;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.PlexServerManager;
import com.monsterbutt.homeview.plex.media.Episode;
import com.monsterbutt.homeview.plex.media.Movie;
import com.monsterbutt.homeview.plex.media.PlexContainerItem;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.plex.media.Season;
import com.monsterbutt.homeview.plex.media.Show;
import com.monsterbutt.homeview.provider.BackgroundContentProvider;
import com.monsterbutt.homeview.settings.SettingsManager;
import com.monsterbutt.homeview.ui.HubInfo;
import com.monsterbutt.homeview.ui.grid.GridActivity;
import com.monsterbutt.homeview.ui.details.DetailsActivity;
import com.monsterbutt.homeview.ui.playback.PlaybackActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import us.nineworlds.plex.rest.model.impl.Directory;
import us.nineworlds.plex.rest.model.impl.Hub;
import us.nineworlds.plex.rest.model.impl.MediaContainer;

import static android.support.media.tv.TvContractCompat.PreviewProgramColumns.ASPECT_RATIO_2_3;


public class UpdateRecommendationsService extends IntentService {

    private static final String TAG = "HV_Recommendations";

    private static final String homeContinue = "home.continue";
    private static final String homeOnDeck = "home.ondeck";
    private static final String homeShows = "home.television.recent";
    private static final String homeMovies = "home.movies.recent";

    public UpdateRecommendationsService() {
        super(TAG);
    }

    private class GroupedObject implements Comparable<GroupedObject>{

        final String group;
        final PlexVideoItem object;
        final String hubIdentifier;
        private final int index;

        GroupedObject(String group, PlexVideoItem object, int index, String hubIdentifier) {
            this.group = group;
            this.object = object;
            this.index = index;
            this.hubIdentifier = hubIdentifier;
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

        PlexServer server = PlexServerManager.getInstance().getSelectedServer();
        if (server == null)
            return;

        MediaContainer container = server.getHubs();
        if (container == null || container.getHubs() == null)
            return;

        long excludeKey = server.getCurrentPlayingVideoRatingKey();

        Context context = getApplicationContext();
        NotificationHandler notifications = new NotificationHandler(context, server, excludeKey);
        ChannelHandler channels = new ChannelHandler(context, server);
        for (Hub hub : container.getHubs()) {
            if (!channels.addToChannels(hub))
                notifications.addToNotifications(hub);
        }

        if (!channels.processChannels())
            notifications.processNotifications();
    }

    private class NotificationHandler {

        private static final String fileName = "notifications";

        private final Map<String, GroupedObject> shows = new HashMap<>();
        private final Map<String, GroupedObject> movies = new HashMap<>();

        private final Map<String, Boolean> checkedAlready = new HashMap<>();
        private int sortOrder = 0;

        private final PlexServer server;
        private final long excludeKey;
        private final Context context;

        private final int MAX_RECOMMENDATIONS;
        private final int BASE_PRIORITY;
        private final int priority_span;

        NotificationHandler(Context context, PlexServer server, long excludeKey) {
            this.context = context;
            this.server = server;
            this.excludeKey = excludeKey;

            MAX_RECOMMENDATIONS = Integer.valueOf(getString(R.string.recommendations_max));
            BASE_PRIORITY = Integer.valueOf(getString(R.string.recommendations_priority_base));
            priority_span = Integer.valueOf(getString(R.string.recommendations_priority_span));
        }

        void addToNotifications(Hub hub) {

            String group = hub.getTitle();
            if (hub.getVideos() == null)
                return;
            for (us.nineworlds.plex.rest.model.impl.Video video : hub.getVideos()) {

                PlexVideoItem item = PlexVideoItem.getItem(video);
                if (item == null || item.getWatchedState() == PlexLibraryItem.WatchedState.Watched
                 || checkedAlready.containsKey(item.getKey()) || excludeKey == item.getRatingKey())
                    continue;

                GroupedObject go = new GroupedObject(group, item, sortOrder, hub.getHubIdentifier());
                if (item instanceof Episode) {

                    Episode ep = (Episode) item;
                    if (shows.containsKey(ep.getShowKey())) {

                        final Episode epPrev = (Episode) shows.get(ep.getShowKey()).object;
                        if ((Integer.valueOf(epPrev.getSeasonNum()) > Integer.valueOf(ep.getSeasonNum()))
                         || (epPrev.getSeasonNum().equals(ep.getSeasonNum()) &&
                         Integer.valueOf(epPrev.getEpisodeNum()) > Integer.valueOf(ep.getEpisodeNum())))
                            shows.put(ep.getShowKey(), go);
                    } else
                        shows.put(ep.getShowKey(), go);
                } else
                    movies.put(item.getKey(), go);

                ++sortOrder;
                checkedAlready.put(item.getKey(), true);
            }
        }

        void processNotifications() {

            List<GroupedObject> recommends = new ArrayList<>();
            recommends.addAll(shows.values());
            recommends.addAll(movies.values());
            Collections.sort(recommends);
            if (!recommends.isEmpty())
                recommends = recommends.subList(0, Math.min(MAX_RECOMMENDATIONS, recommends.size()));

            Map<String, Integer> newNotifications = new HashMap<>();
            NotificationManager mgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (mgr == null)
                return;
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
                     .setIntent(item.buildPendingIntent(UpdateRecommendationsService.this))
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
    }

    private class ChannelHandler {

        private final List<Video> watchNowChannel;
        private final List<Video> showsChannel;
        private final List<Video> moviesChannel;

        private final Context context;
        private final PlexServer server;

        private final boolean useChannels = android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;

        private int sortOrder = 0;

        ChannelHandler(Context context, PlexServer server) {

            this.context = context;
            this.server = server;
            boolean useMoviesChannel = TvUtil.doesSubscriptionExist(context, MockDatabase.getRecentMoviesSubscription(context));
            boolean useShowsChannel = TvUtil.doesSubscriptionExist(context, MockDatabase.getRecentShowSubscription(context));

            if (useChannels) {
                watchNowChannel = new ArrayList<>();
                showsChannel = useShowsChannel ? new ArrayList<Video>() : null;
                moviesChannel = useMoviesChannel ? new ArrayList<Video>() : null;
            }
            else {
                watchNowChannel = null;
                showsChannel = null;
                moviesChannel = null;
            }
        }

        boolean addToChannels(Hub hub) {

            if (!useChannels)
                return false;
            String hubIdentifier = hub.getHubIdentifier();
            List<us.nineworlds.plex.rest.model.impl.Video> vids = hub.getVideos();
            List<Directory> dirs = hub.getDirectories();
            if (hubIdentifier.equals(homeShows) || hubIdentifier.equals(homeMovies)) {

                String limitResultsKey = "&X-Plex-Container-Start=0&X-Plex-Container-Size=";
                String maxCount = SettingsManager.getInstance().getString("preferences_navigation_hubsizelimit");
                MediaContainer mc = server.getHubsData(new HubInfo(hub.getTitle(), hub.getKey(), hub.getKey() + limitResultsKey + maxCount));
                vids = mc.getVideos();
                dirs = mc.getDirectories();
            }

            Iterator<Directory> itDirs = dirs != null ? dirs.iterator() : null;
            Directory currDir = null;
            if (itDirs != null)
                currDir = itDirs.next();

            Iterator<us.nineworlds.plex.rest.model.impl.Video> itVideos = vids != null ? vids.iterator() : null;
            us.nineworlds.plex.rest.model.impl.Video currVid = null;
            if (itVideos != null)
                currVid = itVideos.next();

            while (currDir != null || currVid != null) {

                boolean useDir = false;
                if (currDir != null) {

                    if (currVid != null) {
                        if (currDir.getUpdatedAt() > currVid.getTimeAdded())
                            useDir = true;
                    } else
                        useDir = true;
                }

                PlexLibraryItem item;
                if (useDir) {

                    item = PlexContainerItem.getItem(currDir);
                    currDir = itDirs.hasNext() ? itDirs.next() : null;
                } else {

                    item = PlexVideoItem.getItem(currVid);
                    List<PlexLibraryItem> extras = item != null ? item.getExtraItems() : null;
                    if (item != null && (extras == null || extras.isEmpty())) {
                        MediaContainer mc = server.getVideoMetadata(item.getKey());
                        if (mc != null) {
                            PlexVideoItem newItem = PlexVideoItem.getItem(mc.getVideos().get(0));
                            if (newItem != null && newItem.getKey().equals(item.getKey()))
                                item = newItem;
                        }
                    }
                    currVid = itVideos.hasNext() ? itVideos.next() : null;
                }

                if (item != null)
                    addItem(item.toVideo(context, server), hubIdentifier);
            }
            return true;
        }

        private void addItem(Video item, String hubIdentifier) {

            item.sortOrder = ++sortOrder;
            switch(hubIdentifier) {
                case homeContinue:
                case homeOnDeck:
                    if (watchNowChannel != null)
                        watchNowChannel.add(item);
                    break;
                case homeMovies:
                    if (moviesChannel != null)
                        moviesChannel.add(item);
                    break;
                case homeShows:
                    if (showsChannel != null)
                        showsChannel.add(item);
                    break;
            }
        }

        boolean processChannels() {

            if (!useChannels)
                return false;

            if (showsChannel != null) {
                Subscription sub = MockDatabase.findSubscriptionByName(context, context.getString(R.string.channel_shows));
                if (sub != null)
                    syncPrograms(sub.getChannelId(), context, server, showsChannel, false);
            }
            if (moviesChannel != null) {
                Subscription sub = MockDatabase.findSubscriptionByName(context, context.getString(R.string.channel_movies));
                if (sub != null)
                    syncPrograms(sub.getChannelId(), context, server, moviesChannel, false);
            }
            if (watchNowChannel != null)
                syncPrograms(TvUtil.WATCH_NOW_CHANNEL_ID, context, server, watchNowChannel, true);
            List<Video> recommends = new ArrayList<>();

            if (watchNowChannel != null) {
                recommends.addAll(watchNowChannel);
            }
            if (showsChannel != null) {
                recommends.addAll(showsChannel);
            }
            if (moviesChannel != null) {
                recommends.addAll(moviesChannel);
            }
            Collections.sort(recommends);
            Subscription sub = MockDatabase.findSubscriptionByName(context, context.getString(R.string.channel_recommended));
            if (sub != null)
                syncPrograms(sub.getChannelId(), context, server, recommends, true);

            return true;
        }

        private Channel getChannel(long channelId) {
            try (Cursor cursor =
                  getContentResolver()
                   .query(
                    TvContractCompat.buildChannelUri(channelId),
                    null,
                    null,
                    null,
                    null)) {
                if (cursor != null && cursor.moveToNext()) {
                    return Channel.fromCursor(cursor);
                }
            }
            return null;
        }

        private void syncPrograms(long channelId, Context context, PlexServer server,
                                     List<Video> updateList, boolean excludeWatched) {
            Log.d(TAG, "Sync programs for channel: " + channelId);

            List<Video> cachedMovies;
            if (!excludeWatched)
                cachedMovies = updateList;
            else {
                cachedMovies = new ArrayList<>();
                for (Video vid : updateList) {
                    if (vid.getWatchedState() != PlexLibraryItem.WatchedState.Watched)
                        cachedMovies.add(vid);
                }
            }

            List<Video> originalMovies = new ArrayList<>(MockDatabase.getMovies(context, channelId));
            if (channelId == TvUtil.WATCH_NOW_CHANNEL_ID) {
                updateChannel(channelId, cachedMovies, originalMovies, server);
            } else {
                Channel channel = getChannel(channelId);
                if (channel != null) {
                    if (!channel.isBrowsable()) {
                        Log.d(TAG, "Channel is not browsable: " + channelId);
                        deletePrograms(channelId, originalMovies);
                    } else {
                        Log.d(TAG, "Channel is browsable: " + channelId);
                        updateChannel(channelId, cachedMovies, originalMovies, server);
                    }
                }
            }
        }

        private void updateChannel(long channelId, List<Video> cachedMovies, List<Video> originalMovies, PlexServer server) {
            List<Video> toDelete = new ArrayList<>();
            for (Video orig : originalMovies) {
                if (!cachedMovies.contains(orig))
                    toDelete.add(orig);
            }
            for (Video del : toDelete)
                deleteProgram(channelId, del);

            for (Video vid : cachedMovies) {
                int index = originalMovies.indexOf(vid);
                if (0 <= index) {
                    vid.programId = originalMovies.get(index).programId;
                    updateProgram(channelId, vid, server);
                } else
                    createProgram(channelId, vid, server);
            }
            MockDatabase.saveMovies(getApplicationContext(), channelId, cachedMovies);
        }

        private void createProgram(long channelId, Video movie, PlexServer server) {

            Uri programUri;
            if (channelId == TvUtil.WATCH_NOW_CHANNEL_ID) {
                programUri =
                 getContentResolver()
                  .insert(
                   TvContractCompat.WatchNextPrograms.CONTENT_URI,
                   buildWatchNextProgram(movie, server).toContentValues());
            } else {
                programUri =
                 getContentResolver()
                  .insert(
                   TvContractCompat.PreviewPrograms.CONTENT_URI,
                   buildProgram(channelId, movie, server).toContentValues());
            }
            long programId = ContentUris.parseId(programUri);
            Log.d(TAG, "Inserted new program: " + programId);
            movie.programId = programId;
        }

        private void deleteProgram(long channelId, Video movie) {
            getContentResolver()
             .delete(channelId == TvUtil.WATCH_NOW_CHANNEL_ID ?
               TvContractCompat.buildWatchNextProgramUri(movie.programId)
               : TvContractCompat.buildPreviewProgramUri(movie.programId),
              null,
              null);
        }

        private void updateProgram(long channelId, Video movie, PlexServer server) {

            getContentResolver()
             .update(channelId == TvUtil.WATCH_NOW_CHANNEL_ID ?
               TvContractCompat.buildWatchNextProgramUri(movie.programId)
               : TvContractCompat.buildPreviewProgramUri(movie.programId),
              buildProgram(channelId, movie, server).toContentValues(),
              null,
              null);
            Log.d(TAG, "Updated program: " + movie.programId);
        }

        private void deletePrograms(long channelId, List<Video> movies) {
            if (movies.isEmpty()) {
                return;
            }

            int count = 0;
            for (Video movie : movies) {
                count +=
                 getContentResolver()
                  .delete(channelId == TvUtil.WATCH_NOW_CHANNEL_ID ?
                    TvContractCompat.buildWatchNextProgramUri(movie.programId) :
                    TvContractCompat.buildPreviewProgramUri(movie.programId),
                   null,
                   null);
            }
            Log.d(TAG, "Deleted " + count + " programs for  channel " + channelId);

            // Remove our local records to stay in sync with the TV Provider.
            MockDatabase.removeMovies(getApplicationContext(), channelId);
        }

        void setupBuilder(BasePreviewProgram.Builder builder, Video movie, PlexServer server) {

            @TvContractCompat.PreviewProgramColumns.Type int type;
            switch (movie.category) {

                case Movie.TYPE:
                    type = TvContractCompat.PreviewProgramColumns.TYPE_MOVIE;
                    break;

                case Episode.TYPE:
                    type = TvContractCompat.PreviewProgramColumns.TYPE_TV_EPISODE;
                    break;

                case Season.TYPE:
                    type = TvContractCompat.PreviewProgramColumns.TYPE_TV_SEASON;
                    break;

                case Show.TYPE:
                    type = TvContractCompat.PreviewProgramColumns.TYPE_TV_SERIES;
                    break;

                default:
                    type = TvContractCompat.PreviewProgramColumns.TYPE_CLIP;
                    break;
            }
            List<String> genres = new ArrayList<>();
            if (movie.genre != null && movie.genre.length > 0) {
                for (String g : movie.genre) {
                    if (TvContractCompat.Programs.Genres.isCanonical(g.toUpperCase()))
                        genres.add(g.toUpperCase());
                }
            }

            //Uri appLinkUri = AppLinkHelper.buildPlaybackUri(channelId, movie.getId());

            String trailer = !TextUtils.isEmpty(movie.trailerUrl) ? PlexVideoItem.getVideoPath(server, movie.trailerUrl)
             : (!TextUtils.isEmpty(movie.videoUrl) ? PlexVideoItem.getVideoPath(server, movie.trailerUrl) : "");
            if (!TextUtils.isEmpty(trailer)) {
                try {
                    HttpURLConnection con = (HttpURLConnection) (new URL(trailer).openConnection());
                    con.setInstanceFollowRedirects(false);
                    con.connect();
                    if (302 == con.getResponseCode())
                        trailer = con.getHeaderField("Location");
                    con.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            String appLinkUri;
            switch (type) {

                case TvContractCompat.PreviewProgramColumns.TYPE_TV_EPISODE:
                case TvContractCompat.PreviewProgramColumns.TYPE_MOVIE:
                case TvContractCompat.PreviewProgramColumns.TYPE_CLIP:

                    appLinkUri = PlaybackActivity.URI + movie.getKey();
                    break;

                case TvContractCompat.PreviewProgramColumns.TYPE_TV_SERIES:

                    appLinkUri = DetailsActivity.URI + movie.getKey();
                    break;

                default:

                    appLinkUri = GridActivity.URI + movie.getKey();
                    break;
            }
            builder
             .setType(type)
             .setIntentUri(Uri.parse(appLinkUri))
             .setPreviewVideoUri(Uri.parse(trailer))
             .setLastPlaybackPositionMillis(movie.getViewedProgress())
             .setDurationMillis((int) movie.duration)
             .setTitle(movie.getTitle())
             .setDescription(movie.description)
             .setPosterArtUri(Uri.parse(server.makeServerURL(movie.cardImageUrl)))
             .setThumbnailUri(Uri.parse(server.makeServerURL(movie.thumbNail)))
             .setVideoHeight(movie.height)
             .setVideoWidth(movie.width)
             .setCanonicalGenres(genres.toArray(new String[genres.size()]));
            switch (type) {
                case TvContractCompat.PreviewProgramColumns.TYPE_TV_EPISODE:
                    builder
                     .setEpisodeNumber(movie.episodeNum)
                     .setEpisodeTitle(movie.getTitle())
                     .setSeasonNumber(movie.seasonNum)
                     .setTitle(movie.showTitle)
                     .setPosterArtUri(Uri.parse(server.makeServerURL(movie.thumbNail)));
                    break;

                case TvContractCompat.PreviewProgramColumns.TYPE_MOVIE:
                    builder
                     .setPosterArtUri(Uri.parse(server.makeServerURL(movie.thumbNail)));
                    break;

                case TvContractCompat.PreviewProgramColumns.TYPE_TV_SEASON:
                    builder
                     .setSeasonNumber(movie.seasonNum)
                     .setTitle(movie.showTitle);
                    builder.setThumbnailAspectRatio(ASPECT_RATIO_2_3);
                    break;
                case TvContractCompat.PreviewProgramColumns.TYPE_TV_SERIES:
                    builder.setThumbnailAspectRatio(ASPECT_RATIO_2_3);
                    break;
            }
        }

        @NonNull
        WatchNextProgram buildWatchNextProgram(Video movie, PlexServer server) {

            @TvContractCompat.WatchNextPrograms.WatchNextType int type = TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_NEW;
            if (movie.category.equals(Show.TYPE) || movie.category.equals(Season.TYPE))
                type = TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_WATCHLIST;
            else if (movie.watchedOffset > 0)
                type = TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE;
            WatchNextProgram.Builder builder = new WatchNextProgram.Builder();
            builder.setWatchNextType(type);
            setupBuilder(builder, movie, server);
            return builder.build();
        }

        @NonNull
        private PreviewProgram buildProgram(long channelId, Video movie, PlexServer server) {

            PreviewProgram.Builder builder = new PreviewProgram.Builder();
            builder.setChannelId(channelId);
            setupBuilder(builder, movie, server);

            return builder.build();
        }
    }
}
