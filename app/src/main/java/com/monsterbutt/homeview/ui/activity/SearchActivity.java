package com.monsterbutt.homeview.ui.activity;

import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v17.leanback.widget.SpeechRecognitionCallback;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.provider.MediaContentProvider;
import com.monsterbutt.homeview.ui.android.HomeViewActivity;
import com.monsterbutt.homeview.ui.fragment.SearchFragment;

import static android.support.v4.content.IntentCompat.EXTRA_START_PLAYBACK;


public class SearchActivity extends HomeViewActivity {

    private static final int REQUEST_SPEECH = 1;
    private SearchFragment mFragment;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Uri data = intent != null ? intent.getData() : null;
        if (data != null) {

            boolean startPlayback = getIntent().getBooleanExtra(EXTRA_START_PLAYBACK, true);
            String key = intent.getStringExtra(SearchManager.EXTRA_DATA_KEY);
            String path = data.getLastPathSegment();
            if (startPlayback || path.equals(MediaContentProvider.ID_PLAYBACK)) {

                Intent suggestion = new Intent(getApplicationContext(), PlaybackActivity.class);
                suggestion.setAction(PlaybackActivity.ACTION_VIEW);
                suggestion.putExtra(PlaybackActivity.KEY, key);
                PlexVideoItem vid = intent.getParcelableExtra(PlaybackActivity.VIDEO);
                if (vid != null)
                    suggestion.putExtra(PlaybackActivity.VIDEO, vid);
                startActivity(suggestion);
                finish();
                return;
            }
            else {

                Intent suggestion = new Intent(getApplicationContext(), DetailsActivity.class);
                suggestion.putExtra(DetailsActivity.KEY, key);
                startActivity(suggestion);
                finish();
                return;
            }
        }
        setContentView(R.layout.activity_search);
        mFragment = (SearchFragment) getFragmentManager().findFragmentById(R.id.search_fragment);
        SpeechRecognitionCallback speechRecognitionCallback = new SpeechRecognitionCallback() {
            @Override
            public void recognizeSpeech() {
                startActivityForResult(mFragment.getRecognizerIntent(), REQUEST_SPEECH);
            }
        };
        mFragment.setSpeechRecognitionCallback(speechRecognitionCallback);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SPEECH && resultCode == RESULT_OK)
            mFragment.setSearchQuery(data, true);
    }

    @Override
    public boolean onSearchRequested() {
        if (mFragment.hasResults()) {
            startActivity(new Intent(this, SearchActivity.class));
        } else {
            mFragment.startRecognition();
        }
        return true;
    }
}
