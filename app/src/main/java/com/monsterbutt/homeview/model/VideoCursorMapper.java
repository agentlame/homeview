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

import android.database.Cursor;
import android.support.v17.leanback.database.CursorMapper;

import com.monsterbutt.homeview.data.VideoContract;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;

/**
 * VideoCursorMapper maps a database Cursor to a Video object.
 */
public final class VideoCursorMapper extends CursorMapper {

    private static int idIndex;
    private static int nameIndex;
    private static int subtitleIndex;
    private static int descIndex;
    private static int videoUrlIndex;
    private static int bgImageUrlIndex;
    private static int cardImageUrlIndex;
    private static int studioIndex;
    private static int categoryIndex;
    private static int keyIndex;
    private static int filePathIndex;
    private static int serverPathIndex;
    private static int durationIndex;
    private static int watchedOffsetIndex;
    private static int watchedIndex;
    private static int shouldStartIndex;
    private static int frameRateIndex;

    @Override
    protected void bindColumns(Cursor cursor) {
        idIndex = cursor.getColumnIndex(VideoContract.VideoEntry._ID);
        nameIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_NAME);
        subtitleIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_SUBTITLE);
        descIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_DESC);
        videoUrlIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_VIDEO_URL);
        bgImageUrlIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_BG_IMAGE_URL);
        cardImageUrlIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_CARD_IMG);
        studioIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_STUDIO);
        categoryIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_CATEGORY);
        keyIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_KEY);
        filePathIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_FILEPATH);
        serverPathIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_SERVERPATH);
        durationIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_DURATION);
        watchedOffsetIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_WATCHEDOFFSET);
        watchedIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_WATCHED);
        shouldStartIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_SHOULDSTART);
        frameRateIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_FRAMERATE);
    }

    @Override
    protected Object bind(Cursor cursor) {

        // Get the values of the video.
        long id = cursor.getLong(idIndex);
        String category = cursor.getString(categoryIndex);
        String title = cursor.getString(nameIndex);
        String subtitle = cursor.getString(subtitleIndex);
        String desc = cursor.getString(descIndex);
        String videoUrl = cursor.getString(videoUrlIndex);
        String bgImageUrl = cursor.getString(bgImageUrlIndex);
        String cardImageUrl = cursor.getString(cardImageUrlIndex);
        String studio = cursor.getString(studioIndex);
        String key = cursor.getString(keyIndex);
        String filePath = cursor.getString(filePathIndex);
        String serverPath = cursor.getString(serverPathIndex);
        long   duration = cursor.getLong(durationIndex);
        long   watchedOffset = cursor.getLong(watchedOffsetIndex);
        PlexLibraryItem.WatchedState watched = PlexLibraryItem.WatchedState.values()[cursor.getInt(watchedIndex)];
        boolean shouldStartQueuePlayback =  1 == cursor.getShort(shouldStartIndex);
        String frameRate = cursor.getString(frameRateIndex);

        // Build a Video object to be processed.
        return new Video.VideoBuilder()
                .id(id)
                .title(title)
                .subtitle(subtitle)
                .category(category)
                .description(desc)
                .videoUrl(videoUrl)
                .bgImageUrl(bgImageUrl)
                .cardImageUrl(cardImageUrl)
                .studio(studio)
                .key(key)
                .filepath(filePath)
                .serverpath(serverPath)
                .duration(duration)
                .watched(watched)
                .watchedOffset(watchedOffset)
                .frameRate(frameRate)
                .shouldStartQueuePlayback(shouldStartQueuePlayback)
                .build();
    }
}
