package com.monsterbutt.homeview.ui.grid.interfaces;


import us.nineworlds.plex.rest.model.impl.MediaContainer;

public interface IGridParent {
  MediaContainer getContainer();
  void loadData(MediaContainer container);
}
