package com.monsterbutt.homeview.ui.fragment;


import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;

import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.PlexServerManager;
import com.monsterbutt.homeview.services.ThemeService;
import com.monsterbutt.homeview.ui.android.HomeViewActivity;
import com.monsterbutt.homeview.ui.handler.CurrentVideoHandler;
import com.monsterbutt.homeview.ui.handler.MediaSessionHandler;
import com.monsterbutt.homeview.ui.handler.NextUpHandler;
import com.monsterbutt.homeview.ui.handler.PlaybackUIHandler;
import com.monsterbutt.homeview.ui.handler.SubtitleHandler;
import com.monsterbutt.homeview.ui.handler.VideoPlayerHandler;

import java.util.Timer;
import java.util.TimerTask;

public class PlaybackFragment
        extends android.support.v17.leanback.app.PlaybackOverlayFragment
        implements HomeViewActivity.OnStopKeyListener, HomeViewActivity.OnBackPressedListener {

    private static final int BACKGROUND_TYPE = PlaybackFragment.BG_LIGHT;

    private VideoPlayerHandler mPlayerHandler;
    private CurrentVideoHandler mCurrentVideoHandler;
    private MediaSessionHandler mMediaSessionHandler;
    private PlaybackUIHandler mPlaybackUIHandler;

    private OnFadeCompleteListener mFadeListener = new OnFadeCompleteListener() {

        private final static int HALF_SECOND = 500;
        private Timer mTimer = new Timer();
        private boolean mRunning = false;

        @Override
        public void onFadeInComplete() {

            synchronized (this) {

                mPlayerHandler.setGuiShowing(true);
                if (mRunning)
                    return;
                mRunning = false;
                mTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        mPlaybackUIHandler.updateProgress();
                    }
                }, 0, HALF_SECOND);
            }

        }
        @Override
        public void onFadeOutComplete() {

            synchronized (this) {

                mPlayerHandler.setGuiShowing(false);
                mTimer.cancel();
                mRunning = false;
                mTimer = new Timer();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Activity activity = getActivity();
        setBackgroundType(BACKGROUND_TYPE);
        PlexServer server = PlexServerManager.getInstance(getActivity().getApplicationContext()).getSelectedServer();
        SimpleExoPlayerView videoFrame = (SimpleExoPlayerView) activity.findViewById(R.id.player_view);
        videoFrame.setUseController(false);
        SubtitleHandler subtitleHandler = new SubtitleHandler(activity, videoFrame,
                (ImageView) activity.findViewById(R.id.imageSubtitles));
        mMediaSessionHandler = new MediaSessionHandler(this);

        mCurrentVideoHandler = new CurrentVideoHandler(this, server, mMediaSessionHandler, subtitleHandler);
        mPlayerHandler = new VideoPlayerHandler(this, server, mMediaSessionHandler,
                                                mCurrentVideoHandler, subtitleHandler, videoFrame);
        mCurrentVideoHandler.setHandler(mPlayerHandler, new NextUpHandler(activity, mPlayerHandler));
        mPlaybackUIHandler = new PlaybackUIHandler(this, server, mCurrentVideoHandler);
        mPlayerHandler.setUIHandler(mPlaybackUIHandler);
        mMediaSessionHandler.attach(mPlayerHandler, mCurrentVideoHandler, mPlaybackUIHandler);
        ((HomeViewActivity)getActivity()).setStopKeyListner(this);
        ((HomeViewActivity)getActivity()).setBackPressedListener(this);
        setFadeCompleteListener(mFadeListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        mPlayerHandler.onStop();
        mCurrentVideoHandler.updateRecommendations(getActivity(), false);
        mMediaSessionHandler.onStop();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mMediaSessionHandler.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();

        ThemeService.stopTheme(getActivity());
        mPlayerHandler.onResume();
    }

    @TargetApi(24)
    @Override
    public void onPause() {

        super.onPause();

        if (Build.VERSION.SDK_INT <= 23 || !getActivity().isInPictureInPictureMode())
            mPlayerHandler.onPause();
    }

    public VideoPlayerHandler getPlaybackHandler() { return mPlayerHandler; }

    @Override
    public boolean stopKeyPressed() {

        getActivity().finish();
        return true;
    }

    @Override
    public void tickle() {

        mCurrentVideoHandler.getNextUpHandler().dismiss();
        super.tickle();
    }

    @Override
    public boolean backPressed() {
        return mCurrentVideoHandler.getNextUpHandler().dismiss();
    }

    /*@Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {

        mPlayerHandler.pipModeChanged(isInPictureInPictureMode);
        if (!isInPictureInPictureMode)
            tickle();
    }*/
}
