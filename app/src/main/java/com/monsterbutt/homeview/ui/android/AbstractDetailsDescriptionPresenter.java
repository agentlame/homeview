package com.monsterbutt.homeview.ui.android;

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

    public AbstractDetailsDescriptionPresenter(Context context, PlexServer server) {
        this.context = context;
        this.server = server;
    }

    /**
     * The ViewHolder for the {@link AbstractDetailsDescriptionPresenter}.
     */
    public static class ViewHolder extends Presenter.ViewHolder {

        public final TextView Title;
        public final TextView Subtitle;
        public final TextView Description;
        public final TextView Tagline;
        public final TextView Genre;
        public final ImageView Studio;
        public final ImageView Rating;
        public final ProgressBar Progress;
        public final ImageView Watched;

        public final ImageView Resolution;
        public final ImageView FrameRate;
        public final ImageView VideoCodec;

        public final ImageView AudioCodec;
        public final ImageView AudioChannels;

        public ViewHolder(final View view) {
            super(view);

            Title = (TextView) view.findViewById(R.id.title);
            Subtitle = (TextView) view.findViewById(R.id.subtitle);
            Description = (TextView) view.findViewById(R.id.description);
            Tagline = (TextView) view.findViewById(R.id.tagline);
            Genre = (TextView) view.findViewById(R.id.genre);
            Studio = (ImageView) view.findViewById(R.id.studio);
            Rating = (ImageView) view.findViewById(R.id.ratingImage);
            Progress = (ProgressBar) view.findViewById(R.id.progress);
            Watched = (ImageView) view.findViewById(R.id.unwatched);
            Resolution = (ImageView) view.findViewById(R.id.resolutionImage);
            FrameRate = (ImageView) view.findViewById(R.id.framerateImage);
            VideoCodec = (ImageView) view.findViewById(R.id.videoImage);
            AudioCodec = (ImageView) view.findViewById(R.id.audioImage);
            AudioChannels = (ImageView) view.findViewById(R.id.channelsImage);
        }

        public boolean setImage(Context context, ImageView image, String path) {

            boolean ret = !TextUtils.isEmpty(path);
            if (ret) {
                image.setVisibility(View.VISIBLE);
                Glide.with(context).load(path).into(image);
            }
            else if (image != null) {
                image.setVisibility(View.INVISIBLE);
                image.setImageDrawable(null);
            }
            return ret;
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.lb_details_description, parent, false));
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

