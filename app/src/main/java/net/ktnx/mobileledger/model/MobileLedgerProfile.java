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

import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.text.TextUtils;
import android.util.SparseArray;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import net.ktnx.mobileledger.App;
import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.async.DbOpQueue;
import net.ktnx.mobileledger.async.SendTransactionTask;
import net.ktnx.mobileledger.utils.LockHolder;
import net.ktnx.mobileledger.utils.Locker;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.MLDB;
import net.ktnx.mobileledger.utils.Misc;

import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static net.ktnx.mobileledger.utils.Logger.debug;

public final class MobileLedgerProfile {
    private final MutableLiveData<List<LedgerAccount>> displayedAccounts;
    private final MutableLiveData<List<LedgerTransaction>> allTransactions;
    private final MutableLiveData<List<LedgerTransaction>> displayedTransactions;
    // N.B. when adding new fields, update the copy-constructor below
    private final String uuid;
    private final Locker accountsLocker = new Locker();
    private List<LedgerAccount> allAccounts;
    private String name;
    private boolean permitPosting;
    private boolean showCommentsByDefault;
    private boolean showCommodityByDefault;
    private String defaultCommodity;
    private String preferredAccountsFilter;
    private String url;
    private boolean authEnabled;
    private String authUserName;
    private String authPassword;
    private int themeHue;
    private int orderNo = -1;
    private SendTransactionTask.API apiVersion = SendTransactionTask.API.auto;
    private Calendar firstTransactionDate;
    private Calendar lastTransactionDate;
    private FutureDates futureDates = FutureDates.None;
    private boolean accountsLoaded;
    private boolean transactionsLoaded;
    // N.B. when adding new fields, update the copy-constructor below
    transient private AccountListLoader loader = null;
    transient private Thread displayedAccountsUpdater;
    transient private AccountListSaver accountListSaver;
    transient private TransactionListSaver transactionListSaver;
    transient private AccountAndTransactionListSaver accountAndTransactionListSaver;
    private Map<String, LedgerAccount> accountMap = new HashMap<>();
    public MobileLedgerProfile(String uuid) {
        this.uuid = uuid;
        allAccounts = new ArrayList<>();
        displayedAccounts = new MutableLiveData<>();
        allTransactions = new MutableLiveData<>(new ArrayList<>());
        displayedTransactions = new MutableLiveData<>(new ArrayList<>());
    }
    public MobileLedgerProfile(MobileLedgerProfile origin) {
        uuid = origin.uuid;
        name = origin.name;
        permitPosting = origin.permitPosting;
        showCommentsByDefault = origin.showCommentsByDefault;
        showCommodityByDefault = origin.showCommodityByDefault;
        preferredAccountsFilter = origin.preferredAccountsFilter;
        url = origin.url;
        authEnabled = origin.authEnabled;
        authUserName = origin.authUserName;
        authPassword = origin.authPassword;
        themeHue = origin.themeHue;
        orderNo = origin.orderNo;
        futureDates = origin.futureDates;
        apiVersion = origin.apiVersion;
        defaultCommodity = origin.defaultCommodity;
        firstTransactionDate = origin.firstTransactionDate;
        lastTransactionDate = origin.lastTransactionDate;
        displayedAccounts = origin.displayedAccounts;
        allAccounts = origin.allAccounts;
        accountMap = origin.accountMap;
        displayedTransactions = origin.displayedTransactions;
        allTransactions = origin.allTransactions;
        accountsLoaded = origin.accountsLoaded;
        transactionsLoaded = origin.transactionsLoaded;
    }
    // loads all profiles into Data.profiles
    // returns the profile with the given UUID
    public static MobileLedgerProfile loadAllFromDB(@Nullable String currentProfileUUID) {
        MobileLedgerProfile result = null;
        ArrayList<MobileLedgerProfile> list = new ArrayList<>();
        SQLiteDatabase db = App.getDatabase();
        try (Cursor cursor = db.rawQuery("SELECT uuid, name, url, use_authentication, auth_user, " +
                                         "auth_password, permit_posting, theme, order_no, " +
                                         "preferred_accounts_filter, future_dates, api_version, " +
                                         "show_commodity_by_default, default_commodity, " +
                                         "show_comments_by_default FROM " +
                                         "profiles order by order_no", null))
        {
            while (cursor.moveToNext()) {
                MobileLedgerProfile item = new MobileLedgerProfile(cursor.getString(0));
                item.setName(cursor.getString(1));
                item.setUrl(cursor.getString(2));
                item.setAuthEnabled(cursor.getInt(3) == 1);
                item.setAuthUserName(cursor.getString(4));
                item.setAuthPassword(cursor.getString(5));
                item.setPostingPermitted(cursor.getInt(6) == 1);
                item.setThemeId(cursor.getInt(7));
                item.orderNo = cursor.getInt(8);
                item.setPreferredAccountsFilter(cursor.getString(9));
                item.setFutureDates(cursor.getInt(10));
                item.setApiVersion(cursor.getInt(11));
                item.setShowCommodityByDefault(cursor.getInt(12) == 1);
                item.setDefaultCommodity(cursor.getString(13));
                item.setShowCommentsByDefault(cursor.getInt(14) == 1);
                list.add(item);
                if (item.getUuid()
                        .equals(currentProfileUUID))
                    result = item;
            }
        }
        Data.profiles.setValue(list);
        return result;
    }
    public static void storeProfilesOrder() {
        SQLiteDatabase db = App.getDatabase();
        db.beginTransactionNonExclusive();
        try {
            int orderNo = 0;
            for (MobileLedgerProfile p : Data.profiles.getValue()) {
                db.execSQL("update profiles set order_no=? where uuid=?",
                        new Object[]{orderNo, p.getUuid()});
                p.orderNo = orderNo;
                orderNo++;
            }
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }
    }
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
    public void mergeAccountListFromWeb(List<LedgerAccount> newList) {

        try (LockHolder l = accountsLocker.lockForWriting()) {
            allAccounts = mergeAccountListsFromWeb(allAccounts, newList);
            updateAccountsMap(allAccounts);
        }
    }
    public LiveData<List<LedgerAccount>> getDisplayedAccounts() {
        return displayedAccounts;
    }
    @Contract(value = "null -> false", pure = true)
    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (obj.getClass() != this.getClass())
            return false;

        MobileLedgerProfile p = (MobileLedgerProfile) obj;
        if (!uuid.equals(p.uuid))
            return false;
        if (!name.equals(p.name))
            return false;
        if (permitPosting != p.permitPosting)
            return false;
        if (showCommentsByDefault != p.showCommentsByDefault)
            return false;
        if (showCommodityByDefault != p.showCommodityByDefault)
            return false;
        if (!Objects.equals(defaultCommodity, p.defaultCommodity))
            return false;
        if (!Objects.equals(preferredAccountsFilter, p.preferredAccountsFilter))
            return false;
        if (!Objects.equals(url, p.url))
            return false;
        if (authEnabled != p.authEnabled)
            return false;
        if (!Objects.equals(authUserName, p.authUserName))
            return false;
        if (!Objects.equals(authPassword, p.authPassword))
            return false;
        if (themeHue != p.themeHue)
            return false;
        if (apiVersion != p.apiVersion)
            return false;
        if (!Objects.equals(firstTransactionDate, p.firstTransactionDate))
            return false;
        if (!Objects.equals(lastTransactionDate, p.lastTransactionDate))
            return false;
        return futureDates == p.futureDates;
    }
    synchronized public void scheduleAccountListReload() {
        Logger.debug("async-acc", "scheduleAccountListReload() enter");
        if ((loader != null) && loader.isAlive()) {
            Logger.debug("async-acc", "returning early - loader already active");
            return;
        }

        Logger.debug("async-acc", "Starting AccountListLoader");
        loader = new AccountListLoader(this);
        loader.start();
    }
    synchronized public void abortAccountListReload() {
        if (loader == null)
            return;
        loader.interrupt();
        loader = null;
    }
    public boolean getShowCommentsByDefault() {
        return showCommentsByDefault;
    }
    public void setShowCommentsByDefault(boolean newValue) {
        this.showCommentsByDefault = newValue;
    }
    public boolean getShowCommodityByDefault() {
        return showCommodityByDefault;
    }
    public void setShowCommodityByDefault(boolean showCommodityByDefault) {
        this.showCommodityByDefault = showCommodityByDefault;
    }
    public String getDefaultCommodity() {
        return defaultCommodity;
    }
    public void setDefaultCommodity(String defaultCommodity) {
        this.defaultCommodity = defaultCommodity;
    }
    public void setDefaultCommodity(CharSequence defaultCommodity) {
        if (defaultCommodity == null)
            this.defaultCommodity = null;
        else
            this.defaultCommodity = String.valueOf(defaultCommodity);
    }
    public SendTransactionTask.API getApiVersion() {
        return apiVersion;
    }
    public void setApiVersion(SendTransactionTask.API apiVersion) {
        this.apiVersion = apiVersion;
    }
    public void setApiVersion(int apiVersion) {
        this.apiVersion = SendTransactionTask.API.valueOf(apiVersion);
    }
    public FutureDates getFutureDates() {
        return futureDates;
    }
    public void setFutureDates(int anInt) {
        futureDates = FutureDates.valueOf(anInt);
    }
    public void setFutureDates(FutureDates futureDates) {
        this.futureDates = futureDates;
    }
    public String getPreferredAccountsFilter() {
        return preferredAccountsFilter;
    }
    public void setPreferredAccountsFilter(String preferredAccountsFilter) {
        this.preferredAccountsFilter = preferredAccountsFilter;
    }
    public void setPreferredAccountsFilter(CharSequence preferredAccountsFilter) {
        setPreferredAccountsFilter(String.valueOf(preferredAccountsFilter));
    }
    public boolean isPostingPermitted() {
        return permitPosting;
    }
    public void setPostingPermitted(boolean permitPosting) {
        this.permitPosting = permitPosting;
    }
    public String getUuid() {
        return uuid;
    }
    public String getName() {
        return name;
    }
    public void setName(CharSequence text) {
        setName(String.valueOf(text));
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getUrl() {
        return url;
    }
    public void setUrl(CharSequence text) {
        setUrl(String.valueOf(text));
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public boolean isAuthEnabled() {
        return authEnabled;
    }
    public void setAuthEnabled(boolean authEnabled) {
        this.authEnabled = authEnabled;
    }
    public String getAuthUserName() {
        return authUserName;
    }
    public void setAuthUserName(CharSequence text) {
        setAuthUserName(String.valueOf(text));
    }
    public void setAuthUserName(String authUserName) {
        this.authUserName = authUserName;
    }
    public String getAuthPassword() {
        return authPassword;
    }
    public void setAuthPassword(CharSequence text) {
        setAuthPassword(String.valueOf(text));
    }
    public void setAuthPassword(String authPassword) {
        this.authPassword = authPassword;
    }
    public void storeInDB() {
        SQLiteDatabase db = App.getDatabase();
        db.beginTransactionNonExclusive();
        try {
//            debug("profiles", String.format("Storing profile in DB: uuid=%s, name=%s, " +
//                                            "url=%s, permit_posting=%s, authEnabled=%s, " +
//                                            "themeHue=%d", uuid, name, url,
//                    permitPosting ? "TRUE" : "FALSE", authEnabled ? "TRUE" : "FALSE", themeHue));
            db.execSQL("REPLACE INTO profiles(uuid, name, permit_posting, url, " +
                       "use_authentication, auth_user, auth_password, theme, order_no, " +
                       "preferred_accounts_filter, future_dates, api_version, " +
                       "show_commodity_by_default, default_commodity, show_comments_by_default) " +
                       "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new Object[]{uuid, name, permitPosting, url, authEnabled,
                                 authEnabled ? authUserName : null,
                                 authEnabled ? authPassword : null, themeHue, orderNo,
                                 preferredAccountsFilter, futureDates.toInt(), apiVersion.toInt(),
                                 showCommodityByDefault, defaultCommodity, showCommentsByDefault
                    });
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }
    }
    public void storeAccount(SQLiteDatabase db, LedgerAccount acc, boolean storeUiFields) {
        // replace into is a bad idea because it would reset hidden to its default value
        // we like the default, but for new accounts only
        String sql = "update accounts set keep = 1";
        List<Object> params = new ArrayList<>();
        if (storeUiFields) {
            sql += ", expanded=?";
            params.add(acc.isExpanded() ? 1 : 0);
        }
        sql += " where profile=? and name=?";
        params.add(uuid);
        params.add(acc.getName());
        db.execSQL(sql, params.toArray());

        db.execSQL("insert into accounts(profile, name, name_upper, parent_name, level, " +
                   "expanded, keep) " + "select ?,?,?,?,?,0,1 where (select changes() = 0)",
                new Object[]{uuid, acc.getName(), acc.getName().toUpperCase(), acc.getParentName(),
                             acc.getLevel()
                });
//        debug("accounts", String.format("Stored account '%s' in DB [%s]", acc.getName(), uuid));
    }
    public void storeAccountValue(SQLiteDatabase db, String name, String currency, Float amount) {
        db.execSQL("replace into account_values(profile, account, " +
                   "currency, value, keep) values(?, ?, ?, ?, 1);",
                new Object[]{uuid, name, Misc.emptyIsNull(currency), amount});
    }
    public void storeTransaction(SQLiteDatabase db, LedgerTransaction tr) {
        tr.fillDataHash();
        db.execSQL("DELETE from transactions WHERE profile=? and id=?",
                new Object[]{uuid, tr.getId()});
        db.execSQL("DELETE from transaction_accounts WHERE profile = ? and transaction_id=?",
                new Object[]{uuid, tr.getId()});

        db.execSQL("INSERT INTO transactions(profile, id, year, month, day, description, " +
                   "comment, data_hash, keep) values(?,?,?,?,?,?,?,?,1)",
                new Object[]{uuid, tr.getId(), tr.getDate().year, tr.getDate().month,
                             tr.getDate().day, tr.getDescription(), tr.getComment(),
                             tr.getDataHash()
                });

        for (LedgerTransactionAccount item : tr.getAccounts()) {
            db.execSQL("INSERT INTO transaction_accounts(profile, transaction_id, " +
                       "account_name, amount, currency, comment) values(?, ?, ?, ?, ?, ?)",
                    new Object[]{uuid, tr.getId(), item.getAccountName(), item.getAmount(),
                                 Misc.nullIsEmpty(item.getCurrency()), item.getComment()
                    });
        }
//        debug("profile", String.format("Transaction %d stored", tr.getId()));
    }
    public String getOption(String name, String default_value) {
        SQLiteDatabase db = App.getDatabase();
        try (Cursor cursor = db.rawQuery("select value from options where profile = ? and name=?",
                new String[]{uuid, name}))
        {
            if (cursor.moveToFirst()) {
                String result = cursor.getString(0);

                if (result == null) {
                    debug("profile", "returning default value for " + name);
                    result = default_value;
                }
                else
                    debug("profile", String.format("option %s=%s", name, result));

                return result;
            }
            else
                return default_value;
        }
        catch (Exception e) {
            debug("db", "returning default value for " + name, e);
            return default_value;
        }
    }
    public long getLongOption(String name, long default_value) {
        long longResult;
        String result = getOption(name, "");
        if ((result == null) || result.isEmpty()) {
            debug("profile", String.format("Returning default value for option %s", name));
            longResult = default_value;
        }
        else {
            try {
                longResult = Long.parseLong(result);
                debug("profile", String.format("option %s=%s", name, result));
            }
            catch (Exception e) {
                debug("profile", String.format("Returning default value for option %s", name), e);
                longResult = default_value;
            }
        }

        return longResult;
    }
    public void setOption(String name, String value) {
        debug("profile", String.format("setting option %s=%s", name, value));
        DbOpQueue.add("insert or replace into options(profile, name, value) values(?, ?, ?);",
                new String[]{uuid, name, value});
    }
    public void setLongOption(String name, long value) {
        setOption(name, String.valueOf(value));
    }
    public void removeFromDB() {
        SQLiteDatabase db = App.getDatabase();
        debug("db", String.format("removing profile %s from DB", uuid));
        db.beginTransactionNonExclusive();
        try {
            Object[] uuid_param = new Object[]{uuid};
            db.execSQL("delete from profiles where uuid=?", uuid_param);
            db.execSQL("delete from accounts where profile=?", uuid_param);
            db.execSQL("delete from account_values where profile=?", uuid_param);
            db.execSQL("delete from transactions where profile=?", uuid_param);
            db.execSQL("delete from transaction_accounts where profile=?", uuid_param);
            db.execSQL("delete from options where profile=?", uuid_param);
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }
    }
    public LedgerTransaction loadTransaction(int transactionId) {
        LedgerTransaction tr = new LedgerTransaction(transactionId, this.uuid);
        tr.loadData(App.getDatabase());

        return tr;
    }
    public int getThemeHue() {
//        debug("profile", String.format("Profile.getThemeHue() returning %d", themeHue));
        return this.themeHue;
    }
    public void setThemeHue(Object o) {
        setThemeId(Integer.parseInt(String.valueOf(o)));
    }
    public void setThemeId(int themeHue) {
//        debug("profile", String.format("Profile.setThemeHue(%d) called", themeHue));
        this.themeHue = themeHue;
    }
    public void markTransactionsAsNotPresent(SQLiteDatabase db) {
        db.execSQL("UPDATE transactions set keep=0 where profile=?", new String[]{uuid});

    }
    private void markAccountsAsNotPresent(SQLiteDatabase db) {
        db.execSQL("update account_values set keep=0 where profile=?;", new String[]{uuid});
        db.execSQL("update accounts set keep=0 where profile=?;", new String[]{uuid});

    }
    private void deleteNotPresentAccounts(SQLiteDatabase db) {
        db.execSQL("delete from account_values where keep=0 and profile=?", new String[]{uuid});
        db.execSQL("delete from accounts where keep=0 and profile=?", new String[]{uuid});
    }
    private void markTransactionAsPresent(SQLiteDatabase db, LedgerTransaction transaction) {
        db.execSQL("UPDATE transactions SET keep = 1 WHERE profile = ? and id=?",
                new Object[]{uuid, transaction.getId()
                });
    }
    private void markTransactionsBeforeTransactionAsPresent(SQLiteDatabase db,
                                                            LedgerTransaction transaction) {
        db.execSQL("UPDATE transactions SET keep=1 WHERE profile = ? and id < ?",
                new Object[]{uuid, transaction.getId()
                });

    }
    private void deleteNotPresentTransactions(SQLiteDatabase db) {
        db.execSQL("DELETE FROM transactions WHERE profile=? AND keep = 0", new String[]{uuid});
    }
    private void setLastUpdateStamp() {
        debug("db", "Updating transaction value stamp");
        Date now = new Date();
        setLongOption(MLDB.OPT_LAST_SCRAPE, now.getTime());
        Data.lastUpdateDate.postValue(now);
    }
    public void wipeAllData() {
        SQLiteDatabase db = App.getDatabase();
        db.beginTransaction();
        try {
            String[] pUuid = new String[]{uuid};
            db.execSQL("delete from options where profile=?", pUuid);
            db.execSQL("delete from accounts where profile=?", pUuid);
            db.execSQL("delete from account_values where profile=?", pUuid);
            db.execSQL("delete from transactions where profile=?", pUuid);
            db.execSQL("delete from transaction_accounts where profile=?", pUuid);
            db.setTransactionSuccessful();
            debug("wipe", String.format(Locale.ENGLISH, "Profile %s wiped out", pUuid[0]));
        }
        finally {
            db.endTransaction();
        }
    }
    public List<Currency> getCurrencies() {
        SQLiteDatabase db = App.getDatabase();

        ArrayList<Currency> result = new ArrayList<>();

        try (Cursor c = db.rawQuery("SELECT c.id, c.name, c.position, c.has_gap FROM currencies c",
                new String[]{}))
        {
            while (c.moveToNext()) {
                Currency currency = new Currency(c.getInt(0), c.getString(1),
                        Currency.Position.valueOf(c.getInt(2)), c.getInt(3) == 1);
                result.add(currency);
            }
        }

        return result;
    }
    Currency loadCurrencyByName(String name) {
        SQLiteDatabase db = App.getDatabase();
        Currency result = tryLoadCurrencyByName(db, name);
        if (result == null)
            throw new RuntimeException(String.format("Unable to load currency '%s'", name));
        return result;
    }
    private Currency tryLoadCurrencyByName(SQLiteDatabase db, String name) {
        try (Cursor cursor = db.rawQuery(
                "SELECT c.id, c.name, c.position, c.has_gap FROM currencies c WHERE c.name=?",
                new String[]{name}))
        {
            if (cursor.moveToFirst()) {
                return new Currency(cursor.getInt(0), cursor.getString(1),
                        Currency.Position.valueOf(cursor.getInt(2)), cursor.getInt(3) == 1);
            }
            return null;
        }
    }
    public Calendar getFirstTransactionDate() {
        return firstTransactionDate;
    }
    public Calendar getLastTransactionDate() {
        return lastTransactionDate;
    }
    private void applyTransactionFilter(List<LedgerTransaction> list) {
        final String accFilter = Data.accountFilter.getValue();
        if (TextUtils.isEmpty(accFilter)) {
            displayedTransactions.postValue(list);
        }
        else {
            ArrayList<LedgerTransaction> newList = new ArrayList<>();
            for (LedgerTransaction tr : list) {
                if (tr.hasAccountNamedLike(accFilter))
                    newList.add(tr);
            }
            displayedTransactions.postValue(newList);
        }
    }
    synchronized public void storeAccountListAsync(List<LedgerAccount> list,
                                                   boolean storeUiFields) {
        if (accountListSaver != null)
            accountListSaver.interrupt();
        accountListSaver = new AccountListSaver(this, list, storeUiFields);
        accountListSaver.start();
    }
    public void setAndStoreAccountListFromWeb(ArrayList<LedgerAccount> list) {
        SQLiteDatabase db = App.getDatabase();
        db.beginTransactionNonExclusive();
        try {
            markAccountsAsNotPresent(db);
            for (LedgerAccount acc : list) {
                storeAccount(db, acc, false);
                for (LedgerAmount amt : acc.getAmounts()) {
                    storeAccountValue(db, acc.getName(), amt.getCurrency(), amt.getAmount());
                }
            }
            deleteNotPresentAccounts(db);
            setLastUpdateStamp();
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }

        mergeAccountListFromWeb(list);
        updateDisplayedAccounts();
    }
    public synchronized Locker lockAccountsForWriting() {
        accountsLocker.lockForWriting();
        return accountsLocker;
    }
    public void setAndStoreTransactionList(ArrayList<LedgerTransaction> list) {
        storeTransactionListAsync(this, list);
        SQLiteDatabase db = App.getDatabase();
        db.beginTransactionNonExclusive();
        try {
            markTransactionsAsNotPresent(db);
            for (LedgerTransaction tr : list)
                storeTransaction(db, tr);
            deleteNotPresentTransactions(db);
            setLastUpdateStamp();
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }

        allTransactions.postValue(list);
    }
    private void storeTransactionListAsync(MobileLedgerProfile mobileLedgerProfile,
                                           List<LedgerTransaction> list) {
        if (transactionListSaver != null)
            transactionListSaver.interrupt();

        transactionListSaver = new TransactionListSaver(this, list);
        transactionListSaver.start();
    }
    public void setAndStoreAccountAndTransactionListFromWeb(List<LedgerAccount> accounts,
                                                            List<LedgerTransaction> transactions) {
        storeAccountAndTransactionListAsync(accounts, transactions, false);

        mergeAccountListFromWeb(accounts);
        updateDisplayedAccounts();

        allTransactions.postValue(transactions);
    }
    private void storeAccountAndTransactionListAsync(List<LedgerAccount> accounts,
                                                     List<LedgerTransaction> transactions,
                                                     boolean storeAccUiFields) {
        if (accountAndTransactionListSaver != null)
            accountAndTransactionListSaver.interrupt();

        accountAndTransactionListSaver =
                new AccountAndTransactionListSaver(this, accounts, transactions, storeAccUiFields);
        accountAndTransactionListSaver.start();
    }
    synchronized public void updateDisplayedAccounts() {
        if (displayedAccountsUpdater != null) {
            displayedAccountsUpdater.interrupt();
        }
        displayedAccountsUpdater = new AccountListDisplayedFilter(this, allAccounts);
        displayedAccountsUpdater.start();
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

    public enum FutureDates {
        None(0), OneWeek(7), TwoWeeks(14), OneMonth(30), TwoMonths(60), ThreeMonths(90),
        SixMonths(180), OneYear(365), All(-1);
        private static SparseArray<FutureDates> map = new SparseArray<>();

        static {
            for (FutureDates item : FutureDates.values()) {
                map.put(item.value, item);
            }
        }

        private int value;
        FutureDates(int value) {
            this.value = value;
        }
        public static FutureDates valueOf(int i) {
            return map.get(i, None);
        }
        public int toInt() {
            return this.value;
        }
        public String getText(Resources resources) {
            switch (value) {
                case 7:
                    return resources.getString(R.string.future_dates_7);
                case 14:
                    return resources.getString(R.string.future_dates_14);
                case 30:
                    return resources.getString(R.string.future_dates_30);
                case 60:
                    return resources.getString(R.string.future_dates_60);
                case 90:
                    return resources.getString(R.string.future_dates_90);
                case 180:
                    return resources.getString(R.string.future_dates_180);
                case 365:
                    return resources.getString(R.string.future_dates_365);
                case -1:
                    return resources.getString(R.string.future_dates_all);
                default:
                    return resources.getString(R.string.future_dates_none);
            }
        }
    }

    static class AccountListLoader extends Thread {
        MobileLedgerProfile profile;
        AccountListLoader(MobileLedgerProfile profile) {
            this.profile = profile;
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
            profile.allAccounts = list;
            profile.updateAccountsMap(list);
            profile.updateDisplayedAccounts();
        }
    }

    static class AccountListDisplayedFilter extends Thread {
        private final MobileLedgerProfile profile;
        private final List<LedgerAccount> list;
        AccountListDisplayedFilter(MobileLedgerProfile profile, List<LedgerAccount> list) {
            this.profile = profile;
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
                profile.displayedAccounts.postValue(newDisplayed);
            }
            Logger.debug("dFilter", "left synchronized block");
        }
    }

    private static class AccountListSaver extends Thread {
        private final MobileLedgerProfile profile;
        private final List<LedgerAccount> list;
        private final boolean storeUiFields;
        AccountListSaver(MobileLedgerProfile profile, List<LedgerAccount> list,
                         boolean storeUiFields) {
            this.list = list;
            this.profile = profile;
            this.storeUiFields = storeUiFields;
        }
        @Override
        public void run() {
            SQLiteDatabase db = App.getDatabase();
            db.beginTransactionNonExclusive();
            try {
                profile.markAccountsAsNotPresent(db);
                if (isInterrupted())
                    return;
                for (LedgerAccount acc : list) {
                    profile.storeAccount(db, acc, storeUiFields);
                    if (isInterrupted())
                        return;
                }
                profile.deleteNotPresentAccounts(db);
                if (isInterrupted())
                    return;
                profile.setLastUpdateStamp();
                db.setTransactionSuccessful();
            }
            finally {
                db.endTransaction();
            }
        }
    }

    private static class TransactionListSaver extends Thread {
        private final MobileLedgerProfile profile;
        private final List<LedgerTransaction> list;
        TransactionListSaver(MobileLedgerProfile profile, List<LedgerTransaction> list) {
            this.list = list;
            this.profile = profile;
        }
        @Override
        public void run() {
            SQLiteDatabase db = App.getDatabase();
            db.beginTransactionNonExclusive();
            try {
                profile.markTransactionsAsNotPresent(db);
                if (isInterrupted())
                    return;
                for (LedgerTransaction tr : list) {
                    profile.storeTransaction(db, tr);
                    if (isInterrupted())
                        return;
                }
                profile.deleteNotPresentTransactions(db);
                if (isInterrupted())
                    return;
                profile.setLastUpdateStamp();
                db.setTransactionSuccessful();
            }
            finally {
                db.endTransaction();
            }
        }
    }

    private static class AccountAndTransactionListSaver extends Thread {
        private final MobileLedgerProfile profile;
        private final List<LedgerAccount> accounts;
        private final List<LedgerTransaction> transactions;
        private final boolean storeAccUiFields;
        AccountAndTransactionListSaver(MobileLedgerProfile profile, List<LedgerAccount> accounts,
                                       List<LedgerTransaction> transactions,
                                       boolean storeAccUiFields) {
            this.accounts = accounts;
            this.transactions = transactions;
            this.profile = profile;
            this.storeAccUiFields = storeAccUiFields;
        }
        @Override
        public void run() {
            SQLiteDatabase db = App.getDatabase();
            db.beginTransactionNonExclusive();
            try {
                profile.markAccountsAsNotPresent(db);
                if (isInterrupted())
                    return;

                profile.markTransactionsAsNotPresent(db);
                if (isInterrupted()) {
                    return;
                }

                for (LedgerAccount acc : accounts) {
                    profile.storeAccount(db, acc, storeAccUiFields);
                    if (isInterrupted())
                        return;
                }

                for (LedgerTransaction tr : transactions) {
                    profile.storeTransaction(db, tr);
                    if (isInterrupted()) {
                        return;
                    }
                }

                profile.deleteNotPresentAccounts(db);
                if (isInterrupted()) {
                    return;
                }
                profile.deleteNotPresentTransactions(db);
                if (isInterrupted())
                    return;

                profile.setLastUpdateStamp();

                db.setTransactionSuccessful();
            }
            finally {
                db.endTransaction();
            }
        }
    }
}
