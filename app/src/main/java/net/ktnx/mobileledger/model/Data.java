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

import net.ktnx.mobileledger.utils.LockHolder;
import net.ktnx.mobileledger.utils.MLDB;
import net.ktnx.mobileledger.utils.ObservableAtomicInteger;
import net.ktnx.mobileledger.utils.ObservableList;
import net.ktnx.mobileledger.utils.ObservableValue;

import java.util.ArrayList;
import java.util.Date;

public final class Data {
    public static ObservableList<TransactionListItem> transactions = new ObservableList<>(new ArrayList<>());
    public static ObservableList<LedgerAccount> accounts = new ObservableList<>(new ArrayList<>());
    public static ObservableAtomicInteger backgroundTaskCount = new ObservableAtomicInteger(0);
    public static ObservableValue<Date> lastUpdateDate = new ObservableValue<>();
    public static ObservableValue<MobileLedgerProfile> profile = new ObservableValue<>();
    public static ObservableList<MobileLedgerProfile> profiles =
            new ObservableList<>(new ArrayList<>());
    public static ObservableValue<Boolean> optShowOnlyStarred = new ObservableValue<>();
    public static ObservableValue<String> accountFilter = new ObservableValue<>();
    public static void setCurrentProfile(MobileLedgerProfile newProfile) {
        MLDB.setOption(MLDB.OPT_PROFILE_UUID, newProfile.getUuid());
        profile.set(newProfile);
    }
    public static int getProfileIndex(MobileLedgerProfile profile) {
        try (LockHolder lh = profiles.lockForReading()) {
            for (int i = 0; i < profiles.size(); i++) {
                MobileLedgerProfile p = profiles.get(i);
                if (p.equals(profile)) return i;
            }

            return -1;
        }
    }
    public static int getProfileIndex(String profileUUID) {
        try (LockHolder lh = profiles.lockForReading()) {
            for (int i = 0; i < profiles.size(); i++) {
                MobileLedgerProfile p = profiles.get(i);
                if (p.getUuid().equals(profileUUID)) return i;
            }

            return -1;
        }
    }
    public static int retrieveCurrentThemeIdFromDb() {
        String profileUUID = MLDB.getOption(MLDB.OPT_PROFILE_UUID, null);
        if (profileUUID == null) return -1;

        SQLiteDatabase db = MLDB.getDatabase();
        try(Cursor c = db.rawQuery("SELECT theme from profiles where uuid=?", new String[]{profileUUID})) {
            if (c.moveToNext()) return c.getInt(0);
        }

        return -1;
    }
    public static MobileLedgerProfile getProfile(String profileUUID) {
        MobileLedgerProfile profile;
        if (profiles.isEmpty()) {
            profile = MobileLedgerProfile.loadAllFromDB(profileUUID);
        }
        else {
            try (LockHolder lh = profiles.lockForReading()) {
                int i = getProfileIndex(profileUUID);
                if (i == -1) i = 0;
                profile = profiles.get(i);
            }
        }
        return profile;
    }
}
