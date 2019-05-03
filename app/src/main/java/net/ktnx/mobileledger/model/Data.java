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
import net.ktnx.mobileledger.utils.Locker;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.MLDB;
import net.ktnx.mobileledger.utils.ObservableList;
import net.ktnx.mobileledger.utils.ObservableValue;

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.lifecycle.MutableLiveData;

import static net.ktnx.mobileledger.utils.Logger.debug;

public final class Data {
    public static ObservableList<TransactionListItem> transactions =
            new ObservableList<>(new ArrayList<>());
    public static ObservableList<LedgerAccount> accounts = new ObservableList<>(new ArrayList<>());
    public static MutableLiveData<Boolean> backgroundTasksRunning = new MutableLiveData<>(false);
    public static MutableLiveData<Date> lastUpdateDate = new MutableLiveData<>();
    public static MutableLiveData<MobileLedgerProfile> profile = new MutableLiveData<>();
    public static MutableLiveData<ArrayList<MobileLedgerProfile>> profiles =
            new MutableLiveData<>(new ArrayList<>());
    public static ObservableValue<Boolean> optShowOnlyStarred = new ObservableValue<>();
    public static MutableLiveData<String> accountFilter = new MutableLiveData<>();
    private static AtomicInteger backgroundTaskCount = new AtomicInteger(0);
    private static Locker profilesLocker = new Locker();
    public static void backgroundTaskStarted() {
        int cnt = backgroundTaskCount.incrementAndGet();
        debug("data",
                String.format(Locale.ENGLISH, "background task count is %d after incrementing",
                        cnt));
        backgroundTasksRunning.postValue(cnt > 0);
    }
    public static void backgroundTaskFinished() {
        int cnt = backgroundTaskCount.decrementAndGet();
        debug("data",
                String.format(Locale.ENGLISH, "background task count is %d after decrementing",
                        cnt));
        backgroundTasksRunning.postValue(cnt > 0);
    }
    public static void setCurrentProfile(MobileLedgerProfile newProfile) {
        MLDB.setOption(MLDB.OPT_PROFILE_UUID, newProfile.getUuid());
        profile.postValue(newProfile);
    }
    public static int getProfileIndex(MobileLedgerProfile profile) {
        try (LockHolder ignored = profilesLocker.lockForReading()) {
            List<MobileLedgerProfile> prList = profiles.getValue();
            assert prList != null;
            for (int i = 0; i < prList.size(); i++) {
                MobileLedgerProfile p = prList.get(i);
                if (p.equals(profile)) return i;
            }

            return -1;
        }
    }
    @SuppressWarnings("WeakerAccess")
    public static int getProfileIndex(String profileUUID) {
        try (LockHolder ignored = profilesLocker.lockForReading()) {
            List<MobileLedgerProfile> prList = profiles.getValue();
            assert prList != null;
            for (int i = 0; i < prList.size(); i++) {
                MobileLedgerProfile p = prList.get(i);
                if (p.getUuid().equals(profileUUID)) return i;
            }

            return -1;
        }
    }
    public static int retrieveCurrentThemeIdFromDb() {
        String profileUUID = MLDB.getOption(MLDB.OPT_PROFILE_UUID, null);
        if (profileUUID == null) return -1;

        SQLiteDatabase db = MLDB.getDatabase();
        try (Cursor c = db
                .rawQuery("SELECT theme from profiles where uuid=?", new String[]{profileUUID}))
        {
            if (c.moveToNext()) return c.getInt(0);
        }

        return -1;
    }
    public static MobileLedgerProfile getProfile(String profileUUID) {
        MobileLedgerProfile profile;
        try (LockHolder readLock = profilesLocker.lockForReading()) {
            List<MobileLedgerProfile> prList = profiles.getValue();
            assert prList != null;
            if (prList.isEmpty()) {
                readLock.close();
                try (LockHolder ignored = profilesLocker.lockForWriting()) {
                    profile = MobileLedgerProfile.loadAllFromDB(profileUUID);
                }
            }
            else {
                int i = getProfileIndex(profileUUID);
                if (i == -1) i = 0;
                profile = prList.get(i);
            }
        }
        return profile;
    }
}
