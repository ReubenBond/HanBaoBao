package com.tallogre.hanbaobao.Translator;

import com.tallogre.hanbaobao.Database.DictionaryDatabase;
import com.tallogre.hanbaobao.Models.DictionaryEntry;
import com.tallogre.hanbaobao.Models.Phrase;
import com.tallogre.hanbaobao.Models.Term;
import com.tallogre.hanbaobao.Segmenter.SegmentationDictionary;
import com.tallogre.hanbaobao.Segmenter.TextSegmenter;
import com.tallogre.hanbaobao.Utilities.CharacterUtil;
import com.tallogre.hanbaobao.Utilities.DebugUtil;
import com.tallogre.hanbaobao.Utilities.Globals;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class Transliterator {
    private final TextSegmenter segmenter;

    public Transliterator(DictionaryDatabase dictionary) {
        this.segmenter = new TextSegmenter(new SegmentationDictionary(dictionary));
    }

    public Observable<Phrase> initialize() {
        return transliterateAsync("鲁班");
    }

/*    public Observable<List<DictionaryEntry>> lookupTerm(final CharSequence term) {
        return Observable.create(new Observable.OnSubscribe<List<DictionaryEntry>>() {
            @Override
            public void call(Subscriber<? super List<DictionaryEntry>> subscriber) {
                try {
                    subscriber.onNext(Globals.getDictionary().findInHeadword(term));
                    subscriber.onCompleted();
                } catch (Throwable throwable) {
                    subscriber.onError(throwable);
                }
            }
        }).subscribeOn(Schedulers.computation());
    }*/

    public Observable<Phrase> transliterateAsync(final CharSequence input) {
        return transliterateAsync(new Phrase(input));
    }

    public Observable<Phrase> transliterateAsync(final Phrase input) {
        return Observable.create(new Observable.OnSubscribe<Phrase>() {
            @Override
            public void call(Subscriber<? super Phrase> subscriber) {
                try {
                    transliterate(input);
                    input.lookupAll();
                    subscriber.onNext(input);
                    subscriber.onCompleted();
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                    subscriber.onError(throwable);
                }
            }
        }).subscribeOn(Schedulers.computation());
    }

    public void transliterate(Phrase phrase) {
        segmenter.segmentText(phrase);
    }
}