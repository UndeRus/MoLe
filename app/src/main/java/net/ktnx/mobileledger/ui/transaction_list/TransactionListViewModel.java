/*
 * Copyright © 2018 Damyan Ivanov.
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

package net.ktnx.mobileledger.ui.transaction_list;

import android.app.Activity;
import android.arch.lifecycle.ViewModel;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.utils.MLDB;

import java.util.ArrayList;

public class TransactionListViewModel extends ViewModel {

    private ArrayList<LedgerTransaction> transactions;

    public void reloadTransactions(TransactionListFragment context) {
        ArrayList<LedgerTransaction> newList = new ArrayList<>();

        Activity act = context.getActivity();

        boolean hasFilter =
                act.findViewById(R.id.transaction_list_account_name_filter).getVisibility() ==
                View.VISIBLE;

        String sql;
        String[] params;

        sql = "SELECT id FROM transactions  ORDER BY date desc, id desc";
        params = null;

        if (hasFilter) {
            String filterAccName = String.valueOf(
                    ((AutoCompleteTextView) act.findViewById(R.id.transaction_filter_account_name))
                            .getText());

            if (!filterAccName.isEmpty()) {
                sql = "SELECT distinct tr.id from transactions tr JOIN transaction_accounts ta " +
                      "ON ta.transaction_id=tr.id WHERE ta.account_name LIKE ?||'%' AND ta" +
                      ".amount <> 0 ORDER BY tr.date desc, tr.id desc";
                params = new String[]{filterAccName};
            }
        }

        Log.d("tmp", sql);
        try (SQLiteDatabase db = MLDB.getReadableDatabase(act)) {
            try (Cursor cursor = db.rawQuery(sql, params)) {
                while (cursor.moveToNext()) {
                    newList.add(new LedgerTransaction(cursor.getInt(0)));
                }
                transactions = newList;
                Log.d("transactions", "transaction list updated");
            }
        }

    }
    public LedgerTransaction getTransaction(int position) {
        if (position >= transactions.size()) return null;
        return transactions.get(position);
    }
    public int getTransactionCount() {
        if (transactions == null) return 0;
        return transactions.size();
    }
}
