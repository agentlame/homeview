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

import android.os.Build;
import android.text.TextUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import us.nineworlds.plex.rest.config.IConfiguration;
import us.nineworlds.plex.rest.model.impl.Hub;
import us.nineworlds.plex.rest.model.impl.MediaContainer;


/**
 * This class acts as a factory for retrieving items from Plex.
 * 
 * This is a singleton so only one of these will ever exist currently.
 * 
 * @author dcarver
 * 
 */
public class PlexappFactory {

	//private static PlexappFactory instance = null;

	private IConfiguration configuration;
	private ResourcePaths resourcePath = null;
	private Serializer serializer = null;

	public PlexappFactory(IConfiguration config) {
		resourcePath = new ResourcePaths(config);
		configuration = config;
		serializer = new Persister();
	}

	/*public static PlexappFactory getInstance(IConfiguration config) {
		if (instance == null) {
			instance = new PlexappFactory(config);
		}
		return instance;
	}*/

	public String getToken() { return resourcePath.getToken(); }
	/**
	 * Retrieve the root metadata from the Plex Media Server.
	 * 
	 * @return 
	 * @throws Exception
	 */
	public MediaContainer retrieveRootData() throws Exception {
		String rootURL = resourcePath.getRoot();
		if(!resourcePath.getToken().isEmpty())
			rootURL += "?" + resourcePath.getToken();
		MediaContainer mediaContainer = serializeResource(rootURL);

		return mediaContainer;
	}

	/**
	 * This retrieves the available libraries.  This can include such
	 * things as Movies, and TV shows.
	 * 
	 * @return MediaContainer the media container for the library
	 * @throws Exception
	 */
	public MediaContainer retrieveLibrary() throws Exception {
		String libraryURL = resourcePath.getLibraryURL();
		MediaContainer mediaContainer = serializeResource(libraryURL);

		return mediaContainer;
	}

	/**
	 * This retrieves the available libraries.  This can include such
	 * things as Movies, and TV shows.
	 *
	 * @return MediaContainer the media container for the library
	 * @throws Exception
	 */
	public MediaContainer retrieveLibrary(String key) throws Exception {
		String libraryURL = resourcePath.getLibraryURL(key);
		MediaContainer mediaContainer = serializeResource(libraryURL);

		return mediaContainer;
	}

	public MediaContainer retrieveHubs() throws Exception {
		String hubsURL = resourcePath.getHubsURL();
		MediaContainer mediaContainer = serializeResource(hubsURL);

		if (mediaContainer != null && mediaContainer.getHubs() != null) {

			List<Hub> toRemove = new ArrayList<>();
			for (Hub hub : mediaContainer.getHubs()) {
				if (hub.getSize() == 0)
					toRemove.add(hub);
			}
			for(Hub hub : toRemove)
				mediaContainer.getHubs().remove(hub);
		}

		return mediaContainer;
	}
	
	/**
	 * This retrieves the available libraries.  This can include such
	 * things as Movies, and TV shows.
	 * 
	 * @return MediaContainer the media container for the library
	 * @throws Exception
	 */
	public MediaContainer retrieveSections() throws Exception {
		String sectionsURL = resourcePath.getSectionsURL();
		MediaContainer mediaContainer = serializeResource(sectionsURL);

		return mediaContainer;
	}
	
	/**
	 * This retrieves the available libraries.  This can include such
	 * things as Movies, and TV shows.
	 * 
	 * @return MediaContainer the media container for the library
	 * @param key the section key
	 * @throws Exception
	 */
	public MediaContainer retrieveSections(String key) throws Exception {
		String sectionsURL = resourcePath.getSectionsURL(key);
		MediaContainer mediaContainer = serializeResource(sectionsURL);

		return mediaContainer;
	}
	
	/**
	 * For Movies this will return a MediaContainer with Videos.  For
	 * TV Shows this will return a MediaContainer with Directories.
	 * 
	 * @param key
	 * @param category
	 * @return MediaContainer
	 * @throws Exception
	 */
	public MediaContainer retrieveSections(String key, String category) throws Exception {
		String moviesURL = resourcePath.getSectionsURL(key, category);
		MediaContainer mediaContainer = serializeResource(moviesURL);
		return mediaContainer;
	}
	
	public MediaContainer retrieveSections(String key, String category, String secondaryCategory) throws Exception {
		String moviesURL = resourcePath.getSectionsURL(key, category, secondaryCategory);
		MediaContainer mediaContainer = serializeResource(moviesURL);
		return mediaContainer;
	}
	
	
	public MediaContainer retrieveSeasons(String key) throws Exception {
		String seasonsURL = resourcePath.getSeasonsURL(key);
		MediaContainer mediaContainer = serializeResource(seasonsURL);
		return mediaContainer;
	}
	
	public MediaContainer retrieveMusicMetaData(String key) throws Exception {
		String seasonsURL = resourcePath.getSeasonsURL(key);
		MediaContainer mediaContainer = serializeResource(seasonsURL);
		return mediaContainer;
	}

	public MediaContainer retrieveHubForSection(String sectionId) throws Exception {
		String hubURL = resourcePath.getHubsURL(sectionId);
		MediaContainer mediaContainer = serializeResource(hubURL);
		return mediaContainer;
	}
	
	public MediaContainer retrieveEpisodes(String key) throws Exception {
		String episodesURL = resourcePath.getEpisodesURL(key);
		MediaContainer mediaContainer = serializeResource(episodesURL);
		return mediaContainer;
	}

	public MediaContainer retrieveVideoMetaData(String key) throws Exception {
		String episodesURL = resourcePath.getMovieMetaDataURL(key, false);
		MediaContainer mediaContainer = serializeResource(episodesURL);
		return mediaContainer;
	}


	public MediaContainer retrieveMovieMetaData(String key) throws Exception {
		String episodesURL = resourcePath.getMovieMetaDataURL(key, true);
		MediaContainer mediaContainer = serializeResource(episodesURL);
		return mediaContainer;
	}
		
	public MediaContainer searchMovies(String key, String query) throws Exception {
		String searchURL = resourcePath.getMovieSearchURL(key, query);
		MediaContainer mediaContainer = serializeResource(searchURL);
		return mediaContainer;
	}

	public MediaContainer searchTVShows(String key, String query) throws Exception {
		String searchURL = resourcePath.getTVShowSearchURL(key, query);
		MediaContainer mediaContainer = serializeResource(searchURL);
		return mediaContainer;
	}

	public MediaContainer searchEpisodes(String key, String query) throws Exception {
		String searchURL = resourcePath.getEpisodeSearchURL(key, query);
		MediaContainer mediaContainer = serializeResource(searchURL);
		return mediaContainer;
	}
	
	public String baseURL() {
		return resourcePath.getRoot();
	}

	/**
	 * Sets a video as watched. viewCount will be 1.
	 * @param key
	 * @return
	 */
	public boolean setWatched(String key, String ratingKey) {
		String resourceURL = resourcePath.getWatchedUrl(key, ratingKey);
		return requestSuccessful(resourceURL);
	}
	
	/**
	 * Sets a vide as unwatched. viewCount will not be present.
	 * 
	 * @param key
	 * @return
	 */
	public boolean setUnWatched(String key, String ratingKey) {
		String resourceURL = resourcePath.getUnwatchedUrl(key, ratingKey);
		return requestSuccessful(resourceURL);
	}
	
	public boolean setProgress(String key, String ratingKey, String offset) {
		String resourceURL = resourcePath.getProgressUrl(key, ratingKey, offset);
		return requestSuccessful(resourceURL);
	}
	

	/**
	 * @param resourceURL
	 * @param
	 * @return
	 */
	protected boolean requestSuccessful(String resourceURL) {
		HttpURLConnection con = null;
		try {
			URL url = new URL(resourceURL);
			con = (HttpURLConnection) url.openConnection();
			con.setDefaultUseCaches(false);
			configuration.fillRequestProperties(con);
			int responseCode = con.getResponseCode();
			if (responseCode == 200) {
				return true;
			}
		} catch (Exception ex) {
			return false;
		} finally {
			if (con != null) {
				con.disconnect();
			}
		}
		return false;
	}
	
	public String getProgressURL(String key, String ratingKey, String offset) {
		return resourcePath.getProgressUrl(key, ratingKey, offset);
	}
	
	public String getMovieSearchURL(String key, String query) {
		return resourcePath.getMovieSearchURL(key, query);
	}
	
	public String getTVShowSearchURL(String key, String query) {
		return resourcePath.getMovieSearchURL(key, query);
	}

	public String getEpisodeSearchURL(String key, String query) {
		return resourcePath.getMovieSearchURL(key, query);
	}
	
	public String getMediaTagURL(String resourceType, String resourceName) {//, String identifier) {
		return resourcePath.getMediaTagURL(resourceType, resourceName);//, identifier);
	}
	
	public String getSectionsURL(String key, String category) {
		return resourcePath.getSectionsURL(key, category);
	}
	
	public String getSectionsURL() {
		return resourcePath.getSectionsURL();
	}
	
	public String getSectionsUrl(String key) {
		return resourcePath.getSectionsURL(key);
	}
	
	public String getMovieMetadataURL(String key) {
		return resourcePath.getMovieMetaDataURL(key, true);
	}
	
	public String getEpisodesURL(String key) {
		return resourcePath.getEpisodesURL(key);
	}
	
	public String getSeasonsURL(String key) {
		return resourcePath.getSeasonsURL(key);
	}

    public String getImageURL(String url, int width, int height) {
        return resourcePath.getImageURL(url, width, height);
    }

    /**
	 * Given a resource's URL, read and return the serialized MediaContainer
	 * @param resourceURL
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws Exception
	 */
	private MediaContainer serializeResource(String resourceURL)
			throws MalformedURLException, IOException, Exception {
		MediaContainer mediaContainer;
		URL url = new URL(resourceURL);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		// We only want the updated data if something has changed.
		con.addRequestProperty("Cache-Control", "max-age=0");
		mediaContainer = serializer.read(MediaContainer.class,
				con.getInputStream(), false);
		return mediaContainer;
	}
	
	public MediaContainer serializeResourceFromString(String xmlString) throws Exception {
		MediaContainer container = serializer.read(MediaContainer.class, xmlString, false);
		return container;
	}


}
