package com.monsterbutt.homeview.plex.media;

import android.os.Parcel;
import android.os.Parcelable;

import com.monsterbutt.homeview.R;

import us.nineworlds.plex.rest.model.impl.Directory;


public class Folder extends PlexContainerItem implements Parcelable {

    protected Folder(Directory directory) {
        super(directory);
    }

    protected Folder(Parcel in) {
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

    public static final Creator<Folder> CREATOR = new Creator<Folder>() {
        @Override
        public Folder createFromParcel(Parcel in) {
            return new Folder(in);
        }

        @Override
        public Folder[] newArray(int size) {
            return new Folder[size];
        }
    };

    @Override
    public  int getCardImageId() { return R.drawable.ic_folder_open_white_48dp; }
}
