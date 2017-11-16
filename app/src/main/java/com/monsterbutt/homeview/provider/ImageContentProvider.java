package com.monsterbutt.homeview.provider;

import android.annotation.SuppressLint;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
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


@SuppressLint("Registered")
public class ImageContentProvider extends ContentProvider {

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {

        ParcelFileDescriptor[] pipe;
        try {
            String decodedUrl = uri.toString().replaceFirst(getContentURI(), "");
            pipe = ParcelFileDescriptor.createPipe();

            PlexServer server = PlexServerManager.getInstance().getSelectedServer();
            HttpURLConnection connection = (HttpURLConnection) new URL(server.makeServerURL(decodedUrl)).openConnection();

            new TransferThread(connection.getInputStream(),
                    new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]))
                    .start();
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Exception opening pipe", e);
            throw new FileNotFoundException("Could not open pipe for: "
                    + uri.toString());
        }

        return (pipe[0]);
    }

    protected String getContentURI() {
        return "";
    }

    static class TransferThread extends Thread {
        InputStream in;
        OutputStream out;

        TransferThread(InputStream in, OutputStream out) {
            this.in = in;
            this.out = out;
        }

        @Override
        public void run() {
            byte[] buf = new byte[8192];
            int len;

            try {
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }

                in.close();
                out.flush();
                out.close();
            } catch (IOException e) {
                Log.e(getClass().getSimpleName(),
                        "Exception transferring file", e);
            }
        }
    }
}
