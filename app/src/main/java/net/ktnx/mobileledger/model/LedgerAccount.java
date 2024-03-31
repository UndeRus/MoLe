/*
 * Copyright © 2024 Damyan Ivanov.
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.ktnx.mobileledger.db.Account;
import net.ktnx.mobileledger.db.AccountValue;
import net.ktnx.mobileledger.db.AccountWithAmounts;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class LedgerAccount {
    private static final char ACCOUNT_DELIMITER = ':';
    static Pattern reHigherAccount = Pattern.compile("^[^:]+:");
    private final LedgerAccount parent;
    private long dbId;
    private long profileId;
    private String name;
    private String shortName;
    private int level;
    private boolean expanded;
    private List<LedgerAmount> amounts;
    private boolean hasSubAccounts;
    private boolean amountsExpanded;

    public LedgerAccount(String name, @Nullable LedgerAccount parent) {
        this.parent = parent;
        if (parent != null && !name.startsWith(parent.getName() + ":"))
            throw new IllegalStateException(
                    String.format("Account name '%s' doesn't match parent account '%s'", name,
                            parent.getName()));
        this.setName(name);
    }
    @Nullable
    public static String extractParentName(@NonNull String accName) {
        int colonPos = accName.lastIndexOf(ACCOUNT_DELIMITER);
        if (colonPos < 0)
            return null;    // no parent account -- this is a top-level account
        else
            return accName.substring(0, colonPos);
    }
    public static boolean isParentOf(@NonNull String possibleParent, @NonNull String accountName) {
        return accountName.startsWith(possibleParent + ':');
    }
    @NonNull
    static public LedgerAccount fromDBO(AccountWithAmounts in, LedgerAccount parent) {
        LedgerAccount res = new LedgerAccount(in.account.getName(), parent);
        res.dbId = in.account.getId();
        res.profileId = in.account.getProfileId();
        res.setName(in.account.getName());
        res.setExpanded(in.account.isExpanded());
        res.setAmountsExpanded(in.account.isAmountsExpanded());

        res.amounts = new ArrayList<>();
        for (AccountValue val : in.amounts) {
            res.amounts.add(new LedgerAmount(val.getValue(), val.getCurrency()));
        }

        return res;
    }
    public static int determineLevel(String accName) {
        int level = 0;
        int delimiterPosition = accName.indexOf(ACCOUNT_DELIMITER);
        while (delimiterPosition >= 0) {
            level++;
            delimiterPosition = accName.indexOf(ACCOUNT_DELIMITER, delimiterPosition + 1);
        }
        return level;
    }
    @Override
    public int hashCode() {
        return name.hashCode();
    }
    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null)
            return false;

        if (!(obj instanceof LedgerAccount))
            return false;

        LedgerAccount acc = (LedgerAccount) obj;
        if (!name.equals(acc.name))
            return false;

        if (!getAmountsString().equals(acc.getAmountsString()))
            return false;

        return expanded == acc.expanded && amountsExpanded == acc.amountsExpanded;
    }
    // an account is visible if:
    //  - it has an expanded visible parent or is a top account
    public boolean isVisible() {
        if (parent == null)
            return true;

        return (parent.isExpanded() && parent.isVisible());
    }
    public boolean isParentOf(LedgerAccount potentialChild) {
        return potentialChild.getName()
                             .startsWith(name + ":");
    }
    private void stripName() {
        String[] split = name.split(":");
        shortName = split[split.length - 1];
        level = split.length - 1;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
        stripName();
    }
    public void addAmount(float amount, @NonNull String currency) {
        if (amounts == null)
            amounts = new ArrayList<>();
        amounts.add(new LedgerAmount(amount, currency));
    }
    public void addAmount(float amount) {
        this.addAmount(amount, "");
    }
    public int getAmountCount() { return (amounts != null) ? amounts.size() : 0; }
    public String getAmountsString() {
        if ((amounts == null) || amounts.isEmpty())
            return "";

        StringBuilder builder = new StringBuilder();
        for (LedgerAmount amount : amounts) {
            String amt = amount.toString();
            if (builder.length() > 0)
                builder.append('\n');
            builder.append(amt);
        }

        return builder.toString();
    }
    public String getAmountsString(int limit) {
        if ((amounts == null) || amounts.isEmpty())
            return "";

        int included = 0;
        StringBuilder builder = new StringBuilder();
        for (LedgerAmount amount : amounts) {
            String amt = amount.toString();
            if (builder.length() > 0)
                builder.append('\n');
            builder.append(amt);
            included++;
            if (included == limit)
                break;
        }

        return builder.toString();
    }
    public int getLevel() {
        return level;
    }
    @NonNull
    public String getShortName() {
        return shortName;
    }
    public String getParentName() {
        return (parent == null) ? null : parent.getName();
    }
    public boolean hasSubAccounts() {
        return hasSubAccounts;
    }
    public void setHasSubAccounts(boolean hasSubAccounts) {
        this.hasSubAccounts = hasSubAccounts;
    }
    public boolean isExpanded() {
        return expanded;
    }
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }
    public void toggleExpanded() {
        expanded = !expanded;
    }
    public void removeAmounts() {
        if (amounts != null)
            amounts.clear();
    }
    public boolean amountsExpanded() {return amountsExpanded;}
    public void setAmountsExpanded(boolean flag) {amountsExpanded = flag;}
    public void toggleAmountsExpanded() {amountsExpanded = !amountsExpanded;}
    public void propagateAmountsTo(LedgerAccount acc) {
        for (LedgerAmount a : amounts)
            a.propagateToAccount(acc);
    }
    public boolean allAmountsAreZero() {
        for (LedgerAmount a : amounts) {
            if (a.getAmount() != 0)
                return false;
        }

        return true;
    }
    public List<LedgerAmount> getAmounts() {
        return amounts;
    }
    @NonNull
    public Account toDBO() {
        Account dbo = new Account();
        dbo.setName(name);
        dbo.setNameUpper(name.toUpperCase());
        dbo.setParentName(extractParentName(name));
        dbo.setLevel(level);
        dbo.setId(dbId);
        dbo.setProfileId(profileId);
        dbo.setExpanded(expanded);
        dbo.setAmountsExpanded(amountsExpanded);

        return dbo;
    }
    @NonNull
    public AccountWithAmounts toDBOWithAmounts() {
        AccountWithAmounts dbo = new AccountWithAmounts();
        dbo.account = toDBO();

        dbo.amounts = new ArrayList<>();
        for (LedgerAmount amt : getAmounts()) {
            AccountValue val = new AccountValue();
            val.setCurrency(amt.getCurrency());
            val.setValue(amt.getAmount());
            dbo.amounts.add(val);
        }

        return dbo;
    }
    public long getId() {
        return dbId;
    }
}
