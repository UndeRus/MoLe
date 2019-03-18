/*
 * Copyright © 2019 Damyan Ivanov.
 * This file is part of MoLe.
 * MoLe is free software: you can distribute it and/or modify it
 * under the term of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your opinion), any later version.
 *
 * MoLe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License terms for details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MoLe. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ktnx.mobileledger.utils;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.provider.FontsContract;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.FilterQueryProvider;
import android.widget.SimpleCursorAdapter;

import net.ktnx.mobileledger.async.DescriptionSelectedCallback;
import net.ktnx.mobileledger.model.Data;

import org.jetbrains.annotations.NonNls;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

import static net.ktnx.mobileledger.utils.MLDB.DatabaseMode.READ;
import static net.ktnx.mobileledger.utils.MLDB.DatabaseMode.WRITE;

public final class MLDB {
    public static final String ACCOUNTS_TABLE = "accounts";
    public static final String DESCRIPTION_HISTORY_TABLE = "description_history";
    public static final String OPT_LAST_SCRAPE = "last_scrape";
    @NonNls
    public static final String OPT_PROFILE_UUID = "profile_uuid";
    private static final String NO_PROFILE = "-";
    private static MobileLedgerDatabase helperForReading, helperForWriting;
    private static Application context;
    private static void checkState() {
        if (context == null)
            throw new IllegalStateException("First call init with a valid context");
    }
    public static synchronized SQLiteDatabase getDatabase(DatabaseMode mode) {
        checkState();

        SQLiteDatabase db;

        if (mode == READ) {
            if (helperForReading == null) helperForReading = new MobileLedgerDatabase(context);
            db = helperForReading.getReadableDatabase();
        }
        else {
            if (helperForWriting == null) helperForWriting = new MobileLedgerDatabase(context);
            db = helperForWriting.getWritableDatabase();
        }

        db.execSQL("pragma case_sensitive_like=ON;");
        return db;
    }
    public static SQLiteDatabase getReadableDatabase() {
        return getDatabase(READ);
    }
    public static SQLiteDatabase getWritableDatabase() {
        return getDatabase(WRITE);
    }
    static public int getIntOption(String name, int default_value) {
        String s = getOption(name, String.valueOf(default_value));
        try {
            return Integer.parseInt(s);
        }
        catch (Exception e) {
            Log.d("db", "returning default int value of " + name, e);
            return default_value;
        }
    }
    static public long getLongOption(String name, long default_value) {
        String s = getOption(name, String.valueOf(default_value));
        try {
            return Long.parseLong(s);
        }
        catch (Exception e) {
            Log.d("db", "returning default long value of " + name, e);
            return default_value;
        }
    }
    static public String getOption(String name, String default_value) {
        Log.d("db", "about to fetch option " + name);
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.rawQuery("select value from options where profile = ? and name=?",
                new String[]{NO_PROFILE, name}))
        {
            if (cursor.moveToFirst()) {
                String result = cursor.getString(0);

                if (result == null) result = default_value;

                Log.d("db", "option " + name + "=" + result);
                return result;
            }
            else return default_value;
        }
        catch (Exception e) {
            Log.d("db", "returning default value for " + name, e);
            return default_value;
        }
    }
    static public void setOption(String name, String value) {
        Log.d("option", String.format("%s := %s", name, value));
        SQLiteDatabase db = MLDB.getWritableDatabase();
        db.execSQL("insert or replace into options(profile, name, value) values(?, ?, ?);",
                new String[]{NO_PROFILE, name, value});
    }
    static public void setLongOption(String name, long value) {
        setOption(name, String.valueOf(value));
    }
    @TargetApi(Build.VERSION_CODES.N)
    public static void hookAutocompletionAdapter(final Context context,
                                                 final AutoCompleteTextView view,
                                                 final String table, final String field,
                                                 final boolean profileSpecific) {
        hookAutocompletionAdapter(context, view, table, field, profileSpecific, null, null);
    }
    @TargetApi(Build.VERSION_CODES.N)
    public static void hookAutocompletionAdapter(final Context context,
                                                 final AutoCompleteTextView view,
                                                 final String table, final String field,
                                                 final boolean profileSpecific,
                                                 final View nextView,
                                                 final DescriptionSelectedCallback callback) {
        String[] from = {field};
        int[] to = {android.R.id.text1};
        SimpleCursorAdapter adapter =
                new SimpleCursorAdapter(context, android.R.layout.simple_dropdown_item_1line, null,
                        from, to, 0);
        adapter.setStringConversionColumn(1);

        FilterQueryProvider provider = constraint -> {
            if (constraint == null) return null;

            String str = constraint.toString().toUpperCase();
            Log.d("autocompletion", "Looking for " + str);
            String[] col_names = {FontsContract.Columns._ID, field};
            MatrixCursor c = new MatrixCursor(col_names);

            String sql;
            String[] params;
            if (profileSpecific) {
                sql = String.format("SELECT %s as a, case when %s_upper LIKE ?||'%%' then 1 " +
                                    "WHEN %s_upper LIKE '%%:'||?||'%%' then 2 " +
                                    "WHEN %s_upper LIKE '%% '||?||'%%' then 3 else 9 end " +
                                    "FROM %s " +
                                    "WHERE profile=? AND %s_upper LIKE '%%'||?||'%%' " +
                                    "ORDER BY 2, 1;", field, field, field, field, table, field);
                params = new String[]{str, str, str, Data.profile.get().getUuid(), str};
            }
            else {
                sql = String.format("SELECT %s as a, case when %s_upper LIKE ?||'%%' then 1 " +
                                    "WHEN %s_upper LIKE '%%:'||?||'%%' then 2 " +
                                    "WHEN %s_upper LIKE '%% '||?||'%%' then 3 " + "else 9 end " +
                                    "FROM %s " + "WHERE %s_upper LIKE '%%'||?||'%%' " +
                                    "ORDER BY 2, 1;", field, field, field, field, table, field);
                params = new String[]{str, str, str, str};
            }
            Log.d("autocompletion", sql);
            SQLiteDatabase db = MLDB.getReadableDatabase();

            try (Cursor matches = db.rawQuery(sql, params)) {
                int i = 0;
                while (matches.moveToNext()) {
                    String match = matches.getString(0);
                    int order = matches.getInt(1);
                    Log.d("autocompletion", String.format("match: %s |%d", match, order));
                    c.newRow().add(i++).add(match);
                }
            }

            return c;

        };

        adapter.setFilterQueryProvider(provider);

        view.setAdapter(adapter);

        if (nextView != null) {
            view.setOnItemClickListener((parent, itemView, position, id) -> {
                nextView.requestFocus(View.FOCUS_FORWARD);
                if (callback != null) {
                    callback.descriptionSelected(String.valueOf(view.getText()));
                }
            });
        }
    }
    public static void init(Application context) {
        MLDB.context = context;
    }
    public static void done() {
        if (helperForReading != null) helperForReading.close();

        if ((helperForWriting != helperForReading) && (helperForWriting != null))
            helperForWriting.close();
    }

    public enum DatabaseMode {READ, WRITE}
}

class MobileLedgerDatabase extends SQLiteOpenHelper implements AutoCloseable {
    public static final String DB_NAME = "MoLe.db";
    public static final int LATEST_REVISION = 20;

    private final Application mContext;

    public MobileLedgerDatabase(Application context) {
        super(context, DB_NAME, null, LATEST_REVISION);
        Log.d("db", "creating helper instance");
        mContext = context;
        super.setWriteAheadLoggingEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d("db", "onCreate called");
        onUpgrade(db, -1, LATEST_REVISION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d("db", "onUpgrade called");
        for (int i = oldVersion + 1; i <= newVersion; i++) applyRevision(db, i);
    }

    private void applyRevision(SQLiteDatabase db, int rev_no) {
        final Resources rm = mContext.getResources();
        String rev_file = String.format(Locale.US, "sql_%d", rev_no);

        int res_id = rm.getIdentifier(rev_file, "raw", mContext.getPackageName());
        if (res_id == 0)
            throw new SQLException(String.format(Locale.US, "No resource for revision %d", rev_no));
        db.beginTransaction();
        try (InputStream res = rm.openRawResource(res_id)) {
            Log.d("db", "Applying revision " + String.valueOf(rev_no));
            InputStreamReader isr = new InputStreamReader(res);
            BufferedReader reader = new BufferedReader(isr);

            String line;
            int line_no = 1;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("--")) {
                    line_no++;
                    continue;
                }
                if (line.isEmpty()) {
                    line_no++;
                    continue;
                }
                try {
                    db.execSQL(line);
                }
                catch (Exception e) {
                    throw new RuntimeException(
                            String.format("Error applying revision %d, line %d", rev_no, line_no),
                            e);
                }
                line_no++;
            }

            db.setTransactionSuccessful();
        }
        catch (IOException e) {
            Log.e("db", String.format("Error opening raw resource for revision %d", rev_no));
            e.printStackTrace();
        }
        finally {
            db.endTransaction();
        }
    }
}
