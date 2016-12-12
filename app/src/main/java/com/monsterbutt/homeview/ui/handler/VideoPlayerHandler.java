package com.monsterbutt.homeview.ui.handler;

import android.content.Intent;
import android.media.MediaMetadata;
import android.media.session.PlaybackState;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v17.leanback.app.PlaybackOverlayFragment;
import android.support.v17.leanback.widget.ListRow;
import android.util.Log;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.player.FrameRateSwitcher;
import com.monsterbutt.homeview.player.HomeViewPlayer;
import com.monsterbutt.homeview.player.StartPosition;
import com.monsterbutt.homeview.player.TrackSelector;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.Episode;
import com.monsterbutt.homeview.plex.media.Movie;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.plex.media.Season;
import com.monsterbutt.homeview.settings.SettingsManager;
import com.monsterbutt.homeview.ui.UILifecycleManager;
import com.monsterbutt.homeview.ui.activity.ContainerActivity;
import com.monsterbutt.homeview.ui.android.HomeViewActivity;
import com.monsterbutt.homeview.ui.fragment.PlaybackFragment;

import java.io.IOException;

import us.nineworlds.plex.rest.model.impl.MediaContainer;


public class VideoPlayerHandler implements ExoPlayer.EventListener,
                                            UILifecycleManager.LifecycleListener,
                                            PlaybackOverlayFragment.InputEventHandler,
                                            FrameRateSwitcher.FrameRateSwitcherListener,
                                            ExtractorMediaSource.EventListener {

    private static final int CHOOSER_TIMEOUT = 15 * 1000;
    private static final int DOUBLE_CLICK_TIMEOUT = 400;

    public static final String AUTO_PLAY = "auto_play";
    private static final Bundle mAutoPlayExtras = new Bundle();

    static {
        mAutoPlayExtras.putBoolean(AUTO_PLAY, true);
    }

    private final MediaSessionHandler mMediaSessionHandler;
    private final CurrentVideoHandler mCurrentVideoHandler;
    private PlaybackUIHandler mPlaybackUIHandler;
    private final HomeViewActivity mActivity;
    private final PlaybackFragment mFragment;
    private final PlexServer mServer;
    private HomeViewPlayer mPlayer;
    private final SimpleExoPlayerView mVideoFrame;
    private TrackSelector mTrackSelector = new TrackSelector();

    private ProgressBar mProgress = null;

    private long mLastRewind;
    private long mLastForward;

    private boolean mCheckForPIPChanged = false;
    private boolean mGuiShowing = false;
    private boolean mIsMetadataSet = false;

    private boolean mAllowNextTrack = true;

    private FrameRateSwitcher switcher = null;

    private boolean isVideoLoaded = false;

    private static final String Tag = "VideoPlayerHandler";

    public VideoPlayerHandler(PlaybackFragment fragment, PlexServer server,
                              MediaSessionHandler mediaSessionHandler,
                              CurrentVideoHandler currentVideoHandler,
                              SimpleExoPlayerView videoFrame) {

        mFragment = fragment;
        mActivity = (HomeViewActivity) fragment.getActivity();
        mProgress = (ProgressBar) mActivity.findViewById(R.id.progress);
        mServer = server;
        mMediaSessionHandler = mediaSessionHandler;
        mCurrentVideoHandler = currentVideoHandler;
        mVideoFrame = videoFrame;
    }

    public boolean isGuiShowing() {
        boolean ret;
        synchronized (this) {
            ret = mGuiShowing;
        }
        return ret;
    }

    public void setGuiShowing(boolean val) {
        synchronized (this) {
            mGuiShowing = val;
        }
    }

    public boolean rewind() {

        long time = System.currentTimeMillis();
        long offset = Long.valueOf(mActivity.getString(R.string.skip_back_seconds));
        long diff = time - mLastRewind;
        if (diff < DOUBLE_CLICK_TIMEOUT)
            offset = Long.valueOf(mActivity.getString(R.string.jump_backward_seconds));
        mLastRewind = time;
        return seekOffset(-offset);
    }

    public boolean fastForward() {

        long time = System.currentTimeMillis();
        long offset = Long.valueOf(mActivity.getString(R.string.skip_forward_seconds));
        long diff = time - mLastForward;
        if (diff < DOUBLE_CLICK_TIMEOUT)
            offset = Long.valueOf(mActivity.getString(R.string.jump_forward_seconds));
        mLastForward = time;
        return seekOffset(offset);
    }

    public boolean seekOffset(long offsetInSeconds) {

        long duration = mPlayer.getDuration();
        if (duration != C.TIME_UNSET) {

            long offsetInMilli = offsetInSeconds * 1000;
            int direction = offsetInSeconds > 0 ? PlaybackState.STATE_FAST_FORWARDING : PlaybackState.STATE_REWINDING;
            long position = mPlayer.getCurrentPosition();
            long remaining = duration - position;
            // cap the seek to nothing less then skip forward
            if (direction == PlaybackState.STATE_FAST_FORWARDING) {

                long stop = Long.valueOf(mActivity.getString(R.string.skip_forward_seconds)) * 1000;
                while (remaining < offsetInMilli) {

                    offsetInMilli /= 2;
                    if (offsetInMilli < stop)
                        return false;
                }
            }

            int prevState = getPlaybackState();
            mMediaSessionHandler.setPlaybackState(direction);
            long loc =  position + offsetInMilli;
            if (direction == PlaybackState.STATE_REWINDING && position < Math.abs(offsetInMilli))
                loc = 0;
            if (loc < mPlayer.getDuration()) {
                setPosition(loc);
                mMediaSessionHandler.setPlaybackState(prevState);
                return true;
            }
        }
        return false;
    }

    public void pipModeChanged(boolean isInPictureInPictureMode) {

        mServer.isPIPActive(isInPictureInPictureMode);
        if (!isInPictureInPictureMode)
            mCheckForPIPChanged = true;
    }

    @Override
    public void onResume() {

        boolean forcePlay = mCheckForPIPChanged && mCurrentVideoHandler.checkPassedVideo();
        PlexVideoItem video = mCurrentVideoHandler.getVideo();
        if ((mPlayer == null || forcePlay) && video != null)
            playVideo(video);
        mCheckForPIPChanged = false;
    }

    @Override
    public void onPause() {
        if (isPlaying()) {
            if (!mActivity.requestVisibleBehind(true))
                playPause(false);
        }
        else
            mActivity.requestVisibleBehind(false);
    }

    public void onStop() {

        if (switcher != null)
            switcher.unregister();

        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }

    @Override
    public void onDestroyed() { }

    public boolean isPlaying() {
        return mPlayer != null && ExoPlayer.STATE_READY == mPlayer.getPlaybackState() && mPlayer.getPlayWhenReady();
    }

    public void setPosition(long position) {

        if (mPlayer == null)
            return;
        int prevState = getPlaybackState();
        mMediaSessionHandler.setPlaybackState(position > getCurrentPosition() ? PlaybackState.STATE_REWINDING : PlaybackState.STATE_FAST_FORWARDING);
        if (position > mPlayer.getDuration())
            mPlayer.seekTo(mPlayer.getDuration());
        else if (position < 0)
            mPlayer.seekTo(0L);
        else
            mPlayer.seekTo(position);

        mMediaSessionHandler.setPlaybackState(prevState);
    }

    public long getBufferedPosition() {
        if (mPlayer != null)
            return mPlayer.getBufferedPosition();
        return 0L;
    }

    public long getCurrentPosition() {
        if (mPlayer != null)
            return mPlayer.getCurrentPosition();
        return 0L;
    }

    public long getDuration() {
        if (mPlayer != null)
            return mPlayer.getDuration();
        return C.TIME_UNSET;
    }

    @Override
    public void shouldPlay(boolean play) {

        synchronized (this) {
            if (isVideoLoaded) {
                if (mPlayer.getPlayWhenReady() != play)
                    playPause(play);
            } else if (play) {

                if (mPlayer == null) {

                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mPlayer = new HomeViewPlayer(mActivity, mTrackSelector, new DefaultLoadControl());
                            mPlayer.addListener(VideoPlayerHandler.this);
                            mVideoFrame.setPlayer(mPlayer);
                            mPlayer.prepare(mCurrentVideoHandler.getVideo(), mServer, mActivity, VideoPlayerHandler.this);
                        }
                    });

                }
                else
                    mPlayer.prepare(mCurrentVideoHandler.getVideo(), mServer, mActivity, this);
            }
        }
    }

    public void playPause(boolean doPlay) {

        if (mPlayer == null) {
            mMediaSessionHandler.setPlaybackState(PlaybackState.STATE_NONE);
            return;
        }

        if (doPlay) {

            if (getPlaybackState() != PlaybackState.STATE_PLAYING) {
                mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                mPlayer.setPlayWhenReady(true);
                mMediaSessionHandler.setPlaybackState(PlaybackState.STATE_PLAYING);
            }
        } else {
            mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            mPlayer.setPlayWhenReady(false);
            mMediaSessionHandler.setPlaybackState(PlaybackState.STATE_PAUSED);
        }
    }

    @Override
    public boolean handleInputEvent(InputEvent event) {

        if (isGuiShowing() || !(event instanceof KeyEvent))
            return false;

        KeyEvent key = (KeyEvent) event;
        switch (key.getKeyCode()) {

            case KeyEvent.KEYCODE_MEDIA_REWIND:
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (KeyEvent.ACTION_DOWN == key.getAction())
                    rewind();
                return true;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD:
                if (KeyEvent.ACTION_DOWN == key.getAction())
                    mMediaSessionHandler.onSkipToPrevious();
                return true;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (KeyEvent.ACTION_DOWN == key.getAction())
                    fastForward();
                return true;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD:
                if (KeyEvent.ACTION_DOWN == key.getAction())
                    mMediaSessionHandler.onSkipToNext();
                return true;
            default:
                break;
        }

        return false;
    }

    public boolean skipToNext() {

        if (mCurrentVideoHandler.hasNext() && shouldPlayNext())
            mActivity.getMediaController().getTransportControls().skipToNext();
        else
            return false;
        return true;
    }

    public void playVideo(PlexVideoItem video) {
        playVideo(video, false);
    }

    public void playVideo(PlexVideoItem video, boolean forceLoad) {

        if (video.selectedHasMissingData() && !forceLoad)
            new GetFullInfo().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, video.getKey());
        else {
            mLastRewind = 0;
            mLastForward = video.getDuration();
            mCurrentVideoHandler.setVideo(video);
            if (video.shouldUpdateStatusOnPlayback())
                mCurrentVideoHandler.updateProgressTask();
            mPlaybackUIHandler.setupVideoForPlayback();
            mMediaSessionHandler.setPlaybackState(PlaybackState.STATE_PAUSED);

            if (mPlayer != null) {
                mPlayer.stop();
                mPlayer.release();
                mPlayer = null;
            }
            mIsMetadataSet = false;
            isVideoLoaded = false;
            if (switcher != null)
                switcher.unregister();
            switcher = new FrameRateSwitcher(mActivity, this);
            if (!switcher.setDisplayRefreshRate(mCurrentVideoHandler.getVideo()))
                shouldPlay(true);
        }
    }

    public int getPlaybackState() { return mMediaSessionHandler.getPlaybackState(); }

    public PlexVideoItem getCurrentVideo() { return mCurrentVideoHandler.getVideo(); }

    public void setMetadata(MediaMetadata data) { mMediaSessionHandler.setMetadata(data); }

    public ListRow getCodecRowForVideo() { return mCurrentVideoHandler.getCodecRowForVideo(mTrackSelector); }

    public ListRow getExtrasRowForVideo() { return mCurrentVideoHandler.getExtrasRowForVideo(); }

    public void setUIHandler(PlaybackUIHandler playbackUIHandler) { mPlaybackUIHandler = playbackUIHandler; }

    private boolean shouldPlayNext() {

        PlexVideoItem current = mCurrentVideoHandler.getVideo();
        String playNext = SettingsManager.getInstance(mActivity.getApplicationContext()).getString("preferences_playback_playnext");
        return (mAllowNextTrack && playNext.equals("always")
            || (playNext.equals("shows") && current instanceof Episode)
            || (playNext.equals("movies") && current instanceof Movie));
    }

    public void enableNextTrack(boolean enable) {
        mAllowNextTrack = enable;
    }

    public void browseNextTrackParent() {

        PlexVideoItem item = mCurrentVideoHandler.getVideo();
        if (item != null) {

            Intent intent = new Intent(mActivity, ContainerActivity.class);
            String key = String.format("/library/sections/%s/all", item.getSectionId());
            if (item instanceof Episode) {
                key = String.format("%s/%s", ((Episode) item).getShowKey(), Season.ALL_SEASONS);
                intent.putExtra(ContainerActivity.USE_SCENE, true);
            }
            intent.putExtra(ContainerActivity.KEY, key);
            intent.putExtra(ContainerActivity.BACKGROUND, item.getBackgroundImageURL());
            intent.putExtra(ContainerActivity.SELECTED, mCurrentVideoHandler.getNextVideoInQueueKey());
            mFragment.startActivity(intent, null);
            mActivity.finish();
        }
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

        switch (playbackState) {
            case ExoPlayer.STATE_BUFFERING:
                // Do nothing.
                break;
            case ExoPlayer.STATE_ENDED:
                Log.i(Tag, "State_Ended");
                mIsMetadataSet = false;
                if (mProgress != null)
                    mProgress.setVisibility(View.VISIBLE);
                if (!skipToNext())
                    mActivity.finish();
                break;
            case ExoPlayer.STATE_IDLE:
                // Do nothing.:
                break;
            case ExoPlayer.STATE_READY:
                // Duration is set here.

                Log.i(Tag, "State_Ready");
                if (!mIsMetadataSet) {
                    if (mProgress != null)
                        mProgress.setVisibility(View.GONE);
                    mCurrentVideoHandler.setTracks(mTrackSelector);
                    mCurrentVideoHandler.updateRecommendations(mActivity, true);
                    mPlaybackUIHandler.updateMetadata();

                    mPlayer.seekTo(mCurrentVideoHandler.getStartPosition());

                    if (mCurrentVideoHandler.getPlaybackStartType() == StartPosition.PlaybackStartType.Ask)
                        ResumeChoiceHandler.askUser(mFragment, mPlayer, mCurrentVideoHandler.getLastViewedPosition(), CHOOSER_TIMEOUT);
                    isVideoLoaded = true;
                    mIsMetadataSet = true;
                }
                playPause(true);
                break;
            default:
                // Do nothing.
                break;
        }
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        String msg = error != null && error.getMessage() != null && !error.getMessage().isEmpty() ? " " + error.getMessage() : "";
        Toast.makeText(mActivity, mActivity.getString(R.string.video_error_unknown_error) + msg, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPositionDiscontinuity() {

    }

    @Override
    public void onLoadError(IOException error) {
        String msg = error != null && !error.getMessage().isEmpty() ? " " + error.getMessage() : "";
        Toast.makeText(mActivity, mActivity.getString(R.string.video_error_load_error) + msg, Toast.LENGTH_LONG).show();
    }

    private class GetFullInfo extends AsyncTask<String, Void, MediaContainer> {

        @Override
        protected MediaContainer doInBackground(String... params) {

            if (params == null || params.length == 0 || params[0] == null)
                return null;
            return mServer.getVideoMetadata(params[0]);
        }

        @Override
        protected void onPostExecute(MediaContainer result) {

            if (result != null)
                playVideo(PlexVideoItem.getItem(result.getVideos().get(0)), true);
        }
    }
}
