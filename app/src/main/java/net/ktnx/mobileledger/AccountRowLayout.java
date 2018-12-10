package net.ktnx.mobileledger;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

class AccountRowLayout extends LinearLayout {
    private String accountName;

    public
    AccountRowLayout(Context context, String accountName) {
        super(context);
        this.accountName = accountName;
    }

    public
    AccountRowLayout(Context context, AttributeSet attrs, String accountName) {
        super(context, attrs);
        this.accountName = accountName;
    }

    public
    AccountRowLayout(Context context, AttributeSet attrs, int defStyleAttr, String accountName) {
        super(context, attrs, defStyleAttr);
        this.accountName = accountName;
    }

    public
    AccountRowLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes,
                     String accountName) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.accountName = accountName;
    }

    public
    String getAccountName() {
        return accountName;
    }
}
