package com.monsterbutt.homeview.ui.android;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.CredentialRequestResult;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.PlexServerManager;

import static com.monsterbutt.homeview.plex.PlexServerManager.ALLOW_CREDENTIAL_SIGNIN;
import static com.monsterbutt.homeview.plex.PlexServerManager.PICK_CREDENTIALS;

public class ServerLoginDialog {

  public interface ServerLoginInterface {

    void onLoginAttempted(PlexServer server, boolean succeeded);
    void onLoginCanceled();
    void setCredentialProcessor(PlexServerManager.CredentialProcessor processor, final Credential credential);
  }

  private final FragmentActivity activity;
  private final ServerLoginInterface callback;

  public ServerLoginDialog(FragmentActivity activity, ServerLoginInterface callback) {
    this.activity = activity;
    this.callback = callback;
  }

  public void login(final PlexServer server) {

    if (server.hasServerToken())
      callback.onLoginAttempted(server, true);
    new GoogleAuthTask(this, activity, callback, server).execute();
  }

  private void login(final GoogleAuthTask authTask, final FragmentActivity activity,
                           final ServerLoginInterface callback, final PlexServer server) {

    final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
    LayoutInflater inflater = activity.getLayoutInflater();
    final View dialogView = inflater.inflate(R.layout.dialog_login, null);
    dialogBuilder.setView(dialogView);

    final EditText login = (EditText) dialogView.findViewById(R.id.edit_plex_login);
    final EditText pass = (EditText) dialogView.findViewById(R.id.edit_plex_pass);

    final GetTokenTask task = new GetTokenTask(authTask, callback, server);
    dialogBuilder.setTitle(R.string.plex_login_title);
    dialogBuilder.setMessage(R.string.plex_login_description);
    dialogBuilder.setPositiveButton(R.string.plex_login_button, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        if (login.getText().toString().length() == 0 || login.getText().toString().length() == 0) {
          Toast.makeText(activity, activity.getString(R.string.plex_login_nonempty), Toast.LENGTH_LONG).show();
          dialogBuilder.create().show();
        }
        else {
          Credential cred = new Credential.Builder(String.format("%s:%s", server.getServerName(), server.getServerAddress()))
           .setName(login.getText().toString())
           .setPassword(pass.getText().toString())
           .build();
          task.execute(cred);
        }
      }
    });
    dialogBuilder.setOnDismissListener(new DialogInterface.OnDismissListener() {
      @Override
      public void onDismiss(DialogInterface dialog) {
        if (!task.didTry)
          callback.onLoginCanceled();
      }
    });
    dialogBuilder.setNegativeButton(R.string.plex_login_cancel, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        callback.onLoginCanceled();
      }
    });
    dialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
      @Override
      public void onCancel(DialogInterface dialog) {
        callback.onLoginCanceled();
      }});
    dialogBuilder.create().show();
  }

  private class GoogleAuthTask extends AsyncTask<Void, Void, Void>
   implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
   PlexServerManager.CredentialProcessor {

    private final FragmentActivity activity;
    private final ServerLoginDialog.ServerLoginInterface callback;
    private final PlexServer server;
    private final ServerLoginDialog dialog;

    private GoogleApiClient googleApiClient;
    private boolean handled = false;
    private boolean mIsResolving = false;

    private final static String TAG = "GoogleAuthTask";

    GoogleAuthTask(ServerLoginDialog dialog, FragmentActivity activity, final ServerLoginDialog.ServerLoginInterface callback, final PlexServer server) {
      this.activity = activity;
      this.callback = callback;
      this.server = server;
      this.dialog = dialog;
    }

    @Override
    protected Void doInBackground(Void... params) {
      googleApiClient = new GoogleApiClient.Builder(activity)
       .addConnectionCallbacks(this)
       .enableAutoManage(activity, 0, this)
       .addApi(Auth.CREDENTIALS_API)
       .build();
      return null;
    }

    @Override
    protected void onPostExecute(Void result) {}

    @Override
    public void onConnected(Bundle bundle) {
      if (!handled) {
        handled = true;
        requestCredentials();
      }
    }

    @Override
    public void onConnectionSuspended(int cause) {}

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
      if (!handled) {
        handled = true;
        dialog.login(null, activity, callback, server);
      }
    }

    private void requestCredentials() {

      CredentialRequest request = new CredentialRequest.Builder()
       .setPasswordLoginSupported(true)
       .setAccountTypes(IdentityProviders.GOOGLE)
       .build();

      Auth.CredentialsApi.request(googleApiClient, request).setResultCallback(
       new ResultCallback<CredentialRequestResult>() {
         @Override
         public void onResult(@NonNull CredentialRequestResult credentialRequestResult) {
           com.google.android.gms.common.api.Status status = credentialRequestResult.getStatus();
           if (credentialRequestResult.getStatus().isSuccess()) {
             // Successfully read the credential without any user interaction, this
             // means there was only a single credential and the user has auto
             // sign-in enabled.
             Credential credential = credentialRequestResult.getCredential();
             processRetrievedCredential(credential);
           } else if (status.getStatusCode() == CommonStatusCodes.RESOLUTION_REQUIRED) {
             // This is most likely the case where the user has multiple saved
             // credentials and needs to pick one.
             resolveResult(status, null, PICK_CREDENTIALS);
           } else if (status.getStatusCode() == CommonStatusCodes.SIGN_IN_REQUIRED) {
             // This is most likely the case where the user does not currently
             // have any saved credentials and thus needs to provide a username
             // and password to sign in.
             dialog.login(GoogleAuthTask.this, activity, callback, server);
           } else {
             dialog.login(null, activity, callback, server);
           }
         }
       }
      );
    }

    private void resolveResult(com.google.android.gms.common.api.Status status, final Credential credentials, int requestCode) {
      // We don't want to fire multiple resolutions at once since that can result
      // in stacked dialogs after rotation or another similar event.
      if (mIsResolving) {
        Log.w(TAG, "resolveResult: already resolving.");
        return;
      }

      Log.d(TAG, "Resolving: " + status);
      if (status.hasResolution()) {
        Log.d(TAG, "STATUS: RESOLVING");
        try {
          callback.setCredentialProcessor(this, credentials);
          status.startResolutionForResult(activity, requestCode);
          mIsResolving = true;
        } catch (IntentSender.SendIntentException e) {
          Log.e(TAG, "STATUS: Failed to send resolution.", e);
        }
      } else {
        dialog.login(null, activity, callback, server);
      }
    }

    public void retrievalFailed() {
      dialog.login(null, activity, callback, server);
    }

    public void processRetrievedCredential(Credential credential) {
      String accountType = credential.getAccountType();
      if (accountType == null) {
        if (!TextUtils.isEmpty(credential.getName()) && !TextUtils.isEmpty(credential.getPassword())) {
          new GetTokenTask(this, callback, server).execute(credential);
        } else {
          // This is likely due to the credential being changed outside of
          // Smart Lock,
          // ie: away from Android or Chrome. The credential should be deleted
          // and the user allowed to enter a valid credential.
          Toast.makeText(activity, "Retrieved credentials are invalid, so will be deleted.", Toast.LENGTH_LONG).show();
          deleteCredential(credential);
          requestCredentials();
        }
      }
    }

    void saveCredential(final Credential credential, final ServerLoginInterface callback, final PlexServer server) {
      // Credential is valid so save it.
      Auth.CredentialsApi.save(googleApiClient, credential)
       .setResultCallback(new ResultCallback<com.google.android.gms.common.api.Status>() {
        @Override
        public void onResult(@NonNull com.google.android.gms.common.api.Status status) {
          if (status.isSuccess()) {
            Log.d(TAG, "Credential saved");
            callback.onLoginAttempted(server, true);
          } else if (status.getStatusCode() == CommonStatusCodes.RESOLUTION_REQUIRED) {
            resolveResult(status, credential, ALLOW_CREDENTIAL_SIGNIN);
          } else {
            Log.d(TAG, "Attempt to save credential failed " +
             status.getStatusMessage() + " " +
             status.getStatusCode());
            callback.onLoginAttempted(server, true);
          }
        }
      });
    }

    private void deleteCredential(Credential credential) {
      Auth.CredentialsApi.delete(googleApiClient,
       credential).setResultCallback(new ResultCallback<com.google.android.gms.common.api.Status>() {
        @Override
        public void onResult(@NonNull com.google.android.gms.common.api.Status status) {
          if (status.isSuccess()) {
            Log.d(TAG, "Credential successfully deleted.");
          } else {
            // This may be due to the credential not existing, possibly
            // already deleted via another device/app.
            Log.d(TAG, "Credential not deleted successfully.");
          }
        }
      });
    }
  }

  private static class GetTokenTask extends AsyncTask<Credential, Void, Boolean> {

    private final GoogleAuthTask authTask;
    private final ServerLoginInterface callback;
    private final PlexServer server;
    private boolean didTry = false;

    GetTokenTask(GoogleAuthTask authTask, ServerLoginInterface callback, PlexServer server) {
      this.callback = callback;
      this.authTask = authTask;
      this.server = server;
    }

    @Override
    protected Boolean doInBackground(Credential... credentials) {

      didTry = true;
      Credential credential = credentials != null && credentials.length > 0 ? credentials[0] : null;
      boolean success = credential != null && server != null
       && server.fetchServerToken(credential.getName(), credential.getPassword());
      if (success && authTask != null)
        authTask.saveCredential(credential, callback, server);
      return success;
    }

    @Override
    protected void onPostExecute(Boolean result) {
      if (!result || authTask == null)
        callback.onLoginAttempted(server, result);
    }
  }
}
