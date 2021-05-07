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

import androidx.annotation.Nullable;

import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerAccount;
import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.model.LedgerTransactionAccount;
import net.ktnx.mobileledger.model.TransactionListItem;
import net.ktnx.mobileledger.ui.MainModel;
import net.ktnx.mobileledger.utils.Misc;
import net.ktnx.mobileledger.utils.SimpleDate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;

public class TransactionAccumulator {
    private final ArrayList<TransactionListItem> list = new ArrayList<>();
    private final String boldAccountName;
    private final String accumulateAccount;
    private final HashMap<String, BigDecimal> runningTotal = new HashMap<>();
    private SimpleDate earliestDate, latestDate;
    private SimpleDate lastDate;
    private int transactionCount = 0;
    public TransactionAccumulator(@Nullable String boldAccountName,
                                  @Nullable String accumulateAccount) {
        this.boldAccountName = boldAccountName;
        this.accumulateAccount = accumulateAccount;

        list.add(new TransactionListItem());    // head item
    }
    public void put(LedgerTransaction transaction) {
        put(transaction, transaction.getDate());
    }
    public void put(LedgerTransaction transaction, SimpleDate date) {
        transactionCount++;

        // first item
        if (null == earliestDate)
            earliestDate = date;
        latestDate = date;

        if (lastDate != null && !date.equals(lastDate)) {
            boolean showMonth = date.month != lastDate.month || date.year != lastDate.year;
            list.add(1, new TransactionListItem(lastDate, showMonth));
        }

        String currentTotal = null;
        if (accumulateAccount != null) {
            for (LedgerTransactionAccount acc : transaction.getAccounts()) {
                if (acc.getAccountName()
                       .equals(accumulateAccount) ||
                    LedgerAccount.isParentOf(accumulateAccount, acc.getAccountName()))
                {
                    BigDecimal amt = runningTotal.get(acc.getCurrency());
                    if (amt == null)
                        amt = BigDecimal.ZERO;
                    BigDecimal newAmount = BigDecimal.valueOf(acc.getAmount());
                    newAmount = newAmount.setScale(2, RoundingMode.HALF_EVEN);
                    amt = amt.add(newAmount);
                    runningTotal.put(acc.getCurrency(), amt);
                }
            }

            currentTotal = summarizeRunningTotal(runningTotal);
        }
        list.add(1, new TransactionListItem(transaction, boldAccountName, currentTotal));

        lastDate = date;
    }
    private String summarizeRunningTotal(HashMap<String, BigDecimal> runningTotal) {
        StringBuilder b = new StringBuilder();
        for (String currency : runningTotal.keySet()) {
            if (b.length() != 0)
                b.append('\n');
            if (Misc.emptyIsNull(currency) != null)
                b.append(currency)
                 .append(' ');
            BigDecimal val = runningTotal.get(currency);
            b.append(Data.formatNumber((val == null) ? 0f : val.floatValue()));
        }
        return b.toString();
    }
    public void publishResults(MainModel model) {
        if (lastDate != null) {
            SimpleDate today = SimpleDate.today();
            if (!lastDate.equals(today)) {
                boolean showMonth = today.month != lastDate.month || today.year != lastDate.year;
                list.add(1, new TransactionListItem(lastDate, showMonth));
            }
        }

        model.setDisplayedTransactions(list, transactionCount);
        model.setFirstTransactionDate(earliestDate);
        model.setLastTransactionDate(latestDate);
    }
}
