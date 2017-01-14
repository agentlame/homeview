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

import java.util.ArrayList;
import java.util.List;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

/**
 * Represents the video container
 * 
 * @author dcarver
 *
 */
@Root(name="Video")
public class Video extends AbstractPlexObject implements Parcelable {

	public Video() {}

	@Attribute(required=false)
	private String type;

	@Attribute(required=false)
	private String librarySectionID;

	@Attribute(required=false)
	private String librarySectionTitle;

	@Attribute(required=false)
	private String studio;
	
	@Attribute(required=false)
	private String summary;
	
	@Attribute(required=false)
	private String titleSort;
	
	@Attribute(required=false)
	private String title;
	
	@Attribute(required=false)
	private int viewCount;
	
	@Attribute(name="tagline", required=false)
	private String tagLine;
	
	@Attribute(required=false)
	/**
	 * Point where viewing can be resumed.
	 */
	private long viewOffset;

	@Attribute(required=false)
	private long lastViewedAt;
	
	@Attribute(name="thumb",required=false)
	/**
	 * REST path for obtaining thumbnail image
	 */
	private String thumbNailImageKey;
	
	@Attribute(name="art",required=false)
	private String backgroundImageKey;
	
	@Attribute(name="parentThumb", required=false)
	private String parentThumbNailImageKey;
	
	@Attribute(name="grandparentThumb", required=false)
	private String grandParentThumbNailImageKey;

	@Attribute(name="grandparentTitle", required=false)
	private String grandParentTitle;

	@Attribute(required=false)
	private long grandparentRatingKey;

	@Attribute(required=false)
	private String grandparentKey;

	@Attribute(required=false)
	private String grandparentArt;

	@Attribute(required=false)
	private String grandparentTheme;


	public Video(Parcel in) {
		super(in);
		type = in.readString();
		librarySectionID = in.readString();
		librarySectionTitle = in.readString();
		studio = in.readString();
		summary = in.readString();
		titleSort = in.readString();
		title = in.readString();
		viewCount = in.readInt();
		tagLine = in.readString();
		viewOffset = in.readLong();
		thumbNailImageKey = in.readString();
		backgroundImageKey = in.readString();
		parentThumbNailImageKey = in.readString();
		grandParentThumbNailImageKey = in.readString();
		grandParentTitle = in.readString();
		grandparentRatingKey = in.readLong();
		grandparentKey = in.readString();
		grandparentArt = in.readString();
		grandparentTheme = in.readString();
		duration = in.readLong();
		timeAdded = in.readLong();
		timeUpdated = in.readLong();
		originallyAvailableDate = in.readString();
		contentRating = in.readString();
		year = in.readString();
		ratingKey = in.readLong();
		parentKey = in.readString();
		parentRatingKey = in.readLong();
		episode = in.readString();
		season = in.readString();
		rating = in.readDouble();
		lastViewedAt = in.readLong();
		countries = in.createTypedArrayList(Country.CREATOR);
		directors = in.createTypedArrayList(Director.CREATOR);
		actors = in.createTypedArrayList(Role.CREATOR);
		writers = in.createTypedArrayList(Writer.CREATOR);
		genres = in.createTypedArrayList(Genre.CREATOR);
		medias = in.createTypedArrayList(Media.CREATOR);
		collections = in.createTypedArrayList(Collection.CREATOR);
		chapters = in.createTypedArrayList(Chapter.CREATOR);
		extras = in.createTypedArrayList(Extras.CREATOR);
		related = in.createTypedArrayList(Related.CREATOR);
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		super.writeToParcel(dest, flags);
		dest.writeString(type);
		dest.writeString(librarySectionID);
		dest.writeString(librarySectionTitle);
		dest.writeString(studio);
		dest.writeString(summary);
		dest.writeString(titleSort);
		dest.writeString(title);
		dest.writeInt(viewCount);
		dest.writeString(tagLine);
		dest.writeLong(viewOffset);
		dest.writeString(thumbNailImageKey);
		dest.writeString(backgroundImageKey);
		dest.writeString(parentThumbNailImageKey);
		dest.writeString(grandParentThumbNailImageKey);
		dest.writeString(grandParentTitle);
		dest.writeLong(grandparentRatingKey);
		dest.writeString(grandparentKey);
		dest.writeString(grandparentArt);
		dest.writeString(grandparentTheme);
		dest.writeLong(duration);
		dest.writeLong(timeAdded);
		dest.writeLong(timeUpdated);
		dest.writeString(originallyAvailableDate);
		dest.writeString(contentRating);
		dest.writeString(year);
		dest.writeLong(ratingKey);
		dest.writeString(parentKey);
		dest.writeLong(parentRatingKey);
		dest.writeString(episode);
		dest.writeString(season);
		dest.writeDouble(rating);
		dest.writeLong(lastViewedAt);
		dest.writeTypedList(countries);
		dest.writeTypedList(directors);
		dest.writeTypedList(actors);
		dest.writeTypedList(writers);
		dest.writeTypedList(genres);
		dest.writeTypedList(medias);
		dest.writeTypedList(collections);
		dest.writeTypedList(chapters);
		dest.writeTypedList(extras);
		dest.writeTypedList(related);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public static final Creator<Video> CREATOR = new Creator<Video>() {
		@Override
		public Video createFromParcel(Parcel in) {
			return new Video(in);
		}

		@Override
		public Video[] newArray(int size) {
			return new Video[size];
		}
	};

	public long getGrandparentRatingKey() { return grandparentRatingKey; }
	public String getGrandparentKey() { return grandparentKey; }
	public String getGrandparentArtKey() { return grandparentArt; }
	public String getGrandparentThemeKey() { return grandparentTheme; }
	public String getGrandParentTitle() {
		return grandParentTitle;
	}

	public void setGrandParentTitle(String grandParentTitle) {
		this.grandParentTitle = grandParentTitle;
	}

	public String getGrandParentThumbNailImageKey() {
		return grandParentThumbNailImageKey;
	}

	public void setGrandParentThumbNailImageKey(String grandParentThumbNailImageKey) {
		this.grandParentThumbNailImageKey = grandParentThumbNailImageKey;
	}

	@Attribute(name="duration",required=false)
	private long duration;
	
	@Attribute(name="addedAt",required=false)
	private long timeAdded;
	
	@Attribute(name="updatedAt",required=false)
	private long timeUpdated;
	
	@Attribute(name="originallyAvailableAt",required=false)
	/**
	 * Formatted date item was originally available in YYYY-MM-DD format.
	 */
	private String originallyAvailableDate;
	
	@Attribute(name="contentRating",required=false)
	private String contentRating;
	
	@Attribute(name="year", required=false)
	private String year;
	
	@Attribute(name="ratingKey", required=false)
	private long ratingKey;
	
	@Attribute(name="parentKey", required=false)
	private String parentKey;

	@Attribute(required=false)
	private long parentRatingKey;
	
	@Attribute(name="index", required=false)
	private String episode;
	
	@Attribute(name="parentIndex", required=false)
	private String season;
	
	/**
	 * 
	 */
	@Attribute(name="rating", required=false)
	private double rating;

	@ElementList(inline=true,name="Extras",required=false)
	private List<Extras> extras;

	@ElementList(inline=true,name="Related",required=false)
	private List<Related> related;
		
	@ElementList(inline=true,required=false)
	private List<Country> countries;

	@ElementList(inline=true,required=false)
	private List<Director> directors;

	@ElementList(inline=true,required=false)
	private List<Role> actors;

	@ElementList(inline=true,required=false)
	private List<Chapter>chapters;

	@ElementList(inline=true,required=false)
	private List<Writer> writers;

	@ElementList(inline=true,required=false)
	private List<Genre> genres;

	@ElementList(inline=true,name="Media",required=false)
	private List<Media> medias;

	@ElementList(inline=true,required=false)
	private List<Collection> collections;

	public List<Collection> getCollections() {
		return collections;
	}

	public List<Role> getActors() {
		return actors;
	}

	/**
	 * @return the backgroundImageKey
	 */
	public String getBackgroundImageKey() {
		return backgroundImageKey;
	}

	public String getContentRating() {
		return contentRating;
	}

	public List<Country> getCountries() {
		return countries;
	}

	public List<Director> getDirectors() {
		return directors;
	}

	/**
	 * @return the duration
	 */
	public long getDuration() {
		return duration;
	}

	public List<Genre> getGenres() {
		return genres;
	}

	public List<Media> getMedias() {
		return medias;
	}
	
	/**
	 * @return the originallyAvailableDate in YYYY-MM-DD format.
	 */
	public String getOriginallyAvailableDate() {
		return originallyAvailableDate;
	}
	
	/**
	 * @return the summary
	 */
	public String getSummary() {
		return summary;
	}
	
	/**
	 * @return the tagLine
	 */
	public String getTagLine() {
		return tagLine;
	}
	
	
	/**
	 * @return the thumbNailImageKey
	 */
	public String getThumbNailImageKey() {
		return thumbNailImageKey;
	}

	/**
	 * @return the timeAdded
	 */
	public long getTimeAdded() {
		return timeAdded;
	}

	/**
	 * @return the timeUpdated
	 */
	public long getTimeUpdated() {
		return timeUpdated;
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @return the titleSort
	 */
	public String getTitleSort() {
		return titleSort;
	}
	

	/**
	 * @return the viewCount
	 */
	public int getViewCount() {
		return viewCount;
	}

	/**
	 * @return the viewOffset
	 */
	public long getViewOffset() {
		return viewOffset;
	}

	public List<Writer> getWriters() {
		return writers;
	}

	public String getYear() {
		return year;
	}

	public void setActors(List<Role> actors) {
		this.actors = actors;
	}

	/**
	 * @param backgroundImageKey the backgroundImageKey to set
	 */
	public void setBackgroundImageKey(String backgroundImageKey) {
		this.backgroundImageKey = backgroundImageKey;
	}

	public void setContentRating(String contentRating) {
		this.contentRating = contentRating;
	}

	public void setCountries(List<Country> countries) {
		this.countries = countries;
	}

	public void setDirectors(List<Director> directors) {
		this.directors = directors;
	}

	/**
	 * @param duration the duration to set
	 */
	public void setDuration(long duration) {
		this.duration = duration;
	}

	public void setGenres(List<Genre> genres) {
		this.genres = genres;
	}

	public void setMedias(List<Media> medias) {
		this.medias = medias;
	}

	/**
	 * This needs to be formatted in YYYY-MM-DD format.
	 * @param originallyAvailableDate the originallyAvailableDate to set
	 */
	public void setOriginallyAvailableDate(String originallyAvailableDate) {
		this.originallyAvailableDate = originallyAvailableDate;
	}

	/**
	 * @param summary the summary to set
	 */
	public void setSummary(String summary) {
		this.summary = summary;
	}

	/**
	 * @param tagLine the tagLine to set
	 */
	public void setTagLine(String tagLine) {
		this.tagLine = tagLine;
	}

	/**
	 * @param thumbNailImageKey the thumbNailImageKey to set
	 */
	public void setThumbNailImageKey(String thumbNailImageKey) {
		this.thumbNailImageKey = thumbNailImageKey;
	}

	/**
	 * @param timeAdded the timeAdded to set
	 */
	public void setTimeAdded(long timeAdded) {
		this.timeAdded = timeAdded;
	}

	/**
	 * @param timeUpdated the timeUpdated to set
	 */
	public void setTimeUpdated(long timeUpdated) {
		this.timeUpdated = timeUpdated;
	}

	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @param titleSort the titleSort to set
	 */
	public void setTitleSort(String titleSort) {
		this.titleSort = titleSort;
	}

	/**
	 * @param viewCount the viewCount to set
	 */
	public void setViewCount(int viewCount) {
		this.viewCount = viewCount;
	}

	/**
	 * @param viewOffset the viewOffset to set
	 */
	public void setViewOffset(long viewOffset) {
		this.viewOffset = viewOffset;
	}

	public void setWriters(List<Writer> writers) {
		this.writers = writers;
	}

	public void setYear(String year) {
		this.year = year;
	}

	public long getRatingKey() {
		return ratingKey;
	}

	public void setRatingKey(long ratingKey) {
		this.ratingKey = ratingKey;
	}
	
	public String getParentThumbNailImageKey() {
		return parentThumbNailImageKey;
	}

	public void setParentThumbNailImageKey(String parentThumbNailImageKey) {
		this.parentThumbNailImageKey = parentThumbNailImageKey;
	}

	public String getStudio() {
		return studio;
	}

	public void setStudio(String studio) {
		this.studio = studio;
	}

	public double getRating() {
		return rating;
	}

	public void setRating(double rating) {
		this.rating = rating;
	}
	
	public void setParentKey(String parentKey) {
		this.parentKey = parentKey;
	}
	
	public String getParentKey() {
		return parentKey;
	}
	public long getParentRatingKey() { return parentRatingKey; }

	public String getEpisode() {
		return episode;
	}

	public void setEpisode(String episode) {
		this.episode = episode;
	}

	public String getSeason() {
		return season;
	}

	public void setSeason(String season) {
		this.season = season;
	}

	public String getLibrarySectionID() { return librarySectionID; }
	public String getLibrarySectionTitle() { return librarySectionTitle; }
	public String getType() { return type; }

	public List<Extras> getExtras() { return extras; }
	public List<Related> getRelated() { return related; }

	public List<Chapter> getChapters() { return chapters; }

	public long getLastViewedAt() { return lastViewedAt; }
}
