package net.ktnx.mobileledger;

class LedgerTransactionItem {
    private String account_name;
    private float amount;
    private boolean amount_set;

    LedgerTransactionItem(String account_name, float amount) {
        this.account_name = account_name;
        this.amount = amount;
        this.amount_set = true;
    }

    public LedgerTransactionItem(String account_name) {
        this.account_name = account_name;
    }

    public String get_account_name() {
        return account_name;
    }

    public void set_account_name(String account_name) {
        this.account_name = account_name;
    }

    public float get_amount() {
        if (!amount_set)
            throw new IllegalStateException("Account amount is not set");

        return amount;
    }

    public void set_amount(float account_amount) {
        this.amount = account_amount;
        this.amount_set = true;
    }

    public void reset_amount() {
        this.amount_set = false;
    }

    public boolean is_amount_set() {
        return amount_set;
    }
}
