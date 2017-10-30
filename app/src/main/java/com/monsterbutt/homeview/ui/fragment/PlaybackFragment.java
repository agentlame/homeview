package com.monsterbutt.homeview.ui.fragment;


import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v17.leanback.app.VideoFragment;
import android.support.v17.leanback.app.VideoFragmentGlueHost;
import android.support.v4.app.FragmentActivity;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextClock;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ui.SubtitleView;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.player.ExoPlayerAdapter;
import com.monsterbutt.homeview.player.HomeViewExoPlayerAdapter;
import com.monsterbutt.homeview.player.track.MediaTrackSelector;
import com.monsterbutt.homeview.player.notifier.PlaybackGuiVisibleNotifier;
import com.monsterbutt.homeview.player.display.ScreenLock;
import com.monsterbutt.homeview.player.notifier.SeekOccuredNotifier;
import com.monsterbutt.homeview.player.handler.StartPositionHandler;
import com.monsterbutt.homeview.player.notifier.VideoChangedNotifier;
import com.monsterbutt.homeview.player.VideoMediaPlayerGlue;
import com.monsterbutt.homeview.player.track.TrackSelector;
import com.monsterbutt.homeview.plex.PlexServerManager;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.services.ThemeService;


public class PlaybackFragment extends VideoFragment implements VideoChangedNotifier.Observer {

  public static final String TAG = "PlaybackFragment";
  private VideoMediaPlayerGlue<ExoPlayerAdapter> mMediaPlayerGlue;
  private final VideoFragmentGlueHost mHost = new VideoFragmentGlueHost(this);

  private TextClock mClock;
  private boolean initialized = false;

  AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener
   = new AudioManager.OnAudioFocusChangeListener() {
    @Override
    public void onAudioFocusChange(int state) {
    }
  };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    hideControlsOverlay(false);
    TrackSelector trackSelector = new TrackSelector();
    SeekOccuredNotifier seekOccuredNotifier = new SeekOccuredNotifier();
    ExoPlayerAdapter playerAdapter = HomeViewExoPlayerAdapter.getAdapter(getActivity(),
     (SubtitleView) getActivity().findViewById(R.id.exo_subtitles), trackSelector, seekOccuredNotifier);
    playerAdapter.setAudioStreamType(C.STREAM_TYPE_MUSIC);

    final PlaybackGuiVisibleNotifier visibleNotifier = new PlaybackGuiVisibleNotifier();
    VideoChangedNotifier videoChangedNotifier = new VideoChangedNotifier();
    videoChangedNotifier.register(this);
    FragmentActivity activity = (FragmentActivity) getActivity();
    mClock = activity.findViewById(R.id.clock);
    mMediaPlayerGlue = new VideoMediaPlayerGlue<>(this,
     PlexServerManager.getInstance(getContext(), (FragmentActivity) getActivity()).getSelectedServer(),
     new ScreenLock(activity),
     playerAdapter,
     videoChangedNotifier,
     seekOccuredNotifier,
     visibleNotifier,
     trackSelector);
    mMediaPlayerGlue.setHost(mHost);
    AudioManager audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
    if (audioManager == null || audioManager.requestAudioFocus(mOnAudioFocusChangeListener, AudioManager.STREAM_MUSIC,
     AudioManager.AUDIOFOCUS_GAIN) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
      Log.w(TAG, "video player cannot obtain audio focus!");
    }
    //PlaybackSeekDiskDataProvider.setDemoSeekProvider(mMediaPlayerGlue);
    setBackgroundType(BG_NONE);
    setSharedElementEnterTransition(TransitionInflater.from(getContext()).inflateTransition(android.R.transition.move));
    setControlsOverlayAutoHideEnabled(true);
    setFadeCompleteListener(new OnFadeCompleteListener() {
      @Override
      public void onFadeInComplete() {
        resetFocus();
        if (isControlsOverlayAutoHideEnabled())
          setControlsOverlayAutoHideEnabled(false);
        setControlsOverlayAutoHideEnabled(true);

        mClock.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in));
        mClock.setVisibility(View.VISIBLE);
        visibleNotifier.visibleGUI(true);
      }

      @Override
      public void onFadeOutComplete() {

        mClock.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.fade_out));
        mClock.setVisibility(View.GONE);
        visibleNotifier.visibleGUI(false);
      }
    });
  }

  private boolean isPlaying() {
    return mMediaPlayerGlue != null && mMediaPlayerGlue.isPlaying();
  }

  @Override
  public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
    super.onPictureInPictureModeChanged(isInPictureInPictureMode);
    if (mMediaPlayerGlue != null)
      mMediaPlayerGlue.onPictureInPictureModeChanged(isInPictureInPictureMode);
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.d(TAG, "onResume");
    ThemeService.stopTheme(getActivity());
    if (!readIntent() && mMediaPlayerGlue != null)
      mMediaPlayerGlue.setRefreshRateToCurrentVideo(getActivity());
  }

  private boolean readIntent() {

    if (initialized)
      return false;
    initialized = true;
    if (!mMediaPlayerGlue.parseIntent(getActivity().getIntent()))
      Log.e(TAG, "No intent given");
    return true;
  }

  @TargetApi(24)
  @Override
  public void onPause() {
    super.onPause();

    boolean shouldPause = true;
    Activity act = getActivity();
    Log.d(TAG, "onPause");
    if (isPlaying()) {
      Log.d(TAG, "Turn On Visible Behind");
      boolean isVisibleBehind = (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O ? act.requestVisibleBehind(true) : goToPIP());
      if (isVisibleBehind || act.isInPictureInPictureMode()) {
        hideControlsOverlay(false);
        shouldPause = false;
      }
    }
    else {
      Log.d(TAG, "Turn off Visible Behind");
      if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
        act.requestVisibleBehind(false);
    }

    if (mMediaPlayerGlue != null && shouldPause) {
      mMediaPlayerGlue.pause();
    }
  }

  @Override
  public void onStop() {
    super.onStop();
    if (mMediaPlayerGlue != null)
      mMediaPlayerGlue.release();
  }

  @TargetApi(26)
  boolean goToPIP() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
      return getActivity().enterPictureInPictureMode(new PictureInPictureParams.Builder().build());

    getActivity().enterPictureInPictureMode();
    return true;
  }

  @Override
  public void notify(PlexVideoItem item, MediaTrackSelector tracks, boolean usesDefaultTracks,
                     StartPositionHandler startPosition) {
    if (item == null)
      getActivity().finish();
  }

  @Override
  protected void onError(int errorCode, CharSequence errorMessage) {
    ErrorFragment fragment = new ErrorFragment();
    Bundle args = new Bundle();
    args.putString(ErrorFragment.MESSAGE, String.valueOf(errorMessage));
    fragment.setArguments(args);
    View view = getActivity().findViewById(R.id.error_fragment);
    view.setVisibility(View.VISIBLE);
    getFragmentManager().beginTransaction().add(R.id.error_fragment, fragment, "Error").commit();
  }

  private static final float MAX_ASPECT_RATIO_DEFORMATION_FRACTION = 0.01f;
  @Override
  protected void onVideoSizeChanged(int videoWidth, int videoHeight) {

    if (getView() == null)
      return;
    int screenWidth = getView().getWidth();
    int screenHeight = getView().getHeight();

    SurfaceView surface = getSurfaceView();
    ViewGroup.LayoutParams p = surface.getLayoutParams();

    p.width = screenWidth;
    p.height = screenHeight;
    if (videoWidth > 0 && videoHeight > 0) {
      float videoAspectRatio = (float) videoWidth / videoHeight;
      float viewAspectRatio = (float) screenWidth / screenHeight;
      float aspectDeformation = videoAspectRatio / viewAspectRatio - 1;
      if (Math.abs(aspectDeformation) > MAX_ASPECT_RATIO_DEFORMATION_FRACTION) {
        if (aspectDeformation > 0) {
          if (screenWidth != videoWidth) {
            float ratio =  (float) screenWidth / videoWidth;
            p.height = (int) (videoHeight * ratio);
          }
        }
        else {
          if (screenHeight != videoHeight) {
            float ratio = (float) screenHeight / videoHeight;
            p.width = (int) (videoWidth * ratio);
          }
        }
      }
    }
    surface.setLayoutParams(p);
  }

}
