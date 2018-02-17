package com.monsterbutt.homeview.ui.grid.interfaces;


import com.monsterbutt.homeview.ui.grid.QuickJumpRow;

import java.util.List;

public interface IQuickJumpDisplay {
  void setQuickListVisible(boolean visible);
  void setQuickJumpList(List<QuickJumpRow> quickjumpList);
  void setCurrentSelectionName(String name);
}
