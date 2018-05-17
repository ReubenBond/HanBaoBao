package com.tallogre.hanbaobao;

import android.database.Cursor;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;

import com.tallogre.hanbaobao.Adapters.DefinitionCursorAdapter;
import com.tallogre.hanbaobao.Adapters.DefinitionListAdapter;
import com.tallogre.hanbaobao.Adapters.DictionaryEntryViewHolder;
import com.tallogre.hanbaobao.Models.DictionaryEntry;
import com.tallogre.hanbaobao.Models.Term;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Observable;
import rx.subjects.PublishSubject;

public class DictionaryEntriesControl implements View.OnClickListener {
    private final boolean textIsSelectable;
    private PublishSubject<DictionaryEntryViewHolder> onEntryClicked = PublishSubject.create();

    @BindView(R.id.dictionaryEntries)
    public RecyclerView definitionsList;

        private LayoutInflater inflater;
    private boolean selectMode;

    public DictionaryEntriesControl(View rootView, boolean textIsSelectable) {
        this.textIsSelectable = textIsSelectable;
        ButterKnife.bind(this, rootView);
        inflater = LayoutInflater.from(rootView.getContext());
        definitionsList.setLayoutManager(new LinearLayoutManager(rootView.getContext()));
    }

    public void setTerm(Term term) {
        List<DictionaryEntry> entries = null;
        if (term != null) entries = term.getDefinitions();
        definitionsList.setAdapter(new DefinitionListAdapter(textIsSelectable, entries, inflater, this, selectMode));
    }

    // Returns a value indicating whether or not the cursor has results.
    public boolean setCursor(Cursor cursor) {
        boolean result;
        if (cursor == null || !cursor.moveToFirst()) {
            result = false;
            definitionsList.setVisibility(View.GONE);
        } else {
            result = true;
            definitionsList.setVisibility(View.VISIBLE);
        }

        if (cursor != null) {
            definitionsList.setAdapter(new DefinitionCursorAdapter(cursor, textIsSelectable, this, selectMode));
        }

        return result;
    }

    public Observable<DictionaryEntryViewHolder> onEntryClicked() {
        return onEntryClicked;
    }

    @Override
    public void onClick(View v) {
        onEntryClicked.onNext((DictionaryEntryViewHolder)v.getTag());
    }

    public void setSelectionMode(boolean selectMode) {
        this.selectMode = selectMode;
    }
}
