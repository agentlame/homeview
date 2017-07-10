package com.monsterbutt.homeview.ui.handler;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v17.leanback.app.BackgroundManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import com.bumptech.glide.DrawableRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.model.Video;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.presenters.CardObject;
import com.monsterbutt.homeview.presenters.PosterCard;
import com.monsterbutt.homeview.presenters.SceneCard;
import com.monsterbutt.homeview.ui.UILifecycleManager;

import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

import us.nineworlds.plex.rest.model.impl.MediaContainer;

public class MediaCardBackgroundHandler implements UILifecycleManager.LifecycleListener {

    private static final int BACKGROUND_UPDATE_DELAY = 500;
    private final Handler mHandler = new Handler();

    private Activity mActivity;
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private Timer mBackgroundTimer = null;
    private BackgroundManager mBackgroundManager;

    private SimpleTarget<GlideDrawable> mTarget;
    private boolean mSetting = false;

    private String mBackgroundURL = "";
    private String mWaitingBackgroundURL = "";

    private final PlexServer mServer;

    MediaCardBackgroundHandler(Activity activity, PlexServer server, String backgroundURI) {

        mServer = server;
        mActivity = activity;
        mDefaultBackground = mActivity.getDrawable(R.drawable.default_background);
        mMetrics = new DisplayMetrics();
        if (!TextUtils.isEmpty(backgroundURI))
            mBackgroundURL = backgroundURI;
        onResume();
    }

    public String getBackgroundURL() { return mBackgroundURL; }

    public void updateBackground(String uri, boolean animate) {

        if (mServer == null || mActivity.isDestroyed() || mActivity.isFinishing() ||
         (!TextUtils.isEmpty(uri) && uri.equalsIgnoreCase(mBackgroundURL)))
            return;

        onPause();
        synchronized (this) {

            mBackgroundURL = uri;
            mWaitingBackgroundURL = "";
            mSetting = true;
            mTarget = new SimpleTarget<GlideDrawable>(mMetrics.widthPixels, mMetrics.heightPixels) {
                @Override
                public void onResourceReady(GlideDrawable resource,
                                            GlideAnimation<? super GlideDrawable> glideAnimation) {
                    synchronized (this) {
                        mSetting = false;
                        if (resource != null && mBackgroundManager != null)
                            mBackgroundManager.setDrawable(resource);
                    }
                }
            };

            DrawableRequestBuilder builder = Glide.with(mActivity)
                                            .load(mServer.makeServerURL(mBackgroundURL))
                                            .centerCrop()
                                            .error(mDefaultBackground);
            if (!animate)
               builder.dontAnimate();
            builder.into(mTarget);
        }
    }

    private void startBackgroundTimer(String path) {

        onPause();
        mBackgroundTimer = new Timer();
        synchronized (this) {
            mWaitingBackgroundURL = path;
        }
        mBackgroundTimer.schedule(new UpdateBackgroundTask(path), BACKGROUND_UPDATE_DELAY);
    }

    void updateBackgroundTimed(CardObject item) {

        if (mServer == null)
            return;

        String url;
        if (item instanceof SceneCard) {

            SceneCard card = (SceneCard) item;
            if (!card.useItemBackgroundArt()) {

                new GetRandomArtForSectionTask(mServer).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, card.getSectionId());
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
    public void onResume() {

        synchronized (this) {

            mBackgroundManager = BackgroundManager.getInstance(mActivity);
            if (!mBackgroundManager.isAttached())
                mBackgroundManager.attach(mActivity.getWindow());
            mActivity.getWindowManager().getDefaultDisplay().getMetrics(mMetrics);

            if (!TextUtils.isEmpty(mBackgroundURL) || !TextUtils.isEmpty(mWaitingBackgroundURL)) {
                if (!TextUtils.isEmpty(mWaitingBackgroundURL))
                    mBackgroundURL = mWaitingBackgroundURL;
                updateBackground(mBackgroundURL, false);
            }
        }
    }

    @Override
    public void onPause() {

        synchronized (this) {

            if (mSetting)
                Glide.clear(mTarget);

            if (null != mBackgroundTimer)
                mBackgroundTimer.cancel();
        }
    }

    @Override
    public void onDestroyed() {
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
                        updateBackground(mBackgroundURI.toString(), true);
                    }
                }
            });
        }
    }

    private class GetRandomArtForSectionTask extends AsyncTask<String, Void, MediaContainer> {

        final PlexServer mServer;

        GetRandomArtForSectionTask(PlexServer server) {
            mServer = server;
        }

        @Override
        protected MediaContainer doInBackground(String... params) {

            if (mServer == null || params == null || params.length == 0 || params[0] == null)
                return null;
            return mServer.getSectionArts(params[0]);
        }

        @Override
        protected void onPostExecute(MediaContainer result) {

            if (result != null && result.getPhotos() != null && !result.getPhotos().isEmpty()) {

                int pos = (int) (1000 * Math.random()) % result.getPhotos().size();
                startBackgroundTimer(result.getPhotos().get(pos).getKey());
            }
        }
    }
}
