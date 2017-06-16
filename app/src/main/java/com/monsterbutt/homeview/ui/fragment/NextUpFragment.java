package com.monsterbutt.homeview.ui.fragment;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


@SuppressLint("ValidFragment")
public class NextUpFragment extends Fragment {

  public interface NextUpCallback {

    enum Clicked {
      StartNext,
      StopList,
      ShowList
    }

    void clicked(Clicked button);
  }

  private final PlexVideoItem video;
  private final PlexServer server;
  private final Context context;
  private final long currentTimeLeft;
  private final NextUpCallback callback;
  private final int height;

  public NextUpFragment(Context context, PlexServer server, PlexVideoItem item,
                        long currentTimeLeft, NextUpCallback callback, int height) {
    this.context = context;
    this.server = server;
    this.height = height;
    video = item;
    this.currentTimeLeft = currentTimeLeft;
    this.callback = callback;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    View view = inflater.inflate(R.layout.lb_nextup_fragment, container, false);

    ((TextView) view.findViewById(R.id.title)).setText(video.getPlaybackTitle(context));
    ((TextView) view.findViewById(R.id.description)).setText(video.getPlaybackDescription(context));
    ((TextView) view.findViewById(R.id.subtitle)).setText(video.getPlaybackSubtitle(context, true));

    String out = " " + new SimpleDateFormat("h:mm", Locale.US).format((new Date()).getTime() + video.getDuration() + currentTimeLeft);
    ((TextView) view.findViewById(R.id.end_time)).setText(out);

    view.findViewById(R.id.nextup_startNext).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) { clicked(NextUpCallback.Clicked.StartNext); }
    });
    view.findViewById(R.id.nextup_startNext).requestFocus();
    view.findViewById(R.id.nextup_stopList).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) { clicked(NextUpCallback.Clicked.StopList); }
    });
    view.findViewById(R.id.nextup_showList).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) { clicked(NextUpCallback.Clicked.ShowList); }
    });

    String path = server.makeServerURL(video.getCardImageURL());
    Glide.with(getContext()).load(path).into(((ImageView) view.findViewById(R.id.posterImage)));

    return view;
  }

  private void clicked(NextUpCallback.Clicked clicked) {
    if (callback != null)
      callback.clicked(clicked);
  }
}