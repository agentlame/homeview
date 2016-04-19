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

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.DetailsOverviewLogoPresenter;
import android.support.v17.leanback.widget.DetailsOverviewRow;
import android.support.v17.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import android.support.v17.leanback.widget.FullWidthDetailsOverviewSharedElementHelper;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.SparseArrayObjectAdapter;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.monsterbutt.homeview.player.MediaCodecCapabilities;
import com.monsterbutt.homeview.player.MediaTrackSelector;
import com.monsterbutt.homeview.plex.media.Season;
import com.monsterbutt.homeview.plex.tasks.ToggleWatchedStateTask;
import com.monsterbutt.homeview.presenters.DetailsDescriptionPresenter;
import com.monsterbutt.homeview.presenters.CardPresenter;
import com.monsterbutt.homeview.presenters.CodecCard;
import com.monsterbutt.homeview.services.ThemeService;
import com.monsterbutt.homeview.ui.activity.PlaybackActivity;
import com.monsterbutt.homeview.ui.android.ImageCardView;
import com.monsterbutt.homeview.ui.handler.MediaCardBackgroundHandler;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.ui.android.HomeViewActivity;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.PlexServerManager;
import com.monsterbutt.homeview.plex.media.Episode;
import com.monsterbutt.homeview.plex.media.PlexContainerItem;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.presenters.CardObject;
import com.monsterbutt.homeview.presenters.PosterCard;
import com.monsterbutt.homeview.ui.activity.DetailsActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import us.nineworlds.plex.rest.model.impl.Hub;
import us.nineworlds.plex.rest.model.impl.MediaContainer;
import us.nineworlds.plex.rest.model.impl.Video;

/*
 * LeanbackDetailsFragment extends DetailsFragment, a Wrapper fragment for leanback details screens.
 * It shows a detailed view of video and its meta plus related videos.
 */
public class DetailsFragment extends android.support.v17.leanback.app.DetailsFragment implements OnItemViewClickedListener, OnActionClickedListener, HomeViewActivity.OnPlayKeyListener, OnItemViewSelectedListener {

    private View mCurrentCardTransitionImage = null;
    private CardObject mCurrentCard = null;
    private DetailsDescriptionPresenter mDetailPresenter;
    private boolean mContinueTheme = false;
    private boolean mThemeAlreadyRun = false;

    final static int ACTION_PLAY        = 1;
    final static int ACTION_VIEWSTATUS  = 2;

    private ArrayObjectAdapter mAdapter;

    private PlexServer mServer;
    private PlexLibraryItem mItem = null;
    private MediaCardBackgroundHandler mBackgroundHandler;
    private String mBackgroundURL = "";
    private MediaTrackSelector mTracks = null;

    private ListRow mCodecRow = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Activity activity = getActivity();
        mServer = PlexServerManager.getInstance(activity.getApplicationContext()).getSelectedServer();
        mBackgroundHandler = new MediaCardBackgroundHandler(activity);

        mItem = activity.getIntent().getParcelableExtra(DetailsActivity.ITEM);
        mBackgroundURL = activity.getIntent().getStringExtra(DetailsActivity.BACKGROUND);
        mThemeAlreadyRun = activity.getIntent().getBooleanExtra(ThemeService.THEME_ALREADY_RUN, false);
        setOnItemViewClickedListener(this);
        setOnItemViewSelectedListener(this);
        setupDetailsOverviewRowPresenter();

        if (!TextUtils.isEmpty(mBackgroundURL)) {
            mBackgroundURL = mServer.makeServerURL(mBackgroundURL);
            mBackgroundHandler.updateBackground(mBackgroundURL, false);
        }
        if (mItem != null)
            setupDetailsOverviewRow();
    }

    @Override
    public void onResume() {
        super.onResume();
        mContinueTheme = false;
        if (!TextUtils.isEmpty(mBackgroundURL))
            mBackgroundHandler.updateBackground(mBackgroundURL, false);
    }

    @Override
    public void onPause() {

        super.onPause();

        mBackgroundHandler.cancel();
        if (mContinueTheme || !mThemeAlreadyRun ||
                (getActivity() != null && getActivity().isFinishing() &&
                        (mItem instanceof Episode || mItem instanceof Season)))
            return;
        ThemeService.stopTheme(getActivity());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);
        ((HomeViewActivity) getActivity()).setPlayKeyListener(this);

        String key = mItem != null  ? mItem.getKey()
                                    : getActivity().getIntent().getStringExtra(DetailsActivity.KEY);
        new LoadMetadataTask(mServer).execute(key);
    }

    private void setupDetailsOverviewRow() {

        if (TextUtils.isEmpty(mBackgroundURL)) {
            mBackgroundURL = mServer.makeServerURL(mItem.getBackgroundImageURL());
            mBackgroundHandler.updateBackground(mBackgroundURL, true);
        }
        boolean usePoster = !(mItem instanceof Episode);
        final DetailsOverviewRow row = new DetailsOverviewRow(mItem);

        Context context = getActivity().getApplicationContext();
        Resources res = context.getResources();
        int width = res.getDimensionPixelSize(usePoster ? R.dimen.DETAIL_POSTER_WIDTH : R.dimen.DETAIL_THUMBNAIL_WIDTH);
        int height = res.getDimensionPixelSize(usePoster ? R.dimen.DETAIL_POSTER_HEIGHT : R.dimen.DETAIL_THUMBNAIL_HEIGHT);
        Glide.with(context)
                .load(mServer.makeServerURL(usePoster ? mItem.getCardImageURL() : mItem.getWideCardImageURL()))
                .asBitmap()
                .dontAnimate()
                .error(R.drawable.default_background)
                .into(new SimpleTarget<Bitmap>(width, height) {
                    @Override
                    public void onResourceReady(final Bitmap resource,
                                                GlideAnimation glideAnimation) {
                        row.setImageBitmap(getActivity(), resource);
                        startEntranceTransition();
                    }
                });


        SparseArrayObjectAdapter actions = new SparseArrayObjectAdapter();
        setActions(actions, mItem.getWatchedState() == PlexLibraryItem.WatchedState.Watched);
        row.setActionsAdapter(actions);
        mAdapter.add(row);
    }

    private void setActions(SparseArrayObjectAdapter adapter, boolean isWatched) {

        Context context = getActivity();
        adapter.set(ACTION_PLAY, new Action(ACTION_PLAY, context.getString(R.string.action_play)));
        adapter.set(ACTION_VIEWSTATUS,
                new Action(ACTION_VIEWSTATUS, isWatched ? context.getString(R.string.action_watched)
                        : context.getString(R.string.action_unwatched)));
    }

    private void toggleWatched() {

        boolean isWatched = !(mItem.getWatchedState() == PlexLibraryItem.WatchedState.Watched);
        mDetailPresenter.setWatchedState(isWatched);
        setActions((SparseArrayObjectAdapter) ((DetailsOverviewRow) mAdapter.get(0)).getActionsAdapter(),
                isWatched);
        new ToggleWatchedStateTask(mItem).execute(mServer);
    }

    private Bundle bundleTracks() {

        Bundle extras = new Bundle();
        extras.putParcelable(PlaybackActivity.TRACKS, mTracks);
        return extras;
    }

    @Override
    public void onActionClicked(Action action) {

        if (action.getId() == ACTION_PLAY)
            mItem.onPlayPressed(this, bundleTracks(), null);

        else if (action.getId() == ACTION_VIEWSTATUS)
            toggleWatched();
    }

    static class MovieDetailsOverviewLogoPresenter extends DetailsOverviewLogoPresenter {

        private final boolean usePoster;
        public MovieDetailsOverviewLogoPresenter(boolean usePoster) {
            this.usePoster = usePoster;
        }

        static class ViewHolder extends DetailsOverviewLogoPresenter.ViewHolder {
            public ViewHolder(View view) {
                super(view);
            }

            public FullWidthDetailsOverviewRowPresenter getParentPresenter() {
                return mParentPresenter;
            }

            public FullWidthDetailsOverviewRowPresenter.ViewHolder getParentViewHolder() {
                return mParentViewHolder;
            }
        }

        @Override
        public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
            ImageView imageView = (ImageView) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.lb_fullwidth_details_overview_logo, parent, false);

            Resources res = parent.getResources();
            int width = res.getDimensionPixelSize(usePoster ? R.dimen.DETAIL_POSTER_WIDTH : R.dimen.DETAIL_THUMBNAIL_WIDTH);
            int height = res.getDimensionPixelSize(usePoster ? R.dimen.DETAIL_POSTER_HEIGHT : R.dimen.DETAIL_THUMBNAIL_HEIGHT);
            imageView.setLayoutParams(new ViewGroup.MarginLayoutParams(width, height));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            return new ViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
            DetailsOverviewRow row = (DetailsOverviewRow) item;
            ImageView imageView = ((ImageView) viewHolder.view);
            imageView.setImageDrawable(row.getImageDrawable());
            if (isBoundToImage((ViewHolder) viewHolder, row)) {

                MovieDetailsOverviewLogoPresenter.ViewHolder vh =
                        (MovieDetailsOverviewLogoPresenter.ViewHolder) viewHolder;
                vh.getParentPresenter().notifyOnBindLogo(vh.getParentViewHolder());
            }
        }
    }

    private class DetailPresenter extends FullWidthDetailsOverviewRowPresenter {

        @Override
        protected int getLayoutResourceId() {
            return R.layout.lb_fullwidth_details_overview;
        }

        public DetailPresenter(Presenter detailsPresenter, DetailsOverviewLogoPresenter logoPresenter) {
            super(detailsPresenter, logoPresenter);
        }
    }

    private void setupDetailsOverviewRowPresenter() {
        // Set detail background and style.
        mDetailPresenter = new DetailsDescriptionPresenter(getActivity(), mServer);
        DetailPresenter detailsPresenter =
                new DetailPresenter(mDetailPresenter,
                                    new MovieDetailsOverviewLogoPresenter(!(mItem instanceof Episode)));

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getActivity().getTheme();
        theme.resolveAttribute(R.attr.card_translucent, typedValue, true);
        detailsPresenter.setBackgroundColor(typedValue.data);
        theme.resolveAttribute(R.attr.card_normal, typedValue, true);
        detailsPresenter.setActionsBackgroundColor(typedValue.data);
        detailsPresenter.setInitialState(FullWidthDetailsOverviewRowPresenter.STATE_SMALL);
        FullWidthDetailsOverviewSharedElementHelper helper = new FullWidthDetailsOverviewSharedElementHelper();
        helper.setSharedElementEnterTransition(getActivity(),
                DetailsActivity.SHARED_ELEMENT_NAME);
        detailsPresenter.setListener(helper);
        detailsPresenter.setParticipatingEntranceTransition(false);

        detailsPresenter.setOnActionClickedListener(this);

        ClassPresenterSelector presenterSelector = new ClassPresenterSelector();
        presenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());
        presenterSelector.addClassPresenter(DetailsOverviewRow.class, detailsPresenter);
        mAdapter = new ArrayObjectAdapter(presenterSelector);
        setAdapter(mAdapter);
    }

    @Override
    public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                              RowPresenter.ViewHolder rowViewHolder, Row row) {

        if (item instanceof PosterCard && !(item instanceof CodecCard)) {
            mContinueTheme = true; // keep playing music through episodes
            Bundle extras = mTracks != null ? bundleTracks() : new Bundle();
            extras.putBoolean(ThemeService.THEME_ALREADY_RUN, true);
            ((PosterCard) item).onClicked(this, extras, ((ImageCardView) itemViewHolder.view).getMainImageView());
        }
        else if (item instanceof CodecCard) {

            final CodecCard card = (CodecCard) item;
            final int trackTypeClicked = card.getTrackType();
            card.onCardClicked(getActivity(), mServer, mTracks, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    mTracks.setSelectedTrack(trackTypeClicked, which);
                    ArrayObjectAdapter adapter = (ArrayObjectAdapter) mCodecRow.getAdapter();
                    int index = adapter.indexOf(card);
                    if (0 <= index) {
                        adapter.replace(index, new CodecCard(getActivity(),
                                                             mTracks.getSelectedTrack(trackTypeClicked),
                                                             trackTypeClicked,
                                                             mTracks.getCountForType(trackTypeClicked)));

                        adapter.notifyArrayItemRangeChanged(index, 1);
                    }
                    dialog.dismiss();
                }
            });
        }
    }

    @Override
    public boolean playKeyPressed() {

        return mCurrentCard != null && mCurrentCard.onPlayPressed(this, bundleTracks(), mCurrentCardTransitionImage);
    }

    @Override
    public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {

        if (item instanceof CardObject && !(item instanceof CodecCard)) {
            mCurrentCard = (CardObject) item;
            mCurrentCardTransitionImage = ((ImageCardView) itemViewHolder.view).getMainImageView();
        }
    }

    private class LoadMetadataTask extends AsyncTask<String, Void, PlexLibraryItem> {

        private final PlexServer server;


        public LoadMetadataTask(PlexServer server) {
            this.server = server;
        }

        @Override
        protected PlexLibraryItem doInBackground(String... params) {

            PlexLibraryItem ret = null;
            MediaContainer media = server.getVideoMetadata(params[0]);
            if (media != null) {

                if (media.getVideos() != null && 1 == media.getVideos().size())
                    ret = PlexVideoItem.getItem(media.getVideos().get(0));
                else
                    ret = PlexContainerItem.getItem(media);
            }

            return ret;
        }

        @Override
        protected void onPostExecute(PlexLibraryItem item) {

            mItem = item;
            Activity activity = getActivity();
            if (item instanceof PlexVideoItem) {
                mTracks = ((PlexVideoItem) mItem).fillTrackSelector(getActivity(),
                                                                   Locale.getDefault().getISO3Language(),
                                                                    MediaCodecCapabilities.getInstance(getActivity()));

                mCodecRow = ((PlexVideoItem) mItem).getCodecsRow(activity, mServer, mTracks);
                mAdapter.add(mCodecRow);
            }
            else
                mTracks = null;
            ListRow children = item.getChildren(activity, mServer);
            if (children != null)
                mAdapter.add(children);
            ListRow extras = item.getExtras(activity, mServer);
            if (extras != null)
                mAdapter.add(extras);

            HubList list = new HubList(mItem.getRelated());
            new LoadRelatedTask().execute(list);

            if (!mThemeAlreadyRun)
                mThemeAlreadyRun = ThemeService.startTheme(getActivity(), mItem.getThemeKey(mServer));
        }
    }

    private class HubList extends ArrayList<Hub> {

        public HubList(List<Hub> list) {
            if (list != null && !list.isEmpty())
               this.addAll(list);
        }
    }

    private class LoadRelatedTask extends AsyncTask<HubList, Void, List<MediaContainer>> {

        @Override
        protected List<MediaContainer> doInBackground(HubList... params) {

            List<MediaContainer> mcs = new ArrayList<>();
            if (params != null && params.length > 0 && params[0] != null) {

                HubList list = params[0];
                for (Hub hub : list) {

                    MediaContainer mc = mServer.getRelatedForKey(hub.getKey());
                    if (mc != null) {
                        mc.setTitle1(hub.getTitle());
                        mcs.add(mc);
                    }
                }
            }

            return mcs;
        }

        @Override
        protected void onPostExecute(List<MediaContainer> list) {

            Activity act = getActivity();
            for (MediaContainer mc : list) {

                if (mc.getVideos() != null && !mc.getVideos().isEmpty()) {
                    ArrayObjectAdapter adapter = new ArrayObjectAdapter(new CardPresenter(mServer));
                    ListRow row = new ListRow(new HeaderItem(0, mc.getTitle1()),
                            adapter);
                    for (Video video: mc.getVideos())
                        adapter.add(new PosterCard(act, PlexVideoItem.getItem(video)));
                    mAdapter.add(row);
                }
            }
        }
    }

}
