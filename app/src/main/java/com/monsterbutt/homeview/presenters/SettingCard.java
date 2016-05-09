package com.monsterbutt.homeview.presenters;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.monsterbutt.homeview.R;
import com.monsterbutt.homeview.settings.SettingArray;
import com.monsterbutt.homeview.settings.SettingBoolean;
import com.monsterbutt.homeview.settings.SettingLaunch;
import com.monsterbutt.homeview.settings.SettingText;
import com.monsterbutt.homeview.settings.SettingValue;
import com.monsterbutt.homeview.ui.activity.SettingEditTextActivity;
import com.monsterbutt.homeview.ui.activity.SettingsArrayActivity;
import com.monsterbutt.homeview.ui.android.ImageCardView;


public class SettingCard extends CardObject {

    public static int BASE_RESULT = 0;

    protected Drawable icon;
    protected final String title;
    protected final Context context;
    private SettingValue mSetting;

    public SettingCard(Context context, SettingValue setting) {

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
                Class clas = Class.forName(launch.className());
                Intent intent = new Intent(fragment.getActivity(), clas);
                if (extras != null)
                    intent.putExtras(extras);
                fragment.startActivityForResult(intent, launch.result());
                return true;
            }
            catch (ClassNotFoundException e) {
                Log.e(getClass().getName(), e.toString());
            }
            return false;
        }

        return true;
    }
}
