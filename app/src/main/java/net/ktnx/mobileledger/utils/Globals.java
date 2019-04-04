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

package net.ktnx.mobileledger.utils;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Globals {
    private static final ThreadLocal<SimpleDateFormat> dateFormatter =
            new ThreadLocal<SimpleDateFormat>() {
                @Override
                protected SimpleDateFormat initialValue() {
                    return new SimpleDateFormat("yyyy/MM/dd", Locale.US);
                }
            };
    private static final ThreadLocal<SimpleDateFormat> isoDateFormatter =
            new ThreadLocal<SimpleDateFormat>() {
                @Override
                protected SimpleDateFormat initialValue() {
                    return new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                }
            };
    public static String[] monthNames;
    public static String developerEmail = "dam+mole-crash@ktnx.net";
    private static Pattern reLedgerDate =
            Pattern.compile("^(?:(\\d+)/)??(?:(\\d\\d?)/)?(\\d\\d?)$");
    public static void hideSoftKeyboard(Activity act) {
        // hide the keyboard
        View v = act.getCurrentFocus();
        if (v != null) {
            InputMethodManager imm =
                    (InputMethodManager) act.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }
    public static Date parseLedgerDate(String dateString) throws ParseException {
        Matcher m = reLedgerDate.matcher(dateString);
        if (!m.matches()) throw new ParseException(
                String.format("'%s' does not match expected pattern '%s'", dateString,
                        reLedgerDate.toString()), 0);

        String year = m.group(1);
        String month = m.group(2);
        String day = m.group(3);

        String toParse;
        if (year == null) {
            Calendar now = Calendar.getInstance();
            int thisYear = now.get(Calendar.YEAR);
            if (month == null) {
                int thisMonth = now.get(Calendar.MONTH) + 1;
                toParse = String.format(Locale.US, "%04d/%02d/%s", thisYear, thisMonth, dateString);
            }
            else toParse = String.format(Locale.US, "%04d/%s", thisYear, dateString);
        }
        else toParse = dateString;

        return dateFormatter.get().parse(toParse);
    }
    public static Date parseIsoDate(String dateString) throws ParseException {
        return isoDateFormatter.get().parse(dateString);
    }
    public static String formatLedgerDate(Date date) {
        return dateFormatter.get().format(date);
    }
    public static String formatIsoDate(Date date) {
        return isoDateFormatter.get().format(date);
    }
}