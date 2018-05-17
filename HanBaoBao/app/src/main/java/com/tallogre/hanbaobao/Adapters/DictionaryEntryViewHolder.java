package com.tallogre.hanbaobao.Adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.PaintDrawable;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.tallogre.hanbaobao.Models.DictionaryEntry;
import com.tallogre.hanbaobao.R;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DictionaryEntryViewHolder extends RecyclerView.ViewHolder {

    private static PaintDrawable[] hskLevelBackgrounds;
    private final Context context;
    private final LayoutInflater inflater;
    private DictionaryEntry entry;

    private static PaintDrawable[] getHskLevelBackgrounds(Context context) {
        if (hskLevelBackgrounds == null) {
            String[] colors = {"#4a90e2", "#4fc7ac", "#7abc32", "#ffb218", "#ff7d25", "#d0021b"};
            hskLevelBackgrounds = new PaintDrawable[6];
            float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2,
                    context.getResources().getDisplayMetrics());
            Rect p = new Rect(2, 2, 2, 2);
            for (int i = 0; i < hskLevelBackgrounds.length; i++) {
                hskLevelBackgrounds[i] = new PaintDrawable(Color.parseColor(colors[i]));
                hskLevelBackgrounds[i].setCornerRadius(px);
                hskLevelBackgrounds[i].getPadding(p);
            }
        }
        return hskLevelBackgrounds;
    }

    private static PaintDrawable getColorBackground(Context context, String color) {
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2,
                context.getResources().getDisplayMetrics());
        Rect p = new Rect(2, 2, 2, 2);
        PaintDrawable result = new PaintDrawable(Color.parseColor(color));
        result.setCornerRadius(px);
        result.getPadding(p);
        return result;
    }

    @BindView(R.id.definition)
    public TextView definition;

    @BindView(R.id.transliteration)
    public TextView transliteration;

    @BindView(R.id.original)
    public TextView original;

    @BindView(R.id.tags)
    public ViewGroup tags;

    @BindView(R.id.replace)
    public ImageView replace;

    public DictionaryEntryViewHolder(View itemView, boolean textIsSelectable, LayoutInflater inflater, boolean selectMode) {
        super(itemView);
        ButterKnife.bind(this, itemView);
        definition.setTextIsSelectable(textIsSelectable);
        transliteration.setTextIsSelectable(textIsSelectable);
        original.setTextIsSelectable(textIsSelectable);
        context = itemView.getContext();
        this.inflater = inflater;
        if (selectMode) {
            replace.setVisibility(View.VISIBLE);
        }else {
            replace.setVisibility(View.GONE);
        }
    }

    TextView getTagView(int position) {
        if (position >= tags.getChildCount()) {
            TextView view = (TextView)inflater.inflate(R.layout.view_tag, tags, false);
            tags.addView(view);
            return view;
        }

        return (TextView)tags.getChildAt(position);
    }

    public void apply(DictionaryEntry entry) {
        this.entry = entry;
        definition.setText(entry.getDefinition(), TextView.BufferType.SPANNABLE);
        setTerm(entry.getHeadword());
        transliteration.setText(entry.getTransliteration(), TextView.BufferType.SPANNABLE);

        int addedChildren = 0;

        if (entry.hskLevel > 0 && entry.hskLevel <= 6) {
            TextView view = getTagView(addedChildren++);
            view.setText(String.format("HSK%d", entry.hskLevel));
            view.setBackground(getHskLevelBackgrounds(context)[entry.hskLevel - 1]);
        }

        List<String> pos = entry.getPartsOfSpeech();
        if (pos != null && pos.size() > 0) {
            for (int i = 0; i < pos.size(); i++) {
                TextView view = getTagView(addedChildren++);
                view.setText(pos.get(i));
                view.setBackground(getColorBackground(context, "#9013fe"));
            }
        }

        if (entry.concept != null) {
            TextView view = getTagView(addedChildren++);
            view.setText(entry.concept);
            view.setBackground(getColorBackground(context, "#2e7d32"));
        }

        if (entry.topic != null) {
            TextView view = getTagView(addedChildren++);
            view.setText(entry.topic);
            view.setBackground(getColorBackground(context, "#e65100"));
        }

        if (entry.parentTopic != null) {
            TextView view = getTagView(addedChildren++);
            view.setText(entry.parentTopic);
            view.setBackground(getColorBackground(context, "#ad1457"));
        }

        while(tags.getChildCount() > addedChildren) tags.removeViewAt(tags.getChildCount()-1);

        // tags.requestLayout();
    }

    private void setTerm(CharSequence value) {
        original.setText(value);
    }

    public DictionaryEntry getEntry() {
        return entry;
    }
}