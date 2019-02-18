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

public class LedgerAccount {
    private String name;
    private String shortName;
    private int level;
    private String parentName;
    private boolean hidden;
    private boolean hiddenToBe;
    private List<LedgerAmount> amounts;
    static Pattern reHigherAccount = Pattern.compile("^[^:]+:");

    public LedgerAccount(String name) {
        this.setName(name);
        hidden = false;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public LedgerAccount(String name, float amount) {
        this.setName(name);
        this.hidden = false;
        this.amounts = new ArrayList<LedgerAmount>();
        this.addAmount(amount);
    }

    public void setName(String name) {
        this.name = name;
        stripName();
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

    public void addAmount(float amount, String currency) {
        if (amounts == null ) amounts = new ArrayList<>();
        amounts.add(new LedgerAmount(amount, currency));
    }
    public void addAmount(float amount) {
        this.addAmount(amount, null);
    }

    public String getAmountsString() {
        if ((amounts == null) || amounts.isEmpty()) return "";

        StringBuilder builder = new StringBuilder();
        for( LedgerAmount amount : amounts ) {
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
        hidden = !hidden;
    }

    public boolean isHiddenToBe() {
        return hiddenToBe;
    }
    public void setHiddenToBe(boolean hiddenToBe) {
        this.hiddenToBe = hiddenToBe;
    }
    public void toggleHiddenToBe() {
        setHiddenToBe(!hiddenToBe);
    }
}
