package com.monsterbutt.homeview.player.handler;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v17.leanback.app.VideoFragment;
import android.support.v17.leanback.media.PlaybackGlue;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.player.display.DesiredVideoMode;
import com.monsterbutt.homeview.player.display.FrameRateSwitcher;
import com.monsterbutt.homeview.player.HomeViewExoPlayerAdapter;
import com.monsterbutt.homeview.player.notifier.ChapterSelectionNotifier;
import com.monsterbutt.homeview.player.notifier.FrameRateSwitchNotifier;
import com.monsterbutt.homeview.player.track.MediaCodecCapabilities;
import com.monsterbutt.homeview.player.track.MediaTrackSelector;
import com.monsterbutt.homeview.player.notifier.QueueChangeNotifier;
import com.monsterbutt.homeview.player.notifier.VideoChangedNotifier;
import com.monsterbutt.homeview.player.VideoMediaPlayerGlue;
import com.monsterbutt.homeview.plex.PlexServer;
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
import com.monsterbutt.homeview.settings.SettingsManager;
import com.monsterbutt.homeview.ui.C;
import com.monsterbutt.homeview.ui.grid.GridActivity;
import com.monsterbutt.homeview.ui.playback.PlaybackActivity;

import java.util.Locale;

import us.nineworlds.plex.rest.model.impl.Media;
import us.nineworlds.plex.rest.model.impl.MediaContainer;

import static com.monsterbutt.homeview.plex.media.PlexVideoItem.BAD_CHAPTER_START;
import static com.monsterbutt.homeview.plex.media.PlexVideoItem.START_CHAPTER_THRESHOLD;
import static com.monsterbutt.homeview.ui.playback.PlaybackActivity.ACTION;
import static com.monsterbutt.homeview.ui.playback.PlaybackActivity.KEY;
import static com.monsterbutt.homeview.ui.playback.PlaybackActivity.URI;

public class VideoHandler implements PlexServerTaskCaller, VideoChangedNotifier.Observer,
 ChapterSelectionNotifier.Observer, FrameRateSwitchNotifier.Observer {

  private static final String TAG = "HV_GlueVideoHandler";

  private final PlexServer mServer;
  private final VideoFragment mFragment;
  private final VideoMediaPlayerGlue mGlue;
  private final VideoChangedNotifier mVideoChangedNotifier;
  private final QueueChangeNotifier mQueueChangedNotifier;
  private final VideoHolder mVideos = new VideoHolder();

  private long currStartOffset = -1;
  private boolean didFirstPlay = false;

  public VideoHandler(VideoFragment fragment, VideoMediaPlayerGlue glue, PlexServer server,
                      VideoChangedNotifier videoChangedNotifier,
                      QueueChangeNotifier queueChangeNotifier, ChapterSelectionNotifier chapterSelectionNotifier) {
    mFragment = fragment;
    mGlue = glue;
    mServer = server;
    mQueueChangedNotifier = queueChangeNotifier;
    mVideoChangedNotifier = videoChangedNotifier;
    if (mVideoChangedNotifier != null)
      mVideoChangedNotifier.register(this);
    if (chapterSelectionNotifier != null)
      chapterSelectionNotifier.register(this);
  }

  private boolean setVideoIntent(PlexVideoItem video) {

    if (video == null)
      return false;
    Intent intent = new Intent();
    intent.setAction(PlaybackActivity.ACTION_VIEW);
    intent.putExtra(PlaybackActivity.KEY, video.getKey());
    intent.putExtra(PlaybackActivity.VIDEO, video);
    ((FragmentActivity) mGlue.getContext()).setIntent(intent);
    return mGlue.parseIntent(intent);
  }

  private boolean playIntentVideo(String key, PlexVideoItem video, MediaTrackSelector chosenTracks) {

    if (video == null) {
      if (TextUtils.isEmpty(key))
        return false;
      new GetVideoTask(this, mServer)
       .setChosenTracks(chosenTracks)
       .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, key);
      return true;
    }
    new GetFullInfo(this, mServer, chosenTracks).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, video.getKey());
    return true;
  }

  public boolean parseIntent(Intent intent) {
    if (intent == null)
      return false;
    String action = intent.getAction();
    Uri data = intent.getData();
    if (action != null && action.equals(PlaybackActivity.ACTION_VIEW) && data != null && data.toString().startsWith(URI)) {
      intent.putExtra(ACTION, PlaybackActivity.ACTION_VIEW);
      intent.putExtra(KEY, data.toString().substring(URI.length()));
    }
    if (PlaybackActivity.ACTION_VIEW.equals(action) ||
     (!TextUtils.isEmpty(intent.getStringExtra(ACTION)) &&
      PlaybackActivity.ACTION_VIEW.equals(intent.getStringExtra(ACTION)))) {
      Log.d(PlaybackActivity.Tag, "Playing video from Action_View");
      currStartOffset = intent.getLongExtra(PlaybackActivity.START_OFFSET, StartPositionHandler.START_POSITION_DEFAULT);

      return playIntentVideo(intent.getStringExtra(PlaybackActivity.KEY),
       (PlexVideoItem) intent.getParcelableExtra(PlaybackActivity.VIDEO),
       (MediaTrackSelector) intent.getParcelableExtra(PlaybackActivity.TRACKS));
    }
    return false;
  }

  private void playVideo(PlexVideoItem video, MediaTrackSelector chosenTracks) {

    PlexVideoItem currentVideo = mVideos.currentVideo();
    if (video == null || (currentVideo != null && video.getKey().equals(currentVideo.getKey())))
      return;

    boolean usesDefaultTracks = chosenTracks == null;
    MediaTrackSelector tracks = !usesDefaultTracks ? chosenTracks :
     video.fillTrackSelector(Locale.getDefault().getISO3Language(), MediaCodecCapabilities.getInstance(mGlue.getContext()));
    mVideoChangedNotifier.videoChanged(video, tracks, usesDefaultTracks,
     new StartPositionHandler(currStartOffset, video.getViewedOffset()));
  }

  @Override
  public void notify(PlexVideoItem video, MediaTrackSelector tracks, boolean usesDefaulTracks,
                     StartPositionHandler startPosition) {

    if (video == null)
      return;

    if (mVideos.currentVideo() == null) {
      mGlue.addPlayerCallback(new PlaybackGlue.PlayerCallback() {
        @Override
        public void onPreparedStateChanged(PlaybackGlue glue) {
          if (glue.isPrepared()) {
            glue.removePlayerCallback(this);
            glue.play();
          }
        }
      });
    }

    didFirstPlay = false;
    mVideos.currentVideo(video, tracks, usesDefaulTracks);

    Context context = mGlue.getContext();
    mGlue.setTitle(video.getPlaybackTitle(context));
    mGlue.setSubtitle(video.getPlaybackSubtitle(context, false));

    String artURL = "";
    if (video instanceof UpnpItem)
      artURL = video.getPlaybackImageURL();
    else if (mServer != null)
      artURL = mServer.makeServerURL(video.getPlaybackImageURL());
    final Resources res = context.getResources();
    final int cardWidth = res.getDimensionPixelSize(R.dimen.POSTER_WIDTH);
    final int cardHeight = res.getDimensionPixelSize(R.dimen.POSTER_HEIGHT);
    Glide.with(context)
     .load(Uri.parse(artURL))
     .asBitmap()
     .error(context.getDrawable(R.drawable.default_video_cover))
     .into(new SimpleTarget<Bitmap>(cardWidth, cardHeight) {
       @Override
       public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
         mGlue.setArt(new BitmapDrawable(res, bitmap));
       }
     });

    if (!setRefreshRateToCurrentVideo((Activity) mGlue.getContext(), false))
      mVideos.setCurrentDataSource((HomeViewExoPlayerAdapter) mGlue.getPlayerAdapter());
  }

  public void shouldDoFirstPlay() {

    if (didFirstPlay)
      return;
    didFirstPlay = true;
    ((HomeViewExoPlayerAdapter) mGlue.getPlayerAdapter()).checkInitialTrack(Stream.Subtitle_Stream);
    PlexVideoItem currentVideo = mVideos.currentVideo();
    new GetVideoQueueTask(this, mServer).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, currentVideo.getKey());

    StartPositionHandler startPosition = new StartPositionHandler(currStartOffset, currentVideo.getViewedOffset());
    long startPos = startPosition.getStartPosition();
    if (startPos > 0)
        mGlue.seekTo(startPos);
  }

  public boolean playPrevious() {

    PlexVideoItem previousVideo = mVideos.previousVideo();
    if(previousVideo == null || mGlue.getCurrentPosition() < START_CHAPTER_THRESHOLD)
      return false;

    Log.i(TAG, "Playing Previous");
    mGlue.pause();
    return setVideoIntent(previousVideo);
  }

  public boolean playNext() {

    PlexVideoItem nextVideo = mVideos.nextVideo();
    if (nextVideo == null) {
      Log.i(TAG, "No next video");
      return false;
    }
    Log.i(TAG, "Playing Next");
    mGlue.pause();
    setVideoIntent(nextVideo);
    return true;
  }

  synchronized void cancelNext() {
    mVideos.nextVideo(null);
  }

  @Override
  public void handlePostTaskUI(Boolean result, PlexServerTask task) {

    if (task instanceof GetVideoTask) {
      GetVideoTask videoTask = (GetVideoTask) task;
      PlexVideoItem item = videoTask.getVideo();
      if (item != null && item.selectedHasMissingData()) {
        new GetFullInfo(this, mServer, videoTask.getChosenTracks()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, item.getKey());
        return;
      }
      playVideo(item, videoTask.getChosenTracks());
    }
    else if (task instanceof GetFullInfo) {
      GetFullInfo infoTask = (GetFullInfo) task;
      playVideo(infoTask.getVideo(), infoTask.chosenTracks);
    }
    else if (task instanceof GetVideoQueueTask) {
      GetVideoQueueTask queueTask = (GetVideoQueueTask) task;
      synchronized (this) {
        if (queueTask.mCurrent != null &&
         queueTask.mCurrent.getRatingKey() == mVideos.currentVideo().getRatingKey()) {
          synchronized (this) {
            mVideos.previousVideo(queueTask.mPrevious);
            mVideos.nextVideo(queueTask.mNext);
          }
          mQueueChangedNotifier.queueChanged(queueTask.mPrevious, queueTask.mCurrent, queueTask.mNext);
        }
      }
    }
  }

  public boolean hasVideo() {
    return mVideos.currentVideo() != null;
  }

  public long getPreviousChapterPosition(long currPosition) {
    PlexVideoItem video = mVideos.currentVideo();
    return (video != null && video.hasChapters()) ?
     video.getPreviousChapterStart(currPosition): BAD_CHAPTER_START;
  }

  public long getNextChapterPosition(long currPosition) {
    PlexVideoItem video = mVideos.currentVideo();
    return (video != null && video.hasChapters()) ?
     video.getNextChapterStart(currPosition): BAD_CHAPTER_START;
  }

  void showQueue() {
    PlexVideoItem currentVideo = mVideos.currentVideo();
    if (currentVideo != null) {

      Activity activity = mFragment.getActivity();
      Intent intent = new Intent(activity, GridActivity.class);
      String key = String.format("/library/sections/%s/all", currentVideo.getSectionId());
      if (currentVideo instanceof Episode) {
        key = String.format("%s/%s", ((Episode) currentVideo).getShowKey(), Season.ALL_SEASONS);
        intent.putExtra(C.EPISODEIST, true);
      }
      key += "?" + mServer.getToken();
      intent.putExtra(C.KEY, key);
      intent.putExtra(C.BACKGROUND, currentVideo.getBackgroundImageURL());
      intent.putExtra(C.SELECTED, Long.toString(mVideos.nextVideo.getRatingKey()));
      activity.startActivity(intent, null);
      mVideoChangedNotifier.finish();
    }
  }

  PlexVideoItem getCurrentVideo() { return mVideos.currentVideo(); }

  PlexVideoItem getNextVideo() { return mVideos.nextVideo(); }

  @Override
  public void chapterSelected(Chapter chapter) {
    if (getCurrentVideo() != null && chapter != null)
      mGlue.seekTo(chapter.getChapterStart());
  }

  @Override
  public void frameRateSwitched(long requestedDelay) {

    final Handler handler = new Handler();
    handler.postDelayed(new Runnable() {
        @Override
        public void run() {
          handler.removeCallbacks(this);
          mVideos.setCurrentDataSource((HomeViewExoPlayerAdapter) mGlue.getPlayerAdapter());
        }
      }, requestedDelay);
  }

  private static class GetFullInfo extends PlexServerTask {

    private PlexVideoItem mVideo = null;
    public PlexVideoItem getVideo() { return mVideo; }

    final MediaTrackSelector chosenTracks;

    GetFullInfo(PlexServerTaskCaller caller, PlexServer server, MediaTrackSelector chosenTracks) {
      super(caller, server);
      this.chosenTracks = chosenTracks;
    }

    @Override
    protected Boolean doInBackground(Object... params) {

      if (params == null || params.length == 0 || !(params[0] instanceof String))
        return false;
      MediaContainer mc = getServer().getVideoMetadata((String) params[0]);
      if (mc != null && mc.getVideos() != null)
        mVideo = PlexVideoItem.getItem(mc.getVideos().get(0));
      return mVideo != null;
    }
  }

  public boolean setRefreshRateToCurrentVideo(Activity activity, boolean force) {
    PlexVideoItem video = getCurrentVideo();
    SettingsManager settings = SettingsManager.getInstance();
    if (!settings.getBoolean("preferences_device_refreshrate") ||
     video == null || !video.hasSourceStats() || video.getMedia() == null)
      return false;
    Media media = video.getMedia().get(0);
    if (media == null || media.getVideoPart() == null || media.getVideoPart().get(0) == null ||
     media.getVideoPart().get(0).getStreams() == null)
      return false;
    us.nineworlds.plex.rest.model.impl.Stream videoStream = null;
    for (us.nineworlds.plex.rest.model.impl.Stream stream :  media.getVideoPart().get(0).getStreams()) {
      if (stream.getStreamType() == Stream.Video_Stream) {
        videoStream = stream;
        break;
      }
    }
    if (videoStream == null)
      return false;
    FrameRateSwitchNotifier notifier = new FrameRateSwitchNotifier();
    notifier.register(this);
    boolean allowUpconvert = settings.getBoolean("preferences_device_upconvert");
    return FrameRateSwitcher.setDisplayRefreshRate(activity,
     new DesiredVideoMode(video.getMedia().get(0), videoStream.getFrameRate(), allowUpconvert),
     force,
     notifier);
  }

  private class VideoHolder {

    private PlexVideoItem currentVideo = null;
    private MediaTrackSelector currentTracks = null;
    private boolean currentUsesDefaultTracks = false;
    private PlexVideoItem previousVideo = null;
    private PlexVideoItem nextVideo = null;

    synchronized PlexVideoItem currentVideo() { return currentVideo; }
    synchronized void currentVideo(PlexVideoItem item, MediaTrackSelector tracks,
                                   boolean usesDefaultTracks) {
      currentVideo = item;
      currentTracks = tracks;
      currentUsesDefaultTracks = usesDefaultTracks;
    }

    synchronized PlexVideoItem previousVideo() { return previousVideo; }
    synchronized void previousVideo(PlexVideoItem item) { previousVideo = item; }

    synchronized PlexVideoItem nextVideo() { return nextVideo; }
    synchronized void nextVideo(PlexVideoItem item) { nextVideo = item; }


    synchronized void setCurrentDataSource(HomeViewExoPlayerAdapter adapter) {
      adapter.setDataSource(currentVideo, currentTracks, currentUsesDefaultTracks, mServer);
    }

  }
}
