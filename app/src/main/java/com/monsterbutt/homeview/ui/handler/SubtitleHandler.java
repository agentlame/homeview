package com.monsterbutt.homeview.ui.handler;

import android.app.Activity;
import android.graphics.Point;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.exoplayer.AspectRatioFrameLayout;
import com.google.android.exoplayer.text.Cue;
import com.monsterbutt.homeview.player.VideoPlayer;
import com.monsterbutt.homeview.player.text.PgsCue;

import java.util.List;

public class SubtitleHandler implements VideoPlayer.CaptionListener {

    private int mSourceHeight = 0;
    private int mSourceWidth = 0;

    private final AspectRatioFrameLayout mVideoFrame;
    private final Activity mActvity;
    private final ImageView mSubtitlesImage;
    private final TextView mSubtitlesText;
    private CurrentVideoHandler mCurrentVideoHandler = null;

    public SubtitleHandler(Activity act, AspectRatioFrameLayout videoFrame,
                           ImageView subtitlesImage, TextView subtitlesText) {

        mActvity = act;
        mVideoFrame = videoFrame;
        mSubtitlesImage = subtitlesImage;
        mSubtitlesText = subtitlesText;
        if (mSubtitlesText != null) {

            Display display = act.getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int width = size.x;
            int height = size.y;
            int subtextX = (int) ((double) width * 0.20);
            int subtextY = (int) ((double) height * 0.80);
            mSubtitlesText.setX(subtextX);
            mSubtitlesText.setWidth(width - (2 * subtextX));
            mSubtitlesText.setY(subtextY);
        }
    }

    @Override
    public void onCues(List<Cue> cues) {

        if (mActvity == null || mActvity.isFinishing() || mActvity.isDestroyed())
            return;

        final Cue cue = (cues != null && !cues.isEmpty()) ? cues.get(0) : null;
        mActvity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // if no cue... duh
                // or if cue is not PGS (we only handle that currently)
                // or subs are turned off but we are checking for forced subs and it isn't forced
                if (cue == null || (mCurrentVideoHandler != null &&  !mCurrentVideoHandler.areSubtitlesEnabled() &&
                        (cue instanceof PgsCue && !((PgsCue)cue).isForcedSubtitle()))) {

                    mSubtitlesText.setText("");
                    mSubtitlesText.setVisibility(View.INVISIBLE);
                    mSubtitlesImage.setImageBitmap(null);
                    mSubtitlesImage.setVisibility(View.INVISIBLE);
                }
                else if (!(cue instanceof PgsCue)) {

                    mSubtitlesText.setText(cue.text);
                    mSubtitlesText.setVisibility(View.VISIBLE);
                }
                // pgs sub and subs are on or it is forced
                else {
                    ((PgsCue)cue).updateParams((int) mVideoFrame.getX(), (int) mVideoFrame.getY(),
                            mVideoFrame.getWidth(), mVideoFrame.getHeight(), mSourceWidth, mSourceHeight,
                            mSubtitlesImage);
                }
            }
        });
    }

    public void setSourceStatus(Integer height, Integer width) {

        mSourceHeight = height;
        mSourceWidth = width;
    }

    public void setHandler(CurrentVideoHandler currentVideoHandler) { mCurrentVideoHandler = currentVideoHandler; }
}
