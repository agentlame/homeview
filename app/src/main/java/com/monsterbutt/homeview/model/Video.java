package com.monsterbutt.homeview.model;

/*
 * Copyright (c) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Fragment;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.MediaDescription;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.view.View;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.ui.presenters.CardObject;
import com.monsterbutt.homeview.provider.SearchImagesProvider;
import com.monsterbutt.homeview.ui.C;

import java.io.Serializable;

import static com.monsterbutt.homeview.plex.media.PlexLibraryItem.WatchedState.Watched;

/**
 * Video is an immutable object that holds the various metadata associated with a single video.
 */
public final class Video extends CardObject implements Parcelable, Serializable, Comparable<Video> {

    public final long id;
    public final String category;
    public final String title;
    public final String subtitle;
    public final String description;
    public final String bgImageUrl;
    public final String cardImageUrl;
    public final String videoUrl;
    public final String trailerUrl;
    public final String studio;
    public final String rating;
    public final String key;
    public final String parentKey;
    public final String filePath;
    public final String serverPath;
    public final long   duration;
    public final long   watchedOffset;
    public final PlexLibraryItem.WatchedState watched;
    public final boolean shouldStartQueuePlayback;
    public final String frameRate;
    public final String releaseDate;

    public final int episodeNum;
    public final int seasonNum;
    public final String showTitle;
    public final String thumbNail;
    public final int height;
    public final int width;
    public final String[] genre;
    public final String[] ratings;

    public long programId = -1;
    public int sortOrder = 0;

    private Video(
            final long id,
            final String category,
            final String title,
            final String subtitle,
            final String desc,
            final String videoUrl,
            final String bgImageUrl,
            final String cardImageUrl,
            final String studio,
            final String rating,
            final String key,
            final String parentKey,
            final String filePath,
            final String serverPath,
            final long duration,
            final long watchedOffset,
            final PlexLibraryItem.WatchedState watched,
            final String frameRate,
            final String releaseDate,
            final boolean shouldStartQueuePlayback,
            final int episodeNum,
            final int seasonNum,
            final String showTitle,
            final String thumbNail,
            final int height,
            final int width,
            final String trailerUrl,
            final String[] genre,
            final String[] ratings,
            final int sortOrder) {
        this.id = id;
        this.category = category;
        this.title = title;
        this.subtitle = subtitle;
        this.description = desc;
        this.videoUrl = videoUrl;
        this.frameRate = frameRate;
        String back = bgImageUrl;
        if (back != null)
            back = bgImageUrl.replace(SearchImagesProvider.CONTENT_URI, "");

        this.bgImageUrl = back;
        String poster = cardImageUrl;
        if (poster != null)
            poster = cardImageUrl.replace(SearchImagesProvider.CONTENT_URI, "");
        this.cardImageUrl = poster;
        this.studio = studio;
        this.rating = rating;
        this.key = key;
        this.parentKey = parentKey;
        this.filePath = filePath;
        this.serverPath = serverPath;
        this.duration = duration;
        this.watchedOffset = watchedOffset;
        this.watched = watched;
        this.releaseDate = releaseDate;
        this.shouldStartQueuePlayback = shouldStartQueuePlayback;
        this.episodeNum = episodeNum;
        this.seasonNum = seasonNum;
        this.showTitle = showTitle;
        this.thumbNail = thumbNail;
        this.height = height;
        this.width = width;
        this.genre = genre;
        this.trailerUrl = trailerUrl;
        this.ratings = ratings;
        this.sortOrder = sortOrder;
    }


    protected Video(Parcel in) {
        id = in.readLong();
        category = in.readString();
        title = in.readString();
        subtitle = in.readString();
        description = in.readString();
        bgImageUrl = in.readString();
        cardImageUrl = in.readString();
        videoUrl = in.readString();
        studio = in.readString();
        rating = in.readString();
        key = in.readString();
        parentKey = in.readString();
        filePath = in.readString();
        serverPath = in.readString();
        duration = in.readLong();
        watchedOffset = in.readLong();
        watched = PlexLibraryItem.WatchedState.values()[in.readInt()];
        frameRate = in.readString();
        releaseDate = in.readString();
        shouldStartQueuePlayback = in.readByte() == 1;
        episodeNum = in.readInt();
        seasonNum = in.readInt();
        showTitle = in.readString();
        thumbNail = in.readString();
        height = in.readInt();
        width = in.readInt();
        trailerUrl = in.readString();
        genre = in.createStringArray();
        ratings = in.createStringArray();
        sortOrder = in.readInt();
    }

    public static final Creator<Video> CREATOR = new Creator<Video>() {
        @Override
        public Video createFromParcel(Parcel in) {
            return new Video(in);
        }

        @Override
        public Video[] newArray(int size) {
            return new Video[size];
        }
    };

    @Override
    public String getKey() { return key; }

    @Override
    public String getParentKey() {
        return null;
    }

    @Override
    public boolean equals(Object m) {
        return m instanceof Video && id == ((Video) m).id;
    }

    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(category);
        dest.writeString(title);
        dest.writeString(subtitle);
        dest.writeString(description);
        dest.writeString(bgImageUrl);
        dest.writeString(cardImageUrl);
        dest.writeString(videoUrl);
        dest.writeString(studio);
        dest.writeString(rating);
        dest.writeString(key);
        dest.writeString(parentKey);
        dest.writeString(filePath);
        dest.writeString(serverPath);
        dest.writeLong(duration);
        dest.writeLong(watchedOffset);
        dest.writeInt(watched.ordinal());
        dest.writeString(frameRate);
        dest.writeString(releaseDate);
        dest.writeByte((byte) (shouldStartQueuePlayback ? 1 : 0));
        dest.writeInt(episodeNum);
        dest.writeInt(seasonNum);
        dest.writeString(showTitle);
        dest.writeString(thumbNail);
        dest.writeInt(height);
        dest.writeInt(width);
        dest.writeString(trailerUrl);
        dest.writeStringArray(genre);
        dest.writeStringArray(ratings);
        dest.writeInt(sortOrder);
    }

    @Override
    public String toString() {
        String s = "{ Video : {";
        s += "id:" + id;
        s += ", category:\"" + category + "\"";
        s += ", title:\"" + title + "\"";
        s += ", videoUrl:\"" + videoUrl + "\"";
        s += ", bgImageUrl:\"" + bgImageUrl + "\"";
        s += ", cardImageUrl:\"" + cardImageUrl + "\"";
        s += ", studio:\"" + studio + "\"";
        s += ", subtitle:\"" + subtitle + "\"";
        s += ", description:\"" + description + "\"";
        s += ", bgImageUrl:\"" + bgImageUrl + "\"";
        s += ", rating:\"" + rating + "\"";
        s += ", key:\"" + key + "\"";
        s += ", parentKey:\"" + parentKey + "\"";
        s += ", filePath:\"" + filePath + "\"";
        s += ", serverPath:\"" + serverPath + "\"";
        s += ", frameRate:\"" + frameRate + "\"";
        s += ", duration:" + duration;
        s += ", watchedOffset:" + watchedOffset;
        s += ", programId:" + programId;
        s += ", watched:" + watched.getValue();
        s += ", releaseDate:\"" + releaseDate + "\"";
        s += ", shouldStartQueuePlayback:" + (shouldStartQueuePlayback ? 1 : 0);
        s += ", episodeNum:" + episodeNum;
        s += ", seasonNum:" + seasonNum;
        s += ", showTitle:\"" + showTitle + "\"";
        s += ", thumbNail:\"" + thumbNail + "\"";
        s += ", height:" + height;
        s += ", width:" + width;
        s += ", trailerUrl:\"" + trailerUrl + "\"";
        s += ", sortOrder:" + sortOrder;
        s += ", genre:{";
        if (genre != null && genre.length > 0) {
            boolean comma = false;
            for (String g : genre) {
                if (comma)
                    s += ", ";
                comma = true;
                s += "\"" + g + "\"";
            }
        }
        s += "}";
        s += ", ratings:{";
        if (ratings != null && ratings.length > 0) {
            boolean comma = false;
            for (String r : ratings) {
                if (comma)
                    s += ", ";
                comma = true;
                s += "\"" + r + "\"";
            }
        }
        s += "}";
        s += "}}";
        return s;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getSortTitle() { return getTitle(); }

    @Override
    public String getContent() {
        return subtitle;
    }

    @Override
    public String getImageUrl(PlexServer server) {
        return server.makeServerURL(cardImageUrl);
    }

    @Override
    public Drawable getImage(Context context) {
        return null;
    }

    @Override
    public String getBackgroundImageUrl() {
        return bgImageUrl;
    }

    @Override
    public int getHeight() {
        return  R.dimen.CARD_PORTRAIT_HEIGHT;
    }

    @Override
    public int getWidth() {
        return R.dimen.CARD_PORTRAIT_WIDTH;
    }

    @Override
    public PlexLibraryItem.WatchedState getWatchedState() {
        return watched;
    }

    @Override
    public boolean updateStatus(PlexLibraryItem.WatchedState status, int totalCount, int unwatchedCount) {
        return false;
    }

    @Override
    public int getTotalLeaves() {
        return 0;
    }

    @Override
    public int getUnwatchedLeaves() {
        return 0;
    }

    @Override
    public int getViewedProgress() {
        int ret = 0;
        switch(watched) {
            case WatchedAndPartial:
            case PartialWatched:
                double val = (double) watchedOffset / duration;
                ret = (int) (val * 100);
                break;
            case Watched:
            case Unwatched:
            default:
                break;
        }
        return ret;
    }

    @SuppressWarnings("unused")
    public String getFrameRate() { return frameRate;}

    @Override
    public boolean onClicked(Fragment fragment, Bundle extras, View transitionView) {
        return false;
    }

    @Override
    public int compareTo(@NonNull Video r) {
        if (this.sortOrder < r.sortOrder)
            return -1;
        else
            return 1;
    }

    // Builder for Video object.
    public static class VideoBuilder {
        private long id = 0;
        private String category = "";
        private String title = "";
        private String subtitle = "";
        private String description = "";
        private String bgImageUrl = "";
        private String cardImageUrl = "";
        private String videoUrl = "";
        private String studio = "";
        private String rating = "";
        private String key = "";
        private String parentKey = "";
        private String filePath = "";
        private String serverPath = "";
        private long   duration = 0;
        private long   watchedOffset = 0;
        private String releaseDate = "";
        private PlexLibraryItem.WatchedState watched = Watched;
        private String frameRate = "";
        private boolean shouldStartQueuePlayback = false;
        private int episodeNum = 0;
        private int seasonNum = 0;
        private String showTitle = "";
        private String thumbNail = "";
        private int height = 0;
        private int width = 0;
        private String trailerUrl = "";
        private String[] genre = {};
        private String[] ratings = {};
        private int sortOrder = 0;

        public VideoBuilder id(long id) {
            this.id = id;
            return this;
        }

        public VideoBuilder category(String category) {
            this.category = category;
            return this;
        }

        public VideoBuilder title(String title) {
            this.title = title;
            return this;
        }

        public VideoBuilder subtitle(String subtitle) {
            this.subtitle = subtitle;
            return this;
        }

        public VideoBuilder description(String description) {
            this.description = description;
            return this;
        }

        public VideoBuilder videoUrl(String videoUrl) {
            this.videoUrl = videoUrl;
            return this;
        }

        public VideoBuilder bgImageUrl(String bgImageUrl) {
            this.bgImageUrl = bgImageUrl;
            return this;
        }

        public VideoBuilder cardImageUrl(String cardImageUrl) {
            this.cardImageUrl = cardImageUrl;
            return this;
        }

        public VideoBuilder studio(String studio) {
            this.studio = studio;
            return this;
        }

        public VideoBuilder rating(String rating) {
            this.rating = rating;
            return this;
        }

        public VideoBuilder key(String key) {
            this.key = key;
            return this;
        }

        public VideoBuilder parentKey(String parentKey) {
            this.parentKey = parentKey;
            return this;
        }

        public VideoBuilder filepath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public VideoBuilder serverpath(String serverPath) {
            this.serverPath = serverPath;
            return this;
        }

        public VideoBuilder duration(long duration) {
            this.duration = duration;
            return this;
        }

        public VideoBuilder watchedOffset(long watchedOffset) {
            this.watchedOffset = watchedOffset;
            return this;
        }

        public VideoBuilder watched(PlexLibraryItem.WatchedState watched) {
            this.watched = watched;
            return this;
        }

        public VideoBuilder releaseDate(String releaseDate) {
            this.releaseDate = releaseDate;
            return this;
        }

        public VideoBuilder frameRate(String frameRate) {
            this.frameRate = frameRate;
            return this;
        }

        public VideoBuilder shouldStartQueuePlayback(boolean shouldStartQueuePlayback) {
            this.shouldStartQueuePlayback = shouldStartQueuePlayback;
            return this;
        }

        public VideoBuilder episodeNum(int num) {
            this.episodeNum = num;
            return this;
        }

        public VideoBuilder seasonNum(int num) {
            this.seasonNum = num;
            return this;
        }

        public VideoBuilder showTitle(String title) {
            this.showTitle = title;
            return this;
        }

        public VideoBuilder thumbNail(String url) {
            this.thumbNail = url;
            return this;
        }

        public VideoBuilder height(int val) {
            this.height = val;
            return this;
        }

        public VideoBuilder width(int val) {
            this.width = val;
            return this;
        }

        public VideoBuilder trailerUrl(String val) {
            this.trailerUrl = val;
            return this;
        }

        public VideoBuilder genre(String[] vals) {
            this.genre = vals;
            return this;
        }

        public VideoBuilder ratings(String[] vals) {
            this.ratings = vals;
            return this;
        }

        public VideoBuilder sortOrder(int sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        public Video buildFromMediaDesc(MediaDescription desc) {
            return new Video(
                    Long.parseLong(desc.getMediaId()),
                    "", // Category - not provided by MediaDescription.
                    String.valueOf(desc.getTitle()),
                    String.valueOf(desc.getSubtitle()),
                    String.valueOf(desc.getDescription()),
                    "", // Media URI - not provided by MediaDescription.
                    "", // Background Image URI - not provided by MediaDescription.
                    String.valueOf(desc.getIconUri()),
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    0,
                    0,
                    Watched,
                    "",
                    "",
                    false,
                    0,
                    0,
                    "",
                    "",
                    0,
                    0,
                    "",
                    null,
                    null,
                    0
            );
        }

        public Video build() {
            return new Video(
                    id,
                    category,
                    title,
                    subtitle,
                    description,
                    videoUrl,
                    bgImageUrl,
                    cardImageUrl,
                    studio,
                    rating,
                    key,
                    parentKey,
                    filePath,
                    serverPath,
                    duration,
                    watchedOffset,
                    watched,
                    frameRate,
                    releaseDate,
                    shouldStartQueuePlayback,
                    episodeNum,
                    seasonNum,
                    showTitle,
                    thumbNail,
                    height,
                    width,
                    trailerUrl,
                    genre,
                    ratings,
                    sortOrder
            );
        }
    }
}