package com.tallogre.hanbaobao.Fragments;

import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tallogre.hanbaobao.Database.DictionaryDatabase;
import com.tallogre.hanbaobao.Database.UserDatabase;
import com.tallogre.hanbaobao.DictionaryEntriesControl;
import com.tallogre.hanbaobao.R;
import com.tallogre.hanbaobao.Utilities.CharacterUtil;
import com.tallogre.hanbaobao.Utilities.Globals;

import java.util.concurrent.TimeUnit;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class QuickDictionaryFragment extends Fragment {
    final PublishSubject<CharSequence> query = PublishSubject.create();

    @Bind(R.id.phraseInput)
    TextView phraseInput;

    @Bind(R.id.resultsPlaceholder)
    View resultsPlaceholder;

    DictionaryEntriesControl dictionaryEntriesControl;

    private DictionaryDatabase dictionary;
    private UserDatabase userDb;
    private CharSequence lastQuery;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.fragment_dictionary, container, false);
        ButterKnife.bind(this, result);
        dictionary = Globals.getDictionary();
        userDb = Globals.getUserDatabase();
        dictionaryEntriesControl = new DictionaryEntriesControl(result, true);

        // Subscribe to search term changes.
        phraseInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!CharacterUtil.equals(phraseInput.getText(), lastQuery)) {
                    query.onNext(phraseInput.getText());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        // Restore the last query from the saved state.
        lastQuery = null;
        if (savedInstanceState!=null)
        {
            lastQuery = savedInstanceState.getCharSequence("query");
        }

        Observable.just(lastQuery).concatWith(query).debounce(400, TimeUnit.MILLISECONDS).map(new Func1<CharSequence, Cursor>() {
            @Override
            public Cursor call(CharSequence key) {

                if (key == null) {
                    key = userDb.getLatestQuery();
                } else if (!CharacterUtil.equals(key, lastQuery)) {
                    // If the query is new, add it to history.
                    userDb.addQuery(key);
                }

                if (key == null) return null;

                lastQuery = key.toString();

                return dictionary.queryAnywhere(key);
            }
        }).subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Cursor>() {
            @Override
            public void call(Cursor cursor) {
                if (cursor == null) return;
                setResults(cursor);
            }
        });

        return result;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence("query", lastQuery);
    }

    private void setResults(Cursor cursor) {
        boolean hasResults = dictionaryEntriesControl.setCursor(cursor);
        if (!hasResults) {
            resultsPlaceholder.setVisibility(View.VISIBLE);
        } else {
            resultsPlaceholder.setVisibility(View.GONE);
        }

        CharSequence text = phraseInput.getText();
        if (!CharacterUtil.equals(text, lastQuery)) {
            phraseInput.setText(lastQuery);
        }
    }
}
