package com.monsterbutt.homeview.ui.android;

import android.app.Dialog;
import android.content.Context;
import android.media.MediaDescription;
import android.media.session.MediaSession;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.PlexServerManager;


public class NextUpView extends Dialog implements View.OnClickListener {

    public enum ButtonId {
        None,
        Play,
        Cancel,
        Browse
    }
    public interface Callback {

        void onNextUpButtonClicked(ButtonId id);
    }

    public static final String BUNDLE_EXTRA_SUBTITLE = "subtitle";
    public static final String BUNDLE_EXTRA_CONTENT = "content";
    public static final String BUNDLE_EXTRA_STUDIO = "studio";
    public static final String BUNDLE_EXTRA_RATING = "rating";
    public static final String BUNDLE_EXTRA_SUMMARY = "summary";
    public static final String BUNDLE_EXTRA_TITLE = "title";
    public static final String BUNDLE_THUMB = "thumb";
    public static final String BUNDLE_EXTRA_VIEWEDOFFSET = "viewedoffse";
    public static final String BUNDLE_EXTRA_MINUTES = "minutes";

    ImageView poster;
    TextView title;
    TextView subtitle;
    TextView content;
    TextView minutes;
    TextView summary;
    TextView secondsLeft;

    ImageButton playBtn;
    ImageButton cancelBtn;
    ImageButton browseBtn;

    final Callback callback;

    public NextUpView(Context context, Callback callback) {

        super(context);
        this.callback = callback;

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.nextup_view);
        poster = (ImageView) findViewById(R.id.nextup_poster);
        title = (TextView) findViewById(R.id.nextup_title);
        content = (TextView) findViewById(R.id.nextup_content);
        subtitle = (TextView) findViewById(R.id.nextup_subtitle);
        minutes = (TextView) findViewById(R.id.nextup_minutes);
        secondsLeft = (TextView) findViewById(R.id.upnext_time);
        summary = (TextView) findViewById(R.id.nextup_summary);
        playBtn = (ImageButton) findViewById(R.id.playbtn);
        playBtn.setOnClickListener(this);
        cancelBtn = (ImageButton) findViewById(R.id.cancelbtn);
        cancelBtn.setOnClickListener(this);
        browseBtn = (ImageButton) findViewById(R.id.browsebtn);
        browseBtn.setOnClickListener(this);
    }

    public void setVideo(MediaSession.QueueItem item) {

        MediaDescription desc = item.getDescription();
        Bundle bundle = desc.getExtras();
        String posterURL = desc.getIconUri() != null ? desc.getIconUri().toString() : "";
        if (bundle != null) {

            posterURL = bundle.getString(BUNDLE_THUMB);
            if (TextUtils.isEmpty(posterURL))
                posterURL = desc.getIconUri().toString();
            subtitle.setText(bundle.getString(BUNDLE_EXTRA_SUBTITLE));
            subtitle.setVisibility(subtitle.getText().length() == 0 ? View.GONE : View.VISIBLE);
            content.setText(bundle.getString(BUNDLE_EXTRA_CONTENT));
            minutes.setText(bundle.getString(BUNDLE_EXTRA_MINUTES));
            summary.setText(bundle.getString(BUNDLE_EXTRA_SUMMARY));
            title.setText(bundle.getString(BUNDLE_EXTRA_TITLE));
        }
        if (!TextUtils.isEmpty(posterURL)) {

            PlexServer server = PlexServerManager.getInstance(getContext().getApplicationContext(), null).getSelectedServer();
            Glide.with(getContext())
                    .load(server.makeServerURL(posterURL))
                    .into(poster);
        }
    }

    public void setSecondsLeft(long secondsLeft) {
        this.secondsLeft.setText(String.format(" %s ", Long.toString(secondsLeft)));
    }

    @Override
    public void onClick(View v) {

        ButtonId id = ButtonId.None;
        if (v == playBtn)
            id = ButtonId.Play;
        else if (v == cancelBtn)
            id = ButtonId.Cancel;
        else if (v == browseBtn)
            id = ButtonId.Browse;

        if (callback != null)
            callback.onNextUpButtonClicked(id);
        dismiss();
    }
}
