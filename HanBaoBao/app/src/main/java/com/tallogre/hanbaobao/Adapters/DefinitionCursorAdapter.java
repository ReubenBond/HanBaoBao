package com.tallogre.hanbaobao.Adapters;

import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tallogre.hanbaobao.DictionaryEntriesControl;
import com.tallogre.hanbaobao.Models.DictionaryEntry;
import com.tallogre.hanbaobao.R;
import com.tallogre.hanbaobao.Utilities.CursorAdapter.CursorRecyclerAdapter;

public class DefinitionCursorAdapter extends CursorRecyclerAdapter<DictionaryEntryViewHolder> {
    private boolean textIsSelectable;
    private final View.OnClickListener onClickListener;
    private final boolean selectMode;

    public DefinitionCursorAdapter(Cursor cursor, boolean textIsSelectable, View.OnClickListener onClickListener, boolean selectMode) {
        super(cursor);
        this.textIsSelectable = textIsSelectable;
        this.onClickListener = onClickListener;
        this.selectMode = selectMode;
    }

    @Override
    public void onBindViewHolderCursor(DictionaryEntryViewHolder holder, Cursor cursor) {
        DictionaryEntry entry = DictionaryEntry.getFromCursor(cursor);
        holder.apply(entry);
    }

    @Override
    public DictionaryEntryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.view_dictionary_entry, parent, false);

        DictionaryEntryViewHolder holder = new DictionaryEntryViewHolder(view, this.textIsSelectable, inflater, selectMode);
        view.setOnClickListener(onClickListener);
        view.setTag(holder);
        return holder;
    }
}
