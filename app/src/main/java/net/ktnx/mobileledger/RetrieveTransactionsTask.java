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

package net.ktnx.mobileledger;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Params {
    static final int DEFAULT_LIMIT = 100;
    private SharedPreferences backendPref;
    private String accountsRoot;
    private int limit;

    Params(SharedPreferences backendPref) {
        this.backendPref = backendPref;
        this.accountsRoot = null;
        this.limit = DEFAULT_LIMIT;
    }
    Params(SharedPreferences backendPref, String accountsRoot) {
        this(backendPref, accountsRoot, DEFAULT_LIMIT);
    }
    Params(SharedPreferences backendPref, String accountsRoot, int limit) {
        this.backendPref = backendPref;
        this.accountsRoot = accountsRoot;
        this.limit = limit;
    }
    String getAccountsRoot() {
        return accountsRoot;
    }
    SharedPreferences getBackendPref() {
        return backendPref;
    }
    int getLimit() {
        return limit;
    }
}

class RetrieveTransactionsTask extends AsyncTask<Params, Integer, Void> {
    private static final Pattern transactionStartPattern = Pattern.compile("<tr class=\"title\" "
            + "id=\"transaction-(\\d+)\"><td class=\"date\"[^\\\"]*>([\\d.-]+)</td>");
    private static final Pattern transactionDescriptionPattern =
            Pattern.compile("<tr class=\"posting\" title=\"(\\S+)\\s(.+)");
    private static final Pattern transactionDetailsPattern =
            Pattern.compile("^\\s+" + "(\\S[\\S\\s]+\\S)\\s\\s+([-+]?\\d[\\d,.]*)");
    protected WeakReference<Context> contextRef;
    protected int error;
    @Override
    protected Void doInBackground(Params... params) {
        try {
            HttpURLConnection http =
                    NetworkUtil.prepare_connection(params[0].getBackendPref(), "journal");
            http.setAllowUserInteraction(false);
            publishProgress(0);
            Context ctx = contextRef.get();
            if (ctx == null) return null;
            try (MobileLedgerDatabase dbh = new MobileLedgerDatabase(ctx)) {
                try (SQLiteDatabase db = dbh.getWritableDatabase()) {
                    try (InputStream resp = http.getInputStream()) {
                        if (http.getResponseCode() != 200) throw new IOException(
                                String.format("HTTP error %d", http.getResponseCode()));
                        db.beginTransaction();
                        try {
                            String root = params[0].getAccountsRoot();
                            if (root == null) db.execSQL("DELETE FROM transaction_history;");
                            else {
                                StringBuilder sql = new StringBuilder();
                                sql.append("DELETE FROM transaction_history ");
                                sql.append(
                                        "where id in (select transactions.id from transactions ");
                                sql.append("join transaction_accounts ");
                                sql.append(
                                        "on transactions.id=transaction_accounts.transaction_id ");
                                sql.append("where transaction_accounts.account_name like ?||'%'");
                                db.execSQL(sql.toString(), new String[]{root});
                            }

                            int state = ParserState.EXPECTING_JOURNAL;
                            String line;
                            BufferedReader buf =
                                    new BufferedReader(new InputStreamReader(resp, "UTF-8"));

                            int transactionCount = 0;
                            String transactionId = null;
                            LedgerTransaction transaction = null;
                            while ((line = buf.readLine()) != null) {
                                switch (state) {
                                    case ParserState.EXPECTING_JOURNAL: {
                                        if (line.equals("<h2>General Journal</h2>"))
                                            state = ParserState.EXPECTING_TRANSACTION;
                                        continue;
                                    }
                                    case ParserState.EXPECTING_TRANSACTION: {
                                        Matcher m = transactionStartPattern.matcher(line);
                                        if (m.find()) {
                                            transactionId = m.group(1);
                                            state = ParserState.EXPECTING_TRANSACTION_DESCRIPTION;
                                        }
                                    }
                                    case ParserState.EXPECTING_TRANSACTION_DESCRIPTION: {
                                        Matcher m = transactionDescriptionPattern.matcher(line);
                                        if (m.find()) {
                                            if (transactionId == null)
                                                throw new TransactionParserException(
                                                        "Transaction Id is null while expecting description");

                                            transaction =
                                                    new LedgerTransaction(transactionId, m.group(1),
                                                            m.group(2));
                                            state = ParserState.EXPECTING_TRANSACTION_DETAILS;
                                        }
                                    }
                                    case ParserState.EXPECTING_TRANSACTION_DETAILS: {
                                        if (transaction == null)
                                            throw new TransactionParserException(
                                                    "Transaction is null while expecting details");
                                        if (line.isEmpty()) {
                                            // transaction data collected
                                            transaction.insertInto(db);

                                            state = ParserState.EXPECTING_TRANSACTION;
                                            publishProgress(++transactionCount);
                                        }
                                        else {
                                            Matcher m = transactionDetailsPattern.matcher(line);
                                            if (m.find()) {
                                                String acc_name = m.group(1);
                                                String amount = m.group(2);
                                                amount = amount.replace(',', '.');
                                                transaction.add_item(
                                                        new LedgerTransactionItem(acc_name,
                                                                Float.valueOf(amount)));
                                            }
                                            else throw new IllegalStateException(String.format(
                                                    "Can't" + " parse transaction details"));
                                        }
                                    }
                                    default:
                                        throw new RuntimeException(
                                                String.format("Unknown " + "parser state %d",
                                                        state));
                                }
                            }
                            db.setTransactionSuccessful();
                        }
                        finally {
                            db.endTransaction();
                        }
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
        return null;
    }
    WeakReference<Context> getContextRef() {
        return contextRef;
    }

    private class TransactionParserException extends IllegalStateException {
        TransactionParserException(String message) {
            super(message);
        }
    }

    private class ParserState {
        static final int EXPECTING_JOURNAL = 0;
        static final int EXPECTING_TRANSACTION = 1;
        static final int EXPECTING_TRANSACTION_DESCRIPTION = 2;
        static final int EXPECTING_TRANSACTION_DETAILS = 3;
    }
}
