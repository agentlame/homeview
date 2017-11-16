package com.monsterbutt.homeview.ui.interfaces;


import com.monsterbutt.homeview.ui.C;

public interface IMediaObserver {
  void statusChanged(IRegisteredMedia media, C.StatusChanged status);
}
