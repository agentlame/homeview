package com.monsterbutt.homeview.ui.fragment;

import android.Manifest;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.CursorObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.SpeechRecognitionCallback;
import android.text.TextUtils;
import android.util.Log;

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
        implements android.support.v17.leanback.app.SearchFragment.SearchResultProvider,
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "SearchFragment";

    //private static final boolean FINISH_ON_RECOGNIZER_CANCELED = true;
    private static final int REQUEST_SPEECH = 0x00000010;

    private final Handler mHandler = new Handler();
    private ArrayObjectAdapter mRowsAdapter;
    private String mQuery;
    private CursorObjectAdapter mVideoCursorAdapter;

    private int mSearchLoaderId = 1;

    private UILifecycleManager mLifeCycleMgr = new UILifecycleManager();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PlexServer server = PlexServerManager.getInstance(getActivity().getApplicationContext()).getSelectedServer();
        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        mVideoCursorAdapter.setMapper(new VideoCursorMapper());

        CardSelectionHandler selectionHandler = new CardSelectionHandler(this, server);
        mLifeCycleMgr.put(CardSelectionHandler.key, selectionHandler);

        mVideoCursorAdapter = new CursorObjectAdapter(new CardPresenter(server, selectionHandler));
        setSearchResultProvider(this);

        if (!hasPermission(Manifest.permission.RECORD_AUDIO)) {
            // SpeechRecognitionCallback is not required and if not provided recognition will be
            // handled using internal speech recognizer, in which case you must have RECORD_AUDIO
            // permission
            setSpeechRecognitionCallback(new SpeechRecognitionCallback() {
                @Override
                public void recognizeSpeech() {
                    try {
                        startActivityForResult(getRecognizerIntent(), REQUEST_SPEECH);
                    } catch (ActivityNotFoundException e) {
                        Log.e(TAG, "Cannot find activity for speech recognizer", e);
                    }
                }
            });
        }
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_SPEECH:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        setSearchQuery(data, true);
                        break;
                    case Activity.RESULT_CANCELED:
                        // Once recognizer canceled, user expects the current activity to process
                        // the same BACK press as user doesn't know about overlay activity.
                        // However, you may not want this behaviour as it makes harder to
                        // fall back to keyboard input.
                        //if (FINISH_ON_RECOGNIZER_CANCELED) {
                           // if (!hasResults()) {
                           //     getActivity().onBackPressed();
                           // }
                       // }
                        break;
                    // the rest includes various recognizer errors, see {@link RecognizerIntent}
                }
                break;
        }
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

    private boolean hasPermission(final String permission) {
        final Context context = getActivity();
        return PackageManager.PERMISSION_GRANTED == context.getPackageManager().checkPermission(
                permission, context.getPackageName());
    }

    private void loadQuery(String query) {
        if (!TextUtils.isEmpty(query) && !query.equals("nil")) {
            mQuery = query;
            getLoaderManager().initLoader(mSearchLoaderId++, null, this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String query = mQuery;
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
            HeaderItem header = new HeaderItem(getString(R.string.search_results, mQuery));
            ListRow row = new ListRow(header, mVideoCursorAdapter);
            mRowsAdapter.add(row);
        } else {
            // No results were found.
            mRowsAdapter.clear();
            HeaderItem header = new HeaderItem(getString(R.string.no_search_results, mQuery));
            ListRow row = new ListRow(header, new ArrayObjectAdapter());
            mRowsAdapter.add(row);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mVideoCursorAdapter.changeCursor(null);
    }
}