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

import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.model.TransactionListItem;
import net.ktnx.mobileledger.ui.MainModel;
import net.ktnx.mobileledger.utils.SimpleDate;

import java.util.ArrayList;

public class TransactionAccumulator {
    private final ArrayList<TransactionListItem> list = new ArrayList<>();
    private final String boldAccountName;
    private SimpleDate earliestDate, latestDate;
    private SimpleDate lastDate;
    private int transactionCount = 0;
    public TransactionAccumulator(String boldAccountName) {
        this.boldAccountName = boldAccountName;

        list.add(new TransactionListItem());    // head item
    }
    public void put(LedgerTransaction transaction) {
        put(transaction, transaction.getDate());
    }
    public void put(LedgerTransaction transaction, SimpleDate date) {
        transactionCount++;

        // first item
        if (null == latestDate)
            latestDate = date;
        earliestDate = date;

        if (!date.equals(lastDate)) {
            if (lastDate == null)
                lastDate = SimpleDate.today();
            boolean showMonth = date.month != lastDate.month || date.year != lastDate.year;
            list.add(new TransactionListItem(date, showMonth));
        }

        list.add(new TransactionListItem(transaction, boldAccountName));

        lastDate = date;
    }
    public void publishResults(MainModel model) {
        model.setDisplayedTransactions(list, transactionCount);
        model.setFirstTransactionDate(earliestDate);
        model.setLastTransactionDate(latestDate);
    }
}
