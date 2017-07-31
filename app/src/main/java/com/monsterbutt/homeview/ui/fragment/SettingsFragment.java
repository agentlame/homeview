package com.monsterbutt.homeview.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.TvUtil;
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


public class SettingsFragment extends BrowseFragment implements OnItemViewSelectedListener,
 SettingPresenter.SettingsClickCallback, HomeViewActivity.OnPlayKeyListener {

    protected ArrayObjectAdapter mRowsAdapter;

    private SettingCard mCurrentCard = null;
    private ImageCardView mCurrentView = null;

    private final static String API_KEY = "api/";

    private class DependencyValue {

        DependencyValue(String key, ArrayObjectAdapter row) {
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

        setHeadersTransitionOnBackEnabled(false);
        Context context = getActivity();
        SettingsManager mgr = SettingsManager.getInstance(getActivity());
        List<SettingsManager.SettingsSection> sections = mgr.getSettingsLayout();
        int rowIndex = 0;
        SettingPresenter presenter = new SettingPresenter(this, this);
        for(SettingsManager.SettingsSection section : sections) {

            ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(presenter);
            for(SettingValue sett : section.settings) {

                if (sett.enabled()) {

                    SettingCard card = new SettingCard(context, sett);
                    String dependencyKey = sett.dependencyKey();
                    if (!TextUtils.isEmpty(dependencyKey)) {

                        if (dependencyKey.startsWith(API_KEY)) {
                            int api = Integer.valueOf(dependencyKey.substring(API_KEY.length()));
                            if (android.os.Build.VERSION.SDK_INT < api)
                                continue;
                        }
                        else {
                            DependencyValue deps = mDependencies.get(dependencyKey);
                            if (deps == null) {

                                deps = new DependencyValue(dependencyKey, gridRowAdapter);
                                mDependencies.put(dependencyKey, deps);
                            }
                            deps.list.add(card);
                        }
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
        setOnItemViewClickedListener(presenter);
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

        if (requestCode == TvUtil.MAKE_BROWSABLE_REQUEST_CODE)
            TvUtil.scheduleSyncingChannel(getContext());
    }

    @Override
    public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                               RowPresenter.ViewHolder rowViewHolder, Row row) {

        if (item instanceof SettingCard) {

            mCurrentCard = (SettingCard) item;
            mCurrentView = (ImageCardView) itemViewHolder.view;
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

    @Override
    public void onClicked(SettingCard card) {

        if (card.isBooleanSetting()) {

            SettingsFragment.DependencyValue deps = mDependencies.get(card.getKey());
            if (deps != null) {

                if (card.getContent().equals(getActivity().getString(R.string.preferences_checked_true)))
                    showDependencies(deps);
                else
                    hideDependencies(deps);

            }
        }
    }
}
