package com.monsterbutt.homeview.presenters;

import android.content.res.Resources;
import android.support.v17.leanback.widget.Presenter;
import android.util.TypedValue;
import android.view.ViewGroup;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.ui.android.ResumeCardView;


public class ResumeChoicePresenter extends Presenter {


  private int mSelectedBackgroundColor = -1;
  private int mDefaultBackgroundColor = -1;

  @Override
  public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {

    TypedValue typedValue = new TypedValue();
    Resources.Theme theme = parent.getContext().getTheme();
    theme.resolveAttribute(R.attr.card_normal, typedValue, true);
    mDefaultBackgroundColor = typedValue.data;
    theme.resolveAttribute(R.attr.card_selected, typedValue, true);
    mSelectedBackgroundColor = typedValue.data;

    ResumeCardView cardView = new ResumeCardView(parent.getContext()) {
      @Override
      public void setSelected(boolean selected) {
        updateCardBackgroundColor(this, selected);
        super.setSelected(selected);
      }
    };

    cardView.setFocusable(true);
    cardView.setFocusableInTouchMode(true);
    updateCardBackgroundColor(cardView, false);
    return new Presenter.ViewHolder(cardView);
  }

  private void updateCardBackgroundColor(ResumeCardView view, boolean selected) {
    int color = selected ? mSelectedBackgroundColor : mDefaultBackgroundColor;

    // Both background colors should be set because the view's
    // background is temporarily visible during animations.
    view.setBackgroundColor(color);
  }

  @Override
  public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {

    ResumeChoiceCard obj = (ResumeChoiceCard) item;
    ResumeCardView view = (ResumeCardView) viewHolder.view;

    view.title.setText(obj.title);
    view.time.setText(obj.time);
    view.image.setImageDrawable(obj.image);
  }

  @Override
  public void onUnbindViewHolder(ViewHolder viewHolder) {
  }

}
