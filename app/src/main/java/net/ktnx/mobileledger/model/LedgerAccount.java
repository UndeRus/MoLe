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

package net.ktnx.mobileledger.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class LedgerAccount {
    static Pattern reHigherAccount = Pattern.compile("^[^:]+:");
    private String name;
    private String shortName;
    private int level;
    private final LedgerAccount parent;
    private boolean expanded;
    private List<LedgerAmount> amounts;
    private boolean hasSubAccounts;
    private boolean amountsExpanded;
    private final WeakReference<MobileLedgerProfile> profileWeakReference;

    public LedgerAccount(MobileLedgerProfile profile, String name, @Nullable LedgerAccount parent) {
        this.profileWeakReference = new WeakReference<>(profile);
        this.parent = parent;
        if (parent != null && !name.startsWith(parent.getName() + ":"))
            throw new IllegalStateException(
                    String.format("Account name '%s' doesn't match parent account '%s'", name,
                            parent.getName()));
        this.setName(name);
    }
    @Nullable
    public static String extractParentName(@NonNull String accName) {
        int colonPos = accName.lastIndexOf(':');
        if (colonPos < 0)
            return null;    // no parent account -- this is a top-level account
        else
            return accName.substring(0, colonPos);
    }
    public @Nullable
    MobileLedgerProfile getProfile() {
        return profileWeakReference.get();
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
        if (parent == null) {
            level = 0;
            shortName = name;
        }
        else {
            level = parent.level + 1;
            shortName = name.substring(parent.getName()
                                             .length() + 1);
        }
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
    public boolean amountsExpanded() { return amountsExpanded; }
    public void setAmountsExpanded(boolean flag) { amountsExpanded = flag; }
    public void toggleAmountsExpanded() { amountsExpanded = !amountsExpanded; }

    public void propagateAmountsTo(LedgerAccount acc) {
        for (LedgerAmount a : amounts)
            a.propagateToAccount(acc);
    }
    public List<LedgerAmount> getAmounts() {
        return amounts;
    }
}
