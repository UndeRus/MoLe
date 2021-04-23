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

package net.ktnx.mobileledger.ui;

import android.os.AsyncTask;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import net.ktnx.mobileledger.async.RetrieveTransactionsTask;
import net.ktnx.mobileledger.async.TransactionAccumulator;
import net.ktnx.mobileledger.async.UpdateTransactionsTask;
import net.ktnx.mobileledger.db.Profile;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerAccount;
import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.model.TransactionListItem;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.SimpleDate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainModel extends ViewModel {
    public final MutableLiveData<Integer> foundTransactionItemIndex = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> updatingFlag = new MutableLiveData<>(false);
    private final MutableLiveData<String> accountFilter = new MutableLiveData<>();
    private final MutableLiveData<List<TransactionListItem>> displayedTransactions =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> updateError = new MutableLiveData<>();
    private SimpleDate firstTransactionDate;
    private SimpleDate lastTransactionDate;
    transient private RetrieveTransactionsTask retrieveTransactionsTask;
    transient private Thread displayedAccountsUpdater;
    private TransactionsDisplayedFilter displayedTransactionsUpdater;
    public void scheduleTransactionListReload() {
        UpdateTransactionsTask task = new UpdateTransactionsTask();
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this);
    }
    public LiveData<Boolean> getUpdatingFlag() {
        return updatingFlag;
    }
    public LiveData<String> getUpdateError() {
        return updateError;
    }
    public LiveData<List<TransactionListItem>> getDisplayedTransactions() {
        return displayedTransactions;
    }
    public void setDisplayedTransactions(List<TransactionListItem> list) {
        displayedTransactions.postValue(list);
        Data.lastUpdateTransactionCount.postValue(list.size());
    }
    public SimpleDate getFirstTransactionDate() {
        return firstTransactionDate;
    }
    public void setFirstTransactionDate(SimpleDate earliestDate) {
        this.firstTransactionDate = earliestDate;
    }
    public MutableLiveData<String> getAccountFilter() {
        return accountFilter;
    }
    public SimpleDate getLastTransactionDate() {
        return lastTransactionDate;
    }
    public void setLastTransactionDate(SimpleDate latestDate) {
        this.lastTransactionDate = latestDate;
    }
    public synchronized void scheduleTransactionListRetrieval() {
        if (retrieveTransactionsTask != null) {
            Logger.debug("db", "Ignoring request for transaction retrieval - already active");
            return;
        }
        Profile profile = Data.getProfile();

        retrieveTransactionsTask = new RetrieveTransactionsTask(this, profile);
        Logger.debug("db", "Created a background transaction retrieval task");

        retrieveTransactionsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    public synchronized void stopTransactionsRetrieval() {
        if (retrieveTransactionsTask != null)
            retrieveTransactionsTask.cancel(true);
        else
            Data.backgroundTaskProgress.setValue(null);
    }
    public void transactionRetrievalDone() {
        retrieveTransactionsTask = null;
    }
    synchronized public void updateDisplayedTransactionsFromWeb(List<LedgerTransaction> list) {
        if (displayedTransactionsUpdater != null) {
            displayedTransactionsUpdater.interrupt();
        }
        displayedTransactionsUpdater = new TransactionsDisplayedFilter(this, list);
        displayedTransactionsUpdater.start();
    }
    public void clearUpdateError() {
        updateError.postValue(null);
    }
    public void clearTransactions() {
        displayedTransactions.setValue(new ArrayList<>());
    }

    static class TransactionsDisplayedFilter extends Thread {
        private final MainModel model;
        private final List<LedgerTransaction> list;
        TransactionsDisplayedFilter(MainModel model, List<LedgerTransaction> list) {
            this.model = model;
            this.list = list;
        }
        @Override
        public void run() {
            List<LedgerAccount> newDisplayed = new ArrayList<>();
            Logger.debug("dFilter", "waiting for synchronized block");
            Logger.debug("dFilter", String.format(Locale.US,
                    "entered synchronized block (about to examine %d transactions)", list.size()));
            String accNameFilter = model.getAccountFilter()
                                        .getValue();

            TransactionAccumulator acc = new TransactionAccumulator(accNameFilter);
            for (LedgerTransaction tr : list) {
                if (isInterrupted()) {
                    return;
                }

                if (accNameFilter == null || tr.hasAccountNamedLike(accNameFilter)) {
                    acc.put(tr, tr.getDate());
                }
            }

            if (isInterrupted())
                return;

            acc.publishResults(model);
            Logger.debug("dFilter", "transaction list updated");
        }
    }
}
