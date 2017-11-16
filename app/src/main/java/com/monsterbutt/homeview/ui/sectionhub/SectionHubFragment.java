package com.monsterbutt.homeview.ui.sectionhub;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.view.View;
import android.widget.TextView;

import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.PlexServerManager;
import com.monsterbutt.homeview.ui.presenters.CustomListRowPresenter;
import com.monsterbutt.homeview.ui.UILifecycleManager;
import com.monsterbutt.homeview.ui.SelectionHandler;
import com.monsterbutt.homeview.ui.BackgroundHandler;


public class SectionHubFragment extends BrowseFragment implements CustomListRowPresenter.Callback {

  private UILifecycleManager lifeCycleMgr = new UILifecycleManager();

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    Activity activity = getActivity();
    TextView text = activity.findViewById(android.support.v17.leanback.R.id.title_text);
    text.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);

    Intent intent = activity.getIntent();
    setTitle(intent.getStringExtra(SectionHubActivity.TITLE));

    setHeadersTransitionOnBackEnabled(false);
    ArrayObjectAdapter rowsAdapter = new ArrayObjectAdapter(new CustomListRowPresenter(this));
    setAdapter(rowsAdapter);

    PlexServer server = PlexServerManager.getInstance().getSelectedServer();
    SelectionHandler selectionHandler = new SelectionHandler(this,
     new BackgroundHandler(getActivity(), server, lifeCycleMgr));
    new SectionHubRows(intent.getStringExtra(SectionHubActivity.SECTIONID),
     this, server, lifeCycleMgr, selectionHandler, rowsAdapter);
  }

  @Override
  public void onResume() {
    super.onResume();
    lifeCycleMgr.resumed();
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

}
