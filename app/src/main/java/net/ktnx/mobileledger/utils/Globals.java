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

package net.ktnx.mobileledger.utils;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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
    public static final String developerEmail = "dam+mole-crash@ktnx.net";
    private static final Pattern reLedgerDate =
            Pattern.compile("^(?:(?:(\\d+)/)??(\\d\\d?)/)?(\\d\\d?)$");
    public static void hideSoftKeyboard(Activity act) {
        // hide the keyboard
        View v = act.getCurrentFocus();
        if (v != null) {
            InputMethodManager imm =
                    (InputMethodManager) act.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }
    public static SimpleDate parseLedgerDate(String dateString) throws ParseException {
        Matcher m = reLedgerDate.matcher(dateString);
        if (!m.matches()) throw new ParseException(
                String.format("'%s' does not match expected pattern '%s'", dateString,
                        reLedgerDate.toString()), 0);

        String yearStr = m.group(1);
        String monthStr = m.group(2);
        String dayStr = m.group(3);

        int year, month, day;

        String toParse;
        if (yearStr == null) {
            SimpleDate today = SimpleDate.today();
            year = today.year;
            if (monthStr == null) {
                month = today.month;
            }
            else month = Integer.parseInt(monthStr);
        }
        else {
            year = Integer.parseInt(yearStr);
            assert monthStr != null;
            month = Integer.parseInt(monthStr);
        }

        assert dayStr != null;
        day = Integer.parseInt(dayStr);

        return new SimpleDate(year, month, day);
    }
    public static Calendar parseLedgerDateAsCalendar(String dateString) throws ParseException {
        return parseLedgerDate(dateString).toCalendar();
    }
    public static SimpleDate parseIsoDate(String dateString) throws ParseException {
        return SimpleDate.fromDate(isoDateFormatter.get().parse(dateString));
    }
    public static String formatLedgerDate(SimpleDate date) {
        return dateFormatter.get().format(date.toDate());
    }
    public static String formatIsoDate(SimpleDate date) {
        return isoDateFormatter.get().format(date.toDate());
    }
}