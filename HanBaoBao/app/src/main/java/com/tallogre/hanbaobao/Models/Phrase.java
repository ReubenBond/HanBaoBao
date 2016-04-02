package com.tallogre.hanbaobao.Models;

import android.util.Log;

import com.tallogre.hanbaobao.Segmenter.Candidate;
import com.tallogre.hanbaobao.Utilities.CharacterUtil;
import com.tallogre.hanbaobao.Utilities.CollectionUtil;
import com.tallogre.hanbaobao.Utilities.Globals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;

public class Phrase {
    public CharSequence original;
    public List<Term> terms;
    public long creationTime;

    public Phrase(CharSequence original) {
        this.original = original;
        terms = new ArrayList<>();
        creationTime = System.currentTimeMillis();
    }

    // The map between start index and end indices for words which should not be included in the
    // Transliterated result.
    public Map<Integer, List<Integer>> bannedRoutes;

    // A map from sentence start indices to word start indices to a list of word end indices.
    public Map<Integer, Map<Integer, List<Candidate<Integer>>>> routes;

    // Resegments this phrase, attempting to select a different alternative for the provided term.
    public Observable<Phrase> resegment(Term term) {
        int start = 0;
        for (Term t : terms) {
            if (t.equals(term)) break;
            start += t.getOriginal().length();
        }

        return resegment(start, start + term.getOriginal().length() - 1);
    }

    // Eagerly touches all terms. This is performed off the main thread to ensure that these values
    // are ready when the main thread needs to access them.
    public void lookupAll() {
        for (Term t : terms) {
            t.getTransliterated();
        }
    }

    public void addTerm(Term term) {
        terms.add(term);
    }
    public void addTerm(CharSequence term) {
        terms.add(new Term(term));
    }

    public void clearTerms() {
        terms.clear();
    }

    // Resegments this phrase, attempting to select a different alternative for the described by
    // the specified start and end indices
    public Observable<Phrase> resegment(int start, int end) {
        Log.i("RESEG", "Banning " + start + " -> " + end);
        List<Integer> ends;
        if (bannedRoutes == null) bannedRoutes = new HashMap<>();
        if (!bannedRoutes.containsKey(start)) {
            ends = new ArrayList<>();
            bannedRoutes.put(start, ends);
        } else {
            ends = bannedRoutes.get(start);
        }
        if (!ends.contains(end)) ends.add(end);
        //printBannedRoutes();

        return Globals.getTransliterator().transliterateAsync(this);
    }

    private void printBannedRoutes() {
        Log.i("RESEG", bannedRoutes.size() + " banned starting points");
        for (int start : bannedRoutes.keySet()) {
            Log.i("RESEG", bannedRoutes.get(start).size() + " banned ending points from starting point " + start);
            for (int end : bannedRoutes.get(start)) {
                StringBuilder b = new StringBuilder();

                for (int i = 0; i < start; i++) b.append(original.charAt(i));
                for (int i = start; i <= end; i++) b.append('_');
                for (int i = end + 1; i < original.length(); i++) b.append(original.charAt(i));
                Log.i("RESEG", b.toString());
            }
        }
    }

    @Override
    public int hashCode() {
        int result = original == null ? 0 : CharacterUtil.hashCode(original);
        result = 31 * result + CollectionUtil.hashCode(terms);
        return result;
    }

    @Override
    public String toString() {
        if (terms == null) return "";
        StringBuilder builder = new StringBuilder();
        for (Term term : terms) {
            if (term.getTransliterated() != null)
                builder.append(term.getTransliterated());
            else builder.append(term.getOriginal());
        }
        return builder.toString();
    }

    public String segmentedTermsString() {
        StringBuilder b = new StringBuilder();
        boolean first = true;
        for (Term t : terms) {
            if(!first) {
                b.append(", ");
            }
            b.append(t.getOriginal());
            first=false;
        }
        return b.toString();
    }
}
