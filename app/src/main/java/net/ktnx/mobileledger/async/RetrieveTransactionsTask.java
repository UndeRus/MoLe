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

import android.annotation.SuppressLint;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.OperationCanceledException;
import android.util.Log;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerAccount;
import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.model.LedgerTransactionAccount;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RetrieveTransactionsTask
        extends AsyncTask<Void, RetrieveTransactionsTask.Progress, Void> {
    public static final int MATCHING_TRANSACTIONS_LIMIT = 50;
    public static final Pattern commentPattern = Pattern.compile("^\\s*;");
    private static final Pattern transactionStartPattern = Pattern.compile("<tr class=\"title\" " +
                                                                           "id=\"transaction-(\\d+)\"><td class=\"date\"[^\"]*>([\\d.-]+)</td>");
    private static final Pattern transactionDescriptionPattern =
            Pattern.compile("<tr class=\"posting\" title=\"(\\S+)\\s(.+)");
    private static final Pattern transactionDetailsPattern =
            Pattern.compile("^\\s+(\\S[\\S\\s]+\\S)\\s\\s+([-+]?\\d[\\d,.]*)(?:\\s+(\\S+)$)?");
    private static final Pattern endPattern = Pattern.compile("\\bid=\"addmodal\"");
    protected WeakReference<MainActivity> contextRef;
    protected int error;
    Pattern account_name_re = Pattern.compile("/register\\?q=inacct%3A([a-zA-Z0-9%]+)\"");
    Pattern account_value_re = Pattern.compile(
            "<span class=\"[^\"]*\\bamount\\b[^\"]*\">\\s*([-+]?[\\d.,]+)(?:\\s+(\\S+))?</span>");
    Pattern tr_end_re = Pattern.compile("</tr>");
    Pattern descriptions_line_re = Pattern.compile("\\bdescriptionsSuggester\\s*=\\s*new\\b");
    Pattern description_items_re = Pattern.compile("\"value\":\"([^\"]+)\"");
    // %3A is '='
    private boolean success;
    public RetrieveTransactionsTask(WeakReference<MainActivity> contextRef) {
        this.contextRef = contextRef;
    }
    private static final void L(String msg) {
        Log.d("transaction-parser", msg);
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
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        MainActivity context = getContext();
        if (context == null) return;
        context.onRetrieveDone(success);
    }
    @Override
    protected void onCancelled() {
        super.onCancelled();
        MainActivity context = getContext();
        if (context == null) return;
        context.onRetrieveDone(false);
    }
    @SuppressLint("DefaultLocale")
    @Override
    protected Void doInBackground(Void... params) {
        MobileLedgerProfile profile = Data.profile.get();
        Progress progress = new Progress();
        int maxTransactionId = Progress.INDETERMINATE;
        success = false;
        ArrayList<LedgerAccount> accountList = new ArrayList<>();
        ArrayList<LedgerTransaction> transactionList = new ArrayList<>();
        HashMap<String, Void> accountNames = new HashMap<>();
        LedgerAccount lastAccount = null;
        Data.backgroundTaskCount.incrementAndGet();
        try {
            HttpURLConnection http = NetworkUtil.prepare_connection("journal");
            http.setAllowUserInteraction(false);
            publishProgress(progress);
            MainActivity ctx = getContext();
            if (ctx == null) return null;
            try (SQLiteDatabase db = MLDB.getWritableDatabase()) {
                try (InputStream resp = http.getInputStream()) {
                    if (http.getResponseCode() != 200) throw new IOException(
                            String.format("HTTP error %d", http.getResponseCode()));
                    db.beginTransaction();
                    try {
                        String ledgerTitle = null;

                        db.execSQL("UPDATE transactions set keep=0");
                        db.execSQL("update account_values set keep=0;");
                        db.execSQL("update accounts set keep=0;");

                        ParserState state = ParserState.EXPECTING_ACCOUNT;
                        String line;
                        BufferedReader buf =
                                new BufferedReader(new InputStreamReader(resp, "UTF-8"));

                        int processedTransactionCount = 0;
                        int transactionId = 0;
                        int matchedTransactionsCount = 0;
                        LedgerTransaction transaction = null;
                        LINES:
                        while ((line = buf.readLine()) != null) {
                            throwIfCancelled();
                            Matcher m;
                            m = commentPattern.matcher(line);
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
                                    m = account_name_re.matcher(line);
                                    if (m.find()) {
                                        String acct_encoded = m.group(1);
                                        String acct_name = URLDecoder.decode(acct_encoded, "UTF-8");
                                        acct_name = acct_name.replace("\"", "");
                                        L(String.format("found account: %s", acct_name));

                                        profile.storeAccount(acct_name);
                                        lastAccount = new LedgerAccount(acct_name);

                                        // make sure the parent account(s) are present,
                                        // synthesising them if necessary
                                        String parentName = lastAccount.getParentName();
                                        if (parentName != null) {
                                            Stack<String> toAppend = new Stack<>();
                                            while (parentName != null) {
                                                if (accountNames.containsKey(parentName)) break;
                                                toAppend.push(parentName);
                                                parentName = new LedgerAccount(parentName)
                                                        .getParentName();
                                            }
                                            while (!toAppend.isEmpty()) {
                                                String aName = toAppend.pop();
                                                LedgerAccount acc = new LedgerAccount(aName);
                                                accountList.add(acc);
                                                L(String.format("gap-filling with %s", aName));
                                                accountNames.put(aName, null);
                                                profile.storeAccount(aName);
                                            }
                                        }

                                        accountList.add(lastAccount);
                                        accountNames.put(acct_name, null);

                                        state = ParserState.EXPECTING_ACCOUNT_AMOUNT;
                                        L("→ expecting account amount");
                                    }
                                    break;

                                case EXPECTING_ACCOUNT_AMOUNT:
                                    m = account_value_re.matcher(line);
                                    boolean match_found = false;
                                    while (m.find()) {
                                        throwIfCancelled();

                                        match_found = true;
                                        String value = m.group(1);
                                        String currency = m.group(2);
                                        if (currency == null) currency = "";
                                        value = value.replace(',', '.');
                                        L("curr=" + currency + ", value=" + value);
                                        profile.storeAccountValue(lastAccount.getName(), currency,
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
                                    m = transactionStartPattern.matcher(line);
                                    if (m.find()) {
                                        transactionId = Integer.valueOf(m.group(1));
                                        state = ParserState.EXPECTING_TRANSACTION_DESCRIPTION;
                                        L(String.format(
                                                "found transaction %d → expecting description",
                                                transactionId));
                                        progress.setProgress(++processedTransactionCount);
                                        if (maxTransactionId < transactionId)
                                            maxTransactionId = transactionId;
                                        if ((progress.getTotal() == Progress.INDETERMINATE) ||
                                            (progress.getTotal() < transactionId))
                                            progress.setTotal(transactionId);
                                        publishProgress(progress);
                                    }
                                    m = endPattern.matcher(line);
                                    if (m.find()) {
                                        L("--- transaction value complete ---");
                                        success = true;
                                        break LINES;
                                    }
                                    break;

                                case EXPECTING_TRANSACTION_DESCRIPTION:
                                    if (!line.isEmpty() && (line.charAt(0) == ' ')) continue;
                                    m = transactionDescriptionPattern.matcher(line);
                                    if (m.find()) {
                                        if (transactionId == 0)
                                            throw new TransactionParserException(
                                                    "Transaction Id is 0 while expecting " +
                                                    "description");

                                        transaction =
                                                new LedgerTransaction(transactionId, m.group(1),
                                                        m.group(2));
                                        state = ParserState.EXPECTING_TRANSACTION_DETAILS;
                                        L(String.format("transaction %d created for %s (%s) →" +
                                                        " expecting details", transactionId,
                                                m.group(1), m.group(2)));
                                    }
                                    break;

                                case EXPECTING_TRANSACTION_DETAILS:
                                    if (line.isEmpty()) {
                                        // transaction data collected
                                        if (transaction.existsInDb(db)) {
                                            db.execSQL("UPDATE transactions SET keep = 1 WHERE " +
                                                       "profile = ? and id=?",
                                                    new Object[]{profile.getUuid(),
                                                                 transaction.getId()
                                                    });
                                            matchedTransactionsCount++;

                                            if (matchedTransactionsCount ==
                                                MATCHING_TRANSACTIONS_LIMIT)
                                            {
                                                db.execSQL("UPDATE transactions SET keep=1 WHERE " +
                                                           "profile = ? and id < ?",
                                                        new Object[]{profile.getUuid(),
                                                                     transaction.getId()
                                                        });
                                                success = true;
                                                progress.setTotal(progress.getProgress());
                                                publishProgress(progress);
                                                break LINES;
                                            }
                                        }
                                        else {
                                            profile.storeTransaction(transaction);
                                            matchedTransactionsCount = 0;
                                            progress.setTotal(maxTransactionId);
                                        }

                                        state = ParserState.EXPECTING_TRANSACTION;
                                        L(String.format(
                                                "transaction %s saved → expecting transaction",
                                                transaction.getId()));
                                        transaction.finishLoading();
                                        transactionList.add(transaction);

// sounds like a good idea, but transaction-1 may not be the first one chronologically
// for example, when you add the initial seeding transaction after entering some others
//                                            if (transactionId == 1) {
//                                                L("This was the initial transaction. Terminating " +
//                                                  "parser");
//                                                break LINES;
//                                            }
                                    }
                                    else {
                                        m = transactionDetailsPattern.matcher(line);
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
                                            String.format("Unknown parser updating %s",
                                                    state.name()));
                            }
                        }

                        throwIfCancelled();

                        db.execSQL("DELETE FROM transactions WHERE profile=? AND keep = 0",
                                new String[]{profile.getUuid()});
                        db.setTransactionSuccessful();

                        Log.d("db", "Updating transaction value stamp");
                        Date now = new Date();
                        profile.set_option_value(MLDB.OPT_LAST_SCRAPE, now.getTime());
                        Data.lastUpdateDate.set(now);
                        Data.transactions.set(transactionList);
                    }
                    finally {
                        db.endTransaction();
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
        catch (OperationCanceledException e) {
            error = R.string.err_cancelled;
            e.printStackTrace();
        }
        finally {
            Data.backgroundTaskCount.decrementAndGet();
        }
        return null;
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
