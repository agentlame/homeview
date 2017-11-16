package com.monsterbutt.homeview.ui.details.presenters;


import android.content.res.Resources;

import android.support.v17.leanback.widget.DetailsOverviewLogoPresenter;

import android.support.v17.leanback.widget.DetailsOverviewRow;
import android.support.v17.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import android.support.v17.leanback.widget.Presenter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.monsterbutt.homeview.R;


public class MovieDetailsOverviewLogoPresenter extends DetailsOverviewLogoPresenter {

    private final boolean usePoster;

    public MovieDetailsOverviewLogoPresenter(boolean usePoster) {
        this.usePoster = usePoster;
    }

    static class ViewHolder extends DetailsOverviewLogoPresenter.ViewHolder {
        public ViewHolder(View view) {
            super(view);
        }

        public FullWidthDetailsOverviewRowPresenter getParentPresenter() {
            return mParentPresenter;
        }

        public FullWidthDetailsOverviewRowPresenter.ViewHolder getParentViewHolder() {
            return mParentViewHolder;
        }
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        ImageView imageView = (ImageView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.lb_fullwidth_details_overview_logo, parent, false);

        Resources res = parent.getResources();
        int width = res.getDimensionPixelSize(usePoster ? R.dimen.DETAIL_POSTER_WIDTH : R.dimen.DETAIL_THUMBNAIL_WIDTH);
        int height = res.getDimensionPixelSize(usePoster ? R.dimen.DETAIL_POSTER_HEIGHT : R.dimen.DETAIL_THUMBNAIL_HEIGHT);
        imageView.setLayoutParams(new ViewGroup.MarginLayoutParams(width, height));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        return new ViewHolder(imageView);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        DetailsOverviewRow row = (DetailsOverviewRow) item;
        ImageView imageView = ((ImageView) viewHolder.view);
        imageView.setImageDrawable(row.getImageDrawable());
        if (isBoundToImage((ViewHolder) viewHolder, row)) {

            MovieDetailsOverviewLogoPresenter.ViewHolder vh =
                    (MovieDetailsOverviewLogoPresenter.ViewHolder) viewHolder;
            vh.getParentPresenter().notifyOnBindLogo(vh.getParentViewHolder());
        }
    }
}
