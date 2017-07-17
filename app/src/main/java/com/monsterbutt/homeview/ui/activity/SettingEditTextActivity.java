package com.monsterbutt.homeview.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;
import android.support.v4.app.FragmentActivity;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.settings.SettingText;

import java.util.List;


public class SettingEditTextActivity extends FragmentActivity {

    private static final int DONE   = 1000;
    private static final int CANCEL   = 1001;
    private static final int TEXT   = 1;

    public static final String SETTING = "setting";

    private SettingText mSetting;

    private static SettingEditTextActivity activity;
    private String value;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        activity = this;

        if (null == savedInstanceState) {

            Intent intent = getIntent();
            mSetting = intent.getParcelableExtra(SETTING);
            value = mSetting.value();
            GuidedStepFragment.addAsRoot(this, new EditTextStepFragment(), android.R.id.content);
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

    public static class EditTextStepFragment extends GuidedStepFragment {

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


            addEditAction(actions, TEXT, activity.value, getResources().getString(R.string.preferences_setvalue));

            addAction(actions, DONE,
                    getResources().getString(R.string.preferences_save),
                    getResources().getString(R.string.preferences_save_desc));
            addAction(actions, CANCEL,
                    getResources().getString(R.string.preferences_cancel),
                    getResources().getString(R.string.preferences_cancel_desc));
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {

            if (action.getId() == DONE) {

                activity.mSetting.value(activity, activity.value);
                getActivity().finishAfterTransition();
            }
            else if (action.getId() == CANCEL)
                getActivity().finishAfterTransition();
            else if (action.getId() == TEXT) {

                activity.value = action.getEditTitle().toString();
                action.setTitle(activity.value);
            }
        }
    }
}
