package com.monsterbutt.homeview.ui.details.presenters;


import android.support.v17.leanback.widget.DetailsOverviewLogoPresenter;
import android.support.v17.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import android.support.v17.leanback.widget.Presenter;

import com.monsterbutt.homeview.R;

public class DetailPresenter extends FullWidthDetailsOverviewRowPresenter {

    @Override
    protected int getLayoutResourceId() {
        return R.layout.lb_fullwidth_details_overview;
    }

    public DetailPresenter(Presenter detailsPresenter, DetailsOverviewLogoPresenter logoPresenter) {
        super(detailsPresenter, logoPresenter);
    }
}
