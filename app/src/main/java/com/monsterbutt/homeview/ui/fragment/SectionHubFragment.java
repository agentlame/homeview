package com.monsterbutt.homeview.ui.fragment;

import android.app.Activity;
import android.os.AsyncTask;
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

import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.PlexServerManager;
import com.monsterbutt.homeview.presenters.CardObject;
import com.monsterbutt.homeview.ui.MediaRowCreator;
import com.monsterbutt.homeview.ui.activity.SectionHubActivity;
import com.monsterbutt.homeview.ui.android.ImageCardView;
import com.monsterbutt.homeview.ui.android.HomeViewActivity;
import com.monsterbutt.homeview.ui.handler.MediaCardBackgroundHandler;

import java.util.List;

import us.nineworlds.plex.rest.model.impl.MediaContainer;


public class SectionHubFragment extends BrowseFragment implements HomeViewActivity.OnPlayKeyListener, OnItemViewClickedListener, OnItemViewSelectedListener {

    private PlexServer mServer;
    private MediaCardBackgroundHandler mBackgroundHandler;

    private View mCurrentCardTransitionImage = null;
    private CardObject mCurrentCard = null;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);
        ((HomeViewActivity) getActivity()).setPlayKeyListener(this);

        Activity activity = getActivity();
        mServer = PlexServerManager.getInstance(activity.getApplicationContext()).getSelectedServer();
        mBackgroundHandler = new MediaCardBackgroundHandler(activity);
        setOnItemViewClickedListener(this);
        setOnItemViewSelectedListener(this);
        ((HomeViewActivity)activity).setPlayKeyListener(this);

        TextView text = (TextView) getActivity().findViewById(android.support.v17.leanback.R.id.title_text);
        text.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
        setTitle(activity.getIntent().getStringExtra(SectionHubActivity.TITLE));
        new LoadMetadataTask().execute(activity.getIntent().getStringExtra(SectionHubActivity.SECTIONID));
    }

    @Override
    public void onStop() {

        super.onStop();
        mBackgroundHandler.cancel();
    }

    @Override
    public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                               RowPresenter.ViewHolder rowViewHolder, Row row) {

        if (item instanceof CardObject) {
            mCurrentCard = (CardObject) item;
            mCurrentCardTransitionImage = ((ImageCardView) itemViewHolder.view).getMainImageView();
            mBackgroundHandler.updateBackgroundTimed(mServer, (CardObject) item);
        }
    }

    @Override
    public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                              RowPresenter.ViewHolder rowViewHolder, Row row) {

        if (item instanceof CardObject)
            ((CardObject) item).onClicked(this, mCurrentCardTransitionImage);
    }

    @Override
    public boolean playKeyPressed() {

        return  mCurrentCard != null && mCurrentCard.onPlayPressed(this, mCurrentCardTransitionImage);
    }

    private class LoadMetadataTask extends AsyncTask<String, Void, MediaContainer> {

        @Override
        protected MediaContainer doInBackground(String... params) {

            return  mServer.getHubForSection(params[0]);
        }

        @Override
        protected void onPostExecute(MediaContainer item) {

            ArrayObjectAdapter rowAdapter = new ArrayObjectAdapter(new ListRowPresenter());
            setAdapter(rowAdapter);
            if (item != null && item.getHubs() != null) {

                List<MediaRowCreator.MediaRow> rows = MediaRowCreator.buildRowList(null, item);
                for (MediaRowCreator.MediaRow row : rows) {

                    ArrayObjectAdapter adapter = MediaRowCreator.fillAdapterForRow(getActivity(),
                            mServer, row, false);
                    rowAdapter.add(new ListRow(new HeaderItem(row.title),adapter));
                }
            }
        }
    }
}
