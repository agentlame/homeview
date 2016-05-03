#Homeview

This is an AndroidTV 6.0 app designed to leverage an existing Plex server.  It is heavily influenced by the offical Plex AndroidTV app for UI and uses the Plex Rest API from serenity-android.  There is no licensing for this code.  Use my code however you want.  Licenses for any code/libraries I use are subject to their terms.  This is intended for my use only, but anyone may use or modify this as they see fit.
	
##Limitations
1.  Music and Photo sections are not currently supported
2.  No channel addon support.
3.  Only container formats that ExoPlayer uses are supported (MKV, MP4, etc) and in whatever naming conventions ExoPlayer supports.  No ISO or AVI.
4.  Video formats are limited to what the hardware supports.  This means no VC1 for the Nexus Player and a software decoder in the future may not even make that usable as Ffmpeg's decoding is single threaded.
5.  TrueHD passthrough is not currently supported.
6.  Any decoded audio (or 24bit PCM) is down res'd to 16 bit as Android's 32bit float is only two channels.
7.  SRT subtitles are not currently supported.  PGS is supported.
8.  "Extras" listed from the server for an item are displayed in the UI but they are not usable/playable.  Cause has not been investigated.
9.  Audio passthrough and auto refresh rate switching are enabled by default and are used when available.

##Requirements
1. Android Studio
2. Android SDK 23+ and Android NDK 10e (latest know version to build Ffmpeg).
3. Gradle 2.10

##Building
1. Building has only been done with Ubuntu 14.04.
2. Open the project in Android Studio first and make sure the SDK and NDK are setup.  See Google for instructions on those.  This should create a file "./local.properties" which includes sdk.dir and ndk.dir entries.
3. In a terminal run "./gradlew buildFFmpeg".  It will update git submodules, build ffmpeg for arm-v7 and x86, and copy the binaries.
4. Proceed as normal now.  You will need to handle your own signing certificates.  Alias, passwords, and file locations can be stored in "./app/signing.properties".

##Usage
1. The app should autodetect local Plex servers and choose the first one at initial startup.  You can choose specific ones or manually enter details in the settings.
2. The "Play" media button should shortcut you to the playback screen when pressing over highlighted media.
