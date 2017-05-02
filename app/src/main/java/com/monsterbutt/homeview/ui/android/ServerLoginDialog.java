package com.monsterbutt.homeview.ui.android;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;

public class ServerLoginDialog {

  public interface ServerLoginInterface {

    void onLoginAttempted(PlexServer server, boolean succeeded);
    void onLoginCanceled();
  }

  public static void login(final Activity activity, final ServerLoginInterface callback, final PlexServer server) {

    if (server.hasServerToken())
      callback.onLoginAttempted(server, true);

    final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
    LayoutInflater inflater = activity.getLayoutInflater();
    final View dialogView = inflater.inflate(R.layout.dialog_login, null);
    dialogBuilder.setView(dialogView);

    final EditText login = (EditText) dialogView.findViewById(R.id.edit_plex_login);
    final EditText pass = (EditText) dialogView.findViewById(R.id.edit_plex_pass);

    dialogBuilder.setTitle(R.string.plex_login_title);
    dialogBuilder.setMessage(R.string.plex_login_description);
    dialogBuilder.setPositiveButton(R.string.plex_login_button, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        if (login.getText().toString().length() == 0 || login.getText().toString().length() == 0) {
          Toast.makeText(activity, activity.getString(R.string.plex_login_nonempty), Toast.LENGTH_LONG).show();
          dialogBuilder.create().show();
        }
        else
          new GetTokenTask(callback, login.getText().toString(), pass.getText().toString()).execute(server);
      }
    });
    dialogBuilder.setNegativeButton(R.string.plex_login_cancel, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        callback.onLoginCanceled();
      }
    });
    dialogBuilder.create().show();
  }

  private static class GetTokenTask extends AsyncTask<PlexServer, Void, Boolean> {

    final ServerLoginInterface callback;
    final String user,pass;
    PlexServer server;

    GetTokenTask(ServerLoginInterface callback, String user, String pass) {
      this.callback = callback;
      this.user = user;
      this.pass = pass;
    }

    @Override
    protected Boolean doInBackground(PlexServer... servers) {
      server = servers != null && servers.length > 0 ? servers[0] : null;
      return server != null && server.fetchServerToken(user, pass);
    }

    @Override
    protected void onPostExecute(Boolean result) {
      callback.onLoginAttempted(server, result);
    }
  }
}
