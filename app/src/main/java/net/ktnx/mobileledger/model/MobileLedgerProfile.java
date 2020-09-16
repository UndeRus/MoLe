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

import androidx.annotation.Nullable;

import net.ktnx.mobileledger.App;
import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.async.DbOpQueue;
import net.ktnx.mobileledger.async.SendTransactionTask;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.Misc;
import net.ktnx.mobileledger.utils.SimpleDate;

import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static net.ktnx.mobileledger.utils.Logger.debug;

public final class MobileLedgerProfile {
    // N.B. when adding new fields, update the copy-constructor below
    private final String uuid;
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
    private FutureDates futureDates = FutureDates.None;
    private boolean accountsLoaded;
    private boolean transactionsLoaded;
    // N.B. when adding new fields, update the copy-constructor below
    transient private AccountAndTransactionListSaver accountAndTransactionListSaver;
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
            for (MobileLedgerProfile p : Objects.requireNonNull(Data.profiles.getValue())) {
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
        sql += " where profile=? and name=?";
        params.add(uuid);
        params.add(acc.getName());
        db.execSQL(sql, params.toArray());

        db.execSQL("insert into accounts(profile, name, name_upper, parent_name, level, " +
                   "expanded, generation) select ?,?,?,?,?,0,? where (select changes() = 0)",
                new Object[]{uuid, acc.getName(), acc.getName().toUpperCase(), acc.getParentName(),
                             acc.getLevel(), generation
                });
//        debug("accounts", String.format("Stored account '%s' in DB [%s]", acc.getName(), uuid));
    }
    public void storeAccountValue(SQLiteDatabase db, int generation, String name, String currency,
                                  Float amount) {
        db.execSQL("replace into account_values(profile, account, " +
                   "currency, value, generation) values(?, ?, ?, ?, ?);",
                new Object[]{uuid, name, Misc.emptyIsNull(currency), amount, generation});
    }
    public void storeTransaction(SQLiteDatabase db, int generation, LedgerTransaction tr) {
        tr.fillDataHash();
//        Logger.debug("storeTransaction", String.format(Locale.US, "ID %d", tr.getId()));
        SimpleDate d = tr.getDate();
        db.execSQL("UPDATE transactions SET year=?, month=?, day=?, description=?, comment=?, " +
                   "data_hash=?, generation=? WHERE profile=? AND id=?",
                new Object[]{d.year, d.month, d.day, tr.getDescription(), tr.getComment(),
                             tr.getDataHash(), generation, uuid, tr.getId()
                });
        db.execSQL("INSERT INTO transactions(profile, id, year, month, day, description, " +
                   "comment, data_hash, generation) " +
                   "select ?,?,?,?,?,?,?,?,? WHERE (select changes() = 0)",
                new Object[]{uuid, tr.getId(), tr.getDate().year, tr.getDate().month,
                             tr.getDate().day, tr.getDescription(), tr.getComment(),
                             tr.getDataHash(), generation
                });

        int accountOrderNo = 1;
        for (LedgerTransactionAccount item : tr.getAccounts()) {
            db.execSQL("UPDATE transaction_accounts SET account_name=?, amount=?, currency=?, " +
                       "comment=?, generation=? " +
                       "WHERE profile=? AND transaction_id=? AND order_no=?",
                    new Object[]{item.getAccountName(), item.getAmount(),
                                 Misc.nullIsEmpty(item.getCurrency()), item.getComment(),
                                 generation, uuid, tr.getId(), accountOrderNo
                    });
            db.execSQL("INSERT INTO transaction_accounts(profile, transaction_id, " +
                       "order_no, account_name, amount, currency, comment, generation) " +
                       "select ?, ?, ?, ?, ?, ?, ?, ? WHERE (select changes() = 0)",
                    new Object[]{uuid, tr.getId(), accountOrderNo, item.getAccountName(),
                                 item.getAmount(), Misc.nullIsEmpty(item.getCurrency()),
                                 item.getComment(), generation
                    });

            accountOrderNo++;
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
    public int getNextTransactionsGeneration(SQLiteDatabase db) {
        int generation = 1;
        try (Cursor c = db.rawQuery("SELECT generation FROM transactions WHERE profile=? LIMIT 1",
                new String[]{uuid}))
        {
            if (c.moveToFirst()) {
                generation = c.getInt(0) + 1;
            }
        }
        return generation;
    }
    private int getNextAccountsGeneration(SQLiteDatabase db) {
        int generation = 1;
        try (Cursor c = db.rawQuery("SELECT generation FROM accounts WHERE profile=? LIMIT 1",
                new String[]{uuid}))
        {
            if (c.moveToFirst()) {
                generation = c.getInt(0) + 1;
            }
        }
        return generation;
    }
    private void deleteNotPresentAccounts(SQLiteDatabase db, int generation) {
        Logger.debug("db/benchmark", "Deleting obsolete accounts");
        db.execSQL("DELETE FROM account_values WHERE profile=? AND generation <> ?",
                new Object[]{uuid, generation});
        db.execSQL("DELETE FROM accounts WHERE profile=? AND generation <> ?",
                new Object[]{uuid, generation});
        Logger.debug("db/benchmark", "Done deleting obsolete accounts");
    }
    private void deleteNotPresentTransactions(SQLiteDatabase db, int generation) {
        Logger.debug("db/benchmark", "Deleting obsolete transactions");
        db.execSQL("DELETE FROM transaction_accounts WHERE profile=? AND generation <> ?",
                new Object[]{uuid, generation});
        db.execSQL("DELETE FROM transactions WHERE profile=? AND generation <> ?",
                new Object[]{uuid, generation});
        Logger.debug("db/benchmark", "Done deleting obsolete transactions");
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
    public void storeAccountAndTransactionListAsync(List<LedgerAccount> accounts,
                                                    List<LedgerTransaction> transactions) {
        if (accountAndTransactionListSaver != null)
            accountAndTransactionListSaver.interrupt();

        accountAndTransactionListSaver =
                new AccountAndTransactionListSaver(this, accounts, transactions);
        accountAndTransactionListSaver.start();
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
                int accountsGeneration = profile.getNextAccountsGeneration(db);
                if (isInterrupted())
                    return;

                int transactionsGeneration = profile.getNextTransactionsGeneration(db);
                if (isInterrupted())
                    return;

                for (LedgerAccount acc : accounts) {
                    profile.storeAccount(db, accountsGeneration, acc, false);
                    if (isInterrupted())
                        return;
                    for (LedgerAmount amt : acc.getAmounts()) {
                        profile.storeAccountValue(db, accountsGeneration, acc.getName(),
                                amt.getCurrency(), amt.getAmount());
                        if (isInterrupted())
                            return;
                    }
                }

                for (LedgerTransaction tr : transactions) {
                    profile.storeTransaction(db, transactionsGeneration, tr);
                    if (isInterrupted())
                        return;
                }

                profile.deleteNotPresentTransactions(db, transactionsGeneration);
                if (isInterrupted()) {
                    return;
                }
                profile.deleteNotPresentAccounts(db, accountsGeneration);
                if (isInterrupted())
                    return;

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
