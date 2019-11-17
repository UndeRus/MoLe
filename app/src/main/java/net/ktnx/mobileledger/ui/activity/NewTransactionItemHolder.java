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

import android.annotation.SuppressLint;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.view.View;
import android.view.inputmethod.EditorInfo;
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
import net.ktnx.mobileledger.utils.Misc;

import java.text.DecimalFormatSymbols;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

class NewTransactionItemHolder extends RecyclerView.ViewHolder
        implements DatePickerFragment.DatePickedListener, DescriptionSelectedCallback {
    private final String decimalSeparator;
    private final String decimalDot;
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
    private Observer<Boolean> editableObserver;
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
            if (hasFocus)
                pickTransactionDate();
        });
        tvDate.setOnClickListener(v -> pickTransactionDate());

        mProfile = Data.profile.getValue();
        if (mProfile == null)
            throw new AssertionError();

        MLDB.hookAutocompletionAdapter(tvDescription.getContext(), tvDescription,
                MLDB.DESCRIPTION_HISTORY_TABLE, "description", false, adapter, mProfile);
        MLDB.hookAutocompletionAdapter(tvAccount.getContext(), tvAccount, MLDB.ACCOUNTS_TABLE,
                "name", true, this, mProfile);

        // FIXME: react on configuration (locale) changes
        decimalSeparator = String.valueOf(DecimalFormatSymbols.getInstance()
                                                              .getMonetaryDecimalSeparator());
        decimalDot = ".";

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
                if (inUpdate)
                    return;

                Logger.debug("textWatcher", "calling syncData()");
                syncData();
                Logger.debug("textWatcher",
                        "syncData() returned, checking if transaction is submittable");
                adapter.model.checkTransactionSubmittable(adapter);
                Logger.debug("textWatcher", "done");
            }
        };
        final TextWatcher amountWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
            @Override
            public void afterTextChanged(Editable s) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    // only one decimal separator is allowed
                    // plus and minus are allowed only at the beginning
                    String val = s.toString();
                    if (val.isEmpty())
                        tvAmount.setKeyListener(DigitsKeyListener.getInstance(
                                "0123456789+-" + decimalSeparator + decimalDot));
                    else if (val.contains(decimalSeparator) || val.contains(decimalDot))
                        tvAmount.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
                    else
                        tvAmount.setKeyListener(DigitsKeyListener.getInstance(
                                "0123456789" + decimalSeparator + decimalDot));

                    syncData();
                    adapter.model.checkTransactionSubmittable(adapter);
                }
            }
        };
        tvDescription.addTextChangedListener(tw);
        tvAccount.addTextChangedListener(tw);
        tvAmount.addTextChangedListener(amountWatcher);

        // FIXME: react on locale changes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            tvAmount.setKeyListener(DigitsKeyListener.getInstance(Locale.getDefault(), true, true));
        else
            tvAmount.setKeyListener(
                    DigitsKeyListener.getInstance("0123456789+-" + decimalSeparator + decimalDot));

        dateObserver = date -> {
            if (syncingData)
                return;
            syncingData = true;
            try {
                tvDate.setText(item.getFormattedDate());
            }
            finally {
                syncingData = false;
            }
        };
        descriptionObserver = description -> {
            if (syncingData)
                return;
            syncingData = true;
            try {
                tvDescription.setText(description);
            }
            finally {
                syncingData = false;
            }
        };
        hintObserver = hint -> {
            if (syncingData)
                return;
            syncingData = true;
            try {
                if (hint == null)
                    tvAmount.setHint(R.string.zero_amount);
                else
                    tvAmount.setHint(hint);
            }
            finally {
                syncingData = false;
            }
        };
        editableObserver = this::setEditable;
        focusedAccountObserver = index -> {
            if ((index != null) && index.equals(getAdapterPosition())) {
                switch (item.getType()) {
                    case generalData:
                        // bad idea - double pop-up, and not really necessary.
                        // the user can tap the input to get the calendar
                        //if (!tvDate.hasFocus()) tvDate.requestFocus();
                        boolean focused = tvDescription.requestFocus();
                        tvDescription.dismissDropDown();
                        if (focused)
                            Misc.showSoftKeyboard(
                                    (NewTransactionActivity) tvDescription.getContext());
                        break;
                    case transactionRow:
                        focused = tvAccount.requestFocus();
                        tvAccount.dismissDropDown();
                        if (focused)
                            Misc.showSoftKeyboard((NewTransactionActivity) tvAccount.getContext());

                        break;
                }
            }
        };
        accountCountObserver = count -> {
            final int adapterPosition = getAdapterPosition();
            final int layoutPosition = getLayoutPosition();
            Logger.debug("holder",
                    String.format(Locale.US, "count=%d; pos=%d, layoutPos=%d [%s]", count,
                            adapterPosition, layoutPosition, item.getType()
                                                                 .toString()
                                                                 .concat(item.getType() ==
                                                                         NewTransactionModel.ItemType.transactionRow
                                                                         ? String.format(Locale.US,
                                                                         "'%s'=%s",
                                                                         item.getAccount()
                                                                             .getAccountName(),
                                                                         item.getAccount()
                                                                             .isAmountSet()
                                                                         ? String.format(Locale.US,
                                                                                 "%.2f",
                                                                                 item.getAccount()
                                                                                     .getAmount())
                                                                         : "unset") : "")));
            if (adapterPosition == count)
                tvAmount.setImeOptions(EditorInfo.IME_ACTION_DONE);
            else
                tvAmount.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        };
    }
    private void setEditable(Boolean editable) {
        tvDate.setEnabled(editable);
        tvDescription.setEnabled(editable);
        tvAccount.setEnabled(editable);
        tvAmount.setEnabled(editable);
    }
    private void beginUpdates() {
        if (inUpdate)
            throw new RuntimeException("Already in update mode");
        inUpdate = true;
    }
    private void endUpdates() {
        if (!inUpdate)
            throw new RuntimeException("Not in update mode");
        inUpdate = false;
    }
    /**
     * syncData()
     * <p>
     * Stores the data from the UI elements into the model item
     */
    private void syncData() {
        if (item == null)
            return;

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

                    if (amount.isEmpty()) {
                        item.getAccount()
                            .resetAmount();
                    }
                    else {
                        try {
                            amount = amount.replace(decimalSeparator, decimalDot);
                            item.getAccount()
                                .setAmount(Float.parseFloat(amount));
                        }
                        catch (NumberFormatException e) {
                            Logger.debug("new-trans", String.format(
                                    "assuming amount is not set due to number format exception. " +
                                    "input was '%s'", amount));
                            item.getAccount()
                                .resetAmount();
                        }
                    }

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
    @SuppressLint("DefaultLocale")
    public void setData(NewTransactionModel.Item item) {
        beginUpdates();
        try {
            if (this.item != null && !this.item.equals(item)) {
                this.item.stopObservingDate(dateObserver);
                this.item.stopObservingDescription(descriptionObserver);
                this.item.stopObservingAmountHint(hintObserver);
                this.item.stopObservingEditableFlag(editableObserver);
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
                    setEditable(true);
                    break;
                case transactionRow:
                    LedgerTransactionAccount acc = item.getAccount();
                    tvAccount.setText(acc.getAccountName());
                    if (acc.isAmountSet()) {
                        tvAmount.setText(String.format("%1.2f", acc.getAmount()));
                    }
                    else {
                        tvAmount.setText("");
//                        tvAmount.setHint(R.string.zero_amount);
                    }
                    tvAmount.setHint(item.getAmountHint());
                    lHead.setVisibility(View.GONE);
                    lAccount.setVisibility(View.VISIBLE);
                    lPadding.setVisibility(View.GONE);
                    setEditable(true);
                    break;
                case bottomFiller:
                    lHead.setVisibility(View.GONE);
                    lAccount.setVisibility(View.GONE);
                    lPadding.setVisibility(View.VISIBLE);
                    setEditable(false);
                    break;
            }

            if (this.item == null) { // was null or has changed
                this.item = item;
                final NewTransactionActivity activity =
                        (NewTransactionActivity) tvDescription.getContext();
                item.observeDate(activity, dateObserver);
                item.observeDescription(activity, descriptionObserver);
                item.observeAmountHint(activity, hintObserver);
                item.observeEditableFlag(activity, editableObserver);
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
        boolean focused = tvDescription.requestFocus();
        if (focused)
            Misc.showSoftKeyboard((NewTransactionActivity) tvAccount.getContext());

    }
    @Override
    public void descriptionSelected(String description) {
        tvAccount.setText(description);
        tvAmount.requestFocus(View.FOCUS_FORWARD);
    }
}
