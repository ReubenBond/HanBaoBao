package com.tallogre.hanbaobao.Segmenter;

public class Candidate<T> {
    public String pinyin;
    public T key;
    public Double frequency;

    public Candidate(T key, double frequency, String pinyin) {
        this.key = key;
        this.frequency = frequency;
        this.pinyin = pinyin;
    }

    @Override
    public String toString() {
        return String.format("Candidate [key=%s, frequency=%s]", key, frequency);
    }

}
