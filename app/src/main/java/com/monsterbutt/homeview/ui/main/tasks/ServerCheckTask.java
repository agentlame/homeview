package com.monsterbutt.homeview.ui.main.tasks;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.auth.api.credentials.Credential;

import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.PlexServerManager;
import com.monsterbutt.homeview.ui.main.ServerLoginDialog;


public class ServerCheckTask extends AsyncTask<PlexServer, Void, Boolean>
 implements ServerLoginDialog.ServerLoginInterface {

  public static final int PICK_CREDENTIALS = 12;
  public static final int ALLOW_CREDENTIAL_SIGNIN = 13;

  public interface CredentialRequestor {

    void loginAttempted(boolean success);
    void loginCanceled();
  }

  public interface CredentialProcessor {
    void processRetrievedCredential(Credential credential);
    void retrievalFailed();
  }

  private CredentialRequestor requestor;
  private CredentialProcessor processor;
  private Credential credentials;

  private final boolean isSwitchingFromCurrent;
  private boolean isRun = false;

  public ServerCheckTask(@NonNull CredentialRequestor requestor, boolean isSwitchingFromCurrent) {
    this.requestor = requestor;
    this.isSwitchingFromCurrent = isSwitchingFromCurrent;
  }

  public void getServerToken(FragmentActivity activity, PlexServer server) {
    new ServerLoginDialog(this).login(activity, server);
  }

  @Override
  protected Boolean doInBackground(PlexServer... server) {
    return PlexServerManager.getInstance().setSelectedServer(server[0]);
  }

  @Override
  protected void onPostExecute(Boolean result) {
    requestor.loginAttempted(result);
  }

  @Override
  public void onLoginAttempted(PlexServer server, boolean succeeded) {

    if (succeeded) {
      ServerCheckTask task = this;
      if (isSwitchingFromCurrent && isRun)
        task = new ServerCheckTask(requestor, true);
      isRun = true;
      task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, server);
    }
    else
      requestor.loginAttempted(false);
  }

  @Override
  public void onLoginCanceled() {
    requestor.loginCanceled();
  }

  @Override
  public void setCredentialProcessor(CredentialProcessor processor, final Credential credentials) {
    this.processor = processor;
    this.credentials = credentials;
  }

  public void onActivityResult(int requestCode, int resultCode, Intent data) {

    if (requestCode == ALLOW_CREDENTIAL_SIGNIN || requestCode == PICK_CREDENTIALS) {
      if (resultCode == Activity.RESULT_OK)
        processor.processRetrievedCredential(requestCode == ALLOW_CREDENTIAL_SIGNIN ?
         credentials : data != null ? (Credential) data.getParcelableExtra(Credential.EXTRA_KEY) : null);
      else {
        processor.retrievalFailed();
        requestor.loginAttempted(false);
      }
    }
  }

}
