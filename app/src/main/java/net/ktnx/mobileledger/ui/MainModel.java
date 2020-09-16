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

package net.ktnx.mobileledger.ui;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import net.ktnx.mobileledger.App;
import net.ktnx.mobileledger.async.RetrieveTransactionsTask;
import net.ktnx.mobileledger.async.TransactionAccumulator;
import net.ktnx.mobileledger.async.UpdateTransactionsTask;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerAccount;
import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
import net.ktnx.mobileledger.model.TransactionListItem;
import net.ktnx.mobileledger.utils.LockHolder;
import net.ktnx.mobileledger.utils.Locker;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.MLDB;
import net.ktnx.mobileledger.utils.SimpleDate;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static net.ktnx.mobileledger.utils.Logger.debug;

public class MainModel extends ViewModel {
    public final MutableLiveData<Integer> foundTransactionItemIndex = new MutableLiveData<>(null);
    public final MutableLiveData<Date> lastUpdateDate = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> updatingFlag = new MutableLiveData<>(false);
    private final MutableLiveData<String> accountFilter = new MutableLiveData<>();
    private final MutableLiveData<List<TransactionListItem>> displayedTransactions =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<LedgerAccount>> displayedAccounts = new MutableLiveData<>();
    private final Locker accountsLocker = new Locker();
    private final MutableLiveData<String> updateError = new MutableLiveData<>();
    private MobileLedgerProfile profile;
    private List<LedgerAccount> allAccounts = new ArrayList<>();
    private final Map<String, LedgerAccount> accountMap = new HashMap<>();
    private SimpleDate firstTransactionDate;
    private SimpleDate lastTransactionDate;
    transient private RetrieveTransactionsTask retrieveTransactionsTask;
    transient private Thread displayedAccountsUpdater;
    transient private AccountListLoader loader = null;
    private TransactionsDisplayedFilter displayedTransactionsUpdater;
    public static ArrayList<LedgerAccount> mergeAccountListsFromWeb(List<LedgerAccount> oldList,
                                                                    List<LedgerAccount> newList) {
        LedgerAccount oldAcc, newAcc;
        ArrayList<LedgerAccount> merged = new ArrayList<>();

        Iterator<LedgerAccount> oldIterator = oldList.iterator();
        Iterator<LedgerAccount> newIterator = newList.iterator();

        while (true) {
            if (!oldIterator.hasNext()) {
                // the rest of the incoming are new
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    newIterator.forEachRemaining(merged::add);
                }
                else {
                    while (newIterator.hasNext())
                        merged.add(newIterator.next());
                }
                break;
            }
            oldAcc = oldIterator.next();

            if (!newIterator.hasNext()) {
                // no more incoming accounts. ignore the rest of the old
                break;
            }
            newAcc = newIterator.next();

            // ignore now missing old items
            if (oldAcc.getName()
                      .compareTo(newAcc.getName()) < 0)
                continue;

            // add newly found items
            if (oldAcc.getName()
                      .compareTo(newAcc.getName()) > 0)
            {
                merged.add(newAcc);
                continue;
            }

            // two items with same account names; forward-merge UI-controlled fields
            // it is important that the result list contains a new LedgerAccount instance
            // so that the change is propagated to the UI
            newAcc.setExpanded(oldAcc.isExpanded());
            newAcc.setAmountsExpanded(oldAcc.amountsExpanded());
            merged.add(newAcc);
        }

        return merged;
    }
    private void setLastUpdateStamp() {
        debug("db", "Updating transaction value stamp");
        Date now = new Date();
        profile.setLongOption(MLDB.OPT_LAST_SCRAPE, now.getTime());
        lastUpdateDate.postValue(now);
    }
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
    public void setProfile(MobileLedgerProfile profile) {
        stopTransactionsRetrieval();
        this.profile = profile;
    }
    public LiveData<List<TransactionListItem>> getDisplayedTransactions() {
        return displayedTransactions;
    }
    public void setDisplayedTransactions(List<TransactionListItem> list) {
        displayedTransactions.postValue(list);
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
    private void applyTransactionFilter(List<LedgerTransaction> list) {
        final String accFilter = accountFilter.getValue();
        ArrayList<TransactionListItem> newList = new ArrayList<>();

        TransactionAccumulator accumulator = new TransactionAccumulator(this);
        if (TextUtils.isEmpty(accFilter))
            for (LedgerTransaction tr : list)
                newList.add(new TransactionListItem(tr));
        else
            for (LedgerTransaction tr : list)
                if (tr.hasAccountNamedLike(accFilter))
                    newList.add(new TransactionListItem(tr));

        displayedTransactions.postValue(newList);
    }
    public synchronized void scheduleTransactionListRetrieval() {
        if (retrieveTransactionsTask != null) {
            Logger.debug("db", "Ignoring request for transaction retrieval - already active");
            return;
        }
        MobileLedgerProfile profile = Data.getProfile();

        retrieveTransactionsTask = new RetrieveTransactionsTask(this, profile, allAccounts);
        Logger.debug("db", "Created a background transaction retrieval task");

        retrieveTransactionsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    public synchronized void stopTransactionsRetrieval() {
        if (retrieveTransactionsTask != null)
            retrieveTransactionsTask.cancel(true);
    }
    public void transactionRetrievalDone() {
        retrieveTransactionsTask = null;
    }
    public synchronized Locker lockAccountsForWriting() {
        accountsLocker.lockForWriting();
        return accountsLocker;
    }
    public void mergeAccountListFromWeb(List<LedgerAccount> newList) {

        try (LockHolder l = accountsLocker.lockForWriting()) {
            allAccounts = mergeAccountListsFromWeb(allAccounts, newList);
            updateAccountsMap(allAccounts);
        }
    }
    public LiveData<List<LedgerAccount>> getDisplayedAccounts() {
        return displayedAccounts;
    }
    synchronized public void scheduleAccountListReload() {
        Logger.debug("async-acc", "scheduleAccountListReload() enter");
        if ((loader != null) && loader.isAlive()) {
            Logger.debug("async-acc", "returning early - loader already active");
            return;
        }

        loader = new AccountListLoader(profile, this);
        loader.start();
    }
    public synchronized void setAndStoreAccountAndTransactionListFromWeb(
            List<LedgerAccount> accounts, List<LedgerTransaction> transactions) {
        profile.storeAccountAndTransactionListAsync(accounts, transactions);

        setLastUpdateStamp();

        mergeAccountListFromWeb(accounts);
        updateDisplayedAccounts();

        updateDisplayedTransactionsFromWeb(transactions);
    }
    synchronized public void abortAccountListReload() {
        if (loader == null)
            return;
        loader.interrupt();
        loader = null;
    }
    synchronized public void updateDisplayedAccounts() {
        if (displayedAccountsUpdater != null) {
            displayedAccountsUpdater.interrupt();
        }
        displayedAccountsUpdater = new AccountListDisplayedFilter(this, allAccounts);
        displayedAccountsUpdater.start();
    }
    synchronized public void updateDisplayedTransactionsFromWeb(List<LedgerTransaction> list) {
        if (displayedTransactionsUpdater != null) {
            displayedTransactionsUpdater.interrupt();
        }
        displayedTransactionsUpdater = new TransactionsDisplayedFilter(this, list);
        displayedTransactionsUpdater.start();
    }
    public List<LedgerAccount> getAllAccounts() {
        return allAccounts;
    }
    private void updateAccountsMap(List<LedgerAccount> newAccounts) {
        accountMap.clear();
        for (LedgerAccount acc : newAccounts) {
            accountMap.put(acc.getName(), acc);
        }
    }
    @Nullable
    public LedgerAccount locateAccount(String name) {
        return accountMap.get(name);
    }
    public void clearUpdateError() {
        updateError.postValue(null);
    }
    public void clearTransactions() {
        displayedTransactions.setValue(new ArrayList<>());
    }

    static class AccountListLoader extends Thread {
        private final MobileLedgerProfile profile;
        private final MainModel model;
        AccountListLoader(MobileLedgerProfile profile, MainModel model) {
            this.profile = profile;
            this.model = model;
        }
        @Override
        public void run() {
            Logger.debug("async-acc", "AccountListLoader::run() entered");
            String profileUUID = profile.getUuid();
            ArrayList<LedgerAccount> list = new ArrayList<>();
            HashMap<String, LedgerAccount> map = new HashMap<>();

            String sql = "SELECT a.name, a.expanded, a.amounts_expanded";
            sql += " from accounts a WHERE a.profile = ?";
            sql += " ORDER BY a.name";

            SQLiteDatabase db = App.getDatabase();
            Logger.debug("async-acc", "AccountListLoader::run() connected to DB");
            try (Cursor cursor = db.rawQuery(sql, new String[]{profileUUID})) {
                Logger.debug("async-acc", "AccountListLoader::run() executed query");
                while (cursor.moveToNext()) {
                    if (isInterrupted())
                        return;

                    final String accName = cursor.getString(0);
//                    debug("accounts",
//                            String.format("Read account '%s' from DB [%s]", accName,
//                            profileUUID));
                    String parentName = LedgerAccount.extractParentName(accName);
                    LedgerAccount parent;
                    if (parentName != null) {
                        parent = map.get(parentName);
                        if (parent == null)
                            throw new IllegalStateException(
                                    String.format("Can't load account '%s': parent '%s' not loaded",
                                            accName, parentName));
                        parent.setHasSubAccounts(true);
                    }
                    else
                        parent = null;

                    LedgerAccount acc = new LedgerAccount(profile, accName, parent);
                    acc.setExpanded(cursor.getInt(1) == 1);
                    acc.setAmountsExpanded(cursor.getInt(2) == 1);
                    acc.setHasSubAccounts(false);

                    try (Cursor c2 = db.rawQuery(
                            "SELECT value, currency FROM account_values WHERE profile = ?" + " " +
                            "AND account = ?", new String[]{profileUUID, accName}))
                    {
                        while (c2.moveToNext()) {
                            acc.addAmount(c2.getFloat(0), c2.getString(1));
                        }
                    }

                    list.add(acc);
                    map.put(accName, acc);
                }
                Logger.debug("async-acc", "AccountListLoader::run() query execution done");
            }

            if (isInterrupted())
                return;

            Logger.debug("async-acc", "AccountListLoader::run() posting new list");
            model.allAccounts = list;
            model.updateAccountsMap(list);
            model.updateDisplayedAccounts();
        }
    }

    static class AccountListDisplayedFilter extends Thread {
        private final MainModel model;
        private final List<LedgerAccount> list;
        AccountListDisplayedFilter(MainModel model, List<LedgerAccount> list) {
            this.model = model;
            this.list = list;
        }
        @Override
        public void run() {
            List<LedgerAccount> newDisplayed = new ArrayList<>();
            Logger.debug("dFilter", "waiting for synchronized block");
            Logger.debug("dFilter", String.format(Locale.US,
                    "entered synchronized block (about to examine %d accounts)", list.size()));
            for (LedgerAccount a : list) {
                if (isInterrupted()) {
                    return;
                }

                if (a.isVisible()) {
                    newDisplayed.add(a);
                }
            }
            if (!isInterrupted()) {
                model.displayedAccounts.postValue(newDisplayed);
            }
            Logger.debug("dFilter", "left synchronized block");
        }
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

            TransactionAccumulator acc = new TransactionAccumulator(model);
            for (LedgerTransaction tr : list) {
                if (isInterrupted()) {
                    return;
                }

                if (accNameFilter == null || tr.hasAccountNamedLike(accNameFilter)) {
                    acc.put(tr, tr.getDate());
                }
            }
            if (!isInterrupted()) {
                acc.done();
            }
            Logger.debug("dFilter", "left synchronized block");
        }
    }
}
