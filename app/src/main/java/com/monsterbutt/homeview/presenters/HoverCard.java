package com.monsterbutt.homeview.presenters;

import android.content.Context;
import android.support.v17.leanback.widget.Presenter;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.monsterbutt.homeview.R;

import org.w3c.dom.Text;


public class HoverCard extends Presenter {
  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent) {
    HoverCardView view =  new HoverCardView(parent.getContext());
    view.setFocusable(false);
    view.setFocusableInTouchMode(false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(ViewHolder viewHolder, Object item) {

    PosterCard obj = (PosterCard) item;
    HoverCardView view = (HoverCardView) viewHolder.view;
    view.title.setText(obj.getDetailTitle());
    view.subtitle.setText(obj.getDetailSubtitle());
    view.description.setText(obj.getSummary());
  }

  @Override
  public void onUnbindViewHolder(ViewHolder viewHolder) {

  }

  private class HoverCardView extends LinearLayout {

    TextView title;
    TextView subtitle;
    TextView description;

    public HoverCardView(Context context) {
      super(context);

      LayoutInflater inflater = LayoutInflater.from(getContext());
      inflater.inflate(R.layout.lb_hover_card, this);

      title = (TextView) findViewById(R.id.title);
      subtitle = (TextView) findViewById(R.id.subtitle);
      description = (TextView) findViewById(R.id.description);
    }
  }
}
