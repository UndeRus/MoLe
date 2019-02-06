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

package net.ktnx.mobileledger.ui.transaction_list;

import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.model.LedgerTransactionAccount;

class TransactionLoaderStep {
    private int position;
    private int accountCount;
    private TransactionListAdapter.LoaderStep step;
    private TransactionRowHolder holder;
    private LedgerTransaction transaction;
    private LedgerTransactionAccount account;
    private int accountPosition;
    private String boldAccountName;
    private boolean odd;
    public TransactionLoaderStep(TransactionRowHolder holder, int position,
                                 LedgerTransaction transaction, boolean isOdd) {
        this.step = TransactionListAdapter.LoaderStep.HEAD;
        this.holder = holder;
        this.transaction = transaction;
        this.position = position;
        this.odd = isOdd;
    }
    public TransactionLoaderStep(TransactionRowHolder holder, LedgerTransactionAccount account,
                                 int accountPosition, String boldAccountName) {
        this.step = TransactionListAdapter.LoaderStep.ACCOUNTS;
        this.holder = holder;
        this.account = account;
        this.accountPosition = accountPosition;
        this.boldAccountName = boldAccountName;
    }
    public TransactionLoaderStep(TransactionRowHolder holder, int position, int accountCount) {
        this.step = TransactionListAdapter.LoaderStep.DONE;
        this.holder = holder;
        this.position = position;
        this.accountCount = accountCount;
    }
    public int getAccountCount() {
        return accountCount;
    }
    public int getPosition() {
        return position;
    }
    public String getBoldAccountName() {
        return boldAccountName;
    }
    public int getAccountPosition() {
        return accountPosition;
    }
    public TransactionRowHolder getHolder() {
        return holder;
    }
    public TransactionListAdapter.LoaderStep getStep() {
        return step;
    }
    public LedgerTransaction getTransaction() {
        return transaction;
    }
    public LedgerTransactionAccount getAccount() {
        return account;
    }
    public boolean isOdd() {
        return odd;
    }
}
