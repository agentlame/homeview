package com.monsterbutt.homeview.ui.main;


import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.PlexServerManager;
import com.monsterbutt.homeview.plex.ServerDiscovery;
import com.monsterbutt.homeview.ui.main.tasks.ServerCheckTask;
import com.monsterbutt.homeview.BootupActivity;

import java.util.List;


public class ServerConnectFragment extends Fragment
 implements ServerDiscovery.DiscoveryCallback, ServerCheckTask.CredentialRequestor {


  public static final int SERVER_CHOICE_RESULT = 1839;

  private ServerCheckTask serverCheckTask = null;
  private ServerDiscovery discovery = new ServerDiscovery();

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_serverconnect, container, false);
  }

  @Override
  public void onResume() {
    super.onResume();

    PlexServerManager mgr = PlexServerManager.getInstance();
    if (mgr.getSelectedServer() != null && mgr.getSelectedServer().isValid())
      moveToMain();
    else
      discovery.start(getContext(), this, true);
  }

  @Override
  public void onPause() {
    super.onPause();
    discovery.stop(getContext());
  }

  @Override
  public void serverDiscovered(PlexServer server) { }

  @Override
  public void discoveryDone(List<PlexServer> servers) {

    PlexServerManager mgr = PlexServerManager.getInstance();
    if (servers.size() > 1) {
      for (PlexServer server : servers)
        mgr.addServer(server);
      startActivityForResult(new Intent(getActivity(), ServerChoiceActivity.class), SERVER_CHOICE_RESULT);
      return;
    }
    PlexServer server = servers.get(0);
    if (server.isValid()) {
      mgr.setSelectedServer(server);
      moveToMain();
      return;
    }
    serverCheckTask = new ServerCheckTask(this, true);
    serverCheckTask.getServerToken((FragmentActivity) getActivity(), server);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {

    super.onActivityResult(requestCode, resultCode, data);
    if (serverCheckTask != null)
      serverCheckTask.onActivityResult(requestCode, resultCode, data);
  }

  private void backOut() {
    getFragmentManager().popBackStack();
  }

  private void moveToMain() {

    new NotificationTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    getFragmentManager().beginTransaction()
     .replace(R.id.main_browse_fragment, new MainFragment())
     .commit();
  }

  @Override
  public void loginAttempted(boolean success) {
    if (success)
      moveToMain();
    else {
      Toast.makeText(getContext(), getString(R.string.plex_login_failed), Toast.LENGTH_LONG).show();
      backOut();
    }
  }

  @Override
  public void loginCanceled() {
    backOut();
  }

  private static class NotificationTask extends AsyncTask<Context, Void, Void> {

    @Override
    protected Void doInBackground(Context... params) {

      Context context = params != null && params.length > 0 ? params[0] : null;
      if (context != null)
        BootupActivity.scheduleRecommendationUpdate(context);
      return null;
    }
  }

}
