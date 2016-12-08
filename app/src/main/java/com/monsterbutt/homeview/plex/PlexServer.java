package com.monsterbutt.homeview.plex;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.monsterbutt.homeview.plex.media.Movie;
import com.monsterbutt.homeview.plex.media.PlexContainerItem;
import com.monsterbutt.homeview.plex.media.Season;
import com.monsterbutt.homeview.plex.media.Show;
import com.monsterbutt.homeview.ui.HubInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import us.nineworlds.plex.rest.PlexappFactory;
import us.nineworlds.plex.rest.config.impl.Configuration;
import us.nineworlds.plex.rest.model.impl.Directory;
import us.nineworlds.plex.rest.model.impl.MediaContainer;


public class PlexServer {

    private static final String PREFS                   = "PSM";
    private static final String PREFS_LAST_SERVER_NAME  = "LastServerName";
    private static final String PREFS_LAST_SERVER_HOST  = "LastServerHost";
    private static final String PREFS_LAST_SERVER_PORT  = "LastServerPort";
    private static final String PREFS_DEVICE_ID         = "DeviceId";
    public static final  String DEFAULT_SERVER_PORT     = "32400";
    public static final String BASE_DEVICE_ID           = "-HomeView-Android";

    public enum SearchType {SEARCH_EPISODE,
                            SEARCH_SERIES,
                            SEARCH_MOVIES}

    private  String mServerName;
    private Configuration mConfiguration = new Configuration();
    private PlexappFactory mFactory = null;
    private boolean mIsPIPActive = false;

    public PlexServer(Context context) {

        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        mServerName = prefs.getString(PREFS_LAST_SERVER_NAME, "");
        mConfiguration.setHost(prefs.getString(PREFS_LAST_SERVER_HOST, ""));
        mConfiguration.setPort(prefs.getString(PREFS_LAST_SERVER_PORT, DEFAULT_SERVER_PORT));
        if (isValid()) {
            mFactory = new PlexappFactory(mConfiguration, getDeviceId(context));
        }
    }

    private String getDeviceId(Context context) {

        String ret = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                            .getString(PREFS_DEVICE_ID, "");
        if (TextUtils.isEmpty(ret)) {

            ret = UUID.randomUUID().toString() + BASE_DEVICE_ID;
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString(PREFS_DEVICE_ID, ret)
                    .commit();
        }
        return ret;
    }

    public PlexServer(String server, String ip) {

        ip = ip.replace("http://", "").replace("https://", "").replace("/", "").replace("//", "");
        mServerName = server.trim();

        String[] tokens = ip.split(":");
        if (tokens.length <= 1) {

            mConfiguration.setHost(ip);
            mConfiguration.setPort(DEFAULT_SERVER_PORT);
        }
        else {

            mConfiguration.setHost(tokens[0]);
            mConfiguration.setPort(tokens[1]);
        }
    }

    public String getServerName() { return mServerName; }
    public String getServerAddress() { return String.format("%s:%s", mConfiguration.getHost(), mConfiguration.getPort()); }

    public void saveAsLastServer(Context context) {

        synchronized (this) {

            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString(PREFS_LAST_SERVER_NAME, mServerName)
                    .putString(PREFS_LAST_SERVER_HOST, mConfiguration.getHost())
                    .putString(PREFS_LAST_SERVER_PORT, mConfiguration.getPort())
                    .commit();
        }
    }

    public boolean isValid() { return !mServerName.isEmpty() && !mConfiguration.getHost().isEmpty(); }

    public boolean verifyInstance(Context context) {

        boolean ret = false;
        synchronized (this) {

            if (mFactory == null)
                mFactory = new PlexappFactory(mConfiguration, getDeviceId(context));
        }

        try {

            MediaContainer container = mFactory.retrieveRootData();
            if (container != null) {

                if (!container.getServerName().isEmpty())
                    mServerName = container.getServerName();
                ret = true;
            }
        }
        catch (Exception e) {

            Log.e(getClass().getName(), e.toString());
        }

        return ret;
    }

    public MediaContainer getHubs() {

        MediaContainer ret = null;
        if (mFactory != null) {

            try {
                ret = mFactory.retrieveHubs();
            } catch (Exception e) {

                Log.e(getClass().getName(), e.toString());
            }
        }
        return ret;
    }

    public MediaContainer getHubsData(HubInfo hub) {

        MediaContainer ret = null;
        if (mFactory != null && hub != null) {

            try {
                if (!TextUtils.isEmpty(hub.key))
                    ret = mFactory.retrieveVideoMetaData(hub.path);

            } catch (Exception e) {

                Log.e(getClass().getName(), e.toString());
            }
        }
        return ret;
    }

    public MediaContainer getLibrary() {

        MediaContainer ret = null;
        if (mFactory != null) {

            try {
                ret = mFactory.retrieveLibrary();
            } catch (Exception e) {

                Log.e(getClass().getName(), e.toString());
            }
        }
        return ret;
    }

    public MediaContainer getLibraryDir(String key) {

        MediaContainer ret = null;
        if (mFactory != null) {

            try {
                ret = mFactory.retrieveLibrary(key);
            } catch (Exception e) {

                Log.e(getClass().getName(), e.toString());
            }
        }
        return ret;
    }

    public String getServerURL() {

        if ( mFactory != null)
            return mFactory.baseURL();
        return "";
    }

    public String makeServerURL(String url) {

        String ret = "";
        if (url != null && !url.isEmpty() && mFactory != null) {
            ret = mFactory.baseURL();
            if (url.startsWith("/") && ret.endsWith("/"))
                ret += url.substring(1);
            else
                ret += url;
        }
        return ret;
    }

    public String makeServerURLForCodec(String resourceType, String resourceName) {

        String ret = "";
        if (resourceType != null && !resourceType.isEmpty() &&
                resourceName != null && !resourceName.isEmpty() && mFactory != null) {
            ret = mFactory.getMediaTagURL(resourceType, resourceName);
        }
        return ret;
    }

    public MediaContainer getSectionArts(String sectionKey) {

        MediaContainer ret = null;
        if (mFactory != null) {

            try {
                ret = mFactory.retrieveSections(sectionKey, "arts");
            }
            catch (Exception e) {
                Log.e(getClass().getName(), e.toString());
            }
        }
        return ret;
    }

    public MediaContainer getSection(String sectionKey) {

        MediaContainer ret = null;
        if (mFactory != null) {

            try {
                ret = mFactory.retrieveSections(sectionKey);
            }
            catch (Exception e) {
                Log.e(getClass().getName(), e.toString());
            }
        }
        return ret;
    }

    public MediaContainer getSectionFilter(String sectionKey, String filter) {

        MediaContainer ret = null;
        if (mFactory != null) {

            try {
                ret = mFactory.retrieveSections(sectionKey, filter);
            }
            catch (Exception e) {
                Log.e(getClass().getName(), e.toString());
            }
        }
        return ret;
    }

    public MediaContainer getRelatedForKey(String key) {

        MediaContainer ret = null;
        if (mFactory != null) {

            try {
                ret = mFactory.retrieveVideoMetaData(key);
            }
            catch (Exception e) {
                Log.e(getClass().getName(), e.toString());
            }
        }
        return ret;
    }

    public MediaContainer getHubForSection(String sectionId) {

        MediaContainer ret = null;
        if (mFactory != null) {

            try {
                ret = mFactory.retrieveHubForSection(sectionId);
            }
            catch (Exception e) {
                Log.e(getClass().getName(), e.toString());
            }
        }
        return ret;
    }

    public MediaContainer getVideoMetadata(String key) {

        MediaContainer ret = null;
        if (mFactory != null) {

            try {
                ret = mFactory.retrieveMovieMetaData(key);
            }
            catch (Exception e) {
                Log.e(getClass().getName(), e.toString());
            }
        }
        return ret;
    }

    public MediaContainer searchForMedia(String section, String filter, SearchType type, String query) {

        MediaContainer ret = null;
        if (mFactory != null) {

            try {
                switch (type) {

                    case SEARCH_EPISODE:
                        ret = mFactory.searchEpisodes(section, query);
                        break;
                    case SEARCH_MOVIES:
                        ret = mFactory.searchMovies(section, query);
                        break;
                    case SEARCH_SERIES:
                        ret = mFactory.searchTVShows(section, query);
                    default:
                        break;
                }
            }
            catch (Exception e) {
                Log.e(getClass().getName(), e.toString());
            }
        }
        return ret;
    }


    public List<MediaContainer> searchForMedia(Directory section, String filter, String query) {

        List<MediaContainer> ret = new ArrayList<>();
        if (mFactory != null) {

            try {
                String key = section.getKey();
                String type = section.getType();

                if (type.equals(Show.TYPE)) {

                    MediaContainer result = searchForMedia(key, filter, SearchType.SEARCH_SERIES, query);
                    if (result != null && result.getDirectories() != null && !result.getDirectories().isEmpty())
                        ret.add(result);

                    result = searchForMedia(key, filter, SearchType.SEARCH_EPISODE, query);
                    if (result != null && result.getVideos() != null && !result.getVideos().isEmpty())
                        ret.add(result);
                }
                else if (type.equals(Movie.TYPE)) {

                    MediaContainer result = searchForMedia(key, filter, SearchType.SEARCH_MOVIES, query);
                    if (result != null && result.getVideos() != null && !result.getVideos().isEmpty())
                        ret.add(result);
                }
            }
            catch (Exception e) {
                Log.e(getClass().getName(), e.toString());
            }
        }

        return ret;
    }


    public List<MediaContainer> searchShowsForEpisodes(String filter, String query) {

        List<MediaContainer> ret = new ArrayList<>();
        if (mFactory != null) {

            try {

                MediaContainer sections = mFactory.retrieveSections();
                if (sections != null && sections.getDirectories() != null) {

                    for (Directory section : sections.getDirectories()) {

                        if (!section.getType().equals(Show.TYPE))
                            continue;

                        MediaContainer shows = searchForMedia(section.getKey(), filter, SearchType.SEARCH_SERIES, query);
                        if (shows != null && shows.getDirectories() != null) {

                            for (Directory show : shows.getDirectories()) {

                                String showKey = show.getKey().replace(PlexContainerItem.CHILDREN, Season.ALL_SEASONS);
                                MediaContainer showResult = mFactory.retrieveMovieMetaData(showKey);
                                if (showResult != null && showResult.getVideos() != null) {

                                    showResult.setGrandparentKey(show.getKey());
                                    ret.add(showResult);
                                }
                            }
                        }
                    }
                }
            }
            catch (Exception e) {
                Log.e(getClass().getName(), e.toString());
            }
        }

        return ret;
    }

    public List<MediaContainer> searchForMedia(String filter, String query) {

        List<MediaContainer> ret = new ArrayList<>();
        if (mFactory != null) {

            try {

                MediaContainer sections = mFactory.retrieveSections();
                if (sections != null && sections.getDirectories() != null) {

                    for (Directory dir : sections.getDirectories()) {

                        List<MediaContainer> results = searchForMedia(dir, filter, query);
                        if (!results.isEmpty())
                            ret.addAll(results);
                    }
                }
            }
            catch (Exception e) {
                Log.e(getClass().getName(), e.toString());
            }
        }

        return ret;
    }

    public List<MediaContainer> searchForMedia(String query) {
        return searchForMedia(PlexContainerItem.ALL, query);
    }

    public List<MediaContainer> searchShowsForEpisodes(String query) {
        return searchShowsForEpisodes(PlexContainerItem.ALL, query);
    }

    public boolean setUnwatched(String key, String ratingKey) {

        if (mFactory != null) {

            Log.d("PlexServer", "Setting UnWatched for : " + ratingKey);
            try {
                return mFactory.setUnWatched(key, ratingKey);
            } catch (Exception e) {
                Log.e(getClass().getName(), e.toString());
            }
        }
        return false;
    }

    public boolean setWatched(String key, String ratingKey) {

        if (mFactory != null) {
            Log.d("PlexServer", "Setting Watched for : " + ratingKey);
            try {
                return mFactory.setWatched(key, ratingKey);
            } catch (Exception e) {
                Log.e(getClass().getName(), e.toString());
            }
        }
        return false;
    }

    public boolean toggleWatchedState(String key, String ratingKey, boolean isWatched) {

        if (isWatched)
            return setUnwatched(key, ratingKey);
        return setWatched(key, ratingKey);
    }

    public boolean setProgress(String key, String ratingKey, long progress) {

        if (mFactory != null) {

            try {
                return mFactory.setProgress(key, ratingKey, Long.toString(progress));
            } catch (Exception e) {
                Log.e(getClass().getName(), e.toString());
            }
        }
        return false;
    }


    public String getThemeURL(MediaContainer container) {

        String theme = container.getThemeKey();
        if (TextUtils.isEmpty(theme))
            theme = container.getGrandparentTheme();
        if (!TextUtils.isEmpty(theme))
            theme = makeServerURL(theme);
        return theme;
    }

    public boolean isPIPActive() {
        return mIsPIPActive;
    }

    public void isPIPActive(boolean active) {
        mIsPIPActive = active;
    }
}
