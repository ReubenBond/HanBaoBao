package com.tallogre.hanbaobao.Utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class SQLiteAssetHelper extends SQLiteOpenHelper {

    private static final String TAG = SQLiteAssetHelper.class.getSimpleName();
    private static final String ASSET_DB_PATH = "databases";

    private final Context context;
    private final String databaseName;
    private final CursorFactory factory;

    private SQLiteDatabase database = null;
    private boolean isInitializing = false;

    private String databasePath;

    /**
     * Create a helper object to create, open, and/or manage a database in
     * a specified location.
     * This method always returns very quickly.  The database is not actually
     * created or opened until one of {@link #getWritableDatabase} or
     * {@link #getReadableDatabase} is called.
     *
     * @param context          to use to open or create the database
     * @param name             of the database file
     * @param storageDirectory to store the database file upon creation; caller must
     *                         ensure that the specified absolute path is available and can be written to
     * @param factory          to use for creating cursor objects, or null for the default
     */
    public SQLiteAssetHelper(
            Context context,
            String name,
            String storageDirectory,
            CursorFactory factory) {
        super(context, name, factory, 1);
        if (name == null) throw new IllegalArgumentException("Database name cannot be null");

        this.context = context;
        databaseName = name;
        this.factory = factory;
        if (storageDirectory != null) {
            databasePath = storageDirectory;
        } else {
            databasePath = context.getApplicationInfo().dataDir + "/databases";
        }
    }

    /**
     * Create a helper object to create, open, and/or manage a database in
     * the application's default private data directory.
     * This method always returns very quickly.  The database is not actually
     * created or opened until one of {@link #getWritableDatabase} or
     * {@link #getReadableDatabase} is called.
     *
     * @param context to use to open or create the database
     * @param name    of the database file
     * @param factory to use for creating cursor objects, or null for the default
     */
    public SQLiteAssetHelper(Context context, String name, CursorFactory factory) {
        this(context, name, null, factory);
    }

    /**
     * Create and/or open a database that will be used for reading and writing.
     * The first time this is called, the database will be extracted and copied
     * from the application's assets directory.
     * <p/>
     * <p>Once opened successfully, the database is cached, so you can
     * call this method every time you need to write to the database.
     * (Make sure to call {@link #close} when you no longer need the database.)
     * Errors such as bad permissions or a full disk may cause this method
     * to fail, but future attempts may succeed if the problem is fixed.</p>
     * <p/>
     * <p class="caution">Database upgrade may take a long time, you
     * should not call this method from the application main thread, including
     * from {@link android.content.ContentProvider#onCreate ContentProvider.onCreate()}.
     *
     * @return a read/write database object valid until {@link #close} is called
     * @throws SQLiteException if the database cannot be opened for writing
     */
    @Override
    public synchronized SQLiteDatabase getWritableDatabase() {
        if (database != null && database.isOpen() && !database.isReadOnly()) {
            return database;  // The database is already open for business
        }

        if (isInitializing) {
            throw new IllegalStateException("getWritableDatabase called recursively");
        }

        // If we have a read-only database open, someone could be using it
        // (though they shouldn't), which would cause a lock to be held on
        // the file, and our attempts to open the database read-write would
        // fail waiting for the file lock.  To prevent that, we acquire the
        // lock on the read-only database, which shuts out other users.

        boolean success = false;
        SQLiteDatabase db = null;
        try {
            isInitializing = true;

            // Get all databases in the assets directory with the right name.
            ArrayList<String> files = new ArrayList<>();
            for (String file : context.getAssets().list(ASSET_DB_PATH)) {
                if (file.startsWith(databaseName)) {
                    files.add(file);
                }
            }

            if (files.size() == 0) {
                String msg = "Unable to find database file for database with name: " + databaseName + " in assets/" + ASSET_DB_PATH;
                Log.e(TAG, msg);
                throw new SQLiteAssetException(msg);
            }

            // Find the latest database version in the assets directory.
            Collections.sort(files, CharacterUtil.COMPARE_LENGTH_AND_ORDINAL);
            String latestDb = files.get(files.size() - 1);
            int verDot = latestDb.lastIndexOf('.');
            int assetDatabaseVersion = 0;
            if (verDot >= 0 && verDot != latestDb.length() - 1) {
                assetDatabaseVersion = Integer.parseInt(latestDb.substring(verDot + 1));
            }

            // Get the existing database version.
            SharedPreferences prefs = context.getSharedPreferences("db_versions", Context.MODE_PRIVATE);
            int existingDatabaseVersion = prefs.getInt("ver:" + databaseName, -1);

            // If the version of the database in the assets directory is newer than the version
            // in the data directory, or if the data directory contains no such database, copy the
            // database from the assets directory.
            boolean existingDatabaseExists = new File(databasePath + "/" + databaseName).exists();
            if (assetDatabaseVersion > existingDatabaseVersion || !existingDatabaseExists) {
                copyDatabaseFromAssets(latestDb);
            }

            db = openDatabase();
            if (db == null) throw new SQLiteAssetException("Unable to copy/open database!");

            if (!existingDatabaseExists) {
                onCreate(db);
                onUpgrade(db, existingDatabaseVersion, assetDatabaseVersion);
            } else if (assetDatabaseVersion > existingDatabaseVersion) {
                onUpgrade(db, existingDatabaseVersion, assetDatabaseVersion);
            }

            onOpen(db);
            prefs.edit().putInt("ver:" + databaseName, db.getVersion()).apply();
            success = true;
            return db;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            isInitializing = false;
            if (success) {
                if (database != null) {
                    try {
                        database.close();
                    } catch (Exception e) {
                    }
                }
                database = db;
            } else {
                if (db != null) db.close();
            }
        }
    }

    /**
     * Create and/or open a database.  This will be the same object returned by
     * {@link #getWritableDatabase} unless some problem, such as a full disk,
     * requires the database to be opened read-only.  In that case, a read-only
     * database object will be returned.  If the problem is fixed, a future call
     * to {@link #getWritableDatabase} may succeed, in which case the read-only
     * database object will be closed and the read/write object will be returned
     * in the future.
     * <p/>
     * <p class="caution">Like {@link #getWritableDatabase}, this method may
     * take a long time to return, so you should not call it from the
     * application main thread, including from
     * {@link android.content.ContentProvider#onCreate ContentProvider.onCreate()}.
     *
     * @return a database object valid until {@link #getWritableDatabase}
     * or {@link #close} is called.
     * @throws SQLiteException if the database cannot be opened
     */
    @Override
    public synchronized SQLiteDatabase getReadableDatabase() {
        if (database != null && database.isOpen()) {
            return database;  // The database is already open for business
        }

        if (isInitializing) {
            throw new IllegalStateException("getReadableDatabase called recursively");
        }

        try {
            return getWritableDatabase();
        } catch (SQLiteException e) {
            if (databaseName == null) throw e;  // Can't open a temp database read-only!
            Log.e(TAG, "Couldn't open " + databaseName + " for writing (will try read-only):", e);
        }

        SQLiteDatabase db = null;
        try {
            isInitializing = true;
            String path = context.getDatabasePath(databaseName).getPath();
            db = SQLiteDatabase.openDatabase(path, factory, SQLiteDatabase.OPEN_READONLY);

            onOpen(db);
            Log.w(TAG, "Opened " + databaseName + " in read-only mode");
            database = db;
            return database;
        } finally {
            isInitializing = false;
            if (db != null && db != database) db.close();
        }
    }

    /**
     * Close any open database object.
     */
    @Override
    public synchronized void close() {
        if (isInitializing) throw new IllegalStateException("Closed during initialization");

        if (database != null && database.isOpen()) {
            database.close();
            database = null;
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.setVersion(newVersion);
    }

    private SQLiteDatabase openDatabase() {
        try {
            SQLiteDatabase db = SQLiteDatabase.openDatabase(databasePath + "/" + databaseName, factory,
                    SQLiteDatabase.OPEN_READWRITE);
            Log.i(TAG, "successfully opened database " + databaseName);
            return db;
        } catch (SQLiteException e) {
            Log.w(TAG, "could not open database " + databaseName + " - " + e.getMessage());
            return null;
        }
    }


    private void copyDatabaseFromAssets(String latestDb) throws SQLiteAssetException {
        Log.w(TAG, "Copying database from assets...");

        String dest = databasePath + "/" + databaseName;
        InputStream is;
        boolean isZip;
        String assetPath = ASSET_DB_PATH + "/" + latestDb;
        try {
            is = context.getAssets().open(assetPath);
            isZip = assetPath.endsWith(".zip");
        } catch (IOException exception) {
            SQLiteAssetException newException = new SQLiteAssetException("Missing " + assetPath + " file (or .db, or .zip) in" +
                    " " +
                    "assets, or target " +
                    "directory not " +
                    "writable");
            newException.setStackTrace(exception.getStackTrace());
            throw newException;
        }

        try {
            File f = new File(databasePath + "/");
            if (!f.exists() && !f.mkdir()) {
                throw new SQLiteAssetException("Unable to create directory " + f.toString());
            }
            if (isZip) {
                ZipInputStream zis = getFileFromZip(is);
                if (zis == null) {
                    throw new SQLiteAssetException("Archive is missing a SQLite database file");
                }
                writeExtractedFileToDisk(zis, new FileOutputStream(dest));
            } else {
                writeExtractedFileToDisk(is, new FileOutputStream(dest));
            }

            Log.w(TAG, "database copy complete");

        } catch (IOException e) {
            SQLiteAssetException se = new SQLiteAssetException("Unable to write " + dest + " to data directory");
            se.setStackTrace(e.getStackTrace());
            throw se;
        }
    }

    /**
     * An exception that indicates there was an error with SQLite asset retrieval or parsing.
     */
    @SuppressWarnings("serial")
    public static class SQLiteAssetException extends SQLiteException {

        public SQLiteAssetException() {
        }

        public SQLiteAssetException(String error) {
            super(error);
        }
    }


    public static void writeExtractedFileToDisk(InputStream in, OutputStream outs) throws IOException {
        byte[] buffer = new byte[1024];
        int length;
        while ((length = in.read(buffer)) > 0) {
            outs.write(buffer, 0, length);
        }
        outs.flush();
        outs.close();
        in.close();
    }

    public static ZipInputStream getFileFromZip(InputStream zipFileStream) throws IOException {
        ZipInputStream zis = new ZipInputStream(zipFileStream);
        ZipEntry ze;
        while ((ze = zis.getNextEntry()) != null) {
            Log.w(TAG, "Extracting file: '" + ze.getName() + "'...");
            return zis;
        }
        return null;
    }

}
