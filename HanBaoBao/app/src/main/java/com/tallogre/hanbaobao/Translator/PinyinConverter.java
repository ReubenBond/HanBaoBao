package com.tallogre.hanbaobao.Translator;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.UpdateLayout;
import android.util.Log;
import android.util.LruCache;

import com.tallogre.hanbaobao.Utilities.Globals;

import java.util.HashMap;

public class PinyinConverter {
    private static final String allAccentedPinyinCharacters =
            "āáǎàēéěèīíǐìōóǒòūúǔùǖǘǚǜǖǘǚǜĀÁǍÀĒÉĚÈĪÍǏÌŌÓǑÒŪÚǓÙǕǗǙǛǕǗǙǛ";
    private static final String accentedPinyinTone3Supplement = "ĂăĔĕĬĭŎŏŬŭ";

    private PinyinConverter() {
    }

    private static final HashMap<Character, CharSequence> accentLookup = new HashMap<>();

    static {
        accentLookup.put('a', "āáǎàa");
        accentLookup.put('e', "ēéěèe");
        accentLookup.put('i', "īíǐìi");
        accentLookup.put('o', "ōóǒòo");
        accentLookup.put('u', "ūúǔùu");
        accentLookup.put('ü', "ǖǘǚǜü");
        accentLookup.put('v', "ǖǘǚǜü");
        accentLookup.put('A', "ĀÁǍÀA");
        accentLookup.put('E', "ĒÉĚÈE");
        accentLookup.put('I', "ĪÍǏÌI");
        accentLookup.put('O', "ŌÓǑÒO");
        accentLookup.put('U', "ŪÚǓÙU");
        accentLookup.put('Ü', "ǕǗǙǛÜ");
        accentLookup.put('V', "ǕǗǙǛÜ");
    }

    public static int getToneNumber(CharSequence input) {
        int length = input.length();
        char last = input.charAt(length - 1);
        if (Character.isDigit(last)) {
            int toneNumber = (int) last - 0x30;

            // If the sequence has a tone number, then accept that number if it is within bounds and the sequence
            // also contains at least one letter.
            if (toneNumber > 0 && toneNumber <= 5) {
                for (int i = length - 1; i >= 0; i--) {
                    if (Character.isLetter(input.charAt(i))) return toneNumber;
                }
            }
        }

        for (int i = length - 1; i >= 0; i--) {
            int result = getAccentedPinyinCharacterTone(input.charAt(i));
            if (result >= 0) return result;
        }

        return 0;
    }

    private static int getAccentedPinyinCharacterTone(char c) {
        for (int i = 0; i < allAccentedPinyinCharacters.length(); i++) {
            char lookupChar = allAccentedPinyinCharacters.charAt(i);
            if (c == lookupChar) {
                return (i % 4) + 1;
            }
        }

        // Some converters apparently output a weird representation of tone 3
        for (int i = 0; i < accentedPinyinTone3Supplement.length(); i++) {
            char lookupChar = accentedPinyinTone3Supplement.charAt(i);
            if (c == lookupChar) {
                return 3;
            }
        }

        return -1;
    }

    private static final String vowels = "aAeEiIoOuUüÜvV";

    private static boolean isVowel(char c) {
        for (int i = 0; i < vowels.length(); i++) if (vowels.charAt(i) == c) return true;
        return false;
    }

    private static int getToneMarkCharacterIndex(CharSequence input) {
        int lastVowel = -1;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            /*
            From: https://en.wikipedia.org/wiki/Pinyin#Rules_for_placing_the_tone_mark
            An algorithm to find the correct vowel letter (when there is more than one) is as follows:
            */
            // If there is an a or an e, it will take the tone mark.
            if (c == 'a' || c == 'A' || c == 'e' || c == 'E') return i;

            if (isVowel(c)) lastVowel = i;

            if (i + 1 < input.length()) {
                // If there is an ou, then the o takes the tone mark.
                char n = input.charAt(i + 1);
                if (c == 'o' || c == 'O' && n == 'u' || n == 'U') return i;
            }
        }

        // Otherwise, the last vowel takes the tone mark.
        return lastVowel;
    }

    public static CharSequence formatSingleWord(CharSequence input) {
        SpannableStringBuilder result = new SpannableStringBuilder();
        formatSingleWord(input, result);
        return result;
    }

    private static LruCache<CharSequence, CharSequence> transliterationCache = new LruCache<>(512);
    public static LruCache<CharSequence, CharSequence> getTransliterationCache() {return transliterationCache;}

    public static void formatSingleWord(CharSequence input, SpannableStringBuilder builder) {
        if (input.length() < 1) return;

        // Trim the last character if it's a digit.
        int length = Character.isDigit(input.charAt(input.length() - 1)) ? input.length() - 1 : input.length();
        int startOffset = builder.length();
        builder.append(input.subSequence(0, length));

        // Find the correct tone.
        int toneNumber = getToneNumber(input);

        // Find the correct spot to place the tone and place it.
        boolean addedTone = false;
        if (toneNumber > 0 && toneNumber <= 5) {
            int toneMarkIndex = getToneMarkCharacterIndex(input) ;
            if (toneMarkIndex >= 0) {
                char toneMarkCharacter = builder.charAt(startOffset + toneMarkIndex);
                CharSequence replacements = accentLookup.get(toneMarkCharacter);
                if (replacements != null ) {builder.replace(startOffset+toneMarkIndex, startOffset+toneMarkIndex+1, replacements, toneNumber-1, toneNumber);
                addedTone = true;}
            }
        }

        if (addedTone && builder.length() > startOffset) {
            builder.setSpan(new ForegroundColorSpan(Globals.getToneColor(toneNumber)), startOffset, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    public static CharSequence formatAllWords(CharSequence input) {
        SpannableStringBuilder result = new SpannableStringBuilder();
        int wordStart = 0;
        int wordEnd = 0;
        boolean inWord = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                if (!inWord) {
                    // Copy the previous non-word part of the sequence verbatim.
                    result.append(input.subSequence(wordEnd, i));
                    wordStart = i;
                }
                inWord = true;
            } else {
                if (inWord) {
                    // Was in a word, finish it off.
                    formatSingleWord(input.subSequence(wordStart, i), result);
                    wordStart = wordEnd = i;
                }
                inWord = false;
            }
        }

        if (inWord) {
            // Copy and convert the last word.
            CharSequence subSequence = input.subSequence(wordStart, input.length());
            formatSingleWord(subSequence, result);
        } else {
            // Copy the last non-word part of the sequence verbatim.
            CharSequence subSequence = input.subSequence(wordEnd, input.length());
            result.append(subSequence);
        }

        return result;
    }
}
