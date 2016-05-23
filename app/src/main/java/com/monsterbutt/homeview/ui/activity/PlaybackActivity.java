/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.monsterbutt.homeview.ui.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.graphics.Color;
import android.media.session.MediaSession;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.ui.android.NextUpView;
import com.monsterbutt.homeview.ui.android.HomeViewActivity;

/**
 * PlaybackOverlayActivity for video playback that loads PlaybackOverlayFragment
 */
public class PlaybackActivity extends HomeViewActivity {

    public static final String KEY = "key";
    public static final String START_OFFSET = "startoffset";
    public static final String VIDEO = "video";
    public static final String TRACKS = "tracks";
    public static final String FILTER = "filter";
    public static final String SHARED_ELEMENT_NAME = "hero";

    private NextUpView mNextUp = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_playback);
        getWindow().getDecorView().setBackgroundColor(Color.BLACK);
        mNextUp = (NextUpView) findViewById(R.id.nextup_view);
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public void onVisibleBehindCanceled() {
        getMediaController().getTransportControls().pause();
        super.onVisibleBehindCanceled();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BUTTON_R1) {
            getMediaController().getTransportControls().skipToNext();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BUTTON_L1) {
            getMediaController().getTransportControls().skipToPrevious();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    public void fillNextUp(MediaSession.QueueItem item) {

        mNextUp.setVideo(item);
    }
    public boolean getNextUpVisible() {

        return View.VISIBLE == mNextUp.getVisibility();
    }
    public void setNextUpVisible(boolean visible) {

        int margin = getResources().getDimensionPixelOffset(R.dimen.nextup_view_margin);
        boolean currentVis = getNextUpVisible();
        if (visible && !currentVis) {

            mNextUp.setVisibility(View.VISIBLE);
            mNextUp.setAlpha(0.0f);
            mNextUp.animate()
                    .translationX(-(margin + mNextUp.getWidth()))
                    .alpha(1.0f);
        }
        else if(!visible && currentVis) {

            mNextUp.animate()
                    .translationX(margin + mNextUp.getWidth())
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
