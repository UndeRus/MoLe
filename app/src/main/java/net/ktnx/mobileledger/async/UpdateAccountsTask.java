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

package net.ktnx.mobileledger.async;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerAccount;
import net.ktnx.mobileledger.utils.MLDB;

import java.util.ArrayList;

public class UpdateAccountsTask extends AsyncTask<Void, Void, ArrayList<LedgerAccount>> {
    protected ArrayList<LedgerAccount> doInBackground(Void... params) {
        Data.backgroundTaskCount.incrementAndGet();
        String profileUUID = Data.profile.get().getUuid();
        boolean onlyStarred = Data.optShowOnlyStarred.get();
        try {
            ArrayList<LedgerAccount> newList = new ArrayList<>();

            String sql = "SELECT name, hidden FROM accounts WHERE profile = ?";
            if (onlyStarred) sql += " AND hidden = 0";
            sql += " ORDER BY name";

            SQLiteDatabase db = MLDB.getReadableDatabase();
            try (Cursor cursor = db.rawQuery(sql, new String[]{profileUUID})) {
                while (cursor.moveToNext()) {
                    LedgerAccount acc = new LedgerAccount(cursor.getString(0));
                    acc.setHidden(cursor.getInt(1) == 1);
                    try (Cursor c2 = db.rawQuery(
                            "SELECT value, currency FROM account_values WHERE profile = ? " +
                            "AND account = ?", new String[]{profileUUID, acc.getName()}))
                    {
                        while (c2.moveToNext()) {
                            acc.addAmount(c2.getFloat(0), c2.getString(1));
                        }
                    }
                    newList.add(acc);
                }
            }

            return newList;
        }
        finally {
            Log.d("UAT", "decrementing background task count");
            Data.backgroundTaskCount.decrementAndGet();
        }
    }
}
