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
import com.google.android.exoplayer2.C;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.Stream;

import java.util.List;

public class MediaTrackSelector implements Parcelable {

    private MediaTracks mTracks = null;
    private boolean didSelectSubs = false;
    private final String baseLangCode;

    public MediaTrackSelector(Context context, List<us.nineworlds.plex.rest.model.impl.Stream> streams,
                              String baseLangCode, MediaCodecCapabilities capabilities) {

        this.baseLangCode = TextUtils.isEmpty(baseLangCode) ? "" : baseLangCode;

        mTracks = new MediaTracks(this.baseLangCode);
        for(us.nineworlds.plex.rest.model.impl.Stream stream : streams)
            mTracks.add(context, capabilities, stream);
        mTracks.setInitialSelectedTracks();
    }

    protected MediaTrackSelector(Parcel in) {

        baseLangCode = in.readString();
        didSelectSubs = in.readInt() == 1;
        mTracks = new MediaTracks(in);
    }

    public int getSelectedTrackDisplayIndex(int streamType) {

        return mTracks.getSelectedTrackDisplayIndex(streamType);
    }

    public Stream getSelectedTrack(int streamType) {

        return mTracks.getSelectedTrack(streamType);
    }

    public void setSelectedTrack(TrackSelector selector, int streamType, int displayIndex) {

        if (mTracks.isSelectedTrack(streamType, displayIndex))
            return;

        int index = mTracks.setSelectedTrack(streamType, displayIndex);
        switch(streamType) {

            case Stream.Subtitle_Stream:

                didSelectSubs = index != TrackSelector.TrackTypeOff;
                if (selector != null)
                    selector.setSelectionOverride(C.TRACK_TYPE_TEXT, didSelectSubs ? Integer.toString(index+1) : null);
                break;

            case Stream.Audio_Stream:

                if (selector != null)
                    selector.setSelectionOverride(C.TRACK_TYPE_AUDIO, Integer.toString(index+1));
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
        dest.writeInt(didSelectSubs ? 1 : 0);
        mTracks.writeToParcel(dest, flags);
    }

    public StreamChoiceArrayAdapter getTracks(Context context, PlexServer server, int streamType) {

        return mTracks.getTracks(context, server, streamType);
    }

    public static class StreamChoiceArrayAdapter extends ArrayAdapter<Stream.StreamChoice> {

        private final Context context;
        private final List<Stream.StreamChoice> values;
        private final PlexServer server;

        public StreamChoiceArrayAdapter(Context context, PlexServer server, List<Stream.StreamChoice> values) {
            super(context, R.layout.lb_streamchoiceitem, values);
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

    public boolean areSubtitlesEnabled() {

        return mTracks.getSelectedPlayerIndex(Stream.Subtitle_Stream) != TrackSelector.TrackTypeOff
                && didSelectSubs;
    }
}
