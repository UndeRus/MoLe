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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.ktnx.mobileledger.utils.SimpleDate;

import org.jetbrains.annotations.NotNull;

public class TransactionListItem {
    private final Type type;
    private SimpleDate date;
    private boolean monthShown;
    private LedgerTransaction transaction;
    private String boldAccountName;
    private String runningTotal;
    public TransactionListItem(@NotNull SimpleDate date, boolean monthShown) {
        this.type = Type.DELIMITER;
        this.date = date;
        this.monthShown = monthShown;
    }
    public TransactionListItem(@NotNull LedgerTransaction transaction,
                               @Nullable String boldAccountName, @Nullable String runningTotal) {
        this.type = Type.TRANSACTION;
        this.transaction = transaction;
        this.boldAccountName = boldAccountName;
        this.runningTotal = runningTotal;
    }
    public TransactionListItem() {
        this.type = Type.HEADER;
    }
    public String getRunningTotal() {
        return runningTotal;
    }
    @NonNull
    public Type getType() {
        return type;
    }
    @NonNull
    public SimpleDate getDate() {
        if (date != null)
            return date;
        if (type != Type.TRANSACTION)
            throw new IllegalStateException("Only transaction items have a date");
        return transaction.getDate();
    }
    public boolean isMonthShown() {
        return monthShown;
    }
    @NotNull
    public LedgerTransaction getTransaction() {
        if (type != Type.TRANSACTION)
            throw new IllegalStateException(
                    String.format("Item type is not %s, but %s", Type.TRANSACTION, type));
        return transaction;
    }
    public @Nullable
    String getBoldAccountName() {
        return boldAccountName;
    }
    public enum Type {
        TRANSACTION, DELIMITER, HEADER;
        public static Type valueOf(int i) {
            if (i == TRANSACTION.ordinal())
                return TRANSACTION;
            else if (i == DELIMITER.ordinal())
                return DELIMITER;
            else if (i == HEADER.ordinal())
                return HEADER;
            else
                throw new IllegalStateException("Unexpected value: " + i);
        }
    }
}
