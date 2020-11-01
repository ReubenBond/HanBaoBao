package com.tallogre.hanbaobao.Models;

import android.database.Cursor;
import androidx.annotation.NonNull;
import android.text.SpannableStringBuilder;

import com.tallogre.hanbaobao.Fragments.SettingsFragment;
import com.tallogre.hanbaobao.Translator.PinyinConverter;
import com.tallogre.hanbaobao.Utilities.Globals;

import java.util.List;

public class DictionaryEntry {
    public final String simplified;
    public final String traditional;
    public final String pinyin;
    public final String definition;
    public final int hskLevel;
    public final long partOfSpeech;
    public final String classifier;
    public final double frequency;
    public final String concept;
    public final String topic;
    public final String parentTopic;
    public final String notes;

    public DictionaryEntry(
            String simplified,
            String traditional,
            String pinyin,
            String definition,
            int hskLevel,
            long pos,
            String classifier,
            double frequency, String concept, String topic, String parentTopic, String notes) {
        this.simplified = simplified;
        this.traditional = traditional;
        this.pinyin = pinyin;
        this.definition = definition;
        this.hskLevel = hskLevel;
        this.partOfSpeech = pos;
        this.classifier = classifier;
        this.frequency = frequency;
        this.concept = concept;
        this.topic = topic;
        this.parentTopic = parentTopic;
        this.notes = notes;
    }

    public CharSequence getDefinition() {
        if (definition == null) return "";
        return PinyinConverter.formatAllWords(definition);
    }

    public CharSequence getTransliteration() {
        if (pinyin == null) return "";
        String[] transliteratedTerms = pinyin.split(" ");
        if (transliteratedTerms.length == 1)
            return PinyinConverter.formatSingleWord(pinyin);
        SpannableStringBuilder builder = new SpannableStringBuilder();
        for (String term : transliteratedTerms) {
            PinyinConverter.formatSingleWord(term, builder);
        }
        return builder;
    }

    public String getHeadword() {
        int charSet = Globals.getUserPreferences().getCachedCharacterSet();
        if (charSet == SettingsFragment.CharacterSet.SIMPLIFIED && simplified != null)
        {
            return simplified;
        } else if (charSet == SettingsFragment.CharacterSet.TRADITIONAL && traditional != null) {
            return traditional;
        } else if (charSet == SettingsFragment.CharacterSet.BOTH && traditional != null && simplified != null && !traditional.equals(simplified)) {
            return simplified + " / " + traditional;
        }

        if (simplified != null) return simplified;
        if (traditional != null) return traditional;
        return "";
    }

    public List<String> getPartsOfSpeech() {
        if (partOfSpeech == 0) return null;
        return PartOfSpeech.toStrings(this.partOfSpeech);
    }

    @NonNull
    public static DictionaryEntry getFromCursor(Cursor cursor) {
        int simplifiedColId = cursor.getColumnIndex("simplified");
        int traditionalColId = cursor.getColumnIndex("traditional");
        int transliterationColId = cursor.getColumnIndex("pinyin");
        int definitionColId = cursor.getColumnIndex("definition");
        int hskLevelColId = cursor.getColumnIndex("hsk_level");
        int classifierColId = cursor.getColumnIndex("classifier");
        int posColId = cursor.getColumnIndex("part_of_speech");
        int frequencyColId = cursor.getColumnIndex("frequency");
        int conceptColId = cursor.getColumnIndex("concept");
        int topicColId = cursor.getColumnIndex("topic");
        int parentTopicColId = cursor.getColumnIndex("parent_topic");
        int notesColId = cursor.getColumnIndex("notes");

        String simplified = cursor.getString(simplifiedColId);
        String traditional = cursor.getString(traditionalColId);
        String pinyin = cursor.getString(transliterationColId);
        String definition = cursor.getString(definitionColId);
        int hskLevel = cursor.getInt(hskLevelColId);
        long pos = cursor.getLong(posColId);
        String classifier = cursor.getString(classifierColId);
        double frequency = cursor.getDouble(frequencyColId);
        String concept = cursor.getString(conceptColId);
        String topic = cursor.getString(topicColId);
        String parentTopic = cursor.getString(parentTopicColId);
        String notes = cursor.getString(notesColId);
        return new DictionaryEntry(simplified, traditional, pinyin, definition, hskLevel,
                pos, classifier, frequency, concept, topic, parentTopic, notes);
    }
}
