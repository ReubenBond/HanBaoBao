package com.tallogre.hanbaobao.Utilities;

import java.util.Comparator;
import java.util.regex.Pattern;


public class CharacterUtil {
    public static Pattern reSkip = Pattern.compile("(\\d+\\.\\d+|[a-zA-Z0-9]+)");
    private static Pattern punctuation = Pattern.compile("[\\u3000-\\u303F\\p{P}\\p{S}\\s]");
    public static boolean isPunctuation(char c) {
        return punctuation.matcher(String.valueOf(c)).find();
    }

    public static boolean isProbablyChinese(CharSequence chars) {
        if (chars ==null) return false;
        for(int i = 0; i < chars.length();i++) if (Character.UnicodeBlock.of(chars.charAt(i)) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) return true;
        return false;
    }

    public static boolean isProbablyChinese(char c) {
        return Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS;
    }

    public static final Comparator<String> COMPARE_LENGTH_AND_ORDINAL = new StringLengthOrdinalComparator();

    private static class StringLengthOrdinalComparator implements Comparator<String> {

        @Override
        public int compare(String lhs, String rhs) {
            if ((Object)lhs == (Object)rhs) return 0;
            if (lhs == null) return -1;
            if (rhs == null) return 1;

            int lengthComparison = lhs.length() - rhs.length();
            if (lengthComparison != 0) return lengthComparison;

            return lhs.compareTo(rhs);
        }
    }

    public static boolean equals(CharSequence a, CharSequence b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        for (int i = 0; i<a.length(); i++) {
            if (a.charAt(i) != b.charAt(i)) return false;
        }

        return true;
    }

    public static boolean endsWith(CharSequence haystack, CharSequence suffix) {
        if (haystack == suffix) return true;
        if (haystack == null || suffix == null) return false;
        if (haystack.length() < suffix.length()) return false;
        int haystackLength = haystack.length();
        int start = haystackLength-suffix.length();

        for (int i = start; i<haystackLength; i++) {
            if (haystack.charAt(i) != suffix.charAt(i - start)) return false;
        }

        return true;
    }

    public static int hashCode(CharSequence cSeq) {
        if (cSeq == null) return 0;
        if (cSeq instanceof String) return cSeq.hashCode();
        int h = 0;
        for (int i = 0; i < cSeq.length(); ++i)
            h = 31*h + cSeq.charAt(i);
        return h;
    }

    public static CharSequence getNextSplit(CharSequence string, int start) {
        if (string == null) return null;
        int end;
        for (end = start; end < string.length(); end++) if (string.charAt(end) == ' ') break;
        if (end == start || end > string.length()) return null;
        return string.subSequence(start, end);
    }
}
