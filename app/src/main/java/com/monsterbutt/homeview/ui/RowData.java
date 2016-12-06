package com.monsterbutt.homeview.ui;

import android.support.annotation.NonNull;
import android.support.v17.leanback.widget.ListRow;

public class RowData implements Comparable<RowData> {

    public final String id;
    public final ListRow data;
    public int currentIndex;

    public RowData(String id, int index, ListRow data) {
        this.id = id;
        currentIndex = index;
        this.data = data;
    }

    @Override
    public int compareTo(@NonNull RowData row) {

        // reverse order
        if (this.currentIndex < row.currentIndex)
            return 1;
        return -1;
    }
}

