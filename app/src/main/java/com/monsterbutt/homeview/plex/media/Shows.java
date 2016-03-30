package com.monsterbutt.homeview.plex.media;

import android.os.Parcel;
import android.os.Parcelable;

import us.nineworlds.plex.rest.ResourcePaths;
import us.nineworlds.plex.rest.model.impl.Directory;


public class Shows extends PlexContainerItem implements Parcelable {

    public static final String TYPE = "show";

    Shows(Directory directory) {
        super(directory);
    }

    protected Shows(Parcel in) {
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

    public static final Creator<Shows> CREATOR = new Creator<Shows>() {
        @Override
        public Shows createFromParcel(Parcel in) {
            return new Shows(in);
        }

        @Override
        public Shows[] newArray(int size) {
            return new Shows[size];
        }
    };

    @Override
    public String getKey() {
        return ResourcePaths.SECTIONS_PATH + super.getKey();
    }
}
