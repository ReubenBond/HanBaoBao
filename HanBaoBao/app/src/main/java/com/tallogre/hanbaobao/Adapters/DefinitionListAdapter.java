package com.tallogre.hanbaobao.Adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tallogre.hanbaobao.Models.DictionaryEntry;
import com.tallogre.hanbaobao.R;

import java.util.List;

public class DefinitionListAdapter extends RecyclerView.Adapter<DictionaryEntryViewHolder> {
    private final boolean textIsSelectable;

    // Whether or not tapping this text will trigger text replacement.
    private final boolean selectMode;
    private final List<DictionaryEntry> entries;
    private final LayoutInflater inflater;
    private final View.OnClickListener onClickListener;

    public DefinitionListAdapter(boolean textIsSelectable, List<DictionaryEntry> entries, LayoutInflater inflater, View.OnClickListener onClickListener, boolean selectMode) {
        this.textIsSelectable = textIsSelectable;
        this.entries = entries;
        this.inflater = inflater;
        this.onClickListener = onClickListener;
        this.selectMode = selectMode;
    }

    @Override
    public DictionaryEntryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.view_dictionary_entry, parent, false);
        view.setOnClickListener(onClickListener);

        DictionaryEntryViewHolder holder = new DictionaryEntryViewHolder(view, this.textIsSelectable, inflater, selectMode);
        view.setTag(holder);
        return holder;
    }

    @Override
    public void onBindViewHolder(DictionaryEntryViewHolder holder, int position) {
        holder.apply(entries.get(position));
    }

    @Override
    public int getItemCount() {
        return (entries == null) ? 0 : entries.size();
    }
}
