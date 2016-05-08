#Homeview

This is an AndroidTV 6.0 app designed to leverage an existing Plex server.  It is heavily influenced by the offical Plex AndroidTV app for UI and uses the Plex Rest API from serenity-android.  There is no licensing for this code.  Use my code however you want.  Licenses for any code/libraries I use are subject to their terms.  This is intended for my use only, but anyone may use or modify this as they see fit.
	
##Limitations
1.  Music and Photo sections are not currently supported
2.  No channel addon support.
3.  Only container formats that ExoPlayer uses are supported (MKV, MP4, etc) and in whatever naming conventions ExoPlayer supports.  No ISO or AVI.
4.  Video formats are limited to what the hardware supports.  This means no VC1 for the Nexus Player and a software decoder in the future may not even make that usable as Ffmpeg's decoding is single threaded.
5.  TrueHD passthrough is not currently supported.
6.  Any decoded audio (or 24bit PCM) is down res'd to 16 bit as Android's 32bit float is only two channels.
7.  VOBSUB and external SRT subtitles are not supported.

##Requirements
1. Android Studio
2. Android SDK 23+ and Android NDK 10e (latest know version to build Ffmpeg).
3. Gradle 2.10

##Building
1. Building has only been done with Ubuntu 14.04.
2. You will need to handle your own signing certificates.
3. Open the project in Android Studio first and make sure the SDK and NDK are setup.  See Google for instructions on those.  This should create a file "./local.properties" which includes sdk.dir and ndk.dir entries.
4. In a terminal run "git submodule update --init --recursive ExoPlayer"
5. Make sure the base directory has a local.properties with ndk.dir & sdk.dir filled properly
6. Make sure the base directory has a signing.properties with a STORE_FILE,STORE_PASSWORD,KEY_ALIAS, and KEY_PASSWORD setting filed for your signing certificate.
7. In a terminal run "./gradlew buildFFmpeg".  It will update git submodules, build ffmpeg for arm-v7 and x86, and copy the binaries.
8. Proceed as normal now.  You will need to handle your own signing certificates..

##Usage
1. The app should autodetect local Plex servers and choose the first one at initial startup.  You can choose specific ones or manually enter details in the settings.
2. The "Play" media button should shortcut you to the playback screen when pressing over highlighted media.
3. Audio passthrough is enabled by default and used when available.   Auto refresh rate switching is disabled by default.
