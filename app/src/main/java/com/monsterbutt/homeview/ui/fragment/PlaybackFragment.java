/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.monsterbutt.homeview.ui.fragment;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.PlaybackOverlayFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackControlsRowPresenter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.util.Log;
import android.view.Display;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.google.android.exoplayer.AspectRatioFrameLayout;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.text.Cue;
import com.google.android.exoplayer.util.Util;
import com.monsterbutt.homeview.BuildConfig;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.model.Video;
import com.monsterbutt.homeview.player.ExtractorRendererBuilder;
import com.monsterbutt.homeview.player.MediaCodecCapabilities;
import com.monsterbutt.homeview.player.MediaTrackSelector;
import com.monsterbutt.homeview.player.StartPosition;
import com.monsterbutt.homeview.player.VideoPlayer;
import com.monsterbutt.homeview.player.PlaybackControlHelper;
import com.monsterbutt.homeview.player.text.PgsCue;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.PlexServerManager;
import com.monsterbutt.homeview.plex.media.Chapter;
import com.monsterbutt.homeview.plex.media.Episode;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
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
import com.monsterbutt.homeview.presenters.SceneCard;
import com.monsterbutt.homeview.services.UpdateRecommendationsService;
import com.monsterbutt.homeview.settings.SettingsManager;
import com.monsterbutt.homeview.ui.activity.PlaybackActivity;
import com.monsterbutt.homeview.ui.android.ImageCardView;
import com.monsterbutt.homeview.ui.android.NextUpView;
import com.monsterbutt.homeview.ui.android.HomeViewActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import us.nineworlds.plex.rest.model.impl.Media;
import us.nineworlds.plex.rest.model.impl.MediaContainer;

import static android.media.session.MediaSession.FLAG_HANDLES_MEDIA_BUTTONS;
import static android.media.session.MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS;

/*
 * The PlaybackOverlayFragment class handles the Fragment associated with displaying the UI for the
 * media controls such as play / pause / skip forward / skip backward etc.
 *
 * The UI is updated through events that it receives from its MediaController
 */
public class PlaybackFragment
        extends android.support.v17.leanback.app.PlaybackOverlayFragment
        implements VideoPlayer.Listener, PlexServerTaskCaller,
        OnItemViewClickedListener, HomeViewActivity.OnStopKeyListener,
        PlaybackControlHelper.ProgressUpdateCallback, HomeViewActivity.OnBackPressedListener,
        SurfaceHolder.Callback, VideoPlayer.CaptionListener, CardPresenter.CardPresenterLongClickListener, PlaybackOverlayFragment.InputEventHandler {

    private static final int CHOOSER_TIMEOUT = 15000;
    private static final String TAG = "PlaybackOverlayFragment";
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final int BACKGROUND_TYPE = PlaybackFragment.BG_LIGHT;
    private static final String AUTO_PLAY = "auto_play";
    private static final Bundle mAutoPlayExtras = new Bundle();

    static {
        mAutoPlayExtras.putBoolean(AUTO_PLAY, true);
    }

    private final MediaController.Callback mMediaControllerCallback = new MediaControllerCallback();
    private int mQueueIndex = -1;
    private PlexVideoItem mSelectedVideo = null; // Video is the currently playing Video and its metadata.
    private MediaTrackSelector mSelectedVideoTracks = null;
    private VideoProgressTask mProgressTask = null;
    private long              mProgressTaskLastUpdate = 0;
    private ArrayObjectAdapter mRowsAdapter;
    private List<MediaSession.QueueItem> mQueue;
    private MediaSession mSession; // MediaSession is used to hold the state of our media playback.
    private MediaController mMediaController;
    private PlaybackControlHelper mGlue;
    private VideoPlayer mPlayer;
    private boolean mIsMetadataSet = false;
    private ListRow mCodecRow = null;
    private ListRow mExtrasRow = null;

    private AspectRatioFrameLayout videoFrame;
    private int mSourceHeight = 0;
    private int mSourceWidth = 0;
    private StartPosition mStartPosition;
    private static final String mNextUpLock = "nextuplock";
    private long mNextUpThreshold = PlexVideoItem.NEXTUP_DISABLED;

    private Surface mCacheSurface = null;
    private PlexServer mServer;

    private ImageView mSubtitlesImage = null;
    private TextView mSubtitlesText =null;

    private View mCurrentCardTransitionImage = null;
    private CardObject mCurrentCard = null;

    private boolean mGuiShowing = false;

    @Override
    public void onAttach(Context context) {

        super.onAttach(context);

        createMediaSession();
        mMediaController = getActivity().getMediaController();
        mMediaController.registerCallback(mMediaControllerCallback);
    }

    @Override
    public void onStop() {
        super.onStop();
        mSession.release();
        releasePlayer();
        updateRecommendations(0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mSession.release();
        releasePlayer();
    }

    @Override
    public void onDetach() {
        super.onDetach();

        if (mMediaController != null) {
            mMediaController.unregisterCallback(mMediaControllerCallback);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mPlayer == null && mSelectedVideo != null) {

            if (mSelectedVideo.selectedHasMissingData())
                new GetFullInfo().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mSelectedVideo.getKey());
            else
                playVideo(mSelectedVideo, mAutoPlayExtras);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize instance variables.
        Activity act = getActivity();
        videoFrame = (AspectRatioFrameLayout) act.findViewById(R.id.video_frame);
        SurfaceView surfaceView = (SurfaceView) act.findViewById(R.id.surface_view);
        surfaceView.getHolder().addCallback(this);

        mSubtitlesImage = (ImageView) act.findViewById(R.id.imageSubtitles);
        mSubtitlesText = (TextView) act.findViewById(R.id.textSubtitles);

        Display display = act.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        int subtextX = (int) ((double)width * 0.20);
        int subtextY = (int) ((double)height * 0.80);
        mSubtitlesText.setX(subtextX);
        mSubtitlesText.setWidth(width - (2* subtextX));
        mSubtitlesText.setY(subtextY);

        mQueue = new ArrayList<>();

        // Set up UI.
        setBackgroundType(BACKGROUND_TYPE);

        mServer = PlexServerManager.getInstance(act.getApplicationContext()).getSelectedServer();
        Intent intent = act.getIntent();
        PlexVideoItem intentVideo = intent.getParcelableExtra(PlaybackActivity.VIDEO);
        mSelectedVideoTracks = intent.getParcelableExtra(PlaybackActivity.TRACKS);
        setSelectedVideo(intentVideo);
        mStartPosition = new StartPosition(act, intent, mSelectedVideo != null ?
                                                            mSelectedVideo.getViewedOffset() : 0);

        String key = mSelectedVideo != null ? mSelectedVideo.getKey()
                : act.getIntent().getStringExtra(PlaybackActivity.KEY);
        if (mSelectedVideo == null || mSelectedVideo.shouldDiscoverQueue())
            new GetVideoQueueTask(this, mServer).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, key);
        initializePlaybackRow();

        // Set up listeners.
        setOnItemViewSelectedListener(new OnItemViewSelectedListener() {
            @Override
            public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                       RowPresenter.ViewHolder rowViewHolder, Row row) {
                if (item instanceof CardObject) {

                    mCurrentCard = (CardObject) item;
                    mCurrentCardTransitionImage = itemViewHolder.view instanceof ImageCardView ?
                            ((ImageCardView) itemViewHolder.view).getMainImageView() : null;
                }
            }
        });

        setOnItemViewClickedListener(this);
        setFadeCompleteListener(new OnFadeCompleteListener() {
            @Override
            public void onFadeInComplete() { setGuiShowing(true); }
            @Override
            public void onFadeOutComplete() { setGuiShowing(false); }
        });
        setInputEventHandler(this);
        ((HomeViewActivity)getActivity()).setStopKeyListner(this);
        ((HomeViewActivity)getActivity()).setBackPressedListener(this);
    }

    private boolean isGuiShowing() {
        boolean ret;
        synchronized (this) {
            ret = mGuiShowing;
        }
        return ret;
    }

    private void setGuiShowing(boolean val) {
        synchronized (this) {
            mGuiShowing = val;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mPlayer != null && mPlayer.getPlayerControl().isPlaying()) {
            boolean isVisibleBehind = getActivity().requestVisibleBehind(true);
            if (!isVisibleBehind) {
                playPause(false);
            }
        } else {
            getActivity().requestVisibleBehind(false);
        }
    }

    private void setPosition(long position) {
        if (position > mPlayer.getDuration()) {
            mPlayer.seekTo(mPlayer.getDuration());
        } else if (position < 0) {
            mPlayer.seekTo(0L);
        } else {
            mPlayer.seekTo(position);
        }
    }

    private void createMediaSession() {
        if (mSession == null) {
            mSession = new MediaSession(getActivity(), "HomeViewPlayer");
            mSession.setCallback(new MediaSessionCallback());
            mSession.setFlags(FLAG_HANDLES_MEDIA_BUTTONS | FLAG_HANDLES_TRANSPORT_CONTROLS);
            mSession.setActive(true);

            // Set the Activity's MediaController used to invoke transport controls / adjust volume.
            getActivity().setMediaController(
                    new MediaController(getActivity(), mSession.getSessionToken()));
            setPlaybackState(PlaybackState.STATE_NONE);
        }
    }

    private MediaSession.QueueItem getQueueItem(PlexVideoItem v) {

        Context context = getActivity();
        if (context == null)
            return null;

        Bundle bundle = new Bundle();
        if (v instanceof Episode) {

            bundle.putBoolean(NextUpView.BUNDLE_EXTRA_TYPE_SHOW, true);
            bundle.putString(NextUpView.BUNDLE_EXTRA_SHOW_NAME, ((Episode)v).getShowName());
            bundle.putString(NextUpView.BUNDLE_EXTRA_SHOW_EPISODE, v.getCardContent(getContext()));
        }
        bundle.putString(NextUpView.BUNDLE_EXTRA_SUMMARY, v.getSummary());
        bundle.putString(NextUpView.BUNDLE_EXTRA_YEAR, v.getYear());
        bundle.putString(NextUpView.BUNDLE_EXTRA_STUDIO, v.getStudio());
        bundle.putString(NextUpView.BUNDLE_EXTRA_TITLE, v.getTitle());
        bundle.putLong(NextUpView.BUNDLE_EXTRA_VIEWEDOFFSET, v.getViewedOffset());

        MediaDescription desc = new MediaDescription.Builder()
                .setDescription(v.getPlaybackSubtitle(context))
                .setExtras(bundle)
                .setMediaId(Long.toString(v.getRatingKey()) + "")
                .setMediaUri(Uri.parse(v.getVideoPath(mServer)))
                .setIconUri(Uri.parse(v.getPlaybackImageURL()))
                .setSubtitle(v.getPlaybackSubtitle(context))
                .setTitle(v.getPlaybackTitle(context))
                .build();

        return new MediaSession.QueueItem(desc, v.getKey().hashCode());
    }

    public long getBufferedPosition() {
        if (mPlayer != null) {
            return mPlayer.getBufferedPosition();
        }
        return 0L;
    }

    public long getCurrentPosition() {
        if (mPlayer != null) {
            return mPlayer.getCurrentPosition();
        }
        return 0L;
    }

    public long getDuration() {
        if (mPlayer != null) {
            return mPlayer.getDuration();
        }
        return ExoPlayer.UNKNOWN_TIME;
    }

    private long getAvailableActions(int nextState) {
        long actions = PlaybackState.ACTION_PLAY |
                PlaybackState.ACTION_PLAY_FROM_MEDIA_ID |
                PlaybackState.ACTION_PLAY_FROM_SEARCH |
                PlaybackState.ACTION_SKIP_TO_NEXT |
                PlaybackState.ACTION_SKIP_TO_PREVIOUS |
                PlaybackState.ACTION_PAUSE;

        if (nextState == PlaybackState.STATE_PLAYING) {
            actions |= PlaybackState.ACTION_PAUSE;
        }

        return actions;
    }

    private void playPause(boolean doPlay) {

        if (mPlayer == null) {
            setPlaybackState(PlaybackState.STATE_NONE);
            return;
        }

        if (doPlay && getPlaybackState() != PlaybackState.STATE_PLAYING) {

            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            mPlayer.getPlayerControl().start();
            setPlaybackState(PlaybackState.STATE_PLAYING);
        } else {
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            mPlayer.getPlayerControl().pause();
            setPlaybackState(PlaybackState.STATE_PAUSED);
        }
    }

    private void setupVideoForPlayback() {

        updatePlaybackRow();
        updateMetadata();
        updateCodecAndExtras();
    }

    private void updateCodecAndExtras() {

        if (mCodecRow != null)
            mRowsAdapter.remove(mCodecRow);
        if (mExtrasRow != null)
            mRowsAdapter.remove(mExtrasRow);

        mCodecRow = mSelectedVideo.getCodecsRow(getActivity(), mServer, mSelectedVideoTracks);
        if (mCodecRow != null)
            mRowsAdapter.add(mCodecRow);
        mExtrasRow = mSelectedVideo.getChildren(getActivity(), mServer, this);
        if (mExtrasRow != null)
            mRowsAdapter.add(mExtrasRow);
    }

    private void updatePlaybackRow() {
        mRowsAdapter.notifyArrayItemRangeChanged(0, 1);
    }

    private VideoPlayer.RendererBuilder getRendererBuilder() {
        String userAgent = Util.getUserAgent(getActivity(), "ExoVideoPlayer");

        Uri contentUri = Uri.parse(mSelectedVideo.getVideoPath(mServer));
        int contentType = Util.inferContentType(contentUri.getLastPathSegment());
        switch (contentType) {
            case Util.TYPE_OTHER: {
                return new ExtractorRendererBuilder(getActivity(), userAgent, contentUri);
            }
            default: {
                throw new IllegalStateException("Unsupported type: " + contentType);
            }
        }
    }

    private void preparePlayer(boolean playWhenReady) {

        if (mPlayer == null) {

            mPlayer = new VideoPlayer(getRendererBuilder());
            mPlayer.setCaptionListener(this);
            mPlayer.addListener(this);
            mPlayer.seekTo(mStartPosition.getStartPosition());
            mPlayer.prepare(mSelectedVideo);
            if (mCacheSurface != null)
                mPlayer.setSurface(mCacheSurface);
            mCacheSurface = null;
        }
        else {

            mPlayer.stop();
            mPlayer.seekTo(mStartPosition.getStartPosition());
            mPlayer.setRendererBuilder(getRendererBuilder());
            mPlayer.prepare(mSelectedVideo);
        }

        if (mStartPosition.getStartType() == StartPosition.PlaybackStartType.Ask) {

            final AlertDialog alert = new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setIcon(R.drawable.launcher)
                .setTitle(R.string.playback_start_dialog)
                .setAdapter(getResumeChoiceAdapter(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        final StartPosition.PlaybackStartType val = StartPosition.PlaybackStartType.values()[which];
                        if (val == StartPosition.PlaybackStartType.Begining)
                            return;
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                long pos = mStartPosition.getVideoOffset();
                                switch (mPlayer.getPlaybackState()) {

                                    case ExoPlayer.STATE_READY:
                                    case ExoPlayer.STATE_ENDED:
                                    case ExoPlayer.STATE_BUFFERING:

                                        setPosition(pos);
                                        break;

                                    case ExoPlayer.STATE_IDLE:
                                    case ExoPlayer.STATE_PREPARING:

                                        mPlayer.seekTo(pos);
                                        break;
                                    default:
                                        break;
                                }
                            }
                        });
                        dialog.dismiss();
                    }
                })
                .create();
            // Hide after some seconds
            final Handler handler  = new Handler();
            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (alert.isShowing()) {
                        alert.dismiss();
                    }
                }
            };
            alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    handler.removeCallbacks(runnable);
            }});
            alert.show();

            handler.postDelayed(runnable, CHOOSER_TIMEOUT);
        }
        mPlayer.setPlayWhenReady(playWhenReady);
    }

    private ResumeChoiceArrayAdapter getResumeChoiceAdapter() {

        Activity activity = getActivity();
        List<ResumeChoice> list = new ArrayList<>();
        list.add(new ResumeChoice(0, R.drawable.ic_slow_motion_video_white_48dp, activity.getString(R.string.playback_start_dialog_resume)));
        list.add(new ResumeChoice(1, R.drawable.ic_play_circle_outline_white_48dp, activity.getString(R.string.playback_start_dialog_begin)));

        return new ResumeChoiceArrayAdapter(getActivity(), list);
    }

    private void releasePlayer() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }

    @Override
    public void onStateChanged(boolean playWhenReady, int playbackState) {
        switch (playbackState) {
            case ExoPlayer.STATE_BUFFERING:
                // Do nothing.
                break;
            case ExoPlayer.STATE_ENDED:
                mIsMetadataSet = false;
                if (SettingsManager.getInstance(getActivity().getApplicationContext())
                        .getBoolean("preferences_playback_playnext")) {

                    mMediaController.getTransportControls().skipToNext();
                }
                else
                    getActivity().onBackPressed();
                break;
            case ExoPlayer.STATE_IDLE:
                // Do nothing.
                break;
            case ExoPlayer.STATE_PREPARING:
                mIsMetadataSet = false;

                if (mSelectedVideoTracks != null) {
                    mSelectedVideoTracks.setSelectedTrack(mPlayer, Stream.Audio_Stream,
                            mSelectedVideoTracks.getSelectedTrackDisplayIndex(Stream.Audio_Stream));
                    mSelectedVideoTracks.setSelectedTrack(mPlayer, Stream.Subtitle_Stream,
                            mSelectedVideoTracks.getSelectedTrackDisplayIndex(Stream.Subtitle_Stream));
                }
                break;
            case ExoPlayer.STATE_READY:

                // Duration is set here.
                if (!mIsMetadataSet) {

                    if (!mSelectedVideo.shouldUpdateStatusOnPlayback())
                        updateRecommendations(mSelectedVideo.getRatingKey());
                    updateMetadata();
                    mIsMetadataSet = true;
                }
                break;
            default:
                // Do nothing.
                break;
        }
    }

    @Override
    public void onError(Exception e) {

        Toast.makeText(getActivity(), "An error has occured", Toast.LENGTH_LONG).show();
        Log.e(TAG, "An error occurred: " + e);
    }

    @Override
    public void onVideoSizeChanged(final int width, final int height, int unappliedRotationDegrees,
                                   float pixelWidthHeightRatio) {

        videoFrame.setAspectRatio(
                height == 0 ? 1 : (width * pixelWidthHeightRatio) / height);
    }

    private int getPlaybackState() {
        Activity activity = getActivity();

        if (activity != null) {
            PlaybackState state = activity.getMediaController().getPlaybackState();
            if (state != null) {
                return state.getState();
            } else {
                return PlaybackState.STATE_NONE;
            }
        }
        return PlaybackState.STATE_NONE;
    }

    private void setPlaybackState(int state) {
        long currPosition = getCurrentPosition();

        PlaybackState.Builder stateBuilder = new PlaybackState.Builder()
                .setActions(getAvailableActions(state));
        stateBuilder.setState(state, currPosition, 1.0f);
        mSession.setPlaybackState(stateBuilder.build());
    }

    private void updateMetadata() {

        Context context = getContext();
        final PlexVideoItem video = mSelectedVideo;
        final MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder();

        if (video != null) {
            metadataBuilder.putString(MediaMetadata.METADATA_KEY_MEDIA_ID, Long.toString(video.getRatingKey()) + "");
            metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, video.getPlaybackTitle(context));
            metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, video.getPlaybackSubtitle(context));
            metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION, video.getPlaybackSubtitle(context));
            metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI, video.getPlaybackImageURL());
            metadataBuilder.putLong(MediaMetadata.METADATA_KEY_DURATION, video.getDurationMs());

            // And at minimum the title and artist for legacy support
            metadataBuilder.putString(MediaMetadata.METADATA_KEY_TITLE, video.getTitle());
            metadataBuilder.putString(MediaMetadata.METADATA_KEY_ARTIST, video.getStudio());
        }
        else
            metadataBuilder.putLong(MediaMetadata.METADATA_KEY_DURATION, 32400000);

        Resources res = getResources();
        int cardWidth = res.getDimensionPixelSize(R.dimen.playback_overlay_width);
        int cardHeight = res.getDimensionPixelSize(R.dimen.playback_overlay_height);

        String url = "";
        if (video != null) {

            if (video instanceof UpnpItem)
                url = video.getPlaybackImageURL();
            else if (mServer != null)
                url =  mServer.makeServerURL(video.getPlaybackImageURL());
        }
        Glide.with(this)
                .load(Uri.parse(url))
                .asBitmap()
                .centerCrop()
                .error(getActivity().getDrawable(R.drawable.default_video_cover))
                .into(new SimpleTarget<Bitmap>(cardWidth, cardHeight) {
                    @Override
                    public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
                        metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, bitmap);
                        mSession.setMetadata(metadataBuilder.build());
                    }
                });
    }



    private void playVideo(PlexVideoItem video, Bundle extras) {

        setSelectedVideo(video);
        setCurrentSourceStats();
        if (mQueue.size() > 1 && mQueueIndex != mQueue.size()-1) {

            synchronized (mNextUpLock) {

                mNextUpThreshold = mSelectedVideo.getNextUpThresholdTrigger(getContext());
                ((PlaybackActivity) getActivity()).fillNextUp(mQueue.get(mQueueIndex + 1));
            }
        }
        setupVideoForPlayback();
        preparePlayer(true);
        setPlaybackState(PlaybackState.STATE_PAUSED);
        playPause(extras.getBoolean(AUTO_PLAY));
    }

    private void setSelectedVideo(PlexVideoItem video) {

        mSelectedVideo = video;
        if (mSelectedVideo != null && mSelectedVideo.shouldUpdateStatusOnPlayback())
            mProgressTask = VideoProgressTask.getTask(mServer, mSelectedVideo);
        if (mSelectedVideo != null && mSelectedVideoTracks == null) {
            mSelectedVideoTracks = mSelectedVideo.fillTrackSelector(getActivity(),
                                                            Locale.getDefault().getISO3Language(),
                                                            MediaCodecCapabilities.getInstance(getActivity()));
        }
    }

    @Override
    public void handlePostTaskUI(Boolean result, PlexServerTask task) {

        if (getActivity() == null || getActivity().isDestroyed() || getActivity().isFinishing())
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
                if ( mSelectedVideo == null || v.shouldPlaybackFirst()) {

                    if (mSelectedVideo == null)

                        setSelectedVideo(v);
                    mQueueIndex = mQueue.size();
                }

                mQueue.add(getQueueItem(v));
            }

            if (mQueueIndex == -1 && !mQueue.isEmpty()) {

                if (mSelectedVideo == null && first != null) {

                    setSelectedVideo(first);
                    mStartPosition.setVideoOffset(mSelectedVideo.getViewedOffset());
                }
                mQueueIndex = 0;
            }

            if (getActivity() == null || getActivity().isDestroyed() || getActivity().isFinishing())
                return;
            if (startPlayback && mSelectedVideo != null) {

                if (mSelectedVideo.selectedHasMissingData())
                    new GetFullInfo().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mSelectedVideo.getKey());
                else
                    playVideo(mSelectedVideo, mAutoPlayExtras);
            }
            else if (mQueue.size() > 1 && mQueueIndex != mQueue.size()-1) {

                synchronized (mNextUpLock) {

                    mNextUpThreshold = mSelectedVideo.getNextUpThresholdTrigger(getContext());
                    ((PlaybackActivity) getActivity()).fillNextUp(mQueue.get(mQueueIndex + 1));
                }
             }

            mSession.setQueue(mQueue);
            mSession.setQueueTitle(getString(R.string.queue_name));
        }
        else if (task instanceof GetVideoTask) {

            PlexVideoItem video = ((GetVideoTask) task).getVideo();
            if (video != null) {

                if (video.selectedHasMissingData())
                    new GetFullInfo().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, video.getKey());
                else
                    playVideo(video, mAutoPlayExtras);
            }
        }
    }

    public void chapterSelected(Chapter chapter) {

        long currPos = getCurrentPosition();
        long pos = chapter.getChapterStart();
        int prevState = getPlaybackState();
        setPlaybackState(pos > currPos ? PlaybackState.STATE_REWINDING : PlaybackState.STATE_FAST_FORWARDING);
        setPosition(pos);
        setPlaybackState(prevState);
    }

    private class CodecClickHandler implements CodecCard.OnClickListenerHandler {

        @Override
        public MediaTrackSelector getSelector() {
            return mSelectedVideoTracks;
        }

        @Override
        public DialogInterface.OnClickListener getDialogOnClickListener(final Object card, final int trackType) {
            return new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    mSelectedVideoTracks.setSelectedTrack(mPlayer, trackType, which);
                    ArrayObjectAdapter adapter = (ArrayObjectAdapter) mCodecRow.getAdapter();
                    int index = adapter.indexOf(card);
                    if (0 <= index) {
                        adapter.replace(index, new CodecCard(getActivity(),
                                mSelectedVideoTracks.getSelectedTrack(trackType),
                                trackType,
                                mSelectedVideoTracks.getCount(trackType)));

                        adapter.notifyArrayItemRangeChanged(index, 1);
                    }
                    dialog.dismiss();
                }
            };
        }
    }

    public boolean onItemClicked(Object item) {

        if (item instanceof SceneCard && ((SceneCard) item).getItem() instanceof Chapter)
            chapterSelected((Chapter)((SceneCard) item).getItem());
        else if (item instanceof CodecCard)
            ((CodecCard) item).onCardClicked(getActivity(), mServer, new CodecClickHandler());
        else if (item instanceof PosterCard)
            ((PosterCard)item).onClicked(this, null, mCurrentCardTransitionImage);
        else
            return false;
        return true;
    }

    @Override
    public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
        onItemClicked(item);
    }

    @Override
    public boolean stopKeyPressed() {

        getActivity().finish();
        return true;
    }

    @Override
    public void progressUpdate(long timeInMs) {

        synchronized (mNextUpLock) {

            if (mNextUpThreshold != PlexVideoItem.NEXTUP_DISABLED &&
                    timeInMs >= mNextUpThreshold) {
                ((PlaybackActivity)getActivity()).setNextUpVisible(true);
                mNextUpThreshold = -1;
            }
        }

        if (getPlaybackState() == PlaybackState.STATE_PLAYING) {

            if (Math.abs(timeInMs - mProgressTaskLastUpdate) > VideoProgressTask.PreferedSpanThresholdMs)
                updateProgressTask(timeInMs);
        }

    }

    private void updateProgressTask(long timeInMs) {

        if (mProgressTask != null) {

            mProgressTask.setProgress(timeInMs);
            mProgressTaskLastUpdate = timeInMs;
        }
    }

    @Override
    public void tickle() {

        PlaybackActivity act = (PlaybackActivity) getActivity();
        if (act.getNextUpVisible())
            act.setNextUpVisible(false);
        super.tickle();
    }

    @Override
    public boolean backPressed() {

        PlaybackActivity act = (PlaybackActivity) getActivity();
        if (act.getNextUpVisible()) {
            act.setNextUpVisible(false);
            return true;
        }
        return false;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        if (mPlayer != null) {
            mPlayer.setSurface(holder.getSurface());
        }
        else
            mCacheSurface = holder.getSurface();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

        if (mPlayer != null) {
            mPlayer.blockingClearSurface();
        }
    }

    @Override
    public void onCues(List<Cue> cues) {

        if (getActivity() == null || getActivity().isFinishing() || getActivity().isDestroyed())
            return;

        final Cue cue = (cues != null && !cues.isEmpty()) ? cues.get(0) : null;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // if no cue... duh
                // or if cue is not PGS (we only handle that currently)
                // or subs are turned off but we are checking for forced subs and it isn't forced
                if (cue == null || (mSelectedVideoTracks != null && !mSelectedVideoTracks.areSubtitlesEnabled() &&
                        (cue instanceof PgsCue && !((PgsCue)cue).isForcedSubtitle()))) {

                    mSubtitlesText.setText("");
                    mSubtitlesText.setVisibility(View.INVISIBLE);
                    mSubtitlesImage.setImageBitmap(null);
                    mSubtitlesImage.setVisibility(View.INVISIBLE);
                }
                else if (!(cue instanceof PgsCue)) {

                    mSubtitlesText.setText(cue.text);
                    mSubtitlesText.setVisibility(View.VISIBLE);
                }
                // pgs sub and subs are on or it is forced
                else {
                    ((PgsCue)cue).updateParams((int) videoFrame.getX(), (int) videoFrame.getY(),
                            videoFrame.getWidth(), videoFrame.getHeight(), mSourceWidth, mSourceHeight,
                            mSubtitlesImage);

                }
            }
        });
    }

    private void setCurrentSourceStats() {

        if (!mSelectedVideo.hasSourceStats())
            return;
        Media media = mSelectedVideo.getMedia().get(0);
        mSourceHeight = Integer.valueOf(media.getHeight());
        mSourceWidth = Integer.valueOf(media.getWidth());
    }

    @Override
    public boolean longClickOccured() {
        return mCurrentCard != null && onItemClicked(mCurrentCard);
    }

    @Override
    public boolean handleInputEvent(InputEvent event) {

        if (isGuiShowing() || !(event instanceof KeyEvent))
            return false;

        switch (((KeyEvent) event).getKeyCode()) {

            case KeyEvent.KEYCODE_DPAD_LEFT:
                rewind();
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                fastForward();
                break;
            default:
                break;
        }

        return false;
    }

    private boolean rewind() {

        if (mPlayer.getDuration() != ExoPlayer.UNKNOWN_TIME) {
            int prevState = getPlaybackState();
            setPlaybackState(PlaybackState.STATE_REWINDING);

            long curr = mPlayer.getCurrentPosition();
            if (curr > 0) {
                long loc = curr -
                        (Long.valueOf(getActivity().getString(R.string.skip_back_seconds)) * 1000);
                if (loc < 0)
                    loc = 0;
                setPosition(loc);
                setPlaybackState(prevState);
                return true;
            }
        }
        return false;
    }

    private boolean fastForward() {

        if (mPlayer.getDuration() != ExoPlayer.UNKNOWN_TIME) {
            int prevState = getPlaybackState();
            setPlaybackState(PlaybackState.STATE_FAST_FORWARDING);
            long loc =  mPlayer.getCurrentPosition() +(Long.valueOf(getActivity().getString(R.string.skip_forward_seconds)) * 1000);

            if (loc < mPlayer.getDuration()) {
                setPosition(loc);
                setPlaybackState(prevState);
                return true;
            }
        }
        return false;
    }

    // An event was triggered by MediaController.TransportControls and must be handled here.
    // Here we update the media itself to act on the event that was triggered.
    private class MediaSessionCallback extends MediaSession.Callback {

        @Override
        public void onPlay() {
            playPause(true);
        }

        @Override
        // This method should play any media item regardless of the Queue.
        public void onPlayFromMediaId(String mediaId, Bundle extras) {

            if (mSelectedVideo.shouldDiscoverQueue())
                new GetVideoTask(PlaybackFragment.this, mServer).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "/library/metadata/" + mediaId);
        }

        @Override
        public void onPause() {
            playPause(false);
        }

        @Override
        public void onSkipToNext() {

            if (mSelectedVideo.hasChapters()) {

                long pos = mSelectedVideo.getNextChapterStart(getCurrentPosition());
                if (pos != PlexVideoItem.BAD_CHAPTER_START) {

                    int prevState = getPlaybackState();
                    setPlaybackState(PlaybackState.STATE_FAST_FORWARDING);
                    setPosition(pos);
                    setPlaybackState(prevState);
                    return;
                }
            }

            // Update the media to skip to the next video.
            Bundle bundle = new Bundle();
            bundle.putBoolean(AUTO_PLAY, true);
            int nextIndex = ++mQueueIndex;
            if (nextIndex < mQueue.size()) {

                mSelectedVideoTracks = null;
                MediaSession.QueueItem item = mQueue.get(nextIndex);
                Bundle extra = item.getDescription().getExtras();
                mStartPosition.reset(extra != null ? extra.getLong(NextUpView.BUNDLE_EXTRA_VIEWEDOFFSET) : 0);
                String mediaId = item.getDescription().getMediaId();
                getActivity().getMediaController()
                        .getTransportControls()
                        .playFromMediaId(mediaId, bundle);
            } else {
                getActivity().onBackPressed(); // Return to details presenter.
            }
        }

        @Override
        public void onSkipToPrevious() {

            if (mSelectedVideo.hasChapters()) {

                long pos = mSelectedVideo.getPreviousChapterStart(getCurrentPosition());
                if (pos != PlexVideoItem.BAD_CHAPTER_START) {

                    int prevState = getPlaybackState();
                    setPlaybackState(PlaybackState.STATE_REWINDING);
                    setPosition(pos);
                    setPlaybackState(prevState);
                    return;
                }
            }

            Bundle bundle = new Bundle();
            bundle.putBoolean(AUTO_PLAY, true);

            int prevIndex = --mQueueIndex;
            if (prevIndex >= 0) {

                mSelectedVideoTracks = null;
                MediaSession.QueueItem item = mQueue.get(prevIndex);
                Bundle extra = item.getDescription().getExtras();
                mStartPosition.reset(extra != null ? extra.getLong(NextUpView.BUNDLE_EXTRA_VIEWEDOFFSET) : 0);
                String mediaId = item.getDescription().getMediaId();

                getActivity().getMediaController()
                        .getTransportControls()
                        .playFromMediaId(mediaId, bundle);
            } else {
                getActivity().onBackPressed(); // Return to details presenter.
            }
        }

        @Override
        public void onFastForward() {
            fastForward();
        }

        @Override
        public void onStop() {

            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            getActivity().finish();
        }

        @Override
        public void onRewind() {
            rewind();
        }

        @Override
        public void onSeekTo(long position) {
            setPosition(position);
        }
    }

    private class MediaControllerCallback extends MediaController.Callback {

        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackState state) {
            // Update your UI to reflect the new state. Do not change media playback here.
            if (DEBUG) Log.d(TAG, "Playback state changed: " + state.getState());

            int nextState = state.getState();
            if (nextState != PlaybackState.STATE_NONE) {
                mGlue.updateProgress();
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            mGlue.onMetadataChanged(); // Update metadata on controls.
            updatePlaybackRow();
        }
    }

    private class GetFullInfo extends AsyncTask<String, Void, MediaContainer> {

        @Override
        protected MediaContainer doInBackground(String... params) {
            return mServer.getVideoMetadata(params[0]);
        }

        @Override
        protected void onPostExecute(MediaContainer result) {

            if (result != null)
                playVideo(PlexVideoItem.getItem(result.getVideos().get(0)), mAutoPlayExtras);
        }
    }

    private void initializePlaybackRow() {

        Video video = null;
        if (mSelectedVideo != null)
            video = mSelectedVideo.toVideo(getContext());
        mGlue = new PlaybackControlHelper(getActivity(), this, video, this);
        PlaybackControlsRowPresenter controlsRowPresenter = mGlue.createControlsRowAndPresenter();
        PlaybackControlsRow controlsRow = mGlue.getControlsRow();

        ClassPresenterSelector ps = new ClassPresenterSelector();
        ps.addClassPresenter(PlaybackControlsRow.class, controlsRowPresenter);
        ps.addClassPresenter(ListRow.class, new ListRowPresenter());
        mRowsAdapter = new ArrayObjectAdapter(ps);
        mRowsAdapter.add(controlsRow);
        setAdapter(mRowsAdapter);

        updatePlaybackRow();
        updateMetadata();
    }

    private static class ResumeChoice {

        public final int id;
        public final int drawable;
        public final String text;

        public ResumeChoice(int id, int drawable, String text) {

            this.id = id;
            this.drawable = drawable;
            this.text = text;
        }

    }

    private static class ResumeChoiceArrayAdapter extends ArrayAdapter<ResumeChoice> {

        private final List<ResumeChoice> values;
        private final Context context;

        public ResumeChoiceArrayAdapter(Context context, List<ResumeChoice> values) {
            super(context, R.layout.lb_aboutitem, values);
            this.context = context;
            this.values = values;
        }

        @Override
        public View getView(int position, View row, ViewGroup parent) {

            View rowView = row;
            if (rowView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                rowView = inflater.inflate(R.layout.lb_resumechoice, parent, false);
            }
            final ResumeChoice item = values.get(position);
            ImageView image = (ImageView) rowView.findViewById(R.id.resumeimage);
            image.setImageDrawable(context.getDrawable(item.drawable));
            TextView desc = (TextView) rowView.findViewById(R.id.resumedesc);
            desc.setText(item.text);
            return rowView;
        }
    }

    private void updateRecommendations(long excludeRatingKey) {

        Activity activity = getActivity();
        Intent intent = new Intent(activity.getApplicationContext(), UpdateRecommendationsService.class);
        if (0 < excludeRatingKey)
            intent.putExtra(UpdateRecommendationsService.EXCLUDE_VIDEO, excludeRatingKey);
        activity.startService(intent);
    }
}
