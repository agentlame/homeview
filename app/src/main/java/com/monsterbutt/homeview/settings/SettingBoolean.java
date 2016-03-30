package com.monsterbutt.homeview.settings;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.os.Parcel;


public class SettingBoolean extends SettingValue {

    public static final String NODE_NAME = "CheckBoxPreference";

    private static final String DefaultValue = "defaultValue";

    protected boolean mValue;
    protected final boolean mDefValue;

    public SettingBoolean(Context context, XmlResourceParser xml) {

        super(context, xml);
        mDefValue = xml.getAttributeBooleanValue(namespace, DefaultValue, false);
        reload(context);
    }

    public SettingBoolean(Parcel in) {
        super(in);
        mDefValue = in.readInt() == 1;
        mValue = in.readInt() == 1;
    }

    public static final Creator<SettingBoolean> CREATOR = new Creator<SettingBoolean>() {
        @Override
        public SettingBoolean createFromParcel(Parcel in) {
            return new SettingBoolean(in);
        }

        @Override
        public SettingBoolean[] newArray(int size) {
            return new SettingBoolean[size];
        }
    };

    public boolean value() { return mValue; }
    public boolean defValue() { return mDefValue; }

    public void value(Context context, boolean value) {

        mValue = value;
        context.getSharedPreferences("", Context.MODE_PRIVATE).edit().putBoolean(key(), value).commit();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(defValue() ? 1 : 0);
        dest.writeInt(value() ? 1 : 0);
    }

    @Override
    public SettingValue reload(Context context) {
        mValue = context.getSharedPreferences("", Context.MODE_PRIVATE).getBoolean(key(), mDefValue);
        return this;
    }
}
