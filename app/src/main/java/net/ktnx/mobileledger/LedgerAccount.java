package net.ktnx.mobileledger;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class LedgerAccount {
    private String name;
    private String shortName;
    private int level;
    private String parentName;
    private boolean hidden;
    private List<LedgerAmount> amounts;
    private boolean selected;
    static Pattern higher_account = Pattern.compile("^[^:]+:");

    LedgerAccount(String name) {
        this.setName(name);
        hidden = false;
        selected = false;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    LedgerAccount(String name, float amount) {
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
            Matcher m = higher_account.matcher(shortName);
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

    String getName() {
        return name;
    }

    void addAmount(float amount, String currency) {
        if (amounts == null ) amounts = new ArrayList<>();
        amounts.add(new LedgerAmount(amount, currency));
    }
    void addAmount(float amount) {
        this.addAmount(amount, null);
    }

    String getAmountsString() {
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

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void toggleSelected() {
        selected = !selected;
    }
}
