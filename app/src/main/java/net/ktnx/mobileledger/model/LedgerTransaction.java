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

package net.ktnx.mobileledger.model;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.ktnx.mobileledger.App;
import net.ktnx.mobileledger.utils.Digest;
import net.ktnx.mobileledger.utils.Globals;
import net.ktnx.mobileledger.utils.SimpleDate;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LedgerTransaction {
    private static final String DIGEST_TYPE = "SHA-256";
    public final Comparator<LedgerTransactionAccount> comparator = (o1, o2) -> {
        int res = o1.getAccountName()
                    .compareTo(o2.getAccountName());
        if (res != 0)
            return res;
        res = o1.getCurrency()
                .compareTo(o2.getCurrency());
        if (res != 0)
            return res;
        res = o1.getComment()
                .compareTo(o2.getComment());
        if (res != 0)
            return res;
        return Float.compare(o1.getAmount(), o2.getAmount());
    };
    private final long profile;
    private final Integer id;
    private final List<LedgerTransactionAccount> accounts;
    private SimpleDate date;
    private String description;
    private String comment;
    private String dataHash;
    private boolean dataLoaded;
    public LedgerTransaction(Integer id, String dateString, String description)
            throws ParseException {
        this(id, Globals.parseLedgerDate(dateString), description);
    }
    public LedgerTransaction(Integer id, SimpleDate date, String description,
                             MobileLedgerProfile profile) {
        this.profile = profile.getId();
        this.id = id;
        this.date = date;
        this.description = description;
        this.accounts = new ArrayList<>();
        this.dataHash = null;
        dataLoaded = false;
    }
    public LedgerTransaction(Integer id, SimpleDate date, String description) {
        this(id, date, description, Data.getProfile());
    }
    public LedgerTransaction(SimpleDate date, String description) {
        this(null, date, description);
    }
    public LedgerTransaction(int id) {
        this(id, (SimpleDate) null, null);
    }
    public LedgerTransaction(int id, long profileId) {
        this.profile = profileId;
        this.id = id;
        this.date = null;
        this.description = null;
        this.accounts = new ArrayList<>();
        this.dataHash = null;
        this.dataLoaded = false;
    }
    public List<LedgerTransactionAccount> getAccounts() {
        return accounts;
    }
    public void addAccount(LedgerTransactionAccount item) {
        accounts.add(item);
        dataHash = null;
    }
    @Nullable
    public SimpleDate getDateIfAny() {
        return date;
    }
    @NonNull
    public SimpleDate getDate() {
        if (date == null)
            throw new IllegalStateException("Transaction has no date");
        return date;
    }
    public void setDate(SimpleDate date) {
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
    public String getComment() {
        return comment;
    }
    public void setComment(String comment) {
        this.comment = comment;
    }
    public int getId() {
        return id;
    }
    protected void fillDataHash() {
        loadData(App.getDatabase());
        if (dataHash != null)
            return;
        try {
            Digest sha = new Digest(DIGEST_TYPE);
            StringBuilder data = new StringBuilder();
            data.append("ver1");
            data.append(profile);
            data.append(getId());
            data.append('\0');
            data.append(getDescription());
            data.append('\0');
            data.append(getComment());
            data.append('\0');
            data.append(Globals.formatLedgerDate(getDate()));
            data.append('\0');
            for (LedgerTransactionAccount item : accounts) {
                data.append(item.getAccountName());
                data.append('\0');
                data.append(item.getCurrency());
                data.append('\0');
                data.append(item.getAmount());
                data.append('\0');
                data.append(item.getComment());
            }
            sha.update(data.toString()
                           .getBytes(StandardCharsets.UTF_8));
            dataHash = sha.digestToHexString();
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(
                    String.format("Unable to get instance of %s digest", DIGEST_TYPE), e);
        }
    }
    public synchronized void loadData(SQLiteDatabase db) {
        if (dataLoaded)
            return;

        try (Cursor cTr = db.rawQuery(
                "SELECT year, month, day, description, comment from transactions WHERE id=?",
                new String[]{String.valueOf(id)}))
        {
            if (cTr.moveToFirst()) {
                date = new SimpleDate(cTr.getInt(0), cTr.getInt(1), cTr.getInt(2));
                description = cTr.getString(3);
                comment = cTr.getString(4);

                accounts.clear();

                try (Cursor cAcc = db.rawQuery(
                        "SELECT account_name, amount, currency, comment FROM " +
                        "transaction_accounts WHERE transaction_id = ?",
                        new String[]{String.valueOf(id)}))
                {
                    while (cAcc.moveToNext()) {
//                        debug("transactions",
//                                String.format("Loaded %d: %s %1.2f %s", id, cAcc.getString(0),
//                                        cAcc.getFloat(1), cAcc.getString(2)));
                        addAccount(new LedgerTransactionAccount(cAcc.getString(0), cAcc.getFloat(1),
                                cAcc.getString(2), cAcc.getString(3)));
                    }

                    finishLoading();
                }
            }
        }

    }
    public String getDataHash() {
        fillDataHash();
        return dataHash;
    }
    public void finishLoading() {
        dataLoaded = true;
    }
    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null)
            return false;
        if (!obj.getClass()
                .equals(this.getClass()))
            return false;

        return ((LedgerTransaction) obj).getDataHash()
                                        .equals(getDataHash());
    }

    public boolean hasAccountNamedLike(String name) {
        name = name.toUpperCase();
        for (LedgerTransactionAccount acc : accounts) {
            if (acc.getAccountName()
                   .toUpperCase()
                   .contains(name))
                return true;
        }

        return false;
    }
    public void markDataAsLoaded() {
        dataLoaded = true;
    }
}
