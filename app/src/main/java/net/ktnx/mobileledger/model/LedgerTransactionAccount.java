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

package net.ktnx.mobileledger.model;

import androidx.annotation.NonNull;

import net.ktnx.mobileledger.utils.Misc;

import java.util.Locale;

public class LedgerTransactionAccount {
    private String accountName;
    private String shortAccountName;
    private float amount;
    private boolean amountSet = false;
    private String currency;
    private String comment;
    private boolean amountValid = true;
    public LedgerTransactionAccount(String accountName, float amount, String currency,
                                    String comment) {
        this.setAccountName(accountName);
        this.amount = amount;
        this.amountSet = true;
        this.amountValid = true;
        this.currency = Misc.emptyIsNull(currency);
        this.comment = Misc.emptyIsNull(comment);
    }
    public LedgerTransactionAccount(String accountName) {
        this.accountName = accountName;
    }
    public LedgerTransactionAccount(String accountName, String currency) {
        this.accountName = accountName;
        this.currency = Misc.emptyIsNull(currency);
    }
    public LedgerTransactionAccount(LedgerTransactionAccount origin) {
        // copy constructor
        setAccountName(origin.getAccountName());
        setComment(origin.getComment());
        if (origin.isAmountSet())
            setAmount(origin.getAmount());
        amountValid = origin.amountValid;
        currency = origin.getCurrency();
    }
    public String getComment() {
        return comment;
    }
    public void setComment(String comment) {
        this.comment = comment;
    }
    public String getAccountName() {
        return accountName;
    }
    public void setAccountName(String accountName) {
        this.accountName = accountName;
        shortAccountName = accountName.replaceAll("(?<=^|:)(.)[^:]+(?=:)", "$1");
    }
    public String getShortAccountName() {
        return shortAccountName;
    }
    public float getAmount() {
        if (!amountSet)
            throw new IllegalStateException("Account amount is not set");

        return amount;
    }
    public void setAmount(float account_amount) {
        this.amount = account_amount;
        this.amountSet = true;
        this.amountValid = true;
    }
    public void resetAmount() {
        this.amountSet = false;
        this.amountValid = true;
    }
    public void invalidateAmount() {
        this.amountValid = false;
    }
    public boolean isAmountSet() {
        return amountSet;
    }
    public boolean isAmountValid() { return amountValid; }
    public String getCurrency() {
        return currency;
    }
    public void setCurrency(String currency) {
        this.currency = Misc.emptyIsNull(currency);
    }
    @NonNull
    public String toString() {
        if (!amountSet)
            return "";

        StringBuilder sb = new StringBuilder();
        if (currency != null) {
            sb.append(currency);
            sb.append(' ');
        }
        sb.append(String.format(Locale.US, "%,1.2f", amount));

        return sb.toString();
    }
}