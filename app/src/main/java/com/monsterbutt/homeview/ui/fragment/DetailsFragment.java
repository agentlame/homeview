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
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.SparseArrayObjectAdapter;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.monsterbutt.homeview.player.track.MediaCodecCapabilities;
import com.monsterbutt.homeview.player.track.MediaTrackSelector;
import com.monsterbutt.homeview.plex.media.Chapter;
import com.monsterbutt.homeview.plex.media.Movie;
import com.monsterbutt.homeview.plex.media.Season;
import com.monsterbutt.homeview.plex.media.Show;
import com.monsterbutt.homeview.plex.media.Stream;
import com.monsterbutt.homeview.plex.tasks.ToggleWatchedStateTask;
import com.monsterbutt.homeview.presenters.CardObject;
import com.monsterbutt.homeview.presenters.CustomListRowPresenter;
import com.monsterbutt.homeview.presenters.DetailsDescriptionPresenter;
import com.monsterbutt.homeview.presenters.PosterCard;
import com.monsterbutt.homeview.ui.PlexItemRow;
import com.monsterbutt.homeview.ui.UILifecycleManager;
import com.monsterbutt.homeview.ui.activity.PlaybackActivity;
import com.monsterbutt.homeview.ui.android.HomeViewActivity;
import com.monsterbutt.homeview.ui.android.SelectView;
import com.monsterbutt.homeview.ui.android.SwitchTrackView;
import com.monsterbutt.homeview.ui.handler.CardSelectionHandler;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.PlexServerManager;
import com.monsterbutt.homeview.plex.media.Episode;
import com.monsterbutt.homeview.plex.media.PlexContainerItem;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.ui.activity.DetailsActivity;
import com.monsterbutt.homeview.ui.handler.ThemeHandler;
import com.monsterbutt.homeview.ui.handler.WatchedStatusHandler;
import com.monsterbutt.homeview.ui.handler.WatchedStatusHandler.WatchStatusListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import us.nineworlds.plex.rest.model.impl.Hub;
import us.nineworlds.plex.rest.model.impl.MediaContainer;

/*
 * LeanbackDetailsFragment extends DetailsFragment, a Wrapper fragment for leanback details screens.
 * It shows a detailed view of video and its meta plus related videos.
 */
public class DetailsFragment extends android.support.v17.leanback.app.DetailsFragment
        implements OnActionClickedListener, WatchStatusListener, DetailsDescriptionPresenter.Callback,
                CardSelectionHandler.CardSelectionListener, SelectView.SelectViewCaller, HomeViewActivity.OnBackPressedListener, CustomListRowPresenter.Callback {


    private CardSelectionHandler mSelectionHandler;
    private ThemeHandler mThemeHandler;
    private String themeKey = "";
    private UILifecycleManager mLifeCycleMgr = new UILifecycleManager();

    final static int ACTION_PLAY        = 1;
    final static int ACTION_VIEWSTATUS  = 2;
    final static int ACTION_AUDIO       = 3;
    final static int ACTION_SUBTITLES   = 4;
    final static int ACTION_DELETE      = 5;

    private ArrayObjectAdapter mAdapter;

    private PlexServer mServer;
    private PlexLibraryItem mItem = null;
    private MediaTrackSelector mTracks = null;

    private String mBackgroundURL = "";

    private CustomListRowPresenter listRowPS = null;

    private SelectView selectView = null;

    private List<PlexItemRow> mRelateds;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

        Activity activity = getActivity();
        ((HomeViewActivity) activity).setBackPressedListener(this);
        mServer = PlexServerManager.getInstance(activity.getApplicationContext(), (HomeViewActivity) activity).getSelectedServer();
        mItem = activity.getIntent().getParcelableExtra(DetailsActivity.ITEM);

        mThemeHandler = new ThemeHandler(activity, mServer, !(mItem instanceof Movie), false);

        String key = mItem != null  ? mItem.getKey()
                                    : getActivity().getIntent().getStringExtra(DetailsActivity.KEY);
        ImageView img = activity.findViewById(android.support.v17.leanback.R.id.details_overview_image);

        mBackgroundURL = getActivity().getIntent().getStringExtra(DetailsActivity.BACKGROUND);
        mSelectionHandler = new CardSelectionHandler(this, this, mServer, mItem, img, true, mBackgroundURL);
        mLifeCycleMgr.put(CardSelectionHandler.key, mSelectionHandler);
        mLifeCycleMgr.put(WatchedStatusHandler.key, new WatchedStatusHandler(mServer, this));
        mLifeCycleMgr.put(ThemeHandler.key, mThemeHandler);
        setupDetailsOverviewRowPresenter();

        if (mItem != null)
            setupDetailsOverviewRow();
        new LoadMetadataTask(mServer).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, key);
    }

    @Override
    public void onResume() {

        super.onResume();
        mLifeCycleMgr.resumed();
    }

    @Override
    public void onPause() {

        super.onPause();
        mLifeCycleMgr.paused();
    }

    private void setupDetailsOverviewRow() {

        if (TextUtils.isEmpty(mBackgroundURL) && !TextUtils.isEmpty(mItem.getBackgroundImageURL()))
            mSelectionHandler.updateBackground(mItem.getBackgroundImageURL(), true);
        boolean usePoster = !(mItem instanceof Episode);
        final DetailsOverviewRow row = new DetailsOverviewRow(mItem);

        Context context = getActivity().getApplicationContext();
        Resources res = context.getResources();
        int width = res.getDimensionPixelSize(usePoster ? R.dimen.DETAIL_POSTER_WIDTH : R.dimen.DETAIL_THUMBNAIL_WIDTH);
        int height = res.getDimensionPixelSize(usePoster ? R.dimen.DETAIL_POSTER_HEIGHT : R.dimen.DETAIL_THUMBNAIL_HEIGHT);
        Glide.with(context)
                .load(mServer.makeServerURL(usePoster ? mItem.getCardImageURL() : mItem.getWideCardImageURL()))
                .asBitmap()
                .fitCenter()
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
        adapter.set(ACTION_VIEWSTATUS, new Action(ACTION_VIEWSTATUS, context.getString(R.string.action_mark),
                 isWatched ? context.getString(R.string.action_watched) : context.getString(R.string.action_unwatched)));
        if (mItem instanceof PlexVideoItem && mTracks != null) {

            if (0 < mTracks.getCount(Stream.Audio_Stream)) {
                Stream audio = mTracks.getSelectedTrack(Stream.Audio_Stream);
                String title = audio != null ? audio.getTitle() : context.getString(R.string.audio);
                if (TextUtils.isEmpty(title))
                    title = context.getString(R.string.audio);
                String subtitle = audio != null ? audio.getLanguage() : context.getString(R.string.selection_disabled);
                if (TextUtils.isEmpty(subtitle))
                    subtitle = " ";
                adapter.set(ACTION_AUDIO, new Action(ACTION_AUDIO, title, subtitle,
                 context.getDrawable(R.drawable.ic_audiotrack_white_24dp)));
            }

            if (0 < mTracks.getCount(Stream.Subtitle_Stream)) {
                Stream subtitles = mTracks.getSelectedTrack(Stream.Subtitle_Stream);
                String title = subtitles != null ? subtitles.getLanguage() : context.getString(R.string.subtitle);
                if (TextUtils.isEmpty(title))
                    title = context.getString(R.string.subtitle);
                String subtitle = subtitles != null ? (subtitles.isForced() ? context.getString(R.string.Forced) : " ") : context.getString(R.string.selection_disabled);
                //if (TextUtils.isEmpty(subtitle))
                 //   subtitle = context.getString(R.string.track);
                adapter.set(ACTION_SUBTITLES, new Action(ACTION_SUBTITLES, title, subtitle,
                 context.getDrawable(R.drawable.ic_subtitles_white_24dp)));
            }
        }
        if (mItem != null)
            adapter.set(ACTION_DELETE, new Action(ACTION_DELETE, context.getString(R.string.delete)));

    }

    private void toggleWatched() {

        boolean isWatched = !(mItem.getWatchedState() == PlexLibraryItem.WatchedState.Watched);
        new ToggleWatchedStateTask(getActivity(), mItem).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mServer);

        DetailsOverviewRow row = (DetailsOverviewRow) mAdapter.get(0);
        setActions((SparseArrayObjectAdapter) row.getActionsAdapter(), isWatched);
        row.setItem(null);
        row.setItem(mItem);
    }

    @Override
    public void onActionClicked(Action action) {

        switch ((int) action.getId()) {
            case ACTION_PLAY:
                mItem.onPlayPressed(this, getPlaySelectionBundle(false), null);
                break;
            case ACTION_VIEWSTATUS:
                toggleWatched();
                break;
            case ACTION_AUDIO:
                selectTracks(Stream.Audio_Stream);
                break;
            case ACTION_SUBTITLES:
                selectTracks(Stream.Subtitle_Stream);
                break;
            case ACTION_DELETE:
                if (mItem != null)
                    new DeleteTask(this, mItem.getKey()).execute();
                break;
        }
    }

    @Override
    public WatchedStatusHandler.UpdateStatusList getItemsToCheck() {

        WatchedStatusHandler.UpdateStatusList list = null;
        if (mItem != null) {

            list = new WatchedStatusHandler.UpdateStatusList();
            list.add(new WatchedStatusHandler.UpdateStatus(Long.toString(mItem.getRatingKey()),
                mItem.getViewedOffset(), mItem.getWatchedState()));
        }
        CardObject card = mSelectionHandler.getSelection();
        if (card != null) {

            if (list == null)
               list = new WatchedStatusHandler.UpdateStatusList();
            list.add(card.getUpdateStatus());
        }
        return list;
    }

    @Override
    public void updatedItemsCallback(WatchedStatusHandler.UpdateStatusList items) {

        if(items != null && !items.isEmpty()) {

            CardObject card = mSelectionHandler.getSelection();
            for (WatchedStatusHandler.UpdateStatus update : items) {
                if (mItem != null && update.key.equalsIgnoreCase(Long.toString(mItem.getRatingKey()))) {

                    mItem.setStatus(items.get(0));

                    boolean isWatched = mItem.getWatchedState() == PlexLibraryItem.WatchedState.Watched;
                    DetailsOverviewRow row = (DetailsOverviewRow) mAdapter.get(0);
                    setActions((SparseArrayObjectAdapter) row.getActionsAdapter(), isWatched);
                    row.setItem(null);
                    row.setItem(mItem);
                }
                else if (card != null && mRelateds != null) {

                    for (PlexItemRow row : mRelateds) {
                        row.updatedItemsCallback(items);
                    }
                }
            }
        }
    }

    @Override
    public Bundle getPlaySelectionBundle(boolean cardIsScene) {
        Bundle extras = new Bundle();
        if (mTracks != null)
            extras.putParcelable(PlaybackActivity.TRACKS, mTracks);
        mThemeHandler.getPlaySelectionBundle(extras, themeKey);
        return extras;
    }

    @Override
    public void onCardSelected(CardObject card) {
    }

    @Override
    public MediaTrackSelector getSelector() {
        return mTracks;
    }

    public void selectTracks(int streamType) {

        releaseSelectView();
        selectView = SwitchTrackView.getTracksView(getActivity(), streamType, mTracks, null, mServer, this);
    }

    public boolean releaseSelectView() {

        if (selectView != null) {
            selectView.release();
            selectView = null;
            return true;
        }
        return false;
    }

    @Override
    public void selectionViewState(boolean isVisible, boolean shouldShowPlaybackUI) {
        if (!isVisible) {
            DetailsOverviewRow row = (DetailsOverviewRow) mAdapter.get(0);
            row.setItem(null);
            row.setItem(mItem);
            setActions((SparseArrayObjectAdapter) row.getActionsAdapter(), mItem.getWatchedState() == PlexLibraryItem.WatchedState.Watched);
            selectView = null;
        }
    }

    @Override
    public boolean backPressed() {
        return releaseSelectView();
    }

    private static class MovieDetailsOverviewLogoPresenter extends DetailsOverviewLogoPresenter {

        private final boolean usePoster;
        MovieDetailsOverviewLogoPresenter(boolean usePoster) {
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

        DetailPresenter(Presenter detailsPresenter, DetailsOverviewLogoPresenter logoPresenter) {
            super(detailsPresenter, logoPresenter);
        }
    }

    private void setupDetailsOverviewRowPresenter() {

        // Set detail background and style.
        FullWidthDetailsOverviewRowPresenter detailsPresenter =
                new DetailPresenter(new DetailsDescriptionPresenter(getActivity(), mServer, this),
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

        listRowPS = new CustomListRowPresenter(this);
        ClassPresenterSelector presenterSelector = new ClassPresenterSelector();
        presenterSelector.addClassPresenter(ListRow.class, listRowPS);
        presenterSelector.addClassPresenter(DetailsOverviewRow.class, detailsPresenter);
        mAdapter = new ArrayObjectAdapter(presenterSelector);
        setAdapter(mAdapter);
    }

    private class LoadMetadataTask extends AsyncTask<String, Void, PlexLibraryItem>  {

        private final PlexServer server;

        LoadMetadataTask(PlexServer server) {
            this.server = server;
        }

        @Override
        protected PlexLibraryItem doInBackground(String... params) {

            PlexLibraryItem ret = null;
            String key = params[0];
            MediaContainer media = server.getVideoMetadata(key, false);
            if (media != null) {

                if (media.getVideos() != null && 1 == media.getVideos().size())
                    ret = PlexVideoItem.getItem(media.getVideos().get(0));
                else {
                    if (mItem instanceof Show && 0 < media.getDirectories().size()) {
                        MediaContainer rel = server.getVideoMetadata(key.replace("/children", ""), true);
                        if (rel != null && rel.getDirectories().size() > 0) {
                            media.getDirectories().get(0).setRelated(rel.getDirectories().get(0).getRelated());
                            ((Show) mItem).setDirectories(media.getDirectories());
                        }
                        ret = mItem;
                    }
                    else
                        ret = PlexContainerItem.getItem(media);
                }
            }

            return ret;
        }

        @Override
        protected void onPostExecute(PlexLibraryItem item) {

            mItem = item;
            Activity activity = getActivity();
            boolean isVideo = item instanceof PlexVideoItem;
            boolean isShow = item instanceof Show;
            mTracks = !isVideo ? null : ((PlexVideoItem) mItem).fillTrackSelector(Locale.getDefault().getISO3Language(),
                                                                MediaCodecCapabilities.getInstance(getActivity()));

            ((DetailsOverviewRow) mAdapter.get(0)).setItem(mItem);
            DetailsOverviewRow actionrow = (DetailsOverviewRow) mAdapter.get(0);
            setActions((SparseArrayObjectAdapter) actionrow.getActionsAdapter(), mItem.getWatchedState() == PlexLibraryItem.WatchedState.Watched);

            PlexItemRow childRow = item.getChildren(activity, mServer, mSelectionHandler);
            if (childRow != null) {
                mAdapter.add(childRow);
                mLifeCycleMgr.put("childrow", childRow);

                if (childRow.getAdapter().size() > 0) {
                    long viewedOffset = item.getViewedOffset();
                    if (isVideo)
                        listRowPS.startindex = ((PlexVideoItem) item).getCurrentChapter(viewedOffset);
                    else if (isShow)
                        listRowPS.startindex = ((Show) item).getSeasonIndex((int)viewedOffset);
                }
            }
            ListRow extras = item.getExtras(activity, mServer, mSelectionHandler);
            if (extras != null)
                mAdapter.add(extras);

            HubList list = new HubList(mItem.getRelated());
            new LoadRelatedTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, list);
            themeKey = mItem.getThemeKey(mServer);
            mThemeHandler.startTheme(themeKey);
        }
    }

    private class HubList extends ArrayList<Hub> {

        HubList(List<Hub> list) {
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

            mRelateds = PlexItemRow.buildRelatedRows(getActivity(), mServer, mSelectionHandler, list);
            for (PlexItemRow row : mRelateds) {
                mAdapter.add(row);
            }
        }
    }

    private class CustomListRowPresenter extends com.monsterbutt.homeview.presenters.CustomListRowPresenter {

        int startindex = -1;

        CustomListRowPresenter(Callback caller) {
            super(caller);
        }

        @Override
        protected void onBindRowViewHolder(RowPresenter.ViewHolder holder, Object item) {
            super.onBindRowViewHolder(holder, item);

            if (startindex < 0 || ((ListRow) item).getAdapter().size() == 0)
                return;

            ObjectAdapter adapter = ((ListRow) item).getAdapter();

            if (!(adapter.get(0) instanceof PosterCard))
                return;
            PlexLibraryItem pi = ((PosterCard)adapter.get(0)).getItem();
            if (!(pi instanceof Season) && !(pi instanceof Chapter))
                return;

            ListRowPresenter.SelectItemViewHolderTask task = new ListRowPresenter.SelectItemViewHolderTask(startindex);
            task.setSmoothScroll(true);
            task.run(holder);
        }
    }

    private class DeleteTask extends AsyncTask <Void, Void, Boolean> {

        private final DetailsFragment fragment;
        private final String key;

        DeleteTask(DetailsFragment fragment, String key) {
            this.fragment = fragment;
            this.key = key;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            return mServer.deleteMedia(key);
        }

        @Override
        protected void onPostExecute(Boolean success) {

            Toast.makeText(fragment.getContext(),
             success ? R.string.delete_success : R.string.delete_failed,
             Toast.LENGTH_LONG).show();

            if (!fragment.isDetached() && !fragment.getActivity().isFinishing() &&
             !fragment.getActivity().isDestroyed())
            fragment.getActivity().finish();
        }
    }
}
