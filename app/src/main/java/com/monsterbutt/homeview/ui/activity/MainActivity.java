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

import android.content.Intent;
import android.os.Bundle;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.ui.android.HomeViewActivity;
import com.monsterbutt.homeview.plex.PlexServerManager;
import com.monsterbutt.homeview.services.UpdateRecommendationsService;

/*
 * MainActivity class that loads MainFragment
 */
public class MainActivity extends HomeViewActivity {

    PlexServerManager mMgr = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
    }

    @Override
    public void onResume() {

        super.onResume();
        mMgr = PlexServerManager.getInstance(this);

        Intent recommendationIntent = new Intent(getApplicationContext(), UpdateRecommendationsService.class);
        startService(recommendationIntent);
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        mMgr.stopDiscovery();
    }

    @Override
    public boolean onSearchRequested() {
        startActivity(new Intent(this, SearchActivity.class));
        return true;
    }
}
