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

package com.monsterbutt.homeview.ui.details;

import android.app.Activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import android.support.v17.leanback.widget.FullWidthDetailsOverviewSharedElementHelper;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.Presenter;
import android.text.TextUtils;
import android.util.TypedValue;
import android.widget.ImageView;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.player.track.MediaCodecCapabilities;
import com.monsterbutt.homeview.player.track.MediaTrackSelector;
import com.monsterbutt.homeview.plex.media.Episode;
import com.monsterbutt.homeview.plex.media.Season;
import com.monsterbutt.homeview.plex.media.Show;
import com.monsterbutt.homeview.plex.media.Stream;
import com.monsterbutt.homeview.ui.playback.PlaybackActivity;
import com.monsterbutt.homeview.ui.presenters.CardObject;
import com.monsterbutt.homeview.ui.presenters.CardPresenter;
import com.monsterbutt.homeview.ui.presenters.CodecCard;
import com.monsterbutt.homeview.ui.presenters.CodecPresenter;
import com.monsterbutt.homeview.ui.presenters.CustomListRowPresenter;
import com.monsterbutt.homeview.settings.SettingsManager;
import com.monsterbutt.homeview.ui.C;
import com.monsterbutt.homeview.ui.details.interfaces.ITaskLibraryListener;
import com.monsterbutt.homeview.ui.details.presenters.DetailsDescriptionPresenter;
import com.monsterbutt.homeview.ui.UILifecycleManager;
import com.monsterbutt.homeview.ui.details.presenters.DetailPresenter;
import com.monsterbutt.homeview.ui.details.presenters.DetailsTrackingRowPresenter;
import com.monsterbutt.homeview.ui.details.presenters.MovieDetailsOverviewLogoPresenter;
import com.monsterbutt.homeview.ui.SelectionHandler;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.PlexServerManager;
import com.monsterbutt.homeview.plex.media.PlexContainerItem;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.ui.BackgroundHandler;
import com.monsterbutt.homeview.ui.ThemeHandler;
import com.monsterbutt.homeview.ui.interfaces.ICardSelectionListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import us.nineworlds.plex.rest.model.impl.MediaContainer;

public class DetailsFragment extends android.support.v17.leanback.app.DetailsFragment
 implements CustomListRowPresenter.Callback, ICardSelectionListener, ITaskLibraryListener {

  private ThemeHandler themeHandler;
  private String themeKey = "";
  private MediaTrackSelector tracks;
  private UILifecycleManager lifeCycleMgr = new UILifecycleManager();

  private DetailsOverviewRow detailsOverviewRow;
  private DetailsRelatedRows detailsRelatedRows;

  private BackgroundHandler backgroundHandler;

  private SelectionHandler selectionHandler;

  private String key;
  private PlexLibraryItem item;
  private PlexServer server;

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    Activity activity = getActivity();
    server = PlexServerManager.getInstance().getSelectedServer();
    Intent intent = activity.getIntent();

    themeHandler = new ThemeHandler(lifeCycleMgr, activity, activity.getIntent(), false);

    String backgroundURL = intent.getStringExtra(C.BACKGROUND);
    item = intent.getParcelableExtra(C.ITEM);
    if (item != null) {
      key = item.getKey();
      if (!TextUtils.isEmpty(item.getBackgroundImageURL()))
        backgroundURL = item.getBackgroundImageURL();
    }
    else
      key = getActivity().getIntent().getStringExtra(C.KEY);
    ImageView poster = activity.findViewById(android.support.v17.leanback.R.id.details_overview_image);

    if (!TextUtils.isEmpty(backgroundURL))
      backgroundHandler = new BackgroundHandler(getActivity(), server, lifeCycleMgr, backgroundURL);
    selectionHandler = new SelectionHandler(this, this, item, poster);

    detailsOverviewRow = new DetailsOverviewRow(this, server, item);
    setupAdapter(server, detailsOverviewRow, item != null ? item.getType() : intent.getStringExtra(C.TYPE));
  }

  @Override
  public void onResume() {
    super.onResume();
    lifeCycleMgr.resumed();

    new LoadMetadataTask(this, item, server, key).execute();
  }

  @Override
  public void onPause() {
    super.onPause();
    lifeCycleMgr.paused();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    lifeCycleMgr.destroyed();
  }

  private static class LoadMetadataTask extends AsyncTask<String, Void, PlexLibraryItem> {

    private final PlexServer server;
    private final PlexLibraryItem item;
    private final ITaskLibraryListener callback;
    private final String key;

    LoadMetadataTask(ITaskLibraryListener callback, PlexLibraryItem item, PlexServer server, String key) {
      this.server = server;
      this.item = item;
      this.callback = callback;
      this.key = key;
    }

    @Override
    protected PlexLibraryItem doInBackground(String... params) {
      PlexLibraryItem ret = null;
      MediaContainer media = server.getVideoMetadata(key);
      if (media != null) {
        if (media.getVideos() != null && 1 == media.getVideos().size())
          ret = PlexVideoItem.getItem(media.getVideos().get(0));
        else if (item instanceof Show && 0 < media.getDirectories().size()) {
          MediaContainer rel = server.getVideoMetadata(key.replace("/children", ""));
          if (rel != null && rel.getDirectories().size() > 0) {
            media.getDirectories().get(0).setRelated(rel.getDirectories().get(0).getRelated());
            ((Show) item).setDirectories(media.getDirectories());
          }
          ret = item;
        } else
          ret = PlexContainerItem.getItem(media);
      }
      return ret;
    }

    @Override
    protected void onPostExecute(PlexLibraryItem item) {
      callback.setItem(server, item);
    }
  }

  @Override
  public void setItem(PlexServer server, PlexLibraryItem item) {

    if (detailsRelatedRows != null) {
      detailsRelatedRows.release();
      detailsRelatedRows = null;
    }

    Activity activity = getActivity();
    if (backgroundHandler == null && !TextUtils.isEmpty(item.getBackgroundImageURL()))
      backgroundHandler = new BackgroundHandler(activity, server, lifeCycleMgr, item.getBackgroundImageURL());

    if (item instanceof PlexVideoItem) {
      tracks = ((PlexVideoItem) item).fillTrackSelector(Locale.getDefault().getISO3Language(),
       MediaCodecCapabilities.getInstance(getActivity()));
      detailsOverviewRow.refresh(item, tracks);
    }
    else
      tracks = null;

    ArrayObjectAdapter adapter = (ArrayObjectAdapter) getAdapter();
    addTimelineRow(server, item, selectionHandler, adapter);
    addCodecRows(server, adapter);
    addExtrasRow(server, item, selectionHandler, adapter);

    themeKey = item.getThemeKey(server);
    themeHandler.startTheme(themeKey);

    detailsRelatedRows = new DetailsRelatedRows(getContext(), lifeCycleMgr, server, item,
     adapter, new CardPresenter(server, selectionHandler, true));
  }

  private void addCodecRows(PlexServer server, ArrayObjectAdapter adapter) {
    Presenter presenter = new CodecPresenter(server);
    addCodecRow(Stream.Audio_Stream, presenter, adapter);
    addCodecRow(Stream.Subtitle_Stream, presenter, adapter);
  }

  private void addCodecRow(int streamType, Presenter presenter, ArrayObjectAdapter adapter) {
    if (tracks != null && 0 < tracks.getCount(streamType))
      DetailsCodecRow.addRow(getContext(), tracks, streamType, presenter, adapter);
  }

  private void addTimelineRow(PlexServer server, PlexLibraryItem item,
                              SelectionHandler selectionHandler, ArrayObjectAdapter adapter) {
    Context context = getContext();
    List<PlexLibraryItem> children = item.getChildrenItems();
    if (children != null) {
      boolean skipAllSeason = (item instanceof Show) &&
       !SettingsManager.getInstance().getBoolean("preferences_navigation_showallseason");
      List<PlexLibraryItem> removals = new ArrayList<>();
      for (PlexLibraryItem child : children) {
        if (skipAllSeason && child.getKey().endsWith(Season.ALL_SEASONS))
          removals.add(child);
      }
      if (!removals.isEmpty())
        children.removeAll(removals);
    }
    DetailsTimeLineRow.getRow(context, lifeCycleMgr, server, item, children, adapter,
       new CardPresenter(server, selectionHandler, item instanceof Show));
  }

  private void addExtrasRow(PlexServer server, PlexLibraryItem item,
                            SelectionHandler selectionHandler, ArrayObjectAdapter adapter) {
    Context context = getContext();
    List<PlexLibraryItem> extras = item.getExtraItems();
    if (extras != null && !extras.isEmpty()) {
      adapter.add(new DetailsExtrasRow(context, extras, getString(R.string.extras_row_header),
       new CardPresenter(server, selectionHandler, false)));
    }
  }

  void setupAdapter(PlexServer server, DetailsOverviewRow row, String type) {
    // Set detail background and style.
    FullWidthDetailsOverviewRowPresenter detailPresenter = new DetailPresenter(
     new DetailsDescriptionPresenter(getContext(), server),
     new MovieDetailsOverviewLogoPresenter(!Episode.TYPE.equals(type)));

    TypedValue typedValue = new TypedValue();
    Resources.Theme theme = getActivity().getTheme();
    theme.resolveAttribute(R.attr.card_translucent, typedValue, true);
    detailPresenter.setBackgroundColor(typedValue.data);

    theme.resolveAttribute(R.attr.card_normal, typedValue, true);
    detailPresenter.setActionsBackgroundColor(typedValue.data);

    FullWidthDetailsOverviewSharedElementHelper helper = new FullWidthDetailsOverviewSharedElementHelper();
    helper.setSharedElementEnterTransition(getActivity(), DetailsActivity.SHARED_ELEMENT_NAME);
    detailPresenter.setListener(helper);
    detailPresenter.setParticipatingEntranceTransition(true);

    ClassPresenterSelector presenterSelector = new ClassPresenterSelector();
    presenterSelector.addClassPresenter(DetailsTimeLineRow.SeasonsRow.class, new DetailsTrackingRowPresenter());
    presenterSelector.addClassPresenter(DetailsTimeLineRow.ChaptersRow.class, new DetailsTrackingRowPresenter());
    presenterSelector.addClassPresenter(DetailsCodecRow.class, new DetailsTrackingRowPresenter());
    presenterSelector.addClassPresenter(DetailsOverviewRow.class, detailPresenter);
    presenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());

    detailPresenter.setInitialState(FullWidthDetailsOverviewRowPresenter.STATE_SMALL);
    detailPresenter.setOnActionClickedListener(row);
    ArrayObjectAdapter adapter = new ArrayObjectAdapter(presenterSelector);
    adapter.add(detailsOverviewRow);
    setAdapter(adapter);
  }

  @Override
  public void onCardSelected(CardObject card) {
    if (card instanceof CodecCard)
      tracks.setSelectedTrack(null, ((CodecCard) card).getStream());
  }

  @Override
  public Bundle getPlaySelectionBundle(boolean cardIsScene) {
    Bundle extras = new Bundle();
    themeHandler.getPlaySelectionBundle(extras, themeKey);
    if (tracks != null)
      extras.putParcelable(PlaybackActivity.TRACKS, tracks);
    return extras;
  }

}
