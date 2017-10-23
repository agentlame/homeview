package com.monsterbutt.homeview.plex.tasks;


import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.widget.Toast;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.presenters.CardObject;
import com.monsterbutt.homeview.presenters.CardPresenter;

public class DeleteTask {

  public static void runTask(PlexLibraryItem item, final PlexServer server, final Fragment fragment,
                             final boolean finishAfterDelete, final CardObject card,
                             final CardPresenter.LongClickWatchStatusCallback callback) {
    if (item != null && !TextUtils.isEmpty(item.getKey())) {

      final String key = item.getKey();
      Context context = fragment.getContext();
      AlertDialog alertDialog = new AlertDialog.Builder(fragment.getActivity()).create();
      alertDialog.setTitle(context.getString(R.string.delete_dialog_title));
      alertDialog.setMessage(context.getString(R.string.delete_dialog_message));
      alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, context.getString(R.string.delete_dialog_yes),
       new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface dialog, int which) {
           new DeleteMediaTask(key, server, fragment, finishAfterDelete, callback, card).execute();
           dialog.dismiss();
         }
       });
      alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, context.getString(R.string.delete_dialog_no),
       new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface dialog, int which) {
           dialog.dismiss();
         }
       });
      alertDialog.show();
    }
  }

  private static class DeleteMediaTask extends AsyncTask<Void, Void, Boolean> {

    private final Fragment fragment;
    private final String key;
    private final PlexServer server;
    private final boolean finishAfterDelete;
    private final CardPresenter.LongClickWatchStatusCallback callback;
    private final CardObject card;

    DeleteMediaTask(String key, PlexServer server, Fragment fragment, boolean finishAfterDelete,
                    CardPresenter.LongClickWatchStatusCallback callback, CardObject card) {
      this.fragment = fragment;
      this.key = key;
      this.server = server;
      this.callback = callback;
      this.finishAfterDelete = finishAfterDelete;
      this.card = card;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
      return server.deleteMedia(key);
    }

    @Override
    protected void onPostExecute(Boolean success) {

      Toast.makeText(fragment.getContext(),
       success ? R.string.delete_success : R.string.delete_failed,
       Toast.LENGTH_LONG).show();

      if (callback == null && finishAfterDelete &&
       !fragment.isDetached() && !fragment.getActivity().isFinishing() &&
       !fragment.getActivity().isDestroyed()) {
        fragment.getActivity().finish();
      } else if (callback != null && !finishAfterDelete && card != null) {
        callback.removeSelected(card);
      }
    }
  }
}
