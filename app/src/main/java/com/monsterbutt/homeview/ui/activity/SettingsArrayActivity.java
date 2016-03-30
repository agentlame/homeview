package com.monsterbutt.homeview.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;
import android.text.TextUtils;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.settings.SettingArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class SettingsArrayActivity extends Activity {

    private static final int CANCEL   = 1001;
    public static final String SETTING = "setting";

    private SettingArray mSetting;
    private Map<String, SettingArray.ArrayValue> values;

    private static SettingsArrayActivity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        activity = this;

        if (null == savedInstanceState) {

            Intent intent = getIntent();
            mSetting = intent.getParcelableExtra(SETTING);
            values = mSetting.values();
            ArraySelectStepFragment frag = new ArraySelectStepFragment();
            GuidedStepFragment.addAsRoot(this, frag, android.R.id.content);
        }
    }

    private static void addAction(List<GuidedAction> actions, long id, String title, String desc) {
        actions.add(new GuidedAction.Builder()
                .id(id)
                .title(title)
                .description(desc)
                .build());
    }

    public static class ArraySelectStepFragment extends GuidedStepFragment {

        @Override
        @NonNull
        public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {

            return new GuidanceStylist.Guidance(activity.mSetting.title(),
                    activity.mSetting.summary(),
                    activity.mSetting.sectionTitle(),
                    getActivity().getDrawable(R.drawable.launcher));
        }

        @Override
        public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {

            int index = 0;

            List<SettingArray.ArrayValue> list = new ArrayList<>(activity.values.values());
            Collections.sort(list);

            for (SettingArray.ArrayValue val : list)
                addAction(actions, index++, val.value, "");
            addAction(actions, CANCEL,
                    getResources().getString(R.string.preferences_cancel),
                    getResources().getString(R.string.preferences_cancel_desc));
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {

            if (action.getId() != CANCEL) {

                List<SettingArray.ArrayValue> list = new ArrayList<>(activity.values.values());
                Collections.sort(list);
                SettingArray.ArrayValue val = list.get((int)action.getId());
                if (val != null)
                    activity.mSetting.currentValue(activity, val.key);
            }
            getActivity().finishAfterTransition();
        }
    }
}