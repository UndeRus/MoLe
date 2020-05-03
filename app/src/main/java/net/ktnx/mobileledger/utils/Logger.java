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

import android.util.Log;

import net.ktnx.mobileledger.BuildConfig;

public final class Logger {
    public static void debug(String tag, String msg) {
        if (BuildConfig.DEBUG) Log.d(tag, msg);
    }
    public static void debug(String tag, String msg, Throwable e) {
        if (BuildConfig.DEBUG) Log.d(tag, msg, e);
    }
    public static void warn(String tag, String msg) {
        Log.w(tag, msg);
    }
    public static void warn(String tag, String msg, Throwable e) {
        Log.w(tag, msg, e);
    }
}
