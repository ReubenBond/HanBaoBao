package com.tallogre.hanbaobao.Utilities;


import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class UserPreferences {
    private static final String TAG = "UserPreferences";
    private final SharedPreferences prefs;
    private final List<OnChangeListener> listeners;
    private boolean suppressChangeNotifications;
    private String lookupSymbol;
    private int cachedCharSet = -1;

    public boolean getIsAutoShowEnabled() {
        return prefs.getBoolean("isAutoShowEnabled", true);
    }

    public void setIsAutoShowEnabled(boolean value) {
        if (value != getIsAutoShowEnabled()) {
            prefs.edit().putBoolean("isAutoShowEnabled", value).apply();
            notifyOnChangeListeners();
        }}

    public boolean getShouldShowTutorial() {
        return prefs.getBoolean("shouldShowTutorial", true);
    }

    public void setShouldShowTutorial(boolean value) {
        if (value != getShouldShowTutorial()) {
            prefs.edit().putBoolean("shouldShowTutorial", value).apply();
            notifyOnChangeListeners();
        }
    }

    public String getLookupSymbol() {
        return prefs.getString("lookupSymbol", "#");
    }

    public void setLookupSymbol(String value) {
        if (!CharacterUtil.equals(value, getLookupSymbol())) {
            prefs.edit().putString("lookupSymbol", value).apply();
            notifyOnChangeListeners();
        }
    }

    public interface OnChangeListener {
        void onPreferencesChanged();
    }

    public UserPreferences(Context context) {
        prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        listeners = new ArrayList<>();
    }

    public void addOnChangeListener(OnChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeOnChangeListener(OnChangeListener listener) {
        listeners.remove(listener);
    }

    public void setSuppressChangeNotifications(boolean suppressChangeNotifications) {
        this.suppressChangeNotifications = suppressChangeNotifications;
    }

    public void notifyOnChangeListeners() {
        if (!suppressChangeNotifications) for (OnChangeListener listener : listeners) listener.onPreferencesChanged();
    }

    public boolean getIsTouchModeEnabled() {
        return prefs.getBoolean("isTouchModeEnabled", true);
    }

    public void setIsTouchModeEnabled(boolean value) {
        if (value != getIsTouchModeEnabled()) {
            prefs.edit().putBoolean("isTouchModeEnabled", value).apply();
            notifyOnChangeListeners();
        }
    }

    public boolean getIsClipboardModeEnabled() {
        return prefs.getBoolean("isClipboardModeEnabled", true);
    }

    public void setIsClipboardModeEnabled(boolean value) {
        if (value != getIsClipboardModeEnabled()) {
            prefs.edit().putBoolean("isClipboardModeEnabled", value).apply();
            notifyOnChangeListeners();
        }
    }

    public String getExternalTranslator() {
        return prefs.getString("externalTranslator", null);
    }

    public void setExternalTranslator(String value) {
        if (!equals(value, getExternalTranslator())) {
            prefs.edit().putString("externalTranslator", value).apply();
            notifyOnChangeListeners();
        }
    }

    // Users can decide whether or not to hide pinyin for words below a certain HSK level.
    public int getPinyinHskHideLevel() {
        return prefs.getInt("hskHideLevel", -1);
    }

    public static final int PINYIN_HIDE_ALL = 9001;

    public void setPinyinHskHideLevel(int value) {
        if (value != getPinyinHskHideLevel()) {
            prefs.edit().putInt("hskHideLevel", value).apply();
            notifyOnChangeListeners();
        }
    }

    public void setCharacterSet(int value) {
        cachedCharSet = value;
        prefs.edit().putInt("charSet", value).apply();
        notifyOnChangeListeners();
    }

    public int getCharacterSet() { return prefs.getInt("charSet", 0); }
    public int getCachedCharacterSet() {
        if (cachedCharSet == -1) {
            cachedCharSet = prefs.getInt("charSet", 0);
        }

        return cachedCharSet;
    }

    private static boolean equals(String str1, String str2) {
        return (str1 == null ? str2 == null : str1.equals(str2));
    }
}
