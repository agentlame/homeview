package com.monsterbutt.homeview.ui.details.presenters;

import android.content.Context;
import android.support.v17.leanback.widget.DetailsOverviewRow;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.Presenter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;

public abstract class AbstractDetailsDescriptionPresenter extends Presenter {

    protected PlexServer server;
    protected Context context;

    AbstractDetailsDescriptionPresenter(Context context, PlexServer server) {
        this.context = context;
        this.server = server;
    }

    /**
     * The ViewHolder for the {@link AbstractDetailsDescriptionPresenter}.
     */
    public static class ViewHolder extends Presenter.ViewHolder {

        public final TextView Title;
        public final TextView Subtitle;
        final TextView Description;
        final TextView Tagline;
        final TextView Genre;
        final ImageView Studio;
        final ImageView Rating;
        final ProgressBar Progress;
        public final ImageView Watched;

        final ImageView Resolution;
        final ImageView FrameRate;
        final ImageView VideoCodec;

        final ImageView AudioCodec;
        final ImageView AudioChannels;

        public ViewHolder(final View view) {
            super(view);

            Title = view.findViewById(R.id.title);
            Subtitle = view.findViewById(R.id.subtitle);
            Description = view.findViewById(R.id.description);
            Tagline = view.findViewById(R.id.tagline);
            Genre = view.findViewById(R.id.genre);
            Studio = view.findViewById(R.id.studio);
            Rating = view.findViewById(R.id.ratingImage);
            Progress = view.findViewById(R.id.progress);
            Watched = view.findViewById(R.id.unwatched);
            Resolution = view.findViewById(R.id.resolutionImage);
            FrameRate = view.findViewById(R.id.framerateImage);
            VideoCodec = view.findViewById(R.id.videoImage);
            AudioCodec = view.findViewById(R.id.audioImage);
            AudioChannels = view.findViewById(R.id.channelsImage);
        }

        void setImage(Context context, ImageView image, String path) {

            boolean ret = !TextUtils.isEmpty(path);
            if (ret) {
                image.setVisibility(View.VISIBLE);
                Glide.with(context).load(path).into(image);
            }
            else if (image != null) {
                image.setVisibility(View.INVISIBLE);
                image.setImageDrawable(null);
            }
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.lb_details_description_homeview, parent, false));
    }

    @Override
    public final void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        ViewHolder vh = (ViewHolder) viewHolder;
        onBindDescription(vh, item);
    }

    /**
     * Binds the data from the item to the ViewHolder.  The item is typically associated with
     * a {@link DetailsOverviewRow} or {@link PlaybackControlsRow}.
     *
     * @param vh The ViewHolder for this details description view.
     * @param item The item being presented.
     */
    protected abstract void onBindDescription(ViewHolder vh, Object item);

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {}

    @Override
    public void onViewAttachedToWindow(Presenter.ViewHolder holder) {
        // In case predraw listener was removed in detach, make sure
        // we have the proper layout.
        super.onViewAttachedToWindow(holder);
    }

    @Override
    public void onViewDetachedFromWindow(Presenter.ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
    }
}

