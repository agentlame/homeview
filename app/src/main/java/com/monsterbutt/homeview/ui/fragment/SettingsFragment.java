package com.monsterbutt.homeview.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.view.View;
import android.widget.TextView;

import com.monsterbutt.homeview.presenters.CardObject;
import com.monsterbutt.homeview.presenters.SettingCard;
import com.monsterbutt.homeview.presenters.SettingPresenter;
import com.monsterbutt.homeview.settings.SettingsManager;
import com.monsterbutt.homeview.settings.SettingValue;
import com.monsterbutt.homeview.ui.android.ImageCardView;
import com.monsterbutt.homeview.ui.android.HomeViewActivity;

import java.util.List;


public class SettingsFragment extends BrowseFragment implements OnItemViewClickedListener, OnItemViewSelectedListener, HomeViewActivity.OnPlayKeyListener {

    protected ArrayObjectAdapter mRowsAdapter;

    private SettingCard mCurrentCard = null;
    private ImageCardView mCurrentView = null;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);
        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());

        Context context = getActivity();
        SettingsManager mgr = SettingsManager.getInstance(getActivity());
        List<SettingsManager.SettingsSection> sections = mgr.getSettingsLayout();
        int rowIndex = 0;
        for(SettingsManager.SettingsSection section : sections) {

            ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(new SettingPresenter());
            for(SettingValue sett : section.settings) {

                if (sett.enabled())
                    gridRowAdapter.add(new SettingCard(context, sett));
            }
            mRowsAdapter.add(new ListRow(new HeaderItem(rowIndex++, section.title), gridRowAdapter));
        }

        TextView text = (TextView) getActivity().findViewById(android.support.v17.leanback.R.id.title_text);
        text.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
        setTitle(mgr.title());

        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);
        setOnItemViewClickedListener(this);
        setOnItemViewSelectedListener(this);
        ((HomeViewActivity) getActivity()).setPlayKeyListener(this);

        setAdapter(mRowsAdapter);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (mCurrentCard != null) {
            SettingsManager mgr = SettingsManager.getInstance(getActivity());
            mCurrentCard.updateValue(mgr.getSetting(mCurrentCard.getKey()).reload(getActivity()), mCurrentView);
        }
    }

    @Override
    public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                               RowPresenter.ViewHolder rowViewHolder, Row row) {

        if (item instanceof SettingCard) {

            mCurrentCard = (SettingCard) item;
            mCurrentView = (ImageCardView) itemViewHolder.view;
        }
    }

    @Override
    public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                              RowPresenter.ViewHolder rowViewHolder, Row row) {

        if (item instanceof CardObject) {
            if (((CardObject) item).onClicked(this, null, null)) {
                ((ImageCardView) itemViewHolder.view).setContentText(((CardObject) item).getContent());
            }
        }
    }

    @Override
    public boolean playKeyPressed() {

        return (mCurrentCard != null && mCurrentCard.onPlayPressed(this, null, null));
    }
}
