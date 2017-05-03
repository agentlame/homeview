package com.monsterbutt.homeview.plex;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.JsonReader;
import android.util.Log;

import com.monsterbutt.homeview.plex.media.Movie;
import com.monsterbutt.homeview.plex.media.PlexContainerItem;
import com.monsterbutt.homeview.plex.media.Season;
import com.monsterbutt.homeview.plex.media.Show;
import com.monsterbutt.homeview.services.UpdateRecommendationsService;
import com.monsterbutt.homeview.ui.HubInfo;

import java.io.BufferedOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

import us.nineworlds.plex.rest.PlexappFactory;
import us.nineworlds.plex.rest.config.impl.Configuration;
import us.nineworlds.plex.rest.model.impl.Directory;
import us.nineworlds.plex.rest.model.impl.MediaContainer;


public class PlexServer {

    private static final String PREFS                   = "PSM";
    private static final String PREFS_LAST_SERVER_NAME  = "LastServerName";
    private static final String PREFS_LAST_SERVER_HOST  = "LastServerHost";
    private static final String PREFS_LAST_SERVER_PORT  = "LastServerPort";
    private static final String PREFS_TOKENS            = "ServerTokens";
    private static final String PREFS_DEVICE_ID         = "DeviceId";
    public static final  String DEFAULT_SERVER_PORT     = "32400";
    public static final String BASE_DEVICE_ID           = "-HomeView-Android";
    public static final long INVALID_RATING_KEY         = 0;

    public enum SearchType {SEARCH_EPISODE,
                            SEARCH_SERIES,
                            SEARCH_MOVIES}

    private  String mServerName;
    private Configuration mConfiguration = new Configuration();
    private PlexappFactory mFactory = null;
    private boolean mIsPIPActive = false;
    private long mCurrentPlayingRatingKey = INVALID_RATING_KEY;
    private HashMap<String, String> mTokens = new HashMap<>();
    private String mServerKey;

    public PlexServer(Context context) {

        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        readTokens(prefs);

        mServerName = prefs.getString(PREFS_LAST_SERVER_NAME, "");
        mConfiguration.setHost(prefs.getString(PREFS_LAST_SERVER_HOST, ""));
        mConfiguration.setPort(prefs.getString(PREFS_LAST_SERVER_PORT, DEFAULT_SERVER_PORT));
        mConfiguration.setDeviceId(getDeviceId(context));
        mServerKey = makeServerKey(mServerName, mConfiguration.getHost(), mConfiguration.getPort());
        mConfiguration.setServerToken(mTokens.get(mServerKey));
        String clientVersion;
        try {
            clientVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception e) {
            clientVersion = "1.0.0";
        }
        mConfiguration.setAppVersion(clientVersion);
        if (isValid()) {
            mFactory = new PlexappFactory(mConfiguration);
        }
    }

    private String makeServerKey(String name, String host, String port) {
        return name + ":" + host + ":" + port;
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

    public PlexServer(String server, String ip, Context context) {

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
        mConfiguration.setDeviceId(getDeviceId(context));
        mServerKey = makeServerKey(mServerName, mConfiguration.getHost(), mConfiguration.getPort());
        String clientVersion;
        try {
            clientVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception e) {
            clientVersion = "1.0.0";
        }
        mConfiguration.setAppVersion(clientVersion);
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        readTokens(prefs);
    }

    private void readTokens(SharedPreferences prefs) {

        String tokensAll = prefs.getString(PREFS_TOKENS, "");
        for (String item : tokensAll.split(";")) {
            String[] splits = item.split("\\|");
            if (splits.length > 1)
                mTokens.put(splits[0], splits[1]);
        }
    }

    public String getServerName() { return mServerName; }
    public String getServerAddress() { return String.format("%s:%s", mConfiguration.getHost(), mConfiguration.getPort()); }

    public void saveAsLastServer(Context context) {

        synchronized (this) {

            String tokens = "";
            Set<String> keys = mTokens.keySet();
            for (String key : keys) {
                if (!tokens.isEmpty())
                    tokens += ";";
                tokens += key + "|" + mTokens.get(key);
            }

            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString(PREFS_LAST_SERVER_NAME, mServerName)
                    .putString(PREFS_LAST_SERVER_HOST, mConfiguration.getHost())
                    .putString(PREFS_LAST_SERVER_PORT, mConfiguration.getPort())
                    .putString(PREFS_TOKENS, tokens)
                    .commit();
        }
    }

    public boolean isValid() { return !mServerName.isEmpty() && !mConfiguration.getHost().isEmpty()
        && hasServerToken(); }

    public boolean verifyInstance(Context context) {

        boolean ret = false;
        synchronized (this) {

            if (mFactory == null)
                mFactory = new PlexappFactory(mConfiguration);
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
            String token = mFactory.getToken();
            if (ret.contains("?") && !ret.contains(token))
                ret = ret.replace("?", "?" + token + "&");
            else
                ret += "?" + token;
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

    private void updateNotifications(Context context) {

        if (context != null) {
            Intent intent = new Intent(context.getApplicationContext(), UpdateRecommendationsService.class);
            context.startService(intent);
        }
    }

    public boolean setUnwatched(String key, String ratingKey, Context context) {

        if (mFactory != null) {

            Log.d("PlexServer", "Setting UnWatched for : " + ratingKey);
            try {
                return mFactory.setUnWatched(key, ratingKey);
            } catch (Exception e) {
                Log.e(getClass().getName(), e.toString());
            } finally {
                updateNotifications(context);
            }
        }
        return false;
    }

    public boolean setWatched(String key, String ratingKey, Context context) {

        boolean ret = false;
        if (mFactory != null) {
            Log.d("PlexServer", "Setting Watched for : " + ratingKey);
            try {
                ret = mFactory.setWatched(key, ratingKey);
            } catch (Exception e) {
                Log.e(getClass().getName(), e.toString());
            } finally {
                updateNotifications(context);
            }
        }
        return ret;
    }

    public boolean toggleWatchedState(String key, String ratingKey, boolean isWatched, Context context) {

        if (isWatched)
            return setUnwatched(key, ratingKey, context);
        return setWatched(key, ratingKey, context);
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

    public void setCurrentPlayingVideoRatingKey(long ratingKey) {
        synchronized (this) {
            mCurrentPlayingRatingKey = ratingKey;
        }
    }

    public long getCurrentPlayingVideoRatingKey() {

        long ret;
        synchronized (this) {
            ret = mCurrentPlayingRatingKey;
        }
        return ret;
    }

    public boolean hasServerToken() {
        String token = mTokens.get(mServerKey);
        return token != null && !token.isEmpty();
    }

    public String getToken() {
        return mFactory.getToken();
    }

    public boolean fetchServerToken(String user, String pass) {
        HttpsURLConnection con = null;
        boolean ret = false;
        try {
            URL url = new URL("https://plex.tv/users/sign_in.json");
            con = (HttpsURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setChunkedStreamingMode(0);
            mConfiguration.fillRequestProperties(con);
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            OutputStream os = new BufferedOutputStream(con.getOutputStream());
            StringBuilder data = new StringBuilder();
            data.append(URLEncoder.encode("user[login]", "UTF-8"));
            data.append("=");
            data.append(URLEncoder.encode(user, "UTF-8"));
            data.append("&");
            data.append(URLEncoder.encode("user[password]", "UTF-8"));
            data.append("=");
            data.append(URLEncoder.encode(pass, "UTF-8"));
            os.write(data.toString().getBytes());
            os.flush();
            int responseCode = con.getResponseCode();
            if (responseCode != HttpsURLConnection.HTTP_OK &&
                responseCode != HttpsURLConnection.HTTP_CREATED)
                return false;

            JsonReader reader = new JsonReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
            try {
                reader.beginObject();
                String userObj = reader.nextName();
                if (userObj.equals("user")) {
                    reader.beginObject();
                    while (reader.hasNext()) {
                        String name = reader.nextName();
                        if (name.equals("authentication_token")) {
                            String token = reader.nextString();
                            if (token != null && !token.isEmpty()) {
                                mTokens.put(mServerKey, token);
                                mConfiguration.setServerToken(token);
                                ret = true;
                            }
                        } else {
                            reader.skipValue();
                        }
                    }
                    reader.endObject();
                }
                reader.endObject();
            } finally {
                reader.close();
            }

        } catch (Exception ex) {
            return false;
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
        return ret;
    }
}
