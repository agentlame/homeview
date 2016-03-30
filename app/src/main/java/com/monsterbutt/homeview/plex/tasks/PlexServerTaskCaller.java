package com.monsterbutt.homeview.plex.tasks;


public interface PlexServerTaskCaller {

    public void handlePreTaskUI();
    public void handlePostTaskUI(Boolean result, PlexServerTask task);
}
