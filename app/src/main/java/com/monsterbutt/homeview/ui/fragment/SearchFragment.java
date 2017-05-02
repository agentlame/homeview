package com.monsterbutt.homeview.ui.fragment;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.CursorObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.text.TextUtils;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.data.VideoContract;
import com.monsterbutt.homeview.model.VideoCursorMapper;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.PlexServerManager;
import com.monsterbutt.homeview.presenters.CardPresenter;
import com.monsterbutt.homeview.ui.UILifecycleManager;
import com.monsterbutt.homeview.ui.handler.CardSelectionHandler;

/*
 * This class demonstrates how to do in-app search
 */
public class SearchFragment extends android.support.v17.leanback.app.SearchFragment
        implements android.support.v17.leanback.app.SearchFragment.SearchResultProvider {

    static private final String QUERY_ARG = "query";
    private static final int SEARCH_DELAY_MS = 1000;

    private final Handler mHandler = new Handler();
    private final ArrayObjectAdapter mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
    private final SearchRunnable mDelayedLoad = new SearchRunnable();
    private final UILifecycleManager mLifeCycleMgr = new UILifecycleManager();

    private CursorObjectAdapter mVideoCursorAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PlexServer server = PlexServerManager.getInstance(getActivity().getApplicationContext(), getActivity()).getSelectedServer();
        CardSelectionHandler selectionHandler = new CardSelectionHandler(this, server);
        mLifeCycleMgr.put(CardSelectionHandler.key, selectionHandler);

        mVideoCursorAdapter = new CursorObjectAdapter(new CardPresenter(server, selectionHandler));
        mVideoCursorAdapter.setMapper(new VideoCursorMapper());
        setSearchResultProvider(this);
    }

    @Override
    public void onStop() {

        super.onStop();
        mLifeCycleMgr.destroyed();
    }

    @Override
    public void onPause() {
        mHandler.removeCallbacksAndMessages(null);
        super.onPause();
        mLifeCycleMgr.paused();
    }

    @Override
    public ObjectAdapter getResultsAdapter() {
        return mRowsAdapter;
    }

    @Override
    public boolean onQueryTextChange(String newQuery) {
        loadQuery(newQuery);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        loadQuery(query);
        return true;
    }

    public boolean hasResults() {
        return mRowsAdapter.size() > 0;
    }

    private void loadQuery(String query) {

        mRowsAdapter.clear();
        mHandler.removeCallbacks(mDelayedLoad);
        if (!TextUtils.isEmpty(query) && !query.equals("nil")) {
            mDelayedLoad.setSearchQuery(query);
            mHandler.postDelayed(mDelayedLoad, SEARCH_DELAY_MS);
        }
    }

    private class SearchLoader implements LoaderManager.LoaderCallbacks<Cursor> {

        String query;
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {

            query = args != null ? args.getString(QUERY_ARG) : "";
            if (query == null || query.isEmpty()) {
                query = "";
                return null;
            }
            return new CursorLoader(
                    getActivity(),
                    VideoContract.VideoEntry.CONTENT_URI,
                    null, // Return all fields.
                    VideoContract.VideoEntry.COLUMN_NAME + " LIKE ? OR " +
                            VideoContract.VideoEntry.COLUMN_DESC + " LIKE ?",
                    new String[]{"%" + query + "%", "%" + query + "%"},
                    null // Default sort order
            );
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            if (cursor != null && cursor.moveToFirst()) {
                mVideoCursorAdapter.changeCursor(cursor);

                mRowsAdapter.clear();
                HeaderItem header = new HeaderItem(getString(R.string.search_results, query));
                ListRow row = new ListRow(header, mVideoCursorAdapter);
                mRowsAdapter.add(row);
            } else {
                // No results were found.
                mRowsAdapter.clear();
                HeaderItem header = new HeaderItem(getString(R.string.no_search_results, query));
                ListRow row = new ListRow(header, new ArrayObjectAdapter());
                mRowsAdapter.add(row);
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            mVideoCursorAdapter.changeCursor(null);
        }
    }

    private class SearchRunnable implements Runnable {

        private volatile String searchQuery;
        private int mSearchLoaderId = 1;

        public void run() {

            Bundle args = new Bundle();
            args.putString(QUERY_ARG, searchQuery);
            getLoaderManager().initLoader(mSearchLoaderId++, args, new SearchLoader());
        }

        public void setSearchQuery(String value) {
            this.searchQuery = value;
        }
    }
}