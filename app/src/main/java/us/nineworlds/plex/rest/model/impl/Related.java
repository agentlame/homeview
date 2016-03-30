package us.nineworlds.plex.rest.model.impl;

import android.os.Parcel;
import android.os.Parcelable;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;



@Root(name="Related")
public class Related implements Parcelable {

    public Related() {}

    @ElementList(inline=true,name="Hub",required=false)
    private List<Hub> hubs;

    protected Related(Parcel in) {
    }

    public static final Creator<Related> CREATOR = new Creator<Related>() {
        @Override
        public Related createFromParcel(Parcel in) {
            return new Related(in);
        }

        @Override
        public Related[] newArray(int size) {
            return new Related[size];
        }
    };

    public List<Hub> getHubs() { return hubs; }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }
}
