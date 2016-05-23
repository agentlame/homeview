package com.monsterbutt.homeview.ui.fragment;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.view.View;
import android.widget.TextView;

import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.PlexServerManager;
import com.monsterbutt.homeview.ui.MediaRowCreator;
import com.monsterbutt.homeview.ui.PlexItemRow;
import com.monsterbutt.homeview.ui.UILifecycleManager;
import com.monsterbutt.homeview.ui.activity.SectionHubActivity;
import com.monsterbutt.homeview.ui.handler.CardSelectionHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import us.nineworlds.plex.rest.model.impl.MediaContainer;


public class SectionHubFragment extends BrowseFragment implements PlexItemRow.RefreshAllCallback {

    private PlexServer mServer;

    private CardSelectionHandler mSelectionHandler;
    private UILifecycleManager mLifeCycleMgr = new UILifecycleManager();

    private Map<String, MediaRowCreator.RowData> mRows = new HashMap<>();
    private ArrayObjectAdapter mRowsAdapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

        Activity activity = getActivity();
        mServer = PlexServerManager.getInstance(activity.getApplicationContext()).getSelectedServer();
        mSelectionHandler = new CardSelectionHandler(this, mServer);

        TextView text = (TextView) getActivity().findViewById(android.support.v17.leanback.R.id.title_text);
        text.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
        setTitle(activity.getIntent().getStringExtra(SectionHubActivity.TITLE));

        setHeadersTransitionOnBackEnabled(false);
        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setAdapter(mRowsAdapter);
    }

    @Override
    public void onResume() {

        super.onResume();
        mLifeCycleMgr.resumed();
        new LoadMetadataTask().execute(getActivity().getIntent().getStringExtra(SectionHubActivity.SECTIONID));
    }

    @Override
    public void onPause() {

        super.onPause();
        mLifeCycleMgr.paused();
    }

    @Override
    public void refresh() {
        mLifeCycleMgr.resumed();
    }

    private class LoadMetadataTask extends AsyncTask<String, Void, MediaContainer>  {

        @Override
        protected MediaContainer doInBackground(String... params) {

            if (params == null || params.length == 0 || params[0] == null)
                return null;
            return  mServer.getHubForSection(params[0]);
        }

        @Override
        protected void onPostExecute(MediaContainer item) {

            if (item != null && item.getHubs() != null) {

                List<MediaRowCreator.MediaRow> newRows = MediaRowCreator.buildRowList(null, item);

                List<MediaRowCreator.RowData> currentRows = new ArrayList<>();
                currentRows.addAll(mRows.values());
                Collections.sort(currentRows);
                // remove old rows that aren't there anymore
                for (MediaRowCreator.RowData row : currentRows) {
                    // we are reversing through the list
                    boolean found = false;
                    for (MediaRowCreator.MediaRow newRow : newRows) {

                        if (newRow.key.equals(row.id)) {

                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        mLifeCycleMgr.remove(row.id);
                        mRows.remove(row.id);
                        mRowsAdapter.removeItems(row.currentIndex, 1);
                    }
                }

                int index = 0;
                for (MediaRowCreator.MediaRow row : newRows) {

                    PlexItemRow rowUpdate = MediaRowCreator.fillAdapterForWatchedRow(getActivity(),
                                                            mServer, row, false, SectionHubFragment.this, mSelectionHandler);
                    if (mRows.containsKey(row.title)) {

                        MediaRowCreator.RowData current = mRows.get(row.title);
                        ((PlexItemRow)current.data).updateRow(rowUpdate);
                        current.currentIndex = index;
                    }
                    else {
                        mLifeCycleMgr.put(row.title, rowUpdate);
                        mRows.put(row.title, new MediaRowCreator.RowData(row.title ,index, rowUpdate));
                        mRowsAdapter.add(index, rowUpdate);
                    }
                    ++index;
                }
            }
        }
    }
}
