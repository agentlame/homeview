package com.monsterbutt.homeview.ui.grid;


import android.os.Parcel;
import android.os.Parcelable;

import com.monsterbutt.homeview.ui.grid.interfaces.IGridSorter;

class SectionSort extends SectionFilter implements Parcelable {

    public final IGridSorter.ItemSort id;
    boolean isAscending = true;

    SectionSort(String name, IGridSorter.ItemSort key) {

        super(name, name);
        this.id = key;
    }

    private SectionSort(Parcel in) {

        super(in);
        id = IGridSorter.ItemSort.valueOf(in.readString());
        isAscending = in.readInt() == 1;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        super.writeToParcel(dest, flags);
        dest.writeString(id.name());
        dest.writeInt(isAscending ? 1 : 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<SectionSort> CREATOR = new Creator<SectionSort>() {
        @Override
        public SectionSort createFromParcel(Parcel in) {
            return new SectionSort(in);
        }

        @Override
        public SectionSort[] newArray(int size) {
            return new SectionSort[size];
        }
    };
}
