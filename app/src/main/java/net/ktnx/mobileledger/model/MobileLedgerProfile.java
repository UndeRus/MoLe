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
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import net.ktnx.mobileledger.App;
import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.async.DbOpQueue;
import net.ktnx.mobileledger.async.SendTransactionTask;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.MLDB;
import net.ktnx.mobileledger.utils.Misc;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static net.ktnx.mobileledger.utils.Logger.debug;

public final class MobileLedgerProfile {
    // N.B. when adding new fields, update the copy-constructor below
    private String uuid;
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
    // N.B. when adding new fields, update the copy-constructor below
    private FutureDates futureDates = FutureDates.None;
    private SendTransactionTask.API apiVersion = SendTransactionTask.API.auto;
    private Calendar firstTransactionDate;
    private Calendar lastTransactionDate;
    private MutableLiveData<ArrayList<LedgerAccount>> accounts =
            new MutableLiveData<>(new ArrayList<>());
    private AccountListLoader loader = null;
    public MobileLedgerProfile() {
        this.uuid = String.valueOf(UUID.randomUUID());
    }
    public MobileLedgerProfile(String uuid) {
        this.uuid = uuid;
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
    public LiveData<ArrayList<LedgerAccount>> getAccounts() {
        return accounts;
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
    public void storeAccount(SQLiteDatabase db, LedgerAccount acc) {
        // replace into is a bad idea because it would reset hidden to its default value
        // we like the default, but for new accounts only
        db.execSQL("update accounts set level = ?, keep = 1, expanded=? " +
                   "where profile=? and name = ?",
                new Object[]{acc.getLevel(), acc.isExpanded(), uuid, acc.getName()
                });
        db.execSQL("insert into accounts(profile, name, name_upper, parent_name, level, " +
                   "expanded, keep) " + "select ?,?,?,?,?,?,1 where (select changes() = 0)",
                new Object[]{uuid, acc.getName(), acc.getName().toUpperCase(), acc.getParentName(),
                             acc.getLevel(), acc.isExpanded()
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
    @NonNull
    public LedgerAccount loadAccount(String name) {
        SQLiteDatabase db = App.getDatabase();
        return loadAccount(db, name);
    }
    @Nullable
    public LedgerAccount tryLoadAccount(String acct_name) {
        SQLiteDatabase db = App.getDatabase();
        return tryLoadAccount(db, acct_name);
    }
    @NonNull
    public LedgerAccount loadAccount(SQLiteDatabase db, String accName) {
        LedgerAccount acc = tryLoadAccount(db, accName);

        if (acc == null)
            throw new RuntimeException("Unable to load account with name " + accName);

        return acc;
    }
    @Nullable
    public LedgerAccount tryLoadAccount(SQLiteDatabase db, String accName) {
        try (Cursor cursor = db.rawQuery("SELECT a.expanded, a.amounts_expanded, (select 1 from accounts a2 " +
                                         "where a2.profile = a.profile and a2.name like a" +
                                         ".name||':%' limit 1) " +
                                         "FROM accounts a WHERE a.profile = ? and a.name=?",
                new String[]{uuid, accName}))
        {
            if (cursor.moveToFirst()) {
                LedgerAccount acc = new LedgerAccount(this, accName);
                acc.setExpanded(cursor.getInt(0) == 1);
                acc.setAmountsExpanded(cursor.getInt(1) == 1);
                acc.setHasSubAccounts(cursor.getInt(2) == 1);

                try (Cursor c2 = db.rawQuery(
                        "SELECT value, currency FROM account_values WHERE profile = ? " +
                        "AND account = ?", new String[]{uuid, accName}))
                {
                    while (c2.moveToNext()) {
                        acc.addAmount(c2.getFloat(0), c2.getString(1));
                    }
                }

                return acc;
            }
            return null;
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
    public void markAccountsAsNotPresent(SQLiteDatabase db) {
        db.execSQL("update account_values set keep=0 where profile=?;", new String[]{uuid});
        db.execSQL("update accounts set keep=0 where profile=?;", new String[]{uuid});

    }
    public void deleteNotPresentAccounts(SQLiteDatabase db) {
        db.execSQL("delete from account_values where keep=0 and profile=?", new String[]{uuid});
        db.execSQL("delete from accounts where keep=0 and profile=?", new String[]{uuid});
    }
    public void markTransactionAsPresent(SQLiteDatabase db, LedgerTransaction transaction) {
        db.execSQL("UPDATE transactions SET keep = 1 WHERE profile = ? and id=?",
                new Object[]{uuid, transaction.getId()
                });
    }
    public void markTransactionsBeforeTransactionAsPresent(SQLiteDatabase db,
                                                           LedgerTransaction transaction) {
        db.execSQL("UPDATE transactions SET keep=1 WHERE profile = ? and id < ?",
                new Object[]{uuid, transaction.getId()
                });

    }
    public void deleteNotPresentTransactions(SQLiteDatabase db) {
        db.execSQL("DELETE FROM transactions WHERE profile=? AND keep = 0", new String[]{uuid});
    }
    public void setLastUpdateStamp() {
        debug("db", "Updating transaction value stamp");
        Date now = new Date();
        setLongOption(MLDB.OPT_LAST_SCRAPE, now.getTime());
        Data.lastUpdateDate.postValue(now);
    }
    public List<LedgerAccount> loadChildAccountsOf(LedgerAccount acc) {
        List<LedgerAccount> result = new ArrayList<>();
        SQLiteDatabase db = App.getDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT a.name FROM accounts a WHERE a.profile = ? and a.name like ?||':%'",
                new String[]{uuid, acc.getName()}))
        {
            while (c.moveToNext()) {
                LedgerAccount a = loadAccount(db, c.getString(0));
                result.add(a);
            }
        }

        return result;
    }
    public List<LedgerAccount> loadVisibleChildAccountsOf(LedgerAccount acc) {
        List<LedgerAccount> result = new ArrayList<>();
        ArrayList<LedgerAccount> visibleList = new ArrayList<>();
        visibleList.add(acc);

        SQLiteDatabase db = App.getDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT a.name FROM accounts a WHERE a.profile = ? and a.name like ?||':%'",
                new String[]{uuid, acc.getName()}))
        {
            while (c.moveToNext()) {
                LedgerAccount a = loadAccount(db, c.getString(0));
                if (a.isVisible(visibleList)) {
                    result.add(a);
                    visibleList.add(a);
                }
            }
        }

        return result;
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
    public void setAccounts(ArrayList<LedgerAccount> list) {
        accounts.postValue(list);
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
            ArrayList<LedgerAccount> newList = new ArrayList<>();

            String sql = "SELECT a.name from accounts a WHERE a.profile = ?";
            sql += " ORDER BY a.name";

            SQLiteDatabase db = App.getDatabase();
            try (Cursor cursor = db.rawQuery(sql, new String[]{profileUUID})) {
                while (cursor.moveToNext()) {
                    if (isInterrupted())
                        return;

                    final String accName = cursor.getString(0);
//                    debug("accounts",
//                            String.format("Read account '%s' from DB [%s]", accName,
//                            profileUUID));
                    LedgerAccount acc = profile.loadAccount(db, accName);
                    if (acc.isVisible(newList))
                        newList.add(acc);
                }
            }

            if (isInterrupted())
                return;

            Logger.debug("async-acc", "AccountListLoader::run() posting new list");
            profile.accounts.postValue(newList);
        }
    }
}
