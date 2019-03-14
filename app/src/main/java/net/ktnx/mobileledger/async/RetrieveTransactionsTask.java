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

import android.annotation.SuppressLint;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.OperationCanceledException;
import android.util.Log;

import net.ktnx.mobileledger.err.HTTPException;
import net.ktnx.mobileledger.json.AccountListParser;
import net.ktnx.mobileledger.json.ParsedBalance;
import net.ktnx.mobileledger.json.ParsedLedgerAccount;
import net.ktnx.mobileledger.json.ParsedLedgerTransaction;
import net.ktnx.mobileledger.json.TransactionListParser;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerAccount;
import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.model.LedgerTransactionAccount;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
import net.ktnx.mobileledger.ui.activity.MainActivity;
import net.ktnx.mobileledger.utils.MLDB;
import net.ktnx.mobileledger.utils.NetworkUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RetrieveTransactionsTask
        extends AsyncTask<Void, RetrieveTransactionsTask.Progress, String> {
    private static final int MATCHING_TRANSACTIONS_LIMIT = 50;
    private static final Pattern reComment = Pattern.compile("^\\s*;");
    private static final Pattern reTransactionStart = Pattern.compile("<tr class=\"title\" " +
                                                                      "id=\"transaction-(\\d+)\"><td class=\"date\"[^\"]*>([\\d.-]+)</td>");
    private static final Pattern reTransactionDescription =
            Pattern.compile("<tr class=\"posting\" title=\"(\\S+)\\s(.+)");
    private static final Pattern reTransactionDetails =
            Pattern.compile("^\\s+(\\S[\\S\\s]+\\S)\\s\\s+([-+]?\\d[\\d,.]*)(?:\\s+(\\S+)$)?");
    private static final Pattern reEnd = Pattern.compile("\\bid=\"addmodal\"");
    private WeakReference<MainActivity> contextRef;
    private int error;
    // %3A is '='
    private Pattern reAccountName = Pattern.compile("/register\\?q=inacct%3A([a-zA-Z0-9%]+)\"");
    private Pattern reAccountValue = Pattern.compile(
            "<span class=\"[^\"]*\\bamount\\b[^\"]*\">\\s*([-+]?[\\d.,]+)(?:\\s+(\\S+))?</span>");
    public RetrieveTransactionsTask(WeakReference<MainActivity> contextRef) {
        this.contextRef = contextRef;
    }
    private static void L(String msg) {
        //Log.d("transaction-parser", msg);
    }
    @Override
    protected void onProgressUpdate(Progress... values) {
        super.onProgressUpdate(values);
        MainActivity context = getContext();
        if (context == null) return;
        context.onRetrieveProgress(values[0]);
    }
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        MainActivity context = getContext();
        if (context == null) return;
        context.onRetrieveStart();
    }
    @Override
    protected void onPostExecute(String error) {
        super.onPostExecute(error);
        MainActivity context = getContext();
        if (context == null) return;
        context.onRetrieveDone(error);
    }
    @Override
    protected void onCancelled() {
        super.onCancelled();
        MainActivity context = getContext();
        if (context == null) return;
        context.onRetrieveDone(null);
    }
    private String retrieveTransactionListLegacy(MobileLedgerProfile profile)
            throws IOException, ParseException, HTTPException {
        Progress progress = new Progress();
        int maxTransactionId = Progress.INDETERMINATE;
        ArrayList<LedgerAccount> accountList = new ArrayList<>();
        HashMap<String, Void> accountNames = new HashMap<>();
        LedgerAccount lastAccount = null;
        boolean onlyStarred = Data.optShowOnlyStarred.get();

        HttpURLConnection http = NetworkUtil.prepareConnection(profile, "journal");
        http.setAllowUserInteraction(false);
        publishProgress(progress);
        switch (http.getResponseCode()) {
            case 200:
                break;
            default:
                throw new HTTPException(http.getResponseCode(), http.getResponseMessage());
        }
        try (SQLiteDatabase db = MLDB.getWritableDatabase()) {
            try (InputStream resp = http.getInputStream()) {
                if (http.getResponseCode() != 200)
                    throw new IOException(String.format("HTTP error %d", http.getResponseCode()));
                db.beginTransaction();
                try {
                    prepareDbForRetrieval(db, profile);

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
                            Log.v("transaction-parser", "Ignoring comment");
                            continue;
                        }
                        //L(String.format("State is %d", updating));
                        switch (state) {
                            case EXPECTING_ACCOUNT:
                                if (line.equals("<h2>General Journal</h2>")) {
                                    state = ParserState.EXPECTING_TRANSACTION;
                                    L("→ expecting transaction");
                                    Data.accounts.set(accountList);
                                    continue;
                                }
                                m = reAccountName.matcher(line);
                                if (m.find()) {
                                    String acct_encoded = m.group(1);
                                    String acct_name = URLDecoder.decode(acct_encoded, "UTF-8");
                                    acct_name = acct_name.replace("\"", "");
                                    L(String.format("found account: %s", acct_name));

                                    lastAccount = profile.tryLoadAccount(db, acct_name);
                                    if (lastAccount == null)
                                        lastAccount = new LedgerAccount(acct_name);
                                    else lastAccount.removeAmounts();
                                    profile.storeAccount(db, lastAccount);

                                    // make sure the parent account(s) are present,
                                    // synthesising them if necessary
                                    String parentName = lastAccount.getParentName();
                                    if (parentName != null) {
                                        Stack<String> toAppend = new Stack<>();
                                        while (parentName != null) {
                                            if (accountNames.containsKey(parentName)) break;
                                            toAppend.push(parentName);
                                            parentName =
                                                    new LedgerAccount(parentName).getParentName();
                                        }
                                        while (!toAppend.isEmpty()) {
                                            String aName = toAppend.pop();
                                            LedgerAccount acc = new LedgerAccount(aName);
                                            acc.setHiddenByStar(lastAccount.isHiddenByStar());
                                            if ((!onlyStarred || !acc.isHiddenByStar()) &&
                                                acc.isVisible(accountList)) accountList.add(acc);
                                            L(String.format("gap-filling with %s", aName));
                                            accountNames.put(aName, null);
                                            profile.storeAccount(db, acc);
                                        }
                                    }

                                    if ((!onlyStarred || !lastAccount.isHiddenByStar()) &&
                                        lastAccount.isVisible(accountList))
                                        accountList.add(lastAccount);
                                    accountNames.put(acct_name, null);

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
                                    String value = m.group(1);
                                    String currency = m.group(2);
                                    if (currency == null) currency = "";
                                    value = value.replace(',', '.');
                                    L("curr=" + currency + ", value=" + value);
                                    profile.storeAccountValue(db, lastAccount.getName(), currency,
                                            Float.valueOf(value));
                                    lastAccount.addAmount(Float.parseFloat(value), currency);
                                }

                                if (match_found) {
                                    state = ParserState.EXPECTING_ACCOUNT;
                                    L("→ expecting account");
                                }

                                break;

                            case EXPECTING_TRANSACTION:
                                if (!line.isEmpty() && (line.charAt(0) == ' ')) continue;
                                m = reTransactionStart.matcher(line);
                                if (m.find()) {
                                    transactionId = Integer.valueOf(m.group(1));
                                    state = ParserState.EXPECTING_TRANSACTION_DESCRIPTION;
                                    L(String.format("found transaction %d → expecting description",
                                            transactionId));
                                    progress.setProgress(++processedTransactionCount);
                                    if (maxTransactionId < transactionId)
                                        maxTransactionId = transactionId;
                                    if ((progress.getTotal() == Progress.INDETERMINATE) ||
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
                                if (!line.isEmpty() && (line.charAt(0) == ' ')) continue;
                                m = reTransactionDescription.matcher(line);
                                if (m.find()) {
                                    if (transactionId == 0) throw new TransactionParserException(
                                            "Transaction Id is 0 while expecting " + "description");

                                    String date = m.group(1);
                                    try {
                                        int equalsIndex = date.indexOf('=');
                                        if (equalsIndex >= 0)
                                            date = date.substring(equalsIndex + 1);
                                        transaction = new LedgerTransaction(transactionId, date,
                                                m.group(2));
                                    }
                                    catch (ParseException e) {
                                        e.printStackTrace();
                                        return String.format("Error parsing date '%s'", date);
                                    }
                                    state = ParserState.EXPECTING_TRANSACTION_DETAILS;
                                    L(String.format("transaction %d created for %s (%s) →" +
                                                    " expecting details", transactionId, date,
                                            m.group(2)));
                                }
                                break;

                            case EXPECTING_TRANSACTION_DETAILS:
                                if (line.isEmpty()) {
                                    // transaction data collected
                                    if (transaction.existsInDb(db)) {
                                        profile.markTransactionAsPresent(db, transaction);
                                        matchedTransactionsCount++;

                                        if (matchedTransactionsCount ==
                                            MATCHING_TRANSACTIONS_LIMIT)
                                        {
                                            profile.markTransactionsBeforeTransactionAsPresent(db,
                                                    transaction);
                                            progress.setTotal(progress.getProgress());
                                            publishProgress(progress);
                                            break LINES;
                                        }
                                    }
                                    else {
                                        profile.storeTransaction(db, transaction);
                                        matchedTransactionsCount = 0;
                                        progress.setTotal(maxTransactionId);
                                    }

                                    state = ParserState.EXPECTING_TRANSACTION;
                                    L(String.format("transaction %s saved → expecting transaction",
                                            transaction.getId()));
                                    transaction.finishLoading();

// sounds like a good idea, but transaction-1 may not be the first one chronologically
// for example, when you add the initial seeding transaction after entering some others
//                                            if (transactionId == 1) {
//                                                L("This was the initial transaction. Terminating " +
//                                                  "parser");
//                                                break LINES;
//                                            }
                                }
                                else {
                                    m = reTransactionDetails.matcher(line);
                                    if (m.find()) {
                                        String acc_name = m.group(1);
                                        String amount = m.group(2);
                                        String currency = m.group(3);
                                        if (currency == null) currency = "";
                                        amount = amount.replace(',', '.');
                                        transaction.addAccount(
                                                new LedgerTransactionAccount(acc_name,
                                                        Float.valueOf(amount), currency));
                                        L(String.format("%d: %s = %s", transaction.getId(),
                                                acc_name, amount));
                                    }
                                    else throw new IllegalStateException(String.format(
                                            "Can't parse transaction %d " + "details: %s",
                                            transactionId, line));
                                }
                                break;
                            default:
                                throw new RuntimeException(
                                        String.format("Unknown parser updating %s", state.name()));
                        }
                    }

                    throwIfCancelled();

                    profile.deleteNotPresentTransactions(db);
                    db.setTransactionSuccessful();

                    profile.setLastUpdateStamp();

                    return null;
                }
                finally {
                    db.endTransaction();
                }
            }
        }
    }
    private void prepareDbForRetrieval(SQLiteDatabase db, MobileLedgerProfile profile) {
        db.execSQL("UPDATE transactions set keep=0 where profile=?",
                new String[]{profile.getUuid()});
        db.execSQL("update account_values set keep=0 where profile=?;",
                new String[]{profile.getUuid()});
        db.execSQL("update accounts set keep=0 where profile=?;", new String[]{profile.getUuid()});
    }
    private boolean retrieveAccountList(MobileLedgerProfile profile)
            throws IOException, HTTPException {
        Progress progress = new Progress();

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
        publishProgress(progress);
        try (SQLiteDatabase db = MLDB.getWritableDatabase()) {
            try (InputStream resp = http.getInputStream()) {
                if (http.getResponseCode() != 200)
                    throw new IOException(String.format("HTTP error %d", http.getResponseCode()));

                db.beginTransaction();
                try {
                    profile.markAccountsAsNotPresent(db);

                    AccountListParser parser = new AccountListParser(resp);
                    ArrayList<LedgerAccount> accountList = new ArrayList<>();

                    while (true) {
                        throwIfCancelled();
                        ParsedLedgerAccount parsedAccount = parser.nextAccount();
                        if (parsedAccount == null) break;

                        LedgerAccount acc = profile.tryLoadAccount(db, parsedAccount.getAname());
                        if (acc == null) acc = new LedgerAccount(parsedAccount.getAname());
                        else acc.removeAmounts();

                        profile.storeAccount(db, acc);
                        for (ParsedBalance b : parsedAccount.getAebalance()) {
                            profile.storeAccountValue(db, acc.getName(), b.getAcommodity(),
                                    b.getAquantity().asFloat());
                        }

                        if (acc.isVisible(accountList)) accountList.add(acc);
                    }
                    throwIfCancelled();

                    profile.deleteNotPresentAccounts(db);
                    throwIfCancelled();
                    db.setTransactionSuccessful();
                    Data.accounts.set(accountList);
                }
                finally {
                    db.endTransaction();
                }
            }
        }

        return true;
    }
    private boolean retrieveTransactionList(MobileLedgerProfile profile)
            throws IOException, ParseException, HTTPException {
        Progress progress = new Progress();
        int maxTransactionId = Progress.INDETERMINATE;

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
        try (SQLiteDatabase db = MLDB.getWritableDatabase()) {
            try (InputStream resp = http.getInputStream()) {
                if (http.getResponseCode() != 200)
                    throw new IOException(String.format("HTTP error %d", http.getResponseCode()));
                throwIfCancelled();
                db.beginTransaction();
                try {
                    profile.markTransactionsAsNotPresent(db);

                    int matchedTransactionsCount = 0;
                    TransactionListParser parser = new TransactionListParser(resp);

                    int processedTransactionCount = 0;

                    while (true) {
                        throwIfCancelled();
                        ParsedLedgerTransaction parsedTransaction = parser.nextTransaction();
                        throwIfCancelled();
                        if (parsedTransaction == null) break;
                        LedgerTransaction transaction = parsedTransaction.asLedgerTransaction();
                        if (transaction.existsInDb(db)) {
                            profile.markTransactionAsPresent(db, transaction);
                            matchedTransactionsCount++;

                            if (matchedTransactionsCount == MATCHING_TRANSACTIONS_LIMIT) {
                                profile.markTransactionsBeforeTransactionAsPresent(db, transaction);
                                progress.setTotal(progress.getProgress());
                                publishProgress(progress);
                                db.setTransactionSuccessful();
                                profile.setLastUpdateStamp();
                                return true;
                            }
                        }
                        else {
                            profile.storeTransaction(db, transaction);
                            matchedTransactionsCount = 0;
                            progress.setTotal(maxTransactionId);
                        }

                        if ((progress.getTotal() == Progress.INDETERMINATE) ||
                            (progress.getTotal() < transaction.getId()))
                            progress.setTotal(transaction.getId());

                        progress.setProgress(++processedTransactionCount);
                        publishProgress(progress);
                    }

                    throwIfCancelled();
                    profile.deleteNotPresentTransactions(db);
                    throwIfCancelled();
                    db.setTransactionSuccessful();
                    profile.setLastUpdateStamp();
                }
                finally {
                    db.endTransaction();
                }
            }
        }

        return true;
    }
    @SuppressLint("DefaultLocale")
    @Override
    protected String doInBackground(Void... params) {
        MobileLedgerProfile profile = Data.profile.get();
        Data.backgroundTaskCount.incrementAndGet();
        try {
            if (!retrieveAccountList(profile) || !retrieveTransactionList(profile))
                return retrieveTransactionListLegacy(profile);
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
            Data.backgroundTaskCount.decrementAndGet();
        }
    }
    private MainActivity getContext() {
        return contextRef.get();
    }
    private void throwIfCancelled() {
        if (isCancelled()) throw new OperationCanceledException(null);
    }

    private enum ParserState {
        EXPECTING_ACCOUNT, EXPECTING_ACCOUNT_AMOUNT, EXPECTING_JOURNAL, EXPECTING_TRANSACTION,
        EXPECTING_TRANSACTION_DESCRIPTION, EXPECTING_TRANSACTION_DETAILS
    }

    public class Progress {
        public static final int INDETERMINATE = -1;
        private int progress;
        private int total;
        Progress() {
            this(INDETERMINATE, INDETERMINATE);
        }
        Progress(int progress, int total) {
            this.progress = progress;
            this.total = total;
        }
        public int getProgress() {
            return progress;
        }
        protected void setProgress(int progress) {
            this.progress = progress;
        }
        public int getTotal() {
            return total;
        }
        protected void setTotal(int total) {
            this.total = total;
        }
    }

    private class TransactionParserException extends IllegalStateException {
        TransactionParserException(String message) {
            super(message);
        }
    }
}
