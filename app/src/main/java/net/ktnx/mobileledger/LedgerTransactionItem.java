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

package net.ktnx.mobileledger;

class LedgerTransactionItem {
    private String accountName;
    private float amount;
    private boolean amountSet;
    private String currency;

    LedgerTransactionItem(String accountName, float amount) {
        this(accountName, amount, null);
    }
    LedgerTransactionItem(String accountName, float amount, String currency) {
        this.accountName = accountName;
        this.amount = amount;
        this.amountSet = true;
        this.currency = currency;
    }

    public LedgerTransactionItem(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public float getAmount() {
        if (!amountSet)
            throw new IllegalStateException("Account amount is not set");

        return amount;
    }

    public void setAmount(float account_amount) {
        this.amount = account_amount;
        this.amountSet = true;
    }

    public void resetAmount() {
        this.amountSet = false;
    }

    public boolean isAmountSet() {
        return amountSet;
    }
    public String getCurrency() {
        return currency;
    }
}
