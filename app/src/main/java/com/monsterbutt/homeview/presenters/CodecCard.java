package com.monsterbutt.homeview.presenters;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.Stream;
import com.monsterbutt.homeview.plex.media.VideoFormat;

import us.nineworlds.plex.rest.model.impl.Media;



public class CodecCard extends PosterCard {

    public enum CodecCardType {

        VideoCodec,
        VideoFrame,
        AudioCodec,
        SubtitleCodec
    }

    private class CodecIconHolder {

        public Drawable drawable;
        public final String type;
        public final String id;

        public CodecIconHolder(String type, String id, Drawable drawable) {
            this.id = id;
            this.type = type;
            this.drawable = drawable;
        }

        public CodecIconHolder(String type, String id) {

            this.id = id;
            this.type = type;
            drawable = null;
        }
    }


    private CodecCardType type;
    private String title;
    private String subtitle;
    private CodecIconHolder iconA;
    private CodecIconHolder iconB;

    public CodecCard(Context context, CodecCardType type, Media media) {
        super(context, null);

        this.type = type;
        switch (type) {
            case VideoCodec:

                title = "";
                subtitle = "";
                iconA = new CodecIconHolder(VideoFormat.VideoCodec, media.getVideoCodec());
                iconB = new CodecIconHolder(VideoFormat.VideoFrameRate, media.getVideoFrameRate());
                break;

            case VideoFrame:

                title = "";
                subtitle = "";
                iconA = new CodecIconHolder(VideoFormat.VideoResolution, media.getVideoResolution());
                iconB = new CodecIconHolder(VideoFormat.VideoAspectRatio, media.getAspectRatio());
                break;

            case AudioCodec:

                title = "";
                subtitle = "";
                iconA = new CodecIconHolder(Stream.AudioCodec, media.getAudioCodec());
                iconB = new CodecIconHolder(Stream.AudioChannels, media.getAudioChannels());
                break;

            default:

                title = "";
                subtitle = "";
                iconA = null;
                iconB = null;
                break;
        }
    }

    public CodecCard(Context context, CodecCardType type,
                     us.nineworlds.plex.rest.model.impl.Stream stream) {
        super(context, null);

        this.type = type;
        switch (type) {

            case AudioCodec:

                title = stream.getTitle();
                subtitle = stream.getLanguage();
                iconA = new CodecIconHolder(Stream.AudioCodec, stream.getCodec());
                iconB = new CodecIconHolder(Stream.AudioChannels, stream.getChannels());
                break;

            case SubtitleCodec:

                title = stream.getLanguage();
                subtitle = stream.getForced() > 0 ? context.getString(R.string.Forced) : "";
                iconA = new CodecIconHolder("", stream.getCodec(), context.getDrawable(R.drawable.ic_subtitles_white_48dp));
                iconB = null;
                break;

            default:

                title = "";
                subtitle = "";
                iconA = null;
                iconB = null;
                break;
        }
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getContent() {
        return subtitle;
    }

    @Override
    public String getImageUrl(PlexServer server) {
        return server.makeServerURLForCodec(iconA.type, iconA.id);
    }
    public String getImageUrlSecondary(PlexServer server) {
        if (iconB == null)
            return "";
        return server.makeServerURLForCodec(iconB.type, iconB.id);
    }

    @Override
    public Drawable getImage(Context context) {
        return iconA.drawable;
    }
    public Drawable getImageSecondary() {
        if (iconB == null)
            return null;
        return iconB.drawable;
    }

    public CodecCardType getType() {
        return type;
    }

    @Override
    public int getHeight() {
        return R.dimen.codeccard_height;
    }

    @Override
    public int getWidth() {
        return R.dimen.codeccard_width;
    }
}
