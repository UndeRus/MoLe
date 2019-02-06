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

package net.ktnx.mobileledger.async;

import net.ktnx.mobileledger.model.LedgerAccount;

import java.util.List;

public class CommitAccountsTaskParams {
    List<LedgerAccount> accountList;
    boolean showOnlyStarred;
    public CommitAccountsTaskParams(List<LedgerAccount> accountList, boolean showOnlyStarred) {
        this.accountList = accountList;
        this.showOnlyStarred = showOnlyStarred;
    }
}
