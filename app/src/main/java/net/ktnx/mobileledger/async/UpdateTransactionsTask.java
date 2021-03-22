/*
 * Copyright © 2021 Damyan Ivanov.
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

import net.ktnx.mobileledger.App;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
import net.ktnx.mobileledger.ui.MainModel;
import net.ktnx.mobileledger.utils.SimpleDate;

import static net.ktnx.mobileledger.utils.Logger.debug;

public class UpdateTransactionsTask extends AsyncTask<MainModel, Void, String> {
    protected String doInBackground(MainModel[] model) {
        final MobileLedgerProfile profile = Data.getProfile();

        long profile_id = profile.getId();
        Data.backgroundTaskStarted();
        try {
            String sql;
            String[] params;

            final String accFilter = model[0].getAccountFilter()
                                             .getValue();
            if (accFilter == null) {
                sql = "SELECT id, year, month, day FROM transactions WHERE profile_id=? ORDER BY " +
                      "year desc, month desc, day desc, id desc";
                params = new String[]{String.valueOf(profile_id)};

            }
            else {
                sql = "SELECT distinct tr.id, tr.year, tr.month, tr.day from transactions tr " +
                      "JOIN " + "transaction_accounts ta " +
                      "ON ta.transaction_id=tr.id AND ta.profile=tr.profile WHERE tr.profile_id=?" +
                      " " +
                      "and ta.account_name LIKE ?||'%' AND ta" +
                      ".amount <> 0 ORDER BY tr.year desc, tr.month desc, tr.day desc, tr.id " +
                      "desc";
                params = new String[]{String.valueOf(profile_id), accFilter};
            }

            debug("UTT", sql);
            TransactionAccumulator accumulator = new TransactionAccumulator(model[0]);

            SQLiteDatabase db = App.getDatabase();
            try (Cursor cursor = db.rawQuery(sql, params)) {
                while (cursor.moveToNext()) {
                    if (isCancelled())
                        return null;

                    accumulator.put(new LedgerTransaction(cursor.getInt(0)),
                            new SimpleDate(cursor.getInt(1), cursor.getInt(2), cursor.getInt(3)));
                }
            }

            accumulator.done();
            debug("UTT", "transaction list value updated");

            return null;
        }
        finally {
            Data.backgroundTaskFinished();
        }
    }
}
