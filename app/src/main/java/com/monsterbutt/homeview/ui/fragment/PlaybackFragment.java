package com.monsterbutt.homeview.ui.fragment;


import android.app.Activity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.exoplayer.AspectRatioFrameLayout;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.PlexServerManager;
import com.monsterbutt.homeview.services.ThemeService;
import com.monsterbutt.homeview.ui.activity.PlaybackActivity;
import com.monsterbutt.homeview.ui.android.HomeViewActivity;
import com.monsterbutt.homeview.ui.handler.CurrentVideoHandler;
import com.monsterbutt.homeview.ui.handler.MediaSessionHandler;
import com.monsterbutt.homeview.ui.handler.NextUpHandler;
import com.monsterbutt.homeview.ui.handler.PlaybackUIHandler;
import com.monsterbutt.homeview.ui.handler.SubtitleHandler;
import com.monsterbutt.homeview.ui.handler.VideoPlayerHandler;

public class PlaybackFragment
        extends android.support.v17.leanback.app.PlaybackOverlayFragment
        implements HomeViewActivity.OnStopKeyListener, HomeViewActivity.OnBackPressedListener {

    private static final int BACKGROUND_TYPE = PlaybackFragment.BG_LIGHT;

    private VideoPlayerHandler mPlayerHandler;
    private CurrentVideoHandler mCurrentVideoHandler;
    private MediaSessionHandler mMediaSessionHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Activity activity = getActivity();
        setBackgroundType(BACKGROUND_TYPE);
        PlexServer server = PlexServerManager.getInstance(getActivity().getApplicationContext()).getSelectedServer();
        AspectRatioFrameLayout videoFrame = (AspectRatioFrameLayout) activity.findViewById(R.id.video_frame);
        SubtitleHandler subtitleHandler = new SubtitleHandler(activity, videoFrame,
                (ImageView) activity.findViewById(R.id.imageSubtitles),
                (TextView) activity.findViewById(R.id.textSubtitles));
        mMediaSessionHandler = new MediaSessionHandler(this);
        mCurrentVideoHandler = new CurrentVideoHandler(this, server, mMediaSessionHandler, subtitleHandler);
        mPlayerHandler = new VideoPlayerHandler(this, server, mMediaSessionHandler,
                                                mCurrentVideoHandler, subtitleHandler, videoFrame);
        mCurrentVideoHandler.setHandler(mPlayerHandler);
        PlaybackUIHandler playbackUIHandler = new PlaybackUIHandler(this, server, mCurrentVideoHandler);
        mPlayerHandler.setUIHandler(playbackUIHandler);
        mMediaSessionHandler.attach(mPlayerHandler, mCurrentVideoHandler, playbackUIHandler);
        ((HomeViewActivity)getActivity()).setStopKeyListner(this);
        ((HomeViewActivity)getActivity()).setBackPressedListener(this);
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

    @Override
    public void onPause() {
        super.onPause();

       // if ((Build.VERSION.SDK_INT <= 23 && !Build.VERSION.CODENAME.equals("N")) || !getActivity().isInPictureInPictureMode())
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

        mCurrentVideoHandler.getNextUpHandler().setNextUpVisible(false);
        super.tickle();
    }

    @Override
    public boolean backPressed() {

        NextUpHandler handler = mCurrentVideoHandler.getNextUpHandler();
        if (handler.getNextUpVisible()) {
            handler.setNextUpVisible(false);
            return true;
        }
        return false;
    }

    /*@Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {

        mPlayerHandler.pipModeChanged(isInPictureInPictureMode);
        if (!isInPictureInPictureMode)
            tickle();
    }*/
}
