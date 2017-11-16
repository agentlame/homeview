package com.monsterbutt.homeview.ui;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v17.leanback.app.BackgroundManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;

import com.bumptech.glide.DrawableRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.model.Video;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.ui.presenters.CardObject;
import com.monsterbutt.homeview.ui.presenters.PosterCard;
import com.monsterbutt.homeview.ui.presenters.SceneCard;
import com.monsterbutt.homeview.ui.interfaces.ILifecycleListener;

import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

import us.nineworlds.plex.rest.model.impl.MediaContainer;

public class BackgroundHandler implements ILifecycleListener {

    private static final int BACKGROUND_UPDATE_DELAY = 500;
    private final Handler mHandler = new Handler();

    private Activity mActivity;
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private Timer mBackgroundTimer = null;
    private BackgroundManager mBackgroundManager;

    private String mBackgroundURL = "";
    private String mWaitingBackgroundURL = "";
    private String mLoadedBackgroundURL = "";

    private boolean skippedFirstSection = false;

    private PlexServer server;
    private final static String Tag = "HV_BackgroundHandler";

    public BackgroundHandler(Activity activity, PlexServer server,
                             UILifecycleManager lifecycleManager) {
        this(activity, server, lifecycleManager,  "");
    }

    public BackgroundHandler(Activity activity, PlexServer server,
                             UILifecycleManager lifeCycleMgr, String backgroundURI) {
        if (lifeCycleMgr != null)
            lifeCycleMgr.register(BackgroundHandler.class.getCanonicalName(), this);
        mActivity = activity;
        mDefaultBackground = mActivity.getDrawable(R.drawable.default_background);
        mMetrics = new DisplayMetrics();
        if (!TextUtils.isEmpty(backgroundURI) && !isResourceArtPath(backgroundURI))
            mBackgroundURL = backgroundURI;
        setServer(server);
    }

    public void setServer(PlexServer server) {
        this.server = server;
    }

    public String getBackgroundURL() { return mBackgroundURL; }

    public void updateBackground(final String uri) {
        updateBackground(uri, false);
    }

    private boolean isResourceArtPath(String uri) {
        return !TextUtils.isEmpty(uri) && uri.startsWith("/:/resources");
    }

    private void updateBackground(final String uri, boolean wasResumed) {

        if (server == null || isResourceArtPath(uri) || mActivity.isDestroyed() || mActivity.isFinishing() ||
         (!wasResumed && !TextUtils.isEmpty(uri) && uri.equalsIgnoreCase(mLoadedBackgroundURL)))
            return;

        killTimer();
        synchronized (this) {

            mBackgroundURL = uri;
            Log.d(Tag, "Loading background : " + uri);
            mWaitingBackgroundURL = "";
            if (mMetrics.widthPixels == 0 || mMetrics.heightPixels == 0)
                return;
            SimpleTarget<GlideDrawable> target = new SimpleTarget<GlideDrawable>(mMetrics.widthPixels, mMetrics.heightPixels) {
                @Override
                public void onResourceReady(GlideDrawable resource,
                                            GlideAnimation<? super GlideDrawable> glideAnimation) {
                    synchronized (this) {
                        if (resource != null && mBackgroundManager != null)
                            mBackgroundManager.setDrawable(resource);
                        mLoadedBackgroundURL = uri;
                    }
                }
            };

            DrawableRequestBuilder builder = Glide.with(mActivity)
                                            .load(server.makeServerURL(mBackgroundURL))
                                            .centerCrop()
                                            .error(mDefaultBackground);
            builder.into(target);
        }
    }

    private void startBackgroundTimer(String path) {
        killTimer();
        mBackgroundTimer = new Timer();
        synchronized (this) {
            mWaitingBackgroundURL = path;
        }
        mBackgroundTimer.schedule(new UpdateBackgroundTask(path), BACKGROUND_UPDATE_DELAY);
    }

    private void killTimer() {
        synchronized (this) {
            if (null != mBackgroundTimer)
                mBackgroundTimer.cancel();
        }
    }

    public void updateBackgroundTimed(CardObject item) {
        if (server == null)
            return;

        String url;
        if (item instanceof SceneCard) {

            SceneCard card = (SceneCard) item;
            if (!card.useItemBackgroundArt()) {

                new GetRandomArtForSectionTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, card.getSectionId());
                return;
            }
            else
                url = item.getBackgroundImageUrl();
        }
        else if (item instanceof PosterCard)
            url = item.getBackgroundImageUrl();
        else if (item instanceof Video)
            url = item.getBackgroundImageUrl();
        else
            url = "blank";

        mBackgroundURL = url;
        if (mBackgroundURL != null && !mBackgroundURL.isEmpty())
            startBackgroundTimer(mBackgroundURL);
    }

    @Override
    public void onResume(UILifecycleManager lifeCycleMgr) {
        synchronized (this) {
            mBackgroundManager = BackgroundManager.getInstance(mActivity);
            if (!mBackgroundManager.isAttached())
                mBackgroundManager.attach(mActivity.getWindow());
            mActivity.getWindowManager().getDefaultDisplay().getMetrics(mMetrics);

            if (!TextUtils.isEmpty(mBackgroundURL) || !TextUtils.isEmpty(mWaitingBackgroundURL)) {
                if (!TextUtils.isEmpty(mWaitingBackgroundURL))
                    mBackgroundURL = mWaitingBackgroundURL;
                updateBackground(mBackgroundURL, true);
            }
        }
    }

    @Override
    public void onPause(UILifecycleManager lifeCycleMgr) { killTimer(); }

    @Override
    public void onDestroyed(UILifecycleManager lifeCycleMgr) {
        lifeCycleMgr.unregister(BackgroundHandler.class.getCanonicalName());
    }

    private class UpdateBackgroundTask extends TimerTask {

        private URI mBackgroundURI;
        UpdateBackgroundTask(String path) {

            mBackgroundURI = URI.create(path);
        }

        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mBackgroundURI != null) {
                        updateBackground(mBackgroundURI.toString(), false);
                    }
                }
            });
        }
    }

    private static class GetRandomArtForSectionTask extends AsyncTask<String, Void, MediaContainer> {

        final BackgroundHandler mHandler;

        GetRandomArtForSectionTask( BackgroundHandler handler) {
            mHandler = handler;
        }

        @Override
        protected MediaContainer doInBackground(String... params) {

            if (!mHandler.skippedFirstSection) {
                mHandler.skippedFirstSection = true;
                return null;
            }

            if (mHandler.server == null || params == null || params.length == 0 || params[0] == null)
                return null;
            return mHandler.server.getSectionArts(params[0]);
        }

        @Override
        protected void onPostExecute(MediaContainer result) {

            if (result != null && result.getPhotos() != null && !result.getPhotos().isEmpty()) {

                int pos = (int) (1000 * Math.random()) % result.getPhotos().size();
                mHandler.startBackgroundTimer(result.getPhotos().get(pos).getKey());
            }
        }
    }
}
