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

package net.ktnx.mobileledger.ui.activity;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.async.DescriptionSelectedCallback;
import net.ktnx.mobileledger.model.Currency;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerTransactionAccount;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
import net.ktnx.mobileledger.ui.CurrencySelectorFragment;
import net.ktnx.mobileledger.ui.DatePickerFragment;
import net.ktnx.mobileledger.ui.TextViewClearHelper;
import net.ktnx.mobileledger.utils.DimensionUtils;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.MLDB;
import net.ktnx.mobileledger.utils.Misc;
import net.ktnx.mobileledger.utils.SimpleDate;

import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

import static net.ktnx.mobileledger.ui.activity.NewTransactionModel.ItemType;

class NewTransactionItemHolder extends RecyclerView.ViewHolder
        implements DatePickerFragment.DatePickedListener, DescriptionSelectedCallback {
    private final String decimalDot;
    private final TextView tvCurrency;
    private final Observer<Boolean> showCommentsObserver;
    private final TextView tvTransactionComment;
    private final TextView tvDate;
    private final AutoCompleteTextView tvDescription;
    private final TextView tvDummy;
    private final AutoCompleteTextView tvAccount;
    private final TextView tvComment;
    private final EditText tvAmount;
    private final ViewGroup lHead;
    private final ViewGroup lAccount;
    private final FrameLayout lPadding;
    private final MobileLedgerProfile mProfile;
    private final Observer<SimpleDate> dateObserver;
    private final Observer<String> descriptionObserver;
    private final Observer<String> transactionCommentObserver;
    private final Observer<String> hintObserver;
    private final Observer<Integer> focusedAccountObserver;
    private final Observer<Integer> accountCountObserver;
    private final Observer<Boolean> editableObserver;
    private final Observer<Currency.Position> currencyPositionObserver;
    private final Observer<Boolean> currencyGapObserver;
    private final Observer<Locale> localeObserver;
    private final Observer<Currency> currencyObserver;
    private final Observer<Boolean> showCurrencyObserver;
    private final Observer<String> commentObserver;
    private final Observer<Boolean> amountValidityObserver;
    private String decimalSeparator;
    private NewTransactionModel.Item item;
    private Date date;
    private boolean inUpdate = false;
    private boolean syncingData = false;
    //TODO multiple amounts with different currencies per posting
    NewTransactionItemHolder(@NonNull View itemView, NewTransactionItemsAdapter adapter) {
        super(itemView);
        lAccount = itemView.findViewById(R.id.ntr_account);
        tvAccount = lAccount.findViewById(R.id.account_row_acc_name);
        tvComment = lAccount.findViewById(R.id.comment);
        tvTransactionComment = itemView.findViewById(R.id.transaction_comment);
        new TextViewClearHelper().attachToTextView((EditText) tvComment);
        tvAmount = itemView.findViewById(R.id.account_row_acc_amounts);
        tvCurrency = itemView.findViewById(R.id.currency);
        tvDate = itemView.findViewById(R.id.new_transaction_date);
        tvDescription = itemView.findViewById(R.id.new_transaction_description);
        tvDummy = itemView.findViewById(R.id.dummy_text);
        lHead = itemView.findViewById(R.id.ntr_data);
        lPadding = itemView.findViewById(R.id.ntr_padding);
        final View commentLayout = itemView.findViewById(R.id.comment_layout);
        final View transactionCommentLayout =
                itemView.findViewById(R.id.transaction_comment_layout);

        tvDescription.setNextFocusForwardId(View.NO_ID);
        tvAccount.setNextFocusForwardId(View.NO_ID);
        tvAmount.setNextFocusForwardId(View.NO_ID); // magic!

        tvDate.setOnClickListener(v -> pickTransactionDate());

        lAccount.findViewById(R.id.comment_button)
                .setOnClickListener(v -> {
                    tvComment.setVisibility(View.VISIBLE);
                    tvComment.requestFocus();
                });

        transactionCommentLayout.findViewById(R.id.comment_button)
                                .setOnClickListener(v -> {
                                    tvTransactionComment.setVisibility(View.VISIBLE);
                                    tvTransactionComment.requestFocus();
                                });

        mProfile = Data.getProfile();

        View.OnFocusChangeListener focusMonitor = (v, hasFocus) -> {
            final int id = v.getId();
            if (hasFocus) {
                boolean wasSyncing = syncingData;
                syncingData = true;
                try {
                    final int pos = getAdapterPosition();
                    adapter.updateFocusedItem(pos);
                    switch (id) {
                        case R.id.account_row_acc_name:
                            adapter.noteFocusIsOnAccount(pos);
                            break;
                        case R.id.account_row_acc_amounts:
                            adapter.noteFocusIsOnAmount(pos);
                            break;
                        case R.id.comment:
                            adapter.noteFocusIsOnComment(pos);
                            break;
                        case R.id.transaction_comment:
                            adapter.noteFocusIsOnTransactionComment(pos);
                            break;
                        case R.id.new_transaction_description:
                            adapter.noteFocusIsOnDescription(pos);
                            break;
                    }
                }
                finally {
                    syncingData = wasSyncing;
                }
            }

            if (id == R.id.comment) {
                commentFocusChanged(tvComment, hasFocus);
            }
            else if (id == R.id.transaction_comment) {
                commentFocusChanged(tvTransactionComment, hasFocus);
            }
        };

        tvDescription.setOnFocusChangeListener(focusMonitor);
        tvAccount.setOnFocusChangeListener(focusMonitor);
        tvAmount.setOnFocusChangeListener(focusMonitor);
        tvComment.setOnFocusChangeListener(focusMonitor);
        tvTransactionComment.setOnFocusChangeListener(focusMonitor);

        MLDB.hookAutocompletionAdapter(tvDescription.getContext(), tvDescription,
                MLDB.DESCRIPTION_HISTORY_TABLE, "description", false, adapter, mProfile);
        MLDB.hookAutocompletionAdapter(tvAccount.getContext(), tvAccount, MLDB.ACCOUNTS_TABLE,
                "name", true, this, mProfile);

        decimalSeparator = String.valueOf(DecimalFormatSymbols.getInstance()
                                                              .getMonetaryDecimalSeparator());
        localeObserver = locale -> decimalSeparator = String.valueOf(
                DecimalFormatSymbols.getInstance(locale)
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
                adapter.checkTransactionSubmittable();
                Logger.debug("textWatcher", "done");
            }
        };
        final TextWatcher amountWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                Logger.debug("num",
                        String.format(Locale.US, "beforeTextChanged: start=%d, count=%d, after=%d",
                                start, count, after));
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {

                if (syncData())
                    adapter.checkTransactionSubmittable();
            }
        };
        tvDescription.addTextChangedListener(tw);
        tvTransactionComment.addTextChangedListener(tw);
        tvAccount.addTextChangedListener(tw);
        tvComment.addTextChangedListener(tw);
        tvAmount.addTextChangedListener(amountWatcher);

        tvCurrency.setOnClickListener(v -> {
            CurrencySelectorFragment cpf = new CurrencySelectorFragment();
            cpf.showPositionAndPadding();
            cpf.setOnCurrencySelectedListener(c -> item.setCurrency(c));
            final AppCompatActivity activity = (AppCompatActivity) v.getContext();
            cpf.show(activity.getSupportFragmentManager(), "currency-selector");
        });

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
        transactionCommentObserver = transactionComment -> {
            final View focusedView = tvTransactionComment.findFocus();
            tvTransactionComment.setTypeface(null,
                    (focusedView == tvTransactionComment) ? Typeface.NORMAL : Typeface.ITALIC);
            tvTransactionComment.setVisibility(
                    ((focusedView != tvTransactionComment) && TextUtils.isEmpty(transactionComment))
                    ? View.INVISIBLE : View.VISIBLE);

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
        commentFocusChanged(tvTransactionComment, false);
        commentFocusChanged(tvComment, false);
        focusedAccountObserver = index -> {
            if ((index == null) || !index.equals(getAdapterPosition()) || itemView.hasFocus())
                return;

            switch (item.getType()) {
                case generalData:
                    // bad idea - double pop-up, and not really necessary.
                    // the user can tap the input to get the calendar
                    //if (!tvDate.hasFocus()) tvDate.requestFocus();
                    switch (item.getFocusedElement()) {
                        case TransactionComment:
                            tvTransactionComment.setVisibility(View.VISIBLE);
                            tvTransactionComment.requestFocus();
                            break;
                        case Description:
                            boolean focused = tvDescription.requestFocus();
//                            tvDescription.dismissDropDown();
                            if (focused)
                                Misc.showSoftKeyboard(
                                        (NewTransactionActivity) tvDescription.getContext());
                            break;
                    }
                    break;
                case transactionRow:
                    switch (item.getFocusedElement()) {
                        case Amount:
                            tvAmount.requestFocus();
                            break;
                        case Comment:
                            tvComment.setVisibility(View.VISIBLE);
                            tvComment.requestFocus();
                            break;
                        case Account:
                            boolean focused = tvAccount.requestFocus();
                            tvAccount.dismissDropDown();
                            if (focused)
                                Misc.showSoftKeyboard(
                                        (NewTransactionActivity) tvAccount.getContext());
                            break;
                    }

                    break;
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
                                                                         ItemType.transactionRow
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

        currencyObserver = currency -> {
            setCurrency(currency);
            adapter.checkTransactionSubmittable();
        };

        currencyGapObserver =
                hasGap -> updateCurrencyPositionAndPadding(Data.currencySymbolPosition.getValue(),
                        hasGap);

        currencyPositionObserver =
                position -> updateCurrencyPositionAndPadding(position, Data.currencyGap.getValue());

        showCurrencyObserver = showCurrency -> {
            if (showCurrency) {
                tvCurrency.setVisibility(View.VISIBLE);
                String defaultCommodity = mProfile.getDefaultCommodity();
                item.setCurrency(
                        (defaultCommodity == null) ? null : Currency.loadByName(defaultCommodity));
            }
            else {
                tvCurrency.setVisibility(View.GONE);
                item.setCurrency(null);
            }
        };

        commentObserver = comment -> {
            final View focusedView = tvComment.findFocus();
            tvComment.setTypeface(null,
                    (focusedView == tvComment) ? Typeface.NORMAL : Typeface.ITALIC);
            tvComment.setVisibility(
                    ((focusedView != tvComment) && TextUtils.isEmpty(comment)) ? View.INVISIBLE
                                                                               : View.VISIBLE);
        };

        showCommentsObserver = show -> {
            final View amountLayout = itemView.findViewById(R.id.amount_layout);
            ConstraintLayout.LayoutParams amountLayoutParams =
                    (ConstraintLayout.LayoutParams) amountLayout.getLayoutParams();
            ConstraintLayout.LayoutParams accountParams =
                    (ConstraintLayout.LayoutParams) tvAccount.getLayoutParams();
            if (show) {
                accountParams.endToStart = ConstraintLayout.LayoutParams.UNSET;
                accountParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;

                amountLayoutParams.topToTop = ConstraintLayout.LayoutParams.UNSET;
                amountLayoutParams.topToBottom = tvAccount.getId();

                commentLayout.setVisibility(View.VISIBLE);
            }
            else {
                accountParams.endToStart = amountLayout.getId();
                accountParams.endToEnd = ConstraintLayout.LayoutParams.UNSET;

                amountLayoutParams.topToBottom = ConstraintLayout.LayoutParams.UNSET;
                amountLayoutParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;

                commentLayout.setVisibility(View.GONE);
            }

            tvAccount.setLayoutParams(accountParams);
            amountLayout.setLayoutParams(amountLayoutParams);

            transactionCommentLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        };

        amountValidityObserver = valid -> {
            tvAmount.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    valid ? 0 : R.drawable.ic_error_outline_black_24dp, 0, 0, 0);
            tvAmount.setMinEms(valid ? 4 : 5);
        };
    }
    private void commentFocusChanged(TextView textView, boolean hasFocus) {
        @ColorInt int textColor;
        textColor = tvDummy.getTextColors()
                           .getDefaultColor();
        if (hasFocus) {
            textView.setTypeface(null, Typeface.NORMAL);
            textView.setHint(R.string.transaction_account_comment_hint);
        }
        else {
            int alpha = (textColor >> 24 & 0xff);
            alpha = 3 * alpha / 4;
            textColor = (alpha << 24) | (0x00ffffff & textColor);
            textView.setTypeface(null, Typeface.ITALIC);
            textView.setHint("");
            if (TextUtils.isEmpty(textView.getText())) {
                textView.setVisibility(View.INVISIBLE);
            }
        }
        textView.setTextColor(textColor);

    }
    private void updateCurrencyPositionAndPadding(Currency.Position position, boolean hasGap) {
        ConstraintLayout.LayoutParams amountLP =
                (ConstraintLayout.LayoutParams) tvAmount.getLayoutParams();
        ConstraintLayout.LayoutParams currencyLP =
                (ConstraintLayout.LayoutParams) tvCurrency.getLayoutParams();

        if (position == Currency.Position.before) {
            currencyLP.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            currencyLP.endToEnd = ConstraintLayout.LayoutParams.UNSET;

            amountLP.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            amountLP.endToStart = ConstraintLayout.LayoutParams.UNSET;
            amountLP.startToStart = ConstraintLayout.LayoutParams.UNSET;
            amountLP.startToEnd = tvCurrency.getId();

            tvCurrency.setGravity(Gravity.END);
        }
        else {
            currencyLP.startToStart = ConstraintLayout.LayoutParams.UNSET;
            currencyLP.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;

            amountLP.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            amountLP.startToEnd = ConstraintLayout.LayoutParams.UNSET;
            amountLP.endToEnd = ConstraintLayout.LayoutParams.UNSET;
            amountLP.endToStart = tvCurrency.getId();

            tvCurrency.setGravity(Gravity.START);
        }

        amountLP.resolveLayoutDirection(tvAmount.getLayoutDirection());
        currencyLP.resolveLayoutDirection(tvCurrency.getLayoutDirection());

        tvAmount.setLayoutParams(amountLP);
        tvCurrency.setLayoutParams(currencyLP);

        // distance between the amount and the currency symbol
        int gapSize = DimensionUtils.sp2px(tvCurrency.getContext(), 5);

        if (position == Currency.Position.before) {
            tvCurrency.setPaddingRelative(0, 0, hasGap ? gapSize : 0, 0);
        }
        else {
            tvCurrency.setPaddingRelative(hasGap ? gapSize : 0, 0, 0, 0);
        }
    }
    private void setCurrencyString(String currency) {
        @ColorInt int textColor = tvDummy.getTextColors()
                                         .getDefaultColor();
        if ((currency == null) || currency.isEmpty()) {
            tvCurrency.setText(R.string.currency_symbol);
            int alpha = (textColor >> 24) & 0xff;
            alpha = alpha * 3 / 4;
            tvCurrency.setTextColor((alpha << 24) | (0x00ffffff & textColor));
        }
        else {
            tvCurrency.setText(currency);
            tvCurrency.setTextColor(textColor);
        }
    }
    private void setCurrency(Currency currency) {
        setCurrencyString((currency == null) ? null : currency.getName());
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
     * Returns true if there were changes made that suggest transaction has to be
     * checked for being submittable
     */
    private boolean syncData() {
        if (item == null)
            return false;

        if (syncingData) {
            Logger.debug("new-trans", "skipping syncData() loop");
            return false;
        }

        syncingData = true;

        try {
            switch (item.getType()) {
                case generalData:
                    item.setDate(String.valueOf(tvDate.getText()));
                    item.setDescription(String.valueOf(tvDescription.getText()));
                    item.setTransactionComment(String.valueOf(tvTransactionComment.getText()));
                    break;
                case transactionRow:
                    final LedgerTransactionAccount account = item.getAccount();
                    account.setAccountName(String.valueOf(tvAccount.getText()));

                    item.setComment(String.valueOf(tvComment.getText()));

                    String amount = String.valueOf(tvAmount.getText());
                    amount = amount.trim();

                    if (amount.isEmpty()) {
                        account.resetAmount();
                        item.validateAmount();
                    }
                    else {
                        try {
                            amount = amount.replace(decimalSeparator, decimalDot);
                            account.setAmount(Float.parseFloat(amount));
                            item.validateAmount();
                        }
                        catch (NumberFormatException e) {
                            Logger.debug("new-trans", String.format(
                                    "assuming amount is not set due to number format exception. " +
                                    "input was '%s'", amount));
                            account.invalidateAmount();
                            item.invalidateAmount();
                        }
                        final String curr = String.valueOf(tvCurrency.getText());
                        if (curr.equals(tvCurrency.getContext()
                                                  .getResources()
                                                  .getString(R.string.currency_symbol)) ||
                            curr.isEmpty())
                            account.setCurrency(null);
                        else
                            account.setCurrency(curr);
                    }

                    break;
                case bottomFiller:
                    throw new RuntimeException("Should not happen");
            }

            return true;
        }
        catch (ParseException e) {
            throw new RuntimeException("Should not happen", e);
        }
        finally {
            syncingData = false;
        }
    }
    private void pickTransactionDate() {
        DatePickerFragment picker = new DatePickerFragment();
        picker.setFutureDates(mProfile.getFutureDates());
        picker.setOnDatePickedListener(this);
        picker.setCurrentDateFromText(tvDate.getText());
        picker.show(((NewTransactionActivity) tvDate.getContext()).getSupportFragmentManager(),
                null);
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
                this.item.stopObservingTransactionComment(transactionCommentObserver);
                this.item.stopObservingAmountHint(hintObserver);
                this.item.stopObservingEditableFlag(editableObserver);
                this.item.getModel()
                         .stopObservingFocusedItem(focusedAccountObserver);
                this.item.getModel()
                         .stopObservingAccountCount(accountCountObserver);
                Data.currencySymbolPosition.removeObserver(currencyPositionObserver);
                Data.currencyGap.removeObserver(currencyGapObserver);
                Data.locale.removeObserver(localeObserver);
                this.item.stopObservingCurrency(currencyObserver);
                this.item.getModel().showCurrency.removeObserver(showCurrencyObserver);
                this.item.stopObservingComment(commentObserver);
                this.item.getModel().showComments.removeObserver(showCommentsObserver);
                this.item.stopObservingAmountValidity(amountValidityObserver);

                this.item = null;
            }

            switch (item.getType()) {
                case generalData:
                    tvDate.setText(item.getFormattedDate());
                    tvDescription.setText(item.getDescription());
                    tvTransactionComment.setText(item.getTransactionComment());
                    lHead.setVisibility(View.VISIBLE);
                    lAccount.setVisibility(View.GONE);
                    lPadding.setVisibility(View.GONE);
                    setEditable(true);
                    break;
                case transactionRow:
                    LedgerTransactionAccount acc = item.getAccount();
                    tvAccount.setText(acc.getAccountName());
                    tvComment.setText(acc.getComment());
                    if (acc.isAmountSet()) {
                        tvAmount.setText(String.format("%1.2f", acc.getAmount()));
                    }
                    else {
                        tvAmount.setText("");
//                        tvAmount.setHint(R.string.zero_amount);
                    }
                    tvAmount.setHint(item.getAmountHint());
                    setCurrencyString(acc.getCurrency());
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

                if (!item.isBottomFiller()) {
                    item.observeEditableFlag(activity, editableObserver);
                    item.getModel()
                        .observeFocusedItem(activity, focusedAccountObserver);
                    item.getModel()
                        .observeShowComments(activity, showCommentsObserver);
                }
                switch (item.getType()) {
                    case generalData:
                        item.observeDate(activity, dateObserver);
                        item.observeDescription(activity, descriptionObserver);
                        item.observeTransactionComment(activity, transactionCommentObserver);
                        break;
                    case transactionRow:
                        item.observeAmountHint(activity, hintObserver);
                        Data.currencySymbolPosition.observe(activity, currencyPositionObserver);
                        Data.currencyGap.observe(activity, currencyGapObserver);
                        Data.locale.observe(activity, localeObserver);
                        item.observeCurrency(activity, currencyObserver);
                        item.getModel().showCurrency.observe(activity, showCurrencyObserver);
                        item.observeComment(activity, commentObserver);
                        item.getModel()
                            .observeAccountCount(activity, accountCountObserver);
                        item.observeAmountValidity(activity, amountValidityObserver);
                        break;
                }
            }
        }
        finally {
            endUpdates();
        }
    }
    @Override
    public void onDatePicked(int year, int month, int day) {
        item.setDate(new SimpleDate(year, month + 1, day));
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
