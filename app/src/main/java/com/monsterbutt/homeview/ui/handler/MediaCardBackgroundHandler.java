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


    public MediaCardBackgroundHandler(Activity activity) {

        mActivity = activity;
        mDefaultBackground = mActivity.getDrawable(R.drawable.default_background);
        mMetrics = new DisplayMetrics();

        onResume();
    }

    public String getBackgroundURL() { return mBackgroundURL; }

    public void updateBackground(String uri, boolean animate) {

        if (mActivity.isDestroyed() || mActivity.isFinishing())
            return;

        onPause();
        synchronized (this) {

            mBackgroundURL = uri;
            mSetting = true;
            mTarget = new SimpleTarget<GlideDrawable>(mMetrics.widthPixels, mMetrics.heightPixels) {
                @Override
                public void onResourceReady(GlideDrawable resource,
                                            GlideAnimation<? super GlideDrawable> glideAnimation) {
                    synchronized (this) {
                        mSetting = false;
                        if (resource != null)
                            mBackgroundManager.setDrawable(resource);
                    }
                }
            };

            DrawableRequestBuilder builder = Glide.with(mActivity)
                                            .load(mBackgroundURL)
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
        mBackgroundTimer.schedule(new UpdateBackgroundTask(path), BACKGROUND_UPDATE_DELAY);
    }

    public void updateBackgroundTimed(PlexServer server, CardObject item) {

        String url;
        if (item instanceof SceneCard) {

            SceneCard card = (SceneCard) item;
            if (!card.useItemBackgroundArt()) {

                new GetRandomArtForSectionTask(server).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, card.getSectionId());
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
            startBackgroundTimer(server.makeServerURL(mBackgroundURL));
    }

    @Override
    public void onResume() {

        synchronized (this) {

            mBackgroundManager = BackgroundManager.getInstance(mActivity);
            if (!mBackgroundManager.isAttached())
                mBackgroundManager.attach(mActivity.getWindow());
            mActivity.getWindowManager().getDefaultDisplay().getMetrics(mMetrics);

            if (!TextUtils.isEmpty(mBackgroundURL))
                updateBackground(mBackgroundURL, false);
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
        public UpdateBackgroundTask(String path) {

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

        public GetRandomArtForSectionTask(PlexServer server) {
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
                startBackgroundTimer(mServer.makeServerURL(result.getPhotos().get(pos).getKey()));
            }
        }
    }
}
