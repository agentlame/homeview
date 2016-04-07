package com.monsterbutt.homeview.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v17.leanback.app.VerticalGridFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.VerticalGridPresenter;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.monsterbutt.homeview.plex.media.Episode;
import com.monsterbutt.homeview.services.ThemeService;
import com.monsterbutt.homeview.ui.activity.FilterChoiceActivity;
import com.monsterbutt.homeview.ui.activity.PlaybackActivity;
import com.monsterbutt.homeview.ui.activity.SectionHubActivity;
import com.monsterbutt.homeview.ui.android.ImageCardView;
import com.monsterbutt.homeview.ui.handler.MediaCardBackgroundHandler;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.ui.android.HomeViewActivity;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.PlexServerManager;
import com.monsterbutt.homeview.plex.media.PlexContainerItem;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.presenters.CardPresenter;
import com.monsterbutt.homeview.presenters.CardObject;
import com.monsterbutt.homeview.presenters.PosterCard;
import com.monsterbutt.homeview.presenters.SceneCard;
import com.monsterbutt.homeview.ui.activity.ContainerActivity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import us.nineworlds.plex.rest.model.impl.Directory;
import us.nineworlds.plex.rest.model.impl.MediaContainer;
import us.nineworlds.plex.rest.model.impl.Video;


public class ContainerGridFragment extends VerticalGridFragment implements OnItemViewClickedListener, OnItemViewSelectedListener, HomeViewActivity.OnPlayKeyListener {

    private static final int RESULT_FILTER = 1;

    private MediaCardBackgroundHandler mBackgroundHandler;
    private PlexServer mServer = null;
    private MediaContainer mContainer = null;
    private boolean mUseScene = false;
    private boolean mThemeAlreadyRun = false;
    private boolean mContinueTheme = false;
    private String mBackgroundURL = "";

    private final SectionFilter mAllFilter = new SectionFilter("All", PlexContainerItem.ALL);

    private ArrayList<SectionFilter> mFilters = new ArrayList<>();
    private SectionFilter mCurrentFilter = null;


    private View mCurrentCardTransitionImage = null;
    private CardObject mCurrentCard = null;

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
        mServer = PlexServerManager.getInstance(act.getApplicationContext()).getSelectedServer();
        mUseScene = act.getIntent().getBooleanExtra(ContainerActivity.USE_SCENE, false);
        mThemeAlreadyRun = act.getIntent().getBooleanExtra(ThemeService.THEME_ALREADY_RUN, false);
        mBackgroundHandler = new MediaCardBackgroundHandler(act);

        mBackgroundURL = act.getIntent().getStringExtra(ContainerActivity.BACKGROUND);
        if (!TextUtils.isEmpty(mBackgroundURL)) {
            mBackgroundURL = mServer.makeServerURL(mBackgroundURL);
            mBackgroundHandler.updateBackground(mBackgroundURL, false);
        }
        VerticalGridPresenter gridPresenter = new VerticalGridPresenter();
        String colCount = mUseScene ? act.getString(R.string.gridview_scene_columns)
                                    : act.getString(R.string.gridview_poster_columns);

        setTitle(null);
        gridPresenter.setNumberOfColumns(Integer.valueOf(colCount));
        setGridPresenter(gridPresenter);
        setOnItemViewSelectedListener(this);
        setOnItemViewClickedListener(this);
        ((HomeViewActivity) act).setPlayKeyListener(this);
        String key = act.getIntent().getStringExtra(ContainerActivity.KEY);
        new GetContainerTask().execute(key);
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
                (getActivity() != null && getActivity().isFinishing()
                        && mContainer.getDirectories() == null))
            return;
        ThemeService.stopTheme(getActivity());
    }

    @Override
    public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {

        if (item instanceof PosterCard) {
            Bundle extras = null;
            mContinueTheme = true;
            if (mThemeAlreadyRun) {
                extras = new Bundle();
                extras.putBoolean(ThemeService.THEME_ALREADY_RUN, true);
            }
            ((PosterCard) item).onClicked(this, extras, ((ImageCardView) itemViewHolder.view).getMainImageView());
        }
    }

    @Override
    public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {

        if (item instanceof CardObject) {

            mCurrentCard = (CardObject) item;
            mCurrentCardTransitionImage = ((ImageCardView) itemViewHolder.view).getMainImageView();
            if (!mUseScene)
                mBackgroundURL = mBackgroundHandler.updateBackgroundTimed(mServer, mCurrentCard);
        }
    }

    private void updateAdapter(MediaContainer container) {

        if (mServer == null) {

            setAdapter(null);
            return;
        }

        List<ContainerActivity.QuickJumpRow> quickjumpList = new ArrayList<>();
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(new CardPresenter(mServer));
        if (container != null) {

          //  setTitle(container.getTitle1());

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
                    adapter.add(mUseScene ? new SceneCard(getActivity(), item) : new PosterCard(getActivity(), item));
                    ++index;
                }
            }
        }

        if (!mUseScene)
            ((ContainerActivity) getActivity()).setQuickJumpList(quickjumpList);
        setAdapter(adapter);
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

    @Override
    public boolean playKeyPressed() {

        return mCurrentCard != null && mCurrentCard.onPlayPressed(this, null, mCurrentCardTransitionImage);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == RESULT_FILTER && resultCode != FilterChoiceActivity.CANCEL) {

            mCurrentFilter = mFilters.get(resultCode);
            ((ContainerActivity) getActivity()).setFilterText(mCurrentFilter.name);
            new LoadSectionFilterTask().execute(mContainer.getLibrarySectionID(), mCurrentFilter.key);
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

            ((ContainerActivity) getActivity()).setFilterText(mCurrentFilter.name);
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
                    mBackgroundHandler.updateBackground(mServer.makeServerURL(art), true);
                updateAdapter(container);
            }

            if (!mThemeAlreadyRun) {
                String theme = mContainer.getThemeKey();
                if (TextUtils.isEmpty(theme))
                    theme = mContainer.getGrandparentTheme();
                if (!TextUtils.isEmpty(theme))
                    theme = mServer.makeServerURL(theme);
                mThemeAlreadyRun = ThemeService.startTheme(getActivity(), theme);
            }
        }
    }
}
