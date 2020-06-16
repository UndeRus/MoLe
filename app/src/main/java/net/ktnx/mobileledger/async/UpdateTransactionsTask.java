/*
 * Copyright © 2020 Damyan Ivanov.
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
import net.ktnx.mobileledger.model.TransactionListItem;
import net.ktnx.mobileledger.utils.SimpleDate;

import java.util.ArrayList;

import static net.ktnx.mobileledger.utils.Logger.debug;

public class UpdateTransactionsTask extends AsyncTask<String, Void, String> {
    protected String doInBackground(String[] filterAccName) {
        final MobileLedgerProfile profile = Data.profile.getValue();
        if (profile == null)
            return "Profile not configured";

        String profile_uuid = profile.getUuid();
        Data.backgroundTaskStarted();
        try {
            ArrayList<TransactionListItem> newList = new ArrayList<>();

            String sql;
            String[] params;

            if (filterAccName[0] == null) {
                sql = "SELECT id, year, month, day FROM transactions WHERE profile=? ORDER BY " +
                      "year desc, month desc, day desc, id desc";
                params = new String[]{profile_uuid};

            }
            else {
                sql = "SELECT distinct tr.id, tr.year, tr.month, tr.day from transactions tr " +
                      "JOIN " + "transaction_accounts ta " +
                      "ON ta.transaction_id=tr.id AND ta.profile=tr.profile WHERE tr.profile=? " +
                      "and ta.account_name LIKE ?||'%' AND ta" +
                      ".amount <> 0 ORDER BY tr.year desc, tr.month desc, tr.day desc, tr.id " +
                      "desc";
                params = new String[]{profile_uuid, filterAccName[0]};
            }

            debug("UTT", sql);
            SimpleDate latestDate = null, earliestDate = null;
            SQLiteDatabase db = App.getDatabase();
            boolean odd = true;
            SimpleDate lastDate = SimpleDate.today();
            try (Cursor cursor = db.rawQuery(sql, params)) {
                while (cursor.moveToNext()) {
                    if (isCancelled())
                        return null;

                    int transaction_id = cursor.getInt(0);
                    SimpleDate date =
                            new SimpleDate(cursor.getInt(1), cursor.getInt(2), cursor.getInt(3));

                    if (null == latestDate)
                        latestDate = date;
                    earliestDate = date;

                    if (!date.equals(lastDate)) {
                        boolean showMonth =
                                (date.month != lastDate.month) || (date.year != lastDate.year);
                        newList.add(new TransactionListItem(date, showMonth));
                    }
                    newList.add(
                            new TransactionListItem(new LedgerTransaction(transaction_id), odd));
//                    debug("UTT", String.format("got transaction %d", transaction_id));

                    lastDate = date;
                    odd = !odd;
                }
                Data.transactions.setList(newList);
                Data.latestTransactionDate.postValue(latestDate);
                Data.earliestTransactionDate.postValue(earliestDate);
                debug("UTT", "transaction list value updated");
            }

            return null;
        }
        finally {
            Data.backgroundTaskFinished();
        }
    }
}
