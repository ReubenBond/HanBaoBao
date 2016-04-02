package com.tallogre.hanbaobao.Adapters;

import android.content.res.Resources;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.tallogre.hanbaobao.Models.Term;
import com.tallogre.hanbaobao.R;
import com.tallogre.hanbaobao.Utilities.Globals;
import com.tallogre.hanbaobao.Utilities.UserPreferences;

import butterknife.Bind;
import butterknife.ButterKnife;

public class TermViewHolder {
    @Bind(R.id.transliteration)
    public TextView transliterated;
    @Bind(R.id.original)
    public TextView original;
    @Bind(R.id.termBackground)
    public View termBackground;
    private Term term;
    private int index;

    public TermViewHolder(View rootView) {
        ButterKnife.bind(this, rootView);
    }

    public void apply(Term term, int index) {
        this.term = term;
        this.index  = index;
        int hskLevel = 0;
        if (term.getDefinitions() != null && term.getDefinitions().size() > 0) {
            hskLevel = term.getDefinitions().get(0).hskLevel;
        }

        int pinyinHskHideLevel = Globals.getUserPreferences().getPinyinHskHideLevel();
        boolean showTransliteration = pinyinHskHideLevel != UserPreferences.PINYIN_HIDE_ALL && (hskLevel < 1 || hskLevel > pinyinHskHideLevel);

        // For terms with no transliteration, hide the background. If the translation is merely
        // being hidden, then keep the background visible.
        if (term.getTransliterated() == null) {
            termBackground.setBackgroundResource(0);
        } else {
            termBackground.setBackgroundResource(isHighlighted ? R.drawable.term_bg_highlighted : R.drawable.term_bg);
        }

        if (term.getTransliterated() != null && showTransliteration) {
            transliterated.setVisibility(View.VISIBLE);
            transliterated.setText(term.getTransliterated(), TextView.BufferType.SPANNABLE);
        } else {
            transliterated.setText("");
            transliterated.setVisibility(View.GONE);
        }

        if (term.getOriginal() != null && term.getOriginal().length() > 0) {
            original.setText(term.getOriginal());
            original.setVisibility(View.VISIBLE);
        } else {
            original.setText("");
            original.setVisibility(View.GONE);
        }
    }

    private boolean isHighlighted;

    public int getIndex() {return index;}
    public Term getTerm() {
        return term;
    }

    public boolean isHighlighted() {
        return isHighlighted;
    }

    public void setHighlighted(boolean highlighted) {
        boolean changed = highlighted != isHighlighted;
        isHighlighted = highlighted;
        if (changed) apply(this.term, getIndex());
    }
}
