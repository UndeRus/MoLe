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

import android.arch.lifecycle.ViewModel;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.utils.MobileLedgerDatabase;

import java.util.ArrayList;
import java.util.List;

public class TransactionListViewModel extends ViewModel {

    private List<LedgerTransaction> transactions;

    public List<LedgerTransaction> getTransactions(MobileLedgerDatabase dbh) {
        if (transactions == null) {
            transactions = new ArrayList<>();
            reloadTransactions(dbh);
        }

        return transactions;
    }
    private void reloadTransactions(MobileLedgerDatabase dbh) {
        transactions.clear();
        String sql = "SELECT id, date, description FROM transactions";
        sql += " ORDER BY date desc, id desc";

        try (SQLiteDatabase db = dbh.getReadableDatabase()) {
            try (Cursor cursor = db.rawQuery(sql, null)) {
                while (cursor.moveToNext()) {
                    LedgerTransaction tr =
                            new LedgerTransaction(cursor.getString(0), cursor.getString(1),
                                    cursor.getString(2));
                    // TODO: fill accounts and amounts
                    transactions.add(tr);
                }
            }
        }

    }
}
