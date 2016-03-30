package com.monsterbutt.homeview.presenters;

import android.content.Context;
import android.text.TextUtils;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;


public class SceneCard extends PosterCard {

    public SceneCard(Context context, PlexLibraryItem obj) {
        super(context, obj);
    }


    @Override
    public String getKey() { return item.getKey(); }

    @Override
    public String getTitle() {
        return item.getWideCardTitle(mContext);
    }

    @Override
    public String getContent() {
        return item.getWideCardContent(mContext);
    }

    @Override
    public String getImageUrl(PlexServer server) {

        String url = item.getWideCardImageURL();
        if (!TextUtils.isEmpty(url))
            url = server.makeServerURL(url);
        return url;
    }

    @Override
    public int getHeight() {
        return R.dimen.CARD_LANDSCAPE_HEIGHT;
    }

    @Override
    public int getWidth() {
        return R.dimen.CARD_LANDSCAPE_WIDTH;
    }

    public String getSectionId() {
        return item.getSectionId();
    }
}
