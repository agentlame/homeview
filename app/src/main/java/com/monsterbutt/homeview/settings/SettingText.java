package com.monsterbutt.homeview.settings;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.os.Parcel;


public class SettingText extends SettingValue {

    public static final String NODE_NAME = "EditTextPreference";

    private static final String DefaultValue = "defaultValue";

    private String mValue;
    private final String mDefValue;
    private final String mSectionTitle;

    public SettingText(Context context, String sectionTitle, XmlResourceParser xml) {

        super(context, xml);
        mSectionTitle = sectionTitle;
        mDefValue = getStringAttribute(context, xml, DefaultValue);
        reload(context);
    }

    public SettingText(Parcel in) {
        super(in);
        mDefValue = in.readString();
        mValue = in.readString();
        mSectionTitle = in.readString();
    }

    public static final Creator<SettingText> CREATOR = new Creator<SettingText>() {
        @Override
        public SettingText createFromParcel(Parcel in) {
            return new SettingText(in);
        }

        @Override
        public SettingText[] newArray(int size) {
            return new SettingText[size];
        }
    };

    public String value() { return mValue; }
    public String defValue() { return mDefValue; }
    public String sectionTitle() { return mSectionTitle; }

    public void value(Context context, String value) {

        mValue = value;
        context.getSharedPreferences("", Context.MODE_PRIVATE).edit().putString(key(), value).commit();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(defValue());
        dest.writeString(value());
        dest.writeString(sectionTitle());
    }

    @Override
    public SettingValue reload(Context context) {
        mValue = context.getSharedPreferences("", Context.MODE_PRIVATE).getString(key(), mDefValue);
        return this;
    }
}
