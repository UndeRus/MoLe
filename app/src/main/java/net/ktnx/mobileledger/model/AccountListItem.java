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

import org.jetbrains.annotations.NotNull;

public class AccountListItem {
    private AccountListItem() {}
    @NonNull
    public Type getType() {
        if (this instanceof Account)
            return Type.ACCOUNT;
        else if (this instanceof Header)
            return Type.HEADER;
        else
            throw new RuntimeException("Unsupported sub-class " + this);
    }
    @NotNull
    public LedgerAccount getAccount() {
        if (this instanceof Account)
            return ((Account) this).account;

        throw new IllegalStateException(String.format("Item type is not Account, but %s", this));
    }
    public enum Type {ACCOUNT, HEADER}

    public static class Account extends AccountListItem {
        private final LedgerAccount account;
        public Account(@NotNull LedgerAccount account) {
            this.account = account;
        }
    }

    public static class Header extends AccountListItem {
        public Header() {
        }
    }
}
