package com.monsterbutt.homeview.ui.grid;


import android.os.Parcel;
import android.os.Parcelable;

class SectionFilter implements Parcelable {

    public String name;
    public final String key;

    SectionFilter(String name, String key) {
        this.name = name;
        this.key = key;
    }

    SectionFilter(Parcel in) {
        name = in.readString();
        key = in.readString();
    }

    public static final Creator<SectionFilter> CREATOR = new Creator<SectionFilter>() {
        @Override
        public SectionFilter createFromParcel(Parcel in) {
            return new SectionFilter(in);
        }

        @Override
        public SectionFilter[] newArray(int size) {
            return new SectionFilter[size];
        }
    };

    @Override
    public String toString() { return name; }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(key);
    }
}
