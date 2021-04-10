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

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.SparseArray;

import androidx.annotation.Nullable;
import androidx.room.Transaction;

import net.ktnx.mobileledger.App;
import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.async.DbOpQueue;
import net.ktnx.mobileledger.dao.AccountDAO;
import net.ktnx.mobileledger.dao.DescriptionHistoryDAO;
import net.ktnx.mobileledger.dao.OptionDAO;
import net.ktnx.mobileledger.dao.ProfileDAO;
import net.ktnx.mobileledger.dao.TransactionDAO;
import net.ktnx.mobileledger.db.AccountValue;
import net.ktnx.mobileledger.db.AccountWithAmounts;
import net.ktnx.mobileledger.db.DB;
import net.ktnx.mobileledger.json.API;
import net.ktnx.mobileledger.ui.profiles.ProfileDetailActivity;
import net.ktnx.mobileledger.ui.profiles.ProfileDetailFragment;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.Misc;
import net.ktnx.mobileledger.utils.SimpleDate;

import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static net.ktnx.mobileledger.utils.Logger.debug;

public final class MobileLedgerProfile {
    // N.B. when adding new fields, update the copy-constructor below
    private final long id;
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
    private API apiVersion = API.auto;
    private FutureDates futureDates = FutureDates.None;
    private boolean accountsLoaded;
    private boolean transactionsLoaded;
    private HledgerVersion detectedVersion;
    // N.B. when adding new fields, update the copy-constructor below
    transient private AccountAndTransactionListSaver accountAndTransactionListSaver;
    public MobileLedgerProfile(long id) {
        this.id = id;
    }
    public MobileLedgerProfile(MobileLedgerProfile origin) {
        id = origin.id;
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
        accountsLoaded = origin.accountsLoaded;
        transactionsLoaded = origin.transactionsLoaded;
        if (origin.detectedVersion != null)
            detectedVersion = new HledgerVersion(origin.detectedVersion);
    }
    // loads all profiles into Data.profiles
    // returns the profile with the given UUID
    public static MobileLedgerProfile loadAllFromDB(long currentProfileId) {
        MobileLedgerProfile result = null;
        ArrayList<MobileLedgerProfile> list = new ArrayList<>();
        SQLiteDatabase db = App.getDatabase();
        try (Cursor cursor = db.rawQuery("SELECT id, name, url, use_authentication, auth_user, " +
                                         "auth_password, permit_posting, theme, order_no, " +
                                         "preferred_accounts_filter, future_dates, api_version, " +
                                         "show_commodity_by_default, default_commodity, " +
                                         "show_comments_by_default, detected_version_pre_1_19, " +
                                         "detected_version_major, detected_version_minor FROM " +
                                         "profiles order by order_no", null))
        {
            while (cursor.moveToNext()) {
                MobileLedgerProfile item = new MobileLedgerProfile(cursor.getLong(0));
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
                {
                    boolean pre_1_20 = cursor.getInt(15) == 1;
                    int major = cursor.getInt(16);
                    int minor = cursor.getInt(17);

                    if (!pre_1_20 && major == 0 && minor == 0) {
                        item.detectedVersion = null;
                    }
                    else if (pre_1_20) {
                        item.detectedVersion = new HledgerVersion(true);
                    }
                    else {
                        item.detectedVersion = new HledgerVersion(major, minor);
                    }
                }
                list.add(item);
                if (item.getId() == currentProfileId)
                    result = item;
            }
        }
        Data.profiles.postValue(list);
        return result;
    }
    public static void storeProfilesOrder() {
        SQLiteDatabase db = App.getDatabase();
        db.beginTransactionNonExclusive();
        try {
            int orderNo = 0;
            for (MobileLedgerProfile p : Objects.requireNonNull(Data.profiles.getValue())) {
                db.execSQL("update profiles set order_no=? where id=?",
                        new Object[]{orderNo, p.getId()});
                p.orderNo = orderNo;
                orderNo++;
            }
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }
    }
    static public void startEditProfileActivity(Context context, MobileLedgerProfile profile) {
        Intent intent = new Intent(context, ProfileDetailActivity.class);
        Bundle args = new Bundle();
        if (profile != null) {
            int index = Data.getProfileIndex(profile);
            if (index != -1)
                intent.putExtra(ProfileDetailFragment.ARG_ITEM_ID, index);
        }
        intent.putExtras(args);
        context.startActivity(intent, args);
    }
    public HledgerVersion getDetectedVersion() {
        return detectedVersion;
    }
    public void setDetectedVersion(HledgerVersion detectedVersion) {
        this.detectedVersion = detectedVersion;
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
        if (id != p.id)
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
        if (!Objects.equals(detectedVersion, p.detectedVersion))
            return false;
        return futureDates == p.futureDates;
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
    public API getApiVersion() {
        return apiVersion;
    }
    public void setApiVersion(API apiVersion) {
        this.apiVersion = apiVersion;
    }
    public void setApiVersion(int apiVersion) {
        this.apiVersion = API.valueOf(apiVersion);
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
    public long getId() {
        return id;
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
            db.execSQL("REPLACE INTO profiles(id, name, permit_posting, url, " +
                       "use_authentication, auth_user, auth_password, theme, order_no, " +
                       "preferred_accounts_filter, future_dates, api_version, " +
                       "show_commodity_by_default, default_commodity, show_comments_by_default," +
                       "detected_version_pre_1_19, detected_version_major, " +
                       "detected_version_minor) " +
                       "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new Object[]{id, name, permitPosting, url, authEnabled,
                                 authEnabled ? authUserName : null,
                                 authEnabled ? authPassword : null, themeHue, orderNo,
                                 preferredAccountsFilter, futureDates.toInt(), apiVersion.toInt(),
                                 showCommodityByDefault, defaultCommodity, showCommentsByDefault,
                                 (detectedVersion != null) && detectedVersion.isPre_1_20_1(),
                                 (detectedVersion == null) ? 0 : detectedVersion.getMajor(),
                                 (detectedVersion == null) ? 0 : detectedVersion.getMinor()
                    });
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }
    }
    public void storeAccount(SQLiteDatabase db, int generation, LedgerAccount acc,
                             boolean storeUiFields) {
        // replace into is a bad idea because it would reset hidden to its default value
        // we like the default, but for new accounts only
        String sql = "update accounts set generation = ?";
        List<Object> params = new ArrayList<>();
        params.add(generation);
        if (storeUiFields) {
            sql += ", expanded=?";
            params.add(acc.isExpanded() ? 1 : 0);
        }
        sql += " where profile_id=? and name=?";
        params.add(id);
        params.add(acc.getName());
        db.execSQL(sql, params.toArray());

        db.execSQL("insert into accounts(profile_id, name, name_upper, parent_name, level, " +
                   "expanded, generation) select ?,?,?,?,?,0,? where (select changes() = 0)",
                new Object[]{id, acc.getName(), acc.getName().toUpperCase(), acc.getParentName(),
                             acc.getLevel(), generation
                });
//        debug("accounts", String.format("Stored account '%s' in DB [%s]", acc.getName(), uuid));
    }
    public void storeTransaction(SQLiteDatabase db, int generation, LedgerTransaction tr) {
        tr.fillDataHash();
//        Logger.debug("storeTransaction", String.format(Locale.US, "ID %d", tr.getId()));
        SimpleDate d = tr.getDate();
        db.execSQL("UPDATE transactions SET year=?, month=?, day=?, description=?, comment=?, " +
                   "data_hash=?, generation=? WHERE profile_id=? AND ledger_id=?",
                new Object[]{d.year, d.month, d.day, tr.getDescription(), tr.getComment(),
                             tr.getDataHash(), generation, id, tr.getId()
                });
        db.execSQL(
                "INSERT INTO transactions(profile_id, ledger_id, year, month, day, description, " +
                "comment, data_hash, generation) " +
                "select ?,?,?,?,?,?,?,?,? WHERE (select changes() = 0)",
                new Object[]{id, tr.getId(), tr.getDate().year, tr.getDate().month,
                             tr.getDate().day, tr.getDescription(), tr.getComment(),
                             tr.getDataHash(), generation
                });

        int accountOrderNo = 1;
        for (LedgerTransactionAccount item : tr.getAccounts()) {
            db.execSQL("UPDATE transaction_accounts SET account_name=?, amount=?, currency=?, " +
                       "comment=?, generation=? " + "WHERE transaction_id=? AND order_no=?",
                    new Object[]{item.getAccountName(), item.getAmount(),
                                 Misc.nullIsEmpty(item.getCurrency()), item.getComment(),
                                 generation, tr.getId(), accountOrderNo
                    });
            db.execSQL("INSERT INTO transaction_accounts(transaction_id, " +
                       "order_no, account_name, amount, currency, comment, generation) " +
                       "select ?, ?, ?, ?, ?, ?, ? WHERE (select changes() = 0)",
                    new Object[]{tr.getId(), accountOrderNo, item.getAccountName(),
                                 item.getAmount(), Misc.nullIsEmpty(item.getCurrency()),
                                 item.getComment(), generation
                    });

            accountOrderNo++;
        }
//        debug("profile", String.format("Transaction %d stored", tr.getId()));
    }
    public String getOption(String name, String default_value) {
        SQLiteDatabase db = App.getDatabase();
        try (Cursor cursor = db.rawQuery(
                "select value from options where profile_id = ? and name=?",
                new String[]{String.valueOf(id), name}))
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
        DbOpQueue.add("insert or replace into options(profile_id, name, value) values(?, ?, ?);",
                new String[]{String.valueOf(id), name, value});
    }
    public void setLongOption(String name, long value) {
        setOption(name, String.valueOf(value));
    }
    public void removeFromDB() {
        ProfileDAO dao = DB.get()
                           .getProfileDAO();
        AsyncTask.execute(() -> dao.deleteSync(dao.getByIdSync(id)));
    }
    public LedgerTransaction loadTransaction(int transactionId) {
        LedgerTransaction tr = new LedgerTransaction(transactionId, this.id);
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
    public int getNextTransactionsGeneration(SQLiteDatabase db) {
        try (Cursor c = db.rawQuery(
                "SELECT generation FROM transactions WHERE profile_id=? LIMIT 1",
                new String[]{String.valueOf(id)}))
        {
            if (c.moveToFirst())
                return c.getInt(0) + 1;
        }
        return 1;
    }
    private void deleteNotPresentTransactions(SQLiteDatabase db, int generation) {
        Logger.debug("db/benchmark", "Deleting obsolete transactions");
        db.execSQL(
                "DELETE FROM transaction_accounts WHERE (select t.profile_id from transactions t " +
                "where t.id=transaction_accounts.transaction_id)=? AND generation" + " <> ?",
                new Object[]{id, generation});
        db.execSQL("DELETE FROM transactions WHERE profile_id=? AND generation <> ?",
                new Object[]{id, generation});
        Logger.debug("db/benchmark", "Done deleting obsolete transactions");
    }
    @Transaction
    public void wipeAllDataSync() {
        OptionDAO optDao = DB.get()
                             .getOptionDAO();
        optDao.deleteSync(optDao.allForProfileSync(id));

        AccountDAO accDao = DB.get()
                              .getAccountDAO();
        accDao.deleteSync(accDao.allForProfileSync(id));

        TransactionDAO trnDao = DB.get()
                                  .getTransactionDAO();
        trnDao.deleteSync(trnDao.allForProfileSync(id));

        DescriptionHistoryDAO descDao = DB.get()
                                          .getDescriptionHistoryDAO();
        descDao.sweepSync();
    }
    public void wipeAllData() {
        AsyncTask.execute(this::wipeAllDataSync);
    }
    public List<Currency> getCurrencies() {
        SQLiteDatabase db = App.getDatabase();

        ArrayList<Currency> result = new ArrayList<>();

        try (Cursor c = db.rawQuery("SELECT c.id, c.name, c.position, c.has_gap FROM currencies c",
                new String[]{}))
        {
            while (c.moveToNext()) {
                Currency currency = new Currency(c.getInt(0), c.getString(1),
                        Currency.Position.valueOf(c.getString(2)), c.getInt(3) == 1);
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
                        Currency.Position.valueOf(cursor.getString(2)), cursor.getInt(3) == 1);
            }
            return null;
        }
    }
    public void storeAccountAndTransactionListAsync(List<LedgerAccount> accounts,
                                                    List<LedgerTransaction> transactions) {
        if (accountAndTransactionListSaver != null)
            accountAndTransactionListSaver.interrupt();

        accountAndTransactionListSaver =
                new AccountAndTransactionListSaver(this, accounts, transactions);
        accountAndTransactionListSaver.start();
    }
    private Currency tryLoadCurrencyById(SQLiteDatabase db, int id) {
        try (Cursor cursor = db.rawQuery(
                "SELECT c.id, c.name, c.position, c.has_gap FROM currencies c WHERE c.id=?",
                new String[]{String.valueOf(id)}))
        {
            if (cursor.moveToFirst()) {
                return new Currency(cursor.getInt(0), cursor.getString(1),
                        Currency.Position.valueOf(cursor.getString(2)), cursor.getInt(3) == 1);
            }
            return null;
        }
    }
    public Currency loadCurrencyById(int id) {
        SQLiteDatabase db = App.getDatabase();
        Currency result = tryLoadCurrencyById(db, id);
        if (result == null)
            throw new RuntimeException(String.format("Unable to load currency with id '%d'", id));
        return result;
    }

    public enum FutureDates {
        None(0), OneWeek(7), TwoWeeks(14), OneMonth(30), TwoMonths(60), ThreeMonths(90),
        SixMonths(180), OneYear(365), All(-1);
        private static final SparseArray<FutureDates> map = new SparseArray<>();

        static {
            for (FutureDates item : FutureDates.values()) {
                map.put(item.value, item);
            }
        }

        private final int value;
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

    private static class AccountAndTransactionListSaver extends Thread {
        private final MobileLedgerProfile profile;
        private final List<LedgerAccount> accounts;
        private final List<LedgerTransaction> transactions;
        AccountAndTransactionListSaver(MobileLedgerProfile profile, List<LedgerAccount> accounts,
                                       List<LedgerTransaction> transactions) {
            this.accounts = accounts;
            this.transactions = transactions;
            this.profile = profile;
        }
        public int getNextDescriptionsGeneration(SQLiteDatabase db) {
            int generation = 1;
            try (Cursor c = db.rawQuery("SELECT generation FROM description_history LIMIT 1",
                    null))
            {
                if (c.moveToFirst()) {
                    generation = c.getInt(0) + 1;
                }
            }
            return generation;
        }
        void deleteNotPresentDescriptions(SQLiteDatabase db, int generation) {
            Logger.debug("db/benchmark", "Deleting obsolete descriptions");
            db.execSQL("DELETE FROM description_history WHERE generation <> ?",
                    new Object[]{generation});
            db.execSQL("DELETE FROM description_history WHERE generation <> ?",
                    new Object[]{generation});
            Logger.debug("db/benchmark", "Done deleting obsolete descriptions");
        }
        @Override
        public void run() {
            SQLiteDatabase db = App.getDatabase();
            db.beginTransactionNonExclusive();
            try {
                int transactionsGeneration = profile.getNextTransactionsGeneration(db);
                if (isInterrupted())
                    return;

                for (LedgerTransaction tr : transactions) {
                    profile.storeTransaction(db, transactionsGeneration, tr);
                    if (isInterrupted())
                        return;
                }

                profile.deleteNotPresentTransactions(db, transactionsGeneration);
                if (isInterrupted()) {
                    return;
                }

                Map<String, Boolean> unique = new HashMap<>();

                debug("descriptions", "Starting refresh");
                int descriptionsGeneration = getNextDescriptionsGeneration(db);
                try (Cursor c = db.rawQuery("SELECT distinct description from transactions",
                        null))
                {
                    while (c.moveToNext()) {
                        String description = c.getString(0);
                        String descriptionUpper = description.toUpperCase();
                        if (unique.containsKey(descriptionUpper))
                            continue;

                        storeDescription(db, descriptionsGeneration, description, descriptionUpper);

                        unique.put(descriptionUpper, true);
                    }
                }
                deleteNotPresentDescriptions(db, descriptionsGeneration);

                db.setTransactionSuccessful();
            }
            finally {
                db.endTransaction();
            }

            AsyncTask.execute(() -> {
                List<AccountWithAmounts> list = new ArrayList<>();

                final AccountDAO dao = DB.get()
                                         .getAccountDAO();

                for (LedgerAccount acc : accounts) {
                    AccountWithAmounts rec = new AccountWithAmounts();
                    rec.account = acc.toDBO();

                    if (isInterrupted())
                        return;

                    rec.amounts = new ArrayList<>();
                    for (LedgerAmount amt : acc.getAmounts()) {
                        AccountValue av = new AccountValue();
                        av.setCurrency(amt.getCurrency());
                        av.setValue(amt.getAmount());

                        rec.amounts.add(av);
                    }

                    list.add(rec);
                }

                if (isInterrupted())
                    return;

                dao.storeAccountsSync(list, profile.getId());
            });
        }
        private void storeDescription(SQLiteDatabase db, int generation, String description,
                                      String descriptionUpper) {
            db.execSQL("UPDATE description_history SET description=?, generation=? WHERE " +
                       "description_upper=?", new Object[]{description, generation, descriptionUpper
            });
            db.execSQL(
                    "INSERT INTO description_history(description, description_upper, generation) " +
                    "select ?,?,? WHERE (select changes() = 0)",
                    new Object[]{description, descriptionUpper, generation
                    });
        }
    }
}
