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

package net.ktnx.mobileledger.model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class LedgerAccount {
    static Pattern reHigherAccount = Pattern.compile("^[^:]+:");
    private String name;
    private String shortName;
    private int level;
    private String parentName;
    private boolean hiddenByStar;
    private boolean hiddenByStarToBe;
    private boolean expanded;
    private List<LedgerAmount> amounts;
    private boolean hasSubAccounts;
    private boolean amountsExpanded;

    public LedgerAccount(String name) {
        this.setName(name);
        hiddenByStar = false;
    }

    public LedgerAccount(String name, float amount) {
        this.setName(name);
        this.hiddenByStar = false;
        this.expanded = true;
        this.amounts = new ArrayList<LedgerAmount>();
        this.addAmount(amount);
    }
    @Override
    public int hashCode() {
        return name.hashCode();
    }
    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) return false;

        return obj.getClass().equals(this.getClass()) &&
               name.equals(((LedgerAccount) obj).getName());
    }
    // an account is visible if:
    //  - it is starred (not hidden by a star)
    //  - and it has an expanded parent or is a top account
    public boolean isVisible() {
        if (hiddenByStar) return false;

        if (level == 0) return true;

        return isVisible(Data.accounts);
    }
    public boolean isVisible(List<LedgerAccount> list) {
        for (LedgerAccount acc : list) {
            if (acc.isParentOf(this)) {
                if (!acc.isExpanded()) return false;
            }
        }
        return true;
    }
    public boolean isParentOf(LedgerAccount potentialChild) {
        return potentialChild.getName().startsWith(name + ":");
    }
    public boolean isHiddenByStar() {
        return hiddenByStar;
    }
    public void setHiddenByStar(boolean hiddenByStar) {
        this.hiddenByStar = hiddenByStar;
    }
    private void stripName() {
        level = 0;
        shortName = name;
        StringBuilder parentBuilder = new StringBuilder();
        while (true) {
            Matcher m = reHigherAccount.matcher(shortName);
            if (m.find()) {
                level++;
                parentBuilder.append(m.group(0));
                shortName = m.replaceFirst("");
            }
            else break;
        }
        if (parentBuilder.length() > 0)
            parentName = parentBuilder.substring(0, parentBuilder.length() - 1);
        else parentName = null;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
        stripName();
    }
    public void addAmount(float amount, String currency) {
        if (amounts == null) amounts = new ArrayList<>();
        amounts.add(new LedgerAmount(amount, currency));
    }
    public void addAmount(float amount) {
        this.addAmount(amount, null);
    }

    public String getAmountsString() {
        if ((amounts == null) || amounts.isEmpty()) return "";

        StringBuilder builder = new StringBuilder();
        for (LedgerAmount amount : amounts) {
            String amt = amount.toString();
            if (builder.length() > 0) builder.append('\n');
            builder.append(amt);
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
        return parentName;
    }
    public void togglehidden() {
        hiddenByStar = !hiddenByStar;
    }

    public boolean isHiddenByStarToBe() {
        return hiddenByStarToBe;
    }
    public void setHiddenByStarToBe(boolean hiddenByStarToBe) {
        this.hiddenByStarToBe = hiddenByStarToBe;
    }
    public void toggleHiddenToBe() {
        setHiddenByStarToBe(!hiddenByStarToBe);
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
        if (amounts != null) amounts.clear();
    }
    public boolean amountsExpanded() { return amountsExpanded; }
    public void setAmountsExpanded(boolean flag) { amountsExpanded = flag; }
    public void toggleAmountsExpanded() { amountsExpanded = !amountsExpanded; }
}
