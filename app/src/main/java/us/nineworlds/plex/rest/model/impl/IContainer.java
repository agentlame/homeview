package us.nineworlds.plex.rest.model.impl;


import java.util.List;

public interface IContainer {

  List<Directory> getDirectories();
  List<Video> getVideos();
  long getParentIndex();
}
