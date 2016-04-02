package com.tallogre.hanbaobao.Translator;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

import rx.functions.Action1;

public class TermClickableSpan extends ClickableSpan {
    private String term;
    private int endPosition;
    private int startPosition;
    private Action1<String> onClickAction;

    public TermClickableSpan(String term, int startPosition, int endPosition, Action1<String> onClick) {
        this.term = term;
        this.endPosition = endPosition;
        this.startPosition = startPosition;
        this.onClickAction = onClick;
    }

    public void addToSpannableString(SpannableString string) {
        string.setSpan(this, startPosition, endPosition, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    @Override
    public void onClick(View widget) {
        onClickAction.call(term);
    }

    @Override
    public void updateDrawState(TextPaint ds) {
    }
}
