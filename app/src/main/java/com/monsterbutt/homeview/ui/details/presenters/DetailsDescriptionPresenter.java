/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.monsterbutt.homeview.ui.details.presenters;

import android.content.Context;
import android.view.View;

import com.monsterbutt.homeview.player.track.MediaTrackSelector;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.plex.media.Stream;
import com.monsterbutt.homeview.plex.media.VideoFormat;
import com.monsterbutt.homeview.ui.details.interfaces.IDetailsItem;

import us.nineworlds.plex.rest.model.impl.Media;

public class DetailsDescriptionPresenter extends AbstractDetailsDescriptionPresenter {


    public DetailsDescriptionPresenter(Context context, PlexServer server) {
        super(context, server);
    }

    @Override
    protected void onBindDescription(ViewHolder viewHolder, Object obj) {

        if (obj != null && obj instanceof IDetailsItem) {

            PlexLibraryItem video = ((IDetailsItem) obj).item();
            viewHolder.Title.setText(video.getDetailTitle(context));
            viewHolder.Subtitle.setText(video.getDetailSubtitle(context));
            viewHolder.Description.setText(video.getSummary());
            viewHolder.Genre.setText(video.getDetailGenre(context));
            viewHolder.Tagline.setText(video.getDetailContent(context));
            viewHolder.setImage(context, viewHolder.Studio, video.getDetailStudioPath(server));
            viewHolder.setImage(context, viewHolder.Rating, video.getDetailRatingPath(server));
            boolean watched = video.getWatchedState() == PlexLibraryItem.WatchedState.Watched;
            viewHolder.Watched.setVisibility(watched ? View.INVISIBLE : View.VISIBLE);
            int progress = watched ? 0 : video.getViewedProgress();
            viewHolder.Progress.setProgress(progress);

            if (video instanceof PlexVideoItem) {

                PlexVideoItem item = (PlexVideoItem) video;
                Media media = item.getMedia() != null ? item.getMedia().get(0) : null;
                if (media != null) {
                    viewHolder.setImage(context, viewHolder.FrameRate, server.makeServerURLForCodec(VideoFormat.VideoFrameRate, media.getVideoFrameRate()));
                } else {
                    viewHolder.FrameRate.setVisibility(View.INVISIBLE);
                }

                MediaTrackSelector tracks = ((IDetailsItem) obj).tracks();
                if (tracks != null) {
                    Stream videoStream = tracks.getSelectedTrack(Stream.Video_Stream);
                    if (videoStream != null) {
                        viewHolder.setImage(context, viewHolder.VideoCodec, server.makeServerURLForCodec(VideoFormat.VideoCodec, videoStream.getCodec()));
                        viewHolder.setImage(context, viewHolder.Resolution, media != null ?
                         server.makeServerURLForCodec(VideoFormat.VideoResolution, media.getVideoResolution()) : "");
                    } else {
                        viewHolder.VideoCodec.setVisibility(View.INVISIBLE);
                        viewHolder.Resolution.setVisibility(View.INVISIBLE);
                    }

                    Stream audioStream = tracks.getSelectedTrack(Stream.Audio_Stream);
                    if (audioStream != null) {
                        viewHolder.setImage(context, viewHolder.AudioCodec, server.makeServerURLForCodec(Stream.AudioCodec, audioStream.getCodecAndProfile()));
                        viewHolder.setImage(context, viewHolder.AudioChannels, server.makeServerURLForCodec(Stream.AudioChannels, audioStream.getChannels()));
                    } else {
                        viewHolder.AudioCodec.setVisibility(View.INVISIBLE);
                        viewHolder.AudioChannels.setVisibility(View.INVISIBLE);
                    }
                }
                else {
                    viewHolder.VideoCodec.setVisibility(View.INVISIBLE);
                    viewHolder.Resolution.setVisibility(View.INVISIBLE);
                    viewHolder.AudioCodec.setVisibility(View.INVISIBLE);
                    viewHolder.AudioChannels.setVisibility(View.INVISIBLE);
                }
            }
        }
    }
}
