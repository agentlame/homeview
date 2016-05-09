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
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.presenters.CardObject;
import com.monsterbutt.homeview.presenters.SettingCard;
import com.monsterbutt.homeview.presenters.SettingPresenter;
import com.monsterbutt.homeview.settings.SettingsManager;
import com.monsterbutt.homeview.settings.SettingValue;
import com.monsterbutt.homeview.ui.android.ImageCardView;
import com.monsterbutt.homeview.ui.android.HomeViewActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SettingsFragment extends BrowseFragment implements OnItemViewClickedListener, OnItemViewSelectedListener, HomeViewActivity.OnPlayKeyListener {

    protected ArrayObjectAdapter mRowsAdapter;

    private SettingCard mCurrentCard = null;
    private ImageCardView mCurrentView = null;

    private class DependencyValue {

        public DependencyValue(String key, ArrayObjectAdapter row) {
            this.key = key;
            this.row = row;
        }

        public final String key;
        public final ArrayObjectAdapter row;
        public final List<SettingCard> list = new ArrayList<>();
    }


    private Map<String, DependencyValue> mDependencies = new HashMap<>();

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

                if (sett.enabled()) {

                    SettingCard card = new SettingCard(context, sett);
                    String dependencyKey = sett.dependencyKey();
                    if (!TextUtils.isEmpty(dependencyKey)) {

                        DependencyValue deps = mDependencies.get(dependencyKey);
                        if (deps == null) {

                            deps = new DependencyValue(dependencyKey, gridRowAdapter);
                            mDependencies.put(dependencyKey, deps);
                        }
                        deps.list.add(card);
                    }
                    gridRowAdapter.add(card);
                }
            }
            mRowsAdapter.add(new ListRow(new HeaderItem(rowIndex++, section.title), gridRowAdapter));
        }

        hideDependenciesForOffKeys();

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

                if (item instanceof SettingCard && ((SettingCard)item).isBooleanSetting()) {

                    SettingCard card = (SettingCard) item;
                    DependencyValue deps = mDependencies.get(card.getKey());
                    if (deps != null) {

                        if (card.getContent().equals(getActivity().getString(R.string.preferences_checked_true)))
                            showDependencies(deps);
                        else
                            hideDependencies(deps);

                    }
                }
            }
        }
    }

    private final int NOT_FOUND = -1;
    private int findKeyIndexInRow(String key, ArrayObjectAdapter row) {

        for (int rowIndex = 0; rowIndex < row.size(); ++rowIndex) {

            Object r = row.get(rowIndex);
            if (! (r instanceof SettingCard))
                continue;
            SettingCard card = (SettingCard) r;
            if (card.getKey().equals(key))
                return rowIndex;
        }
        return NOT_FOUND;
    }

    private void showDependencies(DependencyValue deps) {

        int index = findKeyIndexInRow(deps.key, deps.row);
        if (index != NOT_FOUND) {

            // shift to next spot
            ++index;
            for (SettingCard card : deps.list)
                deps.row.add(index++, card);
        }
    }

    private void hideDependencies(DependencyValue deps) {

        for (SettingCard card : deps.list)
            deps.row.remove(card);
    }

    private void hideDependenciesForOffKeys() {

        for(DependencyValue deps : mDependencies.values()) {

            int index = findKeyIndexInRow(deps.key, deps.row);
            if (NOT_FOUND != index) {

                SettingCard keyCard = (SettingCard) deps.row.get(index);
                if (!keyCard.getContent().equals(getActivity().getString(R.string.preferences_checked_true)))
                    hideDependencies(deps);
            }
        }
    }

    @Override
    public boolean playKeyPressed() {

        return (mCurrentCard != null && mCurrentCard.onPlayPressed(this, null, null));
    }
}
