package com.monsterbutt.homeview.ui.handler;

import android.app.Activity;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.view.WindowManager;

import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.ui.fragment.PlaybackFragment;

import java.util.List;

import static android.media.session.MediaSession.FLAG_HANDLES_MEDIA_BUTTONS;
import static android.media.session.MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS;

public class MediaSessionHandler {

    private MediaController.Callback mMediaControllerCallback;
    private MediaController mMediaController;
    private final MediaSession mSession;
    private final PlaybackFragment mFragment;
    private VideoPlayerHandler mVideoPlayerHandler;
    private CurrentVideoHandler mCurrentVideoHandler;

    public MediaSessionHandler(PlaybackFragment fragment) {

        mFragment = fragment;
        Activity activity = mFragment.getActivity();
        mSession = new MediaSession(activity, "HomeViewPlayer");
        mSession.setCallback(new MediaSessionCallback());
        mSession.setFlags(FLAG_HANDLES_MEDIA_BUTTONS | FLAG_HANDLES_TRANSPORT_CONTROLS);
        mSession.setActive(true);

        // Set the Activity's MediaController used to invoke transport controls / adjust volume.
        activity.setMediaController(new MediaController(activity, mSession.getSessionToken()));
        mMediaController = activity.getMediaController();
    }

    private Activity getActivity() { return mFragment.getActivity(); }

    public void attach(VideoPlayerHandler videoPlayerHandler,
                         CurrentVideoHandler currentVideoHandler,
                         MediaController.Callback callback) {

        mVideoPlayerHandler = videoPlayerHandler;
        mCurrentVideoHandler = currentVideoHandler;
        mMediaControllerCallback = callback;
        mMediaController.registerCallback(mMediaControllerCallback);
        setPlaybackState(PlaybackState.STATE_NONE);
    }

    public void onDetach() {

        if (mMediaController != null)
            mMediaController.unregisterCallback(mMediaControllerCallback);
    }

    public void onStop() {
        mSession.release();
    }

    public int getPlaybackState() {

        PlaybackState state = mMediaController.getPlaybackState();
        if (state != null)
            return state.getState();
        return PlaybackState.STATE_NONE;
    }

    public void setPlaybackState(int state) {
        long currPosition = mVideoPlayerHandler.getCurrentPosition();

        PlaybackState.Builder stateBuilder = new PlaybackState.Builder().setActions(getAvailableActions(state));
        stateBuilder.setState(state, currPosition, 1.0f);
        mSession.setPlaybackState(stateBuilder.build());
    }

    public void setMetadata(MediaMetadata data) { mSession.setMetadata(data); }

    public void setQueue(List<MediaSession.QueueItem> queue, String title) {
        mSession.setQueue(queue);
        mSession.setQueueTitle(title);
    }

    private class MediaSessionCallback extends MediaSession.Callback {

        @Override
        public void onPlay() { mVideoPlayerHandler.playPause(true); }

        @Override
        // This method should play any media item regardless of the Queue.
        public void onPlayFromMediaId(String mediaId, Bundle extras) {

            mCurrentVideoHandler.createQueue(PlexVideoItem.RatingKeyToKey(mediaId));
        }

        @Override
        public void onPause() {
            mVideoPlayerHandler.playPause(false);
        }

        @Override
        public void onSkipToNext() {

            PlexVideoItem video = mCurrentVideoHandler.getVideo();
            if (video.hasChapters()) {

                long pos = video.getNextChapterStart(mVideoPlayerHandler.getCurrentPosition());
                if (pos != PlexVideoItem.BAD_CHAPTER_START) {

                    int prevState = getPlaybackState();
                    setPlaybackState(PlaybackState.STATE_FAST_FORWARDING);
                    mVideoPlayerHandler.setPosition(pos);
                    setPlaybackState(prevState);
                    return;
                }
            }
            if (!mCurrentVideoHandler.playNextInQueue(getActivity()))
                mFragment.getActivity().onBackPressed(); // Return to details presenter.
        }

        @Override
        public void onSkipToPrevious() {

            PlexVideoItem video = mCurrentVideoHandler.getVideo();
            if (video.hasChapters()) {

                long pos = video.getPreviousChapterStart(mVideoPlayerHandler.getCurrentPosition());
                if (pos != PlexVideoItem.BAD_CHAPTER_START) {

                    int prevState = getPlaybackState();
                    setPlaybackState(PlaybackState.STATE_REWINDING);
                    mVideoPlayerHandler.setPosition(pos);
                    setPlaybackState(prevState);
                    return;
                }
            }
            else if (PlexVideoItem.START_CHAPTER_THRESHOLD < mVideoPlayerHandler.getCurrentPosition()) {

                int prevState = getPlaybackState();
                setPlaybackState(PlaybackState.STATE_REWINDING);
                mVideoPlayerHandler.setPosition(0);
                setPlaybackState(prevState);
                return;
            }

            mCurrentVideoHandler.playPreviousInQueue(getActivity());
        }

        @Override
        public void onFastForward() {
            mVideoPlayerHandler.fastForward();
        }

        @Override
        public void onStop() {

            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            getActivity().finish();
        }

        @Override
        public void onRewind() {
            mVideoPlayerHandler.rewind();
        }

        @Override
        public void onSeekTo(long position) {
            mVideoPlayerHandler.setPosition(position);
        }
    }

    private long getAvailableActions(int nextState) {
        long actions = PlaybackState.ACTION_PLAY |
                PlaybackState.ACTION_PLAY_FROM_MEDIA_ID |
                PlaybackState.ACTION_PLAY_FROM_SEARCH |
                PlaybackState.ACTION_SKIP_TO_NEXT |
                PlaybackState.ACTION_SKIP_TO_PREVIOUS |
                PlaybackState.ACTION_PAUSE;

        if (nextState == PlaybackState.STATE_PLAYING) {
            actions |= PlaybackState.ACTION_PAUSE;
        }

        return actions;
    }
}
