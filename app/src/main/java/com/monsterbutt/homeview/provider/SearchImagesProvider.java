package com.monsterbutt.homeview.provider;

public class SearchImagesProvider extends ImageContentProvider {

    public static String AUTHORITY = "com.monsterbutt.homeview.provider.SearchImagesProvider";
    public static String CONTENT_URI = "content://" + AUTHORITY + "/";

    @Override
    protected String getContentURI() {
        return CONTENT_URI;
    }
}

