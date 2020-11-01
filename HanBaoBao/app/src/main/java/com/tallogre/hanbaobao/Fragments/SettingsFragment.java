package com.tallogre.hanbaobao.Fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.tallogre.hanbaobao.AcknowledgementsActivity;
import com.tallogre.hanbaobao.R;
import com.tallogre.hanbaobao.Translator.ExternalTranslators;
import com.tallogre.hanbaobao.TutorialActivity;
import com.tallogre.hanbaobao.Utilities.CharacterUtil;
import com.tallogre.hanbaobao.Utilities.Globals;
import com.tallogre.hanbaobao.Utilities.UserPreferences;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.OnItemSelected;
import butterknife.OnTextChanged;

public class SettingsFragment extends Fragment implements UserPreferences.OnChangeListener {

    private ArrayList<HskLevel> hskLevels;

    private ArrayList<CharacterSet> charSets;

    @BindView(R.id.clipboardModeOption)
    public CheckBox clipboardModeOption;
    @BindView(R.id.touchModeOption)
    public CheckBox touchModeOption;
    @BindView(R.id.autoShowOption)
    public CheckBox autoShowOption;
    @BindView(R.id.externalTranslatorSpinner)
    public Spinner externalTranslatorSpinner;
    @BindView(R.id.hskLevelSpinner)
    public Spinner hskLevelSpinner;

    @BindView(R.id.charSetSpinner)
    public Spinner charSetSpinner;

    @BindView(R.id.lookupSymbol)
    public EditText lookupSymbol;

    private ExternalTranslators externalTranslators;
    private UserPreferences userPreferences;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.fragment_settings, container, false);
        ButterKnife.bind(this, result);
        this.externalTranslators = Globals.getExternalTranslators();
        this.userPreferences = Globals.getUserPreferences();
        this.userPreferences.addOnChangeListener(this);
        externalTranslatorSpinner.setAdapter(new ExternalTranslatorSpinnerAdapter(getContext(),
                externalTranslators.getTranslators()));

        hskLevels = new ArrayList<>();
        hskLevels.add(new HskLevel(0, "Show all"));
        hskLevels.add(new HskLevel(1, "HSK 1"));
        hskLevels.add(new HskLevel(2, "HSK 2"));
        hskLevels.add(new HskLevel(3, "HSK 3"));
        hskLevels.add(new HskLevel(4, "HSK 4"));
        hskLevels.add(new HskLevel(5, "HSK 5"));
        hskLevels.add(new HskLevel(6, "HSK 6"));
        hskLevels.add(new HskLevel(UserPreferences.PINYIN_HIDE_ALL, "Hide all"));
        hskLevelSpinner.setAdapter(new HskLevelSpinnerAdapter(getContext(), hskLevels));

        charSets = new ArrayList<>();
        charSets.add(new CharacterSet(CharacterSet.SIMPLIFIED, "Simplified"));
        charSets.add(new CharacterSet(CharacterSet.TRADITIONAL, "Traditional"));
        charSets.add(new CharacterSet(CharacterSet.BOTH, "Both"));
        charSetSpinner.setAdapter(new CharacterSetSpinnerAdapter(getContext(), charSets));

        onPreferencesChanged();
        return result;
    }

    @Override
    public void onDestroy() {
        userPreferences.removeOnChangeListener(this);
        super.onDestroy();
    }

    @OnClick(R.id.accessibilitySettingsButton)
    public void onClickAccessibilitySettingsButton() {
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }

    @OnClick(R.id.acknowledgements)
    public void onClickAcknowledgements() {
        startActivity(new Intent(getContext(), AcknowledgementsActivity.class));
    }

    @OnClick(R.id.touchModeNoteText)
    public void onClickTouchModeNoteText() {
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }

    @OnCheckedChanged(R.id.touchModeOption)
    public void onTouchModeOptionChanged(boolean checked) {
        userPreferences.setIsTouchModeEnabled(checked);
    }

    @OnCheckedChanged(R.id.clipboardModeOption)
    public void onClipboardModeOptionChanged(boolean checked) {
        userPreferences.setIsClipboardModeEnabled(checked);
    }

    @OnCheckedChanged(R.id.autoShowOption)
    public void onAutoShowOptionChanged(boolean checked) {
        userPreferences.setIsAutoShowEnabled(checked);
    }

    @OnItemSelected(R.id.externalTranslatorSpinner)
    public void onExternalTranslatorSelected(int position) {
        externalTranslators.setSelectedTranslator(position);
    }

    @OnItemSelected(R.id.hskLevelSpinner)
    public void onHskLevelSelected(int position) {
        userPreferences.setPinyinHskHideLevel(hskLevels.get(position).level);
    }

    @OnItemSelected(R.id.charSetSpinner)
    public void onCharSetSelected(int position) {
        userPreferences.setCharacterSet(charSets.get(position).charSet);
    }

    @OnClick(R.id.help)
    public void onClickHelp() {
        startActivity(new Intent(getContext(), TutorialActivity.class));
    }

    @OnClick(R.id.feedback)
    public void onClickFeedback() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("mailto:reuben.bond@gmail.com?subject=HanBaoBao feedback"));
        startActivity(intent);
    }

    @OnTextChanged(R.id.lookupSymbol)
    public void onLookupSymbolChanged() {
        userPreferences.setLookupSymbol(lookupSymbol.getText().toString());
    }

    @Override
    public void onPreferencesChanged() {
        clipboardModeOption.setChecked(userPreferences.getIsClipboardModeEnabled());
        touchModeOption.setChecked(userPreferences.getIsTouchModeEnabled());
        autoShowOption.setChecked(userPreferences.getIsAutoShowEnabled());
        int position = externalTranslators.getSelectedTranslatorPosition();
        externalTranslatorSpinner.setSelection(position, false);

        // Get the selected HSK level.
        int hskLevelPosition = 0;
        int selectedHskLevel = userPreferences.getPinyinHskHideLevel();
        for (int i = 0; i < hskLevels.size(); i++) {
            if (hskLevels.get(i).level == selectedHskLevel) {
                hskLevelPosition = i;
                break;
            }
        }
        hskLevelSpinner.setSelection(hskLevelPosition, false);

        int characterSet = userPreferences.getCharacterSet();
        charSetSpinner.setSelection(characterSet, false);

        if (!CharacterUtil.equals(lookupSymbol.getText(), userPreferences.getLookupSymbol())) {
            lookupSymbol.setText(userPreferences.getLookupSymbol());
        }
    }

    public class ExternalTranslatorSpinnerAdapter extends ArrayAdapter<ExternalTranslators.ExternalTranslator> {

        public ExternalTranslatorSpinnerAdapter(
                Context context, List<ExternalTranslators.ExternalTranslator> objects) {
            super(context, R.layout.spinner_item_external_translator, objects);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getView(position, convertView, parent);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // If the view has not yet been inflated, inflate it and associate a view holder with it.
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.spinner_item_external_translator, parent, false);
                convertView.setTag(new ViewHolder(convertView));
            }

            // Apply the selected item to the view.
            ((ViewHolder) convertView.getTag()).apply(getItem(position));
            return convertView;
        }

        public class ViewHolder {
            private final View view;
            @BindView(R.id.icon)
            public ImageView icon;
            @BindView(R.id.name)
            public TextView name;

            public ViewHolder(View view) {
                this.view = view;
                ButterKnife.bind(this, view);
            }

            public void apply(ExternalTranslators.ExternalTranslator item) {
                if (item.icon != 0) {
                    Picasso.with(view.getContext()).load(item.icon).into(icon);
                    icon.setVisibility(View.VISIBLE);
                } else icon.setVisibility(View.INVISIBLE);
                name.setText(item.name);
            }
        }
    }

    public class HskLevel {
        public String label;
        public int level;

        public HskLevel(int level, String label) {
            this.level = level;
            this.label = label;
        }
    }

    public class HskLevelSpinnerAdapter extends ArrayAdapter<HskLevel> {

        public HskLevelSpinnerAdapter(
                Context context, List<HskLevel> objects) {
            super(context, R.layout.spinner_item_hsk_level, objects);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getView(position, convertView, parent);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // If the view has not yet been inflated, inflate it and associate a view holder with it.
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.spinner_item_hsk_level, parent, false);
                convertView.setTag(new ViewHolder(convertView));
            }

            // Apply the selected item to the view.
            ((ViewHolder) convertView.getTag()).apply(getItem(position));
            return convertView;
        }

        public class ViewHolder {
            @BindView(R.id.label)
            public TextView label;

            public ViewHolder(View view) {
                ButterKnife.bind(this, view);
            }

            public void apply(HskLevel item) {
                label.setText(item.label);
            }
        }
    }

    public class CharacterSet {
        public static final int SIMPLIFIED = 0;
        public static final int TRADITIONAL = 1;
        public static final int BOTH = 2;
        public String label;
        public int charSet;

        public CharacterSet(int charSet, String label) {
            this.charSet = charSet;
            this.label = label;
        }
    }

    public class CharacterSetSpinnerAdapter extends ArrayAdapter<CharacterSet> {

        public CharacterSetSpinnerAdapter(
                Context context, List<CharacterSet> objects) {
            super(context, R.layout.spinner_item_charset, objects);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getView(position, convertView, parent);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // If the view has not yet been inflated, inflate it and associate a view holder with it.
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.spinner_item_charset, parent, false);
                convertView.setTag(new ViewHolder(convertView));
            }

            // Apply the selected item to the view.
            ((ViewHolder) convertView.getTag()).apply(getItem(position));
            return convertView;
        }

        public class ViewHolder {
            @BindView(R.id.label)
            public TextView label;

            public ViewHolder(View view) {
                ButterKnife.bind(this, view);
            }

            public void apply(CharacterSet item) {
                label.setText(item.label);
            }
        }
    }
}
