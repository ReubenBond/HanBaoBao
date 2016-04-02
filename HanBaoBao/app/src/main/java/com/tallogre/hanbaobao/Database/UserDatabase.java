package com.tallogre.hanbaobao.Database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.tallogre.hanbaobao.Models.DictionaryEntry;
import com.tallogre.hanbaobao.Models.HistoryItem;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.subjects.PublishSubject;

public class UserDatabase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "user";
    private static final int VERSION = 2;
    private static final String TAG = "UserDatabase";
    private final PublishSubject<String> historyChanges = PublishSubject.create();

    public UserDatabase(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        onUpgrade(db, db.getVersion(), VERSION);
    }

    public Observable<String> getHistoryChanges() {
        return historyChanges;
    }

    public void addToHistory(String phrase) {
        if (phrase == null || phrase.length() == 0) return;
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("insert into history (time, phrase) values(?, ?)", new Object[]{System.currentTimeMillis(), phrase});
        historyChanges.onNext(phrase);
    }

    public List<HistoryItem> getHistory(long dateTime, long maxEntries) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery(
                "select * from history where time < ? order by time limit ?",
                new String[]{String.valueOf(dateTime), String.valueOf(maxEntries)});
        List<HistoryItem> results = new ArrayList<>();
        try {
            if (cursor.moveToFirst()) {
                int timeColId = cursor.getColumnIndex("time");
                int phraseColId = cursor.getColumnIndex("phrase");
                do {
                    HistoryItem entry = new HistoryItem(cursor.getLong(timeColId), cursor.getString(phraseColId));
                    if (entry.phrase == null || entry.phrase.length() == 0) continue;
                    results.add(entry);
                } while (cursor.moveToNext());
            }
            return results;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw throwable;
        } finally {
            if (cursor != null && !cursor.isClosed()) cursor.close();
        }
    }

    public void addQuery(CharSequence query) {
        if (query == null || query.length() == 0) return;
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("insert into query_history (time, query) values(?, ?)", new Object[]{System.currentTimeMillis(), query});
    }

    public String getLatestQuery() {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery("select query from query_history order by time desc limit 1", null);

        try {
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndex("query"));
            }

            return null;
        } finally {
            cursor.close();
        }
    }

    public void addStar(String phrase) {
        if (phrase == null || phrase.length() == 0) return;
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("insert into stars (time, phrase) values(?, ?)", new Object[]{System.currentTimeMillis(), phrase});
    }

    public boolean isStarred(String phrase) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery(
                "select time from stars where phrase = ?",
                new String[]{phrase});
        boolean result = cursor.moveToFirst();
        cursor.close();
        return result;
    }

    public void removeStar(String phrase) {
        if (phrase == null || phrase.length() == 0) return;
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("insert into stars (time, phrase) values(?, ?)", new Object[]{System.currentTimeMillis(), phrase});
    }

    public List<HistoryItem> getStars(long offset, long maxEntries) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery(
                "select * from stars where order by time limit ? offset ?",
                new String[]{String.valueOf(maxEntries), String.valueOf(offset)});
        List<HistoryItem> results = new ArrayList<>();
        try {
            if (cursor.moveToFirst()) {
                int timeColId = cursor.getColumnIndex("time");
                int phraseColId = cursor.getColumnIndex("phrase");
                do {
                    HistoryItem entry = new HistoryItem(cursor.getLong(timeColId), cursor.getString(phraseColId));
                    if (entry.phrase == null || entry.phrase.length() == 0) continue;
                    results.add(entry);
                } while (cursor.moveToNext());
            }
            return results;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw throwable;
        } finally {
            if (cursor != null && !cursor.isClosed()) cursor.close();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 1) {
            Log.i(TAG, "Upgrading database. This can take a few minutes.");

            // Starred words
            db.execSQL("create table stars(time INTEGER, phrase TEXT PRIMARY KEY)");
            db.execSQL("create index idx_stars on stars(time)");

            // History
            db.execSQL("create table history(time INTEGER PRIMARY KEY, phrase TEXT)");
        }

        if (oldVersion < 2) {
            db.execSQL("create table query_history(time INTEGER PRIMARY KEY, query TEXT)");
        }

        db.setVersion(newVersion);
        Log.i(TAG, "Upgrade complete.");
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
    }
}
