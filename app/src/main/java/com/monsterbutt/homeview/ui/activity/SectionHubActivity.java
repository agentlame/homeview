package com.monsterbutt.homeview.ui.activity;

import android.os.Bundle;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.ui.android.HomeViewActivity;


public class SectionHubActivity extends HomeViewActivity {

    public static final String TITLE = "title";
    public static final String SECTIONID = "sectionid";
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_hub);
    }
}
