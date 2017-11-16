package com.monsterbutt.homeview.plex.tasks;

import com.monsterbutt.homeview.player.track.MediaTrackSelector;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;

import us.nineworlds.plex.rest.model.impl.MediaContainer;


public class GetVideoTask extends PlexServerTask {

    private PlexVideoItem mVideo = null;
    public PlexVideoItem getVideo() { return mVideo; }

    private MediaTrackSelector mChosenTracks = null;
    public MediaTrackSelector getChosenTracks() { return mChosenTracks; }
    public GetVideoTask setChosenTracks(MediaTrackSelector tracks) {
        mChosenTracks = tracks;
        return this;
    }


    public GetVideoTask(PlexServerTaskCaller caller, PlexServer server) {
        super(caller, server);
    }

    @Override
    protected Boolean doInBackground(Object... params) {

        if (params == null || params.length == 0)
            return false;

        PlexServer server = getServer();
        String metadataKey = (String) params[0];
        if (!metadataKey.isEmpty() && server != null) {

            MediaContainer mc = server.getVideoMetadata(metadataKey);
            if (mc != null && mc.getVideos() != null)
                mVideo = PlexVideoItem.getItem(mc.getVideos().get(0));
        }

        return true;
    }
}
