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

import org.jetbrains.annotations.NotNull;

public class AccountListItem {
    private final Type type;
    private LedgerAccount account;
    public AccountListItem(@NotNull LedgerAccount account) {
        this.type = Type.ACCOUNT;
        this.account = account;
    }
    public AccountListItem() {
        this.type = Type.HEADER;
    }
    @NonNull
    public Type getType() {
        return type;
    }
    @NotNull
    public LedgerAccount getAccount() {
        if (type != Type.ACCOUNT)
            throw new IllegalStateException(
                    String.format("Item type is not %s, but %s", Type.ACCOUNT, type));
        return account;
    }
    public enum Type {ACCOUNT, HEADER}
}
