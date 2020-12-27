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

import net.ktnx.mobileledger.async.RetrieveTransactionsTask;
import net.ktnx.mobileledger.model.LedgerAccount;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class ParsedLedgerAccount {
    private String aname;
    private int anumpostings;
    public abstract List<SimpleBalance> getSimpleBalance();
    public String getAname() {
        return aname;
    }
    public void setAname(String aname) {
        this.aname = aname;
    }
    public int getAnumpostings() {
        return anumpostings;
    }
    public void setAnumpostings(int anumpostings) {
        this.anumpostings = anumpostings;
    }
    public LedgerAccount toLedgerAccount(RetrieveTransactionsTask task,
                                         HashMap<String, LedgerAccount> map) {
        task.addNumberOfPostings(getAnumpostings());
        final String accName = getAname();
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
        for (SimpleBalance b : getSimpleBalance()) {
            task.throwIfCancelled();
            final String currency = b.getCommodity();
            final float amount = b.getAmount();
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

    static public class SimpleBalance {
        private String commodity;
        private float amount;
        public SimpleBalance(String commodity, float amount) {
            this.commodity = commodity;
            this.amount = amount;
        }
        public String getCommodity() {
            return commodity;
        }
        public void setCommodity(String commodity) {
            this.commodity = commodity;
        }
        public float getAmount() {
            return amount;
        }
        public void setAmount(float amount) {
            this.amount = amount;
        }
    }
}
