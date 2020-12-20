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

package net.ktnx.mobileledger.json;

import com.fasterxml.jackson.databind.MappingIterator;

import net.ktnx.mobileledger.async.RetrieveTransactionsTask;
import net.ktnx.mobileledger.model.LedgerAccount;

import java.util.ArrayList;
import java.util.HashMap;

import static net.ktnx.mobileledger.utils.Logger.debug;

abstract public class AccountListParser {
    protected MappingIterator<net.ktnx.mobileledger.json.ParsedLedgerAccount> iterator;

    public ParsedLedgerAccount nextAccount() {
        if (!iterator.hasNext())
            return null;

        ParsedLedgerAccount next = iterator.next();

        if (next.getAname()
                .equalsIgnoreCase("root"))
            return nextAccount();

        debug("accounts", String.format("Got account '%s' [v1.15]", next.getAname()));
        return next;
    }
    public LedgerAccount nextLedgerAccount(RetrieveTransactionsTask task,
                                           HashMap<String, LedgerAccount> map) {
        ParsedLedgerAccount parsedAccount = nextAccount();
        if (parsedAccount == null)
            return null;

        task.addNumberOfPostings(parsedAccount.getAnumpostings());
        final String accName = parsedAccount.getAname();
        LedgerAccount acc = map.get(accName);
        if (acc != null)
            throw new RuntimeException(
                    String.format("Account '%s' already present", acc.getName()));
        String parentName = LedgerAccount.extractParentName(accName);
        ArrayList<LedgerAccount> createdParents = new ArrayList<>();
        LedgerAccount parent;
        if (parentName == null) {
            parent = null;
        }
        else {
            parent = task.ensureAccountExists(parentName, map, createdParents);
            parent.setHasSubAccounts(true);
        }
        acc = new LedgerAccount(task.getProfile(), accName, parent);
        map.put(accName, acc);

        String lastCurrency = null;
        float lastCurrencyAmount = 0;
        for (ParsedBalance b : parsedAccount.getAibalance()) {
            task.throwIfCancelled();
            final String currency = b.getAcommodity();
            final float amount = b.getAquantity()
                                  .asFloat();
            if (currency.equals(lastCurrency)) {
                lastCurrencyAmount += amount;
            }
            else {
                if (lastCurrency != null) {
                    acc.addAmount(lastCurrencyAmount, lastCurrency);
                }
                lastCurrency = currency;
                lastCurrencyAmount = amount;
            }
        }
        if (lastCurrency != null) {
            acc.addAmount(lastCurrencyAmount, lastCurrency);
        }
        for (LedgerAccount p : createdParents)
            acc.propagateAmountsTo(p);

        return acc;
    }

}
