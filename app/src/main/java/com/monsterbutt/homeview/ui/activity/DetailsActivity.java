/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.monsterbutt.homeview.ui.activity;

import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.plex.media.PlexVideoItem;
import com.monsterbutt.homeview.ui.android.HomeViewActivity;

/*
 * Details activity class that loads LeanbackDetailsFragment class
 */
public class DetailsActivity extends HomeViewActivity {

    public static final String SHARED_ELEMENT_NAME = "hero";
    public static final String KEY = "KEY";
    public static final String ITEM = "item";
    public static final String BACKGROUND = "background";
    public static final String URI = "homeview://app/details";

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        Intent intent = getIntent();
        Uri data = intent.getData();
        String key = intent.getStringExtra(SearchManager.EXTRA_DATA_KEY);
        if (data != null && key != null) {

            if (data.toString().equals("homeview://plex/playback")) {

                Intent next = new Intent(this, PlaybackActivity.class);
                next.setAction(PlaybackActivity.ACTION_VIEW);
                next.putExtra(PlaybackActivity.KEY, key);
                PlexVideoItem vid = intent.getParcelableExtra(PlaybackActivity.VIDEO);
                if (vid != null)
                    next.putExtra(PlaybackActivity.VIDEO, vid);
                startActivity(next);
            }
        }
    }
}
