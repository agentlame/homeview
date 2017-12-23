package com.monsterbutt.homeview.player;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.support.v17.leanback.media.PlaybackGlueHost;
import android.support.v17.leanback.media.PlayerAdapter;
import android.support.v17.leanback.media.SurfaceHolderGlueHost;

import android.view.SurfaceHolder;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.TextOutput;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.SubtitleView;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.monsterbutt.homeview.player.track.TrackSelector;

import java.util.List;

/**
 * This implementation extends the {@link PlayerAdapter} with a {@link SimpleExoPlayer}.
 */
public class ExoPlayerAdapter extends PlayerAdapter implements Player.EventListener{

  private Context mContext;
  private final SimpleExoPlayer mPlayer;
  private SurfaceHolderGlueHost mSurfaceHolderGlueHost;
  private final Runnable mRunnable = new Runnable() {
    @Override
    public void run() {
      getCallback().onCurrentPositionChanged(ExoPlayerAdapter.this);
      getCallback().onBufferedPositionChanged(ExoPlayerAdapter.this);
      mHandler.postDelayed(this, getUpdatePeriod());
    }
  };
  private final SimpleExoPlayer.VideoListener mVideoListener = new SimpleExoPlayer.VideoListener() {
    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
    float pixelWidthHeightRatio) {
      getCallback().onVideoSizeChanged(ExoPlayerAdapter.this, width, height);
    }

    @Override
    public void onRenderedFirstFrame() {
    }
  };
  private final Handler mHandler = new Handler();
  private boolean mInitialized = false;
  private Uri mMediaSourceUri = null;
  private boolean mHasDisplay;
  private boolean mBufferingStart;
  private @C.StreamType int mAudioStreamType;
  private final SubtitleView mSubtitleView;

  /**
   * Constructor.
   */
  ExoPlayerAdapter(Context context, RenderersFactory factory, TrackSelector selector, SubtitleView view) {
    mContext = context;
    mPlayer = ExoPlayerFactory.newSimpleInstance(factory == null ? new DefaultRenderersFactory(context) : factory,
     selector == null ? new DefaultTrackSelector() : selector,
     new DefaultLoadControl());
    mPlayer.addListener(this);
    mSubtitleView = view;
    if (mSubtitleView != null) {

      mPlayer.addTextOutput(new TextOutput() {
        @Override
        public void onCues(List<Cue> cues) {
          mSubtitleView.onCues(cues);
        }
      });
    }
  }

  @Override
  public void onAttachedToHost(PlaybackGlueHost host) {
    if (host instanceof SurfaceHolderGlueHost) {
      mSurfaceHolderGlueHost = ((SurfaceHolderGlueHost) host);
      mSurfaceHolderGlueHost.setSurfaceHolderCallback(new VideoPlayerSurfaceHolderCallback());
    }
  }

  /**
   * Will reset the {@link ExoPlayer} and the glue such that a new file can be played. You are
   * not required to call this method before playing the first file. However you have to call it
   * before playing a second one.
   */
  public void reset() {
    changeToUninitialized();
    mPlayer.stop();
  }

  private void changeToUninitialized() {
    if (mInitialized) {
      mInitialized = false;
      notifyBufferingStartEnd();
      if (mHasDisplay) {
        getCallback().onPreparedStateChanged(ExoPlayerAdapter.this);
      }
    }
  }

  /**
   * Notify the state of buffering. For example, an app may enable/disable a loading figure
   * according to the state of buffering.
   */
  private void notifyBufferingStartEnd() {
    getCallback().onBufferingStateChanged(ExoPlayerAdapter.this,
     mBufferingStart || !mInitialized);
  }

  /**
   * Release internal {@link SimpleExoPlayer}. Should not use the object after call release().
   */
  public void release() {
    changeToUninitialized();
    mHasDisplay = false;
    mPlayer.release();
  }

  @Override
  public void onDetachedFromHost() {
    if (mSurfaceHolderGlueHost != null) {
      mSurfaceHolderGlueHost.setSurfaceHolderCallback(null);
      mSurfaceHolderGlueHost = null;
    }
    reset();
    release();
  }

  /**
   * @see SimpleExoPlayer#setVideoSurfaceHolder(SurfaceHolder)
   */
  void setDisplay(SurfaceHolder surfaceHolder) {
    boolean hadDisplay = mHasDisplay;
    mHasDisplay = surfaceHolder != null;
    if (hadDisplay == mHasDisplay) {
      return;
    }

    mPlayer.setVideoSurfaceHolder(surfaceHolder);
    if (mHasDisplay) {
      if (mInitialized) {
        getCallback().onPreparedStateChanged(ExoPlayerAdapter.this);
      }
    } else {
      if (mInitialized) {
        getCallback().onPreparedStateChanged(ExoPlayerAdapter.this);
      }
    }
  }

  @Override
  public void setProgressUpdatingEnabled(final boolean enabled) {
    mHandler.removeCallbacks(mRunnable);
    if (!enabled) {
      return;
    }
    mHandler.postDelayed(mRunnable, getUpdatePeriod());
  }

  private int getUpdatePeriod() {
    return 16;
  }

  @Override
  public boolean isPlaying() {
    boolean exoPlayerIsPlaying = mPlayer.getPlaybackState() == Player.STATE_READY
     && mPlayer.getPlayWhenReady();
    return mInitialized && exoPlayerIsPlaying;
  }

  @Override
  public long getDuration() {
    return mInitialized ? mPlayer.getDuration() : -1;
  }

  @Override
  public long getCurrentPosition() {
    return mInitialized ? mPlayer.getCurrentPosition() : -1;
  }


  @Override
  public void play() {
    if (!mInitialized || isPlaying()) {
      return;
    }

    mPlayer.setPlayWhenReady(true);
    getCallback().onPlayStateChanged(ExoPlayerAdapter.this);
    getCallback().onCurrentPositionChanged(ExoPlayerAdapter.this);
  }

  @Override
  public void pause() {
    if (isPlaying()) {
      mPlayer.setPlayWhenReady(false);
      getCallback().onPlayStateChanged(ExoPlayerAdapter.this);
    }
  }

  @Override
  public void seekTo(long newPosition) {
    if (!mInitialized || !mPlayer.isCurrentWindowSeekable()) {
      return;
    }
    mPlayer.seekTo(newPosition);
  }

  @Override
  public long getBufferedPosition() {
    return mPlayer.getBufferedPosition();
  }

  public Context getContext() {
    return mContext;
  }

  /**
   * Sets the media source of the player with a given URI.
   *
   * otherwise.
   * @see ExoPlayer#prepare(MediaSource)
   */
  void setDataSource(Uri uri) {
    if (mMediaSourceUri != null ? mMediaSourceUri.equals(uri) : uri == null) {
      return;
    }
    mMediaSourceUri = uri;
    prepareMediaForPlaying();
  }


  public void setAudioStreamType(@C.StreamType int audioStreamType) {
    mAudioStreamType = audioStreamType;
  }

  /**
   * Set {@link MediaSource} for {@link SimpleExoPlayer}. An app may override this method in order
   * to use different {@link MediaSource}.
   * @param uri The url of media source
   * @return MediaSource for the player
   */
  public MediaSource onCreateMediaSource(Uri uri) {
    String userAgent = Util.getUserAgent(mContext, "ExoPlayerAdapter");
    return new ExtractorMediaSource(uri,
     new DefaultDataSourceFactory(mContext, userAgent),
     new DefaultExtractorsFactory(),
     null,
     null);
  }

  private void prepareMediaForPlaying() {
    reset();
    if (mMediaSourceUri != null) {
      MediaSource mediaSource = onCreateMediaSource(mMediaSourceUri);
      //mPlayer.setPlayWhenReady(false);
      mPlayer.prepare(mediaSource);
    } else {
      return;
    }

    @C.AudioUsage int usage = Util.getAudioUsageForStreamType(mAudioStreamType);
    @C.AudioContentType int contentType = Util.getAudioContentTypeForStreamType(mAudioStreamType);
    mPlayer.setAudioAttributes(new AudioAttributes.Builder().setUsage(usage).setContentType(contentType).build());
    mPlayer.removeVideoListener(mVideoListener);
    mPlayer.addVideoListener(mVideoListener);
    notifyBufferingStartEnd();
    getCallback().onPlayStateChanged(ExoPlayerAdapter.this);
  }

  /**
   * @return True if ExoPlayer is ready and got a SurfaceHolder if
   * {@link PlaybackGlueHost} provides SurfaceHolder.
   */
  @Override
  public boolean isPrepared() {
    return mInitialized && (mSurfaceHolderGlueHost == null || mHasDisplay);
  }

  /**
   * Implements {@link SurfaceHolder.Callback} that can then be set on the
   * {@link PlaybackGlueHost}.
   */
  class VideoPlayerSurfaceHolderCallback implements SurfaceHolder.Callback {
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
      setDisplay(surfaceHolder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
      setDisplay(null);
    }
  }

  // ExoPlayer Event Listeners

  @Override
  public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
    mBufferingStart = false;
    if (playbackState == Player.STATE_READY && !mInitialized) {
      mInitialized = true;
      if (mSurfaceHolderGlueHost == null || mHasDisplay) {
        getCallback().onPreparedStateChanged(ExoPlayerAdapter.this);
      }
    } else if (playbackState == Player.STATE_BUFFERING) {
      mBufferingStart = true;
    } else if (playbackState == Player.STATE_ENDED) {
      getCallback().onPlayStateChanged(ExoPlayerAdapter.this);
      getCallback().onPlayCompleted(ExoPlayerAdapter.this);
    }
    notifyBufferingStartEnd();
  }

  @Override
  public void onRepeatModeChanged(int repeatMode) {

  }

  @Override
  public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

  }

  @Override
  public void onPlayerError(ExoPlaybackException error) {
    getCallback().onError(ExoPlayerAdapter.this, error.type,
     mContext.getString(android.support.v17.leanback.R.string.lb_media_player_error,
      error.type,
      error.rendererIndex));
  }

  @Override
  public void onPositionDiscontinuity(int reason) {

  }

  @Override
  public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

  }

  @Override
  public void onSeekProcessed() {

  }

  @Override
  public void onLoadingChanged(boolean isLoading) {
  }

  @Override
  public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {

  }

  @Override
  public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
  }

}