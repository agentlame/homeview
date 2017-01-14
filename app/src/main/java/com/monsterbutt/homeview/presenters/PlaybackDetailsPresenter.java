package com.monsterbutt.homeview.presenters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.player.PlaybackControlHelper;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.ui.android.AbstractDetailsDescriptionPresenter;

public class PlaybackDetailsPresenter extends AbstractDetailsDescriptionPresenter {

    public PlaybackDetailsPresenter(Context context, PlexServer server) {
        super(context, server);
    }

    @Override
    protected void onBindDescription(AbstractDetailsDescriptionPresenter.ViewHolder viewHolder, Object object) {
        PlaybackControlHelper glue = (PlaybackControlHelper) object;
        boolean hasMedia = glue.hasValidMedia();
        viewHolder.getTitle().setText(hasMedia ? glue.getMediaTitle() : "");
        viewHolder.getSubtitle().setText(hasMedia ? glue.getMediaSubtitle() : "");
        //viewHolder.getBody().setText(hasMedia ? glue.getMediaDescription() : "");
        viewHolder.hasStudio(setImage(context, viewHolder.getStudio(), server.makeServerURLForCodec("studio", glue.getMediaStudio())));
        viewHolder.hasRating(setImage(context, viewHolder.getRating(), server.makeServerURLForCodec("contentRating", glue.getMediaRating())));
    }

    @Override
    public final ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.lb_playback_details, parent, false));
    }


}
