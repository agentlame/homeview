package com.monsterbutt.homeview.ui.fragment;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.view.View;
import android.widget.TextView;

import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.PlexServerManager;
import com.monsterbutt.homeview.presenters.CustomListRowPresenter;
import com.monsterbutt.homeview.ui.HubInfo;
import com.monsterbutt.homeview.ui.PlexItemRow;
import com.monsterbutt.homeview.ui.RowData;
import com.monsterbutt.homeview.ui.UILifecycleManager;
import com.monsterbutt.homeview.ui.activity.SectionHubActivity;
import com.monsterbutt.homeview.ui.handler.CardSelectionHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import us.nineworlds.plex.rest.model.impl.Hub;
import us.nineworlds.plex.rest.model.impl.MediaContainer;


public class SectionHubFragment extends BrowseFragment implements PlexItemRow.RefreshAllCallback, CustomListRowPresenter.Callback {

    private PlexServer mServer;

    private CardSelectionHandler mSelectionHandler;
    private UILifecycleManager mLifeCycleMgr = new UILifecycleManager();

    private Map<String, RowData> mRows = new HashMap<>();
    private ArrayObjectAdapter mRowsAdapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

        Activity activity = getActivity();
        mServer = PlexServerManager.getInstance(activity.getApplicationContext(), activity).getSelectedServer();
        mSelectionHandler = new CardSelectionHandler(this, mServer);

        TextView text = (TextView) getActivity().findViewById(android.support.v17.leanback.R.id.title_text);
        text.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
        setTitle(activity.getIntent().getStringExtra(SectionHubActivity.TITLE));

        setHeadersTransitionOnBackEnabled(false);
        mRowsAdapter = new ArrayObjectAdapter(new CustomListRowPresenter(this));
        setAdapter(mRowsAdapter);
    }

    @Override
    public void onResume() {

        super.onResume();
        mLifeCycleMgr.resumed();
        new LoadMetadataTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getActivity().getIntent().getStringExtra(SectionHubActivity.SECTIONID));
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

                List<Hub> toRemove = new ArrayList<>();
                for (Hub hub : item.getHubs()) {
                    if (0 == hub.getSize())
                        toRemove.add(hub);
                }
                for(Hub hub : toRemove)
                    item.getHubs().remove(hub);
                if (item.getHubs().isEmpty())
                    return;
                HubInfo.handleHubRows(getActivity(), mServer, HubInfo.getHubs(item.getHubs()),
                                        null, mRows, mRowsAdapter, mLifeCycleMgr,
                                        SectionHubFragment.this, mSelectionHandler);
                for (Hub hub : item.getHubs()) {
                    RowData row = mRows.get(hub.getHubIdentifier());
                    if (row != null)
                        ((PlexItemRow) row.data).update(hub.getDirectories(), hub.getVideos());
                }
            }
        }
    }
}
