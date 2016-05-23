package com.monsterbutt.homeview.plex.media;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.tasks.SetProgressTask;
import com.monsterbutt.homeview.presenters.CardObject;
import com.monsterbutt.homeview.presenters.CardPresenter;
import com.monsterbutt.homeview.presenters.SceneCard;
import com.monsterbutt.homeview.settings.SettingsManager;
import com.monsterbutt.homeview.ui.PlexItemRow;
import com.monsterbutt.homeview.ui.handler.WatchedStatusHandler;

import java.util.ArrayList;
import java.util.List;

import us.nineworlds.plex.rest.model.impl.Hub;


public abstract class PlexLibraryItem {

    public enum WatchedState {

        Watched,
        Unwatched,
        PartialWatched,
        WatchedAndPartial
    }

    static public final int COMPOSITE_COLS = 3;
    static public final int COMPOSITE_ROW = 2;
    static public final int COMPOSITE_HEIGHT = 320;
    static public final int COMPOSITE_WIDTH = 480;

    public abstract String getKey();

    public abstract long getRatingKey();

    public String getRating() { return ""; }
    public String getOriginalAvailableDate() { return ""; }
    public long getLastViewedAt() { return 0; }
    public long getDuration() { return 0; }

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

    public WatchedState getWatchedState() { return WatchedState.Watched; };

    public int getUnwatchedCount() { return 0; };

    public int getViewedProgress() { return 0; };
    public long getViewedOffset() { return 0; };

    public void setStatus(WatchedStatusHandler.UpdateStatus status) {}

    public String getThemeKey(PlexServer server) {
        return "";
    }

    public abstract boolean useItemBackgroundArt();

    public abstract String getHeaderForChildren(Context context);
    public boolean shouldChildRowWatchState() { return false; }

    public abstract List<PlexLibraryItem> getChildrenItems();

    public List<PlexLibraryItem> getExtraItems() {

        return null;
    }

    public abstract boolean onClicked(Fragment fragment, Bundle extras, View transitionView);

    public abstract boolean onPlayPressed(Fragment fragment, Bundle extras, View transitionView);

    public abstract void fillQueryRow(MatrixCursor.RowBuilder row, Context context, String keyOverride, String yearOverride, boolean isStartOverride);

    public PlexItemRow getChildren(Context context, PlexServer server, CardPresenter.CardPresenterLongClickListener listener) {

        boolean skipAllSeason = (this instanceof Show) &&
                !SettingsManager.getInstance(context).getBoolean("preferences_navigation_showallseason");

        PlexItemRow row = null;
        List<PlexLibraryItem> children = getChildrenItems();
        if (children != null && !children.isEmpty()) {
            // setup bottom row for seasons, episodes, or chapters
            if (shouldChildRowWatchState())
                row = PlexItemRow.getWatchedStateRow(server, getHeaderForChildren(context), null, listener);
            else
                row = PlexItemRow.getRow(server, getHeaderForChildren(context), null, listener);
            for (PlexLibraryItem child : children) {

                if (skipAllSeason && child.getKey().endsWith(Season.ALL_SEASONS))
                    continue;
                row.addItem(context, child, this instanceof Season || this instanceof Episode || this instanceof Movie);
            }
        }
        return row;
    }

    public ListRow getExtras(Context context, PlexServer server, CardPresenter.CardPresenterLongClickListener listener) {

        String title = context != null ? context.getString(R.string.extras_row_header) : "Extras";
        ListRow row = null;
        List<PlexLibraryItem> extras = getExtraItems();
        if (extras != null && !extras.isEmpty()) {
            // setup bottom row for seasons, episodes, or chapters
            ArrayObjectAdapter adapter = new ArrayObjectAdapter(new CardPresenter(server, listener));
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

    public boolean onLongClicked(CardObject obj, PlexServer server, final Fragment fragment, final Bundle extras,
                                 final View transitionView, CardPresenter.LongClickWatchStatusCallback callback) {

        final List<ActionChoice> values = new ArrayList<>();
        values.add(new PlayChoice());
        values.add(new DetailsChoice());
        WatchedState state = getWatchedState();
        if (state != WatchedState.Unwatched)
            values.add(new SetUnwatchedChoice(obj, server, callback));
        if (state != WatchedState.Watched)
            values.add(new SetWatchedChoice(obj, server, callback));
        DialogInterface.OnClickListener dialogCB = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                values.get(which).selected(fragment, extras, transitionView);
            }
        };
        Activity activity = fragment.getActivity();
        AlertDialog dialog = new AlertDialog.Builder(activity, R.style.AlertDialogStyle)
                .setIcon(R.drawable.launcher)
                .setTitle(R.string.action_dialog)
                .setAdapter(new ActionChoiceArrayAdapter(activity, values), dialogCB)
                .create();
        dialog.show();
        return true;
    }

    private abstract class ActionChoice {

        public final int drawable;
        public final int text;

        public ActionChoice(int drawable, int text) {
            this.drawable = drawable;
            this.text = text;
        }
        public abstract void selected(Fragment fragment, final Bundle extras, final View transitionView);
    }

    private class PlayChoice extends ActionChoice {

        public PlayChoice() { super(R.drawable.ic_play_circle_outline_white_48dp, R.string.action_choice_play); }
        @Override
        public void selected(Fragment fragment, final Bundle extras, final View transitionView) {
            PlexLibraryItem.this.onPlayPressed(fragment, extras, transitionView);
        }
    }

    private class DetailsChoice extends ActionChoice {

        public DetailsChoice() { super(R.drawable.ic_art_track_white_48dp, R.string.action_choice_details); }
        @Override
        public void selected(Fragment fragment, final Bundle extras, final View transitionView) {
            PlexLibraryItem.this.onClicked(fragment, extras, transitionView);
        }
    }

    private class SetUnwatchedChoice extends ActionChoice {

        final CardObject obj;
        final PlexServer server;
        final CardPresenter.LongClickWatchStatusCallback callback;
        public SetUnwatchedChoice(CardObject obj, PlexServer server, CardPresenter.LongClickWatchStatusCallback callback) {
            super(R.drawable.ic_watch_later_white_48dp, R.string.action_choice_setunwatched);
            this.callback = callback;
            this.obj = obj;
            this.server = server;
        }
        @Override
        public void selected(Fragment fragment, final Bundle extras, final View transitionView) {
            new SetProgressTask(new SetProgressTask.VideoId(server, PlexLibraryItem.this.getKey(),
                    Long.toString(PlexLibraryItem.this.getRatingKey()))).execute(SetProgressTask.UNWATCHED);
            setStatus(new WatchedStatusHandler.UpdateStatus(getKey(), 0, WatchedState.Unwatched));
            if (callback != null)
                callback.resetSelected(obj);
        }
    }

    private class SetWatchedChoice extends ActionChoice {

        final CardObject obj;
        final PlexServer server;
        final CardPresenter.LongClickWatchStatusCallback callback;
        public SetWatchedChoice(CardObject obj, PlexServer server, CardPresenter.LongClickWatchStatusCallback callback) {
            super(R.drawable.ic_visibility_white_48dp, R.string.action_choice_setwatched);
            this.server = server;
            this.obj = obj;
            this.callback = callback;
        }
        @Override
        public void selected(Fragment fragment, final Bundle extras, final View transitionView) {
            new SetProgressTask(new SetProgressTask.VideoId(server, PlexLibraryItem.this.getKey(),
                    Long.toString(PlexLibraryItem.this.getRatingKey()))).execute(SetProgressTask.WATCHED);
            setStatus(new WatchedStatusHandler.UpdateStatus(getKey(), 0, WatchedState.Watched));
            if (callback != null)
                callback.resetSelected(obj);
        }
    }

    private static class ActionChoiceArrayAdapter extends ArrayAdapter<ActionChoice> {

        private final Context context;
        private final List<ActionChoice> values;

        public ActionChoiceArrayAdapter(Context context, List<ActionChoice> values) {
            super(context, R.layout.lb_actionchoiceitem, values);
            this.context = context;
            this.values = values;
        }

        @Override
        public View getView(int position, View row, ViewGroup parent) {
            View rowView = row;
            if (rowView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                rowView = inflater.inflate(R.layout.lb_actionchoiceitem, parent, false);
            }
            final ActionChoice item = values.get(position);
            ((ImageView) rowView.findViewById(R.id.actionimage)).setImageDrawable(context.getDrawable(item.drawable));
            ((TextView) rowView.findViewById(R.id.actiontext)).setText(context.getString(item.text));

            return rowView;
        }
    }
}