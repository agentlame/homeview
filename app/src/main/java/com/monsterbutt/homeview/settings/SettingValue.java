package com.monsterbutt.homeview.settings;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.monsterbutt.homeview.R;


public abstract class SettingValue implements Parcelable {

    public static final String namespace = "http://schemas.android.com/apk/res/android";
    public static final String Title = "title";
    private static final String Key = "key";
    private static final String Summary = "summary";
    private static final String Dependency = "dependency";
    private static final String DialogTitle = "dialogTitle";
    private static final String Drawable = "drawable";
    private static final String Enabled = "enabled";
    private static final int INVALID_ID = 0;

    private static final String boolFalse  = "false";

    private String    mKey;
    private int       mDrawableId;
    private String    mTitle;
    private String    mSummary;
    private String    mDependencyKey;
    private String    mDialogTitle;
    private boolean   mEnabled;

    protected SettingValue(Context context, XmlResourceParser xml) {

        mDrawableId = getIdForResource(xml, Drawable);
        if (INVALID_ID == mDrawableId)
            mDrawableId = R.drawable.ic_settings_white_48dp;
        mTitle = getStringAttribute(context, xml, Title);
        mKey = xml.getAttributeValue(namespace, Key);
        mSummary = getStringAttribute(context, xml, Summary);
        mDependencyKey = xml.getAttributeValue(namespace, Dependency);
        mDialogTitle = getStringAttribute(context, xml, DialogTitle);
        String enabled = xml.getAttributeValue(namespace, Enabled);
        mEnabled = TextUtils.isEmpty(enabled) || !enabled.equalsIgnoreCase(boolFalse);
    }

    protected SettingValue(String name, int drawableId) {

        mDrawableId = drawableId;
        if (INVALID_ID == mDrawableId)
            mDrawableId = R.drawable.ic_settings_white_48dp;
        mTitle = name;
        mKey = "";
        mSummary = "";
        mDependencyKey = "";
        mDialogTitle = "";
    }

    protected SettingValue(Parcel in) {

        mKey = in.readString();
        mDrawableId = in.readInt();
        mTitle = in.readString();
        mSummary = in.readString();
        mDependencyKey = in.readString();
        mDialogTitle = in.readString();
    }

    private static int getIdForResource(XmlResourceParser xml, String xmlKey) {

        String key = xml.getAttributeValue(namespace, xmlKey);
        if (TextUtils.isEmpty(key))
            return INVALID_ID;

        return Integer.valueOf(key.replace("@", ""));
    }

    public static String getStringAttribute(Context context, XmlResourceParser xml, String xmlKey) {

        int id = getIdForResource(xml, xmlKey);
        if (id == INVALID_ID)
            return "";
        return context.getResources().getString(id);
    }

    protected static String[] getStringArrayAttribute(Context context, XmlResourceParser xml, String xmlKey) {

        int id = getIdForResource(xml, xmlKey);
        if (id == INVALID_ID)
            return new String[0];
        return context.getResources().getStringArray(id);
    }

    public String   key()           { return mKey; }
    public int      drawableId()    { return mDrawableId; }
    public String   title()         { return mTitle; }
    public String   summary()       { return mSummary; }
    public String   dependencyKey() { return mDependencyKey; }
    public String   dialogTitle()   { return mDialogTitle; }
    public boolean  enabled()       { return mEnabled; }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeString(key());
        dest.writeInt(drawableId());
        dest.writeString(title());
        dest.writeString(summary());
        dest.writeString(dependencyKey());
        dest.writeString(dialogTitle());
    }

    public abstract SettingValue reload(Context context);
}
