package com.monsterbutt.homeview.ui.handler;

import android.app.Activity;
import android.view.View;
import android.widget.ImageView;

import com.google.android.exoplayer2.text.TextRenderer;
import com.google.android.exoplayer2.text.Cue;
import com.monsterbutt.homeview.player.HomeViewExoPlayerView;
import com.monsterbutt.homeview.player.text.PgsCue;

import java.util.List;

public class SubtitleHandler implements TextRenderer.Output {

    private int mSourceHeight = 0;
    private int mSourceWidth = 0;

    private final HomeViewExoPlayerView mVideoFrame;
    private final Activity mActvity;
    private final ImageView mSubtitlesImage;
    private CurrentVideoHandler mCurrentVideoHandler = null;

    public SubtitleHandler(Activity act, HomeViewExoPlayerView videoFrame, ImageView subtitlesImage) {

        mActvity = act;
        mVideoFrame = videoFrame;
        mSubtitlesImage = subtitlesImage;
    }

    @Override
    public void onCues(List<Cue> cues) {

        if (mActvity == null || mActvity.isFinishing() || mActvity.isDestroyed())
            return;

        final Cue cue = (cues != null && !cues.isEmpty()) ? cues.get(0) : null;
        mActvity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (cue == null || (mCurrentVideoHandler != null &&  !mCurrentVideoHandler.areSubtitlesEnabled() &&
                        (cue instanceof PgsCue && !((PgsCue)cue).isForcedSubtitle()))) {

                    mSubtitlesImage.setImageBitmap(null);
                    mSubtitlesImage.setVisibility(View.INVISIBLE);
                }
                else if ((cue instanceof PgsCue)) {

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
