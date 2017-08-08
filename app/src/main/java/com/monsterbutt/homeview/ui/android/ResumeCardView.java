package com.monsterbutt.homeview.ui.android;

import android.content.Context;
import android.support.v17.leanback.widget.BaseCardView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;

import com.monsterbutt.homeview.R;


public class ResumeCardView extends BaseCardView {

  public TextView title;
  public TextView time;
  public ImageView image;


  public ResumeCardView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    // Make sure the ImagePlusCardView is focusable.
    setFocusable(true);
    setFocusableInTouchMode(true);

    LayoutInflater inflater = LayoutInflater.from(getContext());
    inflater.inflate(R.layout.lb_resume_card_view, this);

    title = findViewById(R.id.title);
    time = findViewById(R.id.time);
    image = findViewById(R.id.image);
  }

  public ResumeCardView(Context context) {
    this(context, null);
  }

  public ResumeCardView(Context context, AttributeSet attrs) {
    this(context, attrs, android.support.v17.leanback.R.attr.imageCardViewStyle);
  }

}
