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

package com.monsterbutt.homeview.ui.fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.monsterbutt.homeview.presenters.SettingCard;
import com.monsterbutt.homeview.presenters.SettingPresenter;
import com.monsterbutt.homeview.services.ThemeService;
import com.monsterbutt.homeview.settings.SettingLaunch;
import com.monsterbutt.homeview.ui.MediaRowCreator;
import com.monsterbutt.homeview.ui.activity.SearchActivity;
import com.monsterbutt.homeview.ui.activity.SettingsActivity;
import com.monsterbutt.homeview.ui.android.ImageCardView;
import com.monsterbutt.homeview.ui.handler.MediaCardBackgroundHandler;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.PlexServerManager;
import com.monsterbutt.homeview.plex.tasks.PlexServerTask;
import com.monsterbutt.homeview.plex.tasks.PlexServerTaskCaller;
import com.monsterbutt.homeview.plex.tasks.ServerLibraryTask;
import com.monsterbutt.homeview.presenters.CardObject;
import com.monsterbutt.homeview.ui.activity.MainActivity;
import com.monsterbutt.homeview.ui.activity.ServerChoiceActivity;

import us.nineworlds.plex.rest.model.impl.MediaContainer;

public class MainFragment extends BrowseFragment implements PlexServerTaskCaller, OnItemViewSelectedListener, OnItemViewClickedListener, MainActivity.OnPlayKeyListener {

    private static final String TAG = "MainFragment";

    private static final String SETTINGS_ROW_KEY = "Settings";

    private static final int SERVER_CHOICE_RESULT = 1;
    private static final int SERVER_CHECK_DELAY = 1000;
    private View mCurrentCardTransitionImage = null;
    private CardObject mCurrentCard = null;
    private boolean  mForcedServerSelectOnce = false;

    private String mBackgroundURL = "";

    private Timer mServerCheckTimer = null;

    protected ArrayObjectAdapter mRowsAdapter;
    protected PlexServerManager mMgr = null;
    protected MediaCardBackgroundHandler mBackgroundHandler;

    Map<String, RowData> mRows = new HashMap<>();

    private class RowData implements Comparable<RowData> {

        public final String id;
        public final ArrayObjectAdapter data;
        public int currentIndex;

        public RowData(String id, int index, ArrayObjectAdapter data) {
            this.id = id;
            currentIndex = index;
            this.data = data;
        }

        @Override
        public int compareTo(@NonNull RowData row) {

            // reverse order
            if (this.currentIndex < row.currentIndex)
                return 1;
            return -1;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);
        setTitle(getString(R.string.app_name)); // Badge, when set, takes precedent

        MainActivity act = (MainActivity) getActivity();
        mMgr = PlexServerManager.getInstance(act.getApplicationContext());

        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);
        setOnSearchClickedListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SearchActivity.class);
                startActivity(intent);
            }
        });

        setOnItemViewClickedListener(this);
        setOnItemViewSelectedListener(this);
        act.setPlayKeyListener(this);

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setAdapter(mRowsAdapter);
    }


    @Override
    public void onDestroy() {

        super.onDestroy();
        if (null != mServerCheckTimer) {
            Log.d(TAG, "onDestroy: " + mServerCheckTimer.toString());
            mServerCheckTimer.cancel();
        }
    }

    @Override
    public void onPause() {

        super.onPause();
        mBackgroundHandler.cancel();
    }

    @Override
    public void onResume() {

        super.onResume();

        ThemeService.stopTheme(getActivity());
        mBackgroundHandler = new MediaCardBackgroundHandler(getActivity());

        if (!TextUtils.isEmpty(mBackgroundURL))
            mBackgroundHandler.updateBackground(mBackgroundURL, false);

        if (mMgr.isDiscoveryRunning()) {

            mServerCheckTimer = new Timer();
            mServerCheckTimer.schedule(new CheckForPlexServerTask(), SERVER_CHECK_DELAY);
        }
        else
            checkServerStatus();
    }

    private void checkServerStatus() {

        PlexServer selected = mMgr.getSelectedServer();
        if (selected != null && selected.isValid())
            refreshPage();
        else if (!mForcedServerSelectOnce) {

            mForcedServerSelectOnce = true;
            startActivityForResult(new Intent(getActivity(), ServerChoiceActivity.class), SERVER_CHOICE_RESULT);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == SERVER_CHOICE_RESULT) {

            PlexServer server = mMgr.getSelectedServer();
            if (server != null && server.isValid())
                refreshPage();
            else
                addSettingsRow(true, 0);
        }
    }

    @Override
    public boolean playKeyPressed() {

        return  mCurrentCard != null && mCurrentCard.onPlayPressed(this, null, mCurrentCardTransitionImage);
    }

    private class CheckForPlexServerTask extends TimerTask {

        @Override
        public void run() {

            if (mMgr.isDiscoveryRunning())
                mServerCheckTimer.schedule(new CheckForPlexServerTask(), SERVER_CHECK_DELAY);
            else {

                mServerCheckTimer.cancel();
                mServerCheckTimer = null;
                checkServerStatus();
            }
        }
    }

    @Override
    public void handlePreTaskUI() {

    }

    @Override
    public void handlePostTaskUI(Boolean result, PlexServerTask task) {

        if (!(task instanceof ServerLibraryTask))
            return;

        Activity act = getActivity();
        if (act == null || act.isFinishing() || act.isDestroyed())
            return;

        ServerLibraryTask libraryTask = (ServerLibraryTask) task;
        MediaContainer library = libraryTask.getLibrary();
        TextView text = (TextView) getActivity().findViewById(android.support.v17.leanback.R.id.title_text);
        text.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
        setTitle(library.getTitle1());

        if (mCurrentCard == null) {

            String artKey = null;
            MediaContainer arts = libraryTask.getRandomArts();
            if (arts != null && arts.getPhotos() != null && !arts.getPhotos().isEmpty()) {

                int photoPos = (int) (1000 * Math.random()) % arts.getPhotos().size();
                artKey = arts.getPhotos().get(photoPos).getKey();
            }
            if (artKey != null && !artKey.isEmpty()) {
                mBackgroundURL = mMgr.getSelectedServer().makeServerURL(artKey);
                mBackgroundHandler.updateBackground(mBackgroundURL, true);
            }
        }
        int rowCount = setRowsFromLibrary(libraryTask.getSections(), libraryTask.getHubs(), getString(R.string.main_landscape_rows), ";");
        addSettingsRow(false, rowCount);
    }

    private void addSettingsRow(boolean serverOnly, int index) {

        RowData oldRow = mRows.get(SETTINGS_ROW_KEY);
        if (oldRow != null && oldRow.data.size() == 2) {

            oldRow.currentIndex = index;
            return;
        }

        Context context = getActivity();
        ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(new SettingPresenter());
        if (oldRow != null)
            gridRowAdapter = oldRow.data;
        else {

            mRows.put(SETTINGS_ROW_KEY, new RowData(SETTINGS_ROW_KEY, index, gridRowAdapter));
            mRowsAdapter.add(new ListRow(SETTINGS_ROW_KEY.hashCode(),
                                            new HeaderItem(SETTINGS_ROW_KEY.hashCode(),
                                                            getString(R.string.settings)),
                                                            gridRowAdapter));
            gridRowAdapter.add(new SettingCard(context,
                    new SettingLaunch(context.getString(R.string.settings_server),
                            mMgr.getSelectedServer().getServerName(),
                            R.drawable.ic_settings_remote_white_48dp,
                            ServerChoiceActivity.class.getName(),
                            SERVER_CHOICE_RESULT)));
        }

        if (!serverOnly) {

            gridRowAdapter.add(new SettingCard(context,
                    new SettingLaunch(context.getString(R.string.settings_settings),
                            "", R.drawable.ic_settings_white_48dp,
                            SettingsActivity.class.getName(),
                            0)));
        }
    }

    private void refreshPage() {

        new ServerLibraryTask(this, mMgr.getSelectedServer()).execute();
    }

    private void updateMainRow(MediaRowCreator.MediaRow row, boolean useLandscape, int index) {

        RowData existing = mRows.get(row.key);
        if (existing == null)
            return;
        existing.currentIndex = index;
        ArrayObjectAdapter gridRowAdapter = MediaRowCreator.fillAdapterForRow(getActivity(),
                mMgr.getSelectedServer(), row, useLandscape);
        for(int currIndex = existing.data.size(); 0 != currIndex--; /**/) {

            CardObject item = (CardObject) existing.data.get(currIndex);
            if (-1 == gridRowAdapter.indexOf(item))
                existing.data.remove(item);
        }

        for (int newIndex = 0; newIndex < gridRowAdapter.size(); ++newIndex) {

            CardObject item = (CardObject) gridRowAdapter.get(newIndex);
            int currIndex = existing.data.indexOf(item);
            if (-1 == currIndex)
                existing.data.add(newIndex, item);
            else
                existing.data.replace(currIndex, item);
        }

    }

    private void addMainRow(MediaRowCreator.MediaRow row, boolean useLandscape, int index) {

        ArrayObjectAdapter gridRowAdapter = MediaRowCreator.fillAdapterForRow(getActivity(),
                                                    mMgr.getSelectedServer(), row, useLandscape);
        String header = row.title;
        if (index != mRows.size()) {
            for(RowData oldRow : mRows.values()) {
                if (oldRow.currentIndex >= index)
                    ++oldRow.currentIndex;
            }
        }
        mRows.put(header, new RowData(row.key, index, gridRowAdapter));
        int hash = header.hashCode();
        for (String sub : getString(R.string.main_rows_header_strip).split(";"))
            header = header.replace(sub, "").trim();

        mRowsAdapter.add(index, new ListRow(hash, new HeaderItem(hash, header), gridRowAdapter));
    }

    private int setRowsFromLibrary(MediaContainer sections, MediaContainer hubs, String rowLandscape, String delim) {

        List<MediaRowCreator.MediaRow> newRows = MediaRowCreator.buildRowList(sections, hubs);
        List<RowData> currentRows = new ArrayList<>();
        currentRows.addAll(mRows.values());
        Collections.sort(currentRows);
        // remove old rows that aren't there anymore
        for (RowData row : currentRows) {
            // we are reversing through the list
            boolean found = false;
            for (MediaRowCreator.MediaRow newRow : newRows) {

                if (newRow.key.equals(row.id)) {

                    found = true;
                    break;
                }
            }

            if (!found && !row.id.equals(SETTINGS_ROW_KEY)) {
                mRows.put(row.id, null);
                mRowsAdapter.removeItems(row.currentIndex, 1);
            }
        }

        HashMap < String, Integer > landscape = makeOrderHashFromStringDelim(rowLandscape, delim);
        int rows = 0;
        for (MediaRowCreator.MediaRow row : newRows) {

            boolean useLandscape = landscape.containsKey(row.key);
            if (mRows.get(row.title) != null)
                updateMainRow(row, useLandscape, rows++);
            else
                addMainRow(row, useLandscape, rows++);
        }
        return rows;
    }

    @Override
    public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                               RowPresenter.ViewHolder rowViewHolder, Row row) {

        if (item instanceof CardObject) {
            mCurrentCard = (CardObject) item;
            mCurrentCardTransitionImage = ((ImageCardView) itemViewHolder.view).getMainImageView();
            mBackgroundURL = mBackgroundHandler.updateBackgroundTimed(mMgr.getSelectedServer(), (CardObject) item);
        }
    }

    @Override
    public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                              RowPresenter.ViewHolder rowViewHolder, Row row) {

        if (item instanceof CardObject)
            ((CardObject) item).onClicked(this, null, mCurrentCardTransitionImage);
    }



    protected HashMap<String, Integer> makeOrderHashFromStringDelim(String str, String delim) {

        String[] tokens = str.split(delim);
        HashMap<String, Integer> map = new HashMap<>(tokens.length);
        Integer index = 0;
        for (String token : tokens)
            map.put(token, index++);
        return map;
    }
}
