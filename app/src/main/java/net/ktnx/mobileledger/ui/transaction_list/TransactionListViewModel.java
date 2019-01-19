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

package net.ktnx.mobileledger.ui.transaction_list;

import android.arch.lifecycle.ViewModel;
import android.os.AsyncTask;

import net.ktnx.mobileledger.async.UpdateTransactionsTask;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.TransactionListItem;
import net.ktnx.mobileledger.utils.ObservableValue;

import java.util.List;

public class TransactionListViewModel extends ViewModel {
    public static ObservableValue<Boolean> updating = new ObservableValue<>();
    public static ObservableValue<String> updateError = new ObservableValue<>();

    public static void scheduleTransactionListReload() {
        String filter = TransactionListFragment.accountFilter.get();
        AsyncTask<String, Void, String> task = new UTT();
        task.execute(filter);
    }
    public static TransactionListItem getTransactionListItem(int position) {
        List<TransactionListItem> transactions = Data.transactions.get();
        if (position >= transactions.size() + 1) return null;
        if (position == transactions.size()) return new TransactionListItem();
        return transactions.get(position);
    }
    public static int getTransactionCount() {
        List<TransactionListItem> transactions = Data.transactions.get();
        if (transactions == null) return 0;
        return transactions.size();
    }
    private static class UTT extends UpdateTransactionsTask {
        @Override
        protected void onPostExecute(String error) {
            super.onPostExecute(error);
            if (error != null) updateError.set(error);
        }
    }
}
