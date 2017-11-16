package com.monsterbutt.homeview.ui;

import android.annotation.SuppressLint;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;


@SuppressLint("Registered")
public class HomeViewActivity extends FragmentActivity {

    OnBackPressedListener mBackListener = null;
    OnPlayKeyListener mPlayListener = null;

    public interface OnBackPressedListener {

        boolean backPressed();
    }
    public void setBackPressedListener(OnBackPressedListener listener) { mBackListener = listener; }
    public interface OnPlayKeyListener {

        boolean playKeyPressed();
    }
    public void setPlayKeyListener(OnPlayKeyListener listener) {
        mPlayListener = listener;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY
                ||  keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {

            if (mPlayListener != null) {

                if (mPlayListener.playKeyPressed())
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {

        if (mBackListener == null || !mBackListener.backPressed())
            super.onBackPressed();
    }
}
