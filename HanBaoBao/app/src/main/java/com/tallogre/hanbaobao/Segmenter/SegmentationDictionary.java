package com.tallogre.hanbaobao.Segmenter;

import android.database.Cursor;
import android.util.LruCache;

import com.tallogre.hanbaobao.Database.DictionaryDatabase;
import com.tallogre.hanbaobao.Models.DictionaryEntry;

import java.util.List;


public class SegmentationDictionary {
    private final DictionaryDatabase dictionary;

    public SegmentationDictionary(DictionaryDatabase dictionary) {
        this.dictionary = dictionary;
    }

    /*public double getFrequency(CharSequence key) {
        List<DictionaryEntry> entries = dictionary.findInHeadword(key);
        if (entries == null || entries.size() == 0) return Double.MIN_VALUE;
        return entries.get(0).frequency;
    }

    public double getViterbiProbability(char term, char state) {
        return dictionary.getViterbiProbability(term, state);
    }*/

    private LruCache<CharSequence, PrefixMatch> matchCache = new LruCache<>(512);
    public void match(CharSequence charArray, int begin, int length, PrefixMatch result) {
        String key = charArray.subSequence(begin, begin + length).toString();
        PrefixMatch cachedResult = matchCache.get(key);
        if (cachedResult != null) {
            result.copyFrom(cachedResult);
            return;
        }

        Cursor cursor = dictionary.queryPrefix(key);
        result.reset();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String original = cursor.getString(0);
                if (original.length() == key.length()) {
                    result.setMatch();
                    result.setFrequency(cursor.getDouble(1));
                    result.setPinyin(cursor.getString(2));
                } else result.setPrefix();
                if (result.isMatch() && result.isPrefix()) break;
            } while (cursor.moveToNext());
        }

        // Cache the result.
        cachedResult = new PrefixMatch();
        cachedResult.copyFrom(result);
        matchCache.put(key, cachedResult);
    }
}
