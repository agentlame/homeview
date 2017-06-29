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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.TitleView;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import com.monsterbutt.homeview.plex.PlexServerManager;
import com.monsterbutt.homeview.presenters.CustomListRowPresenter;
import com.monsterbutt.homeview.presenters.SettingCard;
import com.monsterbutt.homeview.presenters.SettingPresenter;
import com.monsterbutt.homeview.settings.SettingLaunch;
import com.monsterbutt.homeview.ui.HubInfo;
import com.monsterbutt.homeview.ui.PlexItemRow;
import com.monsterbutt.homeview.ui.RowData;
import com.monsterbutt.homeview.ui.UILifecycleManager;
import com.monsterbutt.homeview.ui.activity.OnBoardingActivity;
import com.monsterbutt.homeview.ui.activity.SearchActivity;
import com.monsterbutt.homeview.ui.activity.SettingsActivity;
import com.monsterbutt.homeview.ui.handler.CardSelectionHandler;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.tasks.PlexServerTask;
import com.monsterbutt.homeview.plex.tasks.PlexServerTaskCaller;
import com.monsterbutt.homeview.plex.tasks.ServerLibraryTask;
import com.monsterbutt.homeview.ui.activity.ServerChoiceActivity;
import com.monsterbutt.homeview.ui.handler.ServerStatusHandler;
import com.monsterbutt.homeview.ui.handler.ThemeHandler;

import us.nineworlds.plex.rest.model.impl.MediaContainer;

public class MainFragment extends BrowseFragment implements PlexServerTaskCaller,
                                                            ServerStatusHandler.ServerStatusListener,
                                                            PlexItemRow.RefreshAllCallback, CustomListRowPresenter.Callback {

    private boolean mFirstResume = true;

    private ArrayObjectAdapter mRowsAdapter;
    private CardSelectionHandler mSelectionHandler;
    private UILifecycleManager mLifeCycleMgr = new UILifecycleManager();
    private PlexServer mServer = null;

    private Map<String, RowData> mRows = new HashMap<>();

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);
        setTitle(getString(R.string.app_name)); // Badge, when set, takes precedent

        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(false);
        setOnSearchClickedListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SearchActivity.class);
                startActivity(intent);
            }
        });

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getContext().getTheme();
        theme.resolveAttribute(R.attr.brand_accent, typedValue, true);
        setSearchAffordanceColor(typedValue.data);

        TitleView tv = (TitleView) getActivity().findViewById(android.support.v17.leanback.R.id.browse_title_group);
        TextView text = (TextView) tv.findViewById(android.support.v17.leanback.R.id.title_text);
        text.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);

        mServer = PlexServerManager.getInstance(getActivity(), getActivity()).getSelectedServer();
        mSelectionHandler = new CardSelectionHandler(this);
        if (mServer != null)
            mSelectionHandler.setServer(mServer);
        mRowsAdapter = new ArrayObjectAdapter(new CustomListRowPresenter(this));
        setAdapter(mRowsAdapter);
        mLifeCycleMgr.put(CardSelectionHandler.key, mSelectionHandler);
        mLifeCycleMgr.put(ServerStatusHandler.key, new ServerStatusHandler(this, this, this, true));
        mLifeCycleMgr.put(ThemeHandler.key, new ThemeHandler(getActivity(), mServer, false, true));
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
        mLifeCycleMgr.destroyed();
    }

    @Override
    public void onPause() {

        super.onPause();
        mLifeCycleMgr.paused();
    }

    @Override
    public void onResume() {

        super.onResume();

        mLifeCycleMgr.resumed();
        if (mFirstResume) {

            Intent intent = getActivity().getIntent();
            Bundle data = intent != null ? intent.getExtras() : null;
            String title = "";
            List<HubInfo> hubs = null;
            MediaContainer sections = null;
            boolean noServer = true;
            if (data != null) {
                title = data.getString(OnBoardingActivity.TitleExtra);
                hubs = data.getParcelableArrayList(OnBoardingActivity.HubsExtra);
                sections = data.getParcelable(OnBoardingActivity.SectionsExtra);
                noServer = !((title != null && !title.isEmpty()) || (hubs != null) || (sections != null));
            }

            mFirstResume = false;
            if (noServer)
                addSettingsRow(true);
            else
                fillData(title, sections, hubs);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == ServerStatusHandler.SERVER_CHOICE_RESULT) {

            PlexServer server = PlexServerManager.getInstance(getActivity().getApplicationContext(), getActivity()).getSelectedServer();
            if (server != null && server.isValid())
                setSelectedServer(server);
            else
                addSettingsRow(true);
        }
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
        String title = "";
        if (library != null)
            title = library.getTitle1();

        if ((title != null && !title.isEmpty()) || (libraryTask.getSections() != null) || (libraryTask.getHubs()!= null))
            fillData(title, libraryTask.getSections(), libraryTask.getHubs());
        else
            addSettingsRow(true);
    }

    private void fillData(String title, MediaContainer sections, List<HubInfo> hubs) {

        if (title != null && !title.isEmpty())
            setTitle(title);
        if (null == mSelectionHandler.getSelection())
            new GetRandomArtWorkTask(mServer).execute(sections);
        handleSectionsRow(sections);
        addSettingsRow(false);
        HubInfo.handleHubRows(getActivity(), mServer, hubs,
                makeOrderHashFromStringDelim(getString(R.string.main_landscape_rows), ";"), mRows, mRowsAdapter,
                mLifeCycleMgr, this, mSelectionHandler);
    }

    private void addSettingsRow(boolean serverOnly) {

        RowData oldRow = mRows.get(PlexItemRow.SETTINGS_ROW_KEY);
        if (oldRow != null && oldRow.data.getAdapter().size() == 2) {

            oldRow.currentIndex = mRowsAdapter.size() - 1;
            return;
        }

        Context context = getActivity();
        ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(new SettingPresenter());
        if (oldRow != null)
            gridRowAdapter = (ArrayObjectAdapter) oldRow.data.getAdapter();
        else {

            ListRow row = new ListRow(PlexItemRow.SETTINGS_ROW_KEY.hashCode(),
                    new HeaderItem(PlexItemRow.SETTINGS_ROW_KEY.hashCode(),
                            getString(R.string.settings)),
                    gridRowAdapter);
            mRows.put(PlexItemRow.SETTINGS_ROW_KEY, new RowData(PlexItemRow.SETTINGS_ROW_KEY, mRowsAdapter.size(), row));
            mRowsAdapter.add(row);
            gridRowAdapter.add(new SettingCard(context,
                    new SettingLaunch(context.getString(R.string.settings_server),
                            mServer.getServerName(),
                            R.drawable.ic_settings_remote_white_48dp,
                            ServerChoiceActivity.class.getName(),
                            ServerStatusHandler.SERVER_CHOICE_RESULT)));
        }

        if (!serverOnly) {

            gridRowAdapter.add(new SettingCard(context,
                    new SettingLaunch(context.getString(R.string.settings_settings),
                            "", R.drawable.ic_settings_white_48dp,
                            SettingsActivity.class.getName(),
                            0)));
        }
    }

   public void setSelectedServer(PlexServer server) {

        mServer = server;
        mSelectionHandler.setServer(server);
    }

    private void handleSectionsRow(MediaContainer sections) {

        if (sections != null) {
            RowData data = mRows.get(PlexItemRow.SECTIONS_ROW_KEY);
            if (data == null) {
                String header = sections.getTitle1();
                for (String sub : getString(R.string.main_rows_header_strip).split(";"))
                    header = header.replace(sub, "").trim();
                PlexItemRow row = PlexItemRow.buildSectionRow(getActivity(), sections, mServer, header, this, mSelectionHandler);
                mRows.put(PlexItemRow.SECTIONS_ROW_KEY, new RowData(PlexItemRow.SECTIONS_ROW_KEY, 0, row));
                mLifeCycleMgr.put(PlexItemRow.SECTIONS_ROW_KEY, row);
                mRowsAdapter.add(0, row);
            }
            else
                ((PlexItemRow) data.data).update(sections.getDirectories(), null);
        }
    }

    protected HashMap<String, Integer> makeOrderHashFromStringDelim(String str, String delim) {

        String[] tokens = str.split(delim);
        HashMap<String, Integer> map = new HashMap<>(tokens.length);
        Integer index = 0;
        for (String token : tokens)
            map.put(token, index++);
        return map;
    }

    @Override
    public void refresh() {
        mLifeCycleMgr.resumed();
    }

    private class GetRandomArtWorkTask extends AsyncTask<MediaContainer, Void, String> {

        final PlexServer server;

        GetRandomArtWorkTask(PlexServer server) {
            this.server = server;
        }

        @Override
        protected String doInBackground(MediaContainer[] params) {

            String artKey = "";
            MediaContainer sections = params != null && params.length > 0 ? params[0] : null;
            if (server != null && sections != null && sections.getDirectories() != null && !sections.getDirectories().isEmpty()) {

                int sectionPos = (int) (1000 % Math.random()) % sections.getDirectories().size();
                MediaContainer arts = server.getSectionArts(sections.getDirectories().get(sectionPos).getKey());
                if (arts != null && arts.getPhotos() != null && !arts.getPhotos().isEmpty())
                    artKey = arts.getPhotos().get((int) (1000 * Math.random()) % arts.getPhotos().size()).getKey();
            }
            return artKey;
        }

        @Override
        protected void onPostExecute(String artKey) {

            if (artKey != null && !artKey.isEmpty())
                mSelectionHandler.updateBackground(mServer.makeServerURL(artKey), true);
        }
    }
}
