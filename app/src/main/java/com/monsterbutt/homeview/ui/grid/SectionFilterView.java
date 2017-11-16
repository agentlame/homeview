package com.monsterbutt.homeview.ui.grid;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.ui.C;
import com.monsterbutt.homeview.ui.grid.interfaces.IGridParent;
import com.monsterbutt.homeview.ui.BackgroundHandler;


import us.nineworlds.plex.rest.model.impl.MediaContainer;

class SectionFilterView {

  private final BackgroundHandler backgroundHandler;
  private SectionFilterArrayAdapter mFilters;
  private final TextView mFilterText;
  private final Activity activity;
  private final IGridParent gridParent;
  private final PlexServer server;

  SectionFilterView(Activity activity, View view, IGridParent gridParent,
                    PlexServer server, BackgroundHandler backgroundHandler, boolean hide) {
    this.backgroundHandler = backgroundHandler;
    this.activity = activity;
    this.server = server;
    this.gridParent = gridParent;
    Button filterBtn = view.findViewById(R.id.filterBtn);
    filterBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        clicked();
      }
    });
    mFilterText = view.findViewById(R.id.filterText);
    if (hide) {
      filterBtn.setVisibility(View.GONE);
      mFilterText.setVisibility(View.GONE);
    }
  }

  private void clicked() {

    new AlertDialog.Builder(activity, R.style.AlertDialogStyle)
     .setIcon(R.drawable.launcher)
     .setTitle(R.string.filter_title)
     .setAdapter(mFilters, new DialogInterface.OnClickListener() {
       @Override
       public void onClick(DialogInterface dialog, int which) {

         SectionFilter selected = mFilters.selected(which);
         if (selected.key.startsWith("/")) {
           Intent intent = new Intent(activity, GridActivity.class);
           intent.putExtra(C.KEY, selected.key);
           if (backgroundHandler.getBackgroundURL() != null)
             intent.putExtra(C.BACKGROUND, backgroundHandler.getBackgroundURL());
           activity.startActivity(intent);
         }
         else{
           mFilterText.setText(selected.name);
           runFilter(mFilters, selected.key);
         }
         dialog.dismiss();
       }
     })
     .create()
     .show();
  }

  void runFilter(SectionFilterArrayAdapter adapter, String key) {
    mFilters = adapter;
    new LoadSectionFilterTask(this, gridParent.getContainer().getLibrarySectionID(), key).execute();
  }

  private static class LoadSectionFilterTask extends AsyncTask<Void, Void, MediaContainer> {

    private final SectionFilterView view;
    private final String sectionId;
    private final String key;

    LoadSectionFilterTask(SectionFilterView view, String sectionId, String key) {
      this.view = view;
      this.sectionId = sectionId;
      this.key = key;
    }

    @Override
    protected MediaContainer doInBackground(Void... params) {
      return view.server.getSectionFilter(sectionId, key);
    }

    @Override
    protected void onPostExecute(MediaContainer mc) {
      if (view.mFilters.selected() != null)
        view.mFilterText.setText(view.mFilters.selected().name);
      view.gridParent.loadData(mc);
    }
  }

}
