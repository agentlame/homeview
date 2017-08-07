package com.monsterbutt.homeview.ui.handler;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.media.MediaMetadataCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.ui.DefaultTimeBar;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.player.EventLogger;
import com.monsterbutt.homeview.player.FrameRateSwitcher;
import com.monsterbutt.homeview.player.HomeViewPlayer;
import com.monsterbutt.homeview.player.MediaCodecCapabilities;
import com.monsterbutt.homeview.player.MediaTrackSelector;
import com.monsterbutt.homeview.player.StartPosition;
import com.monsterbutt.homeview.player.TrackSelector;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.PlexServerManager;
import com.monsterbutt.homeview.plex.media.Chapter;
import com.monsterbutt.homeview.plex.media.Episode;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.plex.media.Season;
import com.monsterbutt.homeview.plex.media.Stream;
import com.monsterbutt.homeview.plex.media.UpnpItem;
import com.monsterbutt.homeview.plex.tasks.GetVideoQueueTask;
import com.monsterbutt.homeview.plex.tasks.GetVideoTask;
import com.monsterbutt.homeview.plex.tasks.PlexServerTask;
import com.monsterbutt.homeview.plex.tasks.PlexServerTaskCaller;
import com.monsterbutt.homeview.plex.tasks.VideoProgressTask;
import com.monsterbutt.homeview.presenters.PosterCard;
import com.monsterbutt.homeview.services.UpdateRecommendationsService;
import com.monsterbutt.homeview.ui.activity.ContainerActivity;
import com.monsterbutt.homeview.ui.activity.PlayerActivity;
import com.monsterbutt.homeview.ui.android.SelectView;
import com.monsterbutt.homeview.ui.android.SelectViewRow;
import com.monsterbutt.homeview.ui.android.SwitchTrackView;
import com.monsterbutt.homeview.ui.fragment.NextUpFragment;

import java.io.IOException;
import java.util.Locale;

import us.nineworlds.plex.rest.model.impl.MediaContainer;

import static com.google.android.exoplayer2.ExoPlayer.STATE_BUFFERING;
import static com.google.android.exoplayer2.ExoPlayer.STATE_READY;
import static com.monsterbutt.homeview.plex.media.PlexVideoItem.BAD_CHAPTER_START;
import static com.monsterbutt.homeview.plex.media.PlexVideoItem.NEXTUP_DISABLED;
import static com.monsterbutt.homeview.plex.media.PlexVideoItem.START_CHAPTER_THRESHOLD;

public class PlaybackHandler implements PlexServerTaskCaller, ExtractorMediaSource.EventListener,
 FrameRateSwitcher.FrameRateSwitcherListener, SelectView.SelectViewCaller {

  public interface Invoker {

    Context getValidContext();
    void onPlayback(boolean isPlaying);
    void updateSessionProgress();
    void updateMetadata(PlexVideoItem item, Bitmap bitmap, PlexServer server, MediaTrackSelector tracks, MediaMetadataCompat build);
    void selectionViewState(boolean isVisible, PlexVideoItem item, PlexServer server, MediaTrackSelector tracks);
    void showControls(boolean show);
    void showProgress(boolean show);
    void showError(String msg);
    boolean shouldForceRefreshRate();
    void onNewIntent(Intent intent);
    void exit();
  }

  final static private String Tag = "HV_PlaybackHandler";

  private static final int CHOOSER_TIMEOUT = 15 * 1000;
  private static final int PROGRESS_UPDATE_INTERVAL = 10 * 1000;

  private final PlexServer server;

  private HomeViewPlayer player = null;
  private PlexVideoItem currentVideo = null;
  private PlexVideoItem previousVideo = null;
  private PlexVideoItem nextVideo = null;

  private String keyOfLastVideo = "";
  private boolean pausedTemp = true;

  private int resumeWindow;
  private long resumePosition;

  private MediaTrackSelector tracks;

  private final ExoPlayer.EventListener eventListener;
  private final SimpleExoPlayerView view;

  private final TrackSelector trackSelector;
  private TrackGroupArray lastSeenTrackGroupArray;

  private boolean loadChangedTracks = false;

  private final Invoker caller;

  private Intent intent;

  private FrameRateSwitcher switcher = null;
  private SelectView selectView = null;

  private final Handler mainHandler;

  private class ProgressRunable implements Runnable {

    private boolean repeat = false;

    public void single() {
      synchronized (this) {
        repeat = false;
      }
      mainHandler.post(this);
    }

    public void repeat(boolean forceNow) {
      synchronized (this) {
        repeat = true;
      }
      mainHandler.postDelayed(this, forceNow ? 0 : PROGRESS_UPDATE_INTERVAL);
    }

    @Override
    public void run() {
      if (player != null) {
        caller.updateSessionProgress();
        VideoProgressTask.getTask(server, currentVideo).setProgress(0 == getTimeLeft(), player.getCurrentPosition());
        boolean again;
        synchronized (this) {
          again = repeat;
        }
        if (again)
          repeat(false);
      }
    }
  }
  private ProgressRunable runnableProgress = new ProgressRunable();


  public PlaybackHandler(Invoker caller, ExoPlayer.EventListener eventListener, SimpleExoPlayerView view, Handler handler) {

    this.caller = caller;
    this.mainHandler = handler;
    server = PlexServerManager.getInstance(caller.getValidContext().getApplicationContext(), null).getSelectedServer();

    Log.d(Tag, "Initializing Player");
    clearResumePosition();
    lastSeenTrackGroupArray = null;
    trackSelector = new TrackSelector();
    this.eventListener = eventListener;
    this.view = view;
  }

  public void playVideo(PlexVideoItem video, Intent intent) {

    caller.showProgress(true);
    this.intent = intent;
    MediaTrackSelector chosenTracks = intent != null ? (MediaTrackSelector) intent.getParcelableExtra(PlayerActivity.TRACKS) : null;
    if (video == null)
      new GetVideoTask(this, server).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
       intent != null ? intent.getStringExtra(PlayerActivity.KEY) : "");
    else if (video.selectedHasMissingData())
      new GetFullInfo(intent).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, video.getKey());
    else
      playVideo(video, chosenTracks,  null);
  }

  private void playVideo(PlexVideoItem video, MediaTrackSelector chosenTracks,
                         MediaTrackSelector readTracks) {

    caller.showProgress(true);
    Context context = caller.getValidContext();
    if (context == null) {
      Log.e(Tag, "No context to play video");
      return;
    }
    else if (video == null) {
      Log.e(Tag, "No video, stopping player");
      if (player != null)
        player.stop();
      return;
    } else if (currentVideo != null && video.getRatingKey() == currentVideo.getRatingKey()) {
      Log.w(Tag, "Not going to reload current video");
      return;
    }
    Log.i(Tag, "Loading Video : " + video.getTitle());
    loadChangedTracks = chosenTracks != null;
    tracks = loadChangedTracks ? chosenTracks : readTracks;

    synchronized (this) {

      Log.d(Tag, "Next up released (play video)");
      mainHandler.removeCallbacks(nextUpRunnable);
      currentVideo = video;
      updateRecommendations(currentVideo);
      updateMetadata(context);

      if (switcher != null)
        switcher.unregister();
      switcher = new FrameRateSwitcher((Activity) context, this);
      if (!switcher.setDisplayRefreshRate(video, caller.shouldForceRefreshRate()))
        initiatePlayOfVideo(resumeWindow != C.INDEX_UNSET);
    }
    new GetVideoQueueTask(this, server).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, video.getKey());
  }

  @Override
  public void shouldPlay(boolean play) {
    if (play) {
      if (switcher != null) {
        switcher.unregister();
        switcher = null;
      }
      initiatePlayOfVideo(resumeWindow != C.INDEX_UNSET);
    }
    else
      play(false);
  }

  public boolean shouldStart() {
    boolean ret = false;
    if (pausedTemp) {
      Log.d(Tag, "Temp pause being cleared");
      pausedTemp = false;
      checkInitialTrack(Stream.Subtitle_Stream);
      ret = true;
    }
    return ret;
  }

  private void initiatePlayOfVideo(boolean haveResumePosition) {

    Context context = caller.getValidContext();

    long startOffset = intent != null ? intent.getLongExtra(PlayerActivity.START_OFFSET, -1) : -1;
    StartPosition startPosition = new StartPosition(context, startOffset, currentVideo.getViewedOffset());

    player = HomeViewPlayer.createPlayer(context, trackSelector, new DefaultLoadControl());
    player.addListener(eventListener);

    EventLogger eventLogger = new EventLogger(trackSelector);
    player.addListener(eventLogger);
    player.setAudioDebugListener(eventLogger);
    player.setVideoDebugListener(eventLogger);
    player.setMetadataOutput(eventLogger);

    trackSelector.setTunnelingAudioSessionId(C.generateAudioSessionIdV21(context));

    DefaultTimeBar timeBar = (DefaultTimeBar) view.findViewById(R.id.exo_progress);
    long[] chapters = currentVideo.getChapters();
    timeBar.setAdBreakTimesMs(chapters, chapters == null ? 0 : chapters.length);
    view.setPlayer(player);

    player.setPlayWhenReady(false);
    Log.d(Tag, "Initializing video, seek Position: " + (resumePosition != C.TIME_UNSET ? resumePosition : 0));

    if (haveResumePosition)
      player.seekTo(resumeWindow, resumePosition);
    else if (startPosition.getStartPosition() > 0)
      player.seekTo(startPosition.getStartPosition());
    else if ( (TextUtils.isEmpty(keyOfLastVideo) || !currentVideo.getKey().equals(keyOfLastVideo)) &&
     startPosition.getStartType() == StartPosition.PlaybackStartType.Ask && startPosition.getVideoOffset() > 0)
      ResumeChoiceHandler.askUser(context, player, startPosition.getVideoOffset(), CHOOSER_TIMEOUT);
    pausedTemp = true;
    keyOfLastVideo = currentVideo.getKey();
    player.prepare(currentVideo, server, caller.getValidContext(), this, !haveResumePosition, false);
  }

  public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
    Log.d(Tag, "PIP Mode changed, is in PIP: " + isInPictureInPictureMode);
    server.isPIPActive(isInPictureInPictureMode);
  }

  private void updateMetadata(Context context) {

    final MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();
    metadataBuilder.putString(MediaMetadata.METADATA_KEY_MEDIA_ID, Long.toString(currentVideo.getRatingKey()) + "");
    metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, currentVideo.getPlaybackTitle(context));
    metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, currentVideo.getPlaybackSubtitle(context, false));
    metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION, currentVideo.getPlaybackDescription(context));
    metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI, currentVideo.getPlaybackImageURL());
    metadataBuilder.putLong(MediaMetadata.METADATA_KEY_DURATION, currentVideo.getDurationMs());

    // And at minimum the title and artist for legacy support
    metadataBuilder.putString(MediaMetadata.METADATA_KEY_TITLE, currentVideo.getTitle());
    metadataBuilder.putString(MediaMetadata.METADATA_KEY_ARTIST, currentVideo.getStudio());

    Resources res = context.getResources();
    int cardWidth = res.getDimensionPixelSize(R.dimen.POSTER_WIDTH);
    int cardHeight = res.getDimensionPixelSize(R.dimen.POSTER_HEIGHT);

    String url = "";
    if (currentVideo != null) {

      if (currentVideo instanceof UpnpItem)
        url = currentVideo.getPlaybackImageURL();
      else if (server != null)
        url = server.makeServerURL(currentVideo.getPlaybackImageURL());
    }
    Glide.with(context)
     .load(Uri.parse(url))
     .asBitmap()
     .centerCrop()
     .error(context.getDrawable(R.drawable.default_video_cover))
     .into(new SimpleTarget<Bitmap>(cardWidth, cardHeight) {
       @Override
       public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
         metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, bitmap);
         caller.updateMetadata(currentVideo, bitmap, server, tracks, metadataBuilder.build());
       }
     });
  }

  public void handlePlaybackChanged(boolean isPlaying) {

    mainHandler.removeCallbacks(runnableProgress);
    if (!isPlaying)
      runnableProgress.single();
    else
      runnableProgress.repeat(true);

    setNextUpPopup();
  }

  private void setNextUpPopup() {
    synchronized (this) {

      Log.d(Tag, "Next up released (setup)");
      mainHandler.removeCallbacks(nextUpRunnable);
      if (nextVideo != null && currentVideo != null && player != null && isPlaying()) {
        long time = currentVideo.getNextUpThresholdTrigger(caller.getValidContext());
        long pos = getCurrentPosition();
        if (time != NEXTUP_DISABLED && pos < time) {
          long delay = time - pos;
          Log.d(Tag, "Next up SET for : " + (delay / 1000) + " seconds");
          mainHandler.postDelayed(nextUpRunnable, delay);
        }
      }
    }
  }

  private void showToast(int id, String additionalMessage) {
    Context context = caller.getValidContext();
    if (context == null)
      return;
    Toast.makeText(context.getApplicationContext(), context.getString(id) + additionalMessage, Toast.LENGTH_LONG).show();
  }

  public void updateResumePosition() {
    resumeWindow = player.getCurrentWindowIndex();
    resumePosition = player.isCurrentWindowSeekable() ? Math.max(0, player.getCurrentPosition())
     : C.TIME_UNSET;
  }

  public void clearResumePosition() {
    resumeWindow = C.INDEX_UNSET;
    resumePosition = C.TIME_UNSET;
  }

  @Override
  public void handlePostTaskUI(Boolean result, PlexServerTask task) {
    Context context = caller.getValidContext();
    if (context == null)
      return;

    if (task instanceof GetVideoTask) {
      PlexVideoItem item = ((GetVideoTask) task).getVideo();
      if (item.selectedHasMissingData()) {
        new GetFullInfo(intent).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, item.getKey());
        return;
      }
      MediaTrackSelector readTracks = item.fillTrackSelector(Locale.getDefault().getISO3Language(), MediaCodecCapabilities.getInstance(context));
      MediaTrackSelector chosenTracks = intent != null ? (MediaTrackSelector) intent.getParcelableExtra(PlayerActivity.TRACKS) : null;
      loadChangedTracks = chosenTracks != null;
      playVideo(((GetVideoTask) task).getVideo(), chosenTracks,  readTracks);
    }
    else if (task instanceof GetVideoQueueTask) {
      GetVideoQueueTask queueTask = (GetVideoQueueTask) task;
      synchronized (this) {
        if (queueTask.mCurrent != null && queueTask.mCurrent.getRatingKey() == currentVideo.getRatingKey()) {
          previousVideo = queueTask.mPrevious;
          nextVideo = queueTask.mNext;
          setNextUpPopup();
        }
      }
    }
  }

  private class GetFullInfo extends AsyncTask<String, Void, MediaContainer> {

    final Intent intent;

    GetFullInfo(Intent intent) {
      this.intent = intent;
    }

    @Override
    protected MediaContainer doInBackground(String... params) {

      if (params == null || params.length == 0 || params[0] == null)
        return null;
      return server.getVideoMetadata(params[0], false);
    }

    @Override
    protected void onPostExecute(MediaContainer result) {

      if (result != null) {
        Context context = caller.getValidContext();
        PlexVideoItem item = PlexVideoItem.getItem(result.getVideos().get(0));
        if (tracks == null && item != null)
          tracks = item.fillTrackSelector(Locale.getDefault().getISO3Language(), MediaCodecCapabilities.getInstance(context));

        MediaTrackSelector chosenTracks = intent != null ? (MediaTrackSelector) intent.getParcelableExtra(PlayerActivity.TRACKS) : null;
        playVideo(item, chosenTracks, tracks);
      }
    }
  }

  public void play(boolean play) {
    if (player != null) {
      if (play != isPlaying()) {
        Log.i(Tag, "Set playback to : " + (play ? "Play" : "Pause"));
        player.setPlayWhenReady(play);
        updateResumePosition();
        caller.onPlayback(play);
      }
    }
  }

  public boolean mute(boolean enable) {
    if (player != null) {
      Log.i(Tag, "Muting");
      player.setVolume(enable ? 0.0f : 1.0f);
      return true;
    }
    return false;
  }

  public boolean isPlaying() {
    if (player == null)
      return false;
    int playbackState = player.getPlaybackState();
    return (playbackState == STATE_READY || playbackState == STATE_BUFFERING) && player.getPlayWhenReady();
  }

  public String getCurrentKey() {

    if (isPlaying() && currentVideo != null)
      return currentVideo.getKey();
    return "";
  }

  private void setVideoIntent(PlexVideoItem video) {
    Intent intent = new Intent();
    intent.setAction(PlayerActivity.ACTION_VIEW);
    intent.putExtra(PlayerActivity.KEY, video.getKey());
    intent.putExtra(PlayerActivity.VIDEO, video);
    caller.onNewIntent(intent);
  }

  private boolean playPrevious() {
    Log.i(Tag, "Playing Previous");
    setVideoIntent(previousVideo);
    return playVideo(previousVideo);
  }

  public boolean playNext() {
    Log.i(Tag, "Playing Next");
    PlexVideoItem video = currentVideo;
    setVideoIntent(nextVideo);
    boolean ret = playVideo(nextVideo);
    if (ret && video != null && server != null)
      VideoProgressTask.getTask(server, currentVideo).setProgress(true, currentVideo.getDuration());
    return ret;
  }

  private boolean playVideo(PlexVideoItem video) {

    releaseSelectView();
    if (player == null || video == null)
      return false;

    mainHandler.removeCallbacks(runnableProgress);
    play(false);
    clearResumePosition();
    playVideo(video, null);
    return true;
  }

  public void updateRecommendations(PlexVideoItem currentVideo) {

    if (currentVideo != null && !currentVideo.shouldUpdateStatusOnPlayback())
      return;

    Context context = caller.getValidContext();
    Intent intent = new Intent(context.getApplicationContext(), UpdateRecommendationsService.class);
    server.setCurrentPlayingVideoRatingKey(currentVideo != null ? currentVideo.getRatingKey() : PlexServer.INVALID_RATING_KEY);
    context.startService(intent);
  }

  public void release() {
    mainHandler.removeCallbacks(runnableProgress);
    Log.d(Tag,"Next up released (release)");
    mainHandler.removeCallbacks(nextUpRunnable);

    if (switcher != null) {
      switcher.unregister();
      switcher = null;
    }
    updateResumePosition();
    if (isPlaying())
      player.stop();
    player.release();
  }

  @Override
  public void onLoadError(IOException error) {
    String msg = error != null && !error.getMessage().isEmpty() ? " " + error.getMessage() : "";
    Context context = caller.getValidContext();
    if (context == null)
      return;
    caller.showError(context.getString(R.string.video_error_load_error) + " : " + msg);
  }

  private void checkInitialTrack(int streamId) {
    if (tracks != null) {
      Stream stream = tracks.getSelectedTrack(streamId);
      if (stream != null)
        tracks.setSelectedTrack(trackSelector, new Stream.StreamChoice(caller.getValidContext(), true, stream));
      else
        tracks.disableTrackType(trackSelector, streamId);
    }
  }

  public void tracksChanged(TrackGroupArray trackGroups) {
    if (loadChangedTracks) {
      loadChangedTracks = false;
      checkInitialTrack(Stream.Audio_Stream);
      checkInitialTrack(Stream.Subtitle_Stream);
    }

    if (trackGroups != lastSeenTrackGroupArray) {
      MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
      if (mappedTrackInfo != null) {
        Context context = caller.getValidContext();
        if (context == null)
          return;
        if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_VIDEO)
         == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
          showToast(R.string.error_unsupported_video, "");
        }
        if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_AUDIO)
         == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
          showToast(R.string.error_unsupported_audio, "");
        }
      }
      lastSeenTrackGroupArray = trackGroups;
    }
  }

  public void previous() {
    if (currentVideo != null) {
      long position = BAD_CHAPTER_START;
      if (currentVideo.hasChapters())
        position = currentVideo.getPreviousChapterStart(player.getCurrentPosition());
      if (position != BAD_CHAPTER_START && player.getCurrentPosition() > START_CHAPTER_THRESHOLD)
        player.seekTo(position);
      else if (previousVideo != null && player.getCurrentPosition() < START_CHAPTER_THRESHOLD)
        playPrevious();
      else
        player.seekTo(0);
    }
  }

  public void seekTo(long pos) {
    player.seekTo(pos);
  }

  public void next() {
    if (currentVideo != null) {
      long position = BAD_CHAPTER_START;
      if (currentVideo.hasChapters())
        position = currentVideo.getNextChapterStart(player.getCurrentPosition());
      if (position != BAD_CHAPTER_START)
        player.seekTo(position);
      else if (nextVideo != null)
        playNext();
      else {
        player.stop();
        caller.exit();
      }
    }
  }

  public void jumpBack() {
    jumpTime(-1 * Long.parseLong(caller.getValidContext().getString(R.string.skip_back_seconds)));
  }

  public void jumpForward() {
    jumpTime(Long.parseLong(caller.getValidContext().getString(R.string.skip_forward_seconds)));
  }

  private void jumpTime(long seconds) {

    long pos = player.getCurrentPosition() + (seconds * 1000);
    if (pos < 0)
      pos = 0;
    else if (pos > player.getDuration())
      pos = player.getDuration();
    player.seekTo(pos);
  }

  public long getDuration() { return player == null ? 0 : player.getDuration(); }

  public long getCurrentPosition() { return player == null ? 0 :  player.getCurrentPosition(); }

  public long getTimeLeft() {  return player == null ? 0 : player.getDuration() - player.getCurrentPosition(); }

  private Runnable nextUpRunnable = new NextUpRunnable(this);
  private class NextUpRunnable implements Runnable {

    private final SelectView.SelectViewCaller viewCaller;

    NextUpRunnable(SelectView.SelectViewCaller viewCaller) {
      this.viewCaller = viewCaller;
    }

    @Override
    public void run() {

      if (nextVideo != null) {
        releaseSelectView();
        caller.showControls(false);

        Log.d(Tag,"Next up initiated");
        selectView = new NextUpView((Activity) caller.getValidContext(), server, nextVideo,
         getTimeLeft(), viewCaller);
      }
    }
  }

  public void selectTracks(Activity activity, int streamType) {

    releaseSelectView();
    selectView = SwitchTrackView.getTracksView(activity, streamType, tracks, trackSelector, server, this);
  }

  public void selectChapter(Activity activity) {

    releaseSelectView();
    selectView = new SelectChapterView(activity);

    ((SelectChapterView) selectView).setRow(currentVideo.getChildren(activity, server, null)
    , currentVideo.getCurrentChapter(player.getCurrentPosition()), this);
  }

  public boolean isShowingNextUp() {
    return selectView != null && selectView instanceof NextUpView;
  }

  public boolean releaseSelectView() {

    if (selectView != null) {
      selectView.release();
      selectView = null;
      return true;
    }
    return false;
  }

  private class NextUpView extends SelectView implements NextUpFragment.NextUpCallback {

    NextUpFragment fragment;
    NextUpView(Activity activity, PlexServer server, PlexVideoItem video, long timeLeft,
               SelectViewCaller caller) {
      super(activity);
      fragment = new NextUpFragment(activity, server, video, timeLeft, this, getHeight());
      setFragment(fragment, caller);
    }

    @Override
    protected String getTag() {
      return "nextup";
    }

    @Override
    protected int getHeight() { return 600; }

    @Override

    public void release() {
      fragment.release();
      super.release();
    }

    @Override
    public void clicked(Clicked button) {

      release();
      switch(button) {

        case StartNext:
          playNext();
          break;

        case StopList:
            nextVideo = null;
          break;

        case ShowList:
          if (currentVideo != null) {

            Activity activity = (Activity) caller.getValidContext();
            Intent intent = new Intent(activity, ContainerActivity.class);
            String key = String.format("/library/sections/%s/all", currentVideo.getSectionId());
            if (currentVideo instanceof Episode) {
              key = String.format("%s/%s", ((Episode) currentVideo).getShowKey(), Season.ALL_SEASONS);
              intent.putExtra(ContainerActivity.EPISODEIST, true);
            }
            key += "?" + server.getToken();
            intent.putExtra(ContainerActivity.KEY, key);
            intent.putExtra(ContainerActivity.BACKGROUND, currentVideo.getBackgroundImageURL());
            intent.putExtra(ContainerActivity.SELECTED, Long.toString(nextVideo.getRatingKey()));
            activity.startActivity(intent, null);
            caller.exit();
          }
          break;

        default:
          break;
      }
    }
  }

  private class SelectChapterView extends SelectViewRow {

    SelectChapterView(Activity activity) {
      super(activity);
    }

    protected  int getHeight() { return 800; }

    protected String getTag() { return "chapters"; }

    protected void cardClicked(PosterCard card) {
      player.seekTo(((Chapter) card.getItem()).getChapterStart());
    }
  }

  @Override
  public void selectionViewState(boolean isVisible) {
    if (caller != null)
      caller.selectionViewState(isVisible, currentVideo, server, tracks);
  }
}
