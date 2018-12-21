/*
 * Copyright © 2018 Damyan Ivanov.
 * This file is part of Mobile-Ledger.
 * Mobile-Ledger is free software: you can distribute it and/or modify it
 * under the term of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your opinion), any later version.
 *
 * Mobile-Ledger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License terms for details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mobile-Ledger. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ktnx.mobileledger.utils;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

import static net.ktnx.mobileledger.utils.MLDB.DatabaseMode.READ;
import static net.ktnx.mobileledger.utils.MLDB.DatabaseMode.WRITE;

public final class MLDB {
    public enum DatabaseMode {READ, WRITE}

    public static final String ACCOUNTS_TABLE = "accounts";
    public static final String DESCRIPTION_HISTORY_TABLE = "description_history";
    public static final String OPT_TRANSACTION_LIST_STAMP = "transaction_list_last_update";
    private static MobileLedgerDatabase helperForReading, helperForWriting;

    public static synchronized SQLiteDatabase getDatabase(Context context, DatabaseMode mode) {
        if (mode == READ) {
            if (helperForReading == null) helperForReading = new MobileLedgerDatabase(context);
            return helperForReading.getReadableDatabase();
        }
        else {
            if (helperForWriting == null) helperForWriting = new MobileLedgerDatabase(context);
            return helperForWriting.getWritableDatabase();
        }
    }
    public static SQLiteDatabase getReadableDatabase(Context context) {
        return getDatabase(context, READ);
    }
    public static SQLiteDatabase getWritableDatabase(Context context) {
        return getDatabase(context, WRITE);
    }
    static public int get_option_value(Context context, String name, int default_value) {
        String s = get_option_value(context, name, String.valueOf(default_value));
        try {
            return Integer.parseInt(s);
        }
        catch (Exception e) {
            Log.d("db", "returning default int value of " + name, e);
            return default_value;
        }
    }

    static public long get_option_value(Context context, String name, long default_value) {
        String s = get_option_value(context, name, String.valueOf(default_value));
        try {
            return Long.parseLong(s);
        }
        catch (Exception e) {
            Log.d("db", "returning default long value of " + name, e);
            return default_value;
        }
    }

    static public String get_option_value(Context context, String name, String default_value) {
        Log.d("db", "about to fetch option " + name);
        try (SQLiteDatabase db = getReadableDatabase(context)) {
            try (Cursor cursor = db
                    .rawQuery("select value from options where name=?", new String[]{name}))
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
    }

    static public void set_option_value(Context context, String name, String value) {
        Log.d("db", "setting option " + name + "=" + value);
        try (SQLiteDatabase db = getWritableDatabase(context)) {
            db.execSQL("insert or replace into options(name, value) values(?, ?);",
                    new String[]{name, value});
        }
    }

    static public void set_option_value(Context context, String name, long value) {
        set_option_value(context, name, String.valueOf(value));
    }
}

class MobileLedgerDatabase extends SQLiteOpenHelper implements AutoCloseable {
    public static final String DB_NAME = "mobile-ledger.db";
    public static final int LATEST_REVISION = 10;

    private final Context mContext;

    public MobileLedgerDatabase(Context context) {
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
        if (res_id == 0) throw new SQLException(
                String.format(Locale.US, "No resource for revision %d", rev_no));
        db.beginTransaction();
        try (InputStream res = rm.openRawResource(res_id)) {
            Log.d("db", "Applying revision " + String.valueOf(rev_no));
            InputStreamReader isr = new InputStreamReader(res);
            BufferedReader reader = new BufferedReader(isr);

            String line;
            while ((line = reader.readLine()) != null) {
                db.execSQL(line);
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
