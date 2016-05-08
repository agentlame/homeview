package com.monsterbutt.homeview.ui.activity;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.ui.android.HomeViewActivity;
import com.monsterbutt.homeview.ui.fragment.ContainerGridFragment;

import java.util.List;


public class ContainerActivity extends HomeViewActivity {

    public static final String KEY = "key";
    public static final String USE_SCENE = "scenelayout";
    public static final String SHARED_ELEMENT_NAME = "hero";
    public static final String BACKGROUND = "background";

    private ListView mQuickList = null;
    private ContainerGridFragment mFragment;
    private boolean mQuickListSelected = false;
    private int mQuickListSelectedIndex = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_container);

        if (getIntent().getBooleanExtra(ContainerActivity.USE_SCENE, false))
            (findViewById(R.id.toolbarQuickJump)).setVisibility(View.GONE);

        mQuickList = (ListView) findViewById(R.id.list_shortcut);
        mQuickList.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (v == mQuickList && !hasFocus) {
                    mQuickListSelected = false;
                }
            }
        });
        mQuickList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mQuickListSelected = true;
                mFragment.setSelectedPosition(((QuickJumpRow) mQuickList.getItemAtPosition(mQuickListSelectedIndex)).index);
            }
        });

        mQuickList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mQuickListSelectedIndex = position;
                if (mQuickListSelected)
                    mFragment.setSelectedPosition(((QuickJumpRow) mQuickList.getItemAtPosition(position)).index);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        if (findViewById(R.id.fragment_container) != null) {

            if (savedInstanceState != null) {
                return;
            }

            mFragment = new ContainerGridFragment();
            mFragment.setArguments(getIntent().getExtras());

            getFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, mFragment).commit();
        }
    }

    public void setQuickListVisible(boolean visible) {

        (findViewById(R.id.toolbarQuickJump)).setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void setQuickJumpList(List<QuickJumpRow> quickjumpList) {

        mQuickList.setAdapter(new ArrayAdapter<>(this, R.layout.quickjumprow, android.R.id.text1, quickjumpList));
    }

    public static class QuickJumpRow {

        public static final String NUM_OR_SYMBOL = "#";

        public final String letter;
        public final int index;

        public QuickJumpRow(String letter, int index) {
            this.letter = letter;
            this.index = index;
        }

        @Override
        public String toString() { return letter; }
    }
}
