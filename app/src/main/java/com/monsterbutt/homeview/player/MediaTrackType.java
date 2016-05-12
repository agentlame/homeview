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
    private final boolean hasOffTrack;
    private final String baseLangCode;
    private Stream selected = null;
    private List<Stream> streams = new ArrayList<>();

    public MediaTrackType(Context context, MediaCodecCapabilities capabilities, String baseLangCode, int streamType) {

        this.streamType = streamType;
        this.baseLangCode = baseLangCode;

        if (streamType == Stream.Subtitle_Stream) {

            us.nineworlds.plex.rest.model.impl.Stream off = new us.nineworlds.plex.rest.model.impl.Stream();
            off.setStreamType(Stream.Subtitle_Stream);
            off.setLanguage(context != null ? context.getString(R.string.subs_off) : "Off");
            off.setLanguageCode(baseLangCode);
            streams.add(new Stream(off, MediaTrackSelector.TrackTypeOff, capabilities));
            hasOffTrack = true;
        }
        else
            hasOffTrack = false;
    }

    protected MediaTrackType(Parcel in) {
        streams = in.createTypedArrayList(Stream.CREATOR);
        streamType = in.readInt();
        hasOffTrack = in.readByte() != 0;
        baseLangCode = in.readString();
        int select = in.readInt();
        if (select < 0)
            selected = null;
        else
            selected = streams.get(select);
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
            return MediaTrackSelector.TrackTypeOff;
        return streams.indexOf(selected);
    }

    public int getSelectedPlayerIndex() {

        // this skips off and unsupported as exoplayer know nothing of off and skips unsupported
        if (selected == null || selected.getDecodeStatus() == MediaCodecCapabilities.DecodeType.Unsupported)
            return MediaTrackSelector.TrackTypeOff;
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

        int index = hasOffTrack ? streams.size() - 1 : streams.size();
        streams.add(new Stream(stream, index, capabilities));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(streams);
        dest.writeInt(streamType);
        dest.writeByte((byte) (hasOffTrack ? 1 : 0));
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
            if (streams.size() == 1)
                streams.clear();
            else if (0 == (Forced & getTrackChoice(selected)))
                selected = streams.get(0);
        }
    }

    public int setSelectedTrack(int displayIndex) {

        selected = null;
        if (displayIndex >= 0 && displayIndex < streams.size())
            selected = streams.get(displayIndex);
        return selected == null ? MediaTrackSelector.TrackTypeOff : getSelectedPlayerIndex();
    }

    public MediaTrackSelector.StreamChoiceArrayAdapter getTracks(Context context, PlexServer server) {

        List<Stream.StreamChoice> list = new ArrayList<>();
        MediaTrackSelector.StreamChoiceArrayAdapter adapter = new MediaTrackSelector.StreamChoiceArrayAdapter(context, server, list);
        for(Stream stream : streams)
            list.add(new Stream.StreamChoice(context, stream == selected, stream));

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
}
