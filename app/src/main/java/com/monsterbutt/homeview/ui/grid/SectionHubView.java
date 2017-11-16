package com.monsterbutt.homeview.ui.grid;


import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.Button;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.ui.sectionhub.SectionHubActivity;
import com.monsterbutt.homeview.ui.grid.interfaces.IGridParent;

class SectionHubView {

  private final Activity activity;
  private final IGridParent gridParent;

  SectionHubView(Activity activity, View view, IGridParent gridParent, boolean hide) {
    this.activity = activity;
    this.gridParent = gridParent;
    Button hubBtn = view.findViewById(R.id.hubBtn);
    hubBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        hubButtonClicked();
      }
    });

    if (hide)
      hubBtn.setVisibility(View.GONE);
  }

  private void hubButtonClicked() {

    Intent intent = new Intent(activity, SectionHubActivity.class);
    intent.putExtra(SectionHubActivity.TITLE, gridParent.getContainer().getLibrarySectionTitle());
    intent.putExtra(SectionHubActivity.SECTIONID, gridParent.getContainer().getLibrarySectionID());
    activity.startActivity(intent);
  }
}
