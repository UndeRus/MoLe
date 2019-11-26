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

import net.ktnx.mobileledger.json.ParsedAmount;
import net.ktnx.mobileledger.json.ParsedPosting;
import net.ktnx.mobileledger.json.ParsedQuantity;
import net.ktnx.mobileledger.json.ParsedStyle;

import java.util.ArrayList;

public class LedgerTransactionAccount {
    private String accountName;
    private String shortAccountName;
    private float amount;
    private boolean amountSet;
    private String currency;

    public LedgerTransactionAccount(String accountName, float amount) {
        this(accountName, amount, null);
    }
    public LedgerTransactionAccount(String accountName, float amount, String currency) {
        this.setAccountName(accountName);
        this.amount = amount;
        this.amountSet = true;
        this.currency = currency;
    }

    public LedgerTransactionAccount(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountName() {
        return accountName;
    }
    public String getShortAccountName() {
        return shortAccountName;
    }
    public void setAccountName(String accountName) {
        this.accountName = accountName;
        shortAccountName = accountName.replaceAll("(?<=^|:)(.)[^:]+(?=:)", "$1");
    }

    public float getAmount() {
        if (!amountSet) throw new IllegalStateException("Account amount is not set");

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
    @NonNull
    public String toString() {
        if (!amountSet) return "";

        StringBuilder sb = new StringBuilder();
        if (currency != null) {
            sb.append(currency);
            sb.append(' ');
        }
        sb.append(String.format("%,1.2f", amount));

        return sb.toString();
    }
    public ParsedPosting asParsedPosting() {
        ParsedPosting result = new ParsedPosting();
        result.setPaccount(accountName);
        ArrayList<ParsedAmount> amounts = new ArrayList<>();
        ParsedAmount amt = new ParsedAmount();
        amt.setAcommodity((currency == null) ? "" : currency);
        amt.setAismultiplier(false);
        ParsedQuantity qty = new ParsedQuantity();
        qty.setDecimalPlaces(2);
        qty.setDecimalMantissa(Math.round(amount * 100));
        amt.setAquantity(qty);
        ParsedStyle style = new ParsedStyle();
        style.setAscommodityside('L');
        style.setAscommodityspaced(false);
        style.setAsprecision(2);
        style.setAsdecimalpoint('.');
        amt.setAstyle(style);
        amounts.add(amt);
        result.setPamount(amounts);
        return result;
    }
}
