package com.tallogre.hanbaobao.Fragments;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tallogre.hanbaobao.Database.UserDatabase;
import com.tallogre.hanbaobao.Models.HistoryItem;
import com.tallogre.hanbaobao.R;
import com.tallogre.hanbaobao.Utilities.Globals;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class HistoryFragment extends Fragment {
    final PublishSubject<CharSequence> query = PublishSubject.create();

    @BindView(R.id.phraseInput)
    TextView phraseInput;

    @BindView(R.id.historyEntries)
    RecyclerView historyView;

    @BindView(R.id.resultsPlaceholder)
    View resultsPlaceholder;

    private UserDatabase userDb;
    private List<HistoryItem> entries;
    private HistoryEntryAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.fragment_history, container, false);
        ButterKnife.bind(this, result);
        userDb = Globals.getUserDatabase();

        // Subscribe to search term changes.
        phraseInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                query.onNext(phraseInput.getText());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        historyView.setAdapter(adapter = new HistoryEntryAdapter());
        historyView.setLayoutManager(new LinearLayoutManager(getContext()));

        query.map(new Func1<CharSequence, List<HistoryItem>>() {
            @Override
            public List<HistoryItem> call(CharSequence key) {
                return userDb.getHistory(System.currentTimeMillis(), 100);
                // TODO: Search
            }
        }).subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<List<HistoryItem>>() {
            @Override
            public void call(List<HistoryItem> items) {
                updateResultsView(items);
            }
        });

        userDb.getHistoryChanges().subscribeOn(Schedulers.computation()).subscribe(new Action1<String>() {
            @Override
            public void call(String s) {
                // Re-evaluate the query.
                query.onNext(phraseInput.getText());
            }
        });

        return result;
    }

    @Override
    public void onResume() {
        super.onResume();
        query.onNext(phraseInput.getText());
    }

    private void updateResultsView(List<HistoryItem> items) {
        if (items == null || items.size() == 0) {
            resultsPlaceholder.setVisibility(View.VISIBLE);
            historyView.setVisibility(View.GONE);
        } else {
            resultsPlaceholder.setVisibility(View.GONE);
            historyView.setVisibility(View.VISIBLE);
        }

        entries = items;
        adapter.notifyDataSetChanged();
    }

    private class HistoryEntryAdapter extends RecyclerView.Adapter<HistoryEntryViewHolder> {
        @Override
        public HistoryEntryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (entries == null) return null;
            LayoutInflater inflater = LayoutInflater.from(HistoryFragment.this.getContext());
            return new HistoryEntryViewHolder(inflater.inflate(R.layout.view_history_entry, parent, false));
        }

        @Override
        public void onBindViewHolder(HistoryEntryViewHolder holder, int position) {
            holder.apply(entries.get(position), position);
        }

        @Override
        public int getItemCount() {
            return entries == null ? 0 : entries.size();
        }
    }

    public static class HistoryEntryViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.phrase)
        TextView phrase;

        public HistoryEntryViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void apply(HistoryItem item, int position) {
            phrase.setText(item.phrase);
        }
    }
}
