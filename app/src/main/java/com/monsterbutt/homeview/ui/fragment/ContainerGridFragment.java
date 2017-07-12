package com.monsterbutt.homeview.ui.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.VerticalGridFragment;
import android.support.v17.leanback.widget.TitleView;
import android.support.v17.leanback.widget.VerticalGridPresenter;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.monsterbutt.homeview.plex.media.Episode;
import com.monsterbutt.homeview.presenters.CardObject;
import com.monsterbutt.homeview.presenters.PosterCard;
import com.monsterbutt.homeview.ui.PlexItemGrid;
import com.monsterbutt.homeview.ui.UILifecycleManager;
import com.monsterbutt.homeview.ui.activity.SearchActivity;
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

    private PlexServer mServer = null;
    private MediaContainer mContainer = null;
    private boolean mIsEpisodeList = false;

    private PlexItemGrid mGrid = null;

    private final SectionFilter mAllFilter = new SectionFilter("All", PlexContainerItem.ALL);

    private SectionFilterArrayAdapter mFilters;
    private SectionFilterArrayAdapter mSorts;

    private UILifecycleManager mLifeCycleMgr = new UILifecycleManager();
    private CardSelectionHandler mSelectionHandler;
    private ThemeHandler mThemeHandler;
    private String themeKey = "";

    private TextView mFilterText;
    private TextView mSortText;

    private String mPassedSelectedKey;

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

    private static class SectionSort extends SectionFilter implements Parcelable {

        public final PlexItemGrid.ItemSort id;
        boolean isAscending = true;

        SectionSort(String name, PlexItemGrid.ItemSort key) {

            super(name, name);
            this.id = key;
        }

        SectionSort(Parcel in) {

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

    static class SectionFilter implements Parcelable {

        public String name;
        public final String key;

        SectionFilter(String name, String key) {
            this.name = name;
            this.key = key;
        }

        SectionFilter(Parcel in) {
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
        List<SectionFilter> list = new ArrayList<>();
        list.add(new SectionSort(act.getString(R.string.sort_DateAdded), PlexItemGrid.ItemSort.DateAdded));
        list.add(new SectionSort(act.getString(R.string.sort_Duration), PlexItemGrid.ItemSort.Duration));
        list.add(new SectionSort(act.getString(R.string.sort_LastViewed), PlexItemGrid.ItemSort.LastViewed));
        list.add(new SectionSort(act.getString(R.string.sort_Rating), PlexItemGrid.ItemSort.Rating));
        list.add(new SectionSort(act.getString(R.string.sort_ReleaseDate), PlexItemGrid.ItemSort.ReleaseDate));
        list.add(new SectionSort(act.getString(R.string.sort_Title), PlexItemGrid.ItemSort.Title));
        mSorts = new SectionFilterArrayAdapter(act, list, list.get(list.size()-1));

        mServer = PlexServerManager.getInstance(act.getApplicationContext(), act).getSelectedServer();
        mIsEpisodeList = act.getIntent().getBooleanExtra(ContainerActivity.EPISODEIST, false);
        if (mIsEpisodeList)
            mLifeCycleMgr.put(WatchedStatusHandler.key, new WatchedStatusHandler(mServer, this));

        mThemeHandler = new ThemeHandler(act, mServer, mIsEpisodeList, !mIsEpisodeList);
        mLifeCycleMgr.put(ThemeHandler.key, mThemeHandler);
        mSelectionHandler = new CardSelectionHandler(this, this, mServer, act.getIntent().getStringExtra(ContainerActivity.BACKGROUND));
        mLifeCycleMgr.put(CardSelectionHandler.key, mSelectionHandler);
        mPassedSelectedKey = act.getIntent().getStringExtra(ContainerActivity.SELECTED);
        VerticalGridPresenter gridPresenter = new VerticalGridPresenter();
        String colCount = mIsEpisodeList ? act.getString(R.string.gridview_scene_columns) : act.getString(R.string.gridview_poster_columns);

        setTitle(null);
        gridPresenter.setNumberOfColumns(Integer.valueOf(colCount));
        setGridPresenter(gridPresenter);
        String key = act.getIntent().getStringExtra(ContainerActivity.KEY);
        new GetContainerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, key);
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

    private List<ContainerActivity.QuickJumpRow> updateAdapter(MediaContainer container) {

        if (mServer == null) {

            mGrid = null;
            return null;
        }

        List<ContainerActivity.QuickJumpRow> quickjumpList = new ArrayList<>();
        mGrid = PlexItemGrid.getWatchedStateGrid(mServer, mSelectionHandler);
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
                    mGrid.addItem(getActivity(), item);
                    ++index;
                }
            }
        }
        return quickjumpList;
    }

    public void hubButtonClicked() {

        Intent intent = new Intent(getActivity(), SectionHubActivity.class);
        intent.putExtra(SectionHubActivity.TITLE, mContainer.getLibrarySectionTitle());
        intent.putExtra(SectionHubActivity.SECTIONID, mContainer.getLibrarySectionID());
        startActivity(intent);
    }

    public void filterButtonClicked() {

        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setIcon(R.drawable.launcher)
                .setTitle(R.string.filter_title)
                .setAdapter(mFilters, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        SectionFilter selected = mFilters.selected(which);
                        if (selected.key.startsWith("/")) {

                            dialog.dismiss();
                            Intent intent = new Intent(getActivity(), ContainerActivity.class);
                            intent.putExtra(ContainerActivity.KEY, selected.key);
                            if (mSelectionHandler.getBackgroundURL() != null)
                                intent.putExtra(ContainerActivity.BACKGROUND, mSelectionHandler.getBackgroundURL());
                            startActivity(intent);
                        }
                        else{
                            mFilterText.setText(selected.name);
                            new LoadSectionFilterTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mContainer.getLibrarySectionID(), selected.key);
                            dialog.dismiss();
                        }
                    }
                })
                .create()
                .show();
    }

    public void sortButtonClicked() {

        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setIcon(R.drawable.launcher)
                .setTitle(R.string.sort_title)
                .setAdapter(mSorts, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        SectionSort oldSelected = (SectionSort) mSorts.selected();
                        boolean isAscending = oldSelected.isAscending;
                        PlexItemGrid.ItemSort lastSort = oldSelected.id;
                        SectionSort selected = (SectionSort) mSorts.selected(which);
                        ((ContainerActivity)getActivity()).setQuickListVisible(selected.id == PlexItemGrid.ItemSort.Title);
                        if (lastSort == selected.id) {

                            isAscending = !isAscending;
                            selected.isAscending = isAscending;
                        }

                        mSortText.setText(selected.name);
                        mGrid.sort(getActivity(), selected.id, isAscending);
                        dialog.dismiss();
                    }
                })
                .create()
                .show();
    }

    @Override
    public void onStart() {

        super.onStart();
        TitleView tv = (TitleView) getActivity().findViewById(android.support.v17.leanback.R.id.browse_title_group);
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.lb_container_header, tv, false);
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
        mSortText.setText(mSorts.selected().name);

        if (mIsEpisodeList) {

            hubBtn.setVisibility(View.GONE);
            sortBtn.setVisibility(View.GONE);
            filterBtn.setVisibility(View.GONE);
            mSortText.setVisibility(View.GONE);
            mFilterText.setVisibility(View.GONE);
        }
    }

    @Override
    public Bundle getPlaySelectionBundle(boolean cardWasScene) {
        return mThemeHandler.getPlaySelectionBundle(null, themeKey);
    }

    @Override
    public void onCardSelected(CardObject card) {
        if (card instanceof PosterCard)
            ((ContainerActivity) getActivity()).setCurrentItem(((PosterCard) card).getItem());
    }

    private class LoadSectionFilterTask extends AsyncTask<String, Void, List<ContainerActivity.QuickJumpRow>> {

        @Override
        protected List<ContainerActivity.QuickJumpRow> doInBackground(String... params) {

            if (params == null || params.length == 0 || params[0] == null)
                return null;
            return updateAdapter(mServer.getSectionFilter(params[0], params[1]));
        }

        @Override
        protected void onPostExecute(List<ContainerActivity.QuickJumpRow> quickjumpList) {

            if (mFilters.selected() != null)
               mFilterText.setText(mFilters.selected().name);
            if (!mIsEpisodeList && getActivity() != null)
                ((ContainerActivity) getActivity()).setQuickJumpList(quickjumpList);
            setAdapter(mGrid != null ? mGrid.getAdapter() : null);
            setSelectedPosition(0);
            startEntranceTransition();
        }
    }

    private class GetContainerTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            if (params == null || params.length == 0 || params[0] == null)
                return null;

            mContainer = mServer.getVideoMetadata(params[0], false);
            if (mContainer.getDirectories() != null && !mContainer.getDirectories().isEmpty()) {

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
                mFilters = new SectionFilterArrayAdapter(getActivity(), filters, current);
            }
            else
                updateAdapter(mContainer);
            return null;
        }

        @Override
        protected void onPostExecute(Void param) {

            if (mContainer == null)
                return;

            TextView text = (TextView) getActivity().findViewById(android.support.v17.leanback.R.id.title_text);
            text.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
            if (mIsEpisodeList) {
                setTitle(String.format("%s %s %s",
                        mContainer.getTitle1(),
                                getString(R.string.mid_dot),
                        mContainer.getTitle2()));
            }
            else
                setTitle(mContainer.getTitle1());

            if (mContainer.getDirectories() != null && !mContainer.getDirectories().isEmpty())
                new LoadSectionFilterTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mContainer.getLibrarySectionID(), mFilters.selected().key);
            else {

                String art = mContainer.getArt();
                if (art != null && !art.isEmpty())
                    mSelectionHandler.updateBackground(art, true);
                setAdapter(mGrid != null ? mGrid.getAdapter() : null);
                setSelectedPosition(mGrid.getIndexForKey(mPassedSelectedKey));
                mPassedSelectedKey = "";
                startEntranceTransition();
            }

            themeKey = mServer.getThemeURL(mContainer);
            mThemeHandler.startTheme(themeKey);
        }
    }

    private static class SectionFilterArrayAdapter extends ArrayAdapter<SectionFilter> {

        private final List<SectionFilter> values;
        private final Context context;
        private SectionFilter selected;

        SectionFilterArrayAdapter(Context context, List<SectionFilter> values, SectionFilter selected) {
            super(context, R.layout.lb_aboutitem, values);
            this.context = context;
            this.values = values;
            this.selected = selected;
        }

        public SectionFilter selected() { return selected; }

        public SectionFilter selected(int selected) {
            this.selected = values.get(selected);
            return selected();
        }

        @NonNull
        @Override
        public View getView(int position, View row, @NonNull ViewGroup parent) {

            View rowView = row;
            if (rowView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                rowView = inflater.inflate(R.layout.lb_filterchoice, parent, false);
            }
            final SectionFilter item = values.get(position);
            ImageView image = (ImageView) rowView.findViewById(R.id.directionImage);
            image.setVisibility(item instanceof SectionSort && !((SectionSort)item).isAscending ? View.VISIBLE: View.INVISIBLE);
            CheckBox name = (CheckBox) rowView.findViewById(R.id.name);
            name.setText(item.name);
            name.setChecked(item == selected);
            return rowView;
        }
    }
}

