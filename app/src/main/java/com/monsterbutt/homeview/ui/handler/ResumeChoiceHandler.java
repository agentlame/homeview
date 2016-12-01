package com.monsterbutt.homeview.ui.handler;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.player.StartPosition;
import com.monsterbutt.homeview.ui.fragment.PlaybackFragment;

import java.util.ArrayList;
import java.util.List;

public class ResumeChoiceHandler implements DialogInterface.OnClickListener,
                                            DialogInterface.OnDismissListener,
                                            DialogInterface.OnCancelListener,
                                            Runnable {

    private static class ResumeChoice {

        public final int id;
        public final int drawable;
        public final String text;

        public ResumeChoice(int id, int drawable, String text) {

            this.id = id;
            this.drawable = drawable;
            this.text = text;
        }
    }

    private static class ResumeChoiceArrayAdapter extends ArrayAdapter<ResumeChoice> {

        private final List<ResumeChoice> values;
        private final Context context;

        public ResumeChoiceArrayAdapter(Context context, List<ResumeChoice> values) {
            super(context, R.layout.lb_aboutitem, values);
            this.context = context;
            this.values = values;
        }

        @Override
        public View getView(int position, View row, ViewGroup parent) {

            View rowView = row;
            if (rowView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                rowView = inflater.inflate(R.layout.lb_resumechoice, parent, false);
            }
            final ResumeChoice item = values.get(position);
            ImageView image = (ImageView) rowView.findViewById(R.id.resumeimage);
            image.setImageDrawable(context.getDrawable(item.drawable));
            TextView desc = (TextView) rowView.findViewById(R.id.resumedesc);
            desc.setText(item.text);
            return rowView;
        }
    }

    public static void askUser(PlaybackFragment fragment, SimpleExoPlayer player, long lastVideoOffset, int timeout) {

        new ResumeChoiceHandler(fragment, player, lastVideoOffset, timeout);
    }

    private static ResumeChoiceArrayAdapter getResumeChoiceAdapter(Activity activity) {

        List<ResumeChoice> list = new ArrayList<>();
        list.add(new ResumeChoice(0, R.drawable.ic_slow_motion_video_white_48dp, activity.getString(R.string.playback_start_dialog_resume)));
        list.add(new ResumeChoice(1, R.drawable.ic_play_circle_outline_white_48dp, activity.getString(R.string.playback_start_dialog_begin)));
        return new ResumeChoiceArrayAdapter(activity, list);
    }


    private PlaybackFragment fragment;
    private Handler handler  = new Handler();
    private AlertDialog alert;
    private SimpleExoPlayer player;
    private long lastVideoOffset;

    private ResumeChoiceHandler(PlaybackFragment fragment, SimpleExoPlayer player, long lastVideoOffset, int timeout) {

        this.fragment = fragment;
        this.player = player;
        this.lastVideoOffset = lastVideoOffset;
        Activity activity = fragment.getActivity();
        alert = new AlertDialog.Builder(activity, R.style.AlertDialogStyle)
                .setIcon(R.drawable.launcher)
                .setTitle(R.string.playback_start_dialog)
                .setAdapter(getResumeChoiceAdapter(activity), this)
                .setOnDismissListener(this)
                .create();
        alert.show();
        handler.postDelayed(this, timeout);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {

        synchronized (this) {
            final StartPosition.PlaybackStartType val = StartPosition.PlaybackStartType.values()[which];
            if (val == StartPosition.PlaybackStartType.Begining)
                return;
            fragment.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    switch (player.getPlaybackState()) {

                        case ExoPlayer.STATE_READY:
                        case ExoPlayer.STATE_ENDED:
                        case ExoPlayer.STATE_BUFFERING:
                        case ExoPlayer.STATE_IDLE:

                            player.seekTo(lastVideoOffset);
                            fragment.tickle();
                            break;
                        default:
                            break;
                    }
                }
            });
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        synchronized (this) {
            handler.removeCallbacks(this);
            alert = null;
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        onDismiss(dialog);
    }

    @Override
    public void run() {
        synchronized (this) {
            if (alert != null && alert.isShowing())
                alert.dismiss();
        }
    }
}
