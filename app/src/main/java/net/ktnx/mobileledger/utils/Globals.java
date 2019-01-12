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

package net.ktnx.mobileledger.utils;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.ColorInt;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class Globals {
    @ColorInt
    public static int tableRowEvenBG;
    @ColorInt
    public static int tableRowOddBG;
    @ColorInt
    public static int primaryDark, defaultTextColor;
    public static String[] monthNames;
    private static SimpleDateFormat ledgerDateFormatter =
            new SimpleDateFormat("yyyy/MM/dd", Locale.US);
    public static void hideSoftKeyboard(Activity act) {
        // hide the keyboard
        View v = act.getCurrentFocus();
        if (v != null) {
            InputMethodManager imm =
                    (InputMethodManager) act.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }
    public static Date parseLedgerDate(String dateString) {
        try {
            return ledgerDateFormatter.parse(dateString);
        }
        catch (ParseException e) {
            throw new RuntimeException(String.format("Error parsing date '%s'", dateString), e);
        }
    }
    public static String formatLedgerDate(Date date) {
        return ledgerDateFormatter.format(date);
    }
}