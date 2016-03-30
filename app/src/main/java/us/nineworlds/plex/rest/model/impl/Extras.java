package us.nineworlds.plex.rest.model.impl;

import android.os.Parcel;
import android.os.Parcelable;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;



@Root(name="Extras")
public class Extras implements Parcelable {

    public Extras() {}

    @ElementList(inline=true,name="Video",required=false)
    private List<Video> videos;

    protected Extras(Parcel in) {
        videos = in.createTypedArrayList(Video.CREATOR);
    }

    public static final Creator<Extras> CREATOR = new Creator<Extras>() {
        @Override
        public Extras createFromParcel(Parcel in) {
            return new Extras(in);
        }

        @Override
        public Extras[] newArray(int size) {
            return new Extras[size];
        }
    };

    public List<Video> getVideos() { return videos; }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(videos);
    }
}
