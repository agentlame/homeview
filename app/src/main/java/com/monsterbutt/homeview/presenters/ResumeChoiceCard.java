package com.monsterbutt.homeview.presenters;


import android.content.Context;
import android.graphics.drawable.Drawable;

import com.monsterbutt.homeview.Utils;

public class ResumeChoiceCard extends PosterCard {

  public String title;
  public String time = "";
  public long offset;
  public Drawable image;

  public ResumeChoiceCard(Context context, String title, long offset, Drawable image) {
    super(context, null);
    this.title = title;
    this.offset = offset;
    if (offset > 0)
      time = Utils.timeMStoString(context, offset);
    this.image = image;
  }
}
