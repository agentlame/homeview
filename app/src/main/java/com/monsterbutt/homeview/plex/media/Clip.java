package com.monsterbutt.homeview.plex.media;

import android.os.Parcel;
import android.os.Parcelable;

import us.nineworlds.plex.rest.model.impl.Video;


public class Clip extends Episode implements Parcelable {

    public static final String TYPE = "clip";

    public Clip(Video video) {
        super(video);
    }

    protected Clip(Parcel in) {
        super(in);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Clip> CREATOR = new Creator<Clip>() {
        @Override
        public Clip createFromParcel(Parcel in) {
            return new Clip(in);
        }

        @Override
        public Clip[] newArray(int size) {
            return new Clip[size];
        }
    };
}
