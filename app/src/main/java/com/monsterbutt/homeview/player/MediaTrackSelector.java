package com.monsterbutt.homeview.player;


import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.Stream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MediaTrackSelector implements Parcelable {

    Map<Integer, List<Stream>> tracks = new HashMap<>();

    private Stream selectedVideoTrack = null;
    private Stream selectedAudioTrack = null;
    private Stream selectedSubtitleTrack = null;
    private boolean didSelectSubs = false;
    private final String baseLangCode;

    private final static int None = 0;
    private final static int First = None;
    private final static int Language = 1;
    private final static int Default = 2;
    private final static int Forced = 4;

    public final static int TrackTypeOff = -1;

    public MediaTrackSelector(Context context, List<us.nineworlds.plex.rest.model.impl.Stream> streams,
                              String baseLangCode, MediaCodecCapabilities capabilities) {

        this.baseLangCode = TextUtils.isEmpty(baseLangCode) ? "" : baseLangCode;
        int videoIndex = 0;
        int audioIndex = 0;
        int subIndex = 0;
        int otherIndex = 0;

        fillOffSubtitleStream(context.getString(R.string.subs_off), capabilities);
        for(us.nineworlds.plex.rest.model.impl.Stream stream : streams) {

            final int streamType = (int) stream.getStreamType();
            int trackIndex;
            switch(streamType) {

                case Stream.Video_Stream:
                    trackIndex = videoIndex++;
                    break;

                case Stream.Audio_Stream:
                    trackIndex = audioIndex++;
                    break;

                case Stream.Subtitle_Stream:
                    trackIndex = subIndex++;
                    break;

                default:
                    trackIndex = otherIndex++;
                    break;
            }

            List<Stream> type = tracks.get(streamType);
            if (type == null) {
                type = new ArrayList<>();
                tracks.put(streamType, type);
            }
            type.add(new Stream(stream, trackIndex, capabilities));
        }

        selectedVideoTrack = selectStreamForType(Stream.Video_Stream);
        selectedAudioTrack = selectStreamForType(Stream.Audio_Stream);

        List<Stream> subs = tracks.get(Stream.Subtitle_Stream);
        if (subs.size() == 1)
            subs.clear();
        else {
            selectedSubtitleTrack = selectStreamForType(Stream.Subtitle_Stream);
            if (0 == (Forced & getTrackChoice(selectedSubtitleTrack)))
                setSelectedTrack(Stream.Subtitle_Stream, 0);
        }
    }

    protected MediaTrackSelector(Parcel in) {

        baseLangCode = in.readString();
        didSelectSubs = in.readInt() == 1;
        List<Stream> list = in.createTypedArrayList(Stream.CREATOR);
        tracks.put(Stream.Video_Stream, list);
        list = in.createTypedArrayList(Stream.CREATOR);
        tracks.put(Stream.Audio_Stream, list);
        list = in.createTypedArrayList(Stream.CREATOR);
        tracks.put(Stream.Subtitle_Stream, list);
        setSelectedTrack(Stream.Video_Stream, in.readInt());
        setSelectedTrack(Stream.Audio_Stream, in.readInt());
        setSelectedTrack(Stream.Subtitle_Stream, 1 + in.readInt());
    }

    private void fillOffSubtitleStream(String title, MediaCodecCapabilities capabilities) {

        List<Stream> list = tracks.get(Stream.Subtitle_Stream);
        if (list == null) {
            list = new ArrayList<>();
            tracks.put(Stream.Subtitle_Stream, list);
        }

        us.nineworlds.plex.rest.model.impl.Stream off = new us.nineworlds.plex.rest.model.impl.Stream();
        off.setStreamType(Stream.Subtitle_Stream);
        off.setLanguage(title);
        off.setLanguageCode(baseLangCode);
        list.add(new Stream(off, TrackTypeOff, capabilities));
    }

    private Stream selectStreamForType(int type) {

        Stream selectedTrack = null;
        List<Stream> currentStreams = tracks.get(type);
        if (currentStreams != null) {
            int choice = First;
            selectedTrack = currentStreams.get(0);
            boolean choiceIsUnsupported = selectedTrack.getDecodeStatus() == MediaCodecCapabilities.DecodeType.Unsupported;
            for (Stream stream : currentStreams) {

                final boolean currentIsUnsupported = stream.getDecodeStatus() == MediaCodecCapabilities.DecodeType.Unsupported;
                final int current = getTrackChoice(stream);
                if ((current != None && choice < current && !currentIsUnsupported)
                 || (choiceIsUnsupported && !currentIsUnsupported)) {

                    choice = current;
                    selectedTrack = stream;
                }
            }
        }
        return selectedTrack;
    }

    private int getTrackChoice(Stream stream) {

        return  (baseLangCode.equals(stream.getLanguageCode()) ? Language : None) |
                (stream.isDefault() ? Default : None) |
                (stream.isForced() ? Forced : None);
    }

    public Stream getSelectedTrack(int type) {

        switch(type) {
            case Stream.Video_Stream:
                return selectedVideoTrack;
            case Stream.Audio_Stream:
                return selectedAudioTrack;
            case Stream.Subtitle_Stream:
                return selectedSubtitleTrack;
            default:
                return null;
        }
    }

    public int getAdjustedIndexForSelectedTrack(int type) {

        switch(type) {
            case Stream.Video_Stream:
                return getAdjustedIndexForSelectedTrack(tracks.get(type), selectedVideoTrack);
            case Stream.Audio_Stream:
                return getAdjustedIndexForSelectedTrack(tracks.get(type), selectedAudioTrack);
            case Stream.Subtitle_Stream:
                return selectedSubtitleTrack.getTrackTypeIndex();
            default:
                return -1;
        }
    }

    private int getAdjustedIndexForSelectedTrack(List<Stream> streams, Stream selected) {

        int adjustment = 0;
        for (Stream stream : streams) {

            if (stream.equals(selected))
                break;
            else if (stream.getDecodeStatus() == MediaCodecCapabilities.DecodeType.Unsupported)
                ++adjustment;
        }
        if (selected.getDecodeStatus() == MediaCodecCapabilities.DecodeType.Unsupported)
            return TrackTypeOff;
        final int index = selected.getTrackTypeIndex();
        return index >= 0 ? index - adjustment : index;
    }

    public boolean setSelectedTrack(int type, int index) {

        Stream stream = null;
        List<Stream> streams = tracks.get(type);
        if (streams != null) {

            if (index >= 0 && index < streams.size()) {

                stream = streams.get(index);
                switch(type) {
                    case Stream.Video_Stream:
                        selectedVideoTrack = stream;
                        break;
                    case Stream.Audio_Stream:
                        selectedAudioTrack = stream;
                        break;
                    case Stream.Subtitle_Stream:
                        didSelectSubs = true;
                        selectedSubtitleTrack = stream;
                        break;
                    default:
                        stream = null;
                        break;
                }
            }
            else if (index == TrackTypeOff && type == Stream.Subtitle_Stream) {

                didSelectSubs = true;
                selectedSubtitleTrack = streams.get(0);
            }
        }

        return stream != null;
    }

    public int getCountForType(int type) {

        List<Stream> streams = tracks.get(type);
        return streams != null ? streams.size() : 0;
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
        dest.writeInt(didSelectSubs ? 1 : 0);
        dest.writeTypedList(tracks.get(Stream.Video_Stream));
        dest.writeTypedList(tracks.get(Stream.Audio_Stream));
        dest.writeTypedList(tracks.get(Stream.Subtitle_Stream));
        dest.writeInt(selectedVideoTrack != null ? selectedVideoTrack.getTrackTypeIndex() : -1);
        dest.writeInt(selectedAudioTrack != null ? selectedAudioTrack.getTrackTypeIndex() : -1);
        dest.writeInt(selectedSubtitleTrack != null ? selectedSubtitleTrack.getTrackTypeIndex() : -1);
    }

    public StreamChoiceArrayAdapter getTracks(Context context, PlexServer server, int trackType) {

        List<Stream.StreamChoice> list = new ArrayList<>();
        StreamChoiceArrayAdapter adapter = new StreamChoiceArrayAdapter(context, server, list);
        List<Stream> streams = tracks.get(trackType);
        if (streams != null) {

            int currentTrackIndex = getSelectedTrack(trackType).getTrackTypeIndex();
            for(Stream stream : streams)
                list.add(new Stream.StreamChoice(context, currentTrackIndex == stream.getTrackTypeIndex(), stream));
        }
        return adapter;
    }

    public static class StreamChoiceArrayAdapter extends ArrayAdapter<Stream.StreamChoice> {

        private final Context context;
        private final List<Stream.StreamChoice> values;
        private final PlexServer server;

        public StreamChoiceArrayAdapter(Context context, PlexServer server, List<Stream.StreamChoice> values) {
            super(context, R.layout.lb_aboutitem, values);
            this.context = context;
            this.values = values;
            this.server = server;
        }

        @Override
        public View getView(int position, View row, ViewGroup parent) {

            View rowView = row;
            if (rowView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                rowView = inflater.inflate(R.layout.lb_streamchoiceitem, parent, false);
            }
            final Stream.StreamChoice item = values.get(position);

            CheckBox checkBox = (CheckBox) rowView.findViewById(R.id.currentcheckbox);
            checkBox.setChecked(item.isCurrentSelection());
            ImageView image = (ImageView) rowView.findViewById(R.id.codecimage);
            String path = item.getCodecImage(server);
            if (!TextUtils.isEmpty(path)) {

                Glide.with(context)
                        .load(path)
                        .into(image);
            }
            else
                image.setImageDrawable(item.getDrawable());

            TextView desc = (TextView) rowView.findViewById(R.id.codecdesc);
            desc.setText(item.toString());
            TextView supported = (TextView) rowView.findViewById(R.id.codecsupported);
            supported.setText(item.getDecoderStatus());

            return rowView;
        }
    }

    public boolean didManuallySelectSubs() { return didSelectSubs; }
}
