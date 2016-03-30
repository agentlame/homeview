package com.monsterbutt.homeview.plex.media;

import android.os.Parcel;
import android.os.Parcelable;

import us.nineworlds.plex.rest.ResourcePaths;
import us.nineworlds.plex.rest.model.impl.Directory;


public class Videos  extends  PlexContainerItem implements Parcelable {

    public static final String TYPE = "movie";
    public static final String SCANNER = "Plex Video Files Scanner";

    Videos(Directory directory) {
        super(directory);
    }

    protected Videos(Parcel in) {
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

    public static final Creator<Videos> CREATOR = new Creator<Videos>() {
        @Override
        public Videos createFromParcel(Parcel in) {
            return new Videos(in);
        }

        @Override
        public Videos[] newArray(int size) {
            return new Videos[size];
        }
    };

    @Override
    public String getBaseFilter() { return "folder"; }

    @Override
    public String getKey() {
        return ResourcePaths.SECTIONS_PATH + super.getKey();
    }
}
