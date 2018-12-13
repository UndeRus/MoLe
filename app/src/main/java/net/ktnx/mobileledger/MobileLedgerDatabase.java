package net.ktnx.mobileledger;

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

class MobileLedgerDatabase extends SQLiteOpenHelper implements AutoCloseable {
    static final String DB_NAME = "mobile-ledger.db";
    static final String ACCOUNTS_TABLE = "accounts";
    static final String DESCRIPTION_HISTORY_TABLE = "description_history";
    static final int LATEST_REVISION = 6;

    final Context mContext;

    public
    MobileLedgerDatabase(Context context) {
        super(context, DB_NAME, null, LATEST_REVISION);
        Log.d("db", "creating helper instance");
        mContext = context;
    }

    @Override
    public
    void onCreate(SQLiteDatabase db) {
        Log.d("db", "onCreate called");
        onUpgrade(db, -1, LATEST_REVISION);
    }

    @Override
    public
    void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d("db", "onUpgrade called");
        for(int i = oldVersion+1; i <= newVersion; i++) applyRevision(db, i);
    }

    private void applyRevision(SQLiteDatabase db, int
            rev_no) {
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
    int get_option_value(String name, int default_value) {
        String s = get_option_value(name, String.valueOf(default_value));
        try {
            return Integer.parseInt(s);
        }
        catch (Exception e) {
            return default_value;
        }
    }

    long get_option_value(String name, long default_value) {
        String s = get_option_value(name, String.valueOf(default_value));
        try {
            return Long.parseLong(s);
        }
        catch (Exception e) {
            Log.d("db", "returning default long value of "+name, e);
            return default_value;
        }
    }

    String get_option_value(String name, String default_value) {
        Log.d("db", "about to fetch option "+name);
        try(SQLiteDatabase db = getReadableDatabase()) {
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

     void set_option_value(String name, String value) {
        Log.d("db", "setting option "+name+"="+value);
        try(SQLiteDatabase db = getWritableDatabase()) {
            db.execSQL("insert or replace into options(name, value) values(?, ?);",
                    new String[]{name, value});
        }
    }

    void set_option_value(String name, long value) {
        set_option_value(name, String.valueOf(value));
    }
    static long get_option_value(Context context, String name, long default_value) {
        try(MobileLedgerDatabase db = new MobileLedgerDatabase(context)) {
            return db.get_option_value(name, default_value);
        }
    }
    static void set_option_value(Context context, String name, String value) {
        try(MobileLedgerDatabase db = new MobileLedgerDatabase(context)) {
            db.set_option_value(name, value);
        }
    }
}
