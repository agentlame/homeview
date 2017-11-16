package com.monsterbutt.homeview.ui.grid;

import android.content.Context;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.text.TextUtils;

import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.StatusWatcher;
import com.monsterbutt.homeview.plex.media.Episode;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.ui.presenters.CardObject;
import com.monsterbutt.homeview.ui.presenters.CardPresenter;
import com.monsterbutt.homeview.ui.presenters.PosterCard;
import com.monsterbutt.homeview.ui.presenters.PosterCardExpanded;
import com.monsterbutt.homeview.ui.presenters.SceneCardExpanded;
import com.monsterbutt.homeview.ui.C;
import com.monsterbutt.homeview.ui.CardObjectList;
import com.monsterbutt.homeview.ui.grid.interfaces.IGridSorter;
import com.monsterbutt.homeview.ui.SelectionHandler;
import com.monsterbutt.homeview.ui.interfaces.ICardObjectListCallback;
import com.monsterbutt.homeview.ui.interfaces.IMediaObserver;
import com.monsterbutt.homeview.ui.interfaces.IRegisteredMedia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import us.nineworlds.plex.rest.model.impl.IContainer;

public class GridList implements CardPresenter.LongClickWatchStatusCallback, IGridSorter,
 ICardObjectListCallback, IMediaObserver {

  private ArrayObjectAdapter adapter = new ArrayObjectAdapter();
  private Map<String, PosterCard> map = new HashMap<>();
  private final Context context;
  private final StatusWatcher.StatusWatcherObserver statusWatcher;

  GridList(Context context, PlexServer server, SelectionHandler listener) {
    CardPresenter presenter = new CardPresenter(server, listener, true);
    presenter.setLongClickWatchStatusCallback(this);
    this.adapter = new ArrayObjectAdapter(presenter);
    this.context = context;
    statusWatcher = server.registerUIObserver(this);
  }

  public ArrayObjectAdapter getAdapter() { return adapter; }

  StatusWatcher.StatusWatcherObserver getStatusWatcherObserver() { return statusWatcher; }

  int getIndexForKey(String key) {
    if (TextUtils.isEmpty(key))
      return 0;
    PosterCard card = map.get(key);
    return card != null ? adapter.indexOf(card) : 0;
  }

  @Override
  public void resetSelected(CardObject card) {
    if (card != null) {
      int index = adapter.indexOf(card);
      if (index != -1)
        adapter.notifyArrayItemRangeChanged(index, 1);
    }
  }

  @Override
  public void removeSelected(CardObject card) {
    if (card != null) {
      adapter.remove(card);
      statusWatcher.release(Collections.singletonList((IRegisteredMedia) card));
      map.remove(card.getKey());
    }
  }

  public void release() {

    List<IRegisteredMedia> list = new ArrayList<>();
    for (int i = 0; i < adapter.size(); ++i)
      list.add((IRegisteredMedia) adapter.get(i));
    statusWatcher.release(list);
  }

  @Override
  public void shouldAdd(IContainer container, PlexLibraryItem item, CardObjectList objects) {
    if (item != null) {
      PosterCard card;
      if (item instanceof Episode) {
        ((Episode) item).setSeasonNum(container.getParentIndex());
        card = new SceneCardExpanded(context, item);
      }
      else
        card = new PosterCardExpanded(context, item);
      if (!map.containsKey(item.getKey()))
        map.put(item.getKey(), card);
      objects.add(item.getKey(), card);
    }
  }

  @Override
  public void statusChanged(IRegisteredMedia media, C.StatusChanged status) {
    PosterCard card = map.get(media.getKey());
    if (card != null) {
      if (status == C.StatusChanged.SetDeleted) {
        adapter.remove(media);
        statusWatcher.release(Collections.singletonList((media)));
      }
      else {
        int index = adapter.indexOf(media);
        if (status != C.StatusChanged.Refresh)
          ((PosterCard) adapter.get(index)).setWatchState(status == C.StatusChanged.SetWatched ?
           PlexLibraryItem.WatchedState.Watched : PlexLibraryItem.WatchedState.Unwatched);
        adapter.notifyItemRangeChanged(index, 1);
      }
    }
  }

  @Override
  public void sort(Context context, IGridSorter.ItemSort sortType, boolean ascending) {
    List<PosterCard> items = new ArrayList<>(map.values());
    Collections.sort(items, getComparator(sortType, ascending));
    adapter.clear();
    for (PosterCard item : items)
      adapter.add(item);
  }

  private Comparator<PosterCard> getComparator(IGridSorter.ItemSort sortType, boolean ascending) {
    switch (sortType) {
      case DateAdded:
        return ascending ? SortDateAddedAsc : SortDateAddedDesc;
      case Duration:
        return ascending ? SortDurationAsc : SortDurationDesc;
      case LastViewed:
        return ascending ? SortLastViewedAsc : SortLastViewedDesc;
      case Rating:
        return ascending ? SortRatingAsc : SortRatingDesc;
      case ReleaseDate:
        return ascending ? SortReleaseDateAsc : SortReleaseDateDesc;
      case Title:
        return ascending ? SortTitleAsc : SortTitleDesc;
      default:
        return null;
    }
  }

  private static Comparator<PosterCard> SortDateAddedAsc = new Comparator<PosterCard>() {
    @Override
    public int compare(PosterCard lhs, PosterCard rhs) {
      if (lhs.getItem().getAddedAt() > rhs.getItem().getAddedAt())
        return 1;
      return -1;
    }
  };

  private static Comparator<PosterCard> SortDateAddedDesc = new Comparator<PosterCard>() {
    @Override
    public int compare(PosterCard lhs, PosterCard rhs) {
      if (rhs.getItem().getAddedAt() > lhs.getItem().getAddedAt())
        return 1;
      return -1;
    }
  };

  private static Comparator<PosterCard> SortDurationAsc = new Comparator<PosterCard>() {
    @Override
    public int compare(PosterCard lhs, PosterCard rhs) {
      if (lhs.getItem().getDuration() > rhs.getItem().getDuration())
        return 1;
      return -1;
    }
  };

  private static Comparator<PosterCard> SortDurationDesc = new Comparator<PosterCard>() {
    @Override
    public int compare(PosterCard lhs, PosterCard rhs) {
      if (rhs.getItem().getDuration() > lhs.getItem().getDuration())
        return 1;
      return -1;
    }
  };

  private static Comparator<PosterCard> SortLastViewedAsc = new Comparator<PosterCard>() {
    @Override
    public int compare(PosterCard lhs, PosterCard rhs) {
      if (lhs.getItem().getLastViewedAt() > rhs.getItem().getLastViewedAt())
        return 1;
      return -1;
    }
  };

  private static Comparator<PosterCard> SortLastViewedDesc = new Comparator<PosterCard>() {
    @Override
    public int compare(PosterCard lhs, PosterCard rhs) {
      if (rhs.getItem().getLastViewedAt() > lhs.getItem().getLastViewedAt())
        return 1;
      return -1;
    }
  };

  private static Comparator<PosterCard> SortRatingAsc = new Comparator<PosterCard>() {
    @Override
    public int compare(PosterCard lhs, PosterCard rhs) {
      return lhs.getItem().getRating().compareTo(rhs.getItem().getRating());
    }
  };

  private static Comparator<PosterCard> SortRatingDesc = new Comparator<PosterCard>() {
    @Override
    public int compare(PosterCard lhs, PosterCard rhs) {
      return rhs.getItem().getRating().compareTo(lhs.getItem().getRating());
    }
  };

  private static Comparator<PosterCard> SortReleaseDateAsc = new Comparator<PosterCard>() {
    @Override
    public int compare(PosterCard lhs, PosterCard rhs) {
      return lhs.getItem().getOriginalAvailableDate().compareTo(rhs.getItem().getOriginalAvailableDate());
    }
  };

  private static Comparator<PosterCard> SortReleaseDateDesc = new Comparator<PosterCard>() {
    @Override
    public int compare(PosterCard lhs, PosterCard rhs) {
      return rhs.getItem().getOriginalAvailableDate().compareTo(lhs.getItem().getOriginalAvailableDate());
    }
  };

  private static Comparator<PosterCard> SortTitleAsc = new Comparator<PosterCard>() {
    @Override
    public int compare(PosterCard lhs, PosterCard rhs) {
      return lhs.getItem().getSortTitle().compareTo(rhs.getItem().getSortTitle());
    }
  };

  private static Comparator<PosterCard> SortTitleDesc = new Comparator<PosterCard>() {
    @Override
    public int compare(PosterCard lhs, PosterCard rhs) {
      return rhs.getItem().getSortTitle().compareTo(lhs.getItem().getSortTitle());
    }
  };
}
