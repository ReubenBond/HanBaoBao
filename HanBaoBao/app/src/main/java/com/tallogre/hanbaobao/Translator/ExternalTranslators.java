package com.tallogre.hanbaobao.Translator;

import android.content.ClipDescription;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import com.tallogre.hanbaobao.R;
import com.tallogre.hanbaobao.Utilities.UserPreferences;

import java.util.ArrayList;
import java.util.List;

public class ExternalTranslators implements UserPreferences.OnChangeListener {
    public static final String PACKAGE_GOOGLE_TRANSLATE = "com.google.android.apps.translate";
    public static final String COM_MICROSOFT_TRANSLATOR = "com.microsoft.translator";
    public static final String PACKAGE_PLECO = "com.pleco.chinesesystem";
    public static final ExternalTranslator NONE = new ExternalTranslator("None", 0, "", "") {
        @Override
        public void launch(String original) {
        }
    };
    private final List<ExternalTranslator> allTranslators;
    private ExternalTranslator selectedTranslator;
    private final Context context;
    private final UserPreferences userPreferences;

    public ExternalTranslators(final Context context, UserPreferences userPreferences) {
        this.context = context;
        this.userPreferences = userPreferences;
        this.userPreferences.addOnChangeListener(this);
        this.allTranslators = new ArrayList<>();


        allTranslators.add(NONE);

        addTranslator(new ExternalTranslator("Google", R.mipmap.ic_google_translate, PACKAGE_GOOGLE_TRANSLATE,
                ".TranslateActivity") {
            @Override
            public void launch(String original) {
                try {
                    launchPopup(original);
                } catch (Throwable e) {
                    launchApp(original);
                }
            }

            private void launchPopup(String original) {
                Intent i = new Intent();
                i.setAction(Intent.ACTION_PROCESS_TEXT);
                i.addCategory(Intent.CATEGORY_DEFAULT);
                i.setType(ClipDescription.MIMETYPE_TEXT_PLAIN);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.setComponent(new ComponentName(PACKAGE_GOOGLE_TRANSLATE,
                        PACKAGE_GOOGLE_TRANSLATE + ".copydrop.CopyDropActivity"));
                i.putExtra(Intent.EXTRA_PROCESS_TEXT, original);
                i.putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true);
                context.startActivity(i);
            }

            private void launchApp(String original) {
                Intent i = new Intent();
                i.setAction(Intent.ACTION_SEND);
                i.addCategory(Intent.CATEGORY_DEFAULT);
                i.setType(ClipDescription.MIMETYPE_TEXT_PLAIN);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.setComponent(new ComponentName(PACKAGE_GOOGLE_TRANSLATE,
                        PACKAGE_GOOGLE_TRANSLATE + ".TranslateActivity"));
                i.putExtra(Intent.EXTRA_TEXT, original);
                context.startActivity(i);
            }
        });

        addTranslator(new ExternalTranslator("Microsoft", R.mipmap.ic_launcher_translator, COM_MICROSOFT_TRANSLATOR,
                ".activity.translate.InAppTranslationActivity") {
            @Override
            public void launch(String original) {
                Intent i = new Intent();
                i.setAction(Intent.ACTION_PROCESS_TEXT);
                i.addCategory(Intent.CATEGORY_DEFAULT);
                i.setType(ClipDescription.MIMETYPE_TEXT_PLAIN);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.setComponent(new ComponentName(packageName, packageName + activityName));
                i.putExtra(Intent.EXTRA_PROCESS_TEXT, original);
                i.putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true);
                context.startActivity(i);
            }
        });

        addTranslator(new ExternalTranslator("Pleco Document Reader", R.drawable.ic_pleco_reader, PACKAGE_PLECO,
                ".PlecoDocumentReaderActivity") {

            @Override
            public void launch(String original) {
                Intent i = new Intent();
                i.setAction(Intent.ACTION_SEND);
                i.addCategory(Intent.CATEGORY_DEFAULT);
                i.setType(ClipDescription.MIMETYPE_TEXT_PLAIN);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.setComponent(new ComponentName(packageName, packageName + activityName));
                i.putExtra(Intent.EXTRA_TEXT, original);
                context.startActivity(i);
            }
        });
        addTranslator(new ExternalTranslator("Pleco Dictionary", R.drawable.ic_pleco_dict, PACKAGE_PLECO, ".PlecoDictLauncherActivity") {

            @Override
            public void launch(String original) {
                Intent i = new Intent();
                i.setAction(Intent.ACTION_SEND);
                i.addCategory(Intent.CATEGORY_DEFAULT);
                i.setType(ClipDescription.MIMETYPE_TEXT_PLAIN);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.setComponent(new ComponentName(packageName, packageName + activityName));
                i.putExtra(Intent.EXTRA_TEXT, original);
                context.startActivity(i);
            }
        });

        updateSelectedTranslator();
        if (allTranslators.size() > 0 && selectedTranslator == null) {
            setSelectedTranslator(allTranslators.get(allTranslators.size() - 1));
        }
    }

    private void updateSelectedTranslator() {
        String selected = userPreferences.getExternalTranslator();
        if (selected != null) for (ExternalTranslator translator : allTranslators) {
            if (translator.equals(selected)) {
                selectedTranslator = translator;
                break;
            }
        }
    }

    public int getSelectedTranslatorPosition() {
        String selected = userPreferences.getExternalTranslator();
        if (selected != null)
            for (int i = 0; i < allTranslators.size(); i++) {
                ExternalTranslator t = allTranslators.get(i);

                if (t.equals(selected)) return i;
            }

        return -1;
    }

    private void addTranslator(ExternalTranslator translator) {
        if (isPackageInstalled(translator.packageName, context)) {
            allTranslators.add(translator);
        }
    }

    public void setSelectedTranslator(ExternalTranslator translator) {
        selectedTranslator = translator;
        userPreferences.setExternalTranslator(translator != null ? translator.packageName + translator.activityName : null);
    }

    private boolean isPackageInstalled(String packageName, Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public ExternalTranslator getSelectedTranslator() {
        return selectedTranslator;
    }

    public List<ExternalTranslator> getTranslators() {
        return this.allTranslators;
    }

    public void launchTranslator(CharSequence original) {
        if (original == null) return;
        if (selectedTranslator == null) {
            Log.e("ExternalTranslators", "No translator selected!");
        } else {
            selectedTranslator.launch(original.toString());
        }
    }

    @Override
    public void onPreferencesChanged() {
        updateSelectedTranslator();
    }

    public void setSelectedTranslator(int position) {
        setSelectedTranslator(getTranslators().get(position));
    }

    public static abstract class ExternalTranslator {
        public final String name;
        public final int icon;
        public final String packageName;
        public final String activityName;

        public boolean equals(String name) {
            return name != null && name.length() == packageName.length() + activityName.length() && name.startsWith(packageName) && name.endsWith(activityName);
        }

        public ExternalTranslator(String name, int icon, String packageName, String activityName) {
            this.name = name;
            this.icon = icon;
            this.packageName = packageName;
            this.activityName = activityName;
        }

        public abstract void launch(String original);
    }
}
