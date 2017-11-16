package com.monsterbutt.homeview.ui;

import android.os.Parcel;
import android.os.Parcelable;


public class HubInfo implements Parcelable {

    final public String name;
    final public String key;
    final public String path;

    public HubInfo(String name, String key, String path) {
        this.name = name;
        this.key = key;
        this.path = path;
    }

    private HubInfo(Parcel in) {
        name = in.readString();
        key = in.readString();
        path = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(key);
        dest.writeString(path);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<HubInfo> CREATOR = new Creator<HubInfo>() {
        @Override
        public HubInfo createFromParcel(Parcel in) {
            return new HubInfo(in);
        }

        @Override
        public HubInfo[] newArray(int size) {
            return new HubInfo[size];
        }
    };

}
