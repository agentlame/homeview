package com.monsterbutt.homeview.ui.details;


import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v17.leanback.app.BaseFragment;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.SparseArrayObjectAdapter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.Episode;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.plex.tasks.DeleteTask;
import com.monsterbutt.homeview.ui.details.interfaces.IDetailsItem;
import com.monsterbutt.homeview.ui.details.interfaces.IDetailsItemUpdateNotifier;
import com.monsterbutt.homeview.ui.details.interfaces.IDetailsItemUpdateListener;
import com.monsterbutt.homeview.ui.interfaces.ICardSelectionListener;


public class DetailsOverviewRow extends android.support.v17.leanback.widget.DetailsOverviewRow
 implements OnActionClickedListener, IDetailsItemUpdateListener {

  private final static int ACTION_PLAY        = 1;
  private final static int ACTION_VIEWSTATUS  = 2;
  private final static int ACTION_DELETE      = 5;

  private final BaseFragment fragment;

  DetailsOverviewRow(final BaseFragment fragment, IDetailsItem obj, IDetailsItemUpdateNotifier notifier) {
    super(obj);
    this.fragment = fragment;

    PlexServer server = obj.server();
    PlexLibraryItem item = obj.item();
    boolean usePoster = !(item instanceof Episode);
    final Context context = fragment.getContext();

    Resources res = context.getResources();
    int width = res.getDimensionPixelSize(usePoster ? R.dimen.DETAIL_POSTER_WIDTH : R.dimen.DETAIL_THUMBNAIL_WIDTH);
    int height = res.getDimensionPixelSize(usePoster ? R.dimen.DETAIL_POSTER_HEIGHT : R.dimen.DETAIL_THUMBNAIL_HEIGHT);
    Glide.with(context)
     .load(server.makeServerURL(usePoster ? item.getCardImageURL() : item.getWideCardImageURL()))
     .asBitmap()
     .fitCenter()
     .dontAnimate()
     .error(R.drawable.default_background)
     .into(new SimpleTarget<Bitmap>(width, height) {
       @Override
       public void onResourceReady(final Bitmap resource,
                                   GlideAnimation glideAnimation) {
         setImageBitmap(context, resource);
         fragment.startEntranceTransition();
       }
     });

    SparseArrayObjectAdapter actions = new SparseArrayObjectAdapter();
    setActionsAdapter(actions);
    setActions();
    notifier.register(this);
  }

  @Override
  public void update(IDetailsItem obj) {
    setItem(null);
    setItem(obj);
    setActions();
  }

  @Override
  public String getKey() {
    return DetailsOverviewRow.class.getCanonicalName();
  }

  private void toggleWatched() {
    IDetailsItem item = getContainer();
    new ToggleWatchedStateTask(getContainer().server(), item.item()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, fragment.getContext());
    update(item);
  }

  private IDetailsItem getContainer() { return (IDetailsItem) getItem(); }

  private void setActions() {
    PlexLibraryItem item = getContainer().item();
    Context context = fragment.getContext();
    SparseArrayObjectAdapter adapter = (SparseArrayObjectAdapter) getActionsAdapter();
    boolean isWatched = item.getWatchedState() == PlexLibraryItem.WatchedState.Watched;
    adapter.set(ACTION_PLAY,
     new Action(ACTION_PLAY, context.getString(R.string.action_play), "",
      context.getDrawable(R.drawable.ic_play_circle_filled_white_24dp)));

    int drawableId = isWatched ? R.drawable.ic_visibility_white_24dp :
     R.drawable.ic_bookmark_white_24dp;
    adapter.set(ACTION_VIEWSTATUS,
     new Action(ACTION_VIEWSTATUS, context.getString(R.string.action_watched), "",
      context.getDrawable(drawableId)));
    adapter.set(ACTION_DELETE,
     new Action(ACTION_DELETE, context.getString(R.string.delete), "",
      context.getDrawable(R.drawable.ic_delete_white_24dp)));
  }

  @Override
  public void onActionClicked(Action action) {
    IDetailsItem obj = getContainer();
    PlexLibraryItem item = obj.item();
    switch ((int) action.getId()) {
      case ACTION_PLAY:
        Bundle bundle = fragment instanceof ICardSelectionListener ?
         ((ICardSelectionListener) fragment).getPlaySelectionBundle(false) : null;
        item.onPlayPressed(fragment, bundle, null);
        break;
      case ACTION_VIEWSTATUS:
        toggleWatched();
        break;
      case ACTION_DELETE:
        new DeleteTask(item, obj.server(), fragment, true, null, null);
        break;
    }
  }

  public static class ToggleWatchedStateTask extends AsyncTask<Context, Void, Void> {
    final private String key;
    final private String ratingKey;
    final private boolean isWatched;
    final private PlexServer server;

    ToggleWatchedStateTask(PlexServer server, PlexLibraryItem item) {

      this.server = server;
      key = item.getKey();
      ratingKey = Long.toString(item.getRatingKey());
      isWatched = item.getWatchedState() == PlexLibraryItem.WatchedState.Watched;
      item.toggleWatched();
    }

    @Override
    protected Void doInBackground(Context[] params) {

      Context context = params != null && params.length > 0 && params[0] != null ? params[0] : null;
      if (context != null)
        server.toggleWatchedState(key, ratingKey, isWatched, context);
      return null;
    }
  }

}
