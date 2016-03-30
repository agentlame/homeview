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

package com.monsterbutt.homeview.presenters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.widget.Presenter;
import android.util.TypedValue;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.ui.android.ImageCardView;

/*
 * A CardPresenter is used to generate Views and bind Objects to them on demand.
 * It contains an Image CardView
 */
public class CardPresenter extends Presenter {

    private int mSelectedBackgroundColor = -1;
    private int mDefaultBackgroundColor = -1;

    private PlexServer mPlex = null;

    public CardPresenter(PlexServer plex) {

        mPlex = plex;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = parent.getContext().getTheme();
        theme.resolveAttribute(R.attr.card_normal, typedValue, true);
        mDefaultBackgroundColor = typedValue.data;
        theme.resolveAttribute(R.attr.card_selected, typedValue, true);
        mSelectedBackgroundColor = typedValue.data;

        ImageCardView cardView = new ImageCardView(parent.getContext()) {
            @Override
            public void setSelected(boolean selected) {
                updateCardBackgroundColor(this, selected);
                super.setSelected(selected);
            }
        };

        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        updateCardBackgroundColor(cardView, false);
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

        CardObject obj = (CardObject) item;

        final ImageCardView cardView = (ImageCardView) viewHolder.view;
        Context context = cardView.getContext();
        cardView.setTitleText(obj.getTitle());
        cardView.setContentText(obj.getContent());

        String imageURL = obj.getImageUrl(mPlex);
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

        if (obj.getViewedProgress() > 0)
            cardView.setProgress(obj.getViewedProgress());

        Drawable drawable = obj.getImage(context);
        if (drawable != null)
            cardView.setMainImage(drawable, true);
        else {

            cardView.setTarget(new SimpleTarget<GlideDrawable>(width, height) {

                @Override
                public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                    cardView.setMainImage(resource, true);
                    cardView.setTarget(null);
                }
            });

            Glide.with(context)
                    .load(imageURL)
                    .into(cardView.getTarget());
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
        cardView.setMainImage(null);
        cardView.setFlag(null, "");
    }
}
