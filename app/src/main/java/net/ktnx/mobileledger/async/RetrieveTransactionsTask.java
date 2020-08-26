/*
 * Copyright © 2020 Damyan Ivanov.
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

import android.annotation.SuppressLint;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.OperationCanceledException;

import androidx.annotation.NonNull;

import net.ktnx.mobileledger.App;
import net.ktnx.mobileledger.err.HTTPException;
import net.ktnx.mobileledger.json.v1_15.AccountListParser;
import net.ktnx.mobileledger.json.v1_15.ParsedBalance;
import net.ktnx.mobileledger.json.v1_15.ParsedLedgerAccount;
import net.ktnx.mobileledger.json.v1_15.ParsedLedgerTransaction;
import net.ktnx.mobileledger.json.v1_15.TransactionListParser;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerAccount;
import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.model.LedgerTransactionAccount;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
import net.ktnx.mobileledger.utils.NetworkUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RetrieveTransactionsTask
        extends AsyncTask<Void, RetrieveTransactionsTask.Progress, String> {
    private static final int MATCHING_TRANSACTIONS_LIMIT = 150;
    private static final Pattern reComment = Pattern.compile("^\\s*;");
    private static final Pattern reTransactionStart = Pattern.compile(
            "<tr class=\"title\" " + "id=\"transaction-(\\d+)" + "\"><td class=\"date" +
            "\"[^\"]*>([\\d.-]+)</td>");
    private static final Pattern reTransactionDescription =
            Pattern.compile("<tr class=\"posting\" title=\"(\\S+)\\s(.+)");
    private static final Pattern reTransactionDetails = Pattern.compile(
            "^\\s+" + "([!*]\\s+)?" + "(\\S[\\S\\s]+\\S)\\s\\s+" + "(?:([^\\d\\s+\\-]+)\\s*)?" +
            "([-+]?\\d[\\d,.]*)" + "(?:\\s*([^\\d\\s+\\-]+)\\s*$)?");
    private static final Pattern reEnd = Pattern.compile("\\bid=\"addmodal\"");
    private static final Pattern reDecimalPoint = Pattern.compile("\\.\\d\\d?$");
    private static final Pattern reDecimalComma = Pattern.compile(",\\d\\d?$");
    // %3A is '='
    private Pattern reAccountName = Pattern.compile("/register\\?q=inacct%3A([a-zA-Z0-9%]+)\"");
    private Pattern reAccountValue = Pattern.compile(
            "<span class=\"[^\"]*\\bamount\\b[^\"]*\">\\s*([-+]?[\\d.,]+)(?:\\s+(\\S+))?</span>");
    private MobileLedgerProfile profile;
    private int expectedPostingsCount = -1;
    public RetrieveTransactionsTask(@NonNull MobileLedgerProfile profile) {
        this.profile = profile;
    }
    private static void L(String msg) {
        //debug("transaction-parser", msg);
    }
    static LedgerTransactionAccount parseTransactionAccountLine(String line) {
        Matcher m = reTransactionDetails.matcher(line);
        if (m.find()) {
            String postingStatus = m.group(1);
            String acc_name = m.group(2);
            String currencyPre = m.group(3);
            String amount = Objects.requireNonNull(m.group(4));
            String currencyPost = m.group(5);

            String currency = null;
            if ((currencyPre != null) && (currencyPre.length() > 0)) {
                if ((currencyPost != null) && (currencyPost.length() > 0))
                    return null;
                currency = currencyPre;
            }
            else if ((currencyPost != null) && (currencyPost.length() > 0)) {
                currency = currencyPost;
            }

            amount = amount.replace(',', '.');

            return new LedgerTransactionAccount(acc_name, Float.parseFloat(amount), currency, null);
        }
        else {
            return null;
        }
    }
    @Override
    protected void onProgressUpdate(Progress... values) {
        super.onProgressUpdate(values);
        Data.backgroundTaskProgress.postValue(values[0]);
    }
    @Override
    protected void onPostExecute(String error) {
        super.onPostExecute(error);
        Progress progress = new Progress();
        progress.setState(ProgressState.FINISHED);
        progress.setError(error);
        onProgressUpdate(progress);
    }
    @Override
    protected void onCancelled() {
        super.onCancelled();
        Progress progress = new Progress();
        progress.setState(ProgressState.FINISHED);
        onProgressUpdate(progress);
    }
    private String retrieveTransactionListLegacy() throws IOException, HTTPException {
        Progress progress = Progress.indeterminate();
        progress.setState(ProgressState.RUNNING);
        progress.setTotal(expectedPostingsCount);
        int maxTransactionId = -1;
        ArrayList<LedgerAccount> list = new ArrayList<>();
        HashMap<String, LedgerAccount> map = new HashMap<>();
        ArrayList<LedgerAccount> displayed = new ArrayList<>();
        ArrayList<LedgerTransaction> transactions = new ArrayList<>();
        LedgerAccount lastAccount = null;
        ArrayList<LedgerAccount> syntheticAccounts = new ArrayList<>();

        HttpURLConnection http = NetworkUtil.prepareConnection(profile, "journal");
        http.setAllowUserInteraction(false);
        publishProgress(progress);
        if (http.getResponseCode() != 200)
            throw new HTTPException(http.getResponseCode(), http.getResponseMessage());

        try (InputStream resp = http.getInputStream()) {
            if (http.getResponseCode() != 200)
                throw new IOException(String.format("HTTP error %d", http.getResponseCode()));

            int matchedTransactionsCount = 0;

            ParserState state = ParserState.EXPECTING_ACCOUNT;
            String line;
            BufferedReader buf =
                    new BufferedReader(new InputStreamReader(resp, StandardCharsets.UTF_8));

            int processedTransactionCount = 0;
            int transactionId = 0;
            LedgerTransaction transaction = null;
            LINES:
            while ((line = buf.readLine()) != null) {
                throwIfCancelled();
                Matcher m;
                m = reComment.matcher(line);
                if (m.find()) {
                    // TODO: comments are ignored for now
//                            Log.v("transaction-parser", "Ignoring comment");
                    continue;
                }
                //L(String.format("State is %d", updating));
                switch (state) {
                    case EXPECTING_ACCOUNT:
                        if (line.equals("<h2>General Journal</h2>")) {
                            state = ParserState.EXPECTING_TRANSACTION;
                            L("→ expecting transaction");
                            continue;
                        }
                        m = reAccountName.matcher(line);
                        if (m.find()) {
                            String acct_encoded = m.group(1);
                            String accName = URLDecoder.decode(acct_encoded, "UTF-8");
                            accName = accName.replace("\"", "");
                            L(String.format("found account: %s", accName));

                            lastAccount = map.get(accName);
                            if (lastAccount != null) {
                                L(String.format("ignoring duplicate account '%s'", accName));
                                continue;
                            }
                            String parentAccountName = LedgerAccount.extractParentName(accName);
                            LedgerAccount parentAccount;
                            if (parentAccountName != null) {
                                parentAccount = ensureAccountExists(parentAccountName, map,
                                        syntheticAccounts);
                            }
                            else {
                                parentAccount = null;
                            }
                            lastAccount = new LedgerAccount(profile, accName, parentAccount);

                            list.add(lastAccount);
                            map.put(accName, lastAccount);

                            state = ParserState.EXPECTING_ACCOUNT_AMOUNT;
                            L("→ expecting account amount");
                        }
                        break;

                    case EXPECTING_ACCOUNT_AMOUNT:
                        m = reAccountValue.matcher(line);
                        boolean match_found = false;
                        while (m.find()) {
                            throwIfCancelled();

                            match_found = true;
                            String value = Objects.requireNonNull(m.group(1));
                            String currency = m.group(2);
                            if (currency == null)
                                currency = "";

                            {
                                Matcher tmpM = reDecimalComma.matcher(value);
                                if (tmpM.find()) {
                                    value = value.replace(".", "");
                                    value = value.replace(',', '.');
                                }

                                tmpM = reDecimalPoint.matcher(value);
                                if (tmpM.find()) {
                                    value = value.replace(",", "");
                                    value = value.replace(" ", "");
                                }
                            }
                            L("curr=" + currency + ", value=" + value);
                            final float val = Float.parseFloat(value);
                            lastAccount.addAmount(val, currency);
                            for (LedgerAccount syn : syntheticAccounts) {
                                L(String.format(Locale.ENGLISH, "propagating %s %1.2f to %s",
                                        currency, val, syn.getName()));
                                syn.addAmount(val, currency);
                            }
                        }

                        if (match_found) {
                            syntheticAccounts.clear();
                            state = ParserState.EXPECTING_ACCOUNT;
                            L("→ expecting account");
                        }

                        break;

                    case EXPECTING_TRANSACTION:
                        if (!line.isEmpty() && (line.charAt(0) == ' '))
                            continue;
                        m = reTransactionStart.matcher(line);
                        if (m.find()) {
                            transactionId = Integer.parseInt(Objects.requireNonNull(m.group(1)));
                            state = ParserState.EXPECTING_TRANSACTION_DESCRIPTION;
                            L(String.format(Locale.ENGLISH,
                                    "found transaction %d → expecting description", transactionId));
                            progress.setProgress(++processedTransactionCount);
                            if (maxTransactionId < transactionId)
                                maxTransactionId = transactionId;
                            if ((progress.isIndeterminate()) ||
                                (progress.getTotal() < transactionId))
                                progress.setTotal(transactionId);
                            publishProgress(progress);
                        }
                        m = reEnd.matcher(line);
                        if (m.find()) {
                            L("--- transaction value complete ---");
                            break LINES;
                        }
                        break;

                    case EXPECTING_TRANSACTION_DESCRIPTION:
                        if (!line.isEmpty() && (line.charAt(0) == ' '))
                            continue;
                        m = reTransactionDescription.matcher(line);
                        if (m.find()) {
                            if (transactionId == 0)
                                throw new TransactionParserException(
                                        "Transaction Id is 0 while expecting " + "description");

                            String date = Objects.requireNonNull(m.group(1));
                            try {
                                int equalsIndex = date.indexOf('=');
                                if (equalsIndex >= 0)
                                    date = date.substring(equalsIndex + 1);
                                transaction =
                                        new LedgerTransaction(transactionId, date, m.group(2));
                            }
                            catch (ParseException e) {
                                e.printStackTrace();
                                return String.format("Error parsing date '%s'", date);
                            }
                            state = ParserState.EXPECTING_TRANSACTION_DETAILS;
                            L(String.format(Locale.ENGLISH,
                                    "transaction %d created for %s (%s) →" + " expecting details",
                                    transactionId, date, m.group(2)));
                        }
                        break;

                    case EXPECTING_TRANSACTION_DETAILS:
                        if (line.isEmpty()) {
                            // transaction data collected

                            transaction.finishLoading();
                            transactions.add(transaction);

                            state = ParserState.EXPECTING_TRANSACTION;
                            L(String.format("transaction %s parsed → expecting transaction",
                                    transaction.getId()));

// sounds like a good idea, but transaction-1 may not be the first one chronologically
// for example, when you add the initial seeding transaction after entering some others
//                                            if (transactionId == 1) {
//                                                L("This was the initial transaction.
//                                                Terminating " +
//                                                  "parser");
//                                                break LINES;
//                                            }
                        }
                        else {
                            LedgerTransactionAccount lta = parseTransactionAccountLine(line);
                            if (lta != null) {
                                transaction.addAccount(lta);
                                L(String.format(Locale.ENGLISH, "%d: %s = %s", transaction.getId(),
                                        lta.getAccountName(), lta.getAmount()));
                            }
                            else
                                throw new IllegalStateException(
                                        String.format("Can't parse transaction %d details: %s",
                                                transactionId, line));
                        }
                        break;
                    default:
                        throw new RuntimeException(
                                String.format("Unknown parser updating %s", state.name()));
                }
            }

            throwIfCancelled();

            profile.setAndStoreAccountAndTransactionListFromWeb(list, transactions);

            return null;
        }
    }
    private @NonNull
    LedgerAccount ensureAccountExists(String accountName, HashMap<String, LedgerAccount> map,
                                      ArrayList<LedgerAccount> createdAccounts) {
        LedgerAccount acc = map.get(accountName);

        if (acc != null)
            return acc;

        String parentName = LedgerAccount.extractParentName(accountName);
        LedgerAccount parentAccount;
        if (parentName != null) {
            parentAccount = ensureAccountExists(parentName, map, createdAccounts);
        }
        else {
            parentAccount = null;
        }

        acc = new LedgerAccount(profile, accountName, parentAccount);
        createdAccounts.add(acc);
        return acc;
    }
    private boolean retrieveAccountList() throws IOException, HTTPException {
        HttpURLConnection http = NetworkUtil.prepareConnection(profile, "accounts");
        http.setAllowUserInteraction(false);
        switch (http.getResponseCode()) {
            case 200:
                break;
            case 404:
                return false;
            default:
                throw new HTTPException(http.getResponseCode(), http.getResponseMessage());
        }
        publishProgress(Progress.indeterminate());
        SQLiteDatabase db = App.getDatabase();
        ArrayList<LedgerAccount> list = new ArrayList<>();
        HashMap<String, LedgerAccount> map = new HashMap<>();
        HashMap<String, LedgerAccount> currentMap = new HashMap<>();
        for (LedgerAccount acc : Objects.requireNonNull(profile.getAllAccounts()))
            currentMap.put(acc.getName(), acc);
        try (InputStream resp = http.getInputStream()) {
            if (http.getResponseCode() != 200)
                throw new IOException(String.format("HTTP error %d", http.getResponseCode()));

            AccountListParser parser = new AccountListParser(resp);
            expectedPostingsCount = 0;

            while (true) {
                throwIfCancelled();
                ParsedLedgerAccount parsedAccount = parser.nextAccount();
                if (parsedAccount == null) {
                    break;
                }
                expectedPostingsCount += parsedAccount.getAnumpostings();
                final String accName = parsedAccount.getAname();
                LedgerAccount acc = map.get(accName);
                if (acc != null)
                    throw new RuntimeException(
                            String.format("Account '%s' already present", acc.getName()));
                String parentName = LedgerAccount.extractParentName(accName);
                ArrayList<LedgerAccount> createdParents = new ArrayList<>();
                LedgerAccount parent;
                if (parentName == null) {
                    parent = null;
                }
                else {
                    parent = ensureAccountExists(parentName, map, createdParents);
                    parent.setHasSubAccounts(true);
                }
                acc = new LedgerAccount(profile, accName, parent);
                list.add(acc);
                map.put(accName, acc);

                String lastCurrency = null;
                float lastCurrencyAmount = 0;
                for (ParsedBalance b : parsedAccount.getAibalance()) {
                    throwIfCancelled();
                    final String currency = b.getAcommodity();
                    final float amount = b.getAquantity()
                                          .asFloat();
                    if (currency.equals(lastCurrency)) {
                        lastCurrencyAmount += amount;
                    }
                    else {
                        if (lastCurrency != null) {
                            acc.addAmount(lastCurrencyAmount, lastCurrency);
                        }
                        lastCurrency = currency;
                        lastCurrencyAmount = amount;
                    }
                }
                if (lastCurrency != null) {
                    acc.addAmount(lastCurrencyAmount, lastCurrency);
                }
                for (LedgerAccount p : createdParents)
                    acc.propagateAmountsTo(p);
            }
            throwIfCancelled();
        }

        // the current account tree may have changed, update the new-to be tree to match
        for (LedgerAccount acc : list) {
            LedgerAccount prevData = currentMap.get(acc.getName());
            if (prevData != null) {
                acc.setExpanded(prevData.isExpanded());
                acc.setAmountsExpanded(prevData.amountsExpanded());
            }
        }

        profile.setAndStoreAccountListFromWeb(list);
        return true;
    }
    private boolean retrieveTransactionList() throws IOException, ParseException, HTTPException {
        Progress progress = new Progress();
        int maxTransactionId = Data.transactions.size();
        progress.setTotal(expectedPostingsCount);

        HttpURLConnection http = NetworkUtil.prepareConnection(profile, "transactions");
        http.setAllowUserInteraction(false);
        publishProgress(progress);
        switch (http.getResponseCode()) {
            case 200:
                break;
            case 404:
                return false;
            default:
                throw new HTTPException(http.getResponseCode(), http.getResponseMessage());
        }
        try (InputStream resp = http.getInputStream()) {
            throwIfCancelled();
            ArrayList<LedgerTransaction> trList = new ArrayList<>();

            TransactionListParser parser = new TransactionListParser(resp);

            int processedPostings = 0;

            while (true) {
                throwIfCancelled();
                ParsedLedgerTransaction parsedTransaction = parser.nextTransaction();
                throwIfCancelled();
                if (parsedTransaction == null)
                    break;

                LedgerTransaction transaction = parsedTransaction.asLedgerTransaction();
                trList.add(transaction);

                progress.setProgress(processedPostings += transaction.getAccounts()
                                                                     .size());
                publishProgress(progress);
            }

            throwIfCancelled();
            profile.setAndStoreTransactionList(trList);
        }

        return true;
    }

    @SuppressLint("DefaultLocale")
    @Override
    protected String doInBackground(Void... params) {
        Data.backgroundTaskStarted();
        try {
            if (!retrieveAccountList() || !retrieveTransactionList())
                return retrieveTransactionListLegacy();
            return null;
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
            return "Invalid server URL";
        }
        catch (HTTPException e) {
            e.printStackTrace();
            return String.format("HTTP error %d: %s", e.getResponseCode(), e.getResponseMessage());
        }
        catch (IOException e) {
            e.printStackTrace();
            return e.getLocalizedMessage();
        }
        catch (ParseException e) {
            e.printStackTrace();
            return "Network error";
        }
        catch (OperationCanceledException e) {
            e.printStackTrace();
            return "Operation cancelled";
        }
        finally {
            Data.backgroundTaskFinished();
        }
    }
    private void throwIfCancelled() {
        if (isCancelled())
            throw new OperationCanceledException(null);
    }
    private enum ParserState {
        EXPECTING_ACCOUNT, EXPECTING_ACCOUNT_AMOUNT, EXPECTING_TRANSACTION,
        EXPECTING_TRANSACTION_DESCRIPTION, EXPECTING_TRANSACTION_DETAILS
    }

    public enum ProgressState {STARTING, RUNNING, FINISHED}

    public static class Progress {
        private int progress;
        private int total;
        private ProgressState state = ProgressState.RUNNING;
        private String error = null;
        private boolean indeterminate;
        Progress() {
            indeterminate = true;
        }
        Progress(int progress, int total) {
            this.indeterminate = false;
            this.progress = progress;
            this.total = total;
        }
        public static Progress indeterminate() {
            return new Progress();
        }
        public static Progress finished(String error) {
            Progress p = new Progress();
            p.setState(ProgressState.FINISHED);
            p.setError(error);
            return p;
        }
        public int getProgress() {
            ensureState(ProgressState.RUNNING);
            return progress;
        }
        protected void setProgress(int progress) {
            this.progress = progress;
            this.state = ProgressState.RUNNING;
        }
        public int getTotal() {
            ensureState(ProgressState.RUNNING);
            return total;
        }
        protected void setTotal(int total) {
            this.total = total;
            state = ProgressState.RUNNING;
            indeterminate = total == -1;
        }
        private void ensureState(ProgressState wanted) {
            if (state != wanted)
                throw new IllegalStateException(
                        String.format("Bad state: %s, expected %s", state, wanted));
        }
        public ProgressState getState() {
            return state;
        }
        public void setState(ProgressState state) {
            this.state = state;
        }
        public String getError() {
            ensureState(ProgressState.FINISHED);
            return error;
        }
        public void setError(String error) {
            this.error = error;
            state = ProgressState.FINISHED;
        }
        public boolean isIndeterminate() {
            return indeterminate;
        }
        public void setIndeterminate(boolean indeterminate) {
            this.indeterminate = indeterminate;
        }
    }

    private static class TransactionParserException extends IllegalStateException {
        TransactionParserException(String message) {
            super(message);
        }
    }
}
