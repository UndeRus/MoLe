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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.Date;

public class SimpleDate {
    public int year;
    public int month;
    public int day;
    public SimpleDate(int y, int m, int d) {
        year = y;
        month = m;
        day = d;
    }
    public static SimpleDate fromDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return fromCalendar(calendar);
    }
    public static SimpleDate fromCalendar(Calendar calendar) {
        return new SimpleDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DATE));
    }
    public static SimpleDate today() {
        return fromCalendar(Calendar.getInstance());
    }
    public Calendar toCalendar() {
        Calendar result = Calendar.getInstance();
        result.set(year, month - 1, day);
        return result;
    }
    public Date toDate() {
        return toCalendar().getTime();
    }
    public boolean equals(@Nullable SimpleDate date) {
        if (date == null)
            return false;

        if (year != date.year)
            return false;
        if (month != date.month)
            return false;
        if (day != date.day)
            return false;

        return true;
    }
    public boolean earlierThan(@NonNull SimpleDate date) {
        if (year < date.year)
            return true;
        if (year > date.year)
            return false;
        if (month < date.month)
            return true;
        if (month > date.month)
            return false;
        return (day < date.day);
    }
    public boolean laterThan(@NonNull SimpleDate date) {
        if (year > date.year)
            return true;
        if (year < date.year)
            return false;
        if (month > date.month)
            return true;
        if (month < date.month)
            return false;
        return (day > date.day);
    }
}
