/*
 * Copyright © 2019 Damyan Ivanov.
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

package net.ktnx.mobileledger.async;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.utils.MLDB;

import java.util.ArrayList;
import java.util.List;

public class UpdateTransactionsTask extends AsyncTask<String, Void, List<LedgerTransaction>> {
    protected List<LedgerTransaction> doInBackground(String[] filterAccName) {
        Data.backgroundTaskCount.incrementAndGet();
        String profile_uuid = Data.profile.get().getUuid();
        try {
            ArrayList<LedgerTransaction> newList = new ArrayList<>();

            boolean hasFilter = (filterAccName != null) && (filterAccName.length > 0) &&
                                (filterAccName[0] != null) && !filterAccName[0].isEmpty();

            String sql;
            String[] params;

            sql = "SELECT id FROM transactions WHERE profile=? ORDER BY date desc, id desc";
            params = new String[]{profile_uuid};

            if (hasFilter) {
                sql = "SELECT distinct tr.id from transactions tr JOIN transaction_accounts ta " +
                      "ON ta.transaction_id=tr.id AND ta.profile=tr.profile WHERE tr.profile=? " +
                      "and ta" + ".account_name LIKE ?||'%' AND ta" +
                      ".amount <> 0 ORDER BY tr.date desc, tr.id desc";
                params = filterAccName;
            }

            Log.d("UTT", sql);
            SQLiteDatabase db = MLDB.getReadableDatabase();
            try (Cursor cursor = db.rawQuery(sql, params)) {
                while (cursor.moveToNext()) {
                    if (isCancelled()) return null;

                    int transaction_id = cursor.getInt(0);
                    newList.add(new LedgerTransaction(transaction_id));
//                    Log.d("UTT", String.format("got transaction %d", transaction_id));
                }
                Data.transactions.set(newList);
                Log.d("UTT", "transaction list value updated");
            }

            return newList;
        }
        finally {
            Data.backgroundTaskCount.decrementAndGet();
        }
    }
}
