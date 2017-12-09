package com.monsterbutt.homeview.ui.main;

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
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import com.monsterbutt.homeview.plex.PlexServerManager;
import com.monsterbutt.homeview.plex.StatusWatcher;
import com.monsterbutt.homeview.ui.main.tasks.GetRandomArtWorkTask;
import com.monsterbutt.homeview.ui.presenters.CardPresenter;
import com.monsterbutt.homeview.ui.presenters.CustomListRowPresenter;
import com.monsterbutt.homeview.ui.presenters.SettingCard;
import com.monsterbutt.homeview.ui.presenters.SettingPresenter;
import com.monsterbutt.homeview.settings.SettingLaunch;
import com.monsterbutt.homeview.ui.HubRows;
import com.monsterbutt.homeview.ui.UILifecycleManager;
import com.monsterbutt.homeview.ui.search.SearchActivity;
import com.monsterbutt.homeview.ui.settings.SettingsActivity;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.ui.SelectionHandler;
import com.monsterbutt.homeview.ui.BackgroundHandler;
import com.monsterbutt.homeview.ui.interfaces.ILeanbackFragment;


import us.nineworlds.plex.rest.model.impl.MediaContainer;

import static com.monsterbutt.homeview.ui.main.ServerConnectFragment.SERVER_CHOICE_RESULT;

public class MainFragment extends BrowseFragment
 implements CustomListRowPresenter.Callback, ILeanbackFragment {

  private ArrayObjectAdapter rowsAdapter = new ArrayObjectAdapter(new CustomListRowPresenter(this));
  private final UILifecycleManager lifeCycleMgr = new UILifecycleManager();
  private final StatusWatcher statusWatcher = new StatusWatcher();
  private HubRows hubRows = null;
  private MainSectionsRow mainSectionsRow;
  private BackgroundHandler backgroundHandler;
  private SelectionHandler selectionHandler;

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {

    super.onActivityCreated(savedInstanceState);
    setHeaderTitle("");
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

    TitleView tv = getActivity().findViewById(android.support.v17.leanback.R.id.browse_title_group);
    TextView text = tv.findViewById(android.support.v17.leanback.R.id.title_text);
    text.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
    setAdapter(rowsAdapter);
    setBrowseTransitionListener(new BrowseTransitionListener() {

      @Override
      public void onHeadersTransitionStart(boolean withHeaders) {
        if (!withHeaders)
          backgroundHandler.updateBackgroundTimed(selectionHandler.getSelection());
      }
    });
    backgroundHandler = new BackgroundHandler(getActivity(), null, lifeCycleMgr);
    selectionHandler = new SelectionHandler(this, statusWatcher, backgroundHandler);

    setServer(PlexServerManager.getInstance().getSelectedServer());
  }

  @Override
  public void onPause() {
    super.onPause();
    lifeCycleMgr.paused();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    lifeCycleMgr.destroyed();
  }

  @Override
  public void onResume() {
    super.onResume();
    lifeCycleMgr.resumed();
  }

  private void setServer(PlexServer server) {

    release();
    boolean isValid = server != null && server.isValid();
    addSettingsRow(isValid ? server.getServerName() : "", !isValid);
    if (isValid) {
      new GetLibraryTask(this, server).execute();
      backgroundHandler.setServer(server);
      new GetRandomArtWorkTask(backgroundHandler, server).execute();
      mainSectionsRow = new MainSectionsRow(getContext(), server, lifeCycleMgr,
       new CardPresenter(server, selectionHandler, false));
      rowsAdapter.add(0, mainSectionsRow);
      hubRows = new MainHubRows(this, statusWatcher, server, lifeCycleMgr, selectionHandler, rowsAdapter);
    }
    else
      setHeaderTitle("");
  }

  private void release() {
    if (mainSectionsRow != null) {
      mainSectionsRow.release();
      mainSectionsRow = null;
    }
    if (hubRows != null) {
      hubRows.release();
      hubRows = null;
    }
    rowsAdapter.clear();
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == SERVER_CHOICE_RESULT)
      setServer(PlexServerManager.getInstance().getSelectedServer());
  }

  private void addSettingsRow(String serverName, boolean serverOnly) {

    Context context = getActivity();
    ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(new SettingPresenter(this, null));
    rowsAdapter.add(new ListRow(new HeaderItem(getString(R.string.settings)), gridRowAdapter));
    gridRowAdapter.add(new SettingCard(context,
     new SettingLaunch(context.getString(R.string.settings_server), serverName, R.drawable.ic_settings_remote_white_48dp,
      ServerChoiceActivity.class.getName(), SERVER_CHOICE_RESULT)));

    if (!serverOnly) {
      gridRowAdapter.add(new SettingCard(context,
       new SettingLaunch(context.getString(R.string.settings_settings), "", R.drawable.ic_settings_white_48dp,
        SettingsActivity.class.getName(), 0)));
    }
  }

  public void setHeaderTitle(String title) {
    try {
      setTitle(!TextUtils.isEmpty(title) ? title : getString(R.string.app_name));
    } catch (IllegalStateException e) {
      setTitle("");
    }
  }


  private static class GetLibraryTask extends AsyncTask<Void, Void, String> {

    private final PlexServer server;
    private final ILeanbackFragment callback;

    GetLibraryTask(ILeanbackFragment callback, PlexServer server) {
      this.server = server;
      this.callback = callback;
    }

    @Override
    protected String doInBackground(Void... voids) {
      MediaContainer mc = server.getLibrary();
      return mc != null ? mc.getTitle1() : "";
    }

    @Override
    protected void onPostExecute(String result) {
          callback.setHeaderTitle(result);
      }

  }


}
