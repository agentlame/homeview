package com.monsterbutt.homeview.player;


import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.os.Build;
import android.os.Handler;
import android.support.v17.leanback.app.PlaybackControlGlue;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ControlButtonPresenterSelector;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackControlsRowPresenter;
import android.support.v17.leanback.widget.PresenterSelector;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.SparseArrayObjectAdapter;
import android.view.KeyEvent;

import com.google.android.exoplayer2.C;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.model.Video;
import com.monsterbutt.homeview.presenters.PlaybackDetailsPresenter;
import com.monsterbutt.homeview.ui.fragment.PlaybackFragment;
import com.monsterbutt.homeview.ui.handler.VideoPlayerHandler;


public class PlaybackControlHelper extends PlaybackControlGlue {
    private static final int[] SEEK_SPEEDS = {0}; // A single seek speed for fast-forward / rewind.
    private static final int DEFAULT_UPDATE_PERIOD = 500;
    Drawable mMediaArt;
    private boolean mIsPlaying;
    private int mSpeed;
    private PlaybackFragment mFragment;
    private VideoPlayerHandler mPlaybackHandler;
    private MediaController.TransportControls mTransportControls;
    private PlaybackControlsRow.FastForwardAction mFastForwardAction;
    private PlaybackControlsRow.RewindAction mRewindAction;

    private boolean mMetadataSet = false;

    private Handler mHandler = new Handler();
    private Runnable mUpdateProgressRunnable = new Runnable() {
        @Override
        public void run() {
            int totalTime = getControlsRow().getTotalTime();
            int currentTime = getCurrentPosition();
            getControlsRow().setCurrentTime(currentTime);

            int progress = (int) mPlaybackHandler.getBufferedPosition();
            getControlsRow().setBufferedProgress(progress);

            if (mProgressCallback != null)
                mProgressCallback.progressUpdate(mPlaybackHandler.getPlaybackState(), currentTime);

            if (totalTime > 0 && totalTime <= currentTime) {
                if (mHandler != null && mUpdateProgressRunnable != null)
                    mHandler.removeCallbacks(mUpdateProgressRunnable);
            } else {
                mHandler.postDelayed(this, getUpdatePeriod());
            }
        }
    };

    private Video mVideo = null;
    private ProgressUpdateCallback mProgressCallback;

    private PIPAction mPIPAction;

    public interface ProgressUpdateCallback {
        void progressUpdate(int playbackState, long timeInMs);
    }

    public PlaybackControlHelper(Context context, PlaybackFragment fragment, Video video, ProgressUpdateCallback callback) {

        super(context, fragment, SEEK_SPEEDS);

        mPIPAction = new PIPAction(context);
        mFragment = fragment;
        mPlaybackHandler = fragment.getPlaybackHandler();
        fragment.setInputEventHandler(mPlaybackHandler);
        mVideo = video;
        mProgressCallback = callback;

        mTransportControls = mFragment.getActivity().getMediaController().getTransportControls();
        mIsPlaying = true;
        mSpeed = PLAYBACK_SPEED_NORMAL;
    }

    public void onStop() {
        enableProgressUpdating(false);
    }

    public boolean isMetadataSet() {
        boolean ret;
        synchronized (this) {
            ret = mMetadataSet;
        }
        return ret;
    }

    public void setMetadataSet(boolean isSet) {
        synchronized (this) {
            mMetadataSet = isSet;

        }
    }

    private PlaybackControlsRowPresenter makeControlsRowAndPresenter() {
        PlaybackControlsRow controlsRow = new PlaybackControlsRow(this);
        setControlsRow(controlsRow);

        return new PlaybackControlsRowPresenter(new PlaybackDetailsPresenter()) {
            @Override
            protected void onBindRowViewHolder(RowPresenter.ViewHolder vh, Object item) {
                super.onBindRowViewHolder(vh, item);
                vh.setOnKeyListener(PlaybackControlHelper.this);
            }

            @Override
            protected void onUnbindRowViewHolder(RowPresenter.ViewHolder vh) {
                super.onUnbindRowViewHolder(vh);
                vh.setOnKeyListener(null);
            }
        };
    }

    @TargetApi(24)
    @Override
    public PlaybackControlsRowPresenter createControlsRowAndPresenter() {
        PlaybackControlsRowPresenter presenter = makeControlsRowAndPresenter();

        ArrayObjectAdapter adapter = new ArrayObjectAdapter(new ControlButtonPresenterSelector());
        getControlsRow().setSecondaryActionsAdapter(adapter);

        mFastForwardAction = (PlaybackControlsRow.FastForwardAction) getPrimaryActionsAdapter()
                .lookup(ACTION_FAST_FORWARD);
        mRewindAction = (PlaybackControlsRow.RewindAction) getPrimaryActionsAdapter()
                .lookup(ACTION_REWIND);

        presenter.setOnActionClickedListener(new OnActionClickedListener() {
            @Override
            public void onActionClicked(Action action) {
                if (action == mPIPAction)
                    mFragment.getActivity().enterPictureInPictureMode();
                else
                    dispatchAction(action);
            }
        });

        return presenter;
    }

    @Override
    public void enableProgressUpdating(boolean enable) {
        mHandler.removeCallbacks(mUpdateProgressRunnable);
        if (enable)
            mHandler.post(mUpdateProgressRunnable);
    }

    @Override
    public int getUpdatePeriod() {
        return DEFAULT_UPDATE_PERIOD;
    }

    @Override
    public boolean hasValidMedia() {
        return true; // hack for missing control rows
    }

    @Override
    public boolean isMediaPlaying() {
        return mIsPlaying;
    }

    @Override
    public CharSequence getMediaTitle() {

        if (!isMetadataSet())
            return "";
        return mVideo != null ? mVideo.title : "";
    }

    @Override
    public CharSequence getMediaSubtitle() {

        if (!isMetadataSet())
            return "";
        return mVideo != null ? mVideo.description : "";
    }

    @Override
    public int getMediaDuration() {
        long duration = mPlaybackHandler.getDuration();
        if (duration == C.TIME_UNSET || !isMetadataSet())
            return 0;
        return (int) duration;
    }

    @Override
    public Drawable getMediaArt() {

        if (!isMetadataSet())
            return null;
        return mMediaArt;
    }

    @Override
    public long getSupportedActions() {
        return ACTION_PLAY_PAUSE | ACTION_FAST_FORWARD | ACTION_REWIND | ACTION_SKIP_TO_PREVIOUS |
                ACTION_SKIP_TO_NEXT | ACTION_CUSTOM_LEFT_FIRST ;
    }

    @Override
    public int getCurrentSpeedId() {
        return mSpeed;
    }

    private long getPlaybackPosition() {
        long position = mPlaybackHandler.getCurrentPosition();
        if (position == C.TIME_UNSET || !isMetadataSet())
            return 0;
        return position;
    }

    @Override
    public int getCurrentPosition() { return (int) getPlaybackPosition();
    }

    @Override
    protected void startPlayback(int speed) {
        if (mSpeed == speed) {
            return;
        }

        mSpeed = speed;
        mIsPlaying = true;
        mTransportControls.play();
    }

    @Override
    protected void pausePlayback() {
        mSpeed = PlaybackControlGlue.PLAYBACK_SPEED_PAUSED;
        mIsPlaying = false;
        mTransportControls.pause();
    }

    @Override
    protected void skipToNext() {
        mSpeed = PlaybackControlGlue.PLAYBACK_SPEED_NORMAL;
        mIsPlaying = true;
        mTransportControls.skipToNext();
    }

    @Override
    protected void skipToPrevious() {
        mSpeed = PlaybackControlGlue.PLAYBACK_SPEED_NORMAL;
        mIsPlaying = true;
        mTransportControls.skipToPrevious();
    }

    @Override
    protected void onRowChanged(PlaybackControlsRow row) {
        // Do nothing.
    }

    @Override
    public void onMetadataChanged() {
        MediaMetadata metadata = mFragment.getActivity().getMediaController().getMetadata();
        if (metadata != null) {
            mVideo = new Video.VideoBuilder().buildFromMediaDesc(metadata.getDescription());
            int duration = (int) metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
            getControlsRow().setTotalTime(duration);
            mMediaArt = new BitmapDrawable(mFragment.getResources(),
                    metadata.getBitmap(MediaMetadata.METADATA_KEY_ART));
        }
        super.onMetadataChanged();
    }

    public void dispatchAction(Action action) {
       /* if (action instanceof PlaybackControlsRow.MultiAction) {
            PlaybackControlsRow.MultiAction multiAction = (PlaybackControlsRow.MultiAction) action;
            multiAction.nextIndex();
            notifyActionChanged(multiAction);
        }*/

        if (action == mFastForwardAction) {
            mTransportControls.fastForward();
        } else if (action == mRewindAction) {
            mTransportControls.rewind();
        } else {
            super.onActionClicked(action);
        }
    }

    private SparseArrayObjectAdapter getPrimaryActionsAdapter() {
        return (SparseArrayObjectAdapter) getControlsRow().getPrimaryActionsAdapter();
    }

    @Override
    protected SparseArrayObjectAdapter createPrimaryActionsAdapter(
            PresenterSelector presenterSelector) {
        SparseArrayObjectAdapter adapter = new SparseArrayObjectAdapter(presenterSelector);
        if (Build.VERSION.SDK_INT > 23)
            adapter.set(PlaybackControlGlue.ACTION_CUSTOM_LEFT_FIRST, mPIPAction);

        return adapter;
    }

    public static class PIPAction extends Action {

        public PIPAction(Context context) {
            super(R.id.lb_pip);
            setIcon(context.getDrawable(R.drawable.ic_picture_in_picture_white_48dp));
            setLabel1(context.getString(R.string.pip));
            addKeyCode(KeyEvent.KEYCODE_TAB);
        }
    }
}
