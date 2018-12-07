package net.ktnx.mobileledger;

import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

class MobileLedgerDB {
    static final String DATABASE_NAME = "accounts";
    static final String OPT_DB_REVISION = "db_revision";
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
        db.execSQL("create table if not exists account_values(account varchar not null, currency varchar not null, value decimal(18,2) not null);");
        db.execSQL("create index if not exists idx_account_values_account on account_values(account);");
        db.execSQL("create unique index if not exists un_account_values on account_values(account,currency);");
    }

    static void applyRevisions(Resources rm, String pkg_name) {
        int cur_ver = Integer.parseInt(get_option_value(OPT_DB_REVISION, "0"));

        Log.d("db", "Current DB revision is "+String.valueOf(cur_ver));

        while (applyRevision(rm, pkg_name, cur_ver+1)) {
            cur_ver++;
        }

        Log.d("db", "Database revision is "+String.valueOf(cur_ver)+" now");
    }
    private static boolean applyRevision(Resources rm, String pkg_name, int rev_no) {
        String rev_file = String.format(Locale.US, "sql_%d", rev_no);

        int res_id = rm.getIdentifier(rev_file, "raw", pkg_name);
        if (res_id == 0) {
            Log.d("db", String.format(Locale.US, "No resource for revision %d", rev_no));
            return false;
        }
        db.beginTransaction();
        try (InputStream res = rm.openRawResource(res_id)) {
            Log.d("db", "Applying revision " + String.valueOf(rev_no));
            InputStreamReader isr = new InputStreamReader(res);
            BufferedReader reader = new BufferedReader(isr);

            String line;
            while ((line = reader.readLine()) != null) {
                db.execSQL(line);
            }

            set_option_value(OPT_DB_REVISION, rev_no);
            db.setTransactionSuccessful();
        } catch (Resources.NotFoundException e) {
            Log.d("db", "SQL revision "+String.valueOf(rev_no)+" not found");
            return false;
        }
        catch (SQLException e) {
            Log.e("db", String.format(Locale.US, "Error applying revision %d: %s", rev_no, e.getMessage()));
            return false;
        }
        catch (Exception e) {
            Log.w("db", "Error reading revision" + String.valueOf(rev_no)+": "+e.getMessage());
            return false;
        }
        finally {
            db.endTransaction();
        }

        return true;
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
