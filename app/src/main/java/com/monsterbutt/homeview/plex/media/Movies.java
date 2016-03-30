package com.monsterbutt.homeview.plex.media;

import android.os.Parcel;
import android.os.Parcelable;

import us.nineworlds.plex.rest.ResourcePaths;
import us.nineworlds.plex.rest.model.impl.Directory;


public class Movies extends PlexContainerItem implements Parcelable {

    public static final String TYPE = "movie";

    public Movies(Directory directory) {
        super(directory);
    }

    protected Movies(Parcel in) {
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

    public static final Creator<Movies> CREATOR = new Creator<Movies>() {
        @Override
        public Movies createFromParcel(Parcel in) {
            return new Movies(in);
        }

        @Override
        public Movies[] newArray(int size) {
            return new Movies[size];
        }
    };

    @Override
    public String getKey() {
        return ResourcePaths.SECTIONS_PATH + super.getKey();
    }
}
