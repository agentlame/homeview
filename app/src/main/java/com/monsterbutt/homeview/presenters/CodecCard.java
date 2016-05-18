package com.monsterbutt.homeview.presenters;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.player.MediaTrackSelector;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.Stream;
import com.monsterbutt.homeview.plex.media.VideoFormat;

import us.nineworlds.plex.rest.model.impl.Media;


public class CodecCard extends PosterCard {

    public interface OnClickListenerHandler {

        MediaTrackSelector getSelector();
        DialogInterface.OnClickListener getDialogOnClickListener(final Object card, final int trackType);
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

    private String title;
    private String subtitle;
    private String decoder;
    private CodecIconHolder iconA;
    private CodecIconHolder iconB;
    private int trackType;
    private int totalTracksForType;

    public CodecCard(Context context, Media media) {
        super(context, null);

        trackType = 0;
        totalTracksForType = 0;
        decoder = "";

        title = "";
        subtitle = "";
        iconA = new CodecIconHolder(VideoFormat.VideoFrameRate, media.getVideoFrameRate());
        iconB = new CodecIconHolder(VideoFormat.VideoAspectRatio, media.getAspectRatio());
    }

    public CodecCard(Context context, Stream stream, int trackType, int totalTracksForType) {
        super(context, null);

        this.trackType = trackType;
        this.totalTracksForType = totalTracksForType;
        this.decoder = stream.getDecodeStatusText(context);

        switch (trackType) {

            case Stream.Video_Stream:

                title = "";
                subtitle = "";
                iconA = new CodecIconHolder(VideoFormat.VideoCodec, stream.getCodec());
                iconB = new CodecIconHolder(VideoFormat.VideoResolution, stream.getHeight());
                break;

            case Stream.Audio_Stream:

                title = stream.getTitle();
                subtitle = stream.getLanguage();
                if (TextUtils.isEmpty(title)) {
                    title = subtitle;
                    subtitle = "";
                }
                iconA = new CodecIconHolder(Stream.AudioCodec, stream.getCodecAndProfile());
                iconB = new CodecIconHolder(Stream.AudioChannels, stream.getChannels());
                break;

            case Stream.Subtitle_Stream:

                title = stream.getLanguage();
                subtitle = stream.isForced() ? context != null ? context.getString(R.string.Forced) : "Forced" : "";
                iconA = new CodecIconHolder("", stream.getCodec(), context != null ? context.getDrawable(R.drawable.ic_subtitles_white_48dp) : null);
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

    public String getDecoder() {
        return decoder;
    }

    @Override
    public String getImageUrl(PlexServer server) {
        if (iconA == null)
            return "";
        return server.makeServerURLForCodec(iconA.type, iconA.id);
    }

    public String getImageUrlSecondary(PlexServer server) {
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

    public Drawable getImageSecondary() {
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

    public int getTrackType() {
        return trackType;
    }

    public int getTotalTracksForType() {
        return totalTracksForType;
    }

    public void onCardClicked(Activity activity, PlexServer server, OnClickListenerHandler listener) {

        DialogInterface.OnClickListener callback = listener != null ?
                listener.getDialogOnClickListener(this, getTrackType()) : null;
        MediaTrackSelector selector = listener != null ?
                listener.getSelector() : null;
        if (getTotalTracksForType() > 1 && selector != null && callback != null) {
            AlertDialog dialog = new AlertDialog.Builder(activity, R.style.AlertDialogStyle)
                    .setIcon(R.drawable.launcher)
                    .setTitle(R.string.track_dialog)
                    .setAdapter(selector.getTracks(activity, server, getTrackType()), callback)
                    .create();
            dialog.show();
        }
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof CodecCard) && ((CodecCard) obj).getTrackType() == getTrackType();
    }
}
