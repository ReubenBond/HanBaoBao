package com.tallogre.hanbaobao.Models;

public class HistoryItem {
    public final CharSequence phrase;
    public final long time;

    public HistoryItem(long time, CharSequence phrase) {
        this.phrase = phrase;
        this.time = time;
    }
}
