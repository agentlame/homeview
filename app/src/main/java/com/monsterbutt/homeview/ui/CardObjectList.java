package com.monsterbutt.homeview.ui;


import com.monsterbutt.homeview.ui.presenters.CardObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CardObjectList {

  private List<CardObject> list = new ArrayList<>();
  private HashMap<String, CardObject> map = new HashMap<>();

  public boolean add(String key, CardObject card) {
    final boolean contains = map.containsKey(key);
    if (!contains) {
      map.put(key, card);
      list.add(card);
    }
    return !contains;
  }

  public boolean contains(String key) {
    CardObject obj = map.get(key);
    return obj != null;
  }

  public List<CardObject> getList() {
    return list;
  }
}
