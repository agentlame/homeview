package com.monsterbutt.homeview.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;

public class BackgroundContentProvider extends ImageContentProvider {

    public static String AUTHORITY = "com.monsterbutt.homeview.provider.BackgroundContentProvider";
    public static String CONTENT_URI = "content://" + AUTHORITY + "/";

    @Override
    protected String getContentURI() {
        return CONTENT_URI;
    }
}
