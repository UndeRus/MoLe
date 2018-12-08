package net.ktnx.mobileledger;

import android.content.SharedPreferences;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.ktnx.mobileledger.MobileLedgerDB.db;

abstract public class RetrieveAccountsTask extends android.os.AsyncTask<Void, Integer, Void> {
    int error;

    private SharedPreferences pref;

    RetrieveAccountsTask() {
        error = 0;
    }

    void setPref(SharedPreferences pref) {
        this.pref = pref;
    }

    protected Void doInBackground(Void... params) {
        try {
            HttpURLConnection http = NetworkUtil.prepare_connection( pref, "add");
            http.setAllowUserInteraction(false);
            http.setRequestProperty("Accept-Charset", "UTF-8");
            publishProgress(0);
            InputStream resp = http.getInputStream();
            try {
                Log.d("update_accounts", String.valueOf(http.getResponseCode()));
                if (http.getResponseCode() != 200) {
                    throw new IOException(String.format("HTTP error: %d %s", http.getResponseCode(), http.getResponseMessage()));
                }
                else {
                    if (db.inTransaction()) throw new AssertionError();

                    db.beginTransaction();

                    try {
                        db.execSQL("update account_values set keep=0;");
                        db.execSQL("update accounts set keep=0;");

                        String line;
                        String last_account_name = null;
                        BufferedReader buf = new BufferedReader(new InputStreamReader(resp, "UTF-8"));
                        // %3A is '='
                        Pattern account_name_re = Pattern.compile("/register\\?q=inacct%3A([a-zA-Z0-9%]+)\"");
                        Pattern value_re = Pattern.compile("<span class=\"[^\"]*\\bamount\\b[^\"]*\">\\s*([-+]?[\\d.,]+)(?:\\s+(\\S+))?</span>");
                        Pattern tr_re = Pattern.compile("</tr>");
                        Pattern descriptions_line_re = Pattern.compile("\\bdescriptionsSuggester\\s*=\\s*new\\b");
                        Pattern description_items_re = Pattern.compile("\"value\":\"([^\"]+)\"");
                        int count = 0;
                        while ((line = buf.readLine()) != null) {
                            Matcher m = account_name_re.matcher(line);
                            if (m.find()) {
                                String acct_encoded = m.group(1);
                                String acct_name = URLDecoder.decode(acct_encoded, "UTF-8");
                                acct_name = acct_name.replace("\"", "");
                                Log.d("account-parser", acct_name);

                                db.execSQL("insert or replace into accounts(name, name_upper, " +
                                                "keep) values(?, ?, 1)",
                                        new Object[]{acct_name, acct_name.toUpperCase()});
                                publishProgress(++count);

                                last_account_name = acct_name;

                                continue;
                            }

                            Matcher tr_m = tr_re.matcher(line);
                            if (tr_m.find()) {
                                Log.d("account-parser", "<tr> - another account expected");
                                last_account_name = null;
                                continue;
                            }

                            if (last_account_name != null) {
                                m = value_re.matcher(line);
                                boolean match_found = false;
                                while (m.find()) {
                                    match_found = true;
                                    String value = m.group(1);
                                    String currency = m.group(2);
                                    if (currency == null) currency = "";
                                    value = value.replace(',', '.');
                                    Log.d("db", "curr=" + currency + ", value=" + value);
                                    db.execSQL("insert or replace into account_values(account, currency, value, keep) values(?, ?, ?, 1);",
                                            new Object[]{last_account_name, currency, Float.valueOf(value)});
                                }

                                if (match_found) continue;
                            }

                            m = descriptions_line_re.matcher(line);
                            if (m.find()) {
                                db.execSQL("update description_history set keep=0;");
                                m = description_items_re.matcher(line);
                                while(m.find()) {
                                    String description = m.group(1);
                                    if (description.isEmpty()) continue;

                                    Log.d("db", String.format("Stored description: %s", description));
                                    db.execSQL("insert or replace into description_history" +
                                                    "(description, description_upper, keep) " +
                                                    "values(?, ?, 1);",
                                            new Object[]{description, description.toUpperCase()});
                                }
                            }
                        }

                        db.execSQL("delete from account_values where keep=0;");
                        db.execSQL("delete from accounts where keep=0;");
//                        db.execSQL("delete from description_history where keep=0;");
                        db.setTransactionSuccessful();
                    }
                    finally {
                        db.endTransaction();
                    }

                }
            }
            finally {
                resp.close();
            }
        } catch (MalformedURLException e) {
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

        return null;
    }

    abstract protected void onProgressUpdate(Integer... values);

    abstract protected void onPostExecute(Void result);
}
