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

import com.google.gson.annotations.Expose;

import net.ktnx.mobileledger.db.Profile;
import net.ktnx.mobileledger.db.Transaction;
import net.ktnx.mobileledger.db.TransactionAccount;
import net.ktnx.mobileledger.db.TransactionWithAccounts;
import net.ktnx.mobileledger.utils.Digest;
import net.ktnx.mobileledger.utils.Globals;
import net.ktnx.mobileledger.utils.SimpleDate;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class LedgerTransaction {
    private static final String DIGEST_TYPE = "SHA-256";

    @Expose (serialize = false, deserialize = false)
    public final transient Comparator<LedgerTransactionAccount> comparator = (o1, o2) -> {
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
    private final long ledgerId;
    private final List<LedgerTransactionAccount> accounts;
    private long dbId;
    private SimpleDate date;
    private String description;
    private String comment;
    private String dataHash;
    private boolean dataLoaded;
    public LedgerTransaction(long ledgerId, String dateString, String description)
            throws ParseException {
        this(ledgerId, Globals.parseLedgerDate(dateString), description);
    }
    public LedgerTransaction(TransactionWithAccounts dbo) {
        this(dbo.transaction.getLedgerId(), dbo.transaction.getProfileId());
        dbId = dbo.transaction.getId();
        date = new SimpleDate(dbo.transaction.getYear(), dbo.transaction.getMonth(),
                dbo.transaction.getDay());
        description = dbo.transaction.getDescription();
        comment = dbo.transaction.getComment();
        dataHash = dbo.transaction.getDataHash();
        if (dbo.accounts != null)
            for (TransactionAccount acc : dbo.accounts) {
                accounts.add(new LedgerTransactionAccount(acc));
            }
        dataLoaded = true;
    }
    public TransactionWithAccounts toDBO() {
        TransactionWithAccounts o = new TransactionWithAccounts();
        o.transaction = new Transaction();
        o.transaction.setId(dbId);
        o.transaction.setProfileId(profile);
        o.transaction.setLedgerId(ledgerId);
        o.transaction.setYear(date.year);
        o.transaction.setMonth(date.month);
        o.transaction.setDay(date.day);
        o.transaction.setDescription(description);
        o.transaction.setComment(comment);
        fillDataHash();
        o.transaction.setDataHash(dataHash);

        o.accounts = new ArrayList<>();
        int orderNo = 1;
        for (LedgerTransactionAccount acc : accounts) {
            TransactionAccount a = acc.toDBO();
            a.setOrderNo(orderNo++);
            a.setTransactionId(dbId);
            o.accounts.add(a);
        }
        return o;
    }
    public LedgerTransaction(long ledgerId, SimpleDate date, String description, Profile profile) {
        this.profile = profile.getId();
        this.ledgerId = ledgerId;
        this.date = date;
        this.description = description;
        this.accounts = new ArrayList<>();
        this.dataHash = null;
        dataLoaded = false;
    }
    public LedgerTransaction(long ledgerId, SimpleDate date, String description) {
        this(ledgerId, date, description, Data.getProfile());
    }
    public LedgerTransaction(SimpleDate date, String description) {
        this(0, date, description);
    }
    public LedgerTransaction(int ledgerId) {
        this(ledgerId, (SimpleDate) null, null);
    }
    public LedgerTransaction(long ledgerId, long profileId) {
        this.profile = profileId;
        this.ledgerId = ledgerId;
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
    public long getLedgerId() {
        return ledgerId;
    }
    protected void fillDataHash() {
        if (dataHash != null)
            return;
        try {
            Digest sha = new Digest(DIGEST_TYPE);
            StringBuilder data = new StringBuilder();
            data.append("ver1");
            data.append(profile);
            data.append(getLedgerId());
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
