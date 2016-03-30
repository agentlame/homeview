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
import android.graphics.SurfaceTexture;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.util.Util;
import com.monsterbutt.homeview.BuildConfig;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.model.Video;
import com.monsterbutt.homeview.player.ExtractorRendererBuilder;
import com.monsterbutt.homeview.player.StartPosition;
import com.monsterbutt.homeview.player.VideoPlayer;
import com.monsterbutt.homeview.player.PlaybackControlHelper;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.PlexServerManager;
import com.monsterbutt.homeview.plex.media.Chapter;
import com.monsterbutt.homeview.plex.media.Episode;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.plex.tasks.GetVideoQueueTask;
import com.monsterbutt.homeview.plex.tasks.GetVideoTask;
import com.monsterbutt.homeview.plex.tasks.PlexServerTask;
import com.monsterbutt.homeview.plex.tasks.PlexServerTaskCaller;
import com.monsterbutt.homeview.presenters.SceneCard;
import com.monsterbutt.homeview.settings.SettingsManager;
import com.monsterbutt.homeview.ui.activity.PlaybackActivity;
import com.monsterbutt.homeview.ui.android.NextUpView;
import com.monsterbutt.homeview.ui.android.HomeViewActivity;

import java.util.ArrayList;
import java.util.List;

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
        implements TextureView.SurfaceTextureListener, VideoPlayer.Listener, PlexServerTaskCaller,
        OnItemViewClickedListener, HomeViewActivity.OnStopKeyListener, PlaybackControlHelper.ProgressUpdateCallback, HomeViewActivity.OnBackPressedListener {

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
    private ArrayObjectAdapter mRowsAdapter;
    private List<MediaSession.QueueItem> mQueue;
    private MediaSession mSession; // MediaSession is used to hold the state of our media playback.
    private MediaController mMediaController;
    private PlaybackControlHelper mGlue;
    private VideoPlayer mPlayer;
    private boolean mIsMetadataSet = false;
    private ListRow mCodecsRow = null;
    private ListRow mExtrasRow = null;

    private StartPosition mStartPosition;
    private static final String mNextUpLock = "nextuplock";
    private long mNextUpThreshold = PlexVideoItem.NEXTUP_DISABLED;

    private SurfaceTexture mCacheSurfaceTexture = null;
    private PlexServer mServer;

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

            if (selectedHasMissingData())
                new GetFullInfo().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mSelectedVideo.getKey());
            else {

                setupVideoForPlayback();
                preparePlayer(true);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize instance variables.
        TextureView textureView = (TextureView) getActivity().findViewById(R.id.texture_view);
        textureView.setSurfaceTextureListener(this);

        mQueue = new ArrayList<>();

        // Set up UI.
        setBackgroundType(BACKGROUND_TYPE);

        Activity act = getActivity();
        mServer = PlexServerManager.getInstance(act.getApplicationContext()).getSelectedServer();
        Intent intent = act.getIntent();
        mSelectedVideo = intent.getParcelableExtra(PlaybackActivity.VIDEO);
        mStartPosition = new StartPosition(act, intent, mSelectedVideo != null ?
                                                            mSelectedVideo.getViewedOffset() : 0);

        String key = mSelectedVideo != null ? mSelectedVideo.getKey()
                : act.getIntent().getStringExtra(PlaybackActivity.KEY);
        new GetVideoQueueTask(this, mServer).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, key);
        initializePlaybackRow();

        // Set up listeners.
        setOnItemViewSelectedListener(new OnItemViewSelectedListener() {
            @Override
            public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                       RowPresenter.ViewHolder rowViewHolder, Row row) {
            }
        });

        setOnItemViewClickedListener(this);
        ((HomeViewActivity)getActivity()).setStopKeyListner(this);
        ((HomeViewActivity)getActivity()).setBackPressedListener(this);
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
                .setMediaUri(Uri.parse(mServer.getVideoPath(context, v)))
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

            mPlayer.getPlayerControl().start();
            setPlaybackState(PlaybackState.STATE_PLAYING);
        } else {
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            mPlayer.getPlayerControl().pause();
            setPlaybackState(PlaybackState.STATE_PAUSED);
        }
    }

    private boolean selectedHasMissingData() {

        return mSelectedVideo.getMedia().get(0).getVideoPart().get(0).getStreams() == null;
    }

    private void setupVideoForPlayback() {

        updatePlaybackRow();
        updateMetadata();
        updateCodecAndExtras();

        Activity act = getActivity();
        act.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        SettingsManager mgr = SettingsManager.getInstance(getActivity());
        if (mgr.getBoolean("preferences_device_refreshrate")) {

            WindowManager wm = (WindowManager) act.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Display.Mode[] modes = display.getSupportedModes();
            int requestMode = getBestFrameRate(modes, convertFrameRate(mSelectedVideo.getMedia().get(0).getVideoFrameRate()));
            if (requestMode != -1 && display.getMode().getRefreshRate() != modes[requestMode].getRefreshRate()) {

                WindowManager.LayoutParams params = act.getWindow().getAttributes();
                params.preferredDisplayModeId = modes[requestMode].getModeId();
                act.getWindow().setAttributes(params);
            }
        }
    }

    private void updateCodecAndExtras() {

        if (mCodecsRow != null)
            mRowsAdapter.remove(mCodecsRow);
        if (mExtrasRow != null)
            mRowsAdapter.remove(mExtrasRow);

        mCodecsRow = mSelectedVideo.getCodecsRow(getActivity(), mServer);
        if (mCodecsRow != null)
            mRowsAdapter.add(mCodecsRow);
        mExtrasRow = mSelectedVideo.getChildren(getActivity(), mServer);
        if (mExtrasRow != null)
            mRowsAdapter.add(mExtrasRow);
    }

    private void updatePlaybackRow() {
        mRowsAdapter.notifyArrayItemRangeChanged(0, 1);
    }

    private VideoPlayer.RendererBuilder getRendererBuilder() {
        String userAgent = Util.getUserAgent(getActivity(), "ExoVideoPlayer");

        String path = mServer.getVideoPath(getActivity(), mSelectedVideo);
        Uri contentUri = Uri.parse(path);
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
            mPlayer.addListener(this);
            mPlayer.seekTo(mStartPosition.getStartPosition());
            mPlayer.prepare();
            if (mCacheSurfaceTexture != null) {

                mPlayer.setSurface(new Surface(mCacheSurfaceTexture));
                mCacheSurfaceTexture = null;
            }
        } else {
            mPlayer.stop();
            mPlayer.seekTo(mStartPosition.getStartPosition());
            mPlayer.setRendererBuilder(getRendererBuilder());
            mPlayer.prepare();
        }

        if (mStartPosition.getStartType() == StartPosition.PlaybackStartType.Ask) {

            final CharSequence[] array = new String[2];
            array[0] = getActivity().getString(R.string.playback_start_dialog_begin);
            array[1] = getActivity().getString(R.string.playback_start_dialog_resume);
            new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.launcher)
                .setTitle(R.string.playback_start_dialog)
                .setItems(array, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                       final StartPosition.PlaybackStartType val = StartPosition.PlaybackStartType.values()[which];
                        if (val == StartPosition.PlaybackStartType.Begining)
                            return;
                       getActivity().runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {

                                                            long pos = mStartPosition.getVideoOffset();
                                                            boolean ready = ExoPlayer.STATE_READY == mPlayer.getPlaybackState();
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
                    .create()
                .show();
        }
        mPlayer.setPlayWhenReady(playWhenReady);
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
                break;
            case ExoPlayer.STATE_IDLE:
                // Do nothing.
                break;
            case ExoPlayer.STATE_PREPARING:
                mIsMetadataSet = false;
                break;
            case ExoPlayer.STATE_READY:

                // Duration is set here.
                if (!mIsMetadataSet) {

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
        Log.e(TAG, "An error occurred: " + e);
    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
                                   float pixelWidthHeightRatio) {
        // Do nothing.
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        if (mPlayer != null) {
            mPlayer.setSurface(new Surface(surfaceTexture));
        }
        else
            mCacheSurfaceTexture = surfaceTexture;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Do nothing.
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (mPlayer != null) {
            mPlayer.blockingClearSurface();
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Do nothing.
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

        String url = mServer != null ? mServer.makeServerURL(video != null ? video.getPlaybackImageURL() : "") : "";
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

    private static final int NO_MATCH = -1;
    private int hasFrameRate(Display.Mode[] possible, double desired) {

        int match = NO_MATCH;
        double matchDiff = 10000;
        for (int i = 0; i < possible.length; ++i) {

            double curr = possible[i].getRefreshRate();
            if (curr == desired)
                return i;

            if (Math.floor(desired) == Math.floor(curr)) {

                double discrepency = getFrameDiff(desired, curr);
                if (matchDiff > discrepency) {

                    matchDiff = discrepency;
                    match = i;
                }
            }
            else if (Math.ceil(desired) == Math.floor(curr)) {

                double discrepency = getFrameDiff(desired, curr);
                if (matchDiff > discrepency) {

                    matchDiff = discrepency;
                    match = i;
                }
            }

        }
        return match;
    }

    private int findBestForTwoPossibleFrames(Display.Mode[] possible, double desired, double backup) {

        int matchA = hasFrameRate(possible, desired);
        if (NO_MATCH != matchA && desired == possible[matchA].getRefreshRate())
            return matchA;
        int matchB = hasFrameRate(possible, backup);
        if (UNKNOWN != backup && NO_MATCH != matchB) {
            if (NO_MATCH != matchA) {

                double discrepencyA = getFrameDiff(desired, possible[matchA].getRefreshRate());
                double discrepencyB = getFrameDiff(desired, possible[matchB].getRefreshRate());
                if (discrepencyA < discrepencyB)
                    return matchA;
                return matchB;
            }
            else
                return matchB;
        }
        else if (NO_MATCH != matchA)
            return matchA;
        return -1;
    }

    private static final float UNKNOWN = (float)0.0;
    private static final float FILM = (float)23.976;
    private static final float PAL_FILM = (float)25.0;
    private static final float NTSC_INTERLACED = (float)29.97;
    private static final float DIGITAL_30 = (float)30.0;
    private static final float PAL = (float)50.0;
    private static final float NTSC = (float)59.94;
    private static final float DIGITAL_60 = (float) 60.0;

    private int getBestFrameRate(Display.Mode[] possible, double desired) {

        int ret = -1;
        if (desired == DIGITAL_60)
            ret = findBestForTwoPossibleFrames(possible, DIGITAL_60, NTSC);
        else if (desired == NTSC)
            ret = findBestForTwoPossibleFrames(possible, NTSC, DIGITAL_60);
        else if (desired == PAL)
            ret = findBestForTwoPossibleFrames(possible, PAL, UNKNOWN);
        else if (desired == DIGITAL_30) {
            ret = findBestForTwoPossibleFrames(possible, DIGITAL_30, DIGITAL_60);
            if (ret == -1)
                ret =findBestForTwoPossibleFrames(possible, NTSC_INTERLACED, NTSC);
        }
        else if (desired == NTSC_INTERLACED) {
            ret = findBestForTwoPossibleFrames(possible, NTSC_INTERLACED, NTSC);
            if (ret == -1)
                ret = findBestForTwoPossibleFrames(possible, DIGITAL_30, DIGITAL_60);
        }
        else if (desired == PAL_FILM)
            ret = findBestForTwoPossibleFrames(possible, PAL_FILM, PAL);
        else if (desired == FILM)
            return findBestForTwoPossibleFrames(possible, FILM, UNKNOWN);

        return ret;
    }

    private float convertFrameRate(String frameRate) {

        float ret = UNKNOWN;
        if (TextUtils.isEmpty(frameRate))
            return UNKNOWN;
        if (frameRate.equals("PAL") || frameRate.startsWith("50"))
            ret = PAL;
        else if (frameRate.equals("24p") || frameRate.startsWith("23"))
            ret = FILM;
        else if (frameRate.equals("NTSC") || frameRate.startsWith("59"))
            ret = NTSC;
        else if (frameRate.startsWith("25"))
            ret = PAL_FILM;
        else if (frameRate.startsWith("29"))
            ret = NTSC_INTERLACED;
        else if (frameRate.startsWith("30"))
            ret = DIGITAL_30;
        else if (frameRate.startsWith("60"))
            ret = DIGITAL_60;
        return ret;
    }

    private double getFrameDiff(double a, double b) {
        return  Math.abs(a - b);
    }

    private void playVideo(PlexVideoItem video, Bundle extras) {

        mSelectedVideo = video;

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

    @Override
    public void handlePreTaskUI() {

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

                    mSelectedVideo = v;
                    mQueueIndex = mQueue.size();
                }

                mQueue.add(getQueueItem(v));
            }

            if (mQueueIndex == -1 && !mQueue.isEmpty()) {

                if (mSelectedVideo == null && first != null) {
                    mSelectedVideo = first;
                    mStartPosition.setVideoOffset(mSelectedVideo.getViewedOffset());
                }
                mQueueIndex = 0;
            }

            if (getActivity() == null || getActivity().isDestroyed() || getActivity().isFinishing())
                return;
            if (startPlayback && mSelectedVideo != null) {

                if (selectedHasMissingData())
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

                if (selectedHasMissingData())
                    new GetFullInfo().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mSelectedVideo.getKey());
                else
                    playVideo(video, mAutoPlayExtras);
            }
        }
    }

    @Override
    public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {

        if (item instanceof SceneCard && ((SceneCard) item).getItem() instanceof Chapter) {

            Chapter chapter = (Chapter)((SceneCard) item).getItem();
            long currPos = getCurrentPosition();
            long pos = chapter.getChapterStart();
            int prevState = getPlaybackState();
            setPlaybackState(pos > currPos ? PlaybackState.STATE_REWINDING : PlaybackState.STATE_FAST_FORWARDING);
            setPosition(pos);
            setPlaybackState(prevState);
        }
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

            new GetVideoTask(PlaybackFragment.this, mServer).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mediaId);
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

            // Update the media to skip to the previous video.
            setPlaybackState(PlaybackState.STATE_SKIPPING_TO_PREVIOUS);

            Bundle bundle = new Bundle();
            bundle.putBoolean(AUTO_PLAY, true);

            int prevIndex = --mQueueIndex;
            if (prevIndex >= 0) {

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
            if (mPlayer.getDuration() != ExoPlayer.UNKNOWN_TIME) {
                // Fast forward 10 seconds.
                int prevState = getPlaybackState();
                setPlaybackState(PlaybackState.STATE_FAST_FORWARDING);
                setPosition(mPlayer.getCurrentPosition() +
                        (Long.valueOf(getActivity().getString(R.string.skip_forward_seconds)) * 1000));
                setPlaybackState(prevState);
            }
        }

        @Override
        public void onStop() {

            getActivity().finish();
        }

        @Override
        public void onRewind() {
            // Rewind 10 seconds.
            int prevState = getPlaybackState();
            setPlaybackState(PlaybackState.STATE_REWINDING);
            setPosition(mPlayer.getCurrentPosition() -
                    (Long.valueOf(getActivity().getString(R.string.skip_back_seconds))* 1000));
            setPlaybackState(prevState);
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
}
