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

import android.app.Activity;
import android.arch.lifecycle.ViewModel;
import android.os.AsyncTask;
import android.view.View;
import android.widget.AutoCompleteTextView;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.async.UpdateTransactionsTask;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.utils.ObservableValue;

import java.util.List;

public class TransactionListViewModel extends ViewModel {
    public static ObservableValue<Boolean> updating = new ObservableValue<>();

    public static void scheduleTransactionListReload(Activity act) {
        boolean hasFilter =
                act.findViewById(R.id.transaction_list_account_name_filter).getVisibility() ==
                View.VISIBLE;
        String accFilter = hasFilter ? String.valueOf(
                ((AutoCompleteTextView) act.findViewById(R.id.transaction_filter_account_name))
                        .getText()) : null;
        AsyncTask<String, Void, List<LedgerTransaction>> task = new UTT();
        task.execute(accFilter);
    }
    public static LedgerTransaction getTransaction(int position) {
        List<LedgerTransaction> transactions = Data.transactions.get();
        if (position >= transactions.size()) return null;
        return transactions.get(position);
    }
    public static int getTransactionCount() {
        List<LedgerTransaction> transactions = Data.transactions.get();
        if (transactions == null) return 0;
        return transactions.size();
    }
    private static class UTT extends UpdateTransactionsTask {
        @Override
        protected void onPostExecute(List<LedgerTransaction> list) {
            super.onPostExecute(list);
            if (list != null) Data.transactions.set(list);
        }
    }
}
