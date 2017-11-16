package com.monsterbutt.homeview.ui.grid;


public class QuickJumpRow {

    public static final String NUM_OR_SYMBOL = "#";

    final String letter;
    public final int index;

    public QuickJumpRow(String letter, int index) {
        this.letter = letter;
        this.index = index;
    }

    @Override
    public String toString() { return letter; }
}
