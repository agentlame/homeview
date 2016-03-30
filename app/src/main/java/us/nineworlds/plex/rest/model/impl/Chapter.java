package us.nineworlds.plex.rest.model.impl;

import android.os.Parcel;
import android.os.Parcelable;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;


@Root(name="Chapter")
public class Chapter implements Parcelable {

    public Chapter() {}
    @Attribute(required=false)
    private String id;

    @Attribute(required=false)
    private String tag;

    @Attribute(required=false)
    private long index;

    @Attribute(required=false)
    private long startTimeOffset;

    @Attribute(required=false)
    private long endTimeOffset;

    @Attribute(required=false)
    private String thumb;

    protected Chapter(Parcel in) {
        id = in.readString();
        tag = in.readString();
        index = in.readLong();
        startTimeOffset = in.readLong();
        endTimeOffset = in.readLong();
        thumb = in.readString();
    }

    public static final Creator<Chapter> CREATOR = new Creator<Chapter>() {
        @Override
        public Chapter createFromParcel(Parcel in) {
            return new Chapter(in);
        }

        @Override
        public Chapter[] newArray(int size) {
            return new Chapter[size];
        }
    };

    public String getId() { return id; }
    public String getTag() { return tag; }
    public long getindex() { return index; }
    public long getStartTimeOffset() { return startTimeOffset; }
    public long getEndTimeOffset() { return endTimeOffset; }
    public String getThumb() { return thumb;}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(tag);
        dest.writeLong(index);
        dest.writeLong(startTimeOffset);
        dest.writeLong(endTimeOffset);
        dest.writeString(thumb);
    }
}
