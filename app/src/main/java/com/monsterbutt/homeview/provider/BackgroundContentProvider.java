package com.monsterbutt.homeview.provider;


public class BackgroundContentProvider extends ImageContentProvider {

    public static String AUTHORITY = "com.monsterbutt.homeview.provider.BackgroundContentProvider";
    public static String CONTENT_URI = "content://" + AUTHORITY + "/";

    @Override
    protected String getContentURI() {
        return CONTENT_URI;
    }
}
