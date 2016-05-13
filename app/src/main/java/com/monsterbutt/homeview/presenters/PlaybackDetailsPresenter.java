package com.monsterbutt.homeview.presenters;

import android.support.v17.leanback.app.PlaybackControlGlue;
import android.support.v17.leanback.widget.Presenter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.monsterbutt.homeview.R;

public class PlaybackDetailsPresenter extends Presenter {

    public static class ViewHolder extends Presenter.ViewHolder {

        public TextView getTitle() { return mTitle; }
        public TextView getSubtitle() { return mSubtitle; }
        public TextView getBody() { return mBody; }

        private final TextView mTitle;
        private final TextView mSubtitle;
        private final TextView mBody;

        public ViewHolder(final View view) {
            super(view);
            mTitle = (TextView) view.findViewById(android.support.v17.leanback.R.id.lb_details_description_title);
            mSubtitle = (TextView) view.findViewById(android.support.v17.leanback.R.id.lb_details_description_subtitle);
            mBody = (TextView) view.findViewById(android.support.v17.leanback.R.id.lb_details_description_body);
        }
    }

    protected void onBindDescription(PlaybackDetailsPresenter.ViewHolder viewHolder, Object object) {
        PlaybackControlGlue glue = (PlaybackControlGlue) object;
        if (glue.hasValidMedia()) {
            viewHolder.getTitle().setText(glue.getMediaTitle());
            viewHolder.getSubtitle().setText(glue.getMediaSubtitle());
        } else {
            viewHolder.getTitle().setText("");
            viewHolder.getSubtitle().setText("");
        }
    }

    @Override
    public final ViewHolder onCreateViewHolder(ViewGroup parent) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.lb_playback_details, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public final void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        ViewHolder vh = (ViewHolder) viewHolder;
        onBindDescription(vh, item);

        boolean hasTitle = true;
        if (TextUtils.isEmpty(vh.mTitle.getText())) {
            vh.mTitle.setVisibility(View.GONE);
            hasTitle = false;
        } else {
            vh.mTitle.setVisibility(View.VISIBLE);
        }

        boolean hasSubtitle = true;
        if (TextUtils.isEmpty(vh.mSubtitle.getText())) {
            vh.mSubtitle.setVisibility(View.GONE);
            hasSubtitle = false;
        } else {
            vh.mSubtitle.setVisibility(View.VISIBLE);
        }

        if (TextUtils.isEmpty(vh.mBody.getText())) {
            vh.mBody.setVisibility(View.GONE);
        } else {
            vh.mBody.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {}

}
