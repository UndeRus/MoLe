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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import net.ktnx.mobileledger.async.RetrieveTransactionsTask;
import net.ktnx.mobileledger.db.DB;
import net.ktnx.mobileledger.db.Profile;
import net.ktnx.mobileledger.utils.Locker;
import net.ktnx.mobileledger.utils.Logger;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import static net.ktnx.mobileledger.utils.Logger.debug;

public final class Data {
    public static final MutableLiveData<Boolean> backgroundTasksRunning =
            new MutableLiveData<>(false);
    public static final MutableLiveData<RetrieveTransactionsTask.Progress> backgroundTaskProgress =
            new MutableLiveData<>();
    public static final LiveData<List<Profile>> profiles = DB.get()
                                                             .getProfileDAO()
                                                             .getAllOrdered();
    public static final MutableLiveData<Currency.Position> currencySymbolPosition =
            new MutableLiveData<>();
    public static final MutableLiveData<Boolean> currencyGap = new MutableLiveData<>(true);
    public static final MutableLiveData<Locale> locale = new MutableLiveData<>();
    public static final MutableLiveData<Boolean> drawerOpen = new MutableLiveData<>(false);
    public static final MutableLiveData<Date> lastUpdateDate = new MutableLiveData<>(null);
    public static final MutableLiveData<Integer> lastUpdateTransactionCount =
            new MutableLiveData<>(0);
    public static final MutableLiveData<Integer> lastUpdateAccountCount = new MutableLiveData<>(0);
    public static final MutableLiveData<String> lastTransactionsUpdateText =
            new MutableLiveData<>();
    public static final MutableLiveData<String> lastAccountsUpdateText = new MutableLiveData<>();
    private static final MutableLiveData<Profile> profile = new InertMutableLiveData<>();
    private static final AtomicInteger backgroundTaskCount = new AtomicInteger(0);
    private static final Locker profilesLocker = new Locker();
    private static NumberFormat numberFormatter;

    static {
        locale.setValue(Locale.getDefault());
    }

    @Nullable
    public static Profile getProfile() {
        return profile.getValue();
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
    public static void setCurrentProfile(@NonNull Profile newProfile) {
        profile.setValue(newProfile);
    }
    public static void postCurrentProfile(@NonNull Profile newProfile) {
        profile.postValue(newProfile);
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

        NumberFormat newNumberFormatter = NumberFormat.getNumberInstance();
        newNumberFormatter.setParseIntegerOnly(false);
        newNumberFormatter.setGroupingUsed(true);
        newNumberFormatter.setGroupingUsed(true);
        newNumberFormatter.setMinimumIntegerDigits(1);
        newNumberFormatter.setMinimumFractionDigits(2);

        numberFormatter = newNumberFormatter;
    }
    public static String formatCurrency(float number) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(locale.getValue());
        return formatter.format(number);
    }
    public static String formatNumber(float number) {
        return numberFormatter.format(number);
    }
    public static void observeProfile(LifecycleOwner lifecycleOwner, Observer<Profile> observer) {
        profile.observe(lifecycleOwner, observer);
    }
    public static void removeProfileObservers(LifecycleOwner owner) {
        profile.removeObservers(owner);
    }
    public static float parseNumber(String str) throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        Number parsed = numberFormatter.parse(str);
        if (parsed == null || pos.getErrorIndex() > -1)
            throw new ParseException("Error parsing '" + str + "'", pos.getErrorIndex());

        return parsed.floatValue();
    }
}