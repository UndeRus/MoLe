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

package net.ktnx.mobileledger.model;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

public class LedgerAmount {
    private final String currency;
    private final float amount;

    public LedgerAmount(float amount, @NonNull String currency) {
        this.currency = currency;
        this.amount = amount;
    }

    public LedgerAmount(float amount) {
        this.amount = amount;
        this.currency = null;
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
