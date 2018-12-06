package net.ktnx.mobileledger;

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
    }
}
