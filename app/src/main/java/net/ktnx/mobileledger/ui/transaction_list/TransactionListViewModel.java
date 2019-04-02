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

package net.ktnx.mobileledger.ui.transaction_list;

import android.os.AsyncTask;

import net.ktnx.mobileledger.async.UpdateTransactionsTask;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.TransactionListItem;
import net.ktnx.mobileledger.utils.LockHolder;
import net.ktnx.mobileledger.utils.ObservableValue;

import androidx.lifecycle.ViewModel;

public class TransactionListViewModel extends ViewModel {
    public static ObservableValue<Boolean> updating = new ObservableValue<>();
    public static ObservableValue<String> updateError = new ObservableValue<>();

    public static void scheduleTransactionListReload() {
        if (Data.profile.get() == null) return;

        String filter = Data.accountFilter.get();
        AsyncTask<String, Void, String> task = new UTT();
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, filter);
    }
    public static TransactionListItem getTransactionListItem(int position) {
        try(LockHolder lh = Data.transactions.lockForReading()) {
            if (Data.transactions == null) return null;
            if (position >= Data.transactions.size() + 1) return null;
            if (position == Data.transactions.size()) return new TransactionListItem();
            return Data.transactions.get(position);
        }
    }
    private static class UTT extends UpdateTransactionsTask {
        @Override
        protected void onPostExecute(String error) {
            super.onPostExecute(error);
            if (error != null) updateError.set(error);
        }
    }
}
