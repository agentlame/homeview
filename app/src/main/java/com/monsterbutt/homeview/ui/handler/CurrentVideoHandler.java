package com.monsterbutt.homeview.ui.handler;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaDescription;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ListRow;
import android.util.Pair;

import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.player.MediaCodecCapabilities;
import com.monsterbutt.homeview.player.MediaTrackSelector;
import com.monsterbutt.homeview.player.PlaybackControlHelper;
import com.monsterbutt.homeview.player.StartPosition;
import com.monsterbutt.homeview.player.TrackSelector;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.Chapter;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.plex.media.Stream;
import com.monsterbutt.homeview.plex.tasks.GetVideoQueueTask;
import com.monsterbutt.homeview.plex.tasks.GetVideoTask;
import com.monsterbutt.homeview.plex.tasks.PlexServerTask;
import com.monsterbutt.homeview.plex.tasks.PlexServerTaskCaller;
import com.monsterbutt.homeview.plex.tasks.VideoProgressTask;
import com.monsterbutt.homeview.presenters.CodecCard;
import com.monsterbutt.homeview.services.UpdateRecommendationsService;
import com.monsterbutt.homeview.ui.activity.PlaybackActivity;
import com.monsterbutt.homeview.ui.android.HomeViewActivity;
import com.monsterbutt.homeview.ui.android.NextUpView;
import com.monsterbutt.homeview.ui.fragment.PlaybackFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import us.nineworlds.plex.rest.model.impl.Media;

public class CurrentVideoHandler implements PlexServerTaskCaller,
                                            PlaybackControlHelper.ProgressUpdateCallback,
                                            Chapter.OnClickListenerHandler {

    private VideoProgressTask mProgressTask = null;
    private long              mProgressTaskLastUpdate = 0;
    private final String      mProgressLock = "progress";

    private List<MediaSession.QueueItem> mQueue;
    private int mQueueIndex = -1;
    private PlexVideoItem mSelectedVideo = null; // Video is the currently playing Video and its metadata.
    private MediaTrackSelector mSelectedVideoTracks = null;
    private final MediaSessionHandler mSessionHandler;
    private VideoPlayerHandler mVideoHandler;
    private final SubtitleHandler mSubtitleHandler;
    private final CardSelectionHandler mSelectionHandler;
    private NextUpHandler mNextUpHandler = null;
    private final HomeViewActivity mActivity;
    private final PlexServer mServer;
    private StartPosition mStartPosition;
    private long mPassedVideoKey = 0;

    public CurrentVideoHandler(PlaybackFragment fragment, PlexServer server,
                               MediaSessionHandler sessionHandler,
                               SubtitleHandler subtitleHandler) {

        mActivity = (HomeViewActivity) fragment.getActivity();
        mSessionHandler = sessionHandler;
        mSubtitleHandler = subtitleHandler;
        mSubtitleHandler.setHandler(this);
        mServer = server;
        mQueue = new ArrayList<>();

        mSelectionHandler = new CardSelectionHandler(fragment, null, this, mServer);
        fragment.setOnItemViewSelectedListener(mSelectionHandler);
        fragment.setOnItemViewClickedListener(mSelectionHandler);

        Intent intent = mActivity.getIntent();
        PlexVideoItem intentVideo = intent.getParcelableExtra(PlaybackActivity.VIDEO);
        if (intentVideo != null)
            mPassedVideoKey = intentVideo.getRatingKey();
        readPassedVideo(intent, intentVideo);
    }

    private void readPassedVideo(Intent intent, PlexVideoItem intentVideo) {

        mSelectedVideo = intentVideo;
        mSelectedVideoTracks = intent.getParcelableExtra(PlaybackActivity.TRACKS);
        setVideo(intentVideo, mSubtitleHandler);
        mStartPosition = new StartPosition(mActivity, intent, mSelectedVideo != null ?
                mSelectedVideo.getViewedOffset() : 0);

        String key = mSelectedVideo != null ? mSelectedVideo.getKey() : intent.getStringExtra(PlaybackActivity.KEY);
        if (mSelectedVideo == null || mSelectedVideo.shouldDiscoverQueue())
            new GetVideoQueueTask(this, mServer).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, key);
    }

    public boolean checkPassedVideo() {

        Intent intent = mActivity.getIntent();
        PlexVideoItem intentVideo = intent.getParcelableExtra(PlaybackActivity.VIDEO);
        boolean changed = mPassedVideoKey != intentVideo.getRatingKey();
        if (changed)
            readPassedVideo(intent, intentVideo);
        return changed;
    }

    @Override
    public void handlePostTaskUI(Boolean result, PlexServerTask task) {

        if (mActivity.isDestroyed() || mActivity.isFinishing())
            return;

        if (task instanceof GetVideoQueueTask) {

            mQueue.clear();
            mQueueIndex = -1;
            PlexVideoItem first = null;
            final boolean startPlayback = mSelectedVideo == null;
            List<PlexVideoItem> queue = ((GetVideoQueueTask)task).getQueue();
            for (PlexVideoItem v : queue) {

                if (first == null)
                    first = v;

                // Set the queue index to the selected video.
                if (v.shouldPlaybackFirst()) {
                    if (mSelectedVideo == null)
                        setVideo(v, mSubtitleHandler);
                    mQueueIndex = mQueue.size();
                }

                mQueue.add(getQueueItem(v));
            }

            if (mQueueIndex == -1 && !mQueue.isEmpty()) {

                if (mSelectedVideo == null && first != null) {
                    setVideo(first, mSubtitleHandler);
                    mStartPosition.setVideoOffset(mSelectedVideo.getViewedOffset());
                }
                mQueueIndex = 0;
            }

            if (mActivity.isDestroyed() || mActivity.isFinishing())
                return;
            if (startPlayback && mSelectedVideo != null)
                mVideoHandler.playVideo(mSelectedVideo);
            else if (mQueue.size() > 1 && mQueueIndex != mQueue.size()-1) {
                Pair<Long, MediaSession.QueueItem> pair = getNextVideoInQueue();
                if (pair != null)
                    mNextUpHandler.fillNextUp(pair.first, pair.second, mSelectedVideo.getDuration());
            }
            mSessionHandler.setQueue(mQueue, mActivity.getString(R.string.queue_name));
        }
        else if (task instanceof GetVideoTask)
            mVideoHandler.playVideo(((GetVideoTask) task).getVideo());
    }

    public String getPath() {

        String ret;
        synchronized (this) {
            ret = mSelectedVideo.getVideoPath(mServer);
        }
        return ret;
    }

    public PlexVideoItem getVideo() {

        PlexVideoItem ret;
        synchronized (this) {
            ret = mSelectedVideo;
        }
        return ret;
    }

    public NextUpHandler getNextUpHandler() { return mNextUpHandler; }

    public long getStartPosition() { return mStartPosition.getStartPosition(); }

    public long getLastViewedPosition() { return mStartPosition.getVideoOffset(); }

    public StartPosition.PlaybackStartType getPlaybackStartType() { return mStartPosition.getStartType(); }

    public void setVideo(PlexVideoItem video, SubtitleHandler subtitleHandler) {

        if (mNextUpHandler != null)
            mNextUpHandler.dismiss();
        mSelectedVideo = video;
        if (mSelectedVideo != null) {

            if (mSelectedVideoTracks == null) {
                mSelectedVideoTracks = mSelectedVideo.fillTrackSelector(mActivity,
                        Locale.getDefault().getISO3Language(), MediaCodecCapabilities.getInstance(mActivity));
            }

            if (mSelectedVideo.hasSourceStats()) {

                Media media = mSelectedVideo.getMedia().get(0);
                subtitleHandler.setSourceStatus(Integer.valueOf(media.getHeight()), Integer.valueOf(media.getWidth()));
            }
        }
    }

    private MediaSession.QueueItem getQueueItem(PlexVideoItem v) {

        Bundle bundle = new Bundle();
        bundle.putString(NextUpView.BUNDLE_EXTRA_SUMMARY, v.getSummary());
        bundle.putString(NextUpView.BUNDLE_EXTRA_CONTENT, v.getDetailContent(mActivity));
        bundle.putString(NextUpView.BUNDLE_EXTRA_STUDIO, v.getDetailStudioPath(mServer));
        bundle.putString(NextUpView.BUNDLE_EXTRA_RATING, v.getDetailRatingPath(mServer));
        bundle.putString(NextUpView.BUNDLE_EXTRA_TITLE, v.getDetailTitle(mActivity));
        bundle.putString(NextUpView.BUNDLE_EXTRA_SUBTITLE, v.getDetailSubtitle(mActivity));
        bundle.putString(NextUpView.BUNDLE_EXTRA_MINUTES, v.getDetailDuration(mActivity));
        bundle.putString(NextUpView.BUNDLE_THUMB, v.getWideCardImageURL());
        bundle.putLong(NextUpView.BUNDLE_EXTRA_VIEWEDOFFSET, v.getViewedOffset());

        MediaDescription desc = new MediaDescription.Builder()
                .setDescription(v.getPlaybackSubtitle(mActivity))
                .setExtras(bundle)
                .setMediaId(Long.toString(v.getRatingKey()) + "")
                .setMediaUri(Uri.parse(v.getVideoPath(mServer)))
                .setIconUri(Uri.parse(v.getPlaybackImageURL()))
                .setSubtitle(v.getPlaybackSubtitle(mActivity))
                .setTitle(v.getPlaybackTitle(mActivity))
                .build();

        return new MediaSession.QueueItem(desc, v.getKey().hashCode());
    }

    public void createQueue(String key) {

        if (mSelectedVideo.shouldDiscoverQueue())
            new GetVideoTask(this, mServer).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, key);
    }

    public void updateRecommendations(Activity activity, boolean excludeSelf) {

        if (mSelectedVideo == null || !mSelectedVideo.shouldUpdateStatusOnPlayback())
            return;

        Intent intent = new Intent(activity.getApplicationContext(), UpdateRecommendationsService.class);
        if (!excludeSelf)
            intent.putExtra(UpdateRecommendationsService.EXCLUDE_VIDEO, mSelectedVideo.getRatingKey());
        activity.startService(intent);
    }

    public void setTracks(TrackSelector selector) {

        if (mSelectedVideoTracks != null) {
            mSelectedVideoTracks.setSelectedTrack(selector, Stream.Audio_Stream,
                    mSelectedVideoTracks.getSelectedTrackDisplayIndex(Stream.Audio_Stream));
            mSelectedVideoTracks.setSelectedTrack(selector, Stream.Subtitle_Stream,
                    mSelectedVideoTracks.getSelectedTrackDisplayIndex(Stream.Subtitle_Stream));
        }
    }

    public String getNextVideoInQueueKey() {

        String key = "";
        synchronized (this) {
            if (mQueue != null && mQueueIndex >= 0 && mQueueIndex+1 < mQueue.size())
                key = mQueue.get(mQueueIndex+1).getDescription().getMediaId();
        }
        return key;
    }

    public Pair<Long, MediaSession.QueueItem> getNextVideoInQueue() {

        Pair<Long, MediaSession.QueueItem> ret = null;
        synchronized (this) {

            if (mQueue.size() > 1 && mQueueIndex != mQueue.size()-1)
                ret = Pair.create(mSelectedVideo.getNextUpThresholdTrigger(mActivity), mQueue.get(mQueueIndex + 1));
        }
        return ret;
    }

    public boolean areSubtitlesEnabled() {

        boolean ret;
        synchronized (this) {
            ret = mSelectedVideoTracks != null && mSelectedVideoTracks.areSubtitlesEnabled();
        }
        return ret;
    }

    public boolean playPreviousInQueue(Activity activity) {

        boolean ret = false;
        synchronized (this) {
            if (mQueueIndex > 0)
                ret = playQueueItem(activity, --mQueueIndex);
        }
        return ret;
    }

    public boolean playNextInQueue(Activity activity) {

        boolean ret = false;
        synchronized (this) {
            if (mQueueIndex + 1 < mQueue.size())
                ret = playQueueItem(activity, ++mQueueIndex);
        }
        return ret;
    }

    public boolean playQueueItem(Activity activity, int index) {

        mSelectedVideoTracks = null;
        Bundle bundle = new Bundle();
        bundle.putBoolean(VideoPlayerHandler.AUTO_PLAY, true);
        MediaSession.QueueItem item = mQueue.get(index);
        Bundle extra = item.getDescription().getExtras();
        mStartPosition.reset(extra != null ? extra.getLong(NextUpView.BUNDLE_EXTRA_VIEWEDOFFSET) : 0);
        String mediaId = item.getDescription().getMediaId();
        activity.getMediaController().getTransportControls().playFromMediaId(mediaId, bundle);
        return true;
    }

    public void updateProgressTask() {

        synchronized (mProgressLock) {

            mProgressTask = VideoProgressTask.getTask(mServer, getVideo());
            Pair<Long, MediaSession.QueueItem> pair = getNextVideoInQueue();
            if (pair != null)
                mNextUpHandler.fillNextUp(pair.first, pair.second, mSelectedVideo.getDuration());
            else
                mNextUpHandler.disable();
        }
    }

    @Override
    public void progressUpdate(int playbackState, long timeInMs) {

        synchronized (mProgressLock) {

            mNextUpHandler.setCurrentVideoOffset(timeInMs);
            if (playbackState == PlaybackState.STATE_PLAYING) {

                if (Math.abs(timeInMs - mProgressTaskLastUpdate) > VideoProgressTask.PreferedSpanThresholdMs) {

                    if (mProgressTask != null) {

                        mProgressTask.setProgress(mActivity.isFinishing() || mActivity.isDestroyed(), timeInMs);
                        mProgressTaskLastUpdate = timeInMs;
                    }
                }
            }
        }
    }

    public ListRow getCodecRowForVideo(TrackSelector selector) {

        ListRow ret = null;
        synchronized (this) {
            if (mSelectedVideo != null) {
                ret = mSelectedVideo.getCodecsRow(mActivity, mServer, mSelectedVideoTracks);
                mSelectionHandler.setCodecClickListener(new CodecClickHandler(mActivity, mSelectedVideoTracks, selector, ret));
            }
        }
        return ret;
    }

    @Override
    public void chapterSelected(Chapter chapter) {

        if (chapter != null)
            mVideoHandler.setPosition(chapter.getChapterStart());
    }

    public ListRow getExtrasRowForVideo() {

        ListRow ret = null;
        synchronized (this) {

            if (mSelectedVideo != null)
                ret = mSelectedVideo.getChildren(mActivity, mServer, mSelectionHandler);
        }
        return ret;
    }

    public void setHandler(VideoPlayerHandler videoHandler, NextUpHandler nextUpHandler) {

        mNextUpHandler = nextUpHandler;
        mVideoHandler = videoHandler;
    }

    public boolean hasNext() {

        boolean ret;
        synchronized (this) {
            ret = mQueue != null && mQueueIndex >= 0 && mQueueIndex+1 < mQueue.size();
        }
        return ret;
    }

    public class CodecClickHandler implements CodecCard.OnClickListenerHandler {

        private final Activity mActivity;
        private final MediaTrackSelector mTracks;
        private final TrackSelector mSelector;
        private final ListRow mRow;

        public CodecClickHandler(Activity activity, MediaTrackSelector tracks, TrackSelector selector, ListRow row) {
            mActivity = activity;
            mTracks = tracks;
            mSelector = selector;
            mRow = row;
        }

        @Override
        public MediaTrackSelector getSelector() {
            return mTracks;
        }

        @Override
        public DialogInterface.OnClickListener getDialogOnClickListener(final Object card, final int trackType) {
            return new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    mTracks.setSelectedTrack(mSelector, trackType, which);
                    ArrayObjectAdapter adapter = (ArrayObjectAdapter) mRow.getAdapter();
                    int index = adapter.indexOf(card);
                    if (0 <= index) {
                        adapter.replace(index, new CodecCard(mActivity, mTracks.getSelectedTrack(trackType), trackType, mTracks.getCount(trackType)));
                        adapter.notifyArrayItemRangeChanged(index, 1);
                    }
                    dialog.dismiss();
                }
            };
        }
    }
}
