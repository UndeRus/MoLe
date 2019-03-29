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
import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
import net.ktnx.mobileledger.model.TransactionListItem;
import net.ktnx.mobileledger.utils.Globals;
import net.ktnx.mobileledger.utils.MLDB;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;

public class UpdateTransactionsTask extends AsyncTask<String, Void, String> {
    protected String doInBackground(String[] filterAccName) {
        final MobileLedgerProfile profile = Data.profile.get();
        if (profile == null) return "Profile not configured";

        String profile_uuid = profile.getUuid();
        Data.backgroundTaskCount.incrementAndGet();
        try {
            ArrayList<TransactionListItem> newList = new ArrayList<>();

            String sql;
            String[] params;

            if (filterAccName[0] == null) {
                sql = "SELECT id, date FROM transactions WHERE profile=? ORDER BY date desc, id " +
                      "desc";
                params = new String[]{profile_uuid};

            }
            else {
                sql = "SELECT distinct tr.id, tr.date from transactions tr JOIN " +
                      "transaction_accounts ta " +
                      "ON ta.transaction_id=tr.id AND ta.profile=tr.profile WHERE tr.profile=? " +
                      "and ta.account_name LIKE ?||'%' AND ta" +
                      ".amount <> 0 ORDER BY tr.date desc, tr.id desc";
                params = new String[]{profile_uuid, filterAccName[0]};
            }

            Log.d("UTT", sql);
            SQLiteDatabase db = MLDB.getDatabase();
            String lastDateString = Globals.formatLedgerDate(new Date());
            Date lastDate = Globals.parseLedgerDate(lastDateString);
            boolean odd = true;
            try (Cursor cursor = db.rawQuery(sql, params)) {
                while (cursor.moveToNext()) {
                    if (isCancelled()) return null;

                    int transaction_id = cursor.getInt(0);
                    String dateString = cursor.getString(1);
                    Date date = Globals.parseLedgerDate(dateString);

                    if (!lastDateString.equals(dateString)) {
                        boolean showMonth = (date.getMonth() != lastDate.getMonth() ||
                                             date.getYear() != lastDate.getYear());
                        newList.add(new TransactionListItem(date, showMonth));
                    }
                    newList.add(
                            new TransactionListItem(new LedgerTransaction(transaction_id), odd));
//                    Log.d("UTT", String.format("got transaction %d", transaction_id));

                    lastDate = date;
                    lastDateString = dateString;
                    odd = !odd;
                }
                Data.transactions.setList(newList);
                Log.d("UTT", "transaction list value updated");
            }

            return null;
        }
        catch (ParseException e) {
            return String.format("Error parsing stored date '%s'", e.getMessage());
        }
        finally {
            Data.backgroundTaskCount.decrementAndGet();
        }
    }
}
