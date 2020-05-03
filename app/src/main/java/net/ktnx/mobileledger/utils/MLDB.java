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

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.widget.AutoCompleteTextView;
import android.widget.FilterQueryProvider;
import android.widget.SimpleCursorAdapter;

import androidx.annotation.NonNull;

import net.ktnx.mobileledger.App;
import net.ktnx.mobileledger.async.DbOpQueue;
import net.ktnx.mobileledger.async.DescriptionSelectedCallback;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.MobileLedgerProfile;

import org.jetbrains.annotations.NonNls;

import static net.ktnx.mobileledger.utils.Logger.debug;

public final class MLDB {
    public static final String ACCOUNTS_TABLE = "accounts";
    public static final String DESCRIPTION_HISTORY_TABLE = "description_history";
    public static final String OPT_LAST_SCRAPE = "last_scrape";
    @NonNls
    public static final String OPT_PROFILE_UUID = "profile_uuid";
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
    @TargetApi(Build.VERSION_CODES.N)
    public static void hookAutocompletionAdapter(final Context context,
                                                 final AutoCompleteTextView view,
                                                 final String table, final String field) {
        hookAutocompletionAdapter(context, view, table, field, true, null, null);
    }
    @TargetApi(Build.VERSION_CODES.N)
    public static void hookAutocompletionAdapter(final Context context,
                                                 final AutoCompleteTextView view,
                                                 final String table, final String field,
                                                 final boolean profileSpecific,
                                                 final DescriptionSelectedCallback callback,
                                                 final MobileLedgerProfile profile) {
        String[] from = {field};
        int[] to = {android.R.id.text1};
        SimpleCursorAdapter adapter =
                new SimpleCursorAdapter(context, android.R.layout.simple_dropdown_item_1line, null,
                        from, to, 0);
        adapter.setStringConversionColumn(1);

        FilterQueryProvider provider = constraint -> {
            if (constraint == null)
                return null;

            String str = constraint.toString()
                                   .toUpperCase();
            debug("autocompletion", "Looking for " + str);

            String sql;
            String[] params;
            if (profileSpecific) {
                MobileLedgerProfile p = (profile == null) ? Data.profile.getValue() : profile;
                if (p == null)
                    throw new AssertionError();
                sql = String.format(
                        "SELECT rowid as _id, %s, CASE WHEN %s_upper LIKE ?||'%%' THEN 1 " +
                        "WHEN %s_upper LIKE '%%:'||?||'%%' then 2 " +
                        "WHEN %s_upper LIKE '%% '||?||'%%' THEN 3 " + "ELSE 9 END " + "FROM %s " +
                        "WHERE profile=? AND %s_upper LIKE '%%'||?||'%%' " +
                        "ORDER BY 3, %s_upper, 1;", field, field, field, field, table, field,
                        field);
                params = new String[]{str, str, str, p.getUuid(), str};
            }
            else {
                sql = String.format(
                        "SELECT rowid as _id, %s, CASE WHEN %s_upper LIKE ?||'%%' THEN 1 " +
                        "WHEN %s_upper LIKE '%%:'||?||'%%' THEN 2 " +
                        "WHEN %s_upper LIKE '%% '||?||'%%' THEN 3 " + "ELSE 9 END " + "FROM %s " +
                        "WHERE %s_upper LIKE '%%'||?||'%%' " + "ORDER BY 3, %s_upper, 1;", field,
                        field, field, field, table, field, field);
                params = new String[]{str, str, str, str};
            }
            debug("autocompletion", sql);
            SQLiteDatabase db = App.getDatabase();

            return db.rawQuery(sql, params);
        };

        adapter.setFilterQueryProvider(provider);

        view.setAdapter(adapter);

        if (callback != null)
            view.setOnItemClickListener(
                    (parent, itemView, position, id) -> callback.descriptionSelected(
                            String.valueOf(view.getText())));
    }
    public static void queryInBackground(@NonNull String statement, @NonNull String[] params,
                                         @NonNull CallbackHelper callbackHelper) {
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

