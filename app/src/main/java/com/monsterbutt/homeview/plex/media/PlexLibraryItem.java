package com.monsterbutt.homeview.plex.media;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.annotations.SerializedName;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.PlexServerManager;
import com.monsterbutt.homeview.plex.tasks.DeleteTask;
import com.monsterbutt.homeview.plex.tasks.SetProgressTask;
import com.monsterbutt.homeview.ui.presenters.CardObject;
import com.monsterbutt.homeview.ui.presenters.CardPresenter;

import java.util.ArrayList;
import java.util.List;

import us.nineworlds.plex.rest.model.impl.Hub;

import static com.monsterbutt.homeview.ui.C.StatusChanged.SetUnwatched;
import static com.monsterbutt.homeview.ui.C.StatusChanged.SetWatched;

public abstract class PlexLibraryItem {


    public enum WatchedState {

        @SerializedName("0")
        Watched(0),
        @SerializedName("1")
        Unwatched(1),
        @SerializedName("2")
        PartialWatched(2),
        @SerializedName("3")
        WatchedAndPartial(3),
        @SerializedName("4")
        Removed(4);

        private final int value;
        public int getValue() {
            return value;
        }

        WatchedState(int value) {
            this.value = value;
        }
    }

    static final int COMPOSITE_COLS = 3;
    static final int COMPOSITE_ROW = 2;
    static final int COMPOSITE_HEIGHT = 320;
    static final int COMPOSITE_WIDTH = 480;

    public abstract String getKey();
    public String getParentKey() { return ""; }

    public abstract long getRatingKey();

    public String getRating() { return ""; }
    public String getOriginalAvailableDate() { return ""; }
    public long getLastViewedAt() { return 0; }
    public long getDuration() { return 0; }

    public abstract String getSectionId();

    public abstract String getType();

    public abstract String getTitle();

    public abstract String getSortTitle();

    public abstract String getThumbnailKey();

    public abstract long getAddedAt();

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

    public String getDetailDuration(Context context) {
        return "";
    }

    public String getDetailStudioPath(PlexServer server) {
        return "";
    }

    public String getDetailRatingPath(PlexServer server) { return ""; }

    public String getDetailGenre(Context context) { return ""; }

    public WatchedState getWatchedState() { return WatchedState.Watched; }

    public int getUnwatchedCount() { return 0; }

    public int getTotalLeafCount() { return 0; }

    public int getViewedProgress() { return 0; }
    public long getViewedOffset() { return 0; }

    public void setStatus(WatchedState status) { }

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

    public abstract void fillQueryRow(MatrixCursor.RowBuilder row, Context context, String keyOverride, String yearOverride);

    public List<Hub> getRelated() {
        return null;
    }

    public void toggleWatched() {}

    public boolean onLongClicked(CardObject obj, final Fragment fragment, final Bundle extras,
                                 final View transitionView, CardPresenter.LongClickWatchStatusCallback callback) {
        PlexServer server = PlexServerManager.getInstance().getSelectedServer();
        final List<ActionChoice> values = new ArrayList<>();
        values.add(new PlayChoice());
        values.add(new DetailsChoice());
        values.add(new DeleteChoice(server, obj, callback));
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

        ActionChoice(int drawable, int text) {
            this.drawable = drawable;
            this.text = text;
        }
        public abstract void selected(Fragment fragment, final Bundle extras, final View transitionView);
    }

    private class PlayChoice extends ActionChoice {

        PlayChoice() { super(R.drawable.ic_play_circle_outline_white_48dp, R.string.action_choice_play); }
        @Override
        public void selected(Fragment fragment, final Bundle extras, final View transitionView) {
            PlexLibraryItem.this.onPlayPressed(fragment, extras, transitionView);
        }
    }

    private class DetailsChoice extends ActionChoice {

        DetailsChoice() { super(R.drawable.ic_art_track_white_48dp, R.string.action_choice_details); }
        @Override
        public void selected(Fragment fragment, final Bundle extras, final View transitionView) {
            PlexLibraryItem.this.onClicked(fragment, extras, transitionView);
        }
    }

    private class DeleteChoice extends ActionChoice {

        private final PlexServer server;
        private final CardPresenter.LongClickWatchStatusCallback callback;
        private final CardObject obj;

        DeleteChoice(PlexServer server, CardObject obj,
                            CardPresenter.LongClickWatchStatusCallback callback) {
            super(R.drawable.ic_delete_white_48dp, R.string.delete);
            this.server = server;
            this.callback = callback;
            this.obj = obj;
        }
        @Override
        public void selected(final Fragment fragment, final Bundle extras, final View transitionView) {
            new DeleteTask(PlexLibraryItem.this, server, fragment,
             false, obj, callback);
        }

    }

    private class SetUnwatchedChoice extends ActionChoice {

        final CardObject obj;
        final PlexServer server;
        final CardPresenter.LongClickWatchStatusCallback callback;
        SetUnwatchedChoice(CardObject obj, PlexServer server,
                                  CardPresenter.LongClickWatchStatusCallback callback) {
            super(R.drawable.ic_watch_later_white_48dp, R.string.action_choice_setunwatched);
            this.callback = callback;
            this.obj = obj;
            this.server = server;
        }
        @Override
        public void selected(Fragment fragment, final Bundle extras, final View transitionView) {
            new SetProgressTask(server, PlexLibraryItem.this.getKey(),
                    Long.toString(PlexLibraryItem.this.getRatingKey()),
             SetUnwatched).execute(obj.getContext());
            setStatus(WatchedState.Unwatched);
            if (callback != null)
                callback.resetSelected(obj);
        }
    }

    private class SetWatchedChoice extends ActionChoice {

        final CardObject obj;
        final PlexServer server;
        final CardPresenter.LongClickWatchStatusCallback callback;
        SetWatchedChoice(CardObject obj, PlexServer server, CardPresenter.LongClickWatchStatusCallback callback) {
            super(R.drawable.ic_visibility_white_48dp, R.string.action_choice_setwatched);
            this.server = server;
            this.obj = obj;
            this.callback = callback;
        }
        @Override
        public void selected(Fragment fragment, final Bundle extras, final View transitionView) {
            new SetProgressTask(server, PlexLibraryItem.this.getKey(),
                    Long.toString(PlexLibraryItem.this.getRatingKey()),
             SetWatched).execute(obj.getContext());
            setStatus(WatchedState.Watched);
            if (callback != null)
                callback.resetSelected(obj);
        }
    }

    private static class ActionChoiceArrayAdapter extends ArrayAdapter<ActionChoice> {

        private final Context context;
        private final List<ActionChoice> values;

        ActionChoiceArrayAdapter(Context context, List<ActionChoice> values) {
            super(context, R.layout.lb_actionchoiceitem, values);
            this.context = context;
            this.values = values;
        }

        @NonNull
        @Override
        public View getView(int position, View row, @NonNull ViewGroup parent) {
            View rowView = row;
            if (rowView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                assert inflater != null;
                rowView = inflater.inflate(R.layout.lb_actionchoiceitem, parent, false);
            }
            final ActionChoice item = values.get(position);
            ((ImageView) rowView.findViewById(R.id.actionimage)).setImageDrawable(context.getDrawable(item.drawable));
            ((TextView) rowView.findViewById(R.id.actiontext)).setText(context.getString(item.text));

            return rowView;
        }
    }

    protected com.monsterbutt.homeview.model.Video.VideoBuilder toVideo(Context context,
                                                                        com.monsterbutt.homeview.model.Video.VideoBuilder builder,
                                                                        PlexServer server) {
        return builder
         .id(getRatingKey())
         .watched(getWatchedState())
         .releaseDate(getOriginalAvailableDate());
    }

    public com.monsterbutt.homeview.model.Video toVideo(Context context, PlexServer server) {

        return toVideo(context, new com.monsterbutt.homeview.model.Video.VideoBuilder(), server).build();
    }

    public boolean updateCounts(int totalCount, int unwatchedCount) {
        return false;
    }
}