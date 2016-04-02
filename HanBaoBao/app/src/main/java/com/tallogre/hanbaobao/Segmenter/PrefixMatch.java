package com.tallogre.hanbaobao.Segmenter;

import com.tallogre.hanbaobao.Models.DictionaryEntry;

public class PrefixMatch {
    private static final byte NONE = 0x00;
    private static final byte MATCH = 0x01;
    private static final byte PREFIX = 0x10;
    private byte state = NONE;
    public double frequency;
    private String pinyin;

    public boolean isMatch() {
        return (this.state & MATCH) == MATCH;
    }

    public void setMatch() {
        this.state = (byte)(this.state | MATCH);
    }

    public boolean isPrefix() {
        return (this.state & PREFIX) == PREFIX;
    }

    public void setPrefix() {
        this.state = (byte)(this.state | PREFIX);
    }

    public double getFrequency() {
        return this.frequency;
    }

    public String getPinyin() {
        return this.pinyin;
    }

    public void setPinyin(String pinyin) {
        this.pinyin = pinyin;
    }

    public void setFrequency(double frequency) {
        this.frequency = frequency;
    }

    public void reset() {
        this.state = NONE;
        frequency = Double.MIN_VALUE;
    }

    public void copyFrom(PrefixMatch cachedResult) {
        state = cachedResult.state;
        frequency = cachedResult.frequency;
        pinyin = cachedResult.pinyin;
    }
}
