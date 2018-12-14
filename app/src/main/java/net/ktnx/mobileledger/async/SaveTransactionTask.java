/*
 * Copyright © 2018 Damyan Ivanov.
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
import android.os.AsyncTask;
import android.util.Log;

import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.model.LedgerTransactionItem;
import net.ktnx.mobileledger.utils.NetworkUtil;
import net.ktnx.mobileledger.utils.UrlEncodedFormData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Thread.sleep;

public class SaveTransactionTask extends AsyncTask<LedgerTransaction, Void, Void> {
    private final TaskCallback task_callback;
    private String token;
    private String session;
    private String backend_url;
    private LedgerTransaction ltr;
    protected String error;

    private SharedPreferences pref;
    public void setPref(SharedPreferences pref) {
        this.pref = pref;
    }

    public SaveTransactionTask(TaskCallback callback) {
        task_callback = callback;
    }
    private boolean send_ok() throws IOException {
        HttpURLConnection http = NetworkUtil.prepare_connection(pref, "add");
        http.setRequestMethod("POST");
        http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        http.setRequestProperty("Accept", "*/*");
        if ((session != null) && !session.isEmpty()) {
            http.setRequestProperty("Cookie", String.format("_SESSION=%s", session));
        }
        http.setDoOutput(true);
        http.setDoInput(true);

        UrlEncodedFormData params = new UrlEncodedFormData();
        params.add_pair("_formid", "identify-add");
        if (token != null) params.add_pair("_token", token);
        params.add_pair("date", ltr.getDate());
        params.add_pair("description", ltr.getDescription());
        {
            Iterator<LedgerTransactionItem> items = ltr.getItemsIterator();
            while (items.hasNext()) {
                LedgerTransactionItem item = items.next();
                params.add_pair("account", item.getAccountName());
                if (item.isAmountSet())
                    params.add_pair("amount", String.format(Locale.US, "%1.2f", item.getAmount()));
                else params.add_pair("amount", "");
            }
        }

        String body = params.toString();
        http.addRequestProperty("Content-Length", String.valueOf(body.length()));

        Log.d("network", "request header: " + http.getRequestProperties().toString());

        try (OutputStream req = http.getOutputStream()) {
            Log.d("network", "Request body: " + body);
            req.write(body.getBytes("ASCII"));

            try (InputStream resp = http.getInputStream()) {
                Log.d("update_accounts", String.valueOf(http.getResponseCode()));
                if (http.getResponseCode() == 303) {
                    // everything is fine
                    return true;
                } else if (http.getResponseCode() == 200) {
                    // get the new cookie
                    {
                        Pattern sess_cookie_re = Pattern.compile("_SESSION=([^;]+);.*");

                        Map<String, List<String>> header = http.getHeaderFields();
                        List<String> cookie_header = header.get("Set-Cookie");
                        if (cookie_header != null) {
                            String cookie = cookie_header.get(0);
                            Matcher m = sess_cookie_re.matcher(cookie);
                            if (m.matches()) {
                                session = m.group(1);
                                Log.d("network", "new session is " + session);
                            } else {
                                Log.d("network", "set-cookie: " + cookie);
                                Log.w("network", "Response Set-Cookie headers is not a _SESSION one");
                            }
                        } else {
                            Log.w("network", "Response has no Set-Cookie header");
                        }
                    }
                    // the token needs to be updated
                    BufferedReader reader = new BufferedReader(new InputStreamReader(resp));
                    Pattern re = Pattern.compile("<input type=\"hidden\" name=\"_token\" value=\"([^\"]+)\">");
                    String line;
                    while ((line = reader.readLine()) != null) {
                        //Log.d("dump", line);
                        Matcher m = re.matcher(line);
                        if (m.matches()) {
                            token = m.group(1);
                            Log.d("save-transaction", line);
                            Log.d("save-transaction", "Token=" + token);
                            return false;       // retry
                        }
                    }
                    throw new IOException("Can't find _token string");
                } else {
                    throw new IOException(String.format("Error response code %d", http.getResponseCode()));
                }
            }
        }
    }

    @Override
    protected Void doInBackground(LedgerTransaction... ledgerTransactions) {
        error = null;
        try {
            backend_url = pref.getString("backend_url", "");
            ltr = ledgerTransactions[0];

            int tried = 0;
            while (!send_ok()) {
                try {
                    tried++;
                    if (tried >= 2)
                        throw new IOException(String.format("aborting after %d tries", tried));
                    sleep(100);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            error = e.getMessage();
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        task_callback.done(error);
    }
}
