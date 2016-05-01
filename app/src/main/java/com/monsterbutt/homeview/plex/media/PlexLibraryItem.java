package com.monsterbutt.homeview.plex.media;

import android.app.Fragment;
import android.content.Context;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.view.View;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.presenters.CardPresenter;
import com.monsterbutt.homeview.presenters.PosterCard;
import com.monsterbutt.homeview.presenters.SceneCard;
import com.monsterbutt.homeview.settings.SettingsManager;

import java.util.List;

import us.nineworlds.plex.rest.model.impl.Hub;


public abstract class PlexLibraryItem {

    public enum WatchedState {

        Watched,
        Unwatched,
        PartialWatched,
        WatchedAndPartial
    }

    ;

    static public final int COMPOSITE_COLS = 3;
    static public final int COMPOSITE_ROW = 2;
    static public final int COMPOSITE_HEIGHT = 320;
    static public final int COMPOSITE_WIDTH = 480;

    public abstract String getKey();

    public abstract long getRatingKey();

    public abstract String getSectionId();

    public abstract String getSectionTitle();

    public abstract String getType();

    public abstract String getTitle();

    public abstract String getSortTitle();

    public abstract String getThumbnailKey();

    public abstract String getArt();

    public abstract long getAddedAt();

    public abstract long getUpdatedAt();

    public abstract String getSummary();

    public abstract String getCardTitle(Context context);

    public abstract String getCardContent(Context context);

    public int getCardImageId() {
        return 0;
    }

    public abstract String getCardImageURL();

    public abstract String getWideCardTitle(Context context);

    public abstract String getWideCardContent(Context context);

    public abstract String getWideCardImageURL();

    public abstract String getBackgroundImageURL();

    public String getDetailTitle(Context context) {
        return getTitle();
    }

    public String getDetailSubtitle(Context context) {
        return getWideCardContent(context);
    }

    public String getDetailContent(Context context) {
        return "";
    }

    public String getDetailYear(Context context) {
        return "";
    }

    public String getDetailDuration(Context context) {
        return "";
    }

    public String getDetailStudioPath(PlexServer server) {
        return "";
    }

    public String getDetailRatingPath(PlexServer server) { return ""; }

    public String getDetailGenre(Context context) { return ""; }

    public abstract WatchedState getWatchedState();

    public abstract int getUnwatchedCount();

    public abstract int getViewedProgress();

    public String getThemeKey(PlexServer server) {
        return "";
    }

    public abstract boolean useItemBackgroundArt();

    public abstract String getHeaderForChildren(Context context);

    public abstract List<PlexLibraryItem> getChildrenItems();

    public List<PlexLibraryItem> getExtraItems() {

        return null;
    }

    public abstract boolean onClicked(Fragment fragment, Bundle extras, View transitionView);

    public abstract boolean onPlayPressed(Fragment fragment, Bundle extras, View transitionView);

    public abstract void fillQueryRow(MatrixCursor.RowBuilder row, Context context, String keyOverride, String yearOverride, boolean isStartOverride);

    public ListRow getChildren(Context context, PlexServer server) {

        boolean skipAllSeason = (this instanceof Show) &&
                !SettingsManager.getInstance(context).getBoolean("preferences_navigation_showallseason");

        ListRow row = null;
        List<PlexLibraryItem> children = getChildrenItems();
        if (children != null && !children.isEmpty()) {
            // setup bottom row for seasons, episodes, or chapters
            ArrayObjectAdapter adapter = new ArrayObjectAdapter(new CardPresenter(server));
            row = new ListRow(new HeaderItem(0, getHeaderForChildren(context)),
                    adapter);

            for (PlexLibraryItem child : children) {

                if (skipAllSeason && child.getKey().endsWith(Season.ALL_SEASONS))
                    continue;

                adapter.add((this instanceof Season || this instanceof Episode || this instanceof Movie)
                        ? new SceneCard(context, child)
                        : new PosterCard(context, child));
            }
        }
        return row;
    }

    public ListRow getExtras(Context context, PlexServer server) {

        String title = context != null ? context.getString(R.string.extras_row_header) : "Extras";
        ListRow row = null;
        List<PlexLibraryItem> extras = getExtraItems();
        if (extras != null && !extras.isEmpty()) {
            // setup bottom row for seasons, episodes, or chapters
            ArrayObjectAdapter adapter = new ArrayObjectAdapter(new CardPresenter(server));
            row = new ListRow(new HeaderItem(0, title), adapter);
            for (PlexLibraryItem extra : extras)
                adapter.add(new SceneCard(context, extra));
        }

        return row;
    }

    public List<Hub> getRelated() {
        return null;
    }

    public void toggleWatched() {}
}