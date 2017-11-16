package com.monsterbutt.homeview.ui.details.interfaces;


public interface IDetailsItemUpdateNotifier {
  void register(IDetailsItemUpdateListener listener);
  void release(IDetailsItemUpdateListener listener);
}
