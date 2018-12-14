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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class LedgerTransaction {
    private String date;
    private String description;
    private List<LedgerTransactionItem> items;

    LedgerTransaction(String date, String description) {
        this.date = date;
        this.description = description;
        this.items = new ArrayList<>();
    }

    void add_item(LedgerTransactionItem item) {
        items.add(item);
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    Iterator<LedgerTransactionItem> getItemsIterator() {
        return new Iterator<LedgerTransactionItem>() {
            private int pointer = 0;
            @Override
            public boolean hasNext() {
                return pointer < items.size();
            }

            @Override
            public LedgerTransactionItem next() {
                return hasNext() ? items.get(pointer++) : null;
            }
        };
    }
}
