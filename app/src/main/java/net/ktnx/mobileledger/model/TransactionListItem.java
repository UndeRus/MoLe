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

import android.support.annotation.NonNull;

import java.util.Date;

public class TransactionListItem {
    private Type type;
    private Date date;
    private boolean monthShown;
    private LedgerTransaction transaction;
    private boolean odd;
    public TransactionListItem() {
        this.type = Type.TRAILER;
    }
    public TransactionListItem(Date date, boolean monthShown) {
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
    public Date getDate() {
        return date;
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
    public enum Type {TRANSACTION, DELIMITER, TRAILER}
}
