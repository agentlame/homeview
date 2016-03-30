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

/**
 * @author dcarver
 *
 */
@Root(name="Media")
public class Media implements Parcelable {

	public Media() {}

	@Attribute(required=false)
	private String videoFrameRate;

	@Attribute(name="aspectRatio",required=false)
	private String aspectRatio;
	
	@Attribute(name="audioCodec", required=false)
	private String audioCodec;
	
	@Attribute(name="videoCodec", required=false)
	private String videoCodec;
	
	@Attribute(name="videoResolution", required=false)
	private String videoResolution;
	
	@Attribute(name="container", required=false)
	private String container;

	@Attribute(required=false)
	private String width;

	@Attribute(required=false)
	private String height;

	protected Media(Parcel in) {
		videoFrameRate = in.readString();
		aspectRatio = in.readString();
		audioCodec = in.readString();
		videoCodec = in.readString();
		videoResolution = in.readString();
		container = in.readString();
		width = in.readString();
		height = in.readString();
		audioChannels = in.readString();
		videoParts = in.createTypedArrayList(Part.CREATOR);
	}

	public static final Creator<Media> CREATOR = new Creator<Media>() {
		@Override
		public Media createFromParcel(Parcel in) {
			return new Media(in);
		}

		@Override
		public Media[] newArray(int size) {
			return new Media[size];
		}
	};

	public String getContainer() {
		return container;
	}

	public void setContainer(String container) {
		this.container = container;
	}

	@Attribute(name="audioChannels", required=false)
	private String audioChannels;
	
	public String getAudioChannels() {
		return audioChannels;
	}

	public void setAudioChannels(String audioChannels) {
		this.audioChannels = audioChannels;
	}

	@ElementList(inline=true,name="Part", required=false)
	private List<Part> videoParts;

	public List<Part> getVideoPart() {
		return videoParts;
	}

	public void setVideoPart(List<Part> videoParts) {
		this.videoParts = videoParts;
	}

	public String getAspectRatio() {
		return aspectRatio;
	}

	public void setAspectRatio(String aspectRatio) {
		this.aspectRatio = aspectRatio;
	}

	public String getAudioCodec() {
		return audioCodec;
	}

	public void setAudioCodec(String audioCodec) {
		this.audioCodec = audioCodec;
	}

	public String getVideoCodec() {
		return videoCodec;
	}

	public void setVideoCodec(String videoCodec) {
		this.videoCodec = videoCodec;
	}

	public String getVideoResolution() {
		return videoResolution;
	}

	public void setVideoResolution(String videoResolution) {
		this.videoResolution = videoResolution;
	}

	public String getVideoFrameRate() { return videoFrameRate; }

	public String getWidth() { return width; }
	public String getHeight() { return height; }

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(videoFrameRate);
		dest.writeString(aspectRatio);
		dest.writeString(audioCodec);
		dest.writeString(videoCodec);
		dest.writeString(videoResolution);
		dest.writeString(container);
		dest.writeString(width);
		dest.writeString(height);
		dest.writeString(audioChannels);
		dest.writeTypedList(videoParts);
	}
}
