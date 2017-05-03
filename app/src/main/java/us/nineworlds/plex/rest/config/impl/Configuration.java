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

package us.nineworlds.plex.rest.config.impl;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import java.net.HttpURLConnection;

import us.nineworlds.plex.rest.config.IConfiguration;

/**
 * @author dcarver
 *
 */
public class Configuration implements IConfiguration {
	
	private String host;
	
	private String port;

	private String serverToken = "";
	private String appVersion;
	private String deviceId;

	/* (non-Javadoc)
	 * @see com.github.kingargyle.plexapp.config.IConfiguration#getHost()
	 */
	public String getHost() {
		return host;
	}

	/* (non-Javadoc)
	 * @see com.github.kingargyle.plexapp.config.IConfiguration#setHost(java.lang.String)
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/* (non-Javadoc)
	 * @see com.github.kingargyle.plexapp.config.IConfiguration#getPort()
	 */
	public String getPort() {
		return port;
	}

	/* (non-Javadoc)
	 * @see com.github.kingargyle.plexapp.config.IConfiguration#setPort(java.lang.String)
	 */
	public void setPort(String port) {
		this.port = port;
	}

	public String getDeviceId() { return deviceId;}
	public void setDeviceId(String id) {
		deviceId = id;
	}

	public String getAppVersion() { return appVersion;}
	public void setAppVersion(String version) {
		appVersion = version;
	}
	public String getServerToken() { return serverToken;}
	public void setServerToken(String token) {
		serverToken = token;
	}

	public void fillRequestProperties(HttpURLConnection con) {

		if (!TextUtils.isEmpty(getDeviceId())) {
			con.setRequestProperty("X-Plex-Product", "Homeview");
			con.setRequestProperty("X-Plex-Client-Identifier", getDeviceId());
			con.setRequestProperty("X-Plex-Device", android.os.Build.MODEL);
			con.setRequestProperty("X-Plex-Device-Name", Build.DEVICE);
			con.setRequestProperty("X-Plex-Version", getAppVersion());
			con.setRequestProperty("X-Plex-Model", Build.DEVICE);
			con.setRequestProperty("X-Plex-Device-Vendor", Build.MANUFACTURER);
			con.setRequestProperty("X-Plex-Platform", "AndroidTV");
			con.setRequestProperty("X-Plex-Provides", "player,controller");
			con.setRequestProperty("X-Plex-Client-Platform", "Android TV");
			con.setRequestProperty("X-Plex-Platform-Version", Build.VERSION.RELEASE);
		}
	}
}
