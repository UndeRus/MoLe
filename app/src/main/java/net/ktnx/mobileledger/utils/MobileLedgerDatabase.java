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

import android.app.Application;
import android.content.res.Resources;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

import static net.ktnx.mobileledger.utils.Logger.debug;

public class MobileLedgerDatabase extends SQLiteOpenHelper {
    private static final String DB_NAME = "MoLe.db";
    private static final int LATEST_REVISION = 22;
    private static final String CREATE_DB_SQL = "create_db";

    private final Application mContext;

    public MobileLedgerDatabase(Application context) {
        super(context, DB_NAME, null, LATEST_REVISION);
        debug("db", "creating helper instance");
        mContext = context;
        super.setWriteAheadLoggingEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        debug("db", "onCreate called");
        applyRevisionFile(db, CREATE_DB_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        debug("db", "onUpgrade called");
        for (int i = oldVersion + 1; i <= newVersion; i++) applyRevision(db, i);
    }

    private void applyRevision(SQLiteDatabase db, int rev_no) {
        String rev_file = String.format(Locale.US, "sql_%d", rev_no);

        applyRevisionFile(db, rev_file);
    }
    private void applyRevisionFile(SQLiteDatabase db, String rev_file) {
        final Resources rm = mContext.getResources();
        int res_id = rm.getIdentifier(rev_file, "raw", mContext.getPackageName());
        if (res_id == 0)
            throw new SQLException(String.format(Locale.US, "No resource for %s", rev_file));
        db.beginTransaction();
        try (InputStream res = rm.openRawResource(res_id)) {
            debug("db", "Applying " + rev_file);
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
                            String.format("Error applying %s, line %d", rev_file, line_no), e);
                }
                line_no++;
            }

            db.setTransactionSuccessful();
        }
        catch (IOException e) {
            Log.e("db", String.format("Error opening raw resource for %s", rev_file));
            e.printStackTrace();
        }
        finally {
            db.endTransaction();
        }
    }
}
