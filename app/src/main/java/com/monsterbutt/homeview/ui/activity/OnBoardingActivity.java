package com.monsterbutt.homeview.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.auth.api.credentials.Credential;
import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.plex.PlexServerManager;
import com.monsterbutt.homeview.plex.tasks.PlexServerTask;
import com.monsterbutt.homeview.plex.tasks.PlexServerTaskCaller;
import com.monsterbutt.homeview.plex.tasks.ServerLibraryTask;
import com.monsterbutt.homeview.ui.UILifecycleManager;
import com.monsterbutt.homeview.ui.handler.ServerStatusHandler;

import static com.monsterbutt.homeview.plex.PlexServerManager.ALLOW_CREDENTIAL_SIGNIN;
import static com.monsterbutt.homeview.plex.PlexServerManager.PICK_CREDENTIALS;

public class OnBoardingActivity extends FragmentActivity implements PlexServerTaskCaller,
 PlexServerManager.CredentialRequestor {

    public static String TitleExtra = "TITLE";
    public static String SectionsExtra = "SECTIONS";
    public static String HubsExtra = "HUBS";

    private UILifecycleManager mLifeCycleMgr = new UILifecycleManager();

    private PlexServerManager.CredentialProcessor processor;
    private Credential credential = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        mLifeCycleMgr.put(ServerStatusHandler.key, new ServerStatusHandler(this, this, null));
    }

    @Override
    public void onResume() {
        super.onResume();
        mLifeCycleMgr.resumed();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ServerStatusHandler.SERVER_CHOICE_RESULT) {

            PlexServer server = PlexServerManager.getInstance(getApplicationContext(), this).getSelectedServer();
            if (server == null || !server.isValid())
                setResults(null);
        }
        else if (requestCode == ALLOW_CREDENTIAL_SIGNIN || requestCode == PICK_CREDENTIALS) {
            if (resultCode == RESULT_OK)
                processor.processRetrievedCredential(requestCode == ALLOW_CREDENTIAL_SIGNIN ?
                 credential : data != null ? (Credential) data.getParcelableExtra(Credential.EXTRA_KEY) : null);
            else
                processor.retrievalFailed();
        }
    }

    private void setResults(ServerLibraryTask task) {

        Intent intent = new Intent(this, MainActivity.class);
        if (task != null) {
            intent.putExtra(OnBoardingActivity.TitleExtra, task.getLibrary() != null ? task.getLibrary().getTitle1() : "");
            intent.putExtra(OnBoardingActivity.SectionsExtra, task.getSections());
            intent.putParcelableArrayListExtra(OnBoardingActivity.HubsExtra, task.getHubs());
        }
        startActivity(intent);
    }

    @Override
    public void handlePostTaskUI(Boolean result, PlexServerTask task) {

        if (!(task instanceof ServerLibraryTask))
            return;

        if (isFinishing() || isDestroyed())
            return;
        setResults((ServerLibraryTask) task);
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
}