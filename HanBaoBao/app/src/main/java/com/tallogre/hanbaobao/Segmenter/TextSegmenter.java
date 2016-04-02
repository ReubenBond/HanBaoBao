package com.tallogre.hanbaobao.Segmenter;

import android.util.Log;

import com.tallogre.hanbaobao.Models.Phrase;
import com.tallogre.hanbaobao.Models.Term;
import com.tallogre.hanbaobao.Utilities.CharacterUtil;
import com.tallogre.hanbaobao.Utilities.DebugUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TextSegmenter {
    private final SegmentationDictionary dictionary;

    public TextSegmenter(SegmentationDictionary dictionary) {
        this.dictionary = dictionary;
    }

    public void segmentText(Phrase phrase) {
        phrase.clearTerms();
        boolean wasChinese = false;
        int start = 0, end = 0;
        for (; end < phrase.original.length(); end++) {
            char currentChar = phrase.original.charAt(end);
            int length = end - start;
            if (CharacterUtil.isPunctuation(currentChar)) {
                // Add the chinese or non-chinese segment.
                if (length > 0) {
                    if (wasChinese) processSentence(phrase, start, end);
                    else phrase.addTerm(phrase.original.subSequence(start, end));
                }

                // Advance in the sentence.
                start = end;
                if (currentChar == '\n') start++;
                wasChinese = false;
            } else {
                boolean isChinese = CharacterUtil.isProbablyChinese(currentChar);
                if (isChinese && !wasChinese) {
                    // Transitioning from non-CJK to CJK text, add the string verbatim.
                    if (length > 0) phrase.addTerm(phrase.original.subSequence(start, end));
                    wasChinese = true;
                    start = end;
                } else if (!isChinese && wasChinese) {
                    // Transitioning from CJK to non-CJK text, process the CJK text.
                    if (length > 0) processSentence(phrase, start, end);
                    wasChinese = false;
                    start = end;
                }
            }
        }

        // Handle the last segment.
        if (end - start > 0) {
            CharSequence remainder = phrase.original.subSequence(start, end);
            if (wasChinese) processSentence(phrase, start, end);
            else phrase.addTerm(remainder);
        }
    }

    private void processSentence(Phrase phrase, int sentenceStart, int sentenceEnd) {
        CharSequence sentence = phrase.original.subSequence(sentenceStart, sentenceEnd);

        // Generate the directed graph of routes through the sentence.
        Map<Integer, List<Candidate<Integer>>> routes;
        if (phrase.routes == null) phrase.routes = new HashMap<>();
        if (!phrase.routes.containsKey(sentenceStart)) {
            routes = findRoutes(sentence);
            //printRoutes(routes, sentence);
            phrase.routes.put(sentenceStart, routes);
        }

        //printRoutes(routes, sentence);
        Map<Integer, Candidate<Integer>> optimalRoute = findOptimalRoute(phrase, sentenceStart, sentenceEnd);

        // Add all segments from the optimal route to the result.
        for (int start = 0, end; start < sentence.length(); start = end) {
            Candidate<Integer> candidate = optimalRoute.get(start);
            end = candidate.key + 1;
            Term term = new Term(sentence.subSequence(start, end));
            term.setPinyin(candidate.pinyin);
            phrase.addTerm(term);
        }
    }

    private void printRoutes(Map<Integer, List<Candidate<Integer>>> routes, CharSequence sentence) {
        // for each start address, traverse into the inner routes.
        Map<CharSequence, Double> wordScores = new HashMap<>();
        printRoutesInner(routes, sentence, new StringBuilder(), 0, 0, wordScores);
        for (CharSequence word : wordScores.keySet()) {
            Log.i("TextSegmenter", String.format("%.4f: %s", wordScores.get(word), word));
        }
    }

    private void printRoutesInner(
            Map<Integer, List<Candidate<Integer>>> routes,
            CharSequence sentence,
            StringBuilder prefix,
            int start,
            double score,
            Map<CharSequence, Double> wordScores) {
        List<Candidate<Integer>> currentRoutes = routes.get(start);
        if (start >= sentence.length()) {
            Log.i("TextSegmenter", String.format("%.4f: %s", score, prefix.toString()));
            return;
        }
        for (Candidate<Integer> candidate : currentRoutes) {
            int end = candidate.key;
            int initialPrefixLength = prefix.length();
            CharSequence word = sentence.subSequence(start, end + 1);
            wordScores.put(word, candidate.frequency);
            prefix.append(word).append(" ");
            printRoutesInner(routes, sentence, prefix, end + 1, score + candidate.frequency, wordScores);
            prefix.setLength(initialPrefixLength);
        }
    }

    /**
     * Creates a graph of possible paths through the sentence.
     *
     * @param sentence to find paths through.
     * @return a map from start character index to a list of end characters for that word.
     */
    private Map<Integer, List<Candidate<Integer>>> findRoutes(CharSequence sentence) {
        Map<Integer, List<Candidate<Integer>>> routes = new HashMap<>();
        int start = 0, end = 0;
        PrefixMatch searchResult = new PrefixMatch();
        while (start < sentence.length()) {
            dictionary.match(sentence, start, end - start + 1, searchResult);
            if (searchResult.isPrefix() || searchResult.isMatch()) {
                // If this represents an exact match, record that match.
                if (searchResult.isMatch()) {
                    if (!routes.containsKey(start)) {
                        List<Candidate<Integer>> value = new ArrayList<>();
                        routes.put(start, value);
                        value.add(new Candidate<>(end, searchResult.getFrequency(), searchResult.getPinyin()));
                    } else {
                        // There are multiple alternatives for this current start point, add the current alternative
                        // here.
                        routes.get(start).add(new Candidate<>(end, searchResult.getFrequency(), searchResult.getPinyin()));
                    }
                }
                end++;

                // If the end of the sentence has been reached, move the starting point and continue.
                if (end >= sentence.length()) {
                    start++;
                    end = start;
                }
            } else {
                // There are no words starting with the current sequence.
                start++;
                end = start;
            }
        }

        // Add all unknown characters
        for (start = 0; start < sentence.length(); ++start) {
            if (!routes.containsKey(start)) {
                List<Candidate<Integer>> value = new ArrayList<>();
                value.add(new Candidate<>(start, 0, null));
                routes.put(start, value);
            }
        }
        return routes;
    }

    /**
     * Finds the highest-value route through the sentence.
     *
     * @return the highest-value route through the sentence.
     */
    private Map<Integer, Candidate<Integer>> findOptimalRoute(Phrase phrase, int sentenceStart, int sentenceEnd) {
        CharSequence sentence = phrase.original.subSequence(sentenceStart, sentenceEnd);
        Map<Integer, List<Candidate<Integer>>> routes = phrase.routes.get(sentenceStart);
        Map<Integer, List<Integer>> bannedRoutes = phrase.bannedRoutes;

        Map<Integer, Candidate<Integer>> weightedRoutes = new HashMap<>();
        weightedRoutes.put(sentence.length(), new Candidate<>(0, 0.0, null));
        for (int start = sentence.length() - 1; start >= 0; start--) {
            Candidate<Integer> candidate = null;
            List<Integer> bannedRoutesForCandidate = null;
            if (bannedRoutes != null) bannedRoutesForCandidate = bannedRoutes.get(start + sentenceStart);

            // When everything has been banned, reset the bans so that valid paths can be found.
            if (bannedRoutesForCandidate != null && bannedRoutesForCandidate.size() == routes.get(start).size()) {
                bannedRoutesForCandidate.clear();
            }

            for (Candidate<Integer> routeCandidate : routes.get(start)) {
                // Skip banned routes
                if (bannedRoutesForCandidate != null && bannedRoutesForCandidate.contains(routeCandidate.key + sentenceStart))
                {
                    continue;
                }

                int end = routeCandidate.key;
                // Get the total value of this path.
                double frequency = routeCandidate.frequency + weightedRoutes.get(end + 1).frequency;
                if (null == candidate) {
                    // This is the first/only candidate for this position.
                    candidate = new Candidate<>(end, frequency, routeCandidate.pinyin);
                } else if (candidate.frequency < frequency) {
                    // The current candidate is better than the previous candidate for this position.
                    candidate.frequency = frequency;
                    candidate.key = end;
                    candidate.pinyin = routeCandidate.pinyin;
                }
            }

            weightedRoutes.put(start, candidate);
        }
        return weightedRoutes;
    }
}