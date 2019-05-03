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
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.FontsContract;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.FilterQueryProvider;
import android.widget.SimpleCursorAdapter;

import net.ktnx.mobileledger.App;
import net.ktnx.mobileledger.async.DbOpQueue;
import net.ktnx.mobileledger.async.DescriptionSelectedCallback;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.MobileLedgerProfile;

import org.jetbrains.annotations.NonNls;

import java.util.Locale;

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
                try (Cursor cursor = db
                        .rawQuery("select value from options where profile = ? and name=?",
                                new String[]{NO_PROFILE, name}))
                {
                    if (cursor.moveToFirst()) {
                        String result = cursor.getString(0);

                        if (result == null) result = defaultValue;

                        debug("async-db", "option " + name + "=" + result);
                        return result;
                    }
                    else return defaultValue;
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
        try (Cursor cursor = db.rawQuery("select value from options where profile = ? and name=?",
                new String[]{NO_PROFILE, name}))
        {
            if (cursor.moveToFirst()) {
                String result = cursor.getString(0);

                if (result == null) result = default_value;

                debug("db", "option " + name + "=" + result);
                return result;
            }
            else return default_value;
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
                                                 final String table, final String field,
                                                 final boolean profileSpecific) {
        hookAutocompletionAdapter(context, view, table, field, profileSpecific, null, null,
                Data.profile.getValue());
    }
    @TargetApi(Build.VERSION_CODES.N)
    public static void hookAutocompletionAdapter(final Context context,
                                                 final AutoCompleteTextView view,
                                                 final String table, final String field,
                                                 final boolean profileSpecific, final View nextView,
                                                 final DescriptionSelectedCallback callback,
                                                 final MobileLedgerProfile profile) {
        String[] from = {field};
        int[] to = {android.R.id.text1};
        SimpleCursorAdapter adapter =
                new SimpleCursorAdapter(context, android.R.layout.simple_dropdown_item_1line, null,
                        from, to, 0);
        adapter.setStringConversionColumn(1);

        FilterQueryProvider provider = constraint -> {
            if (constraint == null) return null;

            String str = constraint.toString().toUpperCase();
            debug("autocompletion", "Looking for " + str);
            String[] col_names = {FontsContract.Columns._ID, field};
            MatrixCursor c = new MatrixCursor(col_names);

            String sql;
            String[] params;
            if (profileSpecific) {
                sql = String.format("SELECT %s as a, case when %s_upper LIKE ?||'%%' then 1 " +
                                    "WHEN %s_upper LIKE '%%:'||?||'%%' then 2 " +
                                    "WHEN %s_upper LIKE '%% '||?||'%%' then 3 else 9 end " +
                                    "FROM %s " +
                                    "WHERE profile=? AND %s_upper LIKE '%%'||?||'%%' " +
                                    "ORDER BY 2, 1;", field, field, field, field, table, field);
                params = new String[]{str, str, str, profile.getUuid(), str};
            }
            else {
                sql = String.format("SELECT %s as a, case when %s_upper LIKE ?||'%%' then 1 " +
                                    "WHEN %s_upper LIKE '%%:'||?||'%%' then 2 " +
                                    "WHEN %s_upper LIKE '%% '||?||'%%' then 3 " + "else 9 end " +
                                    "FROM %s " + "WHERE %s_upper LIKE '%%'||?||'%%' " +
                                    "ORDER BY 2, 1;", field, field, field, field, table, field);
                params = new String[]{str, str, str, str};
            }
            debug("autocompletion", sql);
            SQLiteDatabase db = App.getDatabase();

            try (Cursor matches = db.rawQuery(sql, params)) {
                int i = 0;
                while (matches.moveToNext()) {
                    String match = matches.getString(0);
                    int order = matches.getInt(1);
                    debug("autocompletion",
                            String.format(Locale.ENGLISH, "match: %s |%d", match, order));
                    c.newRow().add(i++).add(match);
                }
            }

            return c;

        };

        adapter.setFilterQueryProvider(provider);

        view.setAdapter(adapter);

        if (nextView != null) {
            view.setOnItemClickListener((parent, itemView, position, id) -> {
                nextView.requestFocus(View.FOCUS_FORWARD);
                if (callback != null) {
                    callback.descriptionSelected(String.valueOf(view.getText()));
                }
            });
        }
    }
}

