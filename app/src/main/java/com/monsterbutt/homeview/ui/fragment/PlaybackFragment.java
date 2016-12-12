package com.monsterbutt.homeview.ui.fragment;


import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;

import com.google.android.exoplayer2.text.CaptionStyleCompat;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.ui.SubtitleView;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.PlexServerManager;
import com.monsterbutt.homeview.services.ThemeService;
import com.monsterbutt.homeview.ui.android.HomeViewActivity;
import com.monsterbutt.homeview.ui.handler.CurrentVideoHandler;
import com.monsterbutt.homeview.ui.handler.MediaSessionHandler;
import com.monsterbutt.homeview.ui.handler.NextUpHandler;
import com.monsterbutt.homeview.ui.handler.PlaybackUIHandler;
import com.monsterbutt.homeview.ui.handler.VideoPlayerHandler;

import java.util.Timer;

public class PlaybackFragment
        extends android.support.v17.leanback.app.PlaybackOverlayFragment
        implements HomeViewActivity.OnStopKeyListener, HomeViewActivity.OnBackPressedListener {

    private static final int BACKGROUND_TYPE = PlaybackFragment.BG_NONE;

    private VideoPlayerHandler mPlayerHandler;
    private CurrentVideoHandler mCurrentVideoHandler;
    private MediaSessionHandler mMediaSessionHandler;
    private PlaybackUIHandler mPlaybackUIHandler;

    private OnFadeCompleteListener mFadeListener = new OnFadeCompleteListener() {

        private Timer mTimer = new Timer();
        private boolean mRunning = false;

        @Override
        public void onFadeInComplete() {

            synchronized (this) {

                mPlayerHandler.setGuiShowing(true);

                if (mRunning)
                    return;
                mRunning = false;
                mPlaybackUIHandler.updateProgress();
            }

        }
        @Override
        public void onFadeOutComplete() {

            synchronized (this) {

                mPlayerHandler.setGuiShowing(false);
                mPlaybackUIHandler.disableUpdateProgress();
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
        SubtitleView subtitleView = videoFrame.getSubtitleView();
        if (subtitleView != null) {
            subtitleView.setStyle(new CaptionStyleCompat(Color.WHITE,
                    Color.TRANSPARENT, Color.TRANSPARENT,
                    CaptionStyleCompat.EDGE_TYPE_OUTLINE, Color.BLACK, null));
        }
        mMediaSessionHandler = new MediaSessionHandler(this);

        mCurrentVideoHandler = new CurrentVideoHandler(this, server, mMediaSessionHandler);
        mPlayerHandler = new VideoPlayerHandler(this, server, mMediaSessionHandler,
                                                mCurrentVideoHandler, videoFrame);
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
        mPlaybackUIHandler.onStop();
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
