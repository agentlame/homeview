package com.monsterbutt.homeview.ui.grid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v17.leanback.app.VerticalGridFragment;
import android.support.v17.leanback.widget.TitleView;
import android.support.v17.leanback.widget.VerticalGridPresenter;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.monsterbutt.homeview.plex.StatusWatcher;
import com.monsterbutt.homeview.ui.presenters.CardObject;
import com.monsterbutt.homeview.ui.presenters.PosterCard;
import com.monsterbutt.homeview.ui.C;
import com.monsterbutt.homeview.ui.LibraryList;
import com.monsterbutt.homeview.ui.UILifecycleManager;
import com.monsterbutt.homeview.ui.search.SearchActivity;
import com.monsterbutt.homeview.ui.grid.interfaces.IGridParent;
import com.monsterbutt.homeview.ui.grid.interfaces.IGridSorter;
import com.monsterbutt.homeview.ui.grid.interfaces.IQuickJumpCallback;
import com.monsterbutt.homeview.ui.grid.interfaces.IQuickJumpDisplay;
import com.monsterbutt.homeview.ui.grid.interfaces.ISummaryDisplay;
import com.monsterbutt.homeview.ui.SelectionHandler;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.PlexServerManager;
import com.monsterbutt.homeview.plex.media.PlexContainerItem;
import com.monsterbutt.homeview.ui.BackgroundHandler;
import com.monsterbutt.homeview.ui.ThemeHandler;
import com.monsterbutt.homeview.ui.interfaces.ICardSelectionListener;

import java.util.ArrayList;
import java.util.List;

import us.nineworlds.plex.rest.model.impl.Directory;
import us.nineworlds.plex.rest.model.impl.MediaContainer;

public class GridFragment extends VerticalGridFragment
        implements ICardSelectionListener, IQuickJumpCallback, IGridParent, IGridSorter {

    private PlexServer mServer;
    private MediaContainer mContainer;
    private boolean mIsEpisodeList = false;

    private GridList mGrid;

    private final SectionFilter mAllFilter = new SectionFilter("All", PlexContainerItem.ALL);

    private BackgroundHandler backgroundHandler;
    private final UILifecycleManager mLifeCycleMgr = new UILifecycleManager();
    private ThemeHandler mThemeHandler;
    private String themeKey = "";


    private String mPassedSelectedKey;

    private ISummaryDisplay summaryDisplay;
    private IQuickJumpDisplay quickJumpDisplay;
    private SectionFilterView sectionFilterView;
    private StatusWatcher statusWatcher = new StatusWatcher();

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

    @Override
    public void onDestroy() {
      super.onDestroy();
      mGrid.release();
      mLifeCycleMgr.destroyed();
    }

    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      if (null == savedInstanceState)
        prepareEntranceTransition();

      Activity activity = getActivity();
      Intent intent = activity.getIntent();
      mIsEpisodeList = intent.getBooleanExtra(C.EPISODEIST, false);
      VerticalGridPresenter gridPresenter = new VerticalGridPresenter();
      String colCount = mIsEpisodeList ? getString(R.string.gridview_scene_columns) : getString(R.string.gridview_poster_columns);

      setTitle(null);
      gridPresenter.setNumberOfColumns(Integer.valueOf(colCount));
      setGridPresenter(gridPresenter);

      mServer = PlexServerManager.getInstance().getSelectedServer();
      mThemeHandler = new ThemeHandler(mLifeCycleMgr, getContext(), intent, !mIsEpisodeList);
      backgroundHandler = new BackgroundHandler(getActivity(), mServer, mLifeCycleMgr, intent.getStringExtra(C.BACKGROUND));
      SelectionHandler mSelectionHandler = new SelectionHandler(this, statusWatcher, backgroundHandler);
      mGrid = new GridList(activity, mServer, statusWatcher, mSelectionHandler);
      mPassedSelectedKey = intent.getStringExtra(C.SELECTED);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
      super.onActivityCreated(savedInstanceState);

      Activity activity = getActivity();

      TextView headerText = activity.findViewById(android.support.v17.leanback.R.id.title_text);
      headerText.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);

      summaryDisplay = new ItemSummaryView(activity);
      quickJumpDisplay = new QuickJumpView(activity, this);

      TitleView tv = activity.findViewById(android.support.v17.leanback.R.id.browse_title_group);
      View view = LayoutInflater.from(activity).inflate(R.layout.lb_container_header, tv, false);
      tv.addView(view);

      TypedValue typedValue = new TypedValue();
      Resources.Theme theme = getContext().getTheme();
      theme.resolveAttribute(R.attr.brand_accent, typedValue, true);
      setSearchAffordanceColor(typedValue.data);
      setOnSearchClickedListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          Intent intent = new Intent(getActivity(), SearchActivity.class);
          startActivity(intent);
        }
      });

      new SectionSortView(activity, view, this, mIsEpisodeList);
      sectionFilterView = new SectionFilterView(activity, view, this,
       mServer, backgroundHandler, mIsEpisodeList);
      new SectionHubView(activity, view, this, mIsEpisodeList);
      new GetContainerTask(mServer, activity.getIntent().getStringExtra(C.KEY)).execute(this);
    }

    @Override
    public Bundle getPlaySelectionBundle(boolean cardWasScene) {
      return mThemeHandler.getPlaySelectionBundle(null, themeKey);
    }

    @Override
    public void onCardSelected(CardObject card) {
      if (card instanceof PosterCard)
        summaryDisplay.setCurrentItem(((PosterCard) card).getItem());
    }

    @Override
    public MediaContainer getContainer() {
        return mContainer;
    }

  @Override
  public void sort(Context context, ItemSort sortType, boolean ascending) {
    quickJumpDisplay.setQuickListVisible(sortType == IGridSorter.ItemSort.Title);
    mGrid.sort(context, sortType, ascending);
  }

  private static class GetContainerTask extends AsyncTask<GridFragment, Void, GridFragment> {

      private final PlexServer server;
      private final String key;
      private MediaContainer container;

      GetContainerTask(PlexServer server, String key) {
        this.server = server;
        this.key = key;
      }

      @Override
      protected GridFragment doInBackground(GridFragment... params) {
        if (params == null || params.length == 0 || params[0] == null)
          return null;
        container = server.getVideoMetadata(key);
        return params[0];
      }

      @Override
      protected void onPostExecute(GridFragment fragment) {
        if (fragment != null)
          fragment.loadData(container);
      }
    }

    @Override
    public void loadData(MediaContainer container) {
      mContainer = container;
      if (mContainer == null)
        return;

      if (mIsEpisodeList) {
        setTitle(String.format("%s %s %s",
         mContainer.getTitle1(), getString(R.string.mid_dot), mContainer.getTitle2()));
      }
      else
        setTitle(mContainer.getTitle1());

      if (mContainer.getDirectories() != null && !mContainer.getDirectories().isEmpty() &&
       mContainer.getViewGroup().equals("secondary")) {
        SectionFilter current = null;
        List<SectionFilter> filters = new ArrayList<>();
        for(Directory dir : mContainer.getDirectories()) {
          if (dir.getSecondary() > 0 || (dir.getPrompt() != null && !dir.getPrompt().isEmpty()))
            continue;

          SectionFilter filter = new SectionFilter(dir.getTitle(), dir.getKey());
          if (filter.key.equals(PlexContainerItem.ALL)) {
            filter.name = filter.name.replace(mContainer.getTitle1(), "").trim();
            current = filter;
          }
          filters.add(filter);
        }

        if (current == null)
          current = mAllFilter;
        Activity activity = getActivity();
        if (activity.isFinishing() || activity.isDestroyed())
          return;
        sectionFilterView.runFilter(
         new SectionFilterArrayAdapter(activity, filters, current), current.key);
      }
      else {
        LibraryList.update(mGrid.getAdapter(), mGrid.getStatusWatcherObserver(),
         mGrid, mContainer, quickJumpDisplay);

        String art = mContainer.getArt();
        if (art != null && !art.isEmpty())
          backgroundHandler.updateBackground(art);
        setAdapter(mGrid != null ? mGrid.getAdapter() : null);
        setSelectedPosition(mGrid.getIndexForKey(mPassedSelectedKey));
        mPassedSelectedKey = "";
        startEntranceTransition();
      }

      themeKey = mServer.getThemeURL(mContainer);
      mThemeHandler.startTheme(themeKey);
    }
}
