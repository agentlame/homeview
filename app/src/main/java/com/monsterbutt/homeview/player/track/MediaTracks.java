package com.monsterbutt.homeview.player.track;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

import us.nineworlds.plex.rest.model.impl.Stream;


class MediaTracks implements Parcelable {

    @SuppressLint("UseSparseArrays")
    private Map<Integer, MediaTrackType> tracks = new HashMap<>();
    private final String baseLangCode;

    MediaTracks(String baseLangCode) {
        this.baseLangCode = baseLangCode;
    }

    MediaTracks(Parcel in) {

        baseLangCode = in.readString();
        int count = in.readInt();
        for (int i = 0; i < count; ++i) {

            MediaTrackType type = new MediaTrackType(in);
            tracks.put(type.getStreamType(), type);
        }
    }

    public static final Creator<MediaTracks> CREATOR = new Creator<MediaTracks>() {
        @Override
        public MediaTracks createFromParcel(Parcel in) {
            return new MediaTracks(in);
        }

        @Override
        public MediaTracks[] newArray(int size) {
            return new MediaTracks[size];
        }
    };

    public void add(MediaCodecCapabilities capabilities, Stream stream) {

        int streamType = (int) stream.getStreamType();
        MediaTrackType type = tracks.get(streamType);
        if (type == null) {
            type = new MediaTrackType(baseLangCode, streamType);
            tracks.put(streamType, type);
        }
        type.add(capabilities, stream);
    }

    int getCount(int streamType) {
        return tracks.containsKey(streamType) ? tracks.get(streamType).getCount() : 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(baseLangCode);
        dest.writeInt(tracks.size());
        for (MediaTrackType type : tracks.values())
            type.writeToParcel(dest, flags);
    }

    void setInitialSelectedTracks() {

        for (MediaTrackType type : tracks.values())
            type.setInitialSelectedTrack();
    }

     void setSelectedTrack(int streamType, com.monsterbutt.homeview.plex.media.Stream stream) {

        MediaTrackType type = tracks.get(streamType);
        if (type == null)
            return;
        type.setSelectedTrack(stream);
    }

    MediaTrackSelector.StreamChoiceArrayAdapter getTracks(Context context, int streamType) {

        MediaTrackType type = tracks.get(streamType);
        return (type == null) ? null : type.getTracks(context);
    }

    com.monsterbutt.homeview.plex.media.Stream getSelectedTrack(int streamType) {

        MediaTrackType type = tracks.get(streamType);
        return (type == null) ? null : type.getSelectedTrack();
    }
}
