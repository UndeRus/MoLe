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

package net.ktnx.mobileledger.model;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import net.ktnx.mobileledger.async.DbOpQueue;
import net.ktnx.mobileledger.utils.Globals;
import net.ktnx.mobileledger.utils.LockHolder;
import net.ktnx.mobileledger.utils.MLDB;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class MobileLedgerProfile {
    private String uuid;
    private String name;
    private boolean permitPosting;
    private String preferredAccountsFilter;
    private String url;
    private boolean authEnabled;
    private String authUserName;
    private String authPassword;
    private int themeId;
    private int orderNo = -1;
    public MobileLedgerProfile() {
        this.uuid = String.valueOf(UUID.randomUUID());
    }
    public MobileLedgerProfile(String uuid) {
        this.uuid = uuid;
    }
    // loads all profiles into Data.profiles
    // returns the profile with the given UUID
    public static MobileLedgerProfile loadAllFromDB(String currentProfileUUID) {
        MobileLedgerProfile result = null;
        List<MobileLedgerProfile> list = new ArrayList<>();
        SQLiteDatabase db = MLDB.getDatabase();
        try (Cursor cursor = db.rawQuery("SELECT uuid, name, url, use_authentication, auth_user, " +
                                         "auth_password, permit_posting, theme, order_no, " +
                                         "preferred_accounts_filter FROM " +
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
                list.add(item);
                if (item.getUuid().equals(currentProfileUUID)) result = item;
            }
        }
        Data.profiles.setList(list);
        return result;
    }
    public static void storeProfilesOrder() {
        SQLiteDatabase db = MLDB.getDatabase();
        db.beginTransaction();
        try {
            int orderNo = 0;
            try (LockHolder lh = Data.profiles.lockForReading()) {
                for (int i = 0; i < Data.profiles.size(); i++) {
                    MobileLedgerProfile p = Data.profiles.get(i);
                    db.execSQL("update profiles set order_no=? where uuid=?",
                            new Object[]{orderNo, p.getUuid()});
                    p.orderNo = orderNo;
                    orderNo++;
                }
            }
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }
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
        SQLiteDatabase db = MLDB.getDatabase();
        db.beginTransaction();
        try {
//            Log.d("profiles", String.format("Storing profile in DB: uuid=%s, name=%s, " +
//                                            "url=%s, permit_posting=%s, authEnabled=%s, " +
//                                            "themeId=%d", uuid, name, url,
//                    permitPosting ? "TRUE" : "FALSE", authEnabled ? "TRUE" : "FALSE", themeId));
            db.execSQL("REPLACE INTO profiles(uuid, name, permit_posting, url, " +
                       "use_authentication, auth_user, " +
                       "auth_password, theme, order_no, preferred_accounts_filter) " +
                       "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new Object[]{uuid, name, permitPosting, url, authEnabled,
                                 authEnabled ? authUserName : null,
                                 authEnabled ? authPassword : null, themeId, orderNo,
                                 preferredAccountsFilter
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
        db.execSQL("update accounts set level = ?, keep = 1, hidden=?, expanded=? " +
                   "where profile=? and name = ?",
                new Object[]{acc.getLevel(), acc.isHiddenByStar(), acc.isExpanded(), uuid,
                             acc.getName()
                });
        db.execSQL(
                "insert into accounts(profile, name, name_upper, parent_name, level, hidden, expanded, keep) " +
                "select ?,?,?,?,?,?,?,1 where (select changes() = 0)",
                new Object[]{uuid, acc.getName(), acc.getName().toUpperCase(), acc.getParentName(),
                             acc.getLevel(), acc.isHiddenByStar(), acc.isExpanded()
                });
//        Log.d("accounts", String.format("Stored account '%s' in DB [%s]", acc.getName(), uuid));
    }
    public void storeAccountValue(SQLiteDatabase db, String name, String currency, Float amount) {
        db.execSQL("replace into account_values(profile, account, " +
                   "currency, value, keep) values(?, ?, ?, ?, 1);",
                new Object[]{uuid, name, currency, amount});
    }
    public void storeTransaction(SQLiteDatabase db, LedgerTransaction tr) {
        tr.fillDataHash();
        db.execSQL("DELETE from transactions WHERE profile=? and id=?",
                new Object[]{uuid, tr.getId()});
        db.execSQL("DELETE from transaction_accounts WHERE profile = ? and transaction_id=?",
                new Object[]{uuid, tr.getId()});

        db.execSQL("INSERT INTO transactions(profile, id, date, description, data_hash, keep) " +
                   "values(?,?,?,?,?,1)",
                new Object[]{uuid, tr.getId(), Globals.formatLedgerDate(tr.getDate()),
                             tr.getDescription(), tr.getDataHash()
                });

        for (LedgerTransactionAccount item : tr.getAccounts()) {
            db.execSQL("INSERT INTO transaction_accounts(profile, transaction_id, " +
                       "account_name, amount, currency) values(?, ?, ?, ?, ?)",
                    new Object[]{uuid, tr.getId(), item.getAccountName(), item.getAmount(),
                                 item.getCurrency()
                    });
        }
        Log.d("profile", String.format("Transaction %d stored", tr.getId()));
    }
    public String getOption(String name, String default_value) {
        SQLiteDatabase db = MLDB.getDatabase();
        try (Cursor cursor = db.rawQuery("select value from options where profile = ? and name=?",
                new String[]{uuid, name}))
        {
            if (cursor.moveToFirst()) {
                String result = cursor.getString(0);

                if (result == null) {
                    Log.d("profile", "returning default value for " + name);
                    result = default_value;
                }
                else Log.d("profile", String.format("option %s=%s", name, result));

                return result;
            }
            else return default_value;
        }
        catch (Exception e) {
            Log.d("db", "returning default value for " + name, e);
            return default_value;
        }
    }
    public long getLongOption(String name, long default_value) {
        long longResult;
        String result = getOption(name, "");
        if ((result == null) || result.isEmpty()) {
            Log.d("profile", String.format("Returning default value for option %s", name));
            longResult = default_value;
        }
        else {
            try {
                longResult = Long.parseLong(result);
                Log.d("profile", String.format("option %s=%s", name, result));
            }
            catch (Exception e) {
                Log.d("profile", String.format("Returning default value for option %s", name), e);
                longResult = default_value;
            }
        }

        return longResult;
    }
    public void setOption(String name, String value) {
        Log.d("profile", String.format("setting option %s=%s", name, value));
        DbOpQueue.add("insert or replace into options(profile, name, value) values(?, ?, ?);",
                new String[]{uuid, name, value});
    }
    public void setLongOption(String name, long value) {
        setOption(name, String.valueOf(value));
    }
    public void removeFromDB() {
        SQLiteDatabase db = MLDB.getDatabase();
        Log.d("db", String.format("removing profile %s from DB", uuid));
        db.beginTransaction();
        try {
            Object[] uuid_param = new Object[]{uuid};
            db.execSQL("delete from profiles where uuid=?", uuid_param);
            db.execSQL("delete from accounts where profile=?", uuid_param);
            db.execSQL("delete from account_values where profile=?", uuid_param);
            db.execSQL("delete from transactions where profile=?", uuid_param);
            db.execSQL("delete from transaction_accounts where profile=?", uuid_param);
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }
    }
    @NonNull
    public LedgerAccount loadAccount(String name) {
        SQLiteDatabase db = MLDB.getDatabase();
        return loadAccount(db, name);
    }
    @Nullable
    public LedgerAccount tryLoadAccount(String acct_name) {
        SQLiteDatabase db = MLDB.getDatabase();
        return tryLoadAccount(db, acct_name);
    }
    @NonNull
    public LedgerAccount loadAccount(SQLiteDatabase db, String accName) {
        LedgerAccount acc = tryLoadAccount(db, accName);

        if (acc == null) throw new RuntimeException("Unable to load account with name " + accName);

        return acc;
    }
    @Nullable
    public LedgerAccount tryLoadAccount(SQLiteDatabase db, String accName) {
        try (Cursor cursor = db.rawQuery(
                "SELECT a.hidden, a.expanded, (select 1 from accounts a2 " +
                "where a2.profile = a.profile and a2.name like a.name||':%' limit 1) " +
                "FROM accounts a WHERE a.profile = ? and a.name=?", new String[]{uuid, accName}))
        {
            if (cursor.moveToFirst()) {
                LedgerAccount acc = new LedgerAccount(accName);
                acc.setHiddenByStar(cursor.getInt(0) == 1);
                acc.setExpanded(cursor.getInt(1) == 1);
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
        tr.loadData(MLDB.getDatabase());

        return tr;
    }
    public int getThemeId() {
//        Log.d("profile", String.format("Profile.getThemeId() returning %d", themeId));
        return this.themeId;
    }
    public void setThemeId(Object o) {
        setThemeId(Integer.valueOf(String.valueOf(o)).intValue());
    }
    public void setThemeId(int themeId) {
//        Log.d("profile", String.format("Profile.setThemeId(%d) called", themeId));
        this.themeId = themeId;
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
        Log.d("db", "Updating transaction value stamp");
        Date now = new Date();
        setLongOption(MLDB.OPT_LAST_SCRAPE, now.getTime());
        Data.lastUpdateDate.postValue(now);
    }
    public List<LedgerAccount> loadChildAccountsOf(LedgerAccount acc) {
        List<LedgerAccount> result = new ArrayList<>();
        SQLiteDatabase db = MLDB.getDatabase();
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

        SQLiteDatabase db = MLDB.getDatabase();
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
        SQLiteDatabase db = MLDB.getDatabase();
        db.beginTransaction();
        try {
            String[] pUuid = new String[]{uuid};
            db.execSQL("delete from options where profile=?", pUuid);
            db.execSQL("delete from accounts where profile=?", pUuid);
            db.execSQL("delete from account_values where profile=?", pUuid);
            db.execSQL("delete from transactions where profile=?", pUuid);
            db.execSQL("delete from transaction_accounts where profile=?", pUuid);
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }
    }
}
