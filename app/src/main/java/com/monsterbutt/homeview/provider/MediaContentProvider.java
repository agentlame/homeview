package com.monsterbutt.homeview.provider;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.monsterbutt.homeview.data.VideoContract;
import com.monsterbutt.homeview.data.VideoContract.VideoEntry;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.PlexServerManager;
import com.monsterbutt.homeview.plex.media.PlexContainerItem;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;

import java.util.ArrayList;
import java.util.List;

import us.nineworlds.plex.rest.model.impl.Directory;
import us.nineworlds.plex.rest.model.impl.MediaContainer;
import us.nineworlds.plex.rest.model.impl.Video;


public class MediaContentProvider extends ContentProvider {
    private final UriMatcher sUriMatcher = buildUriMatcher();

    // These codes are returned from sUriMatcher#match when the respective Uri matches.
    private static final int VIDEO = 1;
    private static final int VIDEO_WITH_QUEUE = 2;

    private static final int SEARCH_SUGGEST = 3;
    private static final int REFRESH_SHORTCUT = 4;

    public static final String ID_DETAIL = "detail";
    public static final String ID_PLAYBACK = "playback";

    private ContentResolver mContentResolver;

    protected PlexServer mServer;

    @Override
    public boolean onCreate() {
        Context context = getContext();
        mContentResolver = context.getContentResolver();
        mServer = PlexServerManager.getInstance(getContext().getApplicationContext()).getSelectedServer();
        return true;
    }

    private UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = VideoContract.CONTENT_AUTHORITY;

        // For each type of URI to add, create a corresponding code.
        matcher.addURI(authority, VideoContract.PATH_VIDEO, VIDEO);
        matcher.addURI(authority, VideoContract.PATH_VIDEO + "/*", VIDEO_WITH_QUEUE);

        // Search related URIs.
        matcher.addURI(authority, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
        matcher.addURI(authority, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST);
        return matcher;
    }

    private MatrixCursor getCursor() {

        return new MatrixCursor(new String[] {
                VideoEntry._ID,
                VideoEntry.COLUMN_NAME,
                VideoEntry.COLUMN_SUBTITLE,
                VideoEntry.COLUMN_DESC,
                VideoEntry.COLUMN_CATEGORY,
                VideoEntry.COLUMN_VIDEO_URL,
                VideoEntry.COLUMN_BG_IMAGE_URL,
                VideoEntry.COLUMN_CARD_IMG,
                VideoEntry.COLUMN_STUDIO,
                VideoEntry.COLUMN_CONTENT_TYPE,
                VideoEntry.COLUMN_IS_LIVE,
                VideoEntry.COLUMN_VIDEO_WIDTH,
                VideoEntry.COLUMN_VIDEO_HEIGHT,
                VideoEntry.COLUMN_AUDIO_CHANNEL_CONFIG,
                VideoEntry.COLUMN_PURCHASE_PRICE,
                VideoEntry.COLUMN_RENTAL_PRICE,
                VideoEntry.COLUMN_RATING_STYLE,
                VideoEntry.COLUMN_RATING_SCORE,
                VideoEntry.COLUMN_PRODUCTION_YEAR,
                VideoEntry.COLUMN_DURATION,
                VideoEntry.COLUMN_KEY,
                VideoEntry.COLUMN_FILEPATH,
                VideoEntry.COLUMN_SERVERPATH,
                VideoEntry.COLUMN_WATCHEDOFFSET,
                VideoEntry.COLUMN_WATCHED,
                VideoEntry.COLUMN_FRAMERATE,
                VideoEntry.COLUMN_SHOULDSTART,
                VideoEntry.COLUMN_DATA_ID,
                VideoEntry.COLUMN_EXTRA
        });
    }

    protected List<MediaContainer> getMedia(String rawQuery) {
        return new ArrayList<>();
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        MatrixCursor retCursor = getCursor();

        switch (sUriMatcher.match(uri)) {
            case VIDEO:
            case SEARCH_SUGGEST: {

                String rawQuery = "";
                if (selectionArgs != null && selectionArgs.length > 0) {
                    rawQuery = selectionArgs[0];
                }
                if (!rawQuery.isEmpty() && mServer != null) {
                    rawQuery = rawQuery.replace("%", "");
                    if (rawQuery.length() < 3)
                        return null;

                    List<MediaContainer> items = mServer.searchShowsForEpisodes(rawQuery);
                    if (items != null) {
                        for (MediaContainer mc : items) {

                            if (mc.getVideos() != null) {

                                String keyOverride = mc.getGrandparentKey();
                                String yearOverride = mc.getParentYear();
                                for (Video vid : mc.getVideos())
                                    PlexVideoItem.getItem(vid).fillQueryRow(retCursor.newRow(), getContext(), keyOverride, yearOverride, false);
                            }
                        }
                    }
                    items = mServer.searchForMedia(rawQuery);
                    if (items != null) {
                        for (MediaContainer mc : items) {

                            if (mc.getDirectories() != null) {

                                for (Directory dir : mc.getDirectories())
                                    PlexContainerItem.getItem(dir).fillQueryRow(retCursor.newRow(), getContext(), null, null, false);
                            }
                            if (mc.getVideos() != null) {

                                for (Video vid : mc.getVideos())
                                    PlexVideoItem.getItem(vid).fillQueryRow(retCursor.newRow(), getContext(), null, null, false);
                            }
                        }
                    }
                }
                break;
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }

        if (retCursor != null)
            retCursor.setNotificationUri(mContentResolver, uri);
        return retCursor;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        switch (sUriMatcher.match(uri)) {
            // The Android TV global search is querying our app for relevant content.
            case SEARCH_SUGGEST:
                return SearchManager.SUGGEST_MIME_TYPE;
            case REFRESH_SHORTCUT:
                return SearchManager.SHORTCUT_MIME_TYPE;

            // We aren't sure what is being asked of us.
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        return 0;
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        return 0;
    }


}