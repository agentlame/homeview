package com.monsterbutt.homeview.ui.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;

import android.os.Handler;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextClock;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.source.BehindLiveWindowException;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.text.CaptionStyleCompat;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.ui.SubtitleView;
import com.google.android.exoplayer2.util.Util;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.player.MediaTrackSelector;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.plex.media.Stream;
import com.monsterbutt.homeview.plex.media.VideoFormat;
import com.monsterbutt.homeview.presenters.CardObject;
import com.monsterbutt.homeview.presenters.CardPresenter;
import com.monsterbutt.homeview.services.ThemeService;
import com.monsterbutt.homeview.ui.fragment.ErrorFragment;
import com.monsterbutt.homeview.ui.handler.PlaybackHandler;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import us.nineworlds.plex.rest.model.impl.Media;

import static android.media.session.PlaybackState.STATE_PAUSED;
import static android.media.session.PlaybackState.STATE_PLAYING;
import static android.media.session.PlaybackState.STATE_STOPPED;
import static com.google.android.exoplayer2.ExoPlayer.STATE_BUFFERING;
import static com.google.android.exoplayer2.ExoPlayer.STATE_ENDED;
import static com.google.android.exoplayer2.ExoPlayer.STATE_IDLE;
import static com.google.android.exoplayer2.ExoPlayer.STATE_READY;
import static com.google.android.exoplayer2.util.MimeTypes.BASE_TYPE_APPLICATION;
import static com.google.android.exoplayer2.util.MimeTypes.BASE_TYPE_AUDIO;
import static com.google.android.exoplayer2.util.MimeTypes.BASE_TYPE_TEXT;

public class PlayerActivity extends Activity implements ExoPlayer.EventListener,
 PlaybackControlView.VisibilityListener, PlaybackHandler.Invoker, CardPresenter.CardPresenterLongClickListener {

  private static final String Tag = "PictureActivity";
  public static final String ACTION_VIEW = "com.monsterbutt.homeview.ui.activity.action.VIEW";
  public static final String KEY = "key";
  public static final String START_OFFSET = "startoffset";
  public static final String VIDEO = "video";
  public static final String TRACKS = "tracks";
  public static final String FILTER = "filter";
  public static final String SHARED_ELEMENT_NAME = "hero";


  private SimpleExoPlayerView simpleExoPlayerView;

  private boolean needRetrySource;

  private PlaybackHandler player = null;

  private boolean controllerIsVisible = false;
  private boolean selectionViewOnTop = false;

  private TextView title;
  private TextView subtitle;
  private TextView endTime;

  private ImageView poster;
  private ImageView rating;
  private ImageView audio;
  private ImageView channels;
  private ImageView video;
  private ImageView resolution;
  private ImageView frameRate;
  private ImageView studio;

  private ProgressBar progress_waiting;

  private View controls;
  private View clock;


  private MediaSessionCompat session;

  // Activity lifecycle

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(Tag, "onCreate");

    setContentView(R.layout.player_activity);

    session = new MediaSessionCompat(this, "HomeView");
    session.setSessionActivity(PendingIntent.getActivity(this, 99 /*request code*/,
     new Intent(this, PlayerActivity.class), PendingIntent.FLAG_UPDATE_CURRENT));
    session.setCallback(new MediaSessionCallback());
    session.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);

    simpleExoPlayerView = (SimpleExoPlayerView) findViewById(R.id.player_view);
    simpleExoPlayerView.setControllerVisibilityListener(this);
    SubtitleView subtitleView = simpleExoPlayerView.getSubtitleView();
    if (subtitleView != null) {
      subtitleView.setStyle(new CaptionStyleCompat(Color.WHITE,
       Color.TRANSPARENT, Color.TRANSPARENT,
       CaptionStyleCompat.EDGE_TYPE_OUTLINE, Color.BLACK, null));
    }
    simpleExoPlayerView.requestFocus();

    findViewById(R.id.exo_pip).setOnClickListener(new OnClickListener() {
      @TargetApi(24)
      @Override
      public void onClick(View v) {
        enterPictureInPictureMode();
      }
    });
    findViewById(R.id.exo_prev_chapter).setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        player.previous();
      }
    });
    findViewById(R.id.exo_next_chapter).setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        player.next();
      }
    });
    controls = findViewById(R.id.controls_section);
    clock = findViewById(R.id.current_time);
    progress_waiting = (ProgressBar) findViewById(R.id.progress_waiting);

    poster = (ImageView) findViewById(R.id.posterImage);
    video = (ImageView) findViewById(R.id.videoImage);
    resolution = (ImageView) findViewById(R.id.resolutionImage);
    studio = (ImageView) findViewById(R.id.studio);
    frameRate = (ImageView) findViewById(R.id.framerateImage);
    audio = (ImageView) findViewById(R.id.audioImage);
    channels = (ImageView) findViewById(R.id.channelsImage);
    rating = (ImageView) findViewById(R.id.ratingImage);

    title = (TextView) findViewById(R.id.title);
    subtitle = (TextView) findViewById(R.id.subtitle);
    endTime = (TextView) findViewById(R.id.end_time);

    ((TextClock) findViewById(R.id.current_time)).setFormat12Hour( "h:mm" );
  }

  @Override
  public void onNewIntent(Intent intent) {
    Log.d(Tag, "onNewIntent");
    //releasePlayer();
    setIntent(intent);
  }

  @Override
  public void onStart() {
    super.onStart();
    Log.d(Tag, "onStart");
    if (Util.SDK_INT > 23) {
      initializePlayer();
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    ThemeService.stopTheme(this);
  }

  @TargetApi(24)
  @Override
  public void onPause() {
    super.onPause();

    Log.d(Tag, "onPause");
    if (player != null && player.isPlaying()) {
      boolean isVisibleBehind = requestVisibleBehind(true);
      if (!isVisibleBehind && !isInPictureInPictureMode())
        player.play(false);
      else
        showControls(false);
    }
    else {
      requestVisibleBehind(false);
    }
  }

  @Override
  public void onStop() {
    super.onStop();
    releasePlayer();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.d(Tag, "onDestroy");
    releasePlayer();
  }

  private void setControllerIsVisible(boolean isVisible) {
    synchronized (this) {
      controllerIsVisible = isVisible;
    }
  }

  private boolean isControllerVisible() {
    boolean ret;
    synchronized (this) {
      ret = controllerIsVisible;
    }
    return ret;
  }

  // Activity input

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    // Show the controls on any key event.dis
    // If the event was not handled then see if the player view can handle it as a media key event.

    boolean wasControllerVisible = isControllerVisible();
    if (event.getAction() == KeyEvent.ACTION_DOWN) {
      switch (event.getKeyCode()) {

        case KeyEvent.KEYCODE_DPAD_UP:
        case KeyEvent.KEYCODE_DPAD_DOWN:

          if (isSelectionViewOnTop())
            return true;
          showControls(true);
          if (!wasControllerVisible)
            return true;
          break;

        case KeyEvent.KEYCODE_DPAD_CENTER:
        case KeyEvent.KEYCODE_ENTER:
          if (handleSelectionViews())
            return true;
          if (!isSelectionViewOnTop()) {
            showControls(true);
            if (!wasControllerVisible)
              return true;
          }
          break;

        case KeyEvent.KEYCODE_DPAD_LEFT:
        case KeyEvent.KEYCODE_DPAD_RIGHT:
          if (!isSelectionViewOnTop()) {
            if (!wasControllerVisible) {
              if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT)
                player.jumpBack();
              else
                player.jumpForward();
              return true;
            }
            showControls(true);
          }
          break;

        case KeyEvent.KEYCODE_BACK:

          boolean isShowingNextup = player.isShowingNextUp();
          if (isSelectionViewOnTop() && player.releaseSelectView()) {
            if (!isShowingNextup)
              showControls(true);
            return true;
          }
          if (wasControllerVisible && player.isPlaying()) {
            showControls(false);
            return true;
          }
          break;

        case KeyEvent.KEYCODE_MEDIA_NEXT:
          player.next();
          return true;

        case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
          player.previous();
          return true;

        default:
          break;
      }
    }

    return super.dispatchKeyEvent(event) || simpleExoPlayerView.dispatchMediaKeyEvent(event);
  }

  // PlaybackControlView.VisibilityListener implementation

  @Override
  public void onVisibilityChange(int visibility) {
    Log.d(Tag, "Visibility Changed: " + visibility);

    boolean wasVisible = isControllerVisible();
    boolean visible = visibility == View.VISIBLE;
    setControllerIsVisible(visible);
    if (visible && player != null)
        updateTimeRemaining(player.getTimeLeft());

    if (!wasVisible && visible) {
      clock.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
      controls.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_up));
    }
    else if(wasVisible && !visible) {
      clock.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));
      controls.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_out_down));
    }
  }

  // Internal methods

  private void initializePlayer() {
    Intent intent = getIntent();
    boolean needNewPlayer = player == null;
    if (needNewPlayer)
      player = new PlaybackHandler(this, this, simpleExoPlayerView, new Handler(getMainLooper()));

    if (needNewPlayer || needRetrySource) {
      String action = intent.getAction();
      if (ACTION_VIEW.equals(action)) {
        player.playVideo((PlexVideoItem) intent.getParcelableExtra(PlayerActivity.VIDEO), intent);
        needRetrySource = false;
      } else
        showError(getString(R.string.unexpected_intent_action, action));
    }
  }

  private void releasePlayer() {

    if (session != null && session.isActive())
      session.setActive(false);
    if (player != null) {
      Log.d(Tag, "Releasing player");
      player.release();
      player = null;
    }
  }

  // ExoPlayer.EventListener implementation

  @Override
  public void onLoadingChanged(boolean isLoading) {
    // Do nothing.
  }

  @Override
  public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
    Log.d(Tag, "Player State changed, Play When Ready: " + playWhenReady + " , State : " + playbackState);

    switch (playbackState) {

      default:
      case STATE_IDLE:
      case STATE_BUFFERING:
        break;
      case STATE_ENDED:
        exit();
        break;
      case STATE_READY:
        showProgress(false);
        adjustFocusForTimeBar(playWhenReady);
        if (playWhenReady)
          onPlayback(player.isPlaying());
        else if (player.shouldStart()) {
          player.play(true);
          updateTimeRemaining(player.getTimeLeft());
        }
        if (player.isPlaying() && !session.isActive())
          session.setActive(true);
        else if (!player.isPlaying() && session.isActive())
          session.setActive(false);
        break;
    }
  }

  public void showProgress(boolean show) {

    if (!show && progress_waiting.getVisibility() == View.VISIBLE)
      progress_waiting.setVisibility(View.GONE);
    else if (show && progress_waiting.getVisibility() != View.VISIBLE)
      progress_waiting.setVisibility(View.VISIBLE);
  }

  private void adjustFocusForTimeBar(boolean playing) {

    View view = findViewById(R.id.exo_progress);
    view.setNextFocusDownId( playing ? R.id.exo_pause : R.id.exo_play);
  }

  @Override
  public void onRepeatModeChanged(int repeatMode) {
    // Do nothing.
  }

  // User controls

  @Override
  public void showError(String message) {

    showProgress(false);

    ErrorFragment fragment = new ErrorFragment();
    Bundle args = new Bundle();
    args.putString(ErrorFragment.MESSAGE, message);
    fragment.setArguments(args);
    View view = findViewById(R.id.error_fragment);
    view.setVisibility(View.VISIBLE);
    getFragmentManager().beginTransaction().add(R.id.error_fragment, fragment, "Error").commit();
  }

  private static boolean isBehindLiveWindow(ExoPlaybackException e) {
    if (e.type != ExoPlaybackException.TYPE_SOURCE) {
      return false;
    }
    Throwable cause = e.getSourceException();
    while (cause != null) {
      if (cause instanceof BehindLiveWindowException) {
        return true;
      }
      cause = cause.getCause();
    }
    return false;
  }

  @Override
  public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
    player.onPictureInPictureModeChanged(isInPictureInPictureMode);
    if (isInPictureInPictureMode)
      showControls(false);
  }

  @Override
  public void onPositionDiscontinuity() {
    Log.d(Tag, "Position Discontinuity");
    if (needRetrySource) {
      // This will only occur if the user has performed a seek whilst in the error state. Update the
      // resume position so that if the user then retries, playback will resume from the position to
      // which they seeked.
      player.updateResumePosition();
    }
  }

  @Override
  public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
    // Do nothing.
  }

  @Override
  public void onTimelineChanged(Timeline timeline, Object manifest) {
    // Do nothing.
    Log.d(Tag, "Timeline changed");
  }

  @Override
  public void onPlayerError(ExoPlaybackException e) {
    Log.d(Tag, "Player Error");
    String errorString = null;
    if (e.type == ExoPlaybackException.TYPE_RENDERER) {
      Exception cause = e.getRendererException();
      if (cause instanceof MediaCodecRenderer.DecoderInitializationException) {
        // Special case for decoder initialization failures.
        MediaCodecRenderer.DecoderInitializationException decoderInitializationException =
         (MediaCodecRenderer.DecoderInitializationException) cause;
        if (decoderInitializationException.decoderName == null) {
          if (decoderInitializationException.getCause() instanceof MediaCodecUtil.DecoderQueryException) {
            errorString = getString(R.string.error_querying_decoders);
          } else if (decoderInitializationException.secureDecoderRequired) {
            errorString = getString(R.string.error_no_secure_decoder,
             decoderInitializationException.mimeType);
          } else {
            errorString = getString(R.string.error_no_decoder,
             decoderInitializationException.mimeType);
          }
        } else {
          errorString = getString(R.string.error_instantiating_decoder,
           decoderInitializationException.decoderName);
        }
      }
    }
    else
      errorString = getString(R.string.video_error_unknown_error);
    if (errorString != null) {
      showError(errorString);
    }
    needRetrySource = true;
    if (isBehindLiveWindow(e)) {
      player.clearResumePosition();
      initializePlayer();
    } else {
      player.updateResumePosition();
    }
  }

  @Override
  @SuppressWarnings("ReferenceEquality")
  public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    Log.d(Tag, "Tracks changed");
    player.tracksChanged(trackGroups);

    boolean hasSubs = false;
    boolean hasAudio = false;

    for (int i = 0; i < trackGroups.length; ++i) {
      TrackGroup group = trackGroups.get(i);
      if (group.length > 0) {
        if (group.getFormat(0).sampleMimeType.startsWith(BASE_TYPE_AUDIO))
          hasAudio = true;
        else if(group.getFormat(0).sampleMimeType.startsWith(BASE_TYPE_APPLICATION)
         || group.getFormat(0).sampleMimeType.startsWith(BASE_TYPE_TEXT))
          hasSubs = true;
      }
    }

    ImageButton audioButton = (ImageButton) findViewById(R.id.exo_audio_tracks);
    if (hasAudio)
     audioButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        audioSelected();
      }
    });
    SetButtonEnabled(audioButton, hasAudio);

    ImageButton subtitlesButton = (ImageButton) findViewById(R.id.exo_subtitle_tracks);
    if (hasSubs)
     subtitlesButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        subtitlesSelected();
      }
    });
    SetButtonEnabled(subtitlesButton, hasSubs);
  }

  @Override
  public Context getValidContext() {
    return (isFinishing() || isDestroyed()) ? null : this;
  }

  @Override
  public void onPlayback(boolean isPlaying) {

    player.handlePlaybackChanged(isPlaying);
    if (isPlaying)
      getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    else
      getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  }

  @Override
  public void updateMetadata(PlexVideoItem item, Bitmap bitmap, PlexServer server, MediaTrackSelector tracks, MediaMetadataCompat build) {

    session.setMetadata(build);
    setItem(item, bitmap, server, tracks);

    ImageButton chaptersButton = (ImageButton) findViewById(R.id.exo_chapters);
    if (item.hasChapters())
      chaptersButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        chaptersSelection();
      }
    });
    SetButtonEnabled(chaptersButton, item.hasChapters());
  }

  @Override
  public boolean longClickOccured(CardObject card, CardPresenter.LongClickWatchStatusCallback callback) {
    return false;
  }

  private void SetButtonEnabled(View view, boolean enabled) {

    view.setEnabled(enabled);
    view.setAlpha(enabled ? 1f : 0.3f);
  }

  private boolean isSelectionViewOnTop() {
    synchronized (this) {
      if (selectionViewOnTop)
        return true;
    }
    return false;
  }

  private boolean handleSelectionViews() {

    boolean selected = findViewById(R.id.exo_subtitle_tracks).hasFocus();
    if (selected) {
      if (findViewById(R.id.exo_subtitle_tracks).isEnabled())
        subtitlesSelected();
    }
    else {
      selected = findViewById(R.id.exo_audio_tracks).hasFocus();
      if (selected) {
        if (findViewById(R.id.exo_audio_tracks).isEnabled())
          audioSelected();
      }
      else {
        selected = findViewById(R.id.exo_chapters).hasFocus();
        if (selected) {
          if (findViewById(R.id.exo_chapters).isEnabled())
            chaptersSelection();
        }
      }
    }

    return selected;
  }

  private void chaptersSelection() {
    showControls(false);
    player.selectChapter(this);
  }

  private void audioSelected() {
    showControls(false);
    player.selectTracks(this, Stream.Audio_Stream);
  }

  private void subtitlesSelected() {
    showControls(false);
    player.selectTracks(this, Stream.Subtitle_Stream);
  }

  @Override
  public void selectionViewState(boolean visible, PlexVideoItem item, PlexServer server, MediaTrackSelector tracks) {
    synchronized (this) {
      selectionViewOnTop = visible;
    }
    if (!visible)
      updatePlaybackIcons(item, server, tracks);
  }

  @Override
  public void showControls(boolean show) {
    if (show)
      simpleExoPlayerView.showController();
    else
      simpleExoPlayerView.hideController();
  }

  @Override
  public void exit() {
    releasePlayer();
    finish();
  }


  public void setItem(PlexVideoItem item, Bitmap itemPoster, PlexServer server, MediaTrackSelector tracks) {

    title.setText(item.getPlaybackTitle(this));
    String subtitleStr = item.getPlaybackSubtitle(this, false);
    subtitle.setVisibility(TextUtils.isEmpty(subtitleStr) ? View.INVISIBLE : View.VISIBLE);
    subtitle.setText(subtitleStr);

    loadImage(studio, item.getDetailStudioPath(server));
    updateTimeRemaining(item.getDuration());

    if (itemPoster != null)
      poster.setImageBitmap(itemPoster);
    else
      loadImage(poster, server.makeServerURL(item.getCardImageURL()));
    loadImage(rating, item.getDetailRatingPath(server));

    updatePlaybackIcons(item, server, tracks);
  }

  private void updatePlaybackIcons(PlexVideoItem item, PlexServer server, MediaTrackSelector tracks) {

    Media media = item.getMedia() != null ? item.getMedia().get(0) : null;
    if (media != null) {
      loadImage(frameRate, server.makeServerURLForCodec(VideoFormat.VideoFrameRate, media.getVideoFrameRate()));
    }
    else {
      frameRate.setVisibility(View.INVISIBLE);
    }

    if (tracks != null) {
      Stream videoStream = tracks.getSelectedTrack(Stream.Video_Stream);
      if (videoStream != null) {
        loadImage(video, server.makeServerURLForCodec(VideoFormat.VideoCodec, videoStream.getCodec()));
        loadImage(resolution, media != null ?
         server.makeServerURLForCodec(VideoFormat.VideoResolution, media.getVideoResolution()) : "");
      } else {
        video.setVisibility(View.INVISIBLE);
        resolution.setVisibility(View.INVISIBLE);
      }

      Stream audioStream = tracks.getSelectedTrack(Stream.Audio_Stream);
      if (audioStream != null) {
        loadImage(audio, server.makeServerURLForCodec(Stream.AudioCodec, audioStream.getCodecAndProfile()));
        loadImage(channels, server.makeServerURLForCodec(Stream.AudioChannels, audioStream.getChannels()));
      }
      else {
        audio.setVisibility(View.INVISIBLE);
        channels.setVisibility(View.INVISIBLE);
      }
    }
  }

  public void updateTimeRemaining(long timeLeft) {

    if (timeLeft > 0 && player != null && timeLeft < player.getDuration()) {
      String out = " " + new SimpleDateFormat("h:mm", Locale.US).format((new Date()).getTime() + timeLeft);
      endTime.setText(out);
    }
  }

  private void loadImage(ImageView view, String path) {

    if (TextUtils.isEmpty(path))
      view.setVisibility(View.INVISIBLE);
    else {
      view.setVisibility(View.VISIBLE);
      Glide.with(this).load(path).into(view);
    }
  }


  private class MediaSessionCallback extends MediaSessionCompat.Callback {

    @Override
    public void onPlay() {
      player.play(true);
    }

    @Override
    public void onPause() {
      player.play(false);
      requestVisibleBehind(false);
      session.setActive(false);
    }

    @Override
    public void onSkipToNext() {
      player.next();
    }

    @Override
    public void onSkipToPrevious() {
      player.previous();
    }

    @Override
    public void onFastForward() {
      player.jumpForward();
    }

    @Override
    public void onRewind() {
      player.jumpBack();
    }

    @Override
    public void onStop() {
      exit();
    }

    @Override
    public void onSeekTo(long pos) {
      player.seekTo(pos);
    }
  }

  @Override
  public void updateSessionProgress() {
    long position = player == null ? 0 : player.getCurrentPosition();
    PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder();
    //noinspection WrongConstant
    stateBuilder.setActions(getAvailableActions());
    stateBuilder.setState(player == null ? STATE_STOPPED : player.isPlaying() ? STATE_PLAYING : STATE_PAUSED, position, 1.0f);
    session.setPlaybackState(stateBuilder.build());
  }

  private @PlaybackStateCompat.Actions
  long getAvailableActions() {
    return PlaybackState.ACTION_PLAY_PAUSE
     | PlaybackState.ACTION_PAUSE
     | PlaybackState.ACTION_PLAY
     | PlaybackState.ACTION_SKIP_TO_PREVIOUS
     | PlaybackState.ACTION_SKIP_TO_NEXT
     | PlaybackState.ACTION_STOP;
  }
}
