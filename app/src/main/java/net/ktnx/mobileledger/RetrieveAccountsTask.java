package net.ktnx.mobileledger;

import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RetrieveAccountsTask extends android.os.AsyncTask<SQLiteDatabase, Void, Void> {
    private int error;

    SharedPreferences pref;
    public void setPref(SharedPreferences pref) {
        this.pref = pref;
    }
    public RetrieveAccountsTask() {
        error = 0;
    }

    protected Void doInBackground(SQLiteDatabase... sqLiteDatabases) {
        final String backend_url = pref.getString("backend_url", "");
        final boolean use_auth = pref.getBoolean("backend_use_http_auth", false);
        final SQLiteDatabase db = sqLiteDatabases[0];
        try {
            Log.d("update_accounts", "Connecting to "+backend_url);
            HttpURLConnection http = (HttpURLConnection) new URL(backend_url + "/journal").openConnection();
            if (use_auth) {
                final String auth_user = pref.getString("backend_auth_user", "");
                final String auth_password = pref.getString("backend_auth_password", "");
                Authenticator.setDefault(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        Log.d("http-auth", "called");
                        return new PasswordAuthentication(auth_user, auth_password.toCharArray());
                    }
                });
//                final String basic_auth = String.format("Basic %s:%s", auth_user, auth_password);
//                http.setRequestProperty("Authorization", basic_auth);
                Log.d("update_accounts", "Will auth as "+auth_user+" with password of "+auth_password.length()+" characters");
            }
            http.setAllowUserInteraction(true);
            http.setRequestProperty("Accept-Charset", "UTF-8");
            InputStream resp = http.getInputStream();
            try {
                Log.d("update_accounts", String.valueOf(http.getResponseCode()));
                if (http.getResponseCode() != 200) {
                    error = R.string.err_http_error;
                }
                else {
                    db.beginTransaction();
                    db.execSQL("delete from accounts;");

                    try {
                        String line;
                        BufferedReader buf = new BufferedReader(new InputStreamReader(resp, "UTF-8"));
                        // %3A is '='
                        Pattern re = Pattern.compile('"' + backend_url + "/register\\?q=inacct%3A([a-zA-Z0-9%]+)\\\"");
                        while ((line = buf.readLine()) != null) {
                            Matcher m = re.matcher(line);
                            while (m.find()) {
                                String acct_encoded = m.group(1);
                                String acct_name = URLDecoder.decode(acct_encoded, "UTF-8");
                                acct_name = acct_name.replace("\"", "");
                                Log.d("account-parser", acct_name);

                                db.execSQL("insert into accounts(name) values(?)", new Object[]{acct_name} );
                            }
                        }

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
        } catch (IOException e) {
            error = R.string.err_net_io_error;
            e.printStackTrace();
        }
        catch (Exception e) {
            error = R.string.err_net_error;
            e.printStackTrace();
        }

        return null;
    }

    protected void onPostExecute(Void result) {
        if (error != 0)
            Log.e("async-http", String.valueOf(error));
        else
            Log.d("async-http", "Accounts updated successfuly");
    }
}
