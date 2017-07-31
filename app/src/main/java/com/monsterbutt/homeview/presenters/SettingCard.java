package com.monsterbutt.homeview.presenters;

import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.media.tv.TvContractCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.TvUtil;
import com.monsterbutt.homeview.model.MockDatabase;
import com.monsterbutt.homeview.model.Subscription;
import com.monsterbutt.homeview.plex.PlexServer;
import com.monsterbutt.homeview.services.UpdateRecommendationsService;
import com.monsterbutt.homeview.settings.SettingArray;
import com.monsterbutt.homeview.settings.SettingBoolean;
import com.monsterbutt.homeview.settings.SettingLaunch;
import com.monsterbutt.homeview.settings.SettingText;
import com.monsterbutt.homeview.settings.SettingValue;
import com.monsterbutt.homeview.ui.activity.SettingEditTextActivity;
import com.monsterbutt.homeview.ui.activity.SettingsArrayActivity;
import com.monsterbutt.homeview.ui.android.ImageCardView;
import com.monsterbutt.homeview.ui.fragment.SettingsFragment;

import java.util.Arrays;
import java.util.List;

import static android.content.ContentValues.TAG;


public class SettingCard extends CardObject {

    public static int BASE_RESULT = 0;

    protected Drawable icon;
    protected final String title;
    protected final Context context;
    private SettingValue mSetting;

    public SettingCard(Context context, SettingValue setting) {

        super(context);
        this.context = context;
        updateValue(setting, null);
        this.icon = context.getDrawable(mSetting.drawableId());
        title = mSetting.title();
    }

    public boolean isBooleanSetting() { return mSetting instanceof SettingBoolean; }

    @Override
    public String getKey() { return mSetting.key(); }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public Drawable getImage(Context context) {
        return icon;
    }

    @Override
    public int getHeight() {

        return R.dimen.CARD_SQUARE;
    }

    @Override
    public int getWidth() {

        return R.dimen.CARD_SQUARE;
    }

    public void updateValue(SettingValue value, ImageCardView view) {

        mSetting = value;
        if (view != null) {
            view.setContentText(getContent());
        }
    }

    @Override
    public String getContent() {

        if (mSetting instanceof SettingArray)
            return ((SettingArray)mSetting).getValueText();
        else if (mSetting instanceof SettingBoolean)
            return context.getString(((SettingBoolean) mSetting).value() ? R.string.preferences_checked_true : R.string.preferences_checked_false);
        else if (mSetting instanceof SettingText)
            return ((SettingText)mSetting).value();
        else if (mSetting instanceof SettingLaunch)
            return (((SettingLaunch) mSetting).content());
        return "";
    }

    @Override
    public boolean onClicked(Fragment fragment, Bundle extras, View transitionView) {

        if (mSetting instanceof SettingArray) {

            Intent intent = new Intent(fragment.getActivity(), SettingsArrayActivity.class);
            intent.putExtra(SettingsArrayActivity.SETTING, mSetting);
            if (extras != null)
                intent.putExtras(extras);
            fragment.startActivityForResult(intent, BASE_RESULT);
        }
        else if (mSetting instanceof SettingBoolean)
            ((SettingBoolean)mSetting).value(context, !((SettingBoolean)mSetting).value());
        else if (mSetting instanceof SettingText) {

            Intent intent = new Intent(fragment.getActivity(), SettingEditTextActivity.class);
            intent.putExtra(SettingEditTextActivity.SETTING, mSetting);
            if (extras != null)
                intent.putExtras(extras);
            fragment.startActivityForResult(intent, BASE_RESULT);
        }
        else if (mSetting instanceof SettingLaunch) {

            try {
                SettingLaunch launch = (SettingLaunch) mSetting;
                if (!TextUtils.isEmpty(launch.className())) {
                    Class clas = Class.forName(launch.className());
                    Intent intent = new Intent(fragment.getActivity(), clas);
                    if (extras != null)
                        intent.putExtras(extras);
                    fragment.startActivityForResult(intent, launch.result());
                    return true;
                }
                else {
                    String key = launch.key();
                    if (!TextUtils.isEmpty(key) && fragment instanceof SettingsFragment) {

                        Subscription sub = null;
                        switch(key) {
                            case "preferences_server_channels_movies":
                                sub = MockDatabase.getRecentMoviesSubscription(fragment.getContext());
                                break;
                            case "preferences_server_channels_shows":
                                sub = MockDatabase.getRecentShowSubscription(fragment.getContext());
                                break;
                        }
                        if (sub != null) {
                            new AddChannelTask((SettingsFragment) fragment).execute(sub);
                            return true;
                        }
                    }
                    return false;
                }
            }
            catch (ClassNotFoundException e) {
                Log.e(getClass().getName(), e.toString());
            }
            return false;
        }

        return true;
    }

    private class AddChannelTask extends AsyncTask<Subscription, Void, Long> {

        private final SettingsFragment mFragment;

        AddChannelTask(SettingsFragment fragment) {
            this.mFragment = fragment;
        }

        @Override
        protected Long doInBackground(Subscription... varArgs) {
            List<Subscription> subscriptions = Arrays.asList(varArgs);
            if (subscriptions.size() != 1) {
                return -1L;
            }
            Subscription subscription = subscriptions.get(0);
            // TODO: step 16 create channel. Replace declaration with code from code lab.
            long channelId = TvUtil.createChannel(mContext, subscription);

            subscription.setChannelId(channelId);
            MockDatabase.saveSubscription(mContext, subscription);
            // Scheduler listen on channel's uri. Updates after the user interacts with the system
            // dialog.
            context.startService(new Intent(context, UpdateRecommendationsService.class));

            return channelId;
        }

        @Override
        protected void onPostExecute(Long channelId) {
            promptUserToDisplayChannel(channelId);
        }

        private void promptUserToDisplayChannel(long channelId) {
            // TODO: step 17 prompt user.
            Intent intent = new Intent(TvContractCompat.ACTION_REQUEST_CHANNEL_BROWSABLE);
            intent.putExtra(TvContractCompat.EXTRA_CHANNEL_ID, channelId);
            try {
                mFragment.getActivity().startActivityForResult(intent, TvUtil.MAKE_BROWSABLE_REQUEST_CODE);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "Could not start activity: " + intent.getAction(), e);
            }
        }
    }
}
