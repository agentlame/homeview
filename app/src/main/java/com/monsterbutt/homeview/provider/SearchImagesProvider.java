package com.monsterbutt.homeview.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
import android.util.Log;

import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.PlexServerManager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class SearchImagesProvider extends ImageContentProvider {

    public static String AUTHORITY = "com.monsterbutt.homeview.provider.SearchImagesProvider";
    public static String CONTENT_URI = "content://" + AUTHORITY + "/";

    @Override
    protected String getContentURI() {
        return CONTENT_URI;
    }
}

