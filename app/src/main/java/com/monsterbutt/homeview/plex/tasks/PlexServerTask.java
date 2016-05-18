package com.monsterbutt.homeview.plex.tasks;

import android.os.AsyncTask;

import com.monsterbutt.homeview.plex.PlexServer;


public class PlexServerTask extends AsyncTask<Object, Void, Boolean> {

    private PlexServerTaskCaller mCaller = null;
    private PlexServer mServer = null;

    protected PlexServerTask(PlexServerTaskCaller caller, PlexServer server) {

        mCaller = caller;
        mServer = server;
    }

    protected PlexServer getServer() {

        return mServer;
    }

    @Override
    protected Boolean doInBackground(Object... params) {

        return false;
    }

    @Override
    protected void onPostExecute(Boolean result) {

        mCaller.handlePostTaskUI(result, this);
    }
}
