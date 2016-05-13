package com.monsterbutt.homeview.plex.media;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.Utils;
import com.monsterbutt.homeview.plex.PlexServer;

import us.nineworlds.plex.rest.model.impl.Video;


public class Episode extends PlexVideoItem implements Parcelable {

    public static String TYPE = "episode";

    public Episode(Video video) {

        super(video);
    }

    protected Episode(Parcel in) {
        super(in);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Episode> CREATOR = new Creator<Episode>() {
        @Override
        public Episode createFromParcel(Parcel in) {
            return new Episode(in);
        }

        @Override
        public Episode[] newArray(int size) {
            return new Episode[size];
        }
    };

    public String getShowName() {
        return mVideo.getGrandParentTitle();
    }

    public String getEpisodeNum() {
        return mVideo.getEpisode();
    }

    public String getShowKey() {
        return mVideo.getGrandparentKey();
    }

    public long getShowRatingKey() {
        return mVideo.getGrandparentRatingKey();
    }

    public String getShowArtKey() {
        return mVideo.getGrandparentArtKey();
    }

    @Override
    public String getThemeKey(PlexServer server) {
        String ret = mVideo.getGrandparentThemeKey();
        if (!TextUtils.isEmpty(ret))
            ret = server.makeServerURL(ret);
        return ret;
    }

    public String getSeasonNum() {
        return mVideo.getSeason();
    }

    public String getSeasonKey() {
        return mVideo.getParentKey();
    }

    public long getSeasonRatingKey() {
        return mVideo.getParentRatingKey();
    }

    public String getShowThumbKey() {
        return mVideo.getGrandParentThumbNailImageKey();
    }

    public String getSeasonThumbKey() {
        return mVideo.getParentThumbNailImageKey();
    }

    @Override
    public String getCardTitle(Context context) {
        return getTitle();
    }

    @Override
    public String getCardContent(Context context) {

        long duration = getDurationInMin();
        if (duration > 0) {
            return String.format("%s%s %s %s%s %s %s %s",
                    context.getString(R.string.season_abbrev),
                    getSeasonNum(),
                    context.getString(R.string.mid_dot),
                    context.getString(R.string.episodes_abbrev),
                    getEpisodeNum(),
                    context.getString(R.string.mid_dot),
                    Long.toString(duration),
                    context.getString(R.string.minutes_abbrev));
        }
        return String.format("%s%s %s %s%s",
                context.getString(R.string.season_abbrev),
                getSeasonNum(),
                context.getString(R.string.mid_dot),
                context.getString(R.string.episodes_abbrev),
                getEpisodeNum());
    }

    @Override
    public String getPlaybackTitle(Context context) {
        return String.format("%s %s %s",
                getEpisodeNum(),
                context.getString(R.string.mid_dot),
                getTitle());
    }

    @Override
    public String getPlaybackSubtitle(Context context) {

        String showName = getShowName();
        String season = getSeasonNum();
        String date = Utils.convertDateToText(context, mVideo.getOriginallyAvailableDate());
        if (!TextUtils.isEmpty(showName) && !TextUtils.isEmpty(season)) {
            if (!TextUtils.isEmpty(date))
                return String.format("%s %s %s %s %s %s", showName, context.getString(R.string.mid_dot),
                        context.getString(R.string.Season), season,  context.getString(R.string.mid_dot),
                        date);
            return String.format("%s %s %s %s", showName, context.getString(R.string.mid_dot),
                    context.getString(R.string.Season), season);
        }
        if (!TextUtils.isEmpty(showName)) {
            if (!TextUtils.isEmpty(date))
                return String.format("%s %s %s", showName, context.getString(R.string.mid_dot), date);
            return showName;
        }
        if (!TextUtils.isEmpty(season)) {
            if (!TextUtils.isEmpty(date))
                return String.format("%s %s %s %s", context.getString(R.string.Season), season,
                        context.getString(R.string.mid_dot), date);
            return String.format("%s %s", context.getString(R.string.Season), season);
        }
        if (!TextUtils.isEmpty(date))
            return date;

        return "";
    }

    @Override
    public String getPlaybackImageURL() {
        return getCardImageURL();
    }

    @Override
    public String getCardImageURL() {

        String parent = mVideo.getParentThumbNailImageKey();
        if (parent != null && !parent.isEmpty())
            return parent;
        parent = mVideo.getGrandParentThumbNailImageKey();
        if (parent == null)
            return "";
        return parent;
    }

    @Override
    public String getWideCardTitle(Context context) {
        return getTitle();
    }

    @Override
    public String getWideCardContent(Context context) {

        String episode = getEpisodeNum();
        if (!TextUtils.isEmpty(episode))
            return String.format("%s %s %s %s %s",
                context.getString(R.string.Episode),
                episode,
                context.getString(R.string.mid_dot),
                Long.toString(getDurationInMin()),
                context.getString(R.string.minutes_abbrev));
        return String.format("%s %s",
                Long.toString(getDurationInMin()),
                context.getString(R.string.minutes_abbrev));
    }

    public String getWideCardImageURL() {
        return mVideo.getThumbNailImageKey();
    }

    @Override
    public String getDetailSubtitle(Context context) { return getShowName(); }

    @Override
    public String getDetailContent(Context context) {

        String date = Utils.convertDateToText(context, mVideo.getOriginallyAvailableDate());
        if (TextUtils.isEmpty(date))
            date = TextUtils.isEmpty(getYear()) ? "" : getYear();

        String season = getSeasonNum();
        if (TextUtils.isEmpty(date)) {
            if (!TextUtils.isEmpty(season))
            return String.format("%s %s %s %s %s",
                    context.getString(R.string.Season),
                    season,
                    context.getString(R.string.mid_dot),
                    context.getString(R.string.Episode),
                    getEpisodeNum());
            return String.format("%s %s",
                    context.getString(R.string.Episode),
                    getEpisodeNum());
        }
        if (!TextUtils.isEmpty(season))
            return String.format("%s %s %s %s %s %s %s",
                context.getString(R.string.Season),
                season,
                context.getString(R.string.mid_dot),
                context.getString(R.string.Episode),
                getEpisodeNum(),
                context.getString(R.string.mid_dot),
                date);
        return String.format("%s %s %s %s",
                context.getString(R.string.Episode),
                getEpisodeNum(),
                context.getString(R.string.mid_dot),
                date);
    }

    public void setSeasonNum(long parentIndex) {
        mVideo.setSeason(Long.toString(parentIndex));
    }
}
