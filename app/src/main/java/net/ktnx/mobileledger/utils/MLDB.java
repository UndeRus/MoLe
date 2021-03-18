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

package net.ktnx.mobileledger.utils;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.ktnx.mobileledger.App;
import net.ktnx.mobileledger.async.DbOpQueue;

import org.jetbrains.annotations.NonNls;

import static net.ktnx.mobileledger.utils.Logger.debug;

public final class MLDB {
    public static final String ACCOUNTS_TABLE = "accounts";
    public static final String DESCRIPTION_HISTORY_TABLE = "description_history";
    public static final String OPT_LAST_SCRAPE = "last_scrape";
    @NonNls
    public static final String OPT_PROFILE_ID = "profile_id";
    private static final String NO_PROFILE = "-";
    @SuppressWarnings("unused")
    static public int getIntOption(String name, int default_value) {
        String s = getOption(name, String.valueOf(default_value));
        try {
            return Integer.parseInt(s);
        }
        catch (Exception e) {
            debug("db", "returning default int value of " + name, e);
            return default_value;
        }
    }
    @SuppressWarnings("unused")
    static public long getLongOption(String name, long default_value) {
        String s = getOption(name, String.valueOf(default_value));
        try {
            return Long.parseLong(s);
        }
        catch (Exception e) {
            debug("db", "returning default long value of " + name, e);
            return default_value;
        }
    }
    static public void getOption(String name, String defaultValue, GetOptCallback cb) {
        AsyncTask<Void, Void, String> t = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                SQLiteDatabase db = App.getDatabase();
                try (Cursor cursor = db.rawQuery(
                        "select value from options where profile=? and name=?",
                        new String[]{NO_PROFILE, name}))
                {
                    if (cursor.moveToFirst()) {
                        String result = cursor.getString(0);

                        if (result == null)
                            result = defaultValue;

                        debug("async-db", "option " + name + "=" + result);
                        return result;
                    }
                    else
                        return defaultValue;
                }
                catch (Exception e) {
                    debug("db", "returning default value for " + name, e);
                    return defaultValue;
                }
            }
            @Override
            protected void onPostExecute(String result) {
                cb.onResult(result);
            }
        };

        t.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
    }
    static public String getOption(String name, String default_value) {
        debug("db", "about to fetch option " + name);
        SQLiteDatabase db = App.getDatabase();
        try (Cursor cursor = db.rawQuery("select value from options where profile=? and name=?",
                new String[]{NO_PROFILE, name}))
        {
            if (cursor.moveToFirst()) {
                String result = cursor.getString(0);

                if (result == null)
                    result = default_value;

                debug("db", "option " + name + "=" + result);
                return result;
            }
            else
                return default_value;
        }
        catch (Exception e) {
            debug("db", "returning default value for " + name, e);
            return default_value;
        }
    }
    static public void setOption(String name, String value) {
        debug("option", String.format("%s := %s", name, value));
        DbOpQueue.add("insert or replace into options(profile, name, value) values(?, ?, ?);",
                new String[]{NO_PROFILE, name, value});
    }
    @SuppressWarnings("unused")
    static public void setLongOption(String name, long value) {
        setOption(name, String.valueOf(value));
    }
    public static void queryInBackground(@NonNull String statement, String[] params,
                                         @NonNull final CallbackHelper callbackHelper) {
        /* All callbacks are called in the new (asynchronous) thread! */
        Thread t = new Thread(() -> {
            callbackHelper.onStart();
            try {
                SQLiteDatabase db = App.getDatabase();

                try (Cursor cursor = db.rawQuery(statement, params)) {
                    boolean gotRow = false;
                    while (cursor.moveToNext()) {
                        gotRow = true;
                        if (!callbackHelper.onRow(cursor))
                            break;
                    }
                    if (!gotRow) {
                        callbackHelper.onNoRows();
                    }
                }
            }
            catch (Exception e) {
                callbackHelper.onException(e);
            }
            finally {
                callbackHelper.onDone();
            }
        });

        t.start();
    }
    /* MLDB.CallbackHelper -- Abstract class for asynchronous SQL query callbacks */
    @SuppressWarnings("WeakerAccess")
    abstract public static class CallbackHelper {
        public void onStart() {}
        public abstract boolean onRow(@NonNull Cursor cursor);
        public void onNoRows() {}
        public void onException(Exception exception) {
            Logger.debug("MLDB", "Exception in asynchronous SQL", exception);
        }
        public void onDone() {}
    }
}

