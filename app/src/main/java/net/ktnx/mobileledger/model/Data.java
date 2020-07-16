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
import android.os.AsyncTask;

import androidx.lifecycle.MutableLiveData;

import net.ktnx.mobileledger.App;
import net.ktnx.mobileledger.async.RetrieveTransactionsTask;
import net.ktnx.mobileledger.ui.activity.MainActivity;
import net.ktnx.mobileledger.utils.LockHolder;
import net.ktnx.mobileledger.utils.Locker;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.MLDB;
import net.ktnx.mobileledger.utils.ObservableList;
import net.ktnx.mobileledger.utils.SimpleDate;

import java.lang.ref.WeakReference;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import static net.ktnx.mobileledger.utils.Logger.debug;

public final class Data {
    public static final ObservableList<TransactionListItem> transactions =
            new ObservableList<>(new ArrayList<>());
    public static final MutableLiveData<SimpleDate> earliestTransactionDate =
            new MutableLiveData<>(null);
    public static final MutableLiveData<SimpleDate> latestTransactionDate =
            new MutableLiveData<>(null);
    public static final MutableLiveData<Boolean> backgroundTasksRunning =
            new MutableLiveData<>(false);
    public static final MutableLiveData<Date> lastUpdateDate = new MutableLiveData<>();
    public static final MutableLiveData<MobileLedgerProfile> profile = new InertMutableLiveData<>();
    public static final MutableLiveData<ArrayList<MobileLedgerProfile>> profiles =
            new MutableLiveData<>(null);
    public static final MutableLiveData<String> accountFilter = new MutableLiveData<>();
    public static final MutableLiveData<Currency.Position> currencySymbolPosition =
            new MutableLiveData<>();
    public static final MutableLiveData<Boolean> currencyGap = new MutableLiveData<>(true);
    public static final MutableLiveData<Locale> locale = new MutableLiveData<>(Locale.getDefault());
    private static final AtomicInteger backgroundTaskCount = new AtomicInteger(0);
    private static final Locker profilesLocker = new Locker();
    public static MutableLiveData<Integer> foundTransactionItemIndex = new MutableLiveData<>(null);
    private static RetrieveTransactionsTask retrieveTransactionsTask;
    public static final MutableLiveData<Boolean> drawerOpen = new MutableLiveData<>(false);
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
        MLDB.setOption(MLDB.OPT_PROFILE_UUID, (newProfile == null) ? null : newProfile.getUuid());
        stopTransactionsRetrieval();
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
    public synchronized static void scheduleTransactionListRetrieval(MainActivity activity) {
        if (retrieveTransactionsTask != null) {
            Logger.debug("db", "Ignoring request for transaction retrieval - already active");
            return;
        }
        MobileLedgerProfile pr = profile.getValue();
        if (pr == null) {
            Logger.debug("ui", "Ignoring refresh -- no current profile");
            return;
        }

        retrieveTransactionsTask =
                new RetrieveTransactionsTask(new WeakReference<>(activity), profile.getValue());
        Logger.debug("db", "Created a background transaction retrieval task");

        retrieveTransactionsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    public static synchronized void stopTransactionsRetrieval() {
        if (retrieveTransactionsTask != null)
            retrieveTransactionsTask.cancel(false);
    }
    public static void transactionRetrievalDone() {
        retrieveTransactionsTask = null;
    }
    public static void refreshCurrencyData(Locale locale) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(locale);
        java.util.Currency currency = formatter.getCurrency();
        String symbol = currency.getSymbol();
        Logger.debug("locale", String.format(
                "Discovering currency symbol position for locale %s (currency is %s; symbol is %s)",
                locale.toString(), currency.toString(), symbol));
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

}