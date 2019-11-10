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

package net.ktnx.mobileledger.ui.activity;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.async.DescriptionSelectedCallback;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerTransactionAccount;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
import net.ktnx.mobileledger.ui.DatePickerFragment;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.MLDB;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

class NewTransactionItemHolder extends RecyclerView.ViewHolder
        implements DatePickerFragment.DatePickedListener, DescriptionSelectedCallback {
    private NewTransactionModel.Item item;
    private TextView tvDate;
    private AutoCompleteTextView tvDescription;
    private AutoCompleteTextView tvAccount;
    private TextView tvAmount;
    private ConstraintLayout lHead;
    private LinearLayout lAccount;
    private FrameLayout lPadding;
    private MobileLedgerProfile mProfile;
    private Date date;
    private Observer<Date> dateObserver;
    private Observer<String> descriptionObserver;
    private Observer<String> hintObserver;
    private Observer<Integer> focusedAccountObserver;
    private Observer<Integer> accountCountObserver;
    private boolean inUpdate = false;
    private boolean syncingData = false;
    NewTransactionItemHolder(@NonNull View itemView, NewTransactionItemsAdapter adapter) {
        super(itemView);
        tvAccount = itemView.findViewById(R.id.account_row_acc_name);
        tvAmount = itemView.findViewById(R.id.account_row_acc_amounts);
        tvDate = itemView.findViewById(R.id.new_transaction_date);
        tvDescription = itemView.findViewById(R.id.new_transaction_description);
        lHead = itemView.findViewById(R.id.ntr_data);
        lAccount = itemView.findViewById(R.id.ntr_account);
        lPadding = itemView.findViewById(R.id.ntr_padding);

        tvDescription.setNextFocusForwardId(View.NO_ID);
        tvAccount.setNextFocusForwardId(View.NO_ID);
        tvAmount.setNextFocusForwardId(View.NO_ID); // magic!

        tvDate.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) pickTransactionDate();
        });
        tvDate.setOnClickListener(v -> pickTransactionDate());

        mProfile = Data.profile.getValue();
        if (mProfile == null) throw new AssertionError();

        MLDB.hookAutocompletionAdapter(tvDescription.getContext(), tvDescription,
                MLDB.DESCRIPTION_HISTORY_TABLE, "description", false, adapter, mProfile);
        MLDB.hookAutocompletionAdapter(tvAccount.getContext(), tvAccount, MLDB.ACCOUNTS_TABLE,
                "name", true, this, mProfile);

        final TextWatcher tw = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
//                debug("input", "text changed");
                if (inUpdate) return;

                Logger.debug("textWatcher", "calling syncData()");
                syncData();
                Logger.debug("textWatcher",
                        "syncData() returned, checking if transaction is submittable");
                adapter.model.checkTransactionSubmittable(adapter);
                Logger.debug("textWatcher", "done");
            }
        };
        tvDescription.addTextChangedListener(tw);
        tvAccount.addTextChangedListener(tw);
        tvAmount.addTextChangedListener(tw);

        dateObserver = date -> {
            if (syncingData) return;
            tvDate.setText(item.getFormattedDate());
        };
        descriptionObserver = description -> {
            if (syncingData) return;
            tvDescription.setText(description);
        };
        hintObserver = hint -> {
            if (syncingData) return;
            tvAmount.setHint(hint);
        };
        focusedAccountObserver = index -> {
            if ((index != null) && index.equals(getAdapterPosition())) {
                switch (item.getType()) {
                    case generalData:
                        tvDate.requestFocus();
                        break;
                    case transactionRow:
                        tvAccount.requestFocus();
                        tvAccount.dismissDropDown();
                        tvAccount.selectAll();
                        break;
                }
            }
        };
        accountCountObserver = count -> {
            if (getAdapterPosition() == count) tvAmount.setImeOptions(EditorInfo.IME_ACTION_DONE);
            else tvAmount.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        };
    }
    private void beginUpdates() {
        if (inUpdate) throw new RuntimeException("Already in update mode");
        inUpdate = true;
    }
    private void endUpdates() {
        if (!inUpdate) throw new RuntimeException("Not in update mode");
        inUpdate = false;
    }
    /**
     * syncData()
     * <p>
     * Stores the data from the UI elements into the model item
     */
    private void syncData() {
        if (item == null) return;

        if (syncingData) {
            Logger.debug("new-trans", "skipping syncData() loop");
            return;
        }

        syncingData = true;

        try {
            switch (item.getType()) {
                case generalData:
                    item.setDate(String.valueOf(tvDate.getText()));
                    item.setDescription(String.valueOf(tvDescription.getText()));
                    break;
                case transactionRow:
                    item.getAccount()
                        .setAccountName(String.valueOf(tvAccount.getText()));

                    // TODO: handle multiple amounts
                    String amount = String.valueOf(tvAmount.getText());
                    amount = amount.trim();

                    if (!amount.isEmpty()) item.getAccount()
                                               .setAmount(Float.parseFloat(amount));
                    else item.getAccount()
                             .resetAmount();

                    break;
                case bottomFiller:
                    throw new RuntimeException("Should not happen");
            }
        }
        finally {
            syncingData = false;
        }
    }
    private void pickTransactionDate() {
        DatePickerFragment picker = new DatePickerFragment();
        picker.setOnDatePickedListener(this);
        picker.show(((NewTransactionActivity) tvDate.getContext()).getSupportFragmentManager(),
                "datePicker");
    }
    /**
     * setData
     *
     * @param item updates the UI elements with the data from the model item
     */
    public void setData(NewTransactionModel.Item item) {
        beginUpdates();
        try {
            if (this.item != null && !this.item.equals(item)) {
                this.item.stopObservingDate(dateObserver);
                this.item.stopObservingDescription(descriptionObserver);
                this.item.stopObservingAmountHint(hintObserver);
                this.item.getModel()
                         .stopObservingFocusedItem(focusedAccountObserver);
                this.item.getModel()
                         .stopObservingAccountCount(accountCountObserver);

                this.item = null;
            }

            switch (item.getType()) {
                case generalData:
                    tvDate.setText(item.getFormattedDate());
                    tvDescription.setText(item.getDescription());
                    lHead.setVisibility(View.VISIBLE);
                    lAccount.setVisibility(View.GONE);
                    lPadding.setVisibility(View.GONE);
                    break;
                case transactionRow:
                    LedgerTransactionAccount acc = item.getAccount();
                    tvAccount.setText(acc.getAccountName());
                    tvAmount.setText(
                            acc.isAmountSet() ? String.format(Locale.US, "%1.2f", acc.getAmount())
                                              : "");
                    lHead.setVisibility(View.GONE);
                    lAccount.setVisibility(View.VISIBLE);
                    lPadding.setVisibility(View.GONE);
                    break;
                case bottomFiller:
                    lHead.setVisibility(View.GONE);
                    lAccount.setVisibility(View.GONE);
                    lPadding.setVisibility(View.VISIBLE);
                    break;
            }

            if (this.item == null) { // was null or has changed
                this.item = item;
                final NewTransactionActivity activity =
                        (NewTransactionActivity) tvDescription.getContext();
                item.observeDate(activity, dateObserver);
                item.observeDescription(activity, descriptionObserver);
                item.observeAmountHint(activity, hintObserver);
                item.getModel()
                    .observeFocusedItem(activity, focusedAccountObserver);
                item.getModel()
                    .observeAccountCount(activity, accountCountObserver);
            }
        }
        finally {
            endUpdates();
        }
    }
    @Override
    public void onDatePicked(int year, int month, int day) {
        final Calendar c = GregorianCalendar.getInstance();
        c.set(year, month, day);
        item.setDate(c.getTime());
        boolean tookFocus = tvDescription.requestFocus();
        if (tookFocus) {
            // make the keyboard appear
            InputMethodManager imm = (InputMethodManager) tvDate.getContext()
                                                                .getSystemService(
                                                                        Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
        }
    }
    @Override
    public void descriptionSelected(String description) {
        tvAccount.setText(description);
        tvAmount.requestFocus(View.FOCUS_FORWARD);
    }
}
