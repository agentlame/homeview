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

@Root(name="Directory")
public class Directory extends AbstractPlexObject implements Parcelable {

	public Directory() { super(); }

	@Attribute(required=true)
	private String title;
	
	@Attribute(required=false)
	private String art;
	
	@Attribute(required=false)
	private String banner;
	
	@Attribute(required=false)
	private String thumb;


	protected Directory(Parcel in) {
		super(in);
		title = in.readString();
		art = in.readString();
		banner = in.readString();
		thumb = in.readString();
		refreshing = in.readInt();
		type = in.readString();
		agent = in.readString();
		scanner = in.readString();
		language = in.readString();
		uuid = in.readString();
		updatedAt = in.readLong();
		createdAt = in.readLong();
		prompt = in.readString();
		search = in.readString();
		secondary = in.readInt();
		genres = in.createTypedArrayList(Genre.CREATOR);
		locations = in.createTypedArrayList(Location.CREATOR);
		ratingKey = in.readLong();
		parentRatingKey = in.readLong();
		parentYear = in.readString();
		studio = in.readString();
		rating = in.readString();
		year = in.readString();
		contentRating = in.readString();
		summary = in.readString();
		leafCount = in.readString();
		viewedLeafCount = in.readString();
		librarySectionID = in.readString();
		librarySectionTitle = in.readString();
		parentKey = in.readString();
		parentTitle = in.readString();
		parentSummary = in.readString();
		parentThumb = in.readString();
		parentTheme = in.readString();
		theme = in.readString();
		index = in.readLong();
		parentIndex = in.readLong();
		lastViewedAt = in.readLong();
		viewGroup = in.readString();
		viewMode = in.readInt();
		childCount = in.readLong();
		addedAt = in.readLong();
		composite = in.readString();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		super.writeToParcel(dest, flags);
		dest.writeString(title);
		dest.writeString(art);
		dest.writeString(banner);
		dest.writeString(thumb);
		dest.writeInt(refreshing);
		dest.writeString(type);
		dest.writeString(agent);
		dest.writeString(scanner);
		dest.writeString(language);
		dest.writeString(uuid);
		dest.writeLong(updatedAt);
		dest.writeLong(createdAt);
		dest.writeString(prompt);
		dest.writeString(search);
		dest.writeInt(secondary);
		dest.writeTypedList(genres);
		dest.writeTypedList(locations);
		dest.writeLong(ratingKey);
		dest.writeLong(parentRatingKey);
		dest.writeString(parentYear);
		dest.writeString(studio);
		dest.writeString(rating);
		dest.writeString(year);
		dest.writeString(contentRating);
		dest.writeString(summary);
		dest.writeString(leafCount);
		dest.writeString(viewedLeafCount);
		dest.writeString(librarySectionID);
		dest.writeString(librarySectionTitle);
		dest.writeString(parentKey);
		dest.writeString(parentTitle);
		dest.writeString(parentSummary);
		dest.writeString(parentThumb);
		dest.writeString(parentTheme);
		dest.writeString(theme);
		dest.writeLong(index);
		dest.writeLong(parentIndex);
		dest.writeLong(lastViewedAt);
		dest.writeString(viewGroup);
		dest.writeInt(viewMode);
		dest.writeLong(childCount);
		dest.writeLong(addedAt);
		dest.writeString(composite);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public static final Creator<Directory> CREATOR = new Creator<Directory>() {
		@Override
		public Directory createFromParcel(Parcel in) {
			return new Directory(in);
		}

		@Override
		public Directory[] newArray(int size) {
			return new Directory[size];
		}
	};

	public String getThumb() {
		return thumb;
	}

	public void setThumb(String thumb) {
		this.thumb = thumb;
	}

	public String getBanner() {
		return banner;
	}

	public void setBanner(String banner) {
		this.banner = banner;
	}

	@Attribute(required=false)
	private int refreshing;
		
	@Attribute(required=false)
	private String type;
	
	@Attribute(required=false)
	private String agent;
	
	@Attribute(required=false)
	private String scanner;
	
	@Attribute(required=false)
	private String language;
	
	@Attribute(required=false)
	private String uuid;
	
	@Attribute(required=false)
	private long updatedAt;
	
	@Attribute(required=false)
	private long createdAt;
	
	@Attribute(required=false)
	/**
	 * Used for searches.
	 */
	private String prompt;
	
	@Attribute(required=false)
	/**
	 * Only appears with prompt.
	 */
	private String search;
	
	@Attribute(required=false)
	private int secondary;
		
	public int getSecondary() {
		return secondary;
	}

	public void setSecondary(int secondary) {
		this.secondary = secondary;
	}

	public List<Genre> getGenres() {
		return genres;
	}

	public void setGenres(List<Genre> genres) {
		this.genres = genres;
	}

	public long getRatingKey() { return ratingKey; }

	public void setRatingKey(long ratingKey) {
		this.ratingKey = ratingKey;
	}

	public String getStudio() {
		return studio;
	}

	public void setStudio(String studio) {
		this.studio = studio;
	}

	public String getRating() {
		return rating;
	}

	public void setRating(String rating) {
		this.rating = rating;
	}

	public String getYear() {
		return year;
	}

	public void setYear(String year) {
		this.year = year;
	}

	public String getContentRating() {
		return contentRating;
	}

	public void setContentRating(String contentRating) {
		this.contentRating = contentRating;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getLeafCount() {
		return leafCount;
	}

	public void setLeafCount(String leafCount) {
		this.leafCount = leafCount;
	}

	public String getViewedLeafCount() {
		return viewedLeafCount;
	}

	public void setViewedLeafCount(String viewedLeafCount) {
		this.viewedLeafCount = viewedLeafCount;
	}

	@ElementList(inline=true,required=false)
	private List<Genre> genres;

	@ElementList(inline=true,name="Location",required=false)
	private List<Location> locations;
	
	@Attribute(required=false)
	private long ratingKey;

	@Attribute(required=false)
	private long parentRatingKey;

	@Attribute(required=false)
	private String parentYear;
	
	@Attribute(required=false)
	private String studio;
	
	@Attribute(required=false)
	private String rating;
	
	@Attribute(required=false)
	private String year;
	
	@Attribute(required=false)
	private String contentRating;
	
	@Attribute(required=false)
	private String summary;
	
	@Attribute(required=false)
	private String leafCount;
	
	@Attribute(required=false)
	private String viewedLeafCount;

	@Attribute(required=false)
	private String librarySectionID;

	@Attribute(required=false)
	private String librarySectionTitle;

	@Attribute(required=false)
	private String parentKey;

	@Attribute(required=false)
	private String parentTitle;

	@Attribute(required=false)
	private String parentSummary;

	@Attribute(required=false)
	private String parentThumb;

	@Attribute(required=false)
	private String parentTheme;

	@Attribute(required=false)
	private String theme;

	@Attribute(required=false)
	private long index;

	@Attribute(required=false)
	private long parentIndex;

	@Attribute(required=false)
	private long lastViewedAt;

	@Attribute(required=false)
	private String viewGroup;

	@Attribute(required=false)
	private int viewMode;

	@Attribute(required=false)
	private long childCount;

	@Attribute(required=false)
	private long addedAt;

	@Attribute(required=false)
	private String composite;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getArt() {
		return art;
	}

	public void setArt(String art) {
		this.art = art;
	}

	public int getRefreshing() {
		return refreshing;
	}

	public void setRefreshing(int refreshing) {
		this.refreshing = refreshing;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getAgent() {
		return agent;
	}

	public void setAgent(String agent) {
		this.agent = agent;
	}

	public String getScanner() {
		return scanner;
	}

	public void setScanner(String scanner) {
		this.scanner = scanner;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public long getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(long updatedAt) {
		this.updatedAt = updatedAt;
	}

	public long getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(long createdAt) {
		this.createdAt = createdAt;
	}

	public List<Location> getLocations() {
		return locations;
	}

	public void setLocation(List<Location> location) {
		this.locations = location;
	}
	
	public String getPrompt() {
		return prompt;
	}

	public void setPrompt(String prompt) {
		this.prompt = prompt;
	}

	public String getSearch() {
		return search;
	}

	public void setSearch(String search) {
		this.search = search;
	}
	
	public void setLocations(List<Location> locations) {
		this.locations = locations;
	}

	public String getLibrarySectionID() { return librarySectionID; }
	public String getLibrarySectionTitle() { return librarySectionTitle; }
	public void setLibrarySectionID(String id) {  librarySectionID = id; }
	public void setLibrarySectionTitle(String title) { librarySectionTitle = title; }

	public String getParentKey() { return parentKey; }
	public void setParentKey(String key) { parentKey = key; }

	public long getParentRatingKey() { return parentRatingKey; }
	public void setParentRatingKey(long key) { parentRatingKey = key; }

	public String getParentTitle() { return parentTitle; }
	public void setParentTitle(String title) { parentTitle = title; }

	public String getParentSummary() { return parentSummary; }
	public void setParentSummary(String summary) { parentSummary = summary; }

	public String getParentThumbKey() { return parentThumb; }
	public void setParentThumbKey(String key) { parentThumb = key; }

	public String getParentThemeKey() { return parentTheme; }
	public void setParentThemeKey(String key) { parentTheme = key; }

	public String getThemeKey() { return theme; }
	public void setThemeKey(String key) { theme = key; }

	public long getIndex() { return index; }
	public void setIndex(long idx) { index = idx; }

	public long getParentIndex() { return parentIndex; }
	public void setParentIndex(long idx) { parentIndex = idx; }

	public long getLastViewedAt() { return lastViewedAt; }
	public void setLastViewedAt(long time) { lastViewedAt = time; }

	public String getViewGroup() { return viewGroup; }
	public void setViewGroup(String group) { viewGroup = group; }

	public int getViewMode() { return viewMode; }
	public void setViewMode(int mode) { viewMode = mode; }

	public long getChildCount() { return childCount; }

	public long getAddedAt() { return addedAt; }

	public String getParentYear() { return parentYear; }
	public void setParentYear(String parentYear) { this.parentYear = parentYear; }

	public String getComposite(int cols, int rows, int height, int width) {

		if (composite == null || composite.isEmpty())
			return "";
		return String.format("%s?cols=%d&rows=%d&height=%d&width=%d", composite, cols, rows, height, width);
	 }

	public boolean shouldSkipProcessing() {

		if (secondary > 0 || getKey().equals("folder") || getKey().startsWith("search"))
			return true;
		return false;
	}
}
