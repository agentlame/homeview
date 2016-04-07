package com.monsterbutt.homeview.ui.handler;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v17.leanback.app.BackgroundManager;
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

import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

import us.nineworlds.plex.rest.model.impl.MediaContainer;

public class MediaCardBackgroundHandler {

    private static final int BACKGROUND_UPDATE_DELAY = 500;
    private final Handler mHandler = new Handler();

    private Activity mActivity;
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private Timer mBackgroundTimer = null;
    private BackgroundManager mBackgroundManager;

    private SimpleTarget<GlideDrawable> mTarget;
    private final String mLock = "lock";
    private boolean mSetting = false;


    public MediaCardBackgroundHandler(Activity activity) {

        mActivity = activity;
        mBackgroundManager = BackgroundManager.getInstance(mActivity);
        mBackgroundManager.attach(mActivity.getWindow());
        mDefaultBackground = mActivity.getDrawable(R.drawable.default_background);
        mMetrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    public void cancel() {

        synchronized (mLock) {

            if (mSetting)
                Glide.clear(mTarget);

            if (null != mBackgroundTimer)
                mBackgroundTimer.cancel();
        }
    }

    public void updateBackground(String uri, boolean animate) {

        if (mActivity.isDestroyed() || mActivity.isFinishing())
            return;

        cancel();
        synchronized (mLock) {

            mSetting = true;
            mTarget = new SimpleTarget<GlideDrawable>(mMetrics.widthPixels, mMetrics.heightPixels) {
                @Override
                public void onResourceReady(GlideDrawable resource,
                                            GlideAnimation<? super GlideDrawable> glideAnimation) {
                    synchronized (mLock) {
                        mSetting = false;
                        if (resource != null)
                            mBackgroundManager.setDrawable(resource);
                    }
                }
            };

            DrawableRequestBuilder builder = Glide.with(mActivity)
                                            .load(uri)
                                            .centerCrop()
                                            .error(mDefaultBackground);
            if (!animate)
               builder.dontAnimate();
            builder.into(mTarget);
        }
    }

    private void startBackgroundTimer(String path) {

        cancel();
        mBackgroundTimer = new Timer();
        mBackgroundTimer.schedule(new UpdateBackgroundTask(path), BACKGROUND_UPDATE_DELAY);
    }

    public String updateBackgroundTimed(PlexServer server, CardObject item) {

        String url;
        if (item instanceof SceneCard) {

            SceneCard card = (SceneCard) item;
            if (!card.useItemBackgroundArt()) {

                new GetRandomArtForSectionTask(server).execute(card.getSectionId());
                return "";
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

        if (url != null && !url.isEmpty())
            startBackgroundTimer(server.makeServerURL(url));
        return url;
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

            if (mServer == null)
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
