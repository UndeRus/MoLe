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
import androidx.lifecycle.LiveData;

import org.jetbrains.annotations.NotNull;

public abstract class AccountListItem {
    private AccountListItem() {}
    public abstract boolean sameContent(AccountListItem other);
    @NonNull
    public Type getType() {
        if (this instanceof Account)
            return Type.ACCOUNT;
        else if (this instanceof Header)
            return Type.HEADER;
        else
            throw new RuntimeException("Unsupported sub-class " + this);
    }
    public boolean isAccount() {
        return this instanceof Account;
    }
    public Account toAccount() {
        assert isAccount();
        return ((Account) this);
    }
    public boolean isHeader() {
        return this instanceof Header;
    }
    public Header toHeader() {
        assert isHeader();
        return ((Header) this);
    }
    public enum Type {ACCOUNT, HEADER}

    public static class Account extends AccountListItem {
        private final LedgerAccount account;
        public Account(@NotNull LedgerAccount account) {
            this.account = account;
        }
        @Override
        public boolean sameContent(AccountListItem other) {
            if (!(other instanceof Account))
                return false;
            return ((Account) other).account.hasSubAccounts() == account.hasSubAccounts() &&
                   ((Account) other).account.amountsExpanded() == account.amountsExpanded() &&
                   ((Account) other).account.isExpanded() == account.isExpanded() &&
                   ((Account) other).account.getLevel() == account.getLevel() &&
                   ((Account) other).account.getAmountsString()
                                            .equals(account.getAmountsString());
        }
        @NotNull
        public LedgerAccount getAccount() {
            return account;
        }
    }

    public static class Header extends AccountListItem {
        private final LiveData<String> text;
        public Header(@NonNull LiveData<String> text) {
            this.text = text;
        }
        public LiveData<String> getText() {
            return text;
        }
        @Override
        public boolean sameContent(AccountListItem other) {
            return true;
        }
    }
}
