/*
 * Copyright © 2018 Damyan Ivanov.
 * This file is part of Mobile-Ledger.
 * Mobile-Ledger is free software: you can distribute it and/or modify it
 * under the term of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your opinion), any later version.
 *
 * Mobile-Ledger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License terms for details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mobile-Ledger. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ktnx.mobileledger;

class LedgerTransactionItem {
    private String account_name;
    private float amount;
    private boolean amount_set;

    LedgerTransactionItem(String account_name, float amount) {
        this.account_name = account_name;
        this.amount = amount;
        this.amount_set = true;
    }

    public LedgerTransactionItem(String account_name) {
        this.account_name = account_name;
    }

    public String get_account_name() {
        return account_name;
    }

    public void set_account_name(String account_name) {
        this.account_name = account_name;
    }

    public float get_amount() {
        if (!amount_set)
            throw new IllegalStateException("Account amount is not set");

        return amount;
    }

    public void set_amount(float account_amount) {
        this.amount = account_amount;
        this.amount_set = true;
    }

    public void reset_amount() {
        this.amount_set = false;
    }

    public boolean is_amount_set() {
        return amount_set;
    }
}
