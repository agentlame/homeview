package com.monsterbutt.homeview.ui.activity;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.widget.GuidanceStylist.*;
import android.support.v17.leanback.widget.GuidedAction;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.auth.api.credentials.Credential;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.PlexServerManager;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.monsterbutt.homeview.plex.PlexServerManager.ALLOW_CREDENTIAL_SIGNIN;
import static com.monsterbutt.homeview.plex.PlexServerManager.PICK_CREDENTIALS;

public class ServerChoiceActivity extends FragmentActivity implements PlexServerManager.CredentialRequestor {

    private static final int MANUAL = 999;
    private static final int DONE   = 1000;
    private static final int BACK   = 1001;
    private static final int CANCEL = 1002;
    private static final int HOST   = 1003;
    private static final int PORT   = 1004;

    private static final int SERVER_CHECK_DELAY = 2000;
    private Timer mServerCheckTimer = null;
    private static PlexServerManager mMgr = null;
    private PlexServerManager.CredentialProcessor processor;
    private Credential credential = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        mMgr = PlexServerManager.getInstance(getApplicationContext(), this);
        final boolean hasServers = null != mMgr.getSelectedServer() || !mMgr.getDiscoveredServers().isEmpty();
        if (null == savedInstanceState) {

            if (!hasServers)
                GuidedStepFragment.addAsRoot(this, new ManualStepFragment(), android.R.id.content);
            else {
                mMgr.startDiscovery();
                mServerCheckTimer = new Timer();
                mServerCheckTimer.schedule(new CheckForPlexServerTask(this), SERVER_CHECK_DELAY);
            }
        }
    }

    private static void addAction(List<GuidedAction> actions, long id, String title, String desc) {
        actions.add(new GuidedAction.Builder()
                .id(id)
                .title(title)
                .description(desc)
                .build());
    }

    private static void addEditAction(List<GuidedAction> actions, long id, String title, String desc) {
        actions.add(new GuidedAction.Builder()
                .id(id)
                .editTitle(title)
                .title(title)
                .description(desc)
                .editable(true)
                .build());
    }

    @Override
    public FragmentActivity getActivity() {
        return this;
    }

    @Override
    public void setCredentialProcessor(PlexServerManager.CredentialProcessor processor, final Credential credentials) {
        this.processor = processor;
        this.credential = credentials;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ALLOW_CREDENTIAL_SIGNIN || requestCode == PICK_CREDENTIALS) {
            if (resultCode == RESULT_OK)
                processor.processRetrievedCredential(requestCode == ALLOW_CREDENTIAL_SIGNIN ?
                 credential : data != null ? (Credential) data.getParcelableExtra(Credential.EXTRA_KEY) : null);
            else
                processor.retrievalFailed();
        }
    }

    public static class ServersFragment extends GuidedStepFragment {

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

            List<PlexServer> servers = mMgr.getDiscoveredServers();
            for (PlexServer server : servers)
                addAction(actions, actions.size(), server.getServerName(), server.getServerAddress());

            addAction(actions, MANUAL,
                    getResources().getString(R.string.manual_server_choice),
                    getResources().getString(R.string.servermanual_desc));
            addAction(actions, CANCEL,
                    getResources().getString(R.string.cancel_server_choice),
                    getResources().getString(R.string.cancel_server_desc));
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {

            FragmentManager fm = getFragmentManager();
            if (action.getId() == MANUAL)
                GuidedStepFragment.add(fm, new ManualStepFragment());
            else if (action.getId() == CANCEL)
                getActivity().finishAfterTransition();
            else {

                List<PlexServer> servers = mMgr.getDiscoveredServers();
                PlexServer server = servers.get((int) action.getId());
                if (server != null)
                    getServerToken(server, (FragmentActivity) getActivity());
            }
        }
    }

    private static void getServerToken(PlexServer server, FragmentActivity activity) {

        PlexServerManager.getInstance(activity, activity).getServerToken(server,
         (PlexServerManager.CredentialRequestor) activity);
    }

    public static class ManualStepFragment extends GuidedStepFragment {

        String host = "";
        String port = PlexServer.DEFAULT_SERVER_PORT;

        @Override
        @NonNull
        public Guidance onCreateGuidance(Bundle savedInstanceState) {

            return new Guidance(getString(R.string.servermanual_title),
                    getString(R.string.servermanual_desc),
                    getString(R.string.servermanual_breadcrumb),
                    getActivity().getDrawable(R.drawable.launcher));
        }

        @Override
        public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {

            addEditAction(actions, HOST, host, getResources().getString(R.string.serverhost_desc));
            addEditAction(actions, PORT, port, getResources().getString(R.string.serverport_desc));

            addAction(actions, DONE,
                    getResources().getString(R.string.doneserver_title),
                    getResources().getString(R.string.doneserver_desc));
            int cancelId = mMgr.getDiscoveredServers().isEmpty() ? CANCEL : BACK;
            addAction(actions, cancelId,
                    getResources().getString(R.string.cancel_server_choice),
                    getResources().getString(R.string.cancel_server_desc));
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {

            if (action.getId() == DONE)
                getServerToken(new PlexServer("manual", String.format("%s:%s", host, port), getContext()), (FragmentActivity) getActivity());
            else if (action.getId() == CANCEL)
                getActivity().finishAfterTransition();
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
    }

    private class CheckForPlexServerTask extends TimerTask {

        Activity mActivity;

        CheckForPlexServerTask(Activity activity) {
            mActivity = activity;
        }
        @Override
        public void run() {

            if (mMgr.isDiscoveryRunning())
                mServerCheckTimer.schedule(new CheckForPlexServerTask(mActivity), SERVER_CHECK_DELAY);
            else {

                mServerCheckTimer.cancel();
                mServerCheckTimer = null;
                if (mActivity.isDestroyed() || mActivity.isFinishing())
                    return;
                GuidedStepFragment.addAsRoot(mActivity, new ServersFragment(), android.R.id.content);
            }
        }
    }
}
