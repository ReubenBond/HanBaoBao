package com.tallogre.hanbaobao.Database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.LruCache;

import com.tallogre.hanbaobao.Models.DictionaryEntry;
import com.tallogre.hanbaobao.Utilities.CharacterUtil;
import com.tallogre.hanbaobao.Utilities.SQLiteAssetHelper;

import java.util.ArrayList;
import java.util.List;

public class DictionaryDatabase extends SQLiteAssetHelper {

    private static final String DATABASE_NAME = "hanbaobao";
    private static final String TAG = "DictionaryDatabase";

    private static final String ordering = "hsk_level not null desc, hsk_level asc, part_of_speech not null desc, frequency desc";

    public DictionaryDatabase(Context context) {
        super(context, DATABASE_NAME, null);
    }

    private LruCache<CharSequence, List<DictionaryEntry>> headwordCache = new LruCache<>(256);

    public List<DictionaryEntry> findInHeadword(CharSequence key) {
        List<DictionaryEntry> result = headwordCache.get(key);
        if (result == null) {
            headwordCache.put(key, result = getDictionaryEntries(queryHeadword(key)));
        }
        return result;
    }

   /* public List<DictionaryEntry> findInDefinition(CharSequence key) {
        return getDictionaryEntries(queryDefinition(key));
    }

    public List<DictionaryEntry> findAnywhere(CharSequence key) {
        List<DictionaryEntry> results = findInHeadword(key);
        results.addAll(findInDefinition(key));
        return results;
    }

    public double getViterbiProbability(char key, char state) {
        Cursor cursor = getWritableDatabase().rawQuery("select * from viterbi_states where term=?",
                                       new String[]{String.valueOf(key)});

        double result = 0.0;
        if (cursor.moveToFirst()) {
            result = cursor.getDouble(cursor.getColumnIndex(String.valueOf(state)));
        }
        cursor.close();
        return result;
    }*/

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 1) {
            Log.i(TAG, "Upgrading database. This can take a few minutes.");

            // English text indices.
            //db.execSQL("create virtual table fts_definition using fts4 (content='dictionary', definition);");
            //db.execSQL("insert into fts_definition(rowid, definition) select rowid, definition from dictionary;");
            //db.execSQL("insert into fts_definition(fts_definition) values ('optimize');");

            // Simplified character indicies.
            db.execSQL("create index idx_simplified on dictionary(simplified);");
            db.execSQL("analyze idx_simplified;");
            //db.execSQL("create virtual table fts_prefix using fts4 (term, frequency);");
            //db.execSQL("insert into fts_prefix select distinct(simplified), frequency from dictionary where simplified is not null;");
            ////db.execSQL("insert into fts_prefix select distinct(traditional), frequency from dictionary where traditional is not null and not exists (SELECT 1 FROM fts_prefix where fts_prefix match traditional);");
            //db.execSQL("insert into fts_prefix (fts_prefix) values('optimize');");

            // Traditional character indices.
            db.execSQL("create index idx_traditional on dictionary(traditional);");
            db.execSQL("analyze idx_traditional;");
            //db.execSQL("vacuum");
        }

        super.onUpgrade(db, oldVersion, newVersion);
        Log.i(TAG, "Upgrade complete.");
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
    }

    public Cursor queryAnywhere(CharSequence key) {
        if (CharacterUtil.isProbablyChinese(key)) {
            return queryHeadword(key);
        }

        return queryDefinition(key);
    }

    public Cursor queryHeadword(CharSequence key) {
        SQLiteDatabase db = getWritableDatabase();
        return db.rawQuery(
                "select * from dictionary where simplified=? or traditional=? order by " + ordering,
                new String[]{key.toString(), key.toString()});
    }
    public Cursor queryDefinition(CharSequence key) {
        return getWritableDatabase().rawQuery(
                "select * from dictionary where rowid in (select rowid from fts_definition where fts_definition " +
                        "match ?) order by " + ordering,
                new String[]{key.toString() + "*"});
    }

    public Cursor queryPrefix(CharSequence key) {
        return getWritableDatabase().rawQuery("select term, frequency, pinyin from fts_prefix where fts_prefix match ?", new String[]{key.toString() + '*'});
    }

    private List<DictionaryEntry> getDictionaryEntries(Cursor cursor) {
        List<DictionaryEntry> results = new ArrayList<>();
        try {
            if (cursor.moveToFirst()) {
                do {
                    results.add(DictionaryEntry.getFromCursor(cursor));
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
}
