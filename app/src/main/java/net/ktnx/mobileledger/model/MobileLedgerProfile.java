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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import net.ktnx.mobileledger.utils.MLDB;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class MobileLedgerProfile {
    private String uuid;
    private String name;
    private String url;
    private boolean authEnabled;
    private String authUserName;
    private String authPassword;
    public MobileLedgerProfile(String uuid, String name, String url, boolean authEnabled,
                               String authUserName, String authPassword) {
        this.uuid = uuid;
        this.name = name;
        this.url = url;
        this.authEnabled = authEnabled;
        this.authUserName = authUserName;
        this.authPassword = authPassword;
    }
    public MobileLedgerProfile(CharSequence name, CharSequence url, boolean authEnabled,
                               CharSequence authUserName, CharSequence authPassword) {
        this.uuid = String.valueOf(UUID.randomUUID());
        this.name = String.valueOf(name);
        this.url = String.valueOf(url);
        this.authEnabled = authEnabled;
        this.authUserName = String.valueOf(authUserName);
        this.authPassword = String.valueOf(authPassword);
    }
    public static List<MobileLedgerProfile> loadAllFromDB() {
        List<MobileLedgerProfile> result = new ArrayList<>();
        SQLiteDatabase db = MLDB.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT uuid, name, url, use_authentication, auth_user, " +
                                         "auth_password FROM profiles order by order_no", null))
        {
            while (cursor.moveToNext()) {
                result.add(new MobileLedgerProfile(cursor.getString(0), cursor.getString(1),
                        cursor.getString(2), cursor.getInt(3) == 1, cursor.getString(4),
                        cursor.getString(5)));
            }
        }
        return result;
    }
    public static void storeProfilesOrder() {
        SQLiteDatabase db = MLDB.getWritableDatabase();
        db.beginTransaction();
        try {
            int orderNo = 0;
            for (MobileLedgerProfile p : Data.profiles.getList()) {
                db.execSQL("update profiles set order_no=? where uuid=?",
                        new Object[]{orderNo, p.getUuid()});
                orderNo++;
            }
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }
    }
    public static List<MobileLedgerProfile> createInitialProfileList() {
        List<MobileLedgerProfile> result = new ArrayList<>();
        MobileLedgerProfile first =
                new MobileLedgerProfile(UUID.randomUUID().toString(), "default", "", false, "", "");
        first.storeInDB();
        result.add(first);

        return result;
    }
    public static MobileLedgerProfile loadUUIDFromDB(String profileUUID) {
        SQLiteDatabase db = MLDB.getReadableDatabase();
        String name;
        String url;
        String authUser;
        String authPassword;
        Boolean useAuthentication;
        try (Cursor cursor = db.rawQuery("SELECT name, url, use_authentication, auth_user, " +
                                         "auth_password FROM profiles WHERE uuid=?",
                new String[]{profileUUID}))
        {
            if (cursor.moveToNext()) {
                name = cursor.getString(0);
                url = cursor.getString(1);
                useAuthentication = cursor.getInt(2) == 1;
                authUser = useAuthentication ? cursor.getString(3) : null;
                authPassword = useAuthentication ? cursor.getString(4) : null;
            }
            else {
                name = "Unknown profile";
                url = "Https://server/url";
                useAuthentication = false;
                authUser = authPassword = null;
            }
        }

        return new MobileLedgerProfile(profileUUID, name, url, useAuthentication, authUser,
                authPassword);
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
        SQLiteDatabase db = MLDB.getWritableDatabase();
        db.beginTransaction();
        try {
            db.execSQL("REPLACE INTO profiles(uuid, name, url, use_authentication, auth_user, " +
                       "auth_password) VALUES(?, ?, ?, ?, ?, ?)",
                    new Object[]{uuid, name, url, authEnabled, authEnabled ? authUserName : null,
                                 authEnabled ? authPassword : null
                    });
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }
    }
    public void storeAccount(String name) {
        SQLiteDatabase db = MLDB.getWritableDatabase();

        do {
            LedgerAccount acc = new LedgerAccount(name);
            db.execSQL("replace into accounts(profile, name, name_upper, level, keep) values(?, " +
                       "?, ?, ?, 1)",
                    new Object[]{this.uuid, name, name.toUpperCase(), acc.getLevel()});
            name = acc.getParentName();
        } while (name != null);
    }
    public void storeAccountValue(String name, String currency, Float amount) {
        SQLiteDatabase db = MLDB.getWritableDatabase();
        db.execSQL("replace into account_values(profile, account, " +
                   "currency, value, keep) values(?, ?, ?, ?, 1);",
                new Object[]{uuid, name, currency, amount});
    }
    public void storeTransaction(LedgerTransaction tr) {
        SQLiteDatabase db = MLDB.getWritableDatabase();
        tr.fillDataHash();
        db.execSQL("DELETE from transactions WHERE profile=? and id=?",
                new Object[]{uuid, tr.getId()});
        db.execSQL("DELETE from transaction_accounts WHERE profile = ? and transaction_id=?",
                new Object[]{uuid, tr.getId()});

        db.execSQL("INSERT INTO transactions(profile, id, date, description, data_hash, keep) " +
                   "values(?,?,?,?,?,1)",
                new Object[]{uuid, tr.getId(), tr.getDate(), tr.getDescription(), tr.getDataHash()
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
    public String get_option_value(String name, String default_value) {
        SQLiteDatabase db = MLDB.getReadableDatabase();
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
    public long get_option_value(String name, long default_value) {
        long longResult;
        String result = get_option_value(name, "");
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
    public void set_option_value(String name, String value) {
        Log.d("profile", String.format("setting option %s=%s", name, value));
        SQLiteDatabase db = MLDB.getWritableDatabase();
        db.execSQL("insert or replace into options(profile, name, value) values(?, ?, ?);",
                new String[]{uuid, name, value});
    }
    public void set_option_value(String name, long value) {
        set_option_value(name, String.valueOf(value));
    }
    public void removeFromDB() {
        SQLiteDatabase db = MLDB.getWritableDatabase();
        Log.d("db", String.format("removing progile %s from DB", uuid));
        db.execSQL("delete from profiles where uuid=?", new Object[]{uuid});
    }
}
