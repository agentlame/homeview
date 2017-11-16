package com.monsterbutt.homeview.ui.details.interfaces;


public interface IDetailsScrollRowNotifier {
  void notifiy(int index);
  void register(IDetailsScrollRowListener listener);
  void release(IDetailsScrollRowListener listener);
}
