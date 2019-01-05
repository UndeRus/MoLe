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

package net.ktnx.mobileledger.async;

import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.OperationCanceledException;
import android.util.Log;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerAccount;
import net.ktnx.mobileledger.ui.activity.MainActivity;
import net.ktnx.mobileledger.utils.MLDB;
import net.ktnx.mobileledger.utils.NetworkUtil;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RetrieveAccountsTask extends android.os.AsyncTask<Void, Integer, Void> {
    int error;
    WeakReference<MainActivity> mContext;
    private SharedPreferences pref;

    public RetrieveAccountsTask(WeakReference<MainActivity> context) {
        mContext = context;
        error = 0;
    }

    public void setPref(SharedPreferences pref) {
        this.pref = pref;
    }

    protected Void doInBackground(Void... params) {
        Data.backgroundTaskCount.incrementAndGet();
        try {
            HttpURLConnection http = NetworkUtil.prepare_connection(pref, "add");
            publishProgress(0);
            try (SQLiteDatabase db = MLDB.getWritableDatabase()) {
                try (InputStream resp = http.getInputStream()) {
                    Log.d("update_accounts", String.valueOf(http.getResponseCode()));
                    if (http.getResponseCode() != 200) {
                        throw new IOException(
                                String.format("HTTP error: %d %s", http.getResponseCode(),
                                        http.getResponseMessage()));
                    }
                    else {
                        if (db.inTransaction()) throw new AssertionError();

                        db.beginTransaction();

                        try {
                            db.execSQL("update account_values set keep=0;");
                            db.execSQL("update accounts set keep=0;");

                            String line;
                            String last_account_name = null;
                            BufferedReader buf =
                                    new BufferedReader(new InputStreamReader(resp, "UTF-8"));
                            // %3A is '='
                            Pattern account_name_re =
                                    Pattern.compile("/register\\?q=inacct%3A([a-zA-Z0-9%]+)\"");
                            Pattern account_value_re = Pattern.compile(
                                    "<span class=\"[^\"]*\\bamount\\b[^\"]*\">\\s*([-+]?[\\d.,]+)(?:\\s+(\\S+))?</span>");
                            Pattern tr_end_re = Pattern.compile("</tr>");
                            Pattern descriptions_line_re =
                                    Pattern.compile("\\bdescriptionsSuggester\\s*=\\s*new\\b");
                            Pattern description_items_re =
                                    Pattern.compile("\"value\":\"([^\"]+)\"");
                            int count = 0;
                            while ((line = buf.readLine()) != null) {
                                throwIfCancelled();

                                Matcher m = account_name_re.matcher(line);
                                if (m.find()) {
                                    String acct_encoded = m.group(1);
                                    String acct_name = URLDecoder.decode(acct_encoded, "UTF-8");
                                    acct_name = acct_name.replace("\"", "");
                                    Log.d("account-parser", acct_name);

                                    addAccount(db, acct_name);
                                    publishProgress(++count);

                                    last_account_name = acct_name;

                                    continue;
                                }

                                Matcher tr_m = tr_end_re.matcher(line);
                                if (tr_m.find()) {
                                    Log.d("account-parser", "<tr> - another account expected");
                                    last_account_name = null;
                                    continue;
                                }

                                if (last_account_name != null) {
                                    m = account_value_re.matcher(line);
                                    boolean match_found = false;
                                    while (m.find()) {
                                        throwIfCancelled();

                                        match_found = true;
                                        String value = m.group(1);
                                        String currency = m.group(2);
                                        if (currency == null) currency = "";
                                        value = value.replace(',', '.');
                                        Log.d("db", "curr=" + currency + ", value=" + value);
                                        db.execSQL(
                                                "insert or replace into account_values(account, currency, value, keep) values(?, ?, ?, 1);",
                                                new Object[]{last_account_name, currency,
                                                             Float.valueOf(value)
                                                });
                                    }

                                    if (match_found) continue;
                                }

                                m = descriptions_line_re.matcher(line);
                                if (m.find()) {
                                    db.execSQL("update description_history set keep=0;");
                                    m = description_items_re.matcher(line);
                                    while (m.find()) {
                                        throwIfCancelled();

                                        String description = m.group(1);
                                        if (description.isEmpty()) continue;

                                        Log.d("db", String.format("Stored description: %s",
                                                description));
                                        db.execSQL("insert or replace into description_history" +
                                                   "(description, description_upper, keep) " +
                                                   "values(?, ?, 1);",
                                                new Object[]{description, description.toUpperCase()
                                                });
                                    }
                                }
                            }

                            db.execSQL("delete from account_values where keep=0;");
                            db.execSQL("delete from accounts where keep=0;");
                            db.setTransactionSuccessful();
                        }
                        catch (OperationCanceledException e) {
                            Log.w("async", "Account retrieval cancelled");
                        }
                        finally {
                            db.endTransaction();
                        }
                    }
                }
            }
        }
        catch (MalformedURLException e) {
            error = R.string.err_bad_backend_url;
            e.printStackTrace();
        }
        catch (FileNotFoundException e) {
            error = R.string.err_bad_auth;
            e.printStackTrace();
        }
        catch (IOException e) {
            error = R.string.err_net_io_error;
            e.printStackTrace();
        }
        catch (Exception e) {
            error = R.string.err_net_error;
            e.printStackTrace();
        }
        finally {
            Log.d("RAT", "decrementing background task count");
            Data.backgroundTaskCount.decrementAndGet();
        }

        return null;
    }
    private void throwIfCancelled() {
        if (isCancelled()) throw new OperationCanceledException(null);
    }

    private void addAccount(SQLiteDatabase db, String name) {
        do {
            LedgerAccount acc = new LedgerAccount(name);
            db.execSQL("update accounts set level = ?, keep = 1 where name = ?",
                    new Object[]{acc.getLevel(), name});
            db.execSQL("insert into accounts(name, name_upper, parent_name, level) select ?,?," +
                       "?,? " + "where (select changes() = 0)",
                    new Object[]{name, name.toUpperCase(), acc.getParentName(), acc.getLevel()});
            name = acc.getParentName();
        } while (name != null);
    }
    @Override
    protected void onPostExecute(Void result) {
        MainActivity ctx = mContext.get();
        if (ctx == null) return;
        ctx.onRetrieveDone(this.error == 0);
    }

}
