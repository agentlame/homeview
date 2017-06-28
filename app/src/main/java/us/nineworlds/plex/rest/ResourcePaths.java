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

package us.nineworlds.plex.rest;

import android.net.Uri;

import java.net.URLEncoder;
import java.util.StringTokenizer;

import us.nineworlds.plex.rest.config.IConfiguration;


/**
 * @author dcarver
 *
 */
public class ResourcePaths {

	public static final String SECTIONS = "sections/";
	public static final String LIBRARY_PATH = "/library/";
	public static final String SECTIONS_PATH = LIBRARY_PATH + SECTIONS;
	public static final String ROOT_PATH = "/";
	public static final String HUBS_PATH = "/hubs/";
	
	private IConfiguration config;
	
	public ResourcePaths(IConfiguration configuration) {
		config = configuration;
	}
	
	public String getRoot() {
		return getHostPort() + ROOT_PATH;
	}
	
	public String getLibraryURL() {
		return getHostPort() + LIBRARY_PATH + "?" + getToken();
	}

	public String getLibraryURL(String key) {
		return getHostPort() + LIBRARY_PATH + key + "/"  + "?" + getToken();
	}
	
	public String getSectionsURL() {
		return getHostPort() + SECTIONS_PATH + "?" + getToken();
	}
	
	public String getSectionsURL(String key) {
		return getHostPort() + SECTIONS_PATH + key + "?" + getToken();
	}
	
	public String getSectionsURL(String key, String category) {
		return getHostPort() + SECTIONS_PATH + key + "/" + category + "?" + getToken();
	}
	
	public String getSectionsURL(String key, String category, String secondaryCategory) {
		return getHostPort() + SECTIONS_PATH + key + "/" + category + "?" + getToken();
	}

	public String getHubsURL(String sectionId) {
		return getHostPort() + HUBS_PATH + SECTIONS + sectionId  + "?" + getToken();
	}

	public String getHubsURL() {
		return getHostPort() + HUBS_PATH + "?" + getToken() + "&excludeMusic=1&excludePhotos=1&excludePlaylists=1";
	}
	
	protected String getHostPort() {
		return "http://" + config.getHost() + ":" + config.getPort();
	}
	
	public String getSeasonsURL(String key) {

		String mark = "?";
		if (key.contains("?"))
			mark = "&";
		else
			mark += getToken() + "&";
		return getHostPort() + key + mark + "includeChapters=1";
	}
	
	public String getEpisodesURL(String key) {
		return getHostPort() + key;
	}
	
	public String getMovieMetaDataURL(String key, boolean getExtra) {

		if (!getExtra) {
			String ret = getHostPort() + key;
			if (ret.contains("?"))
				ret += "&";
			else
				ret += "?";
			ret += getToken();
			return ret;
		}
		String mark = "?";
		if (key.contains("?"))
			mark = "&";
		else
			mark +=  getToken() + "&";

		String ret = getHostPort() + key + mark + "includeChapters=1&checkFiles=1&includeExtras=1&includeRelated=1&includeRelatedCount=0";
		if (key.endsWith("all") && key.startsWith("/library/sections/"))
			ret = ret.replace("&checkFiles=1", "");
		return ret;
	}

	public String getToken() {

		return config.getServerToken() != null || !config.getServerToken().isEmpty() ?
		"X-Plex-Token=" + config.getServerToken() : "";
	}
	
	public String getWatchedUrl(String key, String ratingKey) {
		return getRoot() + ":/scrobble?" + getToken() + "&identifier=com.plexapp.plugins.library&key=" + ratingKey + "&ratingKey=" + ratingKey;
	}
	
	public String getUnwatchedUrl(String key, String ratingKey) {
		return getRoot() + ":/unscrobble?" + getToken() + "&identifier=com.plexapp.plugins.library&key=" + ratingKey + "&ratingKey=" + ratingKey;
	}
	
	public String getProgressUrl(String key, String ratingKey, String offset) {
		String offseturl = getRoot() + ":/progress?" + getToken() + "&time=" + offset + "&key=" + ratingKey + "&ratingKey=" + ratingKey + "&identifier=com.plexapp.plugins.library";
		//String offseturl = getRoot() + ":/timeline?" + getToken() + "&time=" + offset + "&key=" + ratingKey + "&ratingKey=" + ratingKey + "&identifier=com.plexapp.plugins.library";

		return offseturl;
	}

	private String encodeSearchQuery(String key, String type, String query) {

		String section = getSectionsURL(key);
		StringTokenizer tokens = new StringTokenizer(section, "?");
		section = tokens.nextToken();
		String token = tokens.hasMoreTokens() ? tokens.nextToken() + "&" : "";
		return String.format("%s/search?%stype=%s&query=%s", section, token, type, Uri.encode(query));
	}
	
	public String getMovieSearchURL(String key, String query) {
		String searchURL = encodeSearchQuery(key, "1", query);
		return searchURL;
	}
	
	public String getTVShowSearchURL(String key, String query) {
		String searchURL = encodeSearchQuery(key, "2", query);
		return searchURL;
	}
	
	public String getEpisodeSearchURL(String key, String query) {
		String searchURL = encodeSearchQuery(key, "4", query);
		return searchURL;
	}
	
	public String getMediaTagURL(String resourceType, String resourceName) {
		String encodedResourceName = resourceName;
		try {
			encodedResourceName = URLEncoder.encode(resourceName, "UTF-8");
		} catch (Exception ex) {
			
		}
		String mediaTagURL = getHostPort() + "/system/bundle/media/flags/" + resourceType + "/" + encodedResourceName + "?" + getToken();//+ "?"t=" + identifier;
		return mediaTagURL;
	}

    public String getImageURL(String url, int width, int height) {
    	String u = url;
    	String host = config.getHost();
    	if (u.contains(config.getHost())) {
    		u = u.replaceFirst(host, "127.0.0.1");
    	}
    	String encodedUrl = u;
    	try {
    		encodedUrl = URLEncoder.encode(u, "UTF-8");
    	} catch (Exception ex) {
    		// If there is an exception encoding the url just return the original url
    		return url;
    	}
        return getHostPort() + "/photo/:/transcode?" + getToken() + "&url=" + encodedUrl + "&width=" + width + "&height=" + height;
    }
	
}
