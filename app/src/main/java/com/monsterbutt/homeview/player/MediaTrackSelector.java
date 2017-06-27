package com.monsterbutt.homeview.player;


import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.widget.ArrayAdapter;

import com.google.android.exoplayer2.C;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.Stream;

import java.util.List;

public class MediaTrackSelector implements Parcelable {

    private MediaTracks mTracks = null;
    private final String baseLangCode;

    public MediaTrackSelector(List<us.nineworlds.plex.rest.model.impl.Stream> streams,
                              String baseLangCode, MediaCodecCapabilities capabilities) {

        this.baseLangCode = TextUtils.isEmpty(baseLangCode) ? "" : baseLangCode;

        mTracks = new MediaTracks(this.baseLangCode);
        for(us.nineworlds.plex.rest.model.impl.Stream stream : streams)
            mTracks.add(capabilities, stream);
        mTracks.setInitialSelectedTracks();
    }

    protected MediaTrackSelector(Parcel in) {

        baseLangCode = in.readString();
        mTracks = new MediaTracks(in);
    }

    public Stream getSelectedTrack(int streamType) {

        return mTracks.getSelectedTrack(streamType);
    }

    public void disableTrackType(TrackSelector selector, int streamType) {

        mTracks.setSelectedTrack(streamType, null);
        if (selector != null)
            selector.setSelectionOverride(streamType == Stream.Subtitle_Stream ? C.TRACK_TYPE_TEXT : C.TRACK_TYPE_AUDIO, null);
    }

    public void setSelectedTrack(TrackSelector selector, Stream.StreamChoice choice) {

        int streamType = choice.getTrackType();
        mTracks.setSelectedTrack(streamType, choice.stream);
        switch(streamType) {

            case Stream.Subtitle_Stream:

                if (selector != null)
                    selector.setSelectionOverride(C.TRACK_TYPE_TEXT, choice);
                break;

            case Stream.Audio_Stream:

                if (selector != null)
                    selector.setSelectionOverride(C.TRACK_TYPE_AUDIO, choice);
                break;
        }
    }


    public int getCount(int streamType) {

        return mTracks.getCount(streamType);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<MediaTrackSelector> CREATOR = new Creator<MediaTrackSelector>() {
        @Override
        public MediaTrackSelector createFromParcel(Parcel in) {
            return new MediaTrackSelector(in);
        }

        @Override
        public MediaTrackSelector[] newArray(int size) {
            return new MediaTrackSelector[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeString(baseLangCode);
        mTracks.writeToParcel(dest, flags);
    }

    public StreamChoiceArrayAdapter getTracks(Context context, int streamType) {

        return mTracks.getTracks(context, streamType);
    }

    public static class StreamChoiceArrayAdapter extends ArrayAdapter<Stream.StreamChoice> {


        StreamChoiceArrayAdapter(Context context, List<Stream.StreamChoice> values) {
            super(context, R.layout.lb_streamchoiceitem, values);
        }
    }
}
