package com.monsterbutt.homeview.player;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.monsterbutt.homeview.plex.PlexServer;

import java.util.HashMap;
import java.util.Map;

import us.nineworlds.plex.rest.model.impl.Stream;


public class MediaTracks implements Parcelable {

    private Map<Integer, MediaTrackType> tracks = new HashMap<>();
    private final String baseLangCode;

    public MediaTracks(String baseLangCode) {
        this.baseLangCode = baseLangCode;
    }

    protected MediaTracks(Parcel in) {

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

    public void add(Context context, MediaCodecCapabilities capabilities, Stream stream) {

        int streamType = (int) stream.getStreamType();
        MediaTrackType type = tracks.get(streamType);
        if (type == null) {
            type = new MediaTrackType(context, capabilities, baseLangCode, streamType);
            tracks.put(streamType, type);
        }
        type.add(capabilities, stream);
    }

    public int getSelectedTrackDisplayIndex(int streamType) {

        MediaTrackType type = tracks.get(streamType);
        if (type == null)
            return TrackSelector.TrackTypeOff;
        return type.getSelectedTrackDisplayIndex();
    }

    public int getSelectedPlayerIndex(int streamType) {

        MediaTrackType type = tracks.get(streamType);
        if (type == null)
            return TrackSelector.TrackTypeOff;
        return type.getSelectedPlayerIndex();
    }

    public int getCount(int streamType) {
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

    public void setInitialSelectedTracks() {

        for (MediaTrackType type : tracks.values())
            type.setInitialSelectedTrack();
    }

    public int setSelectedTrack(int streamType, int displayIndex) {

        MediaTrackType type = tracks.get(streamType);
        if (type == null)
            return TrackSelector.TrackTypeOff;
        return type.setSelectedTrack(displayIndex);
    }

    public MediaTrackSelector.StreamChoiceArrayAdapter getTracks(Context context, PlexServer server, int streamType) {

        MediaTrackType type = tracks.get(streamType);
        return (type == null) ? null : type.getTracks(context, server);
    }

    public MediaCodecCapabilities.DecodeType getDecodeStatusForSelected(int streamType) {

        MediaTrackType type = tracks.get(streamType);
        return (type == null) ? MediaCodecCapabilities.DecodeType.Unsupported : type.getDecodeStatusForSelected();
    }

    public com.monsterbutt.homeview.plex.media.Stream getSelectedTrack(int streamType) {

        MediaTrackType type = tracks.get(streamType);
        return (type == null) ? null : type.getSelectedTrack();
    }

    public boolean isSelectedTrack(int streamType, int displayIndex) {

        MediaTrackType type = tracks.get(streamType);
        if (type == null)
            return false;
        return type.isSelectedTrack(displayIndex);
    }
}
