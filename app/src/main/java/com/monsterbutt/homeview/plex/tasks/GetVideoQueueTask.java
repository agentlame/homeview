package com.monsterbutt.homeview.plex.tasks;

import android.text.TextUtils;

import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.Episode;
import com.monsterbutt.homeview.plex.media.PlexContainerItem;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.plex.media.Season;
import com.monsterbutt.homeview.plex.media.Show;

import java.util.ArrayList;
import java.util.List;

import us.nineworlds.plex.rest.model.impl.Directory;
import us.nineworlds.plex.rest.model.impl.MediaContainer;
import us.nineworlds.plex.rest.model.impl.Video;


public class GetVideoQueueTask extends PlexServerTask {


    List<PlexVideoItem> mList = new ArrayList<>();
    public List<PlexVideoItem> getQueue() { return mList; }

    public GetVideoQueueTask(PlexServerTaskCaller caller, PlexServer server) {
        super(caller, server);
    }

    @Override
    protected Boolean doInBackground(Object... params) {

        if (params == null || params.length == 0)
            return false;

        PlexServer server = getServer();
        String metadataKey = (String) params[0];
        if (!TextUtils.isEmpty(metadataKey) && server != null) {

            final long BAD_RATING_KEY = 0;
            long keyCheck = BAD_RATING_KEY;
            MediaContainer mc = server.getVideoMetadata(metadataKey);
            if (mc.getDirectories() != null) {

                String viewGroup = mc.getViewGroup();
                if (viewGroup.equals(Season.TYPE)) {

                    // show seasons, get all episodes of show, first unwatched is current
                    Directory all = mc.getDirectories().get(0);
                    mc = server.getVideoMetadata(all.getKey().replace(PlexContainerItem.CHILDREN, Season.ALL_SEASONS));
                }
                else if (viewGroup.equals(Show.TYPE)) {

                    // all shows, get all episodes of all shows, first unwatched is current
                    mc.setViewGroup(Episode.TYPE);
                    for (Directory dir : mc.getDirectories()) {

                        MediaContainer show = server.getVideoMetadata(
                                dir.getKey().replace(PlexContainerItem.CHILDREN, Season.ALL_SEASONS));
                        if (show != null && show.getVideos() != null)
                            mc.getVideos().addAll(show.getVideos());
                    }
                }
            }
            else if (mc.getVideos() != null) {

                List<Video> vids = mc.getVideos();
                String viewGroup = mc.getViewGroup();
                if (TextUtils.isEmpty(viewGroup)) {

                    Video vid = vids.get(0);
                    // current is episode rating key
                    keyCheck = vid.getRatingKey();
                    String grandParentRatingKey = vid.getGrandparentKey();
                    if (TextUtils.isEmpty(grandParentRatingKey)) {

                        // none , no grandparent, movie get section
                        if (!TextUtils.isEmpty(vid.getLibrarySectionID()))
                            mc = server.getSectionFilter(mc.getLibrarySectionID(), PlexContainerItem.ALL);
                    }
                    else {

                        // no viewgroup with grandparent is a episode, get all episodes for show
                        mc = server.getVideoMetadata(vid.getKey().replace(Long.toString(keyCheck),
                                Long.toString(vid.getGrandparentRatingKey()))
                                + "/" + Season.ALL_SEASONS);
                    }
                }
                else if (viewGroup.equals(Episode.TYPE)){

                    //season, viewgroup is episode,  get show -> all seasons (if not already)
                    // current is first unwatched in list (or first)
                    Video first = vids.get(0);
                    keyCheck = first.getRatingKey();
                    for (Video vid : vids) {

                        if (vid.getViewCount() == 0) {

                            keyCheck = vid.getRatingKey();
                            break;
                        }
                    }
                    String showKey = first.getKey().replace(Long.toString(first.getRatingKey()),
                            Long.toString(first.getGrandparentRatingKey()))
                            + "/" + Season.ALL_SEASONS;
                    mc = server.getVideoMetadata(showKey);
                }
            }
            // movie, use list, current is first unwatched
            // we use previously constructed list here and find first if needed
            if (mc != null && mc.getVideos() != null) {

                for (Video vid : mc.getVideos()) {

                    boolean isFirst = (keyCheck == BAD_RATING_KEY && vid.getViewCount() == 0)
                            || keyCheck == vid.getRatingKey();
                    if (isFirst)
                        keyCheck = vid.getRatingKey();
                    PlexVideoItem item = PlexVideoItem.getItem(vid);
                    if (item != null) {
                        item.setShouldPlayFirst(isFirst);
                        mList.add(item);
                    }
                }
            }
        }
        return true;
    }
}
