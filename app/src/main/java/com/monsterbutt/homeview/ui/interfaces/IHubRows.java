package com.monsterbutt.homeview.ui.interfaces;


import java.util.List;

import us.nineworlds.plex.rest.model.impl.Hub;
import us.nineworlds.plex.rest.model.impl.MediaContainer;

public interface IHubRows {
  MediaContainer getHubs();
  void updateHubs(List<Hub> hubs);
}
