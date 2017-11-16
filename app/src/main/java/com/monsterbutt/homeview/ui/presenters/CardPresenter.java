/*
 * Copyright (c) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.monsterbutt.homeview.ui.presenters;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.widget.Presenter;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.ui.ImageCardView;
import com.monsterbutt.homeview.ui.SelectionHandler;

/*
 * A CardPresenter is used to generate Views and bind Objects to them on demand.
 * It contains an Image CardView
 */
public class CardPresenter extends Presenter {

    public interface LongClickWatchStatusCallback {
        void resetSelected(CardObject card);
        void removeSelected(CardObject card);
    }

    private int mSelectedBackgroundColor = -1;
    private int mDefaultBackgroundColor = -1;

    final private PlexServer mPlex;
    final private SelectionHandler mListener;
    private LongClickWatchStatusCallback mLongClickWatchStatusCallback;
    final private boolean posterOnly;

    public CardPresenter(PlexServer plex, SelectionHandler listener, boolean posterOnly) {

        mPlex = plex;
        mListener = listener;
        mLongClickWatchStatusCallback = null;
        this.posterOnly = posterOnly;
    }

    public void setLongClickWatchStatusCallback(LongClickWatchStatusCallback longClickWatchStatusCallback) {
        mLongClickWatchStatusCallback = longClickWatchStatusCallback;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {

        ImageCardView cardView;
        if (!posterOnly) {
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = parent.getContext().getTheme();
            theme.resolveAttribute(R.attr.card_normal, typedValue, true);
            mDefaultBackgroundColor = typedValue.data;
            theme.resolveAttribute(R.attr.card_selected, typedValue, true);
            mSelectedBackgroundColor = typedValue.data;

            cardView = new ImageCardView(parent.getContext(), false) {
                @Override
                public void setSelected(boolean selected) {
                    updateCardBackgroundColor(this, selected);
                    super.setSelected(selected);
                }
            };

            updateCardBackgroundColor(cardView, false);
        }
        else {

            cardView = new ImageCardView(parent.getContext(), true);
        }
        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);

        return new ViewHolder(cardView);
    }

    private void updateCardBackgroundColor(ImageCardView view, boolean selected) {
        int color = selected ? mSelectedBackgroundColor : mDefaultBackgroundColor;

        // Both background colors should be set because the view's
        // background is temporarily visible during animations.
        view.setBackgroundColor(color);
        view.findViewById(R.id.info_field).setBackgroundColor(color);
        view.findViewById(R.id.card_progress).setBackgroundColor(color);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {

        boolean isExpanded = (item instanceof PosterCardExpanded || item instanceof SceneCardExpanded);
        final CardObject obj = (CardObject) item;
        final ImageCardView cardView = (ImageCardView) viewHolder.view;
        if (mListener != null) {
            cardView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    return mListener.longClickOccured(obj, mLongClickWatchStatusCallback);
                }
            });
            cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onItemClicked(obj);
                }
            });
        }
        Context context = cardView.getContext();
        if (!isExpanded) {
            cardView.setTitleText(obj.getTitle());
            cardView.setContentText(obj.getContent());
        }
        if(obj instanceof PosterCard) {
            cardView.setEpisode((obj instanceof SceneCard) ? "" : ((PosterCard) obj).getSeason(),
             ((PosterCard) obj).getEpisode());
        }
        final String imageURL = obj.getImageUrl(mPlex);
            // Set card size from dimension resources.
        final Resources res = cardView.getResources();
        int width = res.getDimensionPixelSize(obj.getWidth());
        int height = res.getDimensionPixelSize(obj.getHeight());
        cardView.setMainImageDimensions(width, height);
        if (obj.getWatchedState() != PlexLibraryItem.WatchedState.Watched) {

            String unwatched = "";
            int unwatchedCount = obj.getUnwatchedCount();
            if (unwatchedCount > 0)
                unwatched = Integer.toString(unwatchedCount);
            cardView.setFlag(context.getDrawable(R.drawable.right_flag), unwatched);
        }
        else
            cardView.setFlag(null, "");

        cardView.setProgress(obj.getViewedProgress() < 100 ? obj.getViewedProgress() : 0);

        Drawable drawable = obj.getImage(context);
        if (drawable != null)
            cardView.setMainImage(null, drawable);
        else if (TextUtils.isEmpty(imageURL))
            cardView.setMainImage(null, context.getDrawable(R.drawable.default_background));
        else {
            if (cardView.shouldUpdateMain(imageURL)) {
                cardView.setTarget(new SimpleTarget<GlideDrawable>(width, height) {

                    @Override
                    public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                        cardView.setMainImage(imageURL, resource);
                        cardView.setTarget(null);
                    }
                });

                if (context instanceof Activity) {
                    Activity act = (Activity) context;
                    if (act.isFinishing() || act.isDestroyed())
                        return;
                }

                Glide.with(context)
                 .load(imageURL)
                 .into(cardView.getTarget());
            }
        }
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        ImageCardView cardView = (ImageCardView) viewHolder.view;

        SimpleTarget<GlideDrawable> target = cardView.getTarget();
        if (target != null) {

            Glide.clear(target);
            cardView.setTarget(null);
        }
        // Remove references to images so that the garbage collector can free up memory.
        // or we could not forever lose our images and not have them reload...
        cardView.setBadgeImage(null);
        cardView.setMainImage(null, null);
        cardView.setFlag(null, "");
        cardView.setProgress(0);
    }
}
