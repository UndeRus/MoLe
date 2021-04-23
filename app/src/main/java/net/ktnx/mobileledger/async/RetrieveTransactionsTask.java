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

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.OperationCanceledException;

import androidx.annotation.NonNull;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;

import net.ktnx.mobileledger.dao.AccountDAO;
import net.ktnx.mobileledger.dao.TransactionDAO;
import net.ktnx.mobileledger.db.Account;
import net.ktnx.mobileledger.db.AccountWithAmounts;
import net.ktnx.mobileledger.db.DB;
import net.ktnx.mobileledger.db.Option;
import net.ktnx.mobileledger.db.Profile;
import net.ktnx.mobileledger.db.TransactionWithAccounts;
import net.ktnx.mobileledger.err.HTTPException;
import net.ktnx.mobileledger.json.API;
import net.ktnx.mobileledger.json.AccountListParser;
import net.ktnx.mobileledger.json.ApiNotSupportedException;
import net.ktnx.mobileledger.json.TransactionListParser;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerAccount;
import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.model.LedgerTransactionAccount;
import net.ktnx.mobileledger.ui.MainModel;
import net.ktnx.mobileledger.utils.Logger;
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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RetrieveTransactionsTask extends
        AsyncTask<Void, RetrieveTransactionsTask.Progress, RetrieveTransactionsTask.Result> {
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
    private static final String TAG = "RTT";
    // %3A is '='
    private final Pattern reAccountName =
            Pattern.compile("/register\\?q=inacct%3A([a-zA-Z0-9%]+)\"");
    private final Pattern reAccountValue = Pattern.compile(
            "<span class=\"[^\"]*\\bamount\\b[^\"]*\">\\s*([-+]?[\\d.,]+)(?:\\s+(\\S+))?</span>");
    private final MainModel mainModel;
    private final Profile profile;
    private int expectedPostingsCount = -1;
    public RetrieveTransactionsTask(@NonNull MainModel mainModel, @NonNull Profile profile) {
        this.mainModel = mainModel;
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
    protected void onPostExecute(Result result) {
        super.onPostExecute(result);
        Progress progress = new Progress();
        progress.setState(ProgressState.FINISHED);
        progress.setError(result.error);
        onProgressUpdate(progress);
    }
    @Override
    protected void onCancelled() {
        super.onCancelled();
        Progress progress = new Progress();
        progress.setState(ProgressState.FINISHED);
        onProgressUpdate(progress);
    }
    private void retrieveTransactionListLegacy(List<LedgerAccount> accounts,
                                               List<LedgerTransaction> transactions)
            throws IOException, HTTPException {
        Progress progress = Progress.indeterminate();
        progress.setState(ProgressState.RUNNING);
        progress.setTotal(expectedPostingsCount);
        int maxTransactionId = -1;
        HashMap<String, LedgerAccount> map = new HashMap<>();
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
                            lastAccount = new LedgerAccount(accName, parentAccount);

                            accounts.add(lastAccount);
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
                                        "Transaction Id is 0 while expecting description");

                            String date = Objects.requireNonNull(m.group(1));
                            try {
                                int equalsIndex = date.indexOf('=');
                                if (equalsIndex >= 0)
                                    date = date.substring(equalsIndex + 1);
                                transaction =
                                        new LedgerTransaction(transactionId, date, m.group(2));
                            }
                            catch (ParseException e) {
                                throw new TransactionParserException(
                                        String.format("Error parsing date '%s'", date));
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
                                    transaction.getLedgerId()));

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
                                L(String.format(Locale.ENGLISH, "%d: %s = %s",
                                        transaction.getLedgerId(), lta.getAccountName(),
                                        lta.getAmount()));
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
        }
    }
    @NonNull
    public LedgerAccount ensureAccountExists(String accountName, HashMap<String, LedgerAccount> map,
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

        acc = new LedgerAccount(accountName, parentAccount);
        createdAccounts.add(acc);
        return acc;
    }
    public void addNumberOfPostings(int number) {
        expectedPostingsCount += number;
    }
    private List<LedgerAccount> retrieveAccountList()
            throws IOException, HTTPException, ApiNotSupportedException {
        final API apiVersion = API.valueOf(profile.getApiVersion());
        if (apiVersion.equals(API.auto)) {
            return retrieveAccountListAnyVersion();
        }
        else if (apiVersion.equals(API.html)) {
            Logger.debug("json",
                    "Declining using JSON API for /accounts with configured legacy API version");
            return null;
        }
        else {
            return retrieveAccountListForVersion(apiVersion);
        }
    }
    private List<LedgerAccount> retrieveAccountListAnyVersion()
            throws ApiNotSupportedException, IOException, HTTPException {
        for (API ver : API.allVersions) {
            try {
                return retrieveAccountListForVersion(ver);
            }
            catch (JsonParseException | RuntimeJsonMappingException e) {
                Logger.debug("json",
                        String.format(Locale.US, "Error during account list retrieval using API %s",
                                ver.getDescription()), e);
            }

        }

        throw new ApiNotSupportedException();
    }
    private List<LedgerAccount> retrieveAccountListForVersion(API version)
            throws IOException, HTTPException {
        HttpURLConnection http = NetworkUtil.prepareConnection(profile, "accounts");
        http.setAllowUserInteraction(false);
        switch (http.getResponseCode()) {
            case 200:
                break;
            case 404:
                return null;
            default:
                throw new HTTPException(http.getResponseCode(), http.getResponseMessage());
        }
        publishProgress(Progress.indeterminate());
        ArrayList<LedgerAccount> list = new ArrayList<>();
        HashMap<String, LedgerAccount> map = new HashMap<>();
        throwIfCancelled();
        try (InputStream resp = http.getInputStream()) {
            throwIfCancelled();
            if (http.getResponseCode() != 200)
                throw new IOException(String.format("HTTP error %d", http.getResponseCode()));

            AccountListParser parser = AccountListParser.forApiVersion(version, resp);
            expectedPostingsCount = 0;

            while (true) {
                throwIfCancelled();
                LedgerAccount acc = parser.nextAccount(this, map);
                if (acc == null)
                    break;
                list.add(acc);
            }
            throwIfCancelled();
        }

        return list;
    }
    private List<LedgerTransaction> retrieveTransactionList()
            throws ParseException, HTTPException, IOException, ApiNotSupportedException {
        final API apiVersion = API.valueOf(profile.getApiVersion());
        if (apiVersion.equals(API.auto)) {
            return retrieveTransactionListAnyVersion();
        }
        else if (apiVersion.equals(API.html)) {
            Logger.debug("json",
                    "Declining using JSON API for /accounts with configured legacy API version");
            return null;
        }
        else {
            return retrieveTransactionListForVersion(apiVersion);
        }

    }
    private List<LedgerTransaction> retrieveTransactionListAnyVersion()
            throws ApiNotSupportedException {
        for (API ver : API.allVersions) {
            try {
                return retrieveTransactionListForVersion(ver);
            }
            catch (Exception e) {
                Logger.debug("json",
                        String.format(Locale.US, "Error during account list retrieval using API %s",
                                ver.getDescription()));
            }

        }

        throw new ApiNotSupportedException();
    }
    private List<LedgerTransaction> retrieveTransactionListForVersion(API apiVersion)
            throws IOException, ParseException, HTTPException {
        Progress progress = new Progress();
        progress.setTotal(expectedPostingsCount);

        HttpURLConnection http = NetworkUtil.prepareConnection(profile, "transactions");
        http.setAllowUserInteraction(false);
        publishProgress(progress);
        switch (http.getResponseCode()) {
            case 200:
                break;
            case 404:
                return null;
            default:
                throw new HTTPException(http.getResponseCode(), http.getResponseMessage());
        }
        ArrayList<LedgerTransaction> trList = new ArrayList<>();
        try (InputStream resp = http.getInputStream()) {
            throwIfCancelled();

            TransactionListParser parser = TransactionListParser.forApiVersion(apiVersion, resp);

            int processedPostings = 0;

            while (true) {
                throwIfCancelled();
                LedgerTransaction transaction = parser.nextTransaction();
                throwIfCancelled();
                if (transaction == null)
                    break;

                trList.add(transaction);

                progress.setProgress(processedPostings += transaction.getAccounts()
                                                                     .size());
//                Logger.debug("trParser",
//                        String.format(Locale.US, "Parsed transaction %d - %s", transaction
//                        .getId(),
//                                transaction.getDescription()));
//                for (LedgerTransactionAccount acc : transaction.getAccounts()) {
//                    Logger.debug("trParser",
//                            String.format(Locale.US, "  %s", acc.getAccountName()));
//                }
                publishProgress(progress);
            }

            throwIfCancelled();
        }

        // json interface returns transactions if file order and the rest of the machinery
        // expects them in reverse chronological order
        Collections.sort(trList, (o1, o2) -> {
            int res = o2.getDate()
                        .compareTo(o1.getDate());
            if (res != 0)
                return res;
            return Long.compare(o2.getLedgerId(), o1.getLedgerId());
        });
        return trList;
    }

    @SuppressLint("DefaultLocale")
    @Override
    protected Result doInBackground(Void... params) {
        Data.backgroundTaskStarted();
        List<LedgerAccount> accounts;
        List<LedgerTransaction> transactions;
        try {
            accounts = retrieveAccountList();
            // accounts is null in API-version auto-detection and means
            // requesting 'html' API version via the JSON classes
            // this can't work, and the null results in the legacy code below
            // being called
            if (accounts == null)
                transactions = null;
            else
                transactions = retrieveTransactionList();

            if (accounts == null || transactions == null) {
                accounts = new ArrayList<>();
                transactions = new ArrayList<>();
                retrieveTransactionListLegacy(accounts, transactions);
            }

            mainModel.updateDisplayedTransactionsFromWeb(transactions);

            new AccountAndTransactionListSaver(accounts, transactions).start();

            return new Result(null);
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
            return new Result("Invalid server URL");
        }
        catch (HTTPException e) {
            e.printStackTrace();
            return new Result(
                    String.format("HTTP error %d: %s", e.getResponseCode(), e.getMessage()));
        }
        catch (IOException e) {
            e.printStackTrace();
            return new Result(e.getLocalizedMessage());
        }
        catch (RuntimeJsonMappingException e) {
            e.printStackTrace();
            return new Result(Result.ERR_JSON_PARSER_ERROR);
        }
        catch (ParseException e) {
            e.printStackTrace();
            return new Result("Network error");
        }
        catch (OperationCanceledException e) {
            e.printStackTrace();
            return new Result("Operation cancelled");
        }
        catch (ApiNotSupportedException e) {
            e.printStackTrace();
            return new Result("Server version not supported");
        }
        finally {
            Data.backgroundTaskFinished();
        }
    }
    public void throwIfCancelled() {
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

    public static class Result {
        public static String ERR_JSON_PARSER_ERROR = "err_json_parser";
        public String error;
        public List<LedgerAccount> accounts;
        public List<LedgerTransaction> transactions;
        Result(String error) {
            this.error = error;
        }
        Result(List<LedgerAccount> accounts, List<LedgerTransaction> transactions) {
            this.accounts = accounts;
            this.transactions = transactions;
        }
    }

    private class AccountAndTransactionListSaver extends Thread {
        private final List<LedgerAccount> accounts;
        private final List<LedgerTransaction> transactions;
        public AccountAndTransactionListSaver(List<LedgerAccount> accounts,
                                              List<LedgerTransaction> transactions) {
            this.accounts = accounts;
            this.transactions = transactions;
        }
        @Override
        public void run() {
            AccountDAO accDao = DB.get()
                                  .getAccountDAO();
            TransactionDAO trDao = DB.get()
                                     .getTransactionDAO();

            Logger.debug(TAG, "Preparing account list");
            final List<AccountWithAmounts> list = new ArrayList<>();
            for (LedgerAccount acc : accounts) {
                final AccountWithAmounts a = acc.toDBOWithAmounts();
                Account existing = accDao.getByNameSync(profile.getId(), acc.getName());
                if (existing != null) {
                    a.account.setExpanded(existing.isExpanded());
                    a.account.setAmountsExpanded(existing.isAmountsExpanded());
                    a.account.setId(existing.getId()); // not strictly needed, but since we have it
                    // anyway...
                }

                list.add(a);
            }
            Logger.debug(TAG, "Account list prepared. Storing");
            accDao.storeAccountsSync(list, profile.getId());
            Logger.debug(TAG, "Account list stored");

            Logger.debug(TAG, "Preparing transaction list");
            final List<TransactionWithAccounts> tranList = new ArrayList<>();

            for (LedgerTransaction tr : transactions)
                tranList.add(tr.toDBO());

            Logger.debug(TAG, "Storing transaction list");
            trDao.storeTransactionsSync(tranList, profile.getId());

            Logger.debug(TAG, "Transactions stored");

            DB.get()
              .getOptionDAO()
              .insertSync(new Option(profile.getId(), Option.OPT_LAST_SCRAPE,
                      String.valueOf((new Date()).getTime())));
        }
    }
}
