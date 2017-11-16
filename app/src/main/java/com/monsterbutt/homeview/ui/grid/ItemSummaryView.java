package com.monsterbutt.homeview.ui.grid;


import android.app.Activity;
import android.content.Context;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.ui.grid.interfaces.ISummaryDisplay;

public class ItemSummaryView implements ISummaryDisplay {

  private final Context context;

  private LinearLayout summary;
  private TextView title;
  private TextView subtitle;
  private TextView description;

  ItemSummaryView(Activity activity) {
    context = activity;
    summary = activity.findViewById(R.id.summary);
    title = activity.findViewById(R.id.title);
    subtitle = activity.findViewById(R.id.subtitle);
    description = activity.findViewById(R.id.description);
  }


  public void setCurrentItem(PlexLibraryItem item) {

    if (item != null) {
      title.setText(item.getDetailTitle(context));
      subtitle.setText(item.getDetailSubtitle(context));
      description.setText(item.getSummary());
      summary.startAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_in_up));
    }
    else
      summary.startAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_out_down));

  }
}
