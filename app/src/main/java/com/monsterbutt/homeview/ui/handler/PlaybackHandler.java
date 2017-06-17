package com.monsterbutt.homeview.ui.handler;


import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
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
import com.monsterbutt.homeview.presenters.CardObject;
import com.monsterbutt.homeview.presenters.CardPresenter;
import com.monsterbutt.homeview.presenters.CodecCard;
import com.monsterbutt.homeview.presenters.PosterCard;
import com.monsterbutt.homeview.ui.PlexItemRow;
import com.monsterbutt.homeview.ui.activity.ContainerActivity;
import com.monsterbutt.homeview.ui.activity.PlayerActivity;
import com.monsterbutt.homeview.ui.fragment.NextUpFragment;
import com.monsterbutt.homeview.ui.fragment.SelectionFragment;

import java.io.IOException;
import java.util.Locale;

import us.nineworlds.plex.rest.model.impl.MediaContainer;

import static com.google.android.exoplayer2.ExoPlayer.STATE_BUFFERING;
import static com.google.android.exoplayer2.ExoPlayer.STATE_READY;
import static com.monsterbutt.homeview.plex.media.PlexVideoItem.BAD_CHAPTER_START;
import static com.monsterbutt.homeview.plex.media.PlexVideoItem.NEXTUP_DISABLED;
import static com.monsterbutt.homeview.plex.media.PlexVideoItem.START_CHAPTER_THRESHOLD;

public class PlaybackHandler implements PlexServerTaskCaller, ExtractorMediaSource.EventListener, FrameRateSwitcher.FrameRateSwitcherListener {


  public interface Invoker {

    Context getValidContext();
    void onPlayback(boolean isPlaying);
    void updateMetadata(PlexVideoItem item, Bitmap bitmap, PlexServer server, MediaTrackSelector tracks);
    void selectionViewState(boolean isVisible);
    void showControls(boolean show);
    void exit();
  }

  final static private String Tag = "PlaybackHandler";

  private static final int CHOOSER_TIMEOUT = 15 * 1000;
  private static final int PROGRESS_UPDATE_INTERVAL = 10 * 1000;

  private final PlexServer server;

  private HomeViewPlayer player = null;
  private PlexVideoItem currentVideo = null;
  private PlexVideoItem previousVideo = null;
  private PlexVideoItem nextVideo = null;

  private boolean pausedTemp = true;

  private int resumeWindow;
  private long resumePosition;

  private MediaTrackSelector tracks;

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

    public void repeat() {
      synchronized (this) {
        repeat = true;
      }
      mainHandler.postDelayed(this, PROGRESS_UPDATE_INTERVAL);
    }

    @Override
    public void run() {

      VideoProgressTask.getTask(server, currentVideo).setProgress(0 == getTimeLeft(), player.getCurrentPosition());
      boolean again;
      synchronized (this) {
        again = repeat;
      }
      if (again)
        repeat();
    }
  }
  private ProgressRunable runnableProgress = new ProgressRunable();


  public PlaybackHandler(Invoker caller, ExoPlayer.EventListener eventListener, SimpleExoPlayerView view, Handler handler) {
    this.caller = caller;
    this.mainHandler = handler;
    Context context = caller.getValidContext();
    server = PlexServerManager.getInstance(context.getApplicationContext(), null).getSelectedServer();

    Log.d(Tag, "Initializing Player");

    clearResumePosition();
    lastSeenTrackGroupArray = null;
    trackSelector = new TrackSelector();

    player = HomeViewPlayer.createPlayer(context, trackSelector, new DefaultLoadControl());
    player.addListener(eventListener);

    EventLogger eventLogger = new EventLogger(trackSelector);
    player.addListener(eventLogger);
    player.setAudioDebugListener(eventLogger);
    player.setVideoDebugListener(eventLogger);
    player.setMetadataOutput(eventLogger);

    trackSelector.setTunnelingAudioSessionId(C.generateAudioSessionIdV21(context));

    view.setPlayer(player);
    player.setPlayWhenReady(true);
  }

  public void playVideo(PlexVideoItem video, Intent intent) {

    this.intent = intent;
    long startOffset = intent != null ? intent.getLongExtra(PlayerActivity.START_OFFSET, -1) : -1;
    MediaTrackSelector chosenTracks = intent != null ? (MediaTrackSelector) intent.getParcelableExtra(PlayerActivity.TRACKS) : null;
    if (video == null)
      new GetVideoTask(this, server).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
       intent != null ? intent.getStringExtra(PlayerActivity.KEY) : "");
    else if (video.selectedHasMissingData())
      new GetFullInfo(intent).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, video.getKey());
    else
      playVideo(video, startOffset, chosenTracks,  null);
  }

  private void playVideo(PlexVideoItem video, long startOffset, MediaTrackSelector chosenTracks, MediaTrackSelector readTracks) {
    Context context = caller.getValidContext();
    if (context == null)
      return;
    else if (player == null)
      return;
    else if (video == null) {
      player.stop();
      return;
    } else if (currentVideo != null && video.getRatingKey() == currentVideo.getRatingKey())
      return;

    loadChangedTracks = chosenTracks != null;
    tracks = loadChangedTracks ? chosenTracks : readTracks;

    synchronized (this) {
      mainHandler.removeCallbacks(nextUpRunnable);
      currentVideo = video;
      StartPosition startPosition = new StartPosition(context, startOffset, video.getViewedOffset());

      Log.d(Tag, "Playing video, seek Position: " + resumePosition);
      boolean haveResumePosition = resumeWindow != C.INDEX_UNSET;
      if (haveResumePosition)
        player.seekTo(resumeWindow, resumePosition);
      else if (startPosition.getStartPosition() > 0)
        player.seekTo(startPosition.getStartPosition());
      else if (startPosition.getStartType() == StartPosition.PlaybackStartType.Ask
       && startPosition.getVideoOffset() > 0)
        ResumeChoiceHandler.askUser(context, player, startPosition.getVideoOffset(), CHOOSER_TIMEOUT);

      updateMetadata(context);

      if (switcher != null)
        switcher.unregister();
      switcher = new FrameRateSwitcher((Activity) context, this);
      if (!switcher.setDisplayRefreshRate(video))
        initiatePlayOfVideo(haveResumePosition);
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
  }

  public boolean shouldStart() {
    boolean ret = false;
    if (pausedTemp) {
      pausedTemp = false;
      ret = true;
    }
    return ret;
  }

  private void initiatePlayOfVideo(boolean haveResumePosition) {

    player.setPlayWhenReady(false);
    pausedTemp = true;
    player.prepare(currentVideo, server, caller.getValidContext(), this, !haveResumePosition, false);
    caller.onPlayback(true);
  }

  public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
    Log.d(Tag, "PIP Mode changed, is in PIP: " + isInPictureInPictureMode);
    server.isPIPActive(isInPictureInPictureMode);
  }

  private void updateMetadata(Context context) {

    final MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder();
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
         caller.updateMetadata(currentVideo, bitmap, server, tracks);
       }
     });
  }

  public void handlePlaybackChanged(boolean isPlaying) {

    mainHandler.removeCallbacks(runnableProgress);
    if (!isPlaying)
      runnableProgress.single();
    else
      runnableProgress.repeat();

    setNextUpPopup();
  }

  private void setNextUpPopup() {
    synchronized (this) {
      mainHandler.removeCallbacks(nextUpRunnable);
      if (nextVideo != null && currentVideo != null && player != null && isPlaying()) {
        long time = currentVideo.getNextUpThresholdTrigger(caller.getValidContext());
        if (time != NEXTUP_DISABLED)
          mainHandler.postDelayed(nextUpRunnable, time);
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
      MediaTrackSelector readTracks = item.fillTrackSelector(context,
       Locale.getDefault().getISO3Language(), MediaCodecCapabilities.getInstance(context));
      long startOffset = intent != null ? intent.getLongExtra(PlayerActivity.START_OFFSET, -1) : -1;
      MediaTrackSelector chosenTracks = intent != null ? (MediaTrackSelector) intent.getParcelableExtra(PlayerActivity.TRACKS) : null;
      loadChangedTracks = chosenTracks != null;
      playVideo(((GetVideoTask) task).getVideo(), startOffset, chosenTracks,  readTracks);
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
      return server.getVideoMetadata(params[0]);
    }

    @Override
    protected void onPostExecute(MediaContainer result) {

      if (result != null) {
        Context context = caller.getValidContext();
        PlexVideoItem item = PlexVideoItem.getItem(result.getVideos().get(0));
        if (tracks == null && item != null)
          tracks = item.fillTrackSelector(context,
           Locale.getDefault().getISO3Language(), MediaCodecCapabilities.getInstance(context));

        long startOffset = intent != null ? intent.getLongExtra(PlayerActivity.START_OFFSET, -1) : -1;
        MediaTrackSelector chosenTracks = intent != null ? (MediaTrackSelector) intent.getParcelableExtra(PlayerActivity.TRACKS) : null;
        playVideo(item, startOffset, chosenTracks, tracks);
      }
    }
  }

  public void play(boolean play) {
    if (player != null) {
      player.setPlayWhenReady(play);
      caller.onPlayback(play);
    }
  }

  public boolean isPlaying() {

    int playbackState = player.getPlaybackState();
    return (playbackState == STATE_READY || playbackState == STATE_BUFFERING) && player.getPlayWhenReady();
  }

  public void release() {

    mainHandler.removeCallbacks(nextUpRunnable);
    releaseSelectView();
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
    showToast(R.string.video_error_load_error, msg);
  }

  private void checkInitialTrack(int streamId, int trackType) {

    int currentTrackId = trackSelector.getSelectedTrackId(trackType);
    Stream stream = tracks.getSelectedTrack(streamId);
    if (stream != null) {
      int wantedIndex = Integer.parseInt(stream.getIndex());
      if (currentTrackId != (wantedIndex + 1))
        tracks.setSelectedTrack(trackSelector, streamId, wantedIndex-1);
    }
    else if (currentTrackId != -1)
      tracks.disableTrackType(trackSelector, streamId);
  }

  public void tracksChanged(TrackGroupArray trackGroups) {

    if (loadChangedTracks) {

      loadChangedTracks = false;
      checkInitialTrack(Stream.Audio_Stream, C.TRACK_TYPE_AUDIO);
      checkInitialTrack(Stream.Subtitle_Stream, C.TRACK_TYPE_TEXT);
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
      else if (previousVideo != null) {
        play(false);
        pausedTemp = true;
        playVideo(previousVideo, null);
      }
      else
        player.seekTo(0);
    }
  }

  public void next() {
    if (currentVideo != null) {
      long position = BAD_CHAPTER_START;
      if (currentVideo.hasChapters())
        position = currentVideo.getNextChapterStart(player.getCurrentPosition());
      if (position != BAD_CHAPTER_START)
        player.seekTo(position);
      else if (nextVideo != null)
        playVideo(nextVideo, null);
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

  public long getTimeLeft() {
    return player.getDuration() - player.getCurrentPosition();
  }

  private Runnable nextUpRunnable = new Runnable() {
    @Override
    public void run() {

      if (nextVideo != null) {
        releaseSelectView();
        caller.showControls(false);
        selectView = new NextUpView((Activity) caller.getValidContext(), server, nextVideo, getTimeLeft());
      }
    }
  };

  public void selectTracks(Activity activity, int streamType) {

    releaseSelectView();
    selectView = new SwitchTrackView(activity, streamType);

    String header = activity.getString(streamType == Stream.Audio_Stream ?
     R.string.exo_controls_audio_description : R.string.exo_controls_subtitles_description);
    ((SwitchTrackView) selectView).setRow(PlexItemRow.buildCodecItemsRow(activity, server, header,
                                      tracks.getTracks(activity, server, streamType), streamType));
  }

  public void selectChapter(Activity activity) {

    releaseSelectView();
    selectView = new SelectChapterView(activity);
    ((SelectChapterView) selectView).setRow(currentVideo.getChildren(activity, server, (SelectChapterView) selectView));
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

  private abstract class SelectView {

    protected final Activity activity;
    private Fragment fragment = null;

    SelectView(Activity activity) {

      this.activity = activity;
    }

    protected abstract String getTag();
    protected abstract int getHeight();

    protected void setFragment(Fragment fragment) {

      this.fragment = fragment;

      if (fragment != null) {
        caller.selectionViewState(true);
        View view = activity.findViewById(R.id.selection_fragment);
        view.setVisibility(View.VISIBLE);
        view.requestFocus();
        activity.getFragmentManager().beginTransaction().add(R.id.selection_fragment, fragment, getTag()).commit();
      }
      else
        release();
    }

    public void release() {

      if (fragment != null && activity != null && !activity.isDestroyed() && !activity.isFinishing()) {
        activity.runOnUiThread(new Runnable() {
          @Override
          public void run() {
            activity.getFragmentManager().beginTransaction().remove(fragment).commit();
            activity.findViewById(R.id.selection_fragment).setVisibility(View.INVISIBLE);
          }
        });
      }
      caller.selectionViewState(false);
    }
  }

  private class NextUpView extends SelectView implements NextUpFragment.NextUpCallback {

    NextUpView(Activity activity, PlexServer server, PlexVideoItem video, long timeLeft) {
      super(activity);
      setFragment(new NextUpFragment(activity, server, video, timeLeft, this, getHeight()));
    }

    @Override
    protected String getTag() {
      return "nextup";
    }

    @Override
    protected int getHeight() { return 600; }

    @Override
    public void clicked(Clicked button) {

      release();
      switch(button) {

        case StartNext:
          playVideo(nextVideo, null);
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
              intent.putExtra(ContainerActivity.USE_SCENE, true);
            }
            key += "?" + server.getToken();
            intent.putExtra(ContainerActivity.KEY, key);
            intent.putExtra(ContainerActivity.BACKGROUND, currentVideo.getBackgroundImageURL());
            intent.putExtra(ContainerActivity.SELECTED, nextVideo.getRatingKey());
            activity.startActivity(intent, null);
            caller.exit();
          }
          break;

        default:
          break;
      }
    }
  }

  private abstract class SelectViewRow extends SelectView
   implements CardPresenter.CardPresenterLongClickListener, OnItemViewClickedListener {

    SelectViewRow(Activity activity) {
      super(activity);
    }

    protected abstract void cardClicked(PosterCard card);

    public void setRow(PlexItemRow row) {

      setFragment(new SelectionFragment(activity, row, this, getHeight()));
    }

    private void clicked(PosterCard card) {

      cardClicked(card);
      release();
    }

    @Override
    public boolean longClickOccured(CardObject card, CardPresenter.LongClickWatchStatusCallback callback) {

      clicked((PosterCard) card);
      return true;
    }

    @Override
    public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {

      clicked((PosterCard) item);
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


  private class SwitchTrackView extends SelectViewRow {

    final int type;

    SwitchTrackView(Activity activity, int streamType) {

      super(activity);
      type = streamType;
    }


    protected  int getHeight() { return 600; }

    protected String getTag() { return type == Stream.Audio_Stream ? "audio" : "subtitle"; }

    protected void cardClicked(PosterCard card) {

      CodecCard codec = (CodecCard) card;
      Stream.StreamChoice stream = codec.getStream();
      if (stream == null || stream.isCurrentSelection())
        return;

      if (stream instanceof Stream.StreamChoiceDisable)
        tracks.disableTrackType(trackSelector, type);
      else {
        int index = Integer.parseInt(stream.stream.getIndex()) - 1;
        tracks.setSelectedTrack(trackSelector, type, index);
      }
    }
  }

}
