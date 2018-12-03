package net.ktnx.mobileledger;

import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract public class RetrieveAccountsTask extends android.os.AsyncTask<SQLiteDatabase, Integer, Void> {
    int error;

    private SharedPreferences pref;
    public void setPref(SharedPreferences pref) {
        this.pref = pref;
    }
    public RetrieveAccountsTask() {
        error = 0;
    }

    protected Void doInBackground(SQLiteDatabase... sqLiteDatabases) {
        final SQLiteDatabase db = sqLiteDatabases[0];
        try {
            HttpURLConnection http = NetworkUtil.prepare_connection( pref, "add");
            http.setAllowUserInteraction(false);
            http.setRequestProperty("Accept-Charset", "UTF-8");
            publishProgress(0);
            InputStream resp = http.getInputStream();
            try {
                Log.d("update_accounts", String.valueOf(http.getResponseCode()));
                if (http.getResponseCode() != 200) {
                    throw new IOException(String.valueOf(R.string.err_http_error));
                }
                else {
                    db.beginTransaction();
                    db.execSQL("delete from accounts;");

                    try {
                        String line;
                        BufferedReader buf = new BufferedReader(new InputStreamReader(resp, "UTF-8"));
                        // %3A is '='
                        Pattern re = Pattern.compile("/register\\?q=inacct%3A([a-zA-Z0-9%]+)\"");
                        int count = 0;
                        while ((line = buf.readLine()) != null) {
                            Matcher m = re.matcher(line);
                            while (m.find()) {
                                String acct_encoded = m.group(1);
                                String acct_name = URLDecoder.decode(acct_encoded, "UTF-8");
                                acct_name = acct_name.replace("\"", "");
                                Log.d("account-parser", acct_name);

                                db.execSQL("insert into accounts(name) values(?)", new Object[]{acct_name} );
                                publishProgress(++count);
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

    abstract protected void onProgressUpdate(Integer... values);

    abstract protected void onPostExecute(Void result);
}
