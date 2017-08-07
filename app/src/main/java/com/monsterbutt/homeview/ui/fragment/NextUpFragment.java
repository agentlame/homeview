package com.monsterbutt.homeview.ui.fragment;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
      Close,
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

  private CountDownTimer timer = null;

  private TextView countdownSeconds;

  public NextUpFragment(Context context, PlexServer server, PlexVideoItem item,
                        long currentTimeLeft, NextUpCallback callback, int height) {
    this.context = context;
    this.server = server;
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
    countdownSeconds = view.findViewById(R.id.nextup_seconds);
    countdownSeconds.setText(String.format("%d", currentTimeLeft / 1000));
    view.findViewById(R.id.nextup_close).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) { clicked(NextUpCallback.Clicked.Close); }
    });
    view.findViewById(R.id.nextup_close).requestFocus();
    view.findViewById(R.id.nextup_startNext).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) { clicked(NextUpCallback.Clicked.StartNext); }
    });
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
    timer = new CountDownTimer(currentTimeLeft, 1000);
    timer.start();
    return view;
  }

  public void release() {

    if (timer != null)
      timer.cancel();
  }

  private void clicked(NextUpCallback.Clicked clicked) {
    if (callback != null)
      callback.clicked(clicked);
  }

  private class CountDownTimer extends android.os.CountDownTimer {

    CountDownTimer(long millisInFuture, long countDownInterval) {
      super(millisInFuture, countDownInterval);
    }

    @Override
    public void onTick(long l) {
      countdownSeconds.setText(String.format("%d", l / 1000));
    }

    @Override
    public void onFinish() {
      countdownSeconds.setText("0");
    }
  }
}
