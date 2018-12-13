package net.ktnx.mobileledger;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;

class LedgerAmount {
    private String currency;
    private float amount;

    public
    LedgerAmount(float amount, @NonNull String currency) {
        this.currency = currency;
        this.amount = amount;
    }

    public
    LedgerAmount(float amount) {
        this.amount = amount;
        this.currency = null;
    }

    @SuppressLint("DefaultLocale")
    @NonNull
    public String toString() {
        if (currency == null) return String.format("%,1.2f", amount);
        else return String.format("%s %,1.2f", currency, amount);
    }
}
