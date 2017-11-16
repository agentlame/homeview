package com.monsterbutt.homeview.ui.presenters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.player.track.MediaTrackSelector;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.Stream;
import com.monsterbutt.homeview.plex.media.VideoFormat;


public class CodecCard extends PosterCard {


    private class CodecIconHolder {

        public Drawable drawable;
        public final String type;
        public final String id;

        CodecIconHolder(String type, String id, Drawable drawable) {
            this.id = id;
            this.type = type;
            this.drawable = drawable;
        }

        CodecIconHolder(String type, String id) {

            this.id = id;
            this.type = type;
            drawable = null;
        }
    }

    private String title;
    private String subtitle;
    private String decoder;
    private CodecIconHolder iconA;
    private CodecIconHolder iconB;
    private int trackType;
    private int totalTracksForType;
    private Stream.StreamChoice stream;

    private MediaTrackSelector.StreamChoiceArrayAdapter adapter = null;

    public CodecCard(Context context, Stream.StreamChoice choice, int trackType) {
        this(context, choice.stream, trackType, 1, true);
        stream = choice;
    }

    private CodecCard(Context context, Stream stream, int trackType, int totalTracksForType, boolean codecOnly) {

        super(context, null);
        this.stream = null;
        this.trackType = trackType;
        this.totalTracksForType = totalTracksForType;
        this.decoder = stream != null ? stream.getDecodeStatusText(context) : "";

        String isDefault = stream != null && stream.isDefault() ? context != null ? context.getString(R.string.Default) : "Default" : "";
        String midDot = context != null ? " " + context.getString(R.string.mid_dot) + " ": " \u00B7 ";

        switch (trackType) {

            case Stream.Video_Stream:

                if (stream == null) {
                    title = context != null ? context.getString(R.string.selection_disabled) : "";
                    subtitle = "";
                    iconA = new CodecIconHolder("", "", context != null ? context.getDrawable(R.drawable.ic_video_label_white_48dp) : null);
                    iconB = null;
                }
                else {
                    title = "";
                    subtitle = "";
                    iconA = new CodecIconHolder(VideoFormat.VideoCodec, stream.getCodec());
                    iconB = codecOnly ? null : new CodecIconHolder(VideoFormat.VideoResolution, stream.getHeight());
                }
                break;

            case Stream.Audio_Stream:

                if (stream == null) {
                    title = context != null ? context.getString(R.string.selection_disabled) : null;
                    subtitle = "";
                    iconA = new CodecIconHolder("", "", context != null ? context.getDrawable(R.drawable.ic_audiotrack_white_36dp) : null);
                    iconB = null;
                }
                else {
                    title = stream.getTitle();
                    subtitle = stream.getLanguage();
                    if (TextUtils.isEmpty(title)) {
                        title = subtitle;
                        subtitle = "";
                    }
                    if (!TextUtils.isEmpty(isDefault)) {
                        if (TextUtils.isEmpty(title))
                            title = isDefault;
                        else if (TextUtils.isEmpty(subtitle))
                            subtitle = isDefault;
                        else
                            subtitle += midDot + isDefault;
                    }
                    iconA = new CodecIconHolder(Stream.AudioCodec, stream.getCodecAndProfile());
                    iconB = codecOnly ? null : new CodecIconHolder(Stream.AudioChannels, stream.getChannels());
                }
                break;

            case Stream.Subtitle_Stream:

                if (stream == null) {
                    title = context != null ? context.getString(R.string.selection_disabled) : "";
                    subtitle = "";
                    iconA = new CodecIconHolder("", "", context != null ? context.getDrawable(R.drawable.ic_subtitles_white_36dp) : null);
                    iconB = null;
                }
                else {
                    title = stream.getLanguage();
                    subtitle = stream.isForced() ? context != null ? context.getString(R.string.Forced) : "Forced" : "";
                    if (!TextUtils.isEmpty(subtitle) && TextUtils.isEmpty(title))
                        title = subtitle;
                    if (!TextUtils.isEmpty(isDefault)) {
                        if (TextUtils.isEmpty(title))
                            title = isDefault;
                        else if (TextUtils.isEmpty(subtitle))
                            subtitle = isDefault;
                        else
                            subtitle += midDot + isDefault;
                    }
                    iconA = new CodecIconHolder("", stream.getCodec(), context != null ? context.getDrawable(R.drawable.ic_subtitles_white_36dp) : null);
                    iconB = null;
                }
                break;

            default:

                title = "";
                subtitle = "";
                iconA = null;
                iconB = null;
                break;
        }
    }

    public Stream.StreamChoice getStream() { return stream; }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getContent() {
        return subtitle;
    }

    public String getDecoder() {
        return decoder;
    }

    @Override
    public String getImageUrl(PlexServer server) {
        if (iconA == null)
            return "";
        return server.makeServerURLForCodec(iconA.type, iconA.id);
    }

    String getImageUrlSecondary(PlexServer server) {
        if (iconB == null)
            return "";
        return server.makeServerURLForCodec(iconB.type, iconB.id);
    }

    @Override
    public Drawable getImage(Context context) {
        if (iconA == null)
            return null;
        return iconA.drawable;
    }

    Drawable getImageSecondary() {
        if (iconB == null)
            return null;
        return iconB.drawable;
    }

    @Override
    public int getHeight() {
        return R.dimen.codeccard_height;
    }

    @Override
    public int getWidth() {
        return R.dimen.codeccard_width;
    }

    int getTrackType() {
        return trackType;
    }

    int getTotalTracksForType() {
        return totalTracksForType;
    }

    public MediaTrackSelector.StreamChoiceArrayAdapter getAdapter() { return adapter; }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof CodecCard) && ((CodecCard) obj).getTrackType() == getTrackType();
    }
}
