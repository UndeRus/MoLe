/*
 * Copyright © 2019 Damyan Ivanov.
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

package net.ktnx.mobileledger.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class Data {
    public static TransactionList transactions = new TransactionList();
    public static ObservableValue<ArrayList<LedgerAccount>> accounts =
            new ObservableValue<>(new ArrayList<LedgerAccount>());
    public static ObservableValue<List<String>> descriptions = new ObservableValue<>();
    public static ObservableAtomicInteger backgroundTaskCount = new ObservableAtomicInteger(0);
    public static ObservableValue<Date> lastUpdateDate = new ObservableValue<>();
    public static ObservableValue<String> ledgerTitle = new ObservableValue<>();
}
