package com.monsterbutt.homeview.ui.interfaces;


import com.monsterbutt.homeview.plex.media.PlexLibraryItem;
import com.monsterbutt.homeview.ui.CardObjectList;

import us.nineworlds.plex.rest.model.impl.IContainer;

public interface ICardObjectListCallback {
  void shouldAdd(IContainer container, PlexLibraryItem item, CardObjectList objects);
}
