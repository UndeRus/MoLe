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

import net.ktnx.mobileledger.utils.MLDB;

import java.util.ArrayList;
import java.util.List;

public final class MobileLedgerProfile {
    private String uuid;
    private String name;
    private String url;
    private boolean useAuthentication;
    private String authUserName;
    private String authPassword;
    public MobileLedgerProfile(String uuid, String name, String url, boolean useAuthentication,
                               String authUserName, String authPassword) {
        this.uuid = uuid;
        this.name = name;
        this.url = url;
        this.useAuthentication = useAuthentication;
        this.authUserName = authUserName;
        this.authPassword = authPassword;
    }
    public static List<MobileLedgerProfile> loadAllFromDB() {
        List<MobileLedgerProfile> result = new ArrayList<>();
        SQLiteDatabase db = MLDB.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT uuid, name, url, use_authentication, auth_user, " +
                                         "auth_password FROM profiles", null))
        {
            while (cursor.moveToNext()) {
                result.add(new MobileLedgerProfile(cursor.getString(0), cursor.getString(1),
                        cursor.getString(2), cursor.getInt(3) == 1, cursor.getString(4),
                        cursor.getString(5)));
            }
        }
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
    public String getUrl() {
        return url;
    }
    public boolean isUseAuthentication() {
        return useAuthentication;
    }
    public String getAuthUserName() {
        return authUserName;
    }
    public String getAuthPassword() {
        return authPassword;
    }
    public void storeInDB() {
        SQLiteDatabase db = MLDB.getWritableDatabase();
        db.beginTransaction();
        try {
            db.execSQL("REPLACE INTO profiles(uuid, name, url, use_authentication, auth_user, " +
                       "auth_password) VALUES(?, ?, ?, ?, ?, ?)",
                    new Object[]{uuid, name, url, useAuthentication,
                                 useAuthentication ? authUserName : null,
                                 useAuthentication ? authPassword : null
                    });
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }
    }
}
