package com.monsterbutt.homeview.ui.handler;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.app.DetailsFragment;
import android.support.v17.leanback.app.SearchFragment;
import android.support.v17.leanback.app.VerticalGridFragment;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.app.ActivityOptionsCompat;
import android.view.View;
import android.widget.ImageView;

import com.monsterbutt.homeview.model.Video;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.Episode;
import com.monsterbutt.homeview.plex.media.Movie;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.presenters.CardObject;
import com.monsterbutt.homeview.presenters.CardPresenter;
import com.monsterbutt.homeview.presenters.CodecCard;
import com.monsterbutt.homeview.presenters.SceneCard;
import com.monsterbutt.homeview.ui.activity.DetailsActivity;
import com.monsterbutt.homeview.ui.activity.PlaybackActivity;
import com.monsterbutt.homeview.ui.android.HomeViewActivity;
import com.monsterbutt.homeview.ui.android.ImageCardView;

public class CardSelectionHandler extends MediaCardBackgroundHandler
                                    implements OnItemViewClickedListener, OnItemViewSelectedListener,
                                             HomeViewActivity.OnPlayKeyListener,
                                             CardPresenter.CardPresenterLongClickListener {

    public final static String key = "CardSelectionHandler";
    public interface CardSelectionListener {

        Bundle getPlaySelectionBundle(boolean cardWasScene);
    }

    private View mCurrentCardTransitionImage = null;
    private CardObject mCurrentCard = null;

    private final PlexLibraryItem mMainItem;
    private final ImageView mMainItemImage;
    private PlexServer mServer;
    private final Fragment mFragment;
    private final CardSelectionListener mCardListener;
    private final CodecCard.OnClickListenerHandler mCodecClickListener;
    private final boolean mUpdateBackgroundOnCardChange;

    public CardSelectionHandler(Fragment fragment) {
        this(fragment, null, null, null, null, null, true);
    }

    public CardSelectionHandler(Fragment fragment, PlexServer server) {
        this(fragment, null, null, server, null, null, true);
    }

    public CardSelectionHandler(Fragment fragment, CardSelectionListener cardListener, PlexServer server) {
        this(fragment, cardListener, null, server, null, null, true);
    }

    public CardSelectionHandler(Fragment fragment, CardSelectionListener cardListener,
                                 CodecCard.OnClickListenerHandler codecListener, PlexServer server,
                                 PlexLibraryItem mainItem, ImageView mainItemImage) {
        this(fragment, cardListener, codecListener, server, mainItem, mainItemImage, false);
    }

    private CardSelectionHandler(Fragment fragment, CardSelectionListener cardListener,
                                CodecCard.OnClickListenerHandler codecListener, PlexServer server,
                                PlexLibraryItem mainItem, ImageView mainItemImage,
                                boolean updateBackgroundOnCardChange) {

        super(fragment.getActivity());
        mUpdateBackgroundOnCardChange = updateBackgroundOnCardChange;
        mCodecClickListener = codecListener;
        mCardListener = cardListener;
        mServer = server;
        mMainItem = mainItem;
        mMainItemImage = mainItemImage;
        mFragment = fragment;
        if (fragment instanceof BrowseFragment) {
            ((BrowseFragment) fragment).setOnItemViewClickedListener(this);
            ((BrowseFragment) fragment).setOnItemViewSelectedListener(this);
        }
        else if (fragment instanceof VerticalGridFragment) {
            ((VerticalGridFragment) fragment).setOnItemViewClickedListener(this);
            ((VerticalGridFragment) fragment).setOnItemViewSelectedListener(this);
        }
        else if (fragment instanceof DetailsFragment) {
            ((DetailsFragment) fragment).setOnItemViewClickedListener(this);
            ((DetailsFragment) fragment).setOnItemViewSelectedListener(this);
        }
        else if (fragment instanceof SearchFragment) {

            ((SearchFragment) fragment).setOnItemViewClickedListener(this);
            ((SearchFragment) fragment).setOnItemViewSelectedListener(this);
        }
        Activity act = fragment.getActivity();
        if (act instanceof HomeViewActivity)
            ((HomeViewActivity) act).setPlayKeyListener(this);
    }

    public void setServer(PlexServer server) {

        synchronized (this) {
            mServer = server;
        }
    }

    public CardObject getSelection() {

        CardObject ret;
        synchronized (this) {
            ret = mCurrentCard;
        }
        return ret;
    }

    @Override
    public boolean longClickOccured() {
        return playKeyPressed();
    }

    @Override
    public boolean playKeyPressed() {

        boolean currIsScene = mCurrentCard != null && mCurrentCard instanceof SceneCard;
        Bundle extra = mCardListener != null ? mCardListener.getPlaySelectionBundle(currIsScene) : null;
        if (mCurrentCard == null)
            return mMainItem != null && mMainItem.onPlayPressed(mFragment, extra, mMainItemImage);
        return mCurrentCard.onPlayPressed(mFragment, extra, mCurrentCardTransitionImage);
    }

    @Override
    public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                               RowPresenter.ViewHolder rowViewHolder, Row row) {

        synchronized (this) {
            if (item instanceof CardObject) {
                synchronized (this) {
                    mCurrentCard = (CardObject) item;
                    mCurrentCardTransitionImage = ((ImageCardView) itemViewHolder.view).getMainImageView();
                    if (mUpdateBackgroundOnCardChange)
                        updateBackgroundTimed(mServer, (CardObject) item);
                }
            }
            else {
                mCurrentCard = null;
                mCurrentCardTransitionImage = null;
            }
        }
    }

    @Override
    public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                              RowPresenter.ViewHolder rowViewHolder, Row row) {

        Activity act = mFragment.getActivity();
        if (item instanceof CardObject) {

            if (item instanceof Video) {

                Video video = (Video) item;
                Class cls;
                String urlKey;
                String sharedKey;
                if (video.category != null && (video.category.equals(Movie.TYPE) || video.category.equals(Episode.TYPE))) {

                    cls = PlaybackActivity.class;
                    urlKey = PlaybackActivity.KEY;
                    sharedKey = PlaybackActivity.SHARED_ELEMENT_NAME;
                }
                else {
                    cls = DetailsActivity.class;
                    urlKey = DetailsActivity.KEY;
                    sharedKey = DetailsActivity.SHARED_ELEMENT_NAME;
                }
                Intent intent = new Intent(act, cls);
                intent.putExtra(urlKey, video.videoUrl);
                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(act, mCurrentCardTransitionImage, sharedKey).toBundle();
                act.startActivity(intent, bundle);
                act.finish();
            }
            else if (!(item instanceof CodecCard)) {

                Bundle extra = mCardListener != null ? mCardListener.getPlaySelectionBundle(item instanceof SceneCard) : null;
                ((CardObject) item).onClicked(mFragment, extra, mCurrentCardTransitionImage);
            }
            else if (mCodecClickListener != null)
                ((CodecCard) item).onCardClicked(act, mServer, mCodecClickListener);

        }
    }
}
