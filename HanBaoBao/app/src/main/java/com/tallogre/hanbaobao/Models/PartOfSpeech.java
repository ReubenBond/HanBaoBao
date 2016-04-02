package com.tallogre.hanbaobao.Models;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by reube on 6/5/2016.
 */
public class PartOfSpeech {

    public static List<String> toStrings(long input) {
        List<String> result = new ArrayList<>();
        if ((input & ((long)1)) != 0) result.add("ADDRESS");
        if ((input & (((long)1 << 1))) != 0) result.add("ADJECTIVE");
        if ((input & ((long)1 << 2)) != 0) result.add("ADVERB");
        if ((input & ((long)1 << 3)) != 0) result.add("AUXILIARY VERB");
        if ((input & ((long)1 << 4)) != 0) result.add("BOUND MORPHEME");
        if ((input & ((long)1 << 5)) != 0) result.add("SET PHRASE");
        if ((input & ((long)1 << 6)) != 0) result.add("CITY");
        if ((input & ((long)1 << 7)) != 0) result.add("COMPLEMENT");
        if ((input & ((long)1 << 8)) != 0) result.add("CONJUNCTION");
        if ((input & ((long)1 << 9)) != 0) result.add("COUNTRY");
        if ((input & ((long)1 << 10)) != 0) result.add("DATE");
        if ((input & ((long)1 << 11)) != 0) result.add("DETERMINER");
        if ((input & ((long)1 << 12)) != 0) result.add("DIRECTIONAL");
        if ((input & ((long)1 << 13)) != 0) result.add("EXPRESSION");
        if ((input & ((long)1 << 14)) != 0) result.add("FOREIGN TERM");
        if ((input & ((long)1 << 15)) != 0) result.add("GEOGRAPHY");
        if ((input & ((long)1 << 16)) != 0) result.add("IDIOM");
        if ((input & ((long)1 << 17)) != 0) result.add("INTERJECTION");
        if ((input & ((long)1 << 18)) != 0) result.add("MEASURE WORD");
        if ((input & ((long)1 << 19)) != 0) result.add("MEASUREMENT");
        //if ((input & ((long)1 << 20)) != 0) result.add("NAME");
        if ((input & ((long)1 << 20)) != 0 || (input & ((long)1 << 21)) != 0) result.add("NOUN");
        if ((input & ((long)1 << 22)) != 0) result.add("NUMBER");
        if ((input & ((long)1 << 23)) != 0) result.add("NUMERAL");
        if ((input & ((long)1 << 24)) != 0) result.add("ONOMATOPOEIA");
        if ((input & ((long)1 << 25)) != 0) result.add("ORDINAL");
        if ((input & ((long)1 << 26)) != 0) result.add("ORGANIZATION");
        if ((input & ((long)1 << 27)) != 0) result.add("PARTICLE");
        if ((input & ((long)1 << 28)) != 0) result.add("PERSON");
        if ((input & ((long)1 << 29)) != 0) result.add("PHONETIC");
        if ((input & ((long)1 << 30)) != 0) result.add("PHRASE");
        if ((input & ((long)1 << 31)) != 0) result.add("PLACE");
        if ((input & ((long)1 << 32)) != 0) result.add("PREFIX");
        if ((input & ((long)1 << 33)) != 0) result.add("PREPOSITION");
        if ((input & ((long)1 << 34)) != 0) result.add("PRONOUN");
        if ((input & ((long)1 << 35)) != 0) result.add("PROPER NOUN");
        if ((input & ((long)1 << 36)) != 0) result.add("QUANTITY");
        if ((input & ((long)1 << 37)) != 0) result.add("RADICAL");
        if ((input & ((long)1 << 38)) != 0) result.add("SUFFIX");
        if ((input & ((long)1 << 39)) != 0) result.add("TEMPORAL");
        if ((input & ((long)1 << 40)) != 0) result.add("TIME");
        if ((input & ((long)1 << 41)) != 0) result.add("VERB");
        return result;
    }
}