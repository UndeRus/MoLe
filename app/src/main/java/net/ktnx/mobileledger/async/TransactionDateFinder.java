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

import android.os.AsyncTask;

import net.ktnx.mobileledger.model.TransactionListItem;
import net.ktnx.mobileledger.ui.MainModel;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.SimpleDate;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class TransactionDateFinder extends AsyncTask<TransactionDateFinder.Params, Void, Integer> {
    private MainModel model;
    @Override
    protected void onPostExecute(Integer pos) {
        model.foundTransactionItemIndex.setValue(pos);
    }
    @Override
    protected Integer doInBackground(Params... param) {
        this.model = param[0].model;
        SimpleDate date = param[0].date;
        Logger.debug("go-to-date",
                String.format(Locale.US, "Looking for date %04d-%02d-%02d", date.year, date.month,
                        date.day));
        List<TransactionListItem> transactions = Objects.requireNonNull(
                param[0].model.getDisplayedTransactions()
                              .getValue());
        final int transactionCount = transactions.size();
        Logger.debug("go-to-date",
                String.format(Locale.US, "List contains %d transactions", transactionCount));

        TransactionListItem target = new TransactionListItem(date, true);
        int found =
                Collections.binarySearch(transactions, target, new TransactionListItemComparator());
        if (found >= 0)
            return found;
        else
            return -1 - found;
    }

    public static class Params {
        public final MainModel model;
        public final SimpleDate date;
        public Params(@NotNull MainModel model, @NotNull SimpleDate date) {
            this.model = model;
            this.date = date;
        }
    }

    static class TransactionListItemComparator implements Comparator<TransactionListItem> {
        @Override
        public int compare(@NotNull TransactionListItem a, @NotNull TransactionListItem b) {
            final TransactionListItem.Type aType = a.getType();
            if (aType == TransactionListItem.Type.HEADER)
                return +1;
            final TransactionListItem.Type bType = b.getType();
            if (bType == TransactionListItem.Type.HEADER)
                return -1;
            final SimpleDate aDate = a.getDate();
            final SimpleDate bDate = b.getDate();
            int res = aDate.compareTo(bDate);
            if (res != 0)
                return -res;    // transactions are reverse sorted by date

            if (aType == TransactionListItem.Type.DELIMITER) {
                if (bType == TransactionListItem.Type.DELIMITER)
                    return 0;
                else
                    return -1;
            }
            else {
                if (bType == TransactionListItem.Type.DELIMITER)
                    return +1;
                else
                    return 0;
            }
        }
    }
}
