package com.monsterbutt.homeview.player;

import android.content.Context;
import android.content.Intent;

import com.monsterbutt.homeview.settings.SettingsManager;
import com.monsterbutt.homeview.ui.activity.PlaybackActivity;

public class StartPosition {

    private boolean mWasReset = false;
    private long mStartPosition = 0;
    private final PlaybackStartType mPlaybackStartType;
    private long mVideoOffset;

    public StartPosition(Context context, Intent intent, long videoOffset) {

        mVideoOffset = videoOffset;
        mStartPosition = intent.getLongExtra(PlaybackActivity.START_OFFSET, -1);

        String startKey = SettingsManager.getInstance(context).getString("preferences_playback_resume");
        switch(startKey) {
            case "ask":
                mPlaybackStartType = PlaybackStartType.Ask;
                break;
            case "resume":
                mPlaybackStartType = PlaybackStartType.Resume;
                break;
            default:
                mPlaybackStartType = PlaybackStartType.Begining;
                break;
        }
    }

    public PlaybackStartType getStartType() {

        if (mStartPosition >= 0)
            return PlaybackStartType.Resume;
        if (mVideoOffset == 0)
            return PlaybackStartType.Begining;
        return mPlaybackStartType;
    }

    public long getVideoOffset() {
        return mVideoOffset;
    }

    public long getStartPosition() {

        if (mStartPosition >= 0 && !mWasReset)
            return mStartPosition;
        switch(mPlaybackStartType) {

            case Resume:
                return mVideoOffset;
            case Begining:
            case Ask:
            default:
                return 0;
        }
    }

    public void reset(long videoOffset) {

        mWasReset = true;
        setVideoOffset(videoOffset);
    }

    public void setVideoOffset(long videoOffset) {
        mVideoOffset = videoOffset;
    }

    public enum PlaybackStartType {

        Resume,
        Begining,
        Ask
    }
}
