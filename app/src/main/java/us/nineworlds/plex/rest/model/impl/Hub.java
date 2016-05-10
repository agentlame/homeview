package us.nineworlds.plex.rest.model.impl;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;


@Root(name="Hub")
public class Hub  {

    public Hub() {}

    @Attribute(required=false)
    private String key;

    @Attribute(required=true)
    private int size;

    @Attribute(required=false)
    private String hubIdentifier;

    @Attribute(required=false)
    private String type;

    @Attribute(required=false)
    private String hubKey;

    @Attribute(required=false)
    private String title;

    @Attribute(required=false)
    private String more;

    @ElementList(inline=true,required=false)
    private List<Video> videos;

    @ElementList(inline=true,required=false)
    private List<Directory> directories;

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public String getHubIdentifier() { return hubIdentifier; }
    public void setHubIdentifier(String hubIdentifier) { this.hubIdentifier = hubIdentifier; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getHubKey() { return hubKey; }
    public void setHubKey(String hubKey) { this.hubKey = hubKey; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMore() { return more; }
    public void setMore(String more) { this.more = more; }

    /**
     * @return the videos
     */
    public List<Video> getVideos() {
        return videos;
    }

    public List<Directory> getDirectories() { return directories; }

    /**
     * @param videos the videos to set
     */
    public void setVideos(List<Video> videos) {
        this.videos = videos;
    }
    public void setDirectories(List<Directory> directories) {
        this.directories = directories;
    }

    public String getKey() { return key; }
}
