package com.monsterbutt.homeview.ui.android;

import android.content.Context;
import android.media.MediaDescription;
import android.media.session.MediaSession;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.PlexServerManager;


public class NextUpView extends FrameLayout {

    public static final String BUNDLE_EXTRA_TYPE_SHOW = "type_is_show";
    public static final String BUNDLE_EXTRA_SHOW_NAME = "showname";
    public static final String BUNDLE_EXTRA_SHOW_EPISODE = "showepisode";
    public static final String BUNDLE_EXTRA_STUDIO = "studio";
    public static final String BUNDLE_EXTRA_YEAR = "year";
    public static final String BUNDLE_EXTRA_SUMMARY = "summary";
    public static final String BUNDLE_EXTRA_TITLE = "title";
    public static final String BUNDLE_EXTRA_VIEWEDOFFSET = "viewedoffse";

    ImageView poster;
    TextView title;
    TextView subtitle;
    TextView extra;
    TextView summary;

    public NextUpView(Context context, AttributeSet attrs) {
        super(context, attrs);

        View view = inflate(getContext(), R.layout.nextup_view, null);
        poster = (ImageView) view.findViewById(R.id.nextup_poster);
        title = (TextView) view.findViewById(R.id.nextup_title);
        subtitle = (TextView) view.findViewById(R.id.nextup_subtitle);
        extra = (TextView) view.findViewById(R.id.nextup_extra);
        summary = (TextView) view.findViewById(R.id.nextup_summary);
        addView(view);
    }

    public void setVideo(MediaSession.QueueItem item) {

        MediaDescription desc = item.getDescription();
        Bundle bundle = desc.getExtras();

        if (bundle != null) {

            if (bundle.getBoolean(BUNDLE_EXTRA_TYPE_SHOW, false)) {

                subtitle.setText(bundle.getString(BUNDLE_EXTRA_SHOW_NAME));
                extra.setText(bundle.getString(BUNDLE_EXTRA_SHOW_EPISODE));
            } else {

                subtitle.setText(bundle.getString(BUNDLE_EXTRA_YEAR));
                extra.setText(bundle.getString(BUNDLE_EXTRA_STUDIO));
            }
            summary.setText(bundle.getString(BUNDLE_EXTRA_SUMMARY));
            title.setText(bundle.getString(BUNDLE_EXTRA_TITLE));
        }
        if (desc.getIconUri() != null) {

            PlexServer server = PlexServerManager.getInstance(getContext().getApplicationContext()).getSelectedServer();
            Glide.with(getContext())
                    .load(server.makeServerURL(desc.getIconUri().toString()))
                    .into(poster);
        }
    }
}
