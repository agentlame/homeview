/**
 * The MIT License (MIT)
 * Copyright (c) 2012 David Carver
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF
 * OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package us.nineworlds.plex.rest.model.impl;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

@Root(name="MediaContainer")
public class MediaContainer implements Parcelable {

	public MediaContainer() {}

	@Attribute(required=true)
	private int size;
	
	@Attribute(required=false)
	private int allowSync;
	
	@Attribute(required=false)
	private String art;
	
	@Attribute(required=false)
	private String identifier;
	
	@Attribute(required=false)
	private String mediaTagPrefix;
	
	@Attribute(required=false)
	private long mediaTagVersion;

	@Attribute(required=false)
	private String friendlyName;
	
	@Attribute(required=false)
	private String title1;
	
	@Attribute(required=false)
	private String title2;

	protected MediaContainer(Parcel in) {
		size = in.readInt();
		allowSync = in.readInt();
		art = in.readString();
		identifier = in.readString();
		mediaTagPrefix = in.readString();
		mediaTagVersion = in.readLong();
		friendlyName = in.readString();
		title1 = in.readString();
		title2 = in.readString();
		sortAsc = in.readInt();
		content = in.readString();
		viewGroup = in.readString();
		viewMode = in.readInt();
		parentPosterURL = in.readString();
		parentIndex = in.readLong();
		parentYear = in.readString();
		directories = in.createTypedArrayList(Directory.CREATOR);
		videos = in.createTypedArrayList(Video.CREATOR);
		tracks = in.createTypedArrayList(Track.CREATOR);
		hubs = in.createTypedArrayList(Hub.CREATOR);
		photos = in.createTypedArrayList(Photo.CREATOR);
		librarySectionID = in.readString();
		librarySectionTitle = in.readString();
		grandparentContentRating = in.readString();
		grandparentRatingKey = in.readLong();
		grandparentKey = in.readString();
		grandparentStudio = in.readString();
		grandparentTheme = in.readString();
		grandparentThumb = in.readString();
		grandparentTitle = in.readString();
		key = in.readLong();
		theme = in.readString();
		banner = in.readString();
		summary = in.readString();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(size);
		dest.writeInt(allowSync);
		dest.writeString(art);
		dest.writeString(identifier);
		dest.writeString(mediaTagPrefix);
		dest.writeLong(mediaTagVersion);
		dest.writeString(friendlyName);
		dest.writeString(title1);
		dest.writeString(title2);
		dest.writeInt(sortAsc);
		dest.writeString(content);
		dest.writeString(viewGroup);
		dest.writeInt(viewMode);
		dest.writeString(parentPosterURL);
		dest.writeLong(parentIndex);
		dest.writeString(parentYear);
		dest.writeTypedList(directories);
		dest.writeTypedList(videos);
		dest.writeTypedList(tracks);
		dest.writeTypedList(hubs);
		dest.writeTypedList(photos);
		dest.writeString(librarySectionID);
		dest.writeString(librarySectionTitle);
		dest.writeString(grandparentContentRating);
		dest.writeLong(grandparentRatingKey);
		dest.writeString(grandparentKey);
		dest.writeString(grandparentStudio);
		dest.writeString(grandparentTheme);
		dest.writeString(grandparentThumb);
		dest.writeString(grandparentTitle);
		dest.writeLong(key);
		dest.writeString(theme);
		dest.writeString(banner);
		dest.writeString(summary);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public static final Creator<MediaContainer> CREATOR = new Creator<MediaContainer>() {
		@Override
		public MediaContainer createFromParcel(Parcel in) {
			return new MediaContainer(in);
		}

		@Override
		public MediaContainer[] newArray(int size) {
			return new MediaContainer[size];
		}
	};

	public String getTitle2() {
		return title2;
	}

	public void setTitle2(String title2) {
		this.title2 = title2;
	}

	@Attribute(required=false)
	private int sortAsc;
	
	@Attribute(required=false)
	private String content;
	
	@Attribute(required=false)
	private String viewGroup;
	
	@Attribute(required=false)
	private int viewMode;
	
	@Attribute(name="thumb",required=false)
	private String parentPosterURL;
	
	@Attribute(name="parentIndex",required=false)
	private long parentIndex;

	@Attribute(required=false)
	private String parentYear;
	
	@ElementList(inline=true,required=false)
	private List<Directory> directories;
	
	@ElementList(inline=true,required=false)
	private List<Video> videos;
	
	@ElementList(inline=true,required=false)
	private List<Track> tracks;

	@ElementList(inline=true,required=false)
	private List<Hub> hubs;

	@ElementList(inline=true,required=false)
	private List<Photo> photos;

	@Attribute(required=false)
	private String librarySectionID;

	@Attribute(required=false)
	private String librarySectionTitle;

	@Attribute(required=false)
	private String grandparentContentRating;

	@Attribute(required=false)
	private long grandparentRatingKey;

	@Attribute(required=false)
	private String grandparentKey;

	@Attribute(required=false)
	private String grandparentStudio;

	@Attribute(required=false)
	private String grandparentTheme;

	@Attribute(required=false)
	private String grandparentThumb;

	@Attribute(required=false)
	private String grandparentTitle;

	@Attribute(required=false)
	private long key;

	@Attribute(required=false)
	private String theme;

	@Attribute(required=false)
	private String banner;

	@Attribute(required=false)
	private String summary;

	public List<Directory> getDirectories() {
		return directories;
	}

	public void setDirectories(List<Directory> directory) {
		this.directories = directory;
	}

	public String getServerName() { return friendlyName; }

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public int getAllowSync() {
		return allowSync;
	}

	public void setAllowSync(int allowSync) {
		this.allowSync = allowSync;
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public String getMediaTagPrefix() {
		return mediaTagPrefix;
	}

	public void setMediaTagPrefix(String mediaTagPrefix) {
		this.mediaTagPrefix = mediaTagPrefix;
	}

	public long getMediaTagVersion() {
		return mediaTagVersion;
	}

	public String getArt() {
		return art;
	}

	public void setArt(String art) {
		this.art = art;
	}

	public int getSortAsc() {
		return sortAsc;
	}

	public void setSortAsc(int sortAsc) {
		this.sortAsc = sortAsc;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getViewGroup() {
		return viewGroup;
	}

	public void setViewGroup(String viewGroup) {
		this.viewGroup = viewGroup;
	}

	public int getViewMode() {
		return viewMode;
	}

	public void setViewMode(int viewMode) {
		this.viewMode = viewMode;
	}

	public void setMediaTagVersion(long mediaTagVersion) {
		this.mediaTagVersion = mediaTagVersion;
	}

	public void setMediaTagVersion(int mediaTagVersion) {
		this.mediaTagVersion = mediaTagVersion;
	}

	public String getTitle1() {
		return title1;
	}

	public void setTitle1(String title1) {
		this.title1 = title1;
	}

	/**
	 * @return the videos
	 */
	public List<Video> getVideos() {
		return videos;
	}

	/**
	 * @param videos the videos to set
	 */
	public void setVideos(List<Video> videos) {
		this.videos = videos;
	}

	public List<Hub> getHubs() { return hubs;}

	public void setHubs(List<Hub> hubs) { this.hubs = hubs; }

	public List<Photo> getPhotos() { return photos;}

	public void setPhotos(List<Photo> photos) { this.photos = photos; }

	public String getParentPosterURL() {
		return parentPosterURL;
	}

	public void setParentPosterURL(String parentPosterURL) {
		this.parentPosterURL = parentPosterURL;
	}

	public List<Track> getTracks() {
		return tracks;
	}

	public void setTracks(List<Track> tracks) {
		this.tracks = tracks;
	}

	/**
	 * Contains information like Season information for episodes
	 * This needs to be checked if the video elements parents
	 * doesn't exist.
	 * 
	 * @return
	 */
	public long getParentIndex() {
		return parentIndex;
	}

	/**
	 * Set the parentIndex (i.e. Season number).
	 * @param parentIndex
	 */
	public void setParentIndex(long parentIndex) {
		this.parentIndex = parentIndex;
	}

	public void setGrandparentKey(String key) { grandparentKey = key; }


	public long getKey() { return key; }
	public String getLibrarySectionID() { return librarySectionID; }
	public String getLibrarySectionTitle() { return librarySectionTitle; }
	public String getGrandparentContentRating() { return grandparentContentRating; }
	public long getGrandparentRatingKey() { return grandparentRatingKey; }
	public String getGrandparentKey() { return grandparentKey; }
	public String getGrandparentStudio() { return grandparentStudio; }
	public String getGrandparentTheme() { return grandparentTheme; }
	public String getGrandparentThumb() { return grandparentThumb; }
	public String getGrandparentTitle() { return grandparentTitle; }
	public String getParentYear() { return parentYear; }

	public String getThemeKey() { return theme; }
	public String getBannerKey() { return banner; }

	public String getSummary() { return summary; }
}
