package com.tallogre.hanbaobao.Segmenter;

interface PrefixDictionary {
    void match(CharSequence charArray, int begin, int length, PrefixMatch result);
}
