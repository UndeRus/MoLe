package net.ktnx.mobileledger;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

class MobileLedgerDB {
    static final String DATABASE_NAME = "accounts";
    private static String db_filename;
    static SQLiteDatabase db;

    static String getDb_filename() {
        return db_filename;
    }

    static void setDb_filename(String db_filename) {
        MobileLedgerDB.db_filename = db_filename;
    }

    static void initDB() {
        db = SQLiteDatabase.openOrCreateDatabase(db_filename, null);

        db.execSQL("create table if not exists accounts(name varchar);");
        db.execSQL("create index if not exists idx_accounts_name on accounts(name);");
        db.execSQL("create table if not exists options(name varchar, value varchar);");
        db.execSQL("create unique index if not exists idx_options_name on options(name);");
    }

    static int get_option_value(String name, int default_value) {
        String s = get_option_value(name, String.valueOf(default_value));
        try {
            return Integer.parseInt(s);
        }
        catch (Exception e) {
            return default_value;
        }
    }

    static long get_option_value(String name, long default_value) {
        String s = get_option_value(name, String.valueOf(default_value));
        try {
            return Long.parseLong(s);
        }
        catch (Exception e) {
            Log.d("db", "returning default long value of "+name, e);
            return default_value;
        }
    }

    static String get_option_value(String name, String default_value) {
        Log.d("db", "about fo fetch option "+name);
        try (Cursor cursor = db.rawQuery("select value from options where name=?", new String[]{name})) {
            if (cursor.moveToFirst()) {
                String result = cursor.getString(0);

                if (result == null ) result = default_value;

                Log.d("db", "option "+name+"="+result);
                return result;
            }
            else return default_value;
        }
        catch(Exception e) {
            Log.d("db", "returning default value for "+name, e);
            return default_value;
        }
    }

    static void set_option_value(String name, String value) {
        Log.d("db", "setting option "+name+"="+value);
        db.execSQL("insert or replace into options(name, value) values(?, ?);", new String[]{name, value});
    }

    static void set_option_value(String name, long value) {
        set_option_value(name, String.valueOf(value));
    }
}
