package com.tallogre.hanbaobao.Utilities;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;

import com.tallogre.hanbaobao.Database.DictionaryDatabase;
import com.tallogre.hanbaobao.Database.UserDatabase;
import com.tallogre.hanbaobao.MainApplication;
import com.tallogre.hanbaobao.R;
import com.tallogre.hanbaobao.Translator.ExternalTranslators;
import com.tallogre.hanbaobao.Translator.Transliterator;

public class Globals {
    private static Globals instance = new Globals();
    private Context context;
    private Transliterator transliterator;
    private DictionaryDatabase dictionaryDatabase;
    private UserDatabase userDatabase;
    private ExternalTranslators externalTranslators;
    private UserPreferences userPreferences;
    private MainApplication application;
    private Bundle savedInstanceState;
    private ToneColors toneColors;

    private static class ToneColors {
        public int[] tones = new int[5];

        public ToneColors(Context context) {
            tones[0] = ContextCompat.getColor(context, R.color.tone1);
            tones[1] = ContextCompat.getColor(context, R.color.tone2);
            tones[2] = ContextCompat.getColor(context, R.color.tone3);
            tones[3] = ContextCompat.getColor(context, R.color.tone4);
            tones[4] = ContextCompat.getColor(context, R.color.tone5);
        }
    }

    private Globals() {
    }

    public synchronized static void initialize(MainApplication application) {
        if (instance.context == null) instance.context = instance.application = application;
    }

    public static MainApplication getApplication() {
        return instance.application;
    }

    public static Transliterator getTransliterator() {
        if (instance.transliterator == null)
            instance.transliterator = new Transliterator(getDictionary());
        return instance.transliterator;
    }

    public static DictionaryDatabase getDictionary() {
        if (instance.dictionaryDatabase == null)
            instance.dictionaryDatabase = new DictionaryDatabase(instance.context);
        return instance.dictionaryDatabase;
    }

    public static UserDatabase getUserDatabase() {
        if (instance.userDatabase == null)
            instance.userDatabase = new UserDatabase(instance.context);
        return instance.userDatabase;
    }

    public static ExternalTranslators getExternalTranslators() {
        if (instance.externalTranslators == null) {
            instance.externalTranslators = new ExternalTranslators(instance.context, getUserPreferences());
        }
        return instance.externalTranslators;
    }

    public static UserPreferences getUserPreferences() {
        if (instance.userPreferences == null)
            instance.userPreferences = new UserPreferences(instance.context);
        return instance.userPreferences;
    }

    public static void setSavedInstanceState(Bundle savedInstanceState) {
        instance.savedInstanceState = savedInstanceState;
    }

    public static Bundle getSavedInstanceState() {
        return instance.savedInstanceState;
    }


    public static int getToneColor(int toneNumber) {
        if (instance.toneColors == null) instance.toneColors = new ToneColors(instance.context);
        if (toneNumber < 1 || toneNumber > 5) toneNumber = 5;
        return instance.toneColors.tones[toneNumber-1];
    }
}
