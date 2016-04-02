package com.tallogre.hanbaobao.Models;

import android.text.SpannableStringBuilder;
import android.util.Log;

import com.tallogre.hanbaobao.Translator.PinyinConverter;
import com.tallogre.hanbaobao.Utilities.CharacterUtil;
import com.tallogre.hanbaobao.Utilities.Globals;

import java.util.List;

public class Term {
    private CharSequence original;
    private CharSequence transliterated;
    private List<DictionaryEntry> definitions;
    private CharSequence pinyin;

    public Term(CharSequence original) {
        this.original = original;
    }

    public void setPinyin(CharSequence pinyin) {
        this.pinyin = pinyin;
    }

    public CharSequence getOriginal() {
        return original;
    }

    public List<DictionaryEntry> getDefinitions() {
        if (definitions == null && CharacterUtil.isProbablyChinese(original))
        {
            definitions = Globals.getDictionary().findInHeadword(original);
        }

        return definitions;
    }

    public CharSequence getTransliterated() {
        if (transliterated == null && original != null && original.length() > 0) {
            // Fetch from cache.
            transliterated = PinyinConverter.getTransliterationCache().get(original);

            // Not in cache, generate and add to cache if valid.
            if (transliterated == null) {
                transliterated = transliterate();
                if (transliterated != null && transliterated.length() > 0)
                    PinyinConverter.getTransliterationCache().put(original, transliterated);
            }
        }

        return transliterated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Term term = (Term) o;
        return CharacterUtil.equals(original, term.original);
    }

    @Override
    public int hashCode() {
        return CharacterUtil.hashCode(original);
    }

    private CharSequence getPinyin() {
        if (pinyin == null) {
            if (getDefinitions() == null || getDefinitions().size() == 0) return null;
            else {
                DictionaryEntry firstDefinition = getDefinitions().get(0);
                if (firstDefinition == null) return null;
                pinyin = firstDefinition.pinyin;
            }
        }
        return pinyin;
    }

    private CharSequence transliterate() {
        // For terms which aren't a single character, use the dictionary to find the pinyin.
        if (original != null && original.length() >= 1 && getPinyin() != null) {
            SpannableStringBuilder builder = new SpannableStringBuilder();

            int segmentStart = 0;
            for (int i = 0; i < original.length(); i++) {
                char c = original.charAt(i);
                CharSequence transliteratedTerm = CharacterUtil.getNextSplit(getPinyin(), segmentStart);
                if (CharacterUtil.isProbablyChinese(c) && transliteratedTerm != null) {
                    segmentStart += transliteratedTerm.length() + 1;
                    // If the term is known, format the pinyin.
                    PinyinConverter.formatSingleWord(transliteratedTerm, builder);
                }
            }

            if (builder.length() > 0) {
                return builder;
            }
        }

        return null;
    }
}
