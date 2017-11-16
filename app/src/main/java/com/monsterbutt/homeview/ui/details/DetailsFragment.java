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
import android.support.annotation.NonNull;
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
import com.monsterbutt.homeview.plex.media.Show;
import com.monsterbutt.homeview.plex.media.Stream;
import com.monsterbutt.homeview.ui.details.interfaces.IDetailsFragment;
import com.monsterbutt.homeview.ui.details.interfaces.IDetailsItem;
import com.monsterbutt.homeview.ui.details.interfaces.IDetailsItemUpdateNotifier;
import com.monsterbutt.homeview.ui.details.interfaces.IDetailsItemUpdateListener;
import com.monsterbutt.homeview.ui.details.interfaces.IDetailsScrollRowListener;
import com.monsterbutt.homeview.ui.details.interfaces.IDetailsScrollRowNotifier;
import com.monsterbutt.homeview.ui.playback.PlaybackActivity;
import com.monsterbutt.homeview.ui.presenters.CardObject;
import com.monsterbutt.homeview.ui.presenters.CardPresenter;
import com.monsterbutt.homeview.ui.presenters.CodecCard;
import com.monsterbutt.homeview.ui.presenters.CodecPresenter;
import com.monsterbutt.homeview.ui.presenters.CustomListRowPresenter;
import com.monsterbutt.homeview.ui.C;
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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import us.nineworlds.plex.rest.model.impl.MediaContainer;

public class DetailsFragment extends android.support.v17.leanback.app.DetailsFragment
 implements CustomListRowPresenter.Callback, ICardSelectionListener, IDetailsFragment {

  private static class Container implements IDetailsItem {

    private final PlexLibraryItem item;
    public final PlexServer server;
    public final String key;
    private final MediaTrackSelector tracks;

    Container(PlexLibraryItem item, MediaTrackSelector tracks, PlexServer server, String key) {
      this.item = item;
      this.tracks = tracks;
      this.server = server;
      this.key = key;
    }

    @Override
    public PlexServer server() { return server; }

    @Override
    public PlexLibraryItem item() {
      return item;
    }

    @Override
    public MediaTrackSelector tracks() {
      return tracks;
    }
  }

  private ThemeHandler themeHandler;
  private String themeKey = "";
  private UILifecycleManager lifeCycleMgr = new UILifecycleManager();

  private BackgroundHandler backgroundHandler;

  private SelectionHandler selectionHandler;
  private Notifier notifier;
  private Scroller scrollerSeasons = new Scroller();
  private Scroller scrollerChapters = new Scroller();

  private boolean wasSetup = false;

  private static class Scroller implements IDetailsScrollRowNotifier {

    private Map<String, IDetailsScrollRowListener> map = new HashMap<>();

    @Override
    public synchronized void notifiy(int index) {
      for(IDetailsScrollRowListener listener : map.values())
        listener.scrollToIndex(index);
    }

    @Override
    public synchronized void register(IDetailsScrollRowListener listener) {
      String key = listener.getKey();
      if (!map.containsKey(key)) {
        map.put(key, listener);
      }
    }

    @Override
    public synchronized void release(IDetailsScrollRowListener listener) {
      String key = listener.getKey();
      if (!map.containsKey(key)) {
        map.put(key, listener);
      }
    }
  }

  private static class Notifier implements IDetailsItemUpdateNotifier {

    Notifier(@NonNull IDetailsItem item) {
      this.item = item;
    }

    synchronized IDetailsItem getObject() { return item; }

    private Map<String, IDetailsItemUpdateListener> map = new HashMap<>();
    private IDetailsItem item;

    synchronized void setObject(IDetailsItem item) {
      this.item = item;
      for (IDetailsItemUpdateListener listener : map.values())
        listener.update(item);
    }

    @Override
    public void register(IDetailsItemUpdateListener listener) {
      String key = listener.getKey();
      if (!map.containsKey(key)) {
        map.put(key, listener);
      }
    }

    @Override
    public void release(IDetailsItemUpdateListener listener) {
      map.remove(listener.getKey());
    }
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    Activity activity = getActivity();
    PlexServer server = PlexServerManager.getInstance().getSelectedServer();
    Intent intent = activity.getIntent();

    themeHandler = new ThemeHandler(lifeCycleMgr, activity, activity.getIntent(), false);

    String key;
    String backgroundURL = intent.getStringExtra(C.BACKGROUND);
    PlexLibraryItem item = intent.getParcelableExtra(C.ITEM);
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
    notifier = new Notifier(new Container(item, null, server, key));
    setupAdapter(notifier, item != null ? item.getType() : intent.getStringExtra(C.TYPE),
     scrollerSeasons, scrollerChapters);
  }

  @Override
  public void onResume() {
    super.onResume();
    lifeCycleMgr.resumed();
    Container container = (Container) notifier.getObject();
    new LoadMetadataTask(this, container.item, container.server, container.key).execute();
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

  @Override
  public void setItem(PlexServer server, PlexLibraryItem item) {
    MediaTrackSelector tracks = null;
    if (item instanceof PlexVideoItem) {
      tracks = ((PlexVideoItem) item).fillTrackSelector(Locale.getDefault().getISO3Language(),
       MediaCodecCapabilities.getInstance(getActivity()));
    }
    shouldSetupRows(item, tracks, server);
    notifier.setObject(new Container(item, tracks, server, item.getKey()));
  }

  private void shouldSetupRows(PlexLibraryItem item, MediaTrackSelector tracks, PlexServer server) {
    if (wasSetup)
      return;
    wasSetup = true;

    if (backgroundHandler == null && !TextUtils.isEmpty(item.getBackgroundImageURL()))
      backgroundHandler = new BackgroundHandler(getActivity(), server, lifeCycleMgr, item.getBackgroundImageURL());

    Context context = getContext();
    ArrayObjectAdapter adapter = (ArrayObjectAdapter) getAdapter();

    String title = item.getHeaderForChildren(context);
    if (item instanceof Show && ((Show) item).hasSeasons()) {
      adapter.add(new DetailsSeasonsRow(context, notifier, server, title, scrollerSeasons,
       new CardPresenter(server, selectionHandler, true)));
    }
    else if (item instanceof PlexVideoItem && ((PlexVideoItem) item).hasChapters()) {
      adapter.add(new DetailsChaptersRow(context, notifier, title, scrollerChapters,
       new CardPresenter(server, selectionHandler, false)));
    }
    addCodecRows(server, tracks, adapter);
    addExtrasRow(server, item, selectionHandler, adapter);

    themeKey = item.getThemeKey(server);
    themeHandler.startTheme(themeKey);

    new DetailsRelatedRows(context, lifeCycleMgr, server, item, adapter,
     new CardPresenter(server, selectionHandler, true));
  }

  private void addCodecRows(PlexServer server, MediaTrackSelector tracks, ArrayObjectAdapter adapter) {
    Presenter presenter = new CodecPresenter(server);
    addCodecRow(tracks, Stream.Audio_Stream, presenter, adapter);
    addCodecRow(tracks, Stream.Subtitle_Stream, presenter, adapter);
  }

  private void addCodecRow(MediaTrackSelector tracks, int streamType, Presenter presenter, ArrayObjectAdapter adapter) {
    if (tracks != null && 0 < tracks.getCount(streamType)) {
      String header = getString(streamType == Stream.Audio_Stream ?
       R.string.exo_controls_audio_description : R.string.exo_controls_subtitles_description);
      adapter.add(new DetailsCodecRow(getContext(), header, tracks, streamType, presenter));
    }
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

  void setupAdapter(Notifier notifier, String type,
                    Scroller scrollerSeasons, Scroller scrollerChapters) {
    Container container = (Container) notifier.getObject();
    // Set detail background and style.
    FullWidthDetailsOverviewRowPresenter detailPresenter = new DetailPresenter(
     new DetailsDescriptionPresenter(getContext(), container.server),
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
    DetailsTrackingRowPresenter presenterSeasons = new DetailsTrackingRowPresenter("seasons");
    scrollerSeasons.register(presenterSeasons);
    presenterSelector.addClassPresenter(DetailsSeasonsRow.class, presenterSeasons);
    DetailsTrackingRowPresenter presenterChapters = new DetailsTrackingRowPresenter("chapters");
    scrollerChapters.register(presenterChapters);
    presenterSelector.addClassPresenter(DetailsChaptersRow.class, presenterChapters);
    presenterSelector.addClassPresenter(DetailsCodecRow.class, new DetailsTrackingRowPresenter(""));
    presenterSelector.addClassPresenter(DetailsOverviewRow.class, detailPresenter);
    presenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());

    detailPresenter.setInitialState(FullWidthDetailsOverviewRowPresenter.STATE_SMALL);
    DetailsOverviewRow row = new DetailsOverviewRow(this, container, notifier);
    detailPresenter.setOnActionClickedListener(row);
    ArrayObjectAdapter adapter = new ArrayObjectAdapter(presenterSelector);
    adapter.add(row);
    setAdapter(adapter);
  }

  @Override
  public void onCardSelected(CardObject card) {
    if (card instanceof CodecCard)
      notifier.getObject().tracks().setSelectedTrack(null, ((CodecCard) card).getStream());
  }

  @Override
  public Bundle getPlaySelectionBundle(boolean cardIsScene) {
    Bundle extras = new Bundle();
    themeHandler.getPlaySelectionBundle(extras, themeKey);
    MediaTrackSelector tracks = notifier.getObject().tracks();
    if (tracks != null)
      extras.putParcelable(PlaybackActivity.TRACKS, tracks);
    return extras;
  }

  private static class LoadMetadataTask extends AsyncTask<String, Void, PlexLibraryItem>  {

    private final PlexServer server;
    private final PlexLibraryItem item;
    private final IDetailsFragment fragment;
    private final String key;

    LoadMetadataTask(IDetailsFragment fragment, PlexLibraryItem item, PlexServer server, String key) {
      this.server = server;
      this.item = item;
      this.fragment = fragment;
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
      fragment.setItem(server, item);
    }

  }

}
