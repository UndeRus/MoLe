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

package net.ktnx.mobileledger.async;

import android.os.AsyncTask;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import net.ktnx.mobileledger.json.ParsedLedgerTransaction;
import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.model.LedgerTransactionAccount;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
import net.ktnx.mobileledger.utils.Globals;
import net.ktnx.mobileledger.utils.NetworkUtil;
import net.ktnx.mobileledger.utils.UrlEncodedFormData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.os.SystemClock.sleep;

public class SendTransactionTask extends AsyncTask<LedgerTransaction, Void, Void> {
    private final TaskCallback taskCallback;
    protected String error;
    private String token;
    private String session;
    private LedgerTransaction ltr;
    private MobileLedgerProfile mProfile;

    public SendTransactionTask(TaskCallback callback, MobileLedgerProfile profile) {
        taskCallback = callback;
        mProfile = profile;
    }
    private boolean sendOK() throws IOException {
        HttpURLConnection http = NetworkUtil.prepareConnection(mProfile, "add");
        http.setRequestMethod("PUT");
        http.setRequestProperty("Content-Type", "application/json");
        http.setRequestProperty("Accept", "*/*");

        ParsedLedgerTransaction jsonTransaction;
        jsonTransaction = ltr.toParsedLedgerTransaction();
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writerFor(ParsedLedgerTransaction.class);
        String body = writer.writeValueAsString(jsonTransaction);

        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        http.setDoOutput(true);
        http.setDoInput(true);
        http.addRequestProperty("Content-Length", String.valueOf(bodyBytes.length));

        Log.d("network", "request header: " + http.getRequestProperties().toString());

        try (OutputStream req = http.getOutputStream()) {
            Log.d("network", "Request body: " + body);
            req.write(bodyBytes);

            final int responseCode = http.getResponseCode();
            Log.d("network",
                    String.format("Response: %d %s", responseCode, http.getResponseMessage()));

            try (InputStream resp = http.getErrorStream()) {

                switch (responseCode) {
                    case 200:
                    case 201:
                        break;
                    case 405:
                        return false; // will cause a retry with the legacy method
                    default:
                        BufferedReader reader = new BufferedReader(new InputStreamReader(resp));
                        String line = reader.readLine();
                        Log.d("network", "Response content: " + line);
                        throw new IOException(
                                String.format("Error response code %d", responseCode));
                }
            }
        }

        return true;
    }
    private boolean legacySendOK() throws IOException {
        HttpURLConnection http = NetworkUtil.prepareConnection(mProfile, "add");
        http.setRequestMethod("POST");
        http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        http.setRequestProperty("Accept", "*/*");
        if ((session != null) && !session.isEmpty()) {
            http.setRequestProperty("Cookie", String.format("_SESSION=%s", session));
        }
        http.setDoOutput(true);
        http.setDoInput(true);

        UrlEncodedFormData params = new UrlEncodedFormData();
        params.addPair("_formid", "identify-add");
        if (token != null) params.addPair("_token", token);
        params.addPair("date", Globals.formatLedgerDate(ltr.getDate()));
        params.addPair("description", ltr.getDescription());
        for (LedgerTransactionAccount acc : ltr.getAccounts()) {
            params.addPair("account", acc.getAccountName());
            if (acc.isAmountSet())
                params.addPair("amount", String.format(Locale.US, "%1.2f", acc.getAmount()));
            else params.addPair("amount", "");
        }

        String body = params.toString();
        http.addRequestProperty("Content-Length", String.valueOf(body.length()));

        Log.d("network", "request header: " + http.getRequestProperties().toString());

        try (OutputStream req = http.getOutputStream()) {
            Log.d("network", "Request body: " + body);
            req.write(body.getBytes(StandardCharsets.US_ASCII));

            try (InputStream resp = http.getInputStream()) {
                Log.d("update_accounts", String.valueOf(http.getResponseCode()));
                if (http.getResponseCode() == 303) {
                    // everything is fine
                    return true;
                }
                else if (http.getResponseCode() == 200) {
                    // get the new cookie
                    {
                        Pattern reSessionCookie = Pattern.compile("_SESSION=([^;]+);.*");

                        Map<String, List<String>> header = http.getHeaderFields();
                        List<String> cookieHeader = header.get("Set-Cookie");
                        if (cookieHeader != null) {
                            String cookie = cookieHeader.get(0);
                            Matcher m = reSessionCookie.matcher(cookie);
                            if (m.matches()) {
                                session = m.group(1);
                                Log.d("network", "new session is " + session);
                            }
                            else {
                                Log.d("network", "set-cookie: " + cookie);
                                Log.w("network",
                                        "Response Set-Cookie headers is not a _SESSION one");
                            }
                        }
                        else {
                            Log.w("network", "Response has no Set-Cookie header");
                        }
                    }
                    // the token needs to be updated
                    BufferedReader reader = new BufferedReader(new InputStreamReader(resp));
                    Pattern re = Pattern.compile(
                            "<input type=\"hidden\" name=\"_token\" value=\"([^\"]+)\">");
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
                }
                else {
                    throw new IOException(
                            String.format("Error response code %d", http.getResponseCode()));
                }
            }
        }
    }

    @Override
    protected Void doInBackground(LedgerTransaction... ledgerTransactions) {
        error = null;
        try {
            ltr = ledgerTransactions[0];

            if (!sendOK()) {
                int tried = 0;
                while (!legacySendOK()) {
                        tried++;
                        if (tried >= 2)
                            throw new IOException(String.format("aborting after %d tries", tried));
                        sleep(100);
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
        taskCallback.done(error);
    }
}
