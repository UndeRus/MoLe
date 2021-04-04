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

package net.ktnx.mobileledger.model;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import net.ktnx.mobileledger.dao.AccountValueDAO;
import net.ktnx.mobileledger.db.Account;
import net.ktnx.mobileledger.db.AccountValue;
import net.ktnx.mobileledger.db.DB;

public class LedgerAmount {
    private final String currency;
    private final float amount;
    private long dbId;

    public LedgerAmount(float amount, @NonNull String currency) {
        this.currency = currency;
        this.amount = amount;
    }
    public LedgerAmount(float amount) {
        this.amount = amount;
        this.currency = null;
    }
    static public LedgerAmount fromDBO(AccountValue dbo) {
        final LedgerAmount ledgerAmount = new LedgerAmount(dbo.getValue(), dbo.getCurrency());
        ledgerAmount.dbId = dbo.getId();
        return ledgerAmount;
    }
    public AccountValue toDBO(Account account) {
        final AccountValueDAO dao = DB.get()
                                      .getAccountValueDAO();
        AccountValue obj = new AccountValue();
        obj.setId(dbId);
        obj.setAccountId(account.getId());

        obj.setCurrency(currency);
        obj.setValue(amount);

        return obj;
    }
    @SuppressLint("DefaultLocale")
    @NonNull
    public String toString() {
        if (currency == null)
            return String.format("%,1.2f", amount);
        else
            return String.format("%s %,1.2f", currency, amount);
    }
    public void propagateToAccount(@NonNull LedgerAccount acc) {
        if (currency != null)
            acc.addAmount(amount, currency);
        else
            acc.addAmount(amount);
    }
    public String getCurrency() {
        return currency;
    }
    public float getAmount() {
        return amount;
    }
}
