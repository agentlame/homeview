package com.monsterbutt.homeview.ui;

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
import com.monsterbutt.homeview.player.notifier.ChapterSelectionNotifier;
import com.monsterbutt.homeview.plex.media.Chapter;
import com.monsterbutt.homeview.plex.media.Episode;
import com.monsterbutt.homeview.plex.media.Movie;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.ui.presenters.CardObject;
import com.monsterbutt.homeview.ui.presenters.CardPresenter;
import com.monsterbutt.homeview.ui.presenters.CodecCard;
import com.monsterbutt.homeview.ui.presenters.PosterCard;
import com.monsterbutt.homeview.ui.presenters.SceneCard;
import com.monsterbutt.homeview.ui.details.DetailsActivity;
import com.monsterbutt.homeview.ui.playback.PlaybackActivity;
import com.monsterbutt.homeview.ui.interfaces.ICardSelectionListener;

public class SelectionHandler
 implements OnItemViewClickedListener, OnItemViewSelectedListener, HomeViewActivity.OnPlayKeyListener {


    private View mCurrentCardTransitionImage = null;
    private CardObject mCurrentCard = null;

    private final PlexLibraryItem mMainItem;
    private final ImageView mMainItemImage;
    private final Fragment mFragment;
    private final ICardSelectionListener mCardListener;

    private final BackgroundHandler backgroundHandler;
    private final ChapterSelectionNotifier mChapterClickListener;


    public SelectionHandler(Fragment fragment) {
        this(fragment, null, null);
    }

    public SelectionHandler(Fragment fragment, BackgroundHandler backgroundHandler) {
        this(fragment, null, null, null, null, backgroundHandler);
    }

    public SelectionHandler(Fragment fragment, ICardSelectionListener cardListener) {
        this(fragment, cardListener, null, null);
    }

    public SelectionHandler(Fragment fragment, ICardSelectionListener cardListener,
                            BackgroundHandler backgroundHandler) {
        this(fragment, cardListener, null, null, null, backgroundHandler);
    }

    public SelectionHandler(Fragment fragment, ICardSelectionListener cardListener,
                            PlexLibraryItem mainItem, ImageView mainItemImage) {
        this(fragment, cardListener, null, mainItem, mainItemImage, null);
    }

    private SelectionHandler(Fragment fragment, ICardSelectionListener cardListener,
                             ChapterSelectionNotifier chapterListener,
                             PlexLibraryItem mainItem, ImageView mainItemImage,
                             BackgroundHandler backgroundHandler) {

        this.backgroundHandler = backgroundHandler;
        mCardListener = cardListener;
        mChapterClickListener = chapterListener;
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

    public CardObject getSelection() {

        CardObject ret;
        synchronized (this) {
            ret = mCurrentCard;
        }
        return ret;
    }

    public boolean longClickOccured(CardObject obj, CardPresenter.LongClickWatchStatusCallback callback) {

        if (obj != null) {
            if (obj instanceof SceneCard && ((SceneCard) obj).getItem() instanceof Chapter)
                return playKeyPressed();
            else if (obj instanceof PosterCard) {

                boolean currIsScene = obj instanceof SceneCard;
                Bundle extra = mCardListener != null ? mCardListener.getPlaySelectionBundle(currIsScene) : null;
                return obj.onLongClicked(mFragment, extra,mCurrentCardTransitionImage, callback);
            }
        }
        return false;
    }

    @Override
    public boolean playKeyPressed() {

        boolean currIsScene = mCurrentCard != null && mCurrentCard instanceof SceneCard;
        Bundle extra = mCardListener != null ? mCardListener.getPlaySelectionBundle(currIsScene) : null;
        if (mCurrentCard == null)
            return mMainItem != null && mMainItem.onPlayPressed(mFragment, extra, mMainItemImage);
        else if (mCurrentCard instanceof SceneCard && ((SceneCard) mCurrentCard).getItem() instanceof Chapter && mChapterClickListener != null)
            mChapterClickListener.chapterSelected((Chapter) ((SceneCard)mCurrentCard).getItem());
        return mCurrentCard.onPlayPressed(mFragment, extra, mCurrentCardTransitionImage);
    }

    @Override
    public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                               RowPresenter.ViewHolder rowViewHolder, Row row) {
        synchronized (this) {
            if (item instanceof CardObject) {
                synchronized (this) {
                    mCurrentCard = (CardObject) item;
                    if(mCardListener != null)
                        mCardListener.onCardSelected(mCurrentCard);
                    if (itemViewHolder.view instanceof ImageCardView)
                        mCurrentCardTransitionImage = ((ImageCardView) itemViewHolder.view).getMainImageView();
                    else
                        mCurrentCardTransitionImage = null;
                    if (backgroundHandler != null)
                        backgroundHandler.updateBackgroundTimed((CardObject) item);
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
        onItemClicked(item);
    }

    public void onItemClicked(Object item) {

        Activity act = mFragment.getActivity();
        if (item instanceof CardObject) {

            if (item instanceof Video) {

                Video video = (Video) item;
                Class cls;
                String urlKey;
                String sharedKey;
                String action = "";
                if (video.category != null && (video.category.equals(Movie.TYPE) || video.category.equals(Episode.TYPE))) {

                    cls = PlaybackActivity.class;
                    action = PlaybackActivity.ACTION_VIEW;
                    urlKey = PlaybackActivity.KEY;
                    sharedKey = PlaybackActivity.SHARED_ELEMENT_NAME;
                }
                else {
                    cls = DetailsActivity.class;
                    urlKey = C.KEY;
                    sharedKey = DetailsActivity.SHARED_ELEMENT_NAME;
                }
                Intent intent = new Intent(act, cls);
                intent.setAction(action);
                intent.putExtra(urlKey, video.videoUrl);
                intent.putExtra(C.TYPE, video.category);
                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(act, mCurrentCardTransitionImage, sharedKey).toBundle();
                act.startActivity(intent, bundle);
                act.finish();
            }
            else if (!(item instanceof CodecCard)) {

                if (item instanceof SceneCard && ((SceneCard) item).getItem() instanceof Chapter && mChapterClickListener != null)
                    mChapterClickListener.chapterSelected((Chapter) ((SceneCard)item).getItem());
                else {
                    Bundle extra = mCardListener != null ? mCardListener.getPlaySelectionBundle(item instanceof SceneCard) : null;
                    ((CardObject) item).onClicked(mFragment, extra, mCurrentCardTransitionImage);
                }
            }
        }
    }
}
