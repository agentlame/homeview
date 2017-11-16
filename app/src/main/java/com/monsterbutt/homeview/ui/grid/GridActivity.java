package com.monsterbutt.homeview.ui.grid;

import android.os.Bundle;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.ui.HomeViewActivity;


public class GridActivity extends HomeViewActivity {

    public static final String SHARED_ELEMENT_NAME = "hero";
    public static final String URI = "homeview://app/list";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grid);
    }
}
