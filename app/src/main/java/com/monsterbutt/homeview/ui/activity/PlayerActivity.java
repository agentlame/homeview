package com.monsterbutt.homeview.ui.activity;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.os.Handler;
import android.support.v4.app.FragmentActivity;
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
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
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

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.player.MediaTrackSelector;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.plex.media.Stream;
import com.monsterbutt.homeview.plex.media.VideoFormat;
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
import static com.google.android.exoplayer2.Player.STATE_BUFFERING;
import static com.google.android.exoplayer2.Player.STATE_ENDED;
import static com.google.android.exoplayer2.Player.STATE_IDLE;
import static com.google.android.exoplayer2.Player.STATE_READY;
import static com.google.android.exoplayer2.util.MimeTypes.BASE_TYPE_APPLICATION;
import static com.google.android.exoplayer2.util.MimeTypes.BASE_TYPE_AUDIO;
import static com.google.android.exoplayer2.util.MimeTypes.BASE_TYPE_TEXT;

public class PlayerActivity extends FragmentActivity implements Player.EventListener,
 PlaybackControlView.VisibilityListener, PlaybackHandler.Invoker {

  private static final String Tag = "HV_PlayerActivity";
  public static final String ACTION_VIEW = "com.monsterbutt.homeview.ui.activity.action.VIEW";
  public static final String KEY = "key";
  public static final String ACTION = "action";
  public static final String START_OFFSET = "startoffset";
  public static final String VIDEO = "video";
  public static final String TRACKS = "tracks";
  public static final String FILTER = "filter";
  public static final String SHARED_ELEMENT_NAME = "hero";
  public static final String URI = "homeview://app/playback";

  private SimpleExoPlayerView simpleExoPlayerView;

  private String lastPlayingKey = "";

  private boolean needRetrySource;
  private boolean dontStartPlaybackOnLoad = false;

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

  private AudioManager mAudioManager;
  private boolean mHasAudioFocus;
  private boolean mPauseTransient;
  private MediaSessionCompat session;

  private boolean holdWake = false;

  private final AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener =
   new AudioManager.OnAudioFocusChangeListener() {
     @Override
     public void onAudioFocusChange(int focusChange) {
       switch (focusChange) {
         case AudioManager.AUDIOFOCUS_LOSS:
           abandonAudioFocus();
           if (player != null && player.isPlaying())
             pause();
           break;
         case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
           if (player != null && player.isPlaying()) {
             pause();
             mPauseTransient = true;
           }
           break;
         case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
           if (player != null)
             player.mute(true);
           break;
         case AudioManager.AUDIOFOCUS_GAIN:
         case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
         case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
           if (mPauseTransient) {
             if (player != null && player.isPlaying())
               play();
           }
           if (player != null)
             player.mute(false);
           break;
       }
     }
   };

  // Activity lifecycle

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(Tag, "onCreate");

    setContentView(R.layout.player_activity);

    mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    session = new MediaSessionCompat(this, "HomeView");
    session.setSessionActivity(PendingIntent.getActivity(this, 99 /*request code*/,
     new Intent(this, PlayerActivity.class), PendingIntent.FLAG_UPDATE_CURRENT));
    session.setCallback(new MediaSessionCallback());
    session.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
    session.setActive(true);

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
      @Override
      public void onClick(View v) {
        goToPIP();
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
    setIntent(intent);
  }

  @Override
  public void onStart() {
    super.onStart();
    Log.d(Tag, "onStart");
    initializePlayer();
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.d(Tag, "onResume");
    ThemeService.stopTheme(this);

    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !TextUtils.isEmpty(lastPlayingKey)) {

      Intent intent = getIntent();
      if (intent != null) {
        String key = intent.getStringExtra(PlayerActivity.KEY);
        if (TextUtils.isEmpty(key)) {
          Uri data = intent.getData();
          if (intent.getAction().equals(Intent.ACTION_VIEW) && data != null && data.toString().startsWith(URI))
            key = data.toString().substring(URI.length());
        }
        if (!TextUtils.isEmpty(key) && !key.equals(lastPlayingKey)) {
          needRetrySource = true;
          initializePlayer();
        }
      }
      lastPlayingKey = "";
    }
  }

  @TargetApi(26)
  boolean goToPIP() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      PictureInPictureParams params = new PictureInPictureParams.Builder()
       .setAspectRatio(player.getVideoAspectRatio())
       .setSourceRectHint(player.getVideoRect())
       .build();
      return enterPictureInPictureMode(params);
    }

    enterPictureInPictureMode();
    return true;
  }

  @TargetApi(24)
  @Override
  public void onPause() {
    super.onPause();

    lastPlayingKey = "";
    Log.d(Tag, "onPause");
    if (player != null && player.isPlaying()) {
      lastPlayingKey = player.getCurrentKey();
      Log.d(Tag, "Turn On Visible Behind");
      boolean isVisibleBehind = (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O ? requestVisibleBehind(true) : goToPIP());
      if (!isVisibleBehind && !isInPictureInPictureMode()) {
        Log.d(Tag, "Visible Behind failed");
        pause();
      }
      else
        showControls(false);
    }
    else {
      Log.d(Tag, "Turn off Visible Behind");
      if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
        requestVisibleBehind(false);
    }
  }

  @Override
  public void onStop() {
    super.onStop();
    Log.d(Tag, "onStop");
    releasePlayer();
    dontStartPlaybackOnLoad = true;
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

  @Override
  public void onVisibleBehindCanceled() {
    pause();
    super.onVisibleBehindCanceled();
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
      Uri data = intent.getData();
      if (action.equals(Intent.ACTION_VIEW) && data != null && data.toString().startsWith(URI)) {
        intent.putExtra(ACTION, ACTION_VIEW);
        intent.putExtra(KEY, data.toString().substring(URI.length()));
      }
      if (ACTION_VIEW.equals(action) || (!TextUtils.isEmpty(intent.getStringExtra(ACTION)) && ACTION_VIEW.equals(intent.getStringExtra(ACTION)))) {
        Log.d(Tag, "Playing video from Action_View");
        player.playVideo((PlexVideoItem) intent.getParcelableExtra(PlayerActivity.VIDEO), intent);
        needRetrySource = false;
      } else
        showError(getString(R.string.unexpected_intent_action, action));
    }
  }

  private void releasePlayer() {

    if (session != null)
      session.release();
    if (player != null) {
      Log.d(Tag, "Releasing player");
      player.release();
      player = null;
    }

    abandonAudioFocus();
  }

  private void play() {
    // Request audio focus whenever we resume playback
    // because the app might have abandoned audio focus due to the AUDIOFOCUS_LOSS.
    requestAudioFocus();

    if (player != null) {
      if (dontStartPlaybackOnLoad)
        Log.d(Tag, "Skipping Play call because of restart from dreaming");
      else
        player.play(true);
    }
    updateSessionProgress();
  }

  private void pause() {
    mPauseTransient = false;
    if (player != null)
      player.play(false);
    updateSessionProgress();
  }

  private AudioFocusRequest afRequest = null;
  @TargetApi(26)
  private void requestAudioFocus() {
    if (mHasAudioFocus) {
      return;
    }

    synchronized (this) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && null == afRequest) {
        afRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
         .setAudioAttributes(new AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_MEDIA)
          .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
          .build())
         .setAcceptsDelayedFocusGain(true)
         .setOnAudioFocusChangeListener(mOnAudioFocusChangeListener, new Handler(getMainLooper()))
         .setWillPauseWhenDucked(true)
         .build();
      }
    }

    Log.d(Tag, "Request Audio Focus");
    int result;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
      result = mAudioManager.requestAudioFocus(afRequest);
    else
      result = mAudioManager.requestAudioFocus(mOnAudioFocusChangeListener,
     AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
      mHasAudioFocus = true;
    else
      pause();
  }

  private void abandonAudioFocus() {
    if (mHasAudioFocus) {
      Log.d(Tag, "Abandon Audio Focus");
      mHasAudioFocus = false;

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        mAudioManager.abandonAudioFocusRequest(afRequest);
      else
         mAudioManager.abandonAudioFocus(mOnAudioFocusChangeListener);
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
        if (!player.playNext())
          exit();
        break;
      case STATE_READY:
        showProgress(false);
        adjustFocusForTimeBar(playWhenReady);
        if (playWhenReady) {
          dontStartPlaybackOnLoad = false;
          onPlayback(player.isPlaying());
        }
        else if (player.shouldStart())
          play();
        else if (!dontStartPlaybackOnLoad)
          onPlayback(false);

        updateTimeRemaining(player.getTimeLeft());
        break;
    }
  }

  public boolean shouldForceRefreshRate() {
    return dontStartPlaybackOnLoad;
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

  @Override
  public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

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
    super.onPictureInPictureModeChanged(isInPictureInPictureMode);
    if (player != null)
     player.onPictureInPictureModeChanged(isInPictureInPictureMode);
    if (isInPictureInPictureMode)
      showControls(false);
  }

  @Override
  public void onPositionDiscontinuity(@Player.DiscontinuityReason int reason) {
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

  private void getScreenLock() {
    if (!holdWake) {
      getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
      holdWake = true;
      Log.d(Tag, "Stay Awake");
      requestAudioFocus();
    }
  }

  private void releaseScreenLock() {
    if (holdWake){
      abandonAudioFocus();
      getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
      holdWake = false;
      Log.d(Tag, "Clear Awake");
    }
  }

  @Override
  public void onPlayback(final boolean isPlaying) {

    player.handlePlaybackChanged(isPlaying);
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        synchronized (PlayerActivity.this) {
          if (isPlaying)
            getScreenLock();
          else
            releaseScreenLock();
        }
      }
    });
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
    if (simpleExoPlayerView == null)
      return;
    if (show)
      simpleExoPlayerView.showController();
    else
      simpleExoPlayerView.hideController();
  }

  @Override
  public void exit() {
    if (player != null)
     player.updateRecommendations(null);
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
      play();
    }

    @Override
    public void onPause() {
      pause();
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
    Log.d(Tag, "Updating Session Progress");
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
