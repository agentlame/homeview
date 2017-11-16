package com.monsterbutt.homeview.ui.main;

import android.annotation.SuppressLint;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.widget.GuidanceStylist.*;
import android.support.v17.leanback.widget.GuidedAction;
import android.support.v4.app.FragmentActivity;
import android.util.ArraySet;
import android.widget.Toast;

import com.monsterbutt.Homeview;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.PlexServerManager;
import com.monsterbutt.homeview.plex.ServerDiscovery;
import com.monsterbutt.homeview.ui.main.tasks.ServerCheckTask;

import java.util.List;

public class ServerChoiceActivity extends FragmentActivity
 implements ServerCheckTask.CredentialRequestor, ServerDiscovery.DiscoveryCallback {

  private static final int MANUAL = 999;
  private static final int DONE   = 1000;
  private static final int BACK   = 1001;
  private static final int CANCEL = 1002;
  private static final int HOST   = 1003;
  private static final int PORT   = 1004;

  private static ServerCheckTask serverCheckTask = null;
  private static ServerChoiceActivity mActivity;

  private static ServerDiscovery discovery = new ServerDiscovery();
  private ServersFragment fragment;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mActivity = this;
    if (null == savedInstanceState) {
      PlexServerManager mgr = PlexServerManager.getInstance();
      for (PlexServer server : mgr.getDiscoveredServers())
        addServerToFragment(server);
      discovery.start(Homeview.getAppContext(), this, false);
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (serverCheckTask != null)
      serverCheckTask.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  public void loginAttempted(boolean success) {
    if (success) {
      discovery.stop(Homeview.getAppContext());
      finish();
    }
    else
      Toast.makeText(this, getString(R.string.plex_login_failed), Toast.LENGTH_LONG).show();
  }

  @Override
  public void loginCanceled() { }

  @Override
  public void serverDiscovered(PlexServer server) {
      addServerToFragment(server);
  }

  @Override
  public void discoveryDone(List<PlexServer> servers) {
    for (PlexServer server : servers)
      addServerToFragment(server);

    if (servers.isEmpty() && fragment == null)
      GuidedStepFragment.addAsRoot(mActivity, new ManualStepFragment(true), android.R.id.content);
  }

  private void addServerToFragment(PlexServer server) {
    if (server == null)
      return;
    if (fragment == null) {
      fragment = new ServersFragment();
      GuidedStepFragment.addAsRoot(mActivity, fragment, android.R.id.content);
    }
    fragment.addServer(server);
  }

  public static class ServersFragment extends GuidedStepFragment {

    private List<GuidedAction> actions;
    private ArraySet<PlexServer> servers = new ArraySet<>();

    public void addServer(PlexServer server) {
      int index = servers.size();
      if (!servers.add(server) || actions == null)
        return;
      addAction(index, index, server.getServerName(), server.getServerAddress());
    }

    @Override
    @NonNull
    public Guidance onCreateGuidance(@NonNull Bundle savedInstanceState) {
      return new Guidance(getString(R.string.serverchoice_title),
        getString(R.string.serverchoice_desc),
        getString(R.string.serverchoice_breadcrumb),
        getActivity().getDrawable(R.drawable.launcher));
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
      this.actions = actions;
      int index = 0;
      if (!servers.isEmpty()) {
        for(PlexServer server : servers) {
          addAction(index, index, server.getServerName(), server.getServerAddress());
          ++index;
        }
      }
      addAction(index++, MANUAL, getString(R.string.manual_server_choice), getString(R.string.servermanual_desc));
      addAction(index,  CANCEL, getString(R.string.cancel_server_choice), getString(R.string.cancel_server_desc));
    }

    private void addAction(int index, long id, String title, String desc) {
      actions.add(index, new GuidedAction.Builder().id(id).title(title).description(desc).build());
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
      FragmentManager fm = getFragmentManager();
      if (action.getId() == MANUAL)
        GuidedStepFragment.add(fm, new ManualStepFragment(false));
      else if (action.getId() == CANCEL) {
        discovery.stop(Homeview.getAppContext());
        getActivity().finishAfterTransition();
      }
      else {
        PlexServer server = servers.valueAt((int) action.getId());
        if (server != null)
          getServerToken(server);
      }
    }
  }

  private static void getServerToken(PlexServer server) {
    serverCheckTask = new ServerCheckTask(mActivity, true);
    serverCheckTask.getServerToken(mActivity, server);
  }

  public static class ManualStepFragment extends GuidedStepFragment {

    String host = "";
    String port = PlexServer.DEFAULT_SERVER_PORT;

    private final boolean shouldExitOnCancel;

    public ManualStepFragment() { this(false); }

    @SuppressLint("ValidFragment")
    ManualStepFragment(boolean shouldExitOnCancel) {
      this.shouldExitOnCancel = shouldExitOnCancel;
    }

    @Override
    @NonNull
    public Guidance onCreateGuidance(Bundle savedInstanceState) {
      return new Guidance(getString(R.string.servermanual_title), getString(R.string.servermanual_desc),
        getString(R.string.servermanual_breadcrumb), getActivity().getDrawable(R.drawable.launcher));
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
      addEditAction(actions, HOST, host, getString(R.string.serverhost_desc));
      addEditAction(actions, PORT, port, getString(R.string.serverport_desc));
      addAction(actions, DONE, getString(R.string.doneserver_title), getString(R.string.doneserver_desc));
      int cancelId = shouldExitOnCancel ? CANCEL : BACK;
      addAction(actions, cancelId, getString(R.string.cancel_server_choice), getString(R.string.cancel_server_desc));
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
      if (action.getId() == DONE)
        getServerToken(new PlexServer("manual", String.format("%s:%s", host, port), getContext()));
      else if (action.getId() == CANCEL) {
        discovery.stop(Homeview.getAppContext());
        getActivity().finishAfterTransition();
      }
      else if (action.getId() == BACK)
        getFragmentManager().popBackStack();
      else if (action.getId() == HOST) {
        host = action.getEditTitle().toString();
        action.setTitle(host);
      }
      else if (action.getId() == PORT) {
        port = action.getEditTitle().toString();
        action.setTitle(port);
      }
    }

    private void addAction(List<GuidedAction> actions, long id, String title, String desc) {
      actions.add(new GuidedAction.Builder().id(id).title(title).description(desc).build());
    }

    private void addEditAction(List<GuidedAction> actions, long id, String title, String desc) {
      actions.add(new GuidedAction.Builder()
       .id(id).editTitle(title).title(title).description(desc).editable(true).build());
    }
  }
}
