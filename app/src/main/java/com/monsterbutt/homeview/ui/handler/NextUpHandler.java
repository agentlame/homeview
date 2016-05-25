package com.monsterbutt.homeview.ui.handler;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.media.session.MediaSession;
import android.view.View;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.ui.android.NextUpView;

public class NextUpHandler {

    private final NextUpView mNextUp;
    private final int mMargin;

    public NextUpHandler(Activity activity) {

        mNextUp = (NextUpView) activity.findViewById(R.id.nextup_view);
        mMargin = activity.getResources().getDimensionPixelOffset(R.dimen.nextup_view_margin);
    }

    public void fillNextUp(MediaSession.QueueItem item) {

        mNextUp.setVideo(item);
    }
    public boolean getNextUpVisible() {

        return View.VISIBLE == mNextUp.getVisibility();
    }
    public void setNextUpVisible(boolean visible) {

        boolean currentVis = getNextUpVisible();
        if (visible && !currentVis) {

            mNextUp.setVisibility(View.VISIBLE);
            mNextUp.setAlpha(0.0f);
            mNextUp.animate()
                    .translationX(-(mMargin + mNextUp.getWidth()))
                    .alpha(1.0f);
        }
        else if(!visible && currentVis) {

            mNextUp.animate()
                    .translationX(mMargin + mNextUp.getWidth())
                    .alpha(0.0f)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            mNextUp.setVisibility(View.GONE);
                        }
                    });
        }
    }
}
