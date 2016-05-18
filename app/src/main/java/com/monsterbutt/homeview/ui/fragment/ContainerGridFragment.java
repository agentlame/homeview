package com.monsterbutt.homeview.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v17.leanback.app.VerticalGridFragment;
import android.support.v17.leanback.widget.TitleView;
import android.support.v17.leanback.widget.VerticalGridPresenter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.monsterbutt.homeview.plex.media.Episode;
import com.monsterbutt.homeview.presenters.CardObject;
import com.monsterbutt.homeview.ui.PlexItemGrid;
import com.monsterbutt.homeview.ui.UILifecycleManager;
import com.monsterbutt.homeview.ui.activity.FilterChoiceActivity;
import com.monsterbutt.homeview.ui.activity.PlaybackActivity;
import com.monsterbutt.homeview.ui.activity.SectionHubActivity;
import com.monsterbutt.homeview.ui.handler.CardSelectionHandler;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.PlexServerManager;
import com.monsterbutt.homeview.plex.media.PlexContainerItem;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.ui.activity.ContainerActivity;
import com.monsterbutt.homeview.ui.handler.ThemeHandler;
import com.monsterbutt.homeview.ui.handler.WatchedStatusHandler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import us.nineworlds.plex.rest.model.impl.Directory;
import us.nineworlds.plex.rest.model.impl.MediaContainer;
import us.nineworlds.plex.rest.model.impl.Video;

public class ContainerGridFragment extends VerticalGridFragment
        implements  WatchedStatusHandler.WatchStatusListener, CardSelectionHandler.CardSelectionListener {

    private static final int RESULT_FILTER = 1;
    private static final int RESULT_SORT = 2;

    private PlexServer mServer = null;
    private MediaContainer mContainer = null;
    private boolean mUseScene = false;

    private PlexItemGrid mGrid = null;

    private final SectionFilter mAllFilter = new SectionFilter("All", PlexContainerItem.ALL);

    private ArrayList<SectionFilter> mFilters = new ArrayList<>();
    private ArrayList<SectionSort> mSorts = new ArrayList<>();
    private SectionFilter mCurrentFilter = null;
    private SectionSort mCurrentSort = null;


    private UILifecycleManager mLifeCycleMgr = new UILifecycleManager();
    private CardSelectionHandler mSelectionHandler;
    private ThemeHandler mThemeHandler;

    private TextView mFilterText;
    private TextView mSortText;

    @Override
    public WatchedStatusHandler.UpdateStatusList getItemsToCheck() {

        CardObject card = mSelectionHandler.getSelection();
        WatchedStatusHandler.UpdateStatusList list = null;
        if (card != null) {

            list = new WatchedStatusHandler.UpdateStatusList();
            list.add(card.getUpdateStatus());
        }
        return list;
    }

    @Override
    public void updatedItemsCallback(WatchedStatusHandler.UpdateStatusList items) {

        if (items != null && !items.isEmpty()) {

            for (WatchedStatusHandler.UpdateStatus update : items)
                mGrid.updateItem(update);
        }
    }

    public static class SectionSort extends SectionFilter implements Parcelable {

        public final PlexItemGrid.ItemSort id;
        public boolean isAscending = true;

        public SectionSort(String name, PlexItemGrid.ItemSort key) {

            super(name, name);
            this.id = key;
        }

        public SectionSort(Parcel in) {

            super(in);
            id = PlexItemGrid.ItemSort.valueOf(in.readString());
            isAscending = in.readInt() == 1;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {

            super.writeToParcel(dest, flags);
            dest.writeString(id.name());
            dest.writeInt(isAscending ? 1 : 0);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<SectionSort> CREATOR = new Creator<SectionSort>() {
            @Override
            public SectionSort createFromParcel(Parcel in) {
                return new SectionSort(in);
            }

            @Override
            public SectionSort[] newArray(int size) {
                return new SectionSort[size];
            }
        };
    }

    public static class SectionFilter implements Parcelable {

        public String name;
        public final String key;

        public SectionFilter(String name, String key) {
            this.name = name;
            this.key = key;
        }

        public SectionFilter(Parcel in) {
            name = in.readString();
            key = in.readString();
        }

        public static final Creator<SectionFilter> CREATOR = new Creator<SectionFilter>() {
            @Override
            public SectionFilter createFromParcel(Parcel in) {
                return new SectionFilter(in);
            }

            @Override
            public SectionFilter[] newArray(int size) {
                return new SectionFilter[size];
            }
        };

        @Override
        public String toString() { return name; }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(name);
            dest.writeString(key);
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        if (null == savedInstanceState)
            prepareEntranceTransition();

        Activity act = getActivity();
        mSorts.add(new SectionSort(act.getString(R.string.sort_DateAdded), PlexItemGrid.ItemSort.DateAdded));
        mSorts.add(new SectionSort(act.getString(R.string.sort_Duration), PlexItemGrid.ItemSort.Duration));
        mSorts.add(new SectionSort(act.getString(R.string.sort_LastViewed), PlexItemGrid.ItemSort.LastViewed));
        mSorts.add(new SectionSort(act.getString(R.string.sort_Rating), PlexItemGrid.ItemSort.Rating));
        mSorts.add(new SectionSort(act.getString(R.string.sort_ReleaseDate), PlexItemGrid.ItemSort.ReleaseDate));
        mSorts.add(new SectionSort(act.getString(R.string.sort_Title), PlexItemGrid.ItemSort.Title));
        mCurrentSort = mSorts.get(mSorts.size()-1);


        mServer = PlexServerManager.getInstance(act.getApplicationContext()).getSelectedServer();
        mUseScene = act.getIntent().getBooleanExtra(ContainerActivity.USE_SCENE, false);
        if (!mUseScene)
            mLifeCycleMgr.put(WatchedStatusHandler.key, new WatchedStatusHandler(mServer, this));

        mThemeHandler = new ThemeHandler(act, mUseScene, !mUseScene);
        mLifeCycleMgr.put(ThemeHandler.key, mThemeHandler);
        mSelectionHandler = new CardSelectionHandler(this, this, mServer);
        mLifeCycleMgr.put(CardSelectionHandler.key, mSelectionHandler);
        String background = act.getIntent().getStringExtra(ContainerActivity.BACKGROUND);
        if (!TextUtils.isEmpty(background))
            mSelectionHandler.updateBackground(background, true);
        VerticalGridPresenter gridPresenter = new VerticalGridPresenter();
        String colCount = mUseScene ? act.getString(R.string.gridview_scene_columns)
                                    : act.getString(R.string.gridview_poster_columns);

        setTitle(null);
        gridPresenter.setNumberOfColumns(Integer.valueOf(colCount));
        setGridPresenter(gridPresenter);
        String key = act.getIntent().getStringExtra(ContainerActivity.KEY);
        new GetContainerTask().execute(key);
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

    private void updateAdapter(MediaContainer container) {

        if (mServer == null) {

            setAdapter(null);
            return;
        }

        List<ContainerActivity.QuickJumpRow> quickjumpList = new ArrayList<>();
        mGrid = mUseScene ? PlexItemGrid.getWatchedStateGrid(mServer, mSelectionHandler)
                          : PlexItemGrid.getGrid(mServer, mSelectionHandler);
        mLifeCycleMgr.put("containerGrid", mGrid);
        if (container != null) {

            Video currVid = null;
            Iterator<Video> itVideos = null;
            if (container.getVideos() != null && !container.getVideos().isEmpty()) {

                itVideos = container.getVideos().iterator();
                currVid = itVideos.next();
            }

            Directory currDir = null;
            Iterator<Directory> itDirs = null;
            if (container.getDirectories() != null && !container.getDirectories().isEmpty()) {

                itDirs = container.getDirectories().iterator();
                currDir = itDirs.next();
            }

            ContainerActivity.QuickJumpRow lastQuickRow = null;
            int index = 0;
            while (currDir != null || currVid != null) {

                boolean useDir = false;
                PlexLibraryItem item;
                if (currDir != null) {

                    if (currVid != null) {
                        if (currDir.getUpdatedAt() > currVid.getTimeAdded())
                            useDir = true;
                    } else
                        useDir = true;
                }

                if (useDir) {

                    item = PlexContainerItem.getItem(currDir);
                    currDir = itDirs.hasNext() ? itDirs.next() : null;
                }
                else {

                    item = PlexVideoItem.getItem(currVid);
                    if (item instanceof Episode)
                        ((Episode) item).setSeasonNum(mContainer.getParentIndex());
                    currVid = itVideos.hasNext() ? itVideos.next() : null;
                }

                if (item != null) {

                    String titleLetter = item.getSortTitle().trim().substring(0,1);
                    if (!Character.isAlphabetic(titleLetter.charAt(0)))
                        titleLetter = ContainerActivity.QuickJumpRow.NUM_OR_SYMBOL;
                    else
                        titleLetter = titleLetter.toUpperCase();
                    if (lastQuickRow == null || !lastQuickRow.letter.equals(titleLetter)) {

                        lastQuickRow = new ContainerActivity.QuickJumpRow(titleLetter, index);
                        quickjumpList.add(lastQuickRow);
                    }
                    mGrid.addItem(getActivity(), item, mUseScene);
                    ++index;
                }
            }
        }

        if (!mUseScene)
            ((ContainerActivity) getActivity()).setQuickJumpList(quickjumpList);
        setAdapter(mGrid.getAdapter());
        setSelectedPosition(0);
        startEntranceTransition();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Intent intent = new Intent(getActivity(), PlaybackActivity.class);
        String key = getActivity().getIntent().getStringExtra(ContainerActivity.KEY);
        intent.putExtra(PlaybackActivity.KEY, key);
        if (mCurrentFilter != null)
            intent.putExtra(PlaybackActivity.FILTER, mCurrentFilter.key);
        startActivity(intent);

      return true;
    }

    public void hubButtonClicked() {

        Intent intent = new Intent(getActivity(), SectionHubActivity.class);
        intent.putExtra(SectionHubActivity.TITLE, mContainer.getLibrarySectionTitle());
        intent.putExtra(SectionHubActivity.SECTIONID, mContainer.getLibrarySectionID());
        startActivity(intent);
    }

    public void filterButtonClicked() {

        Intent intent = new Intent(getActivity(), FilterChoiceActivity.class);
        intent.putParcelableArrayListExtra(FilterChoiceActivity.FILTERS, mFilters);
        intent.putExtra(FilterChoiceActivity.CURRENT_INDEX, mFilters.indexOf(mCurrentFilter));
        intent.putExtra(FilterChoiceActivity.TITLE, mContainer.getLibrarySectionTitle());
        startActivityForResult(intent, RESULT_FILTER);
    }

    public void sortButtonClicked() {

        Intent intent = new Intent(getActivity(), FilterChoiceActivity.class);
        intent.putParcelableArrayListExtra(FilterChoiceActivity.FILTERS, mSorts);
        intent.putExtra(FilterChoiceActivity.CURRENT_INDEX, mSorts.indexOf(mCurrentSort));
        intent.putExtra(FilterChoiceActivity.TITLE, mContainer.getLibrarySectionTitle());
        intent.putExtra(FilterChoiceActivity.SORT, true);
        startActivityForResult(intent, RESULT_SORT);
    }

    @Override
    public void onStart() {

        super.onStart();
        TitleView tv = (TitleView) getActivity().findViewById(android.support.v17.leanback.R.id.browse_title_group);
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.lb_container_header, tv, false);
        tv.addView(view);

        Button hubBtn = (Button) view.findViewById(R.id.hubBtn);
        hubBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hubButtonClicked();
            }
        });
        Button filterBtn = (Button) view.findViewById(R.id.filterBtn);
        filterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filterButtonClicked();
            }
        });
        mFilterText = (TextView) view.findViewById(R.id.filterText);
        Button sortBtn = (Button) view.findViewById(R.id.sortBtn);
        sortBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sortButtonClicked();
            }
        });
        mSortText = (TextView) view.findViewById(R.id.sortText);
        mSortText.setText(mCurrentSort.name);

        if (mUseScene) {

            hubBtn.setVisibility(View.GONE);
            sortBtn.setVisibility(View.GONE);
            filterBtn.setVisibility(View.GONE);
            mSortText.setVisibility(View.GONE);
            mFilterText.setVisibility(View.GONE);
        }
    }

    @Override
    public Bundle getPlaySelectionBundle(boolean cardWasScene) {
        return mThemeHandler.getPlaySelectionBundle(null);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode != FilterChoiceActivity.CANCEL) {
            switch (requestCode) {

                case RESULT_FILTER:
                    mCurrentFilter = mFilters.get(resultCode);
                    mFilterText.setText(mCurrentFilter.name);
                    new LoadSectionFilterTask().execute(mContainer.getLibrarySectionID(), mCurrentFilter.key);
                    break;
                case RESULT_SORT:
                    boolean isAscending = mCurrentSort.isAscending;
                    PlexItemGrid.ItemSort lastSort = mCurrentSort.id;
                    mCurrentSort = mSorts.get(resultCode);
                    ((ContainerActivity)getActivity()).setQuickListVisible(mCurrentSort.id == PlexItemGrid.ItemSort.Title);
                    if (lastSort == mCurrentSort.id) {

                        isAscending = !isAscending;
                        mCurrentSort.isAscending = isAscending;
                    }

                    mSortText.setText(mCurrentSort.name);
                    mGrid.sort(getActivity(), mCurrentSort.id, isAscending);
                    break;
                default:
                    break;
            }
        }
    }

    private class LoadSectionFilterTask extends AsyncTask<String, Void, MediaContainer> {

        @Override
        protected MediaContainer doInBackground(String... params) {

            if (params != null)
                return mServer.getSectionFilter(params[0], params[1]);
            return null;
        }

        @Override
        protected void onPostExecute(MediaContainer container) {

            mFilterText.setText(mCurrentFilter.name);
            updateAdapter(container);
        }
    }

    private class GetContainerTask extends AsyncTask<String, Void, MediaContainer> {

        @Override
        protected MediaContainer doInBackground(String... params) {
            return mServer.getVideoMetadata(params[0]);
        }

        @Override
        protected void onPostExecute(MediaContainer container) {


            TextView text = (TextView) getActivity().findViewById(android.support.v17.leanback.R.id.title_text);
            text.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
            if (mUseScene) {
                setTitle(String.format("%s %s %s",
                                container.getTitle1(),
                                getString(R.string.mid_dot),
                                container.getTitle2()));
            }
            else
                setTitle(container.getTitle1());
            mCurrentFilter = null;
            mFilters.clear();
            mContainer = container;
            if (container.getDirectories() != null && !container.getDirectories().isEmpty()) {

                for(Directory dir : container.getDirectories()) {

                    if (dir.getSecondary() > 0 || (dir.getPrompt() != null && !dir.getPrompt().isEmpty()))
                        continue;

                    SectionFilter filter = new SectionFilter(dir.getTitle(), dir.getKey());
                    if (filter.key.equals(PlexContainerItem.ALL)) {
                        filter.name = filter.name.replace(container.getTitle1(), "").trim();
                        mCurrentFilter = filter;
                    }
                    mFilters.add(filter);
                }

                if (mCurrentFilter == null)
                    mCurrentFilter = mAllFilter;
                new LoadSectionFilterTask().execute(container.getLibrarySectionID(), mCurrentFilter.key);
            }
            else {

                String art = container.getArt();
                if (art != null && !art.isEmpty())
                    mSelectionHandler.updateBackground(mServer.makeServerURL(art), true);
                updateAdapter(container);
            }

            mThemeHandler.startTheme(mServer.getThemeURL(mContainer));
        }
    }
}

