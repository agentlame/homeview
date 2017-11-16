package com.monsterbutt.homeview.settings;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;


public class SettingArray extends SettingValue implements Parcelable {

    public static class ArrayValue implements Parcelable, Comparable<ArrayValue> {

        public final int index;
        public final String value;
        public final String key;

        ArrayValue(Parcel in) {
            value = in.readString();
            index = in.readInt();
            key = in.readString();
        }

        ArrayValue(int index, String value, String key) {
            this.index = index;
            this.value = value;
            this.key = key;
        }

        public static final Creator<ArrayValue> CREATOR = new Creator<ArrayValue>() {
            @Override
            public ArrayValue createFromParcel(Parcel in) {
                return new ArrayValue(in);
            }

            @Override
            public ArrayValue[] newArray(int size) {
                return new ArrayValue[size];
            }
        };

        @Override
        public int compareTo(@NonNull ArrayValue r) {

            if (this.index < r.index)
                return -1;
            else
                return 1;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(value);
            dest.writeInt(index);
            dest.writeString(key);
        }
    }


    static final String NODE_NAME = "ListPreference";

    private final static String Entries = "entries";
    private final static String EntryValues = "entryValues";
    private final static String DefaultValue = "defaultValue";

    private HashMap<String, ArrayValue> mEntriesMap = new HashMap<>();
    private final String mDefValue;
    private String mValue;

    private final String mSectionTitle;

    SettingArray(Context context, String sectionTitle, XmlResourceParser xml) {
        super(context, xml);

        final String[] entries = getStringArrayAttribute(context, xml, Entries);
        final String[] entriesValue = getStringArrayAttribute(context, xml, EntryValues);
        if (entries.length != entriesValue.length) throw new AssertionError();
        for (int i = 0; i < entries.length; ++i)
            mEntriesMap.put(entriesValue[i], new ArrayValue(i, entries[i], entriesValue[i]));

        mSectionTitle = sectionTitle;
        mDefValue = getStringAttribute(context, xml, DefaultValue);
        reload(context);
    }

    private SettingArray(Parcel in) {
        super(in);
        mDefValue = in.readString();
        mValue = in.readString();
        mSectionTitle = in.readString();
        int count = in.readInt();
        for (int i = 0; i < count; ++i) {

            String key = in.readString();
            int index = in.readInt();
            String value = in.readString();
            mEntriesMap.put(key, new ArrayValue(index, value, key));
        }
    }

    public static final Creator<SettingArray> CREATOR = new Creator<SettingArray>() {
        @Override
        public SettingArray createFromParcel(Parcel in) {
            return new SettingArray(in);
        }

        @Override
        public SettingArray[] newArray(int size) {
            return new SettingArray[size];
        }
    };

    public void currentValue(Context context, String value) {

        mValue = value;
        context.getSharedPreferences("", Context.MODE_PRIVATE).edit().putString(key(), value).apply();

        SettingsManager.getInstance().reloadSetting(context, key());
    }

    public String currentValue() { return mValue; }
    public Map<String, ArrayValue> values() { return mEntriesMap; }
    private String defValue() { return mDefValue; }
    public String sectionTitle() { return mSectionTitle; }

    public String getValueText() {
        ArrayValue val = mEntriesMap.get(mValue);
        if (val == null)
            return "";
        return val.value; }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(defValue());
        dest.writeString(currentValue());
        dest.writeString(sectionTitle());
        dest.writeInt(mEntriesMap.size());
        for (Map.Entry<String, ArrayValue> entry : mEntriesMap.entrySet()) {
            dest.writeString(entry.getKey());
            dest.writeInt(entry.getValue().index);
            dest.writeString(entry.getValue().value);
        }
    }

    @Override
    public SettingValue reload(Context context) {
        mValue = context.getSharedPreferences("", Context.MODE_PRIVATE).getString(key(), mDefValue);
        return this;
    }
}
