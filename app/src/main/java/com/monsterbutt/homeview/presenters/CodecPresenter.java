package com.monsterbutt.homeview.presenters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.widget.Presenter;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.Stream;
import com.monsterbutt.homeview.ui.android.CodecCardView;


public class CodecPresenter extends Presenter {

    PlexServer server;
    public CodecPresenter(PlexServer server) {
        this.server = server;
    }

    private int mSelectedBackgroundColor = -1;
    private int mDefaultBackgroundColor = -1;

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = parent.getContext().getTheme();
        theme.resolveAttribute(R.attr.card_normal, typedValue, true);
        mDefaultBackgroundColor = typedValue.data;
        theme.resolveAttribute(R.attr.card_selected, typedValue, true);
        mSelectedBackgroundColor = typedValue.data;

        CodecCardView cardView = new CodecCardView(parent.getContext()) {
            @Override
            public void setSelected(boolean selected) {
                updateCardBackgroundColor(this, selected);
                super.setSelected(selected);
            }
        };

        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        updateCardBackgroundColor(cardView, false);
        return new Presenter.ViewHolder(cardView);
    }

    private void updateCardBackgroundColor(CodecCardView view, boolean selected) {
        int color = selected ? mSelectedBackgroundColor : mDefaultBackgroundColor;

        // Both background colors should be set because the view's
        // background is temporarily visible during animations.
        view.setBackgroundColor(color);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {

        CodecCard obj = (CodecCard) item;
        final CodecCardView cardView = (CodecCardView) viewHolder.view;
        Context context = cardView.getContext();

        cardView.setDecoderText(obj.getDecoder());
        cardView.setTitleText(obj.getTitle());
        if (!TextUtils.isEmpty(obj.getContent()))
            cardView.setSubtitleText(obj.getContent());

        Drawable a = obj.getImage(context);
        if (a != null)
            cardView.setMainImage(a, true);
        else {
            String url = obj.getImageUrl(server);
            if (!TextUtils.isEmpty(url)) {

                Glide.with(context)
                        .load(url)
                        .into(cardView.getMainImageView());
            }
            else
                cardView.setMainImage(null);
        }

        Drawable b = obj.getImageSecondary();
        if (b != null)
            cardView.setSecondaryImage(b, true);
        else {
            String url = obj.getImageUrlSecondary(server);
            if (!TextUtils.isEmpty(url)) {

                Glide.with(context)
                        .load(url)
                        .into(cardView.getSecondaryImageView());
            }
            else
                cardView.setSecondaryImage(null);
        }

        int trackCount = obj.getTotalTracksForType();
        if (obj.getTrackType() == Stream.Subtitle_Stream && 1 < trackCount) {

            --trackCount;
            if (trackCount == 1)
                cardView.setFlag(context.getDrawable(R.drawable.right_flag), null);
        }
        if (trackCount > 1)
            cardView.setFlag(context.getDrawable(R.drawable.right_flag), Integer.toString(trackCount));
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {

    }
}
