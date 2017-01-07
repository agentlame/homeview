package com.monsterbutt.homeview.ui.handler;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.media.session.MediaSession;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.WindowManager;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.ui.android.NextUpView;

public class NextUpHandler implements NextUpView.Callback {

    private final Activity mActivity;
    private final NextUpView mNextUp;
    private long mCurrentVideoDuration = 0;
    private boolean mWasDismissed = false;
    private boolean mWasSetup = false;

    private final VideoPlayerHandler mPlayerHandler;

    private long mNextUpThreshold = PlexVideoItem.NEXTUP_DISABLED;

    public NextUpHandler(Activity activity, VideoPlayerHandler playerHandler) {

        mPlayerHandler = playerHandler;
        mActivity = activity;
        mNextUp = new NextUpView(activity, this);
    }

    public void fillNextUp(long threshold, MediaSession.QueueItem item, long currentVideoDuration) {

        synchronized (this) {

            mWasDismissed = false;
            mNextUpThreshold = threshold;
            mNextUp.setVideo(item);
            mCurrentVideoDuration = currentVideoDuration;
        }
    }

    public void setCurrentVideoOffset(long offset) {

        synchronized (this) {

            if (offset < mNextUpThreshold)
                mWasDismissed = false;
            if (!mNextUp.isShowing()) {
                if (mNextUpThreshold != PlexVideoItem.NEXTUP_DISABLED && !mWasDismissed
                && offset >= mNextUpThreshold)
                    show();
            }
            else {
                final long remainingSeconds = (mCurrentVideoDuration - offset) / 1000;
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mNextUp.setSecondsLeft(remainingSeconds);
                    }
                });
            }
        }
    }

    private void show() {

        if (!mWasSetup) {
            int margin = mActivity.getResources().getDimensionPixelOffset(R.dimen.nextup_view_margin);
            WindowManager.LayoutParams dialog = mNextUp.getWindow().getAttributes();
            //dialog.gravity = Gravity.BOTTOM;
            dialog.y = margin + dialog.height;
            dialog.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
            dialog.horizontalMargin = margin;
            dialog.verticalMargin = margin;
            dialog.windowAnimations = R.style.dialog_animation;
            mNextUp.getWindow().setAttributes(dialog);
            mNextUp.setOnKeyListener(new Dialog.OnKeyListener() {

                @Override
                public boolean onKey(DialogInterface arg, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        dismiss();
                        return true;
                    }
                    return false;
                }
            });
            mWasSetup = true;
        }

        mNextUp.show();
    }

    public boolean dismiss() {

        boolean ret;
        synchronized (this) {
            ret = mNextUp.isShowing();
            if (ret) {

                mWasDismissed = true;
                mNextUp.dismiss();
            }
        }
        return ret;
    }

    public void disable() {
        synchronized (this) {
            mNextUpThreshold = PlexVideoItem.NEXTUP_DISABLED;
        }
    }

    @Override
    public void onNextUpButtonClicked(NextUpView.ButtonId id) {

        switch(id) {

            case Play:
                mPlayerHandler.enableNextTrack(true);
                mPlayerHandler.skipToNext();
                break;
            case Cancel:
                mPlayerHandler.enableNextTrack(false);
                break;
            case Browse:
                mPlayerHandler.browseNextTrackParent();
                break;
            default:
                break;
        }
        dismiss();
    }
}
