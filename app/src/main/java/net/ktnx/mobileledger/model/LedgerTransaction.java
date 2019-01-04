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

package net.ktnx.mobileledger.model;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import net.ktnx.mobileledger.utils.Digest;

import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;

public class LedgerTransaction {
    private static final String DIGEST_TYPE = "SHA-256";
    public final Comparator<LedgerTransactionAccount> comparator =
            new Comparator<LedgerTransactionAccount>() {
                @Override
                public int compare(LedgerTransactionAccount o1, LedgerTransactionAccount o2) {
                    int res = o1.getAccountName().compareTo(o2.getAccountName());
                    if (res != 0) return res;
                    res = o1.getCurrency().compareTo(o2.getCurrency());
                    if (res != 0) return res;
                    return Float.compare(o1.getAmount(), o2.getAmount());
                }
            };
    private Integer id;
    private String date;
    private String description;
    private ArrayList<LedgerTransactionAccount> accounts;
    public LedgerTransaction(Integer id, String date, String description) {
        this.id = id;
        this.date = date;
        this.description = description;
        this.accounts = new ArrayList<>();
        this.dataHash = null;
        dataLoaded = false;
    }
    private String dataHash;
    private boolean dataLoaded;
    public ArrayList<LedgerTransactionAccount> getAccounts() {
        return accounts;
    }
    public LedgerTransaction(String date, String description) {
        this(null, date, description);
    }
    public LedgerTransaction(int id) {
        this(id, null, null);
    }
    public void addAccount(LedgerTransactionAccount item) {
        accounts.add(item);
        dataHash = null;
    }
    public String getDate() {
        return date;
    }
    public void setDate(String date) {
        this.date = date;
        dataHash = null;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
        dataHash = null;
    }
    public int getId() {
        return id;
    }
    public void insertInto(SQLiteDatabase db) {
        fillDataHash();
        db.execSQL("INSERT INTO transactions(id, date, description, data_hash) values(?,?,?,?)",
                new Object[]{id, date, description, dataHash});

        for (LedgerTransactionAccount item : accounts) {
            db.execSQL("INSERT INTO transaction_accounts(transaction_id, account_name, amount, " +
                       "currency) values(?, ?, ?, ?)",
                    new Object[]{id, item.getAccountName(), item.getAmount(), item.getCurrency()});
        }
    }
    private void fillDataHash() {
        if (dataHash != null) return;
        try {
            Digest sha = new Digest(DIGEST_TYPE);
            StringBuilder data = new StringBuilder();
            data.append(getId());
            data.append('\0');
            data.append(getDescription());
            data.append('\0');
            for (LedgerTransactionAccount item : accounts) {
                data.append(item.getAccountName());
                data.append('\0');
                data.append(item.getCurrency());
                data.append('\0');
                data.append(item.getAmount());
            }
            sha.update(data.toString().getBytes(Charset.forName("UTF-8")));
            dataHash = sha.digestToHexString();
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(
                    String.format("Unable to get instance of %s digest", DIGEST_TYPE), e);
        }
    }
    public boolean existsInDb(SQLiteDatabase db) {
        fillDataHash();
        try (Cursor c = db
                .rawQuery("SELECT 1 from transactions where data_hash = ?", new String[]{dataHash}))
        {
            boolean result = c.moveToFirst();
            Log.d("db", String.format("Transaction %d (%s) %s", id, dataHash,
                    result ? "already present" : "not present"));
            return result;
        }
    }
    public void loadData(SQLiteDatabase db) {
        if (dataLoaded) return;

        try (Cursor cTr = db.rawQuery("SELECT date, description from transactions WHERE id=?",
                new String[]{String.valueOf(id)}))
        {
            if (cTr.moveToFirst()) {
                date = cTr.getString(0);
                description = cTr.getString(1);

                try (Cursor cAcc = db.rawQuery("SELECT account_name, amount, currency FROM " +
                                               "transaction_accounts WHERE transaction_id = ?",
                        new String[]{String.valueOf(id)}))
                {
                    while (cAcc.moveToNext()) {
                        addAccount(new LedgerTransactionAccount(cAcc.getString(0), cAcc.getFloat(1),
                                cAcc.getString(2)));
                    }

                    dataLoaded = true;
                }
            }
        }

    }
}
