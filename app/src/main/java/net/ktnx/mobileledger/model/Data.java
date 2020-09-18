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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import net.ktnx.mobileledger.App;
import net.ktnx.mobileledger.async.RetrieveTransactionsTask;
import net.ktnx.mobileledger.utils.LockHolder;
import net.ktnx.mobileledger.utils.Locker;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.MLDB;
import net.ktnx.mobileledger.utils.ObservableValue;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static net.ktnx.mobileledger.utils.Logger.debug;

public final class Data {
    public static final MutableLiveData<Boolean> backgroundTasksRunning =
            new MutableLiveData<>(false);
    public static final MutableLiveData<RetrieveTransactionsTask.Progress> backgroundTaskProgress =
            new MutableLiveData<>();
    public static final MutableLiveData<ArrayList<MobileLedgerProfile>> profiles =
            new MutableLiveData<>(null);
    public static final MutableLiveData<Currency.Position> currencySymbolPosition =
            new MutableLiveData<>();
    public static final MutableLiveData<Boolean> currencyGap = new MutableLiveData<>(true);
    public static final MutableLiveData<Locale> locale = new MutableLiveData<>();
    public static final MutableLiveData<Boolean> drawerOpen = new MutableLiveData<>(false);
    public static final MutableLiveData<Date> lastUpdateLiveData = new MutableLiveData<>(null);
    public static final ObservableValue<Long> lastUpdate = new ObservableValue<>();
    private static final MutableLiveData<MobileLedgerProfile> profile =
            new InertMutableLiveData<>();
    private static final AtomicInteger backgroundTaskCount = new AtomicInteger(0);
    private static final Locker profilesLocker = new Locker();

    static {
        locale.setValue(Locale.getDefault());
    }

    @NonNull
    public static MobileLedgerProfile getProfile() {
        return Objects.requireNonNull(profile.getValue());
    }
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
    public static void setCurrentProfile(@NonNull MobileLedgerProfile newProfile) {
        MLDB.setOption(MLDB.OPT_PROFILE_UUID, newProfile.getUuid());
        profile.setValue(newProfile);
    }
    public static int getProfileIndex(MobileLedgerProfile profile) {
        try (LockHolder ignored = profilesLocker.lockForReading()) {
            List<MobileLedgerProfile> prList = profiles.getValue();
            if (prList == null)
                throw new AssertionError();
            for (int i = 0; i < prList.size(); i++) {
                MobileLedgerProfile p = prList.get(i);
                if (p.equals(profile))
                    return i;
            }

            return -1;
        }
    }
    @SuppressWarnings("WeakerAccess")
    public static int getProfileIndex(String profileUUID) {
        try (LockHolder ignored = profilesLocker.lockForReading()) {
            List<MobileLedgerProfile> prList = profiles.getValue();
            if (prList == null)
                throw new AssertionError();
            for (int i = 0; i < prList.size(); i++) {
                MobileLedgerProfile p = prList.get(i);
                if (p.getUuid()
                     .equals(profileUUID))
                    return i;
            }

            return -1;
        }
    }
    public static int retrieveCurrentThemeIdFromDb() {
        String profileUUID = MLDB.getOption(MLDB.OPT_PROFILE_UUID, null);
        if (profileUUID == null)
            return -1;

        SQLiteDatabase db = App.getDatabase();
        try (Cursor c = db.rawQuery("SELECT theme from profiles where uuid=?",
                new String[]{profileUUID}))
        {
            if (c.moveToNext())
                return c.getInt(0);
        }

        return -1;
    }
    @Nullable
    public static MobileLedgerProfile getProfile(String profileUUID) {
        MobileLedgerProfile profile;
        try (LockHolder readLock = profilesLocker.lockForReading()) {
            List<MobileLedgerProfile> prList = profiles.getValue();
            if ((prList == null) || prList.isEmpty()) {
                readLock.close();
                try (LockHolder ignored = profilesLocker.lockForWriting()) {
                    profile = MobileLedgerProfile.loadAllFromDB(profileUUID);
                }
            }
            else {
                int i = getProfileIndex(profileUUID);
                if (i == -1)
                    i = 0;
                profile = prList.get(i);
            }
        }
        return profile;
    }
    public static void refreshCurrencyData(Locale locale) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(locale);
        java.util.Currency currency = formatter.getCurrency();
        String symbol = currency != null ? currency.getSymbol() : "";
        Logger.debug("locale", String.format(
                "Discovering currency symbol position for locale %s (currency is %s; symbol is %s)",
                locale.toString(), currency != null ? currency.toString() : "<none>", symbol));
        String formatted = formatter.format(1234.56f);
        Logger.debug("locale", String.format("1234.56 formats as '%s'", formatted));

        if (formatted.startsWith(symbol)) {
            currencySymbolPosition.setValue(Currency.Position.before);

            // is the currency symbol directly followed by the first formatted digit?
            final char canary = formatted.charAt(symbol.length());
            currencyGap.setValue(canary != '1');
        }
        else if (formatted.endsWith(symbol)) {
            currencySymbolPosition.setValue(Currency.Position.after);

            // is the currency symbol directly preceded bu the last formatted digit?
            final char canary = formatted.charAt(formatted.length() - symbol.length() - 1);
            currencyGap.setValue(canary != '6');
        }
        else
            currencySymbolPosition.setValue(Currency.Position.none);
    }

    public static void observeProfile(LifecycleOwner lifecycleOwner,
                                      Observer<MobileLedgerProfile> observer) {
        profile.observe(lifecycleOwner, observer);
    }
    public synchronized static MobileLedgerProfile initProfile() {
        MobileLedgerProfile currentProfile = profile.getValue();
        if (currentProfile != null)
            return currentProfile;

        String profileUUID = MLDB.getOption(MLDB.OPT_PROFILE_UUID, null);
        MobileLedgerProfile startupProfile = getProfile(profileUUID);
        if (startupProfile != null)
            setCurrentProfile(startupProfile);
        return startupProfile;
    }

    public static void removeProfileObservers(LifecycleOwner owner) {
        profile.removeObservers(owner);
    }
}