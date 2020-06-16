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

import androidx.annotation.NonNull;

import net.ktnx.mobileledger.utils.SimpleDate;

public class TransactionListItem {
    private Type type;
    private SimpleDate date;
    private boolean monthShown;
    private LedgerTransaction transaction;
    private boolean odd;
    public TransactionListItem(SimpleDate date, boolean monthShown) {
        this.type = Type.DELIMITER;
        this.date = date;
        this.monthShown = monthShown;
    }
    public TransactionListItem(LedgerTransaction transaction, boolean isOdd) {
        this.type = Type.TRANSACTION;
        this.transaction = transaction;
        this.odd = isOdd;
    }
    @NonNull
    public Type getType() {
        return type;
    }
    @NonNull
    public SimpleDate getDate() {
        return (date != null) ? date : transaction.getDate();
    }
    public boolean isMonthShown() {
        return monthShown;
    }
    public LedgerTransaction getTransaction() {
        return transaction;
    }
    public boolean isOdd() {
        return odd;
    }
    public enum Type {TRANSACTION, DELIMITER}
}
