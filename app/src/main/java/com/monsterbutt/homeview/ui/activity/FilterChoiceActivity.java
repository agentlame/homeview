package com.monsterbutt.homeview.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.ui.fragment.ContainerGridFragment;

import java.util.List;


public class FilterChoiceActivity extends Activity {

    public  static final int CANCEL = 999;

    public static final String FILTERS = "filters";
    public static final String CURRENT_INDEX = "index";
    public static final String TITLE = "title";

    private static FilterChoiceActivity activity;

    private List<ContainerGridFragment.SectionFilter> mFilters = null;
    private String breadcrumb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        activity = this;

        if (null == savedInstanceState) {

            Intent intent = getIntent();
            mFilters = intent.getParcelableArrayListExtra(FILTERS);
            breadcrumb = intent.getStringExtra(TITLE);
            FilterStepFragment frag = new FilterStepFragment();
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

    public static class FilterStepFragment extends GuidedStepFragment {

        @Override
        @NonNull
        public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {

            return new GuidanceStylist.Guidance(
                    activity.getString(R.string.container_filter_selection_title),
                    activity.getString(R.string.container_filter_selection_summary),
                    activity.breadcrumb,
                    getActivity().getDrawable(R.drawable.launcher));
        }

        @Override
        public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {

            int index = 0;
            for (ContainerGridFragment.SectionFilter filter : activity.mFilters)
                addAction(actions, index++, filter.name, "");
            addAction(actions, CANCEL,
                    getResources().getString(R.string.preferences_cancel),
                    getResources().getString(R.string.preferences_cancel_desc));
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {

            activity.setResult((int) action.getId());
            getActivity().finishAfterTransition();
        }
    }
}
