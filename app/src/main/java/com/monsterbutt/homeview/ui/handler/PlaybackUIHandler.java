package com.monsterbutt.homeview.ui.handler;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackControlsRowPresenter;


import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.model.Video;
import com.monsterbutt.homeview.player.PlaybackControlHelper;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.plex.media.UpnpItem;
import com.monsterbutt.homeview.ui.activity.PlaybackActivity;
import com.monsterbutt.homeview.ui.fragment.PlaybackFragment;


public class PlaybackUIHandler extends MediaController.Callback {

    private final PlaybackControlHelper mGlue;
    private final ArrayObjectAdapter mRowsAdapter;
    private final PlaybackFragment mFragment;
    private final PlexServer mServer;

    private ListRow mCodecRow = null;
    private ListRow mExtrasRow = null;

    public PlaybackUIHandler(PlaybackFragment fragment, PlexServer server, CurrentVideoHandler currentVideoHandler) {

        mServer = server;
        mFragment = fragment;

        Activity activity = mFragment.getActivity();
        PlexVideoItem intentVideo = activity.getIntent().getParcelableExtra(PlaybackActivity.VIDEO);
        Video video = null;
        if (intentVideo != null)
            video = intentVideo.toVideo(activity);

        mGlue = new PlaybackControlHelper(activity, mFragment, video, currentVideoHandler);
        PlaybackControlsRowPresenter controlsRowPresenter = mGlue.createControlsRowAndPresenter();
        PlaybackControlsRow controlsRow = mGlue.getControlsRow();
        ClassPresenterSelector ps = new ClassPresenterSelector();
        ps.addClassPresenter(PlaybackControlsRow.class, controlsRowPresenter);
        ps.addClassPresenter(ListRow.class, new ListRowPresenter());
        mRowsAdapter = new ArrayObjectAdapter(ps);
        mFragment.setAdapter(mRowsAdapter);
        mRowsAdapter.add(controlsRow);
        updateMetadata();
    }

    private Activity getActivity() { return mFragment.getActivity(); }

    private void updateCodecAndExtras() {

        if (mCodecRow != null)
            mRowsAdapter.remove(mCodecRow);
        mCodecRow = mFragment.getPlaybackHandler().getCodecRowForVideo();
        if (mCodecRow != null)
            mRowsAdapter.add(mCodecRow);

        if (mExtrasRow != null)
            mRowsAdapter.remove(mExtrasRow);
        mExtrasRow = mFragment.getPlaybackHandler().getExtrasRowForVideo();
        if (mExtrasRow != null)
            mRowsAdapter.add(mExtrasRow);
    }

    @Override
    public void onPlaybackStateChanged(@NonNull PlaybackState state) {

        if (state.getState() != PlaybackState.STATE_NONE)
            mGlue.updateProgress();
    }

    @Override
    public void onMetadataChanged(MediaMetadata metadata) {

        mGlue.onMetadataChanged();
        mRowsAdapter.notifyArrayItemRangeChanged(0, 1);
    }

    public void setupVideoForPlayback() {

        updateMetadata();
        updateCodecAndExtras();
    }

    public void updateMetadata() {

        Context context = getActivity();
        final PlexVideoItem video = mFragment.getPlaybackHandler().getCurrentVideo();
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

        Resources res = context.getResources();
        int cardWidth = res.getDimensionPixelSize(R.dimen.playback_overlay_width);
        int cardHeight = res.getDimensionPixelSize(R.dimen.playback_overlay_height);

        String url = "";
        if (video != null) {

            if (video instanceof UpnpItem)
                url = video.getPlaybackImageURL();
            else if (mServer != null)
                url =  mServer.makeServerURL(video.getPlaybackImageURL());
        }
        Glide.with(context)
                .load(Uri.parse(url))
                .asBitmap()
                .centerCrop()
                .error(getActivity().getDrawable(R.drawable.default_video_cover))
                .into(new SimpleTarget<Bitmap>(cardWidth, cardHeight) {
                    @Override
                    public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
                        metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, bitmap);
                        mFragment.getPlaybackHandler().setMetadata(metadataBuilder.build());
                    }
                });
    }
}
