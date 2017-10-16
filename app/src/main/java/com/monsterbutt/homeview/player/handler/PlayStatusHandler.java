package com.monsterbutt.homeview.player.handler;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v17.leanback.app.VideoFragment;
import android.support.v17.leanback.media.PlaybackGlue;
import android.support.v17.leanback.media.PlaybackTransportControlGlue;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.player.notifier.SeekOccuredNotifier;
import com.monsterbutt.homeview.player.track.MediaTrackSelector;
import com.monsterbutt.homeview.player.notifier.VideoChangedNotifier;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.plex.media.Stream;
import com.monsterbutt.homeview.plex.media.VideoFormat;
import com.monsterbutt.homeview.plex.tasks.VideoProgressTask;
import com.monsterbutt.homeview.services.UpdateRecommendationsService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import us.nineworlds.plex.rest.model.impl.Media;

public class PlayStatusHandler extends PlaybackGlue.PlayerCallback
 implements VideoChangedNotifier.Observer, SeekOccuredNotifier.Observer {

  private final Handler mHandler = new Handler();
  private final VideoFragment mFragment;
  private final PlexServer mServer;
  private final PlaybackTransportControlGlue mGlue;

  private ServerStatusUpdate mServerStatus = null;
  private RemainingTimeUI mRemainingTime = null;

  public PlayStatusHandler(VideoFragment fragment, PlexServer server, PlaybackTransportControlGlue glue,
                           VideoChangedNotifier videoChangedNotifier, SeekOccuredNotifier seekOccuredNotifier) {
    mFragment = fragment;
    mServer = server;
    mGlue = glue;
    glue.addPlayerCallback(this);
    videoChangedNotifier.register(this);
    seekOccuredNotifier.register(this);
  }

  @Override
  public void notify(PlexVideoItem item, MediaTrackSelector tracks, boolean usesDefaultTracks,
                     StartPositionHandler startPosition) {

    if (item == null) {

      if (mServerStatus != null)
        mServerStatus.updateRecommendations(false);
      release();
      return;
    }
    synchronized (this) {
      if (mServerStatus != null)
        mServerStatus.stop();
      mServerStatus = new ServerStatusUpdate(mHandler, item, mServer, mGlue);
      if (mRemainingTime != null)
        mRemainingTime.stop();
      mRemainingTime = new RemainingTimeUI(mHandler, mFragment.getActivity(), mGlue, item.getDuration());
      mRemainingTime.updateRemainingTime();
    }
    updateIcons(mFragment.getActivity(), item, tracks, mServer);
  }

  @Override
  public synchronized void onPlayStateChanged(PlaybackGlue glue) {

    if (mRemainingTime != null)
      mRemainingTime.stop();

    if (glue.isPlaying()) {
      if (mServerStatus != null)
        mServerStatus.start();
      if (mRemainingTime != null)
        mRemainingTime.updateRemainingTime();
    }
    else {
      if (mServerStatus != null)
        mServerStatus.stop();
      if (mRemainingTime != null)
        mRemainingTime.start();
    }
  }

  @Override
  public void onPlayCompleted(PlaybackGlue glue) {
    if (mServerStatus != null)
     mServerStatus.setItemWatched();
    release();
  }


  @Override
  public void seekOccured(int reason) {
    if (mRemainingTime != null)
      mRemainingTime.updateRemainingTime();
    if (mServerStatus != null && mGlue.isPlaying())
      mServerStatus.onSeek();
  }

  public synchronized void release() {
    if (mServerStatus != null)
      mServerStatus.stop();
    if (mRemainingTime != null)
      mRemainingTime.stop();
  }


  private void updateIcons(Activity activity, PlexVideoItem item, MediaTrackSelector tracks,
                           PlexServer server) {

    ImageView resolution = activity.findViewById(R.id.resolutionImage);
    ImageView video = activity.findViewById(R.id.videoImage);
    ImageView studio = activity.findViewById(R.id.studio);
    ImageView frameRate = activity.findViewById(R.id.framerateImage);
    ImageView audio = activity.findViewById(R.id.audioImage);
    ImageView channels = activity.findViewById(R.id.channelsImage);
    ImageView rating = activity.findViewById(R.id.ratingImage);

    loadImage(activity, studio, item.getDetailStudioPath(server));
    loadImage(activity, rating, item.getDetailRatingPath(server));

    Media media = item.getMedia() != null ? item.getMedia().get(0) : null;
    if (media != null)
      loadImage(activity, frameRate, server.makeServerURLForCodec(VideoFormat.VideoFrameRate, media.getVideoFrameRate()));
    else
      frameRate.setVisibility(View.INVISIBLE);

    if (tracks != null) {
      Stream videoStream = tracks.getSelectedTrack(Stream.Video_Stream);
      if (videoStream != null) {
        loadImage(activity, video, server.makeServerURLForCodec(VideoFormat.VideoCodec, videoStream.getCodec()));
        loadImage(activity, resolution, media != null ?
        server.makeServerURLForCodec(VideoFormat.VideoResolution, media.getVideoResolution()) : "");
      } else {
        video.setVisibility(View.INVISIBLE);
        resolution.setVisibility(View.INVISIBLE);
      }

      Stream audioStream = tracks.getSelectedTrack(Stream.Audio_Stream);
      if (audioStream != null) {
        loadImage(activity, audio, server.makeServerURLForCodec(Stream.AudioCodec, audioStream.getCodecAndProfile()));
        loadImage(activity, channels, server.makeServerURLForCodec(Stream.AudioChannels, audioStream.getChannels()));
      }
      else {
        audio.setVisibility(View.INVISIBLE);
        channels.setVisibility(View.INVISIBLE);
      }
    }
  }

  private static void loadImage(Activity activity, ImageView view, String path) {

    if (TextUtils.isEmpty(path))
      view.setVisibility(View.INVISIBLE);
    else {
      view.setVisibility(View.VISIBLE);
      Glide.with(activity).load(path).into(view);
    }
  }


  private class ServerStatusUpdate {

    private static final String Tag = "HV_ServerStatusUpdate";
    private static final int REPEAT_DELAY = 15000;

    private final Handler mHandler;
    private final PlexVideoItem mItem;
    private final PlexServer mServer;
    private final PlaybackTransportControlGlue mGlue;
    private final Runnable mRunnable = new Runnable() {
      @Override
      public void run() {
        updateStatus();
        ServerStatusUpdate.this.start();
      }
    };

    ServerStatusUpdate(Handler handler, PlexVideoItem item, PlexServer server,
                       PlaybackTransportControlGlue glue) {
      mHandler = handler;
      mItem = item;
      mServer = server;
      mGlue = glue;
      updateRecommendations(true);
    }

    void start() {
      stop();
      Log.d(Tag, "Starting updates");
      if (mItem.shouldUpdateStatusOnPlayback())
        mHandler.postDelayed(mRunnable, REPEAT_DELAY);
    }

    void stop() {
      Log.d(Tag, "Stoping updates");
      mHandler.removeCallbacks(mRunnable);
    }

    void onSeek() {
      stop();
      updateStatus();
      start();
    }

    private void updateStatus() {
      long currentPos = mGlue.getCurrentPosition();

      Log.d(Tag, "Updating");
      VideoProgressTask.getTask(mServer, mItem).setProgress(
       0 == (mItem.getDuration() - currentPos), currentPos);
    }

    void setItemWatched() {
      stop();
      Log.d(Tag, "Setting Item watched");
      VideoProgressTask.getTask(mServer, mItem).setProgress(true, mItem.getDuration());
    }

    void updateRecommendations(boolean excludeCurrent) {
      Log.d(Tag, "Updating Recommendations, excluding current = " + excludeCurrent);
      Context context = mFragment.getContext();
      Intent intent = new Intent(context.getApplicationContext(), UpdateRecommendationsService.class);
      mServer.setCurrentPlayingVideoRatingKey(excludeCurrent && mItem != null ?
       mItem.getRatingKey() : PlexServer.INVALID_RATING_KEY);
      context.startService(intent);
    }
  }

  private class RemainingTimeUI {

    private static final int REPEAT_DELAY = 1000;

    private final Handler mHandler;
    private final Activity mActivity;
    private final PlaybackTransportControlGlue mGlue;
    private final long mDuration;
    private final TextView mEndTime;

    private Runnable mRunnable = new Runnable() {
      @Override
      public void run() {
        updateRemainingTime();
        RemainingTimeUI.this.start();
      }
    };

    RemainingTimeUI(Handler handler, Activity activity, PlaybackTransportControlGlue glue, long duration) {
      mHandler = handler;
      mActivity = activity;
      mGlue = glue;
      mDuration = duration;
      mEndTime = activity.findViewById(R.id.end_time);
    }

    void start() {
      stop();
      mHandler.postDelayed(mRunnable, REPEAT_DELAY);
    }

    void updateRemainingTime() {

      if(mGlue != null) {
        final long timeLeft = mDuration - mGlue.getCurrentPosition();
        final String out = timeLeft >= 0 && timeLeft <= mDuration ?
          new SimpleDateFormat("h:mm", Locale.US).format((new Date()).getTime() + timeLeft) :
         "";
        if (mEndTime != null) {
          mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
              mEndTime.setText(out);
            }
          });
        }
      }
    }

    void stop() {
      mHandler.removeCallbacks(mRunnable);
    }

  }

}
