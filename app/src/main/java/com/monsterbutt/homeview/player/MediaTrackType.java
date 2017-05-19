package com.monsterbutt.homeview.player;


import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.Stream;

import java.util.ArrayList;
import java.util.List;

public class MediaTrackType implements Parcelable {

    private final static int None = 0;
    private final static int First = None;
    private final static int Language = 1;
    private final static int Default = 2;
    private final static int Forced = 4;

    private final int streamType;
    private final String baseLangCode;
    private Stream selected = null;
    private Stream defaultStream = null;
    private List<Stream> streams = new ArrayList<>();

    public MediaTrackType(Context context, MediaCodecCapabilities capabilities, String baseLangCode, int streamType) {

        this.streamType = streamType;
        this.baseLangCode = baseLangCode;
    }

    protected MediaTrackType(Parcel in) {
        streams = in.createTypedArrayList(Stream.CREATOR);
        streamType = in.readInt();
        baseLangCode = in.readString();
        int select = in.readInt();
        selected = select < 0 ? null : streams.get(select);

        for (Stream stream : streams) {
            if (evaluateDefault(stream))
                break;
        }
    }

    private boolean evaluateDefault(Stream stream) {
        if (stream.isDefault()) {
            if (stream.getLanguageCode() != null && stream.getLanguageCode().equalsIgnoreCase(baseLangCode)) {
                if (defaultStream == null || !defaultStream.getLanguageCode().equalsIgnoreCase(baseLangCode)) {
                    defaultStream = stream;
                    return true;
                }
            } else if (defaultStream == null)
                defaultStream = stream;
        }
        return false;
    }

    public static final Creator<MediaTrackType> CREATOR = new Creator<MediaTrackType>() {
        @Override
        public MediaTrackType createFromParcel(Parcel in) {
            return new MediaTrackType(in);
        }

        @Override
        public MediaTrackType[] newArray(int size) {
            return new MediaTrackType[size];
        }
    };

    public int getStreamType() { return streamType; }

    public int getCount() { return streams.size(); }

    public int getSelectedTrackDisplayIndex() {

        if (selected == null)
            return TrackSelector.TrackTypeOff;
        return streams.indexOf(selected);
    }

    public int getSelectedPlayerIndex() {

        // this skips off and unsupported as exoplayer know nothing of off and skips unsupported
        if (selected == null || selected.getDecodeStatus() == MediaCodecCapabilities.DecodeType.Unsupported)
            return TrackSelector.TrackTypeOff;
        boolean isSoftware = selected.getDecodeStatus() == MediaCodecCapabilities.DecodeType.Software;
        int playerIndex = selected.getTrackTypeIndex();
        for (int currIndex = playerIndex; 0 < currIndex--; /**/) {

            Stream currStream = streams.get(currIndex);
            if ((isSoftware && currStream.getDecodeStatus() != MediaCodecCapabilities.DecodeType.Software)
                || (!isSoftware && currStream.getDecodeStatus() == MediaCodecCapabilities.DecodeType.Software))
                --playerIndex;
        }
        return playerIndex;
    }

    public void add(MediaCodecCapabilities capabilities, us.nineworlds.plex.rest.model.impl.Stream stream) {

        int index = streams.size();
        streams.add(new Stream(stream, index, capabilities));
        evaluateDefault(streams.get(streams.size()-1));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(streams);
        dest.writeInt(streamType);
        dest.writeString(baseLangCode);
        dest.writeInt(selected == null ? -1 : streams.indexOf(selected));
    }

    private int getTrackChoice(Stream stream) {

        return  (baseLangCode.equals(stream.getLanguageCode()) ? Language : None) |
                (stream.isDefault() ? Default : None) |
                (stream.isForced() ? Forced : None);
    }

    public void setInitialSelectedTrack() {

        if (streams != null) {
            int choice = First;
            selected = streams.get(0);
            boolean choiceIsUnsupported = selected.getDecodeStatus() == MediaCodecCapabilities.DecodeType.Unsupported;
            for (Stream stream : streams) {

                final boolean currentIsUnsupported = stream.getDecodeStatus() == MediaCodecCapabilities.DecodeType.Unsupported;
                final int current = getTrackChoice(stream);
                if ((current != None && choice < current && !currentIsUnsupported)
                        || (choiceIsUnsupported && !currentIsUnsupported)) {

                    choice = current;
                    selected = stream;
                }
            }
        }

        // for subs we are always off unless forced or picked manually
        if (getStreamType() == Stream.Subtitle_Stream) {
            if (streams.size() > 0 && 0 == (Forced & getTrackChoice(selected)))
                selected = null;
        }
    }

    public int setSelectedTrack(int displayIndex) {

        selected = null;
        if (displayIndex >= 0 && displayIndex < streams.size())
            selected = streams.get(displayIndex);
        return selected == null || selected.getIndex() == null ? TrackSelector.TrackTypeOff : Integer.parseInt(selected.getIndex());
    }

    public MediaTrackSelector.StreamChoiceArrayAdapter getTracks(Context context, PlexServer server) {

        List<Stream.StreamChoice> list = new ArrayList<>();
        MediaTrackSelector.StreamChoiceArrayAdapter adapter = new MediaTrackSelector.StreamChoiceArrayAdapter(context, server, list);
        for(Stream stream : streams)
            list.add(new Stream.StreamChoice(context, stream == selected, stream));
        list.add(0, new Stream.StreamChoiceDisable(context, selected == null));

        return adapter;
    }

    public MediaCodecCapabilities.DecodeType getDecodeStatusForSelected() {

        if (selected == null)
            return MediaCodecCapabilities.DecodeType.Unsupported;
        return selected.getDecodeStatus();
    }

    public Stream getSelectedTrack() {
        return selected;
    }

    public boolean isSelectedTrack(int displayIndex) {

        if (displayIndex >= 0 && displayIndex < streams.size())
            return selected == streams.get(displayIndex);
        return false;
    }
}
