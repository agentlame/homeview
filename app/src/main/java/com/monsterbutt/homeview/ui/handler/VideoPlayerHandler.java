package com.monsterbutt.homeview.ui.handler;

import android.media.MediaMetadata;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v17.leanback.app.PlaybackOverlayFragment;
import android.support.v17.leanback.widget.ListRow;
import android.util.Log;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.exoplayer.AspectRatioFrameLayout;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.util.Util;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.player.ExtractorRendererBuilder;
import com.monsterbutt.homeview.player.StartPosition;
import com.monsterbutt.homeview.player.VideoPlayer;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.Episode;
import com.monsterbutt.homeview.plex.media.Movie;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.settings.SettingsManager;
import com.monsterbutt.homeview.ui.UILifecycleManager;
import com.monsterbutt.homeview.ui.android.HomeViewActivity;
import com.monsterbutt.homeview.ui.fragment.PlaybackFragment;

import us.nineworlds.plex.rest.model.impl.MediaContainer;


public class VideoPlayerHandler implements VideoPlayer.Listener, SurfaceHolder.Callback,
                                            UILifecycleManager.LifecycleListener,
                                            PlaybackOverlayFragment.InputEventHandler {

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
    private final SubtitleHandler mSubtitleHandler;
    private final HomeViewActivity mActivity;
    private final PlexServer mServer;
    private final AspectRatioFrameLayout mVideoFrame;
    private VideoPlayer mPlayer;

    private long mLastRewind = 0;
    private long mLastForward = 0;

    private Surface mCacheSurface = null;

    private boolean mCheckForPIPChanged = false;
    private boolean mGuiShowing = false;
    private boolean mIsMetadataSet = false;

    public VideoPlayerHandler(PlaybackFragment fragment, PlexServer server,
                              MediaSessionHandler mediaSessionHandler,
                              CurrentVideoHandler currentVideoHandler,
                              SubtitleHandler subtitleHandler,
                              AspectRatioFrameLayout videoFrame) {

        mActivity = (HomeViewActivity) fragment.getActivity();
        mServer = server;
        mMediaSessionHandler = mediaSessionHandler;
        mCurrentVideoHandler = currentVideoHandler;
        mSubtitleHandler = subtitleHandler;
        mVideoFrame = videoFrame;

        SurfaceView surfaceView = (SurfaceView) mActivity.findViewById(R.id.surface_view);
        surfaceView.getHolder().addCallback(this);
        fragment.setFadeCompleteListener(new PlaybackOverlayFragment.OnFadeCompleteListener() {
            @Override
            public void onFadeInComplete() { setGuiShowing(true); }
            @Override
            public void onFadeOutComplete() { setGuiShowing(false); }
        });
    }

    private boolean isGuiShowing() {
        boolean ret;
        synchronized (this) {
            ret = mGuiShowing;
        }
        return ret;
    }

    private void setGuiShowing(boolean val) {
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
        if (duration != ExoPlayer.UNKNOWN_TIME) {

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
            if (loc < mPlayer.getDuration()) {
                setPosition(loc);
                mMediaSessionHandler.setPlaybackState(prevState);
                return true;
            }
        }
        return false;
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        if (mPlayer != null) {
            mPlayer.setSurface(holder.getSurface());
        }
        else
            mCacheSurface = holder.getSurface();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

        if (mPlayer != null) {
            mPlayer.blockingClearSurface();
        }
    }

    public void pipModeChanged(boolean isInPictureInPictureMode) {

        if (!isInPictureInPictureMode)
            mCheckForPIPChanged = true;
    }

    @Override
    public void onResume() {

        boolean forcePlay = mCheckForPIPChanged && mCurrentVideoHandler.checkPassedVideo();
        PlexVideoItem video = mCurrentVideoHandler.getVideo();
        if ((mPlayer == null || forcePlay) && video != null) {

            if (video.selectedHasMissingData())
                new GetFullInfo().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, video.getKey());
            else
                playVideo(video, mAutoPlayExtras);
        }
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

        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }

    @Override
    public void onDestroyed() { }

    public boolean isPlaying() {
        return mPlayer != null && mPlayer.getPlayerControl().isPlaying();
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
        return ExoPlayer.UNKNOWN_TIME;
    }

    private VideoPlayer.RendererBuilder getRendererBuilder() {
        String userAgent = Util.getUserAgent(mActivity, "ExoVideoPlayer");

        Uri contentUri = Uri.parse(mCurrentVideoHandler.getPath());
        int contentType = Util.inferContentType(contentUri.getLastPathSegment());
        switch (contentType) {
            case Util.TYPE_OTHER: {
                return new ExtractorRendererBuilder(mActivity, userAgent, contentUri);
            }
            default: {
                throw new IllegalStateException("Unsupported type: " + contentType);
            }
        }
    }

    private void preparePlayer(boolean playWhenReady) {

        if (mPlayer == null) {

            mPlayer = new VideoPlayer(getRendererBuilder());
            mPlayer.setCaptionListener(mSubtitleHandler);
            mPlayer.addListener(this);
            if (mCacheSurface != null)
                mPlayer.setSurface(mCacheSurface);
            mCacheSurface = null;
        }
        else {

            mPlayer.stop();
            mPlayer.setRendererBuilder(getRendererBuilder());
        }

        mPlayer.seekTo(mCurrentVideoHandler.getStartPosition());
        mPlayer.prepare(mCurrentVideoHandler.getVideo());

        if (mCurrentVideoHandler.getPlaybackStartType() == StartPosition.PlaybackStartType.Ask)
            ResumeChoiceHandler.askUser(mActivity, mPlayer, mCurrentVideoHandler.getLastViewedPosition(), CHOOSER_TIMEOUT);
        mPlayer.setPlayWhenReady(playWhenReady);
    }

    public void playPause(boolean doPlay) {

        if (mPlayer == null) {
            mMediaSessionHandler.setPlaybackState(PlaybackState.STATE_NONE);
            return;
        }

        if (doPlay && getPlaybackState() != PlaybackState.STATE_PLAYING) {

            mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            mPlayer.getPlayerControl().start();
            mMediaSessionHandler.setPlaybackState(PlaybackState.STATE_PLAYING);
        } else {
            mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            mPlayer.getPlayerControl().pause();
            mMediaSessionHandler.setPlaybackState(PlaybackState.STATE_PAUSED);
        }
    }

    @Override
    public void onError(Exception e) {

        Toast.makeText(mActivity, "An error has occured", Toast.LENGTH_LONG).show();
        Log.e("VideoPlaybackError", "An error occurred: " + e);
    }

    @Override
    public void onVideoSizeChanged(final int width, final int height, int unappliedRotationDegrees,
                                   float pixelWidthHeightRatio) {
        mVideoFrame.setAspectRatio(height == 0 ? 1 : (width * pixelWidthHeightRatio) / height);
    }

    @Override
    public boolean handleInputEvent(InputEvent event) {

        if (isGuiShowing() || !(event instanceof KeyEvent))
            return false;

        switch (((KeyEvent) event).getKeyCode()) {

            case KeyEvent.KEYCODE_DPAD_LEFT:
                rewind();
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                fastForward();
                break;
            default:
                break;
        }

        return false;
    }

    @Override
    public void onStateChanged(boolean playWhenReady, int playbackState) {
        switch (playbackState) {
            case ExoPlayer.STATE_BUFFERING:
                // Do nothing.
                break;
            case ExoPlayer.STATE_ENDED:
                mIsMetadataSet = false;
                if (mCurrentVideoHandler.hasNext() && shouldPlayNext())
                    mActivity.getMediaController().getTransportControls().skipToNext();
                else
                    mActivity.finish();
                break;
            case ExoPlayer.STATE_IDLE:
                // Do nothing.
                break;
            case ExoPlayer.STATE_PREPARING:
                mIsMetadataSet = false;
                mCurrentVideoHandler.setTracks(mPlayer);
                break;
            case ExoPlayer.STATE_READY:

                // Duration is set here.
                if (!mIsMetadataSet) {
                    mCurrentVideoHandler.updateRecommendations(mActivity, true);

                    mPlaybackUIHandler.updateMetadata();
                    mIsMetadataSet = true;
                }
                break;
            default:
                // Do nothing.
                break;
        }
    }

    private void playVideo(PlexVideoItem video, Bundle extras) {

        mCurrentVideoHandler.setVideo(video, mSubtitleHandler);
        if (video.shouldUpdateStatusOnPlayback())
            mCurrentVideoHandler.updateProgressTask();
        preparePlayer(true);
        mPlaybackUIHandler.setupVideoForPlayback();
        mMediaSessionHandler.setPlaybackState(PlaybackState.STATE_PAUSED);
        playPause(extras.getBoolean(AUTO_PLAY));
    }

    public int getPlaybackState() { return mMediaSessionHandler.getPlaybackState(); }

    public PlexVideoItem getCurrentVideo() { return mCurrentVideoHandler.getVideo(); }

    public void setMetadata(MediaMetadata data) { mMediaSessionHandler.setMetadata(data); }

    public void playVideo(PlexVideoItem video) {

        if (video != null) {

            if (video.selectedHasMissingData())
                new GetFullInfo().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, video.getKey());
            else
                playVideo(video, mAutoPlayExtras);
        }
    }

    public ListRow getCodecRowForVideo() { return mCurrentVideoHandler.getCodecRowForVideo(mPlayer); }

    public ListRow getExtrasRowForVideo() { return mCurrentVideoHandler.getExtrasRowForVideo(); }

    public void setUIHandler(PlaybackUIHandler playbackUIHandler) { mPlaybackUIHandler = playbackUIHandler; }

    private boolean shouldPlayNext() {

        PlexVideoItem current = mCurrentVideoHandler.getVideo();
        String playNext = SettingsManager.getInstance(mActivity.getApplicationContext()).getString("preferences_playback_playnext");
        return (playNext.equals("always")
            || (playNext.equals("shows") && current instanceof Episode)
            || (playNext.equals("movies") && current instanceof Movie));
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
                playVideo(PlexVideoItem.getItem(result.getVideos().get(0)), mAutoPlayExtras);
        }
    }
}
