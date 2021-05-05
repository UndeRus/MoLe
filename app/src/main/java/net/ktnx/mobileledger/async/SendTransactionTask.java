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

package net.ktnx.mobileledger.async;

import android.os.AsyncTask;
import android.util.Log;

import net.ktnx.mobileledger.db.Profile;
import net.ktnx.mobileledger.json.API;
import net.ktnx.mobileledger.json.ApiNotSupportedException;
import net.ktnx.mobileledger.json.Gateway;
import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.model.LedgerTransactionAccount;
import net.ktnx.mobileledger.utils.Globals;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.NetworkUtil;
import net.ktnx.mobileledger.utils.SimpleDate;
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
import static net.ktnx.mobileledger.utils.Logger.debug;

/* TODO: get rid of the custom session/cookie and auth code?
 *       (the last problem with the POST was the missing content-length header)
 *       This will resolve itself when hledger-web 1.14+ is released with Debian/stable,
 *       at which point the HTML form emulation can be dropped entirely
 */

public class SendTransactionTask extends AsyncTask<LedgerTransaction, Void, Void> {
    private final TaskCallback taskCallback;
    private final Profile mProfile;
    private final boolean simulate;
    protected String error;
    private String token;
    private String session;
    private LedgerTransaction transaction;

    public SendTransactionTask(TaskCallback callback, Profile profile,
                               boolean simulate) {
        taskCallback = callback;
        mProfile = profile;
        this.simulate = simulate;
    }
    public SendTransactionTask(TaskCallback callback, Profile profile) {
        taskCallback = callback;
        mProfile = profile;
        simulate = false;
    }
    private void sendOK(API apiVersion) throws IOException, ApiNotSupportedException {
        HttpURLConnection http = NetworkUtil.prepareConnection(mProfile, "add");
        http.setRequestMethod("PUT");
        http.setRequestProperty("Content-Type", "application/json");
        http.setRequestProperty("Accept", "*/*");

        Gateway gateway = Gateway.forApiVersion(apiVersion);
        String body = gateway.transactionSaveRequest(transaction);

        Logger.debug("network", "Sending using API " + apiVersion);
        sendRequest(http, body);
    }
    private void sendRequest(HttpURLConnection http, String body)
            throws IOException, ApiNotSupportedException {
        if (simulate) {
            debug("network", "The request would be: " + body);
            try {
                Thread.sleep(1500);
                if (Math.random() > 0.3)
                    throw new RuntimeException("Simulated test exception");
            }
            catch (InterruptedException ex) {
                Logger.debug("network", ex.toString());
            }

            return;
        }

        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        http.setDoOutput(true);
        http.setDoInput(true);
        http.addRequestProperty("Content-Length", String.valueOf(bodyBytes.length));

        debug("network", "request header: " + http.getRequestProperties()
                                                  .toString());

        try (OutputStream req = http.getOutputStream()) {
            debug("network", "Request body: " + body);
            req.write(bodyBytes);

            final int responseCode = http.getResponseCode();
            debug("network", String.format(Locale.US, "Response: %d %s", responseCode,
                    http.getResponseMessage()));

            try (InputStream resp = http.getErrorStream()) {

                switch (responseCode) {
                    case 200:
                    case 201:
                        break;
                    case 400:
                    case 405: {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(resp));
                        StringBuilder errorLines = new StringBuilder();
                        int count = 0;
                        while (count <= 5) {
                            String line = reader.readLine();
                            if (line == null)
                                break;
                            Logger.debug("network", line);

                            if (errorLines.length() != 0)
                                errorLines.append("\n");

                            errorLines.append(line);
                            count++;
                        }
                        throw new ApiNotSupportedException(errorLines.toString());
                    }
                    default:
                        BufferedReader reader = new BufferedReader(new InputStreamReader(resp));
                        String line = reader.readLine();
                        debug("network", "Response content: " + line);
                        throw new IOException(
                                String.format("Error response code %d", responseCode));
                }
            }
        }
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
        if (token != null)
            params.addPair("_token", token);

        SimpleDate transactionDate = transaction.getDateIfAny();
        if (transactionDate == null)
            transactionDate = SimpleDate.today();

        params.addPair("date", Globals.formatLedgerDate(transactionDate));
        params.addPair("description", transaction.getDescription());
        for (LedgerTransactionAccount acc : transaction.getAccounts()) {
            params.addPair("account", acc.getAccountName());
            if (acc.isAmountSet())
                params.addPair("amount", String.format(Locale.US, "%1.2f", acc.getAmount()));
            else
                params.addPair("amount", "");
        }

        String body = params.toString();
        http.addRequestProperty("Content-Length", String.valueOf(body.length()));

        debug("network", "request header: " + http.getRequestProperties()
                                                  .toString());

        try (OutputStream req = http.getOutputStream()) {
            debug("network", "Request body: " + body);
            req.write(body.getBytes(StandardCharsets.US_ASCII));

            try (InputStream resp = http.getInputStream()) {
                debug("update_accounts", String.valueOf(http.getResponseCode()));
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
                                debug("network", "new session is " + session);
                            }
                            else {
                                debug("network", "set-cookie: " + cookie);
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
                        //debug("dump", line);
                        Matcher m = re.matcher(line);
                        if (m.matches()) {
                            token = m.group(1);
                            debug("save-transaction", line);
                            debug("save-transaction", "Token=" + token);
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
            transaction = ledgerTransactions[0];

            final API profileApiVersion = API.valueOf(mProfile.getApiVersion());
            switch (profileApiVersion) {
                case auto:
                    boolean sendOK = false;
                    for (API ver : API.allVersions) {
                        Logger.debug("network", "Trying version " + ver);
                        try {
                            sendOK(ver);
                            sendOK = true;
                            Logger.debug("network", "Version " + ver + " request succeeded");

                            break;
                        }
                        catch (ApiNotSupportedException e) {
                            Logger.debug("network", "Version " + ver + " seems not supported");
                        }
                    }

                    if (!sendOK) {
                        Logger.debug("network", "Trying HTML form emulation");
                        legacySendOkWithRetry();
                    }
                    break;
                case html:
                    legacySendOkWithRetry();
                    break;
                case v1_14:
                case v1_15:
                case v1_19_1:
                    sendOK(profileApiVersion);
                    break;
                default:
                    throw new IllegalStateException("Unexpected API version: " + profileApiVersion);
            }
        }
        catch (ApiNotSupportedException | Exception e) {
            e.printStackTrace();
            error = e.getMessage();
        }

        return null;
    }
    private void legacySendOkWithRetry() throws IOException {
        int tried = 0;
        while (!legacySendOK()) {
            tried++;
            if (tried >= 2)
                throw new IOException(String.format("aborting after %d tries", tried));
            sleep(100);
        }
    }
    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        taskCallback.onTransactionSaveDone(error, transaction);
    }

}