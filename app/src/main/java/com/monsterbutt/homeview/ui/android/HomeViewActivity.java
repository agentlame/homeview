package com.monsterbutt.homeview.ui.android;

import android.app.Activity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ProgressBar;

import com.monsterbutt.homeview.R;


public class HomeViewActivity extends Activity {

    OnBackPressedListener mBackListener = null;
    OnPlayKeyListener mPlayListener = null;
    OnStopKeyListener mStopListener = null;



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

    public interface OnStopKeyListener {

        boolean stopKeyPressed();
    }
    public void setStopKeyListner(OnStopKeyListener listener) { mStopListener = listener; }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY
                ||  keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {

            if (mPlayListener != null) {

                if (mPlayListener.playKeyPressed())
                    return true;
            }
        }
        else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP) {

            if (mStopListener != null) {

                if (mStopListener.stopKeyPressed())
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
