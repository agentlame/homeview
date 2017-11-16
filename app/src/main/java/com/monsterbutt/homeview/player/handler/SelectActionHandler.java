package com.monsterbutt.homeview.player.handler;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.media.PlaybackTransportControlGlue;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.PlaybackControlsRow;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.player.notifier.SwitchChapterNotifier;
import com.monsterbutt.homeview.player.notifier.SwitchTrackNotifier;
import com.monsterbutt.homeview.player.notifier.VideoChangedNotifier;
import com.monsterbutt.homeview.player.track.MediaTrackSelector;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.ui.presenters.CardPresenter;
import com.monsterbutt.homeview.ui.presenters.SceneCard;

import java.util.List;

import static com.monsterbutt.homeview.plex.media.Stream.Audio_Stream;
import static com.monsterbutt.homeview.plex.media.Stream.Subtitle_Stream;

public class SelectActionHandler implements VideoChangedNotifier.Observer {

  private final static int AUDIO_ID = 84765;
  private final static int CHAPTERS_ID = 84765;
  private final static int SUBTITLES_ID = 84765;

  private final Activity mActivity;
  private final PlexServer mServer;
  private final PlaybackTransportControlGlue mGlue;
  private final PlaybackControlsRow.PictureInPictureAction mPipAction;
  private final AudioSelectAction mAudioSelectAction;
  private final SubtitlesSelectAction mSubititleSelectAction;
  private final ChapterSelectAction mChapterSelectAction;

  private final SwitchTrackNotifier mSwitchTrackNotifier;
  private final SwitchChapterNotifier mSwitchChapterNotifier;

  private PlexVideoItem mCurrentVideo = null;
  private MediaTrackSelector mTracks = null;

  public SelectActionHandler(Activity activity, PlexServer server, PlaybackTransportControlGlue glue,
                             VideoChangedNotifier videoChangedNotifier,
                             SwitchTrackNotifier switchTrackNotifier,
                             SwitchChapterNotifier switchChapterNotifier) {
    mActivity = activity;
    mServer = server;
    mGlue = glue;
    mPipAction = new PlaybackControlsRow.PictureInPictureAction(mActivity);
    mAudioSelectAction = new AudioSelectAction(mActivity);
    mSubititleSelectAction = new SubtitlesSelectAction(mActivity);
    mChapterSelectAction = new ChapterSelectAction(mActivity);

    mSwitchTrackNotifier = switchTrackNotifier;
    mSwitchChapterNotifier = switchChapterNotifier;
    videoChangedNotifier.register(this);
  }

  public void onCreateActions(ArrayObjectAdapter adapter) {
    adapter.add(mPipAction);
    adapter.add(mAudioSelectAction);
    adapter.add(mSubititleSelectAction);
    adapter.add(mChapterSelectAction);
  }

  public boolean shouldDispatchAction(Action action) {
    return action == mPipAction || action == mAudioSelectAction ||
     action == mSubititleSelectAction || action == mChapterSelectAction;
  }

  @TargetApi(24)
  public void dispatchAction(Action action) {
    if (action == mPipAction)
      mActivity.enterPictureInPictureMode();
    else if (action == mAudioSelectAction) {
      if (mAudioSelectAction.enabled())
        mSwitchTrackNotifier.switchTrack(Audio_Stream, mTracks);
    } else if (action == mSubititleSelectAction) {
      if (mSubititleSelectAction.enabled())
        mSwitchTrackNotifier.switchTrack(Subtitle_Stream, mTracks);
    } else if (action == mChapterSelectAction) {
      if (mChapterSelectAction.enabled()) {

        List<PlexLibraryItem> items = mCurrentVideo.getChildrenItems();
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(new CardPresenter(mServer, null, false));
        ListRow row = new ListRow(adapter);
        for (PlexLibraryItem item: items)
          adapter.add(new SceneCard(mActivity, item));
        mSwitchChapterNotifier.switchChapter(row,
         mCurrentVideo.getCurrentChapter(mGlue.getCurrentPosition()));
      }
    }
  }

  @Override
  public void notify(PlexVideoItem item, MediaTrackSelector tracks,
                     boolean usesDefaultTracks, StartPositionHandler startPosition) {
    mCurrentVideo = item;
    mTracks = tracks;

    mAudioSelectAction.enabled(mTracks != null && mTracks.getCount(Audio_Stream)  > 0);
    mSubititleSelectAction.enabled(mTracks != null && mTracks.getCount(Subtitle_Stream) > 0);
    mChapterSelectAction.enabled(item != null && item.hasChapters());
  }

  private class SelectAction extends PlaybackControlsRow.MultiAction {

    private static final int INDEX_OFF = 0;
    private static final int INDEX_ON = 1;

    void enabled(boolean enable) {
      setIndex(enable ? INDEX_ON : INDEX_OFF);
    }

    boolean enabled() {
      return INDEX_ON == getIndex();
    }

    SelectAction(Context context, int id, int drawableId) {
      super(id);
      BitmapDrawable uncoloredDrawable = (BitmapDrawable) context.getDrawable(drawableId);
      if (uncoloredDrawable == null)
        return;
      Drawable[] drawables = new Drawable[2];
      drawables[INDEX_ON] = uncoloredDrawable;
      drawables[INDEX_OFF] = new BitmapDrawable(context.getResources(),
       createBitmap(uncoloredDrawable.getBitmap(), getIconHighlightColor(context)));
      setDrawables(drawables);
    }

    private Bitmap createBitmap(Bitmap bitmap, int color) {
      Bitmap dst = bitmap.copy(bitmap.getConfig(), true);
      Canvas canvas = new Canvas(dst);
      Paint paint = new Paint();
      paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
      canvas.drawBitmap(bitmap, 0, 0, paint);
      return dst;
    }

    private int getIconHighlightColor(Context context) {
      return context.getResources().getColor(R.color.light_grey);
    }
  }

  private class AudioSelectAction extends SelectAction {

    AudioSelectAction(Context context) {
      super(context, AUDIO_ID, R.drawable.ic_audiotrack_white_24dp);
    }
  }

  private class ChapterSelectAction extends SelectAction {

    ChapterSelectAction(Context context) {
      super(context, CHAPTERS_ID, R.drawable.ic_playlist_play_white_24dp);
    }
  }

  private class SubtitlesSelectAction extends SelectAction {

    SubtitlesSelectAction(Context context) {
      super(context, SUBTITLES_ID, R.drawable.ic_subtitles_white_24dp);
    }
  }
}
