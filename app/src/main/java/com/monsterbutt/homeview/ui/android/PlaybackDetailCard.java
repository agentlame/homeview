package com.monsterbutt.homeview.ui.android;


import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.player.MediaTrackSelector;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.plex.media.Stream;
import com.monsterbutt.homeview.plex.media.VideoFormat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import us.nineworlds.plex.rest.model.impl.Media;

public class PlaybackDetailCard extends RelativeLayout {



  public PlaybackDetailCard(Context context) {
    this(context, null);
  }

  public PlaybackDetailCard(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public PlaybackDetailCard(Context context, AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, 0);
  }

  public PlaybackDetailCard(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);

    LayoutInflater inflater = LayoutInflater.from(getContext());
    inflater.inflate(R.layout.lb_playback_detail_card, this);



  }

}
