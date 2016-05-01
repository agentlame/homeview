package com.monsterbutt.homeview.ui.android;

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

import com.monsterbutt.homeview.R;

public abstract class AbstractDetailsDescriptionPresenter extends Presenter {

    /**
     * The ViewHolder for the {@link AbstractDetailsDescriptionPresenter}.
     */
    public static class ViewHolder extends Presenter.ViewHolder {
        private final TextView mTitle;
        private final TextView mSubtitle;
        private final TextView mContent;
        private final TextView mBody;
        private final TextView mDuration;
        private final TextView mGenre;
        private final ImageView mStudio;
        private final ImageView mRating;
        private final ProgressBar mProgress;
        private final ImageView mFlagView;

        private boolean mHasStudio = false;
        private boolean mHasRating = false;

        public ViewHolder(final View view) {
            super(view);
            mTitle = (TextView) view.findViewById(R.id.lb_details_description_title);
            mSubtitle = (TextView) view.findViewById(R.id.lb_details_description_subtitle);
            mBody = (TextView) view.findViewById(R.id.lb_details_description_body);
            mContent = (TextView) view.findViewById(R.id.lb_details_description_content);
            mDuration = (TextView) view.findViewById(R.id.lb_details_description_duration);
            mGenre = (TextView) view.findViewById(R.id.lb_details_description_genre);
            mStudio = (ImageView) view.findViewById(R.id.lb_details_description_studio);
            mRating = (ImageView) view.findViewById(R.id.lb_details_description_rating);
            mProgress = (ProgressBar) view.findViewById(R.id.progress);
            mFlagView = (ImageView) view.findViewById(R.id.unwatched);
        }

        public TextView getTitle() {
            return mTitle;
        }

        public TextView getSubtitle() {
            return mSubtitle;
        }

        public TextView getBody() {
            return mBody;
        }

        public TextView getContent() {
            return mContent;
        }

        public TextView getDuration() {
            return mDuration;
        }

        public TextView getGenre() {
            return mGenre;
        }

        public void hasStudio(boolean hasStudio) { mHasStudio = hasStudio; }
        public void hasRating(boolean hasRating) { mHasRating = hasRating; }

        public ImageView getStudio() {
            return mStudio;
        }

        public ImageView getRating() {
            return mRating;
        }

        public ProgressBar getProgress() {
            return mProgress;
        }

        public ImageView getWatched() {
            return mFlagView;
        }
    }
    @Override
    public final ViewHolder onCreateViewHolder(ViewGroup parent) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.lb_details_description, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public final void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        ViewHolder vh = (ViewHolder) viewHolder;
        onBindDescription(vh, item);

        setTextViewVisibility(vh.mTitle);
        setTextViewVisibility(vh.mBody);
        setTextViewVisibility(vh.mContent);
        setTextViewVisibility(vh.mDuration);
        setTextViewVisibility(vh.mSubtitle);
        setTextViewVisibility(vh.mGenre);
        setImageViewVisibility(vh.mStudio, vh.mHasStudio);
        setImageViewVisibility(vh.mRating, vh.mHasRating);
    }

    private void setImageViewVisibility(ImageView view, boolean viewHasDrawable) {

        view.setVisibility(viewHasDrawable ? View.VISIBLE : View.GONE);
    }

    private void setTextViewVisibility(TextView view) {

        view.setVisibility(TextUtils.isEmpty(view.getText()) ? View.GONE : View.VISIBLE);
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

