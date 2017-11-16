package com.monsterbutt.homeview.plex.tasks;


import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.widget.Toast;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.ui.presenters.CardObject;
import com.monsterbutt.homeview.ui.presenters.CardPresenter;

public class DeleteTask {

  private interface Callback {
    void callback(boolean success);
  }

  private final Fragment fragment;
  private final boolean finishAfterDelete;
  private final CardPresenter.LongClickWatchStatusCallback callback;
  private final CardObject card;

  public DeleteTask(final PlexLibraryItem item, final PlexServer server, Fragment fragment,
                             final boolean finishAfterDelete, final CardObject card,
                             final CardPresenter.LongClickWatchStatusCallback callback) {
    this.fragment = fragment;
    this.finishAfterDelete = finishAfterDelete;
    this.callback = callback;
    this.card = card;

    if (item != null && !TextUtils.isEmpty(item.getKey())) {

      final Callback taskCallback = new Callback() {
        @Override
        public void callback(boolean success) {
          Toast.makeText(DeleteTask.this.fragment.getContext(),
           success ? R.string.delete_success : R.string.delete_failed,
           Toast.LENGTH_LONG).show();
          if (DeleteTask.this.callback == null && DeleteTask.this.finishAfterDelete &&
           !DeleteTask.this.fragment.isDetached() && !DeleteTask.this.fragment.getActivity().isFinishing() &&
           !DeleteTask.this.fragment.getActivity().isDestroyed()) {
            DeleteTask.this.fragment.getActivity().finish();
          } else if (DeleteTask.this.callback != null && !DeleteTask.this.finishAfterDelete && DeleteTask.this.card != null) {
            DeleteTask.this.callback.removeSelected(DeleteTask.this.card);
          }
        }
      };

      final String key = item.getKey();
      Context context = fragment.getContext();
      AlertDialog alertDialog = new AlertDialog.Builder(fragment.getActivity()).create();
      alertDialog.setTitle(context.getString(R.string.delete_dialog_title));
      alertDialog.setMessage(context.getString(R.string.delete_dialog_message));
      alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, context.getString(R.string.delete_dialog_yes),
       new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface dialog, int which) {
           new DeleteMediaTask(key, server, taskCallback).execute();
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

    private final String key;
    private final PlexServer server;
    private final Callback callback;

    DeleteMediaTask(@NonNull String key, @NonNull PlexServer server, @NonNull Callback callback) {
      this.key = key;
      this.server = server;
      this.callback = callback;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
      return server.deleteMedia(key);
    }

    @Override
    protected void onPostExecute(Boolean success) {
      callback.callback(success);
    }
  }
}
