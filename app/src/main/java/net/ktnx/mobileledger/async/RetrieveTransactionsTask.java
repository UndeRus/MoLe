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
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.model.LedgerTransactionAccount;
import net.ktnx.mobileledger.ui.transaction_list.TransactionListFragment;
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
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RetrieveTransactionsTask extends
        AsyncTask<RetrieveTransactionsTask.Params, RetrieveTransactionsTask.Progress, Void> {
    private static final Pattern transactionStartPattern = Pattern.compile("<tr class=\"title\" " +
                                                                           "id=\"transaction-(\\d+)\"><td class=\"date\"[^\\\"]*>([\\d.-]+)</td>");
    private static final Pattern transactionDescriptionPattern =
            Pattern.compile("<tr class=\"posting\" title=\"(\\S+)\\s(.+)");
    private static final Pattern transactionDetailsPattern =
            Pattern.compile("^\\s+" + "(\\S[\\S\\s]+\\S)\\s\\s+([-+]?\\d[\\d,.]*)(?:\\s+(\\S+)$)?");
    private static final Pattern endPattern = Pattern.compile("\\bid=\"addmodal\"");
    protected WeakReference<TransactionListFragment> contextRef;
    protected int error;
    private boolean success;
    public RetrieveTransactionsTask(WeakReference<TransactionListFragment> contextRef) {
        this.contextRef = contextRef;
    }
    private static final void L(String msg) {
//        Log.d("transaction-parser", msg);
    }
    @Override
    protected void onProgressUpdate(Progress... values) {
        super.onProgressUpdate(values);
        TransactionListFragment context = getContext();
        if (context == null) return;
        context.onRetrieveProgress(values[0]);
    }
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        TransactionListFragment context = getContext();
        if (context == null) return;
        context.onRetrieveStart();
    }
    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        TransactionListFragment context = getContext();
        if (context == null) return;
        context.onRetrieveDone(success);
    }
    @Override
    protected void onCancelled() {
        super.onCancelled();
        TransactionListFragment context = getContext();
        if (context == null) return;
        context.onRetrieveDone(false);
    }
    @SuppressLint("DefaultLocale")
    @Override
    protected Void doInBackground(Params... params) {
        Progress progress = new Progress();
        int maxTransactionId = Progress.INDETERMINATE;
        success = false;
        try {
            HttpURLConnection http =
                    NetworkUtil.prepare_connection(params[0].getBackendPref(), "journal");
            http.setAllowUserInteraction(false);
            publishProgress(progress);
            TransactionListFragment ctx = getContext();
            if (ctx == null) return null;
            try (SQLiteDatabase db = MLDB.getWritableDatabase()) {
                try (InputStream resp = http.getInputStream()) {
                    if (http.getResponseCode() != 200) throw new IOException(
                            String.format("HTTP error %d", http.getResponseCode()));
                    db.beginTransaction();
                    try {
                        db.execSQL("UPDATE transactions set keep=0");

                        ParserState state = ParserState.EXPECTING_JOURNAL;
                        String line;
                        BufferedReader buf =
                                new BufferedReader(new InputStreamReader(resp, "UTF-8"));

                        int processedTransactionCount = 0;
                        int transactionId = 0;
                        int matchedTransactionsCount = 0;
                        LedgerTransaction transaction = null;
                        LINES:
                        while ((line = buf.readLine()) != null) {
                            if (isCancelled()) break;
                            Matcher m;
                            //L(String.format("State is %d", state));
                            switch (state) {
                                case EXPECTING_JOURNAL:
                                    if (!line.isEmpty() && (line.charAt(0) == ' ')) continue;
                                    if (line.equals("<h2>General Journal</h2>")) {
                                        state = ParserState.EXPECTING_TRANSACTION;
                                        L("→ expecting transaction");
                                    }
                                    break;
                                case EXPECTING_TRANSACTION:
                                    if (!line.isEmpty() && (line.charAt(0) == ' ')) continue;
                                    m = transactionStartPattern.matcher(line);
                                    if (m.find()) {
                                        transactionId = Integer.valueOf(m.group(1));
                                        state = ParserState.EXPECTING_TRANSACTION_DESCRIPTION;
                                        L(String.format(
                                                "found transaction %d → expecting " + "description",
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
                                        L("--- transaction list complete ---");
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
                                            db.execSQL("UPDATE transactions SET keep = 1 WHERE id" +
                                                       "=?", new Integer[]{transaction.getId()});
                                            matchedTransactionsCount++;

                                            if (matchedTransactionsCount == 100) {
                                                db.execSQL("UPDATE transactions SET keep=1 WHERE " +
                                                           "id < ?",
                                                        new Integer[]{transaction.getId()});
                                                success = true;
                                                progress.setTotal(progress.getProgress());
                                                publishProgress(progress);
                                                break LINES;
                                            }
                                        }
                                        else {
                                            db.execSQL("DELETE from transactions WHERE id=?",
                                                    new Integer[]{transaction.getId()});
                                            db.execSQL("DELETE from transaction_accounts WHERE " +
                                                       "transaction_id=?",
                                                    new Integer[]{transaction.getId()});
                                            transaction.insertInto(db);
                                            matchedTransactionsCount = 0;
                                            progress.setTotal(maxTransactionId);
                                        }

                                        state = ParserState.EXPECTING_TRANSACTION;
                                        L(String.format(
                                                "transaction %s saved → expecting " + "transaction",
                                                transaction.getId()));

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
                                            amount = amount.replace(',', '.');
                                            transaction.addAccount(
                                                    new LedgerTransactionAccount(acc_name,
                                                            Float.valueOf(amount), currency));
                                            L(String.format("%s = %s", acc_name, amount));
                                        }
                                        else throw new IllegalStateException(
                                                String.format("Can't parse transaction %d details",
                                                        transactionId));
                                    }
                                    break;
                                default:
                                    throw new RuntimeException(
                                            String.format("Unknown parser state %s", state.name()));
                            }
                        }
                        if (!isCancelled()) {
                            db.execSQL("DELETE FROM transactions WHERE keep = 0");
                            db.setTransactionSuccessful();
                        }
                    }
                    finally {
                        db.endTransaction();
                    }
                }
            }

            if (success && !isCancelled()) {
                Log.d("db", "Updating transaction list stamp");
                MLDB.set_option_value(MLDB.OPT_TRANSACTION_LIST_STAMP, new Date().getTime());
                ctx.model.reloadTransactions(ctx);
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
        return null;
    }
    TransactionListFragment getContext() {
        return contextRef.get();
    }

    private enum ParserState {
        EXPECTING_JOURNAL, EXPECTING_TRANSACTION, EXPECTING_TRANSACTION_DESCRIPTION,
        EXPECTING_TRANSACTION_DETAILS
    }

    public static class Params {
        private SharedPreferences backendPref;

        public Params(SharedPreferences backendPref) {
            this.backendPref = backendPref;
        }
        SharedPreferences getBackendPref() {
            return backendPref;
        }
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
