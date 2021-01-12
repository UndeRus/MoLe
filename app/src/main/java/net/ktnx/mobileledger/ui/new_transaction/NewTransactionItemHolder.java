/*
 * Copyright © 2021 Damyan Ivanov.
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

package net.ktnx.mobileledger.ui.new_transaction;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.async.DescriptionSelectedCallback;
import net.ktnx.mobileledger.databinding.NewTransactionRowBinding;
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

import static net.ktnx.mobileledger.ui.new_transaction.NewTransactionModel.ItemType;

class NewTransactionItemHolder extends RecyclerView.ViewHolder
        implements DatePickerFragment.DatePickedListener, DescriptionSelectedCallback {
    private final String decimalDot;
    private final Observer<Boolean> showCommentsObserver;
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
    private final NewTransactionRowBinding b;
    private String decimalSeparator;
    private NewTransactionModel.Item item;
    private Date date;
    private boolean inUpdate = false;
    private boolean syncingData = false;
    //TODO multiple amounts with different currencies per posting
    NewTransactionItemHolder(@NonNull NewTransactionRowBinding b,
                             NewTransactionItemsAdapter adapter) {
        super(b.getRoot());
        this.b = b;
        new TextViewClearHelper().attachToTextView(b.comment);

        b.newTransactionDescription.setNextFocusForwardId(View.NO_ID);
        b.accountRowAccName.setNextFocusForwardId(View.NO_ID);
        b.accountRowAccAmounts.setNextFocusForwardId(View.NO_ID); // magic!

        b.newTransactionDate.setOnClickListener(v -> pickTransactionDate());

        b.accountCommentButton.setOnClickListener(v -> {
            b.comment.setVisibility(View.VISIBLE);
            b.comment.requestFocus();
        });

        b.transactionCommentButton.setOnClickListener(v -> {
            b.transactionComment.setVisibility(View.VISIBLE);
            b.transactionComment.requestFocus();
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
                    if (id == R.id.account_row_acc_name) {
                        adapter.noteFocusIsOnAccount(pos);
                    }
                    else if (id == R.id.account_row_acc_amounts) {
                        adapter.noteFocusIsOnAmount(pos);
                    }
                    else if (id == R.id.comment) {
                        adapter.noteFocusIsOnComment(pos);
                    }
                    else if (id == R.id.transaction_comment) {
                        adapter.noteFocusIsOnTransactionComment(pos);
                    }
                    else if (id == R.id.new_transaction_description) {
                        adapter.noteFocusIsOnDescription(pos);
                    }
                }
                finally {
                    syncingData = wasSyncing;
                }
            }

            if (id == R.id.comment) {
                commentFocusChanged(b.comment, hasFocus);
            }
            else if (id == R.id.transaction_comment) {
                commentFocusChanged(b.transactionComment, hasFocus);
            }
        };

        b.newTransactionDescription.setOnFocusChangeListener(focusMonitor);
        b.accountRowAccName.setOnFocusChangeListener(focusMonitor);
        b.accountRowAccAmounts.setOnFocusChangeListener(focusMonitor);
        b.comment.setOnFocusChangeListener(focusMonitor);
        b.transactionComment.setOnFocusChangeListener(focusMonitor);

        MLDB.hookAutocompletionAdapter(b.getRoot()
                                        .getContext(), b.newTransactionDescription,
                MLDB.DESCRIPTION_HISTORY_TABLE, "description", false, adapter, mProfile);
        MLDB.hookAutocompletionAdapter(b.getRoot()
                                        .getContext(), b.accountRowAccName, MLDB.ACCOUNTS_TABLE,
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
        b.newTransactionDescription.addTextChangedListener(tw);
        b.transactionComment.addTextChangedListener(tw);
        b.accountRowAccName.addTextChangedListener(tw);
        b.comment.addTextChangedListener(tw);
        b.accountRowAccAmounts.addTextChangedListener(amountWatcher);

        b.currencyButton.setOnClickListener(v -> {
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
                b.newTransactionDate.setText(item.getFormattedDate());
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
                b.newTransactionDescription.setText(description);
            }
            finally {
                syncingData = false;
            }
        };
        transactionCommentObserver = transactionComment -> {
            final View focusedView = b.transactionComment.findFocus();
            b.transactionComment.setTypeface(null,
                    (focusedView == b.transactionComment) ? Typeface.NORMAL : Typeface.ITALIC);
            b.transactionComment.setVisibility(
                    ((focusedView != b.transactionComment) && TextUtils.isEmpty(transactionComment))
                    ? View.INVISIBLE : View.VISIBLE);

        };
        hintObserver = hint -> {
            if (syncingData)
                return;
            syncingData = true;
            try {
                if (hint == null)
                    b.accountRowAccAmounts.setHint(R.string.zero_amount);
                else
                    b.accountRowAccAmounts.setHint(hint);
            }
            finally {
                syncingData = false;
            }
        };
        editableObserver = this::setEditable;
        commentFocusChanged(b.transactionComment, false);
        commentFocusChanged(b.comment, false);
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
                            b.transactionComment.setVisibility(View.VISIBLE);
                            b.transactionComment.requestFocus();
                            break;
                        case Description:
                            boolean focused = b.newTransactionDescription.requestFocus();
//                            tvDescription.dismissDropDown();
                            if (focused)
                                Misc.showSoftKeyboard((NewTransactionActivity) b.getRoot()
                                                                                .getContext());
                            break;
                    }
                    break;
                case transactionRow:
                    switch (item.getFocusedElement()) {
                        case Amount:
                            b.accountRowAccAmounts.requestFocus();
                            break;
                        case Comment:
                            b.comment.setVisibility(View.VISIBLE);
                            b.comment.requestFocus();
                            break;
                        case Account:
                            boolean focused = b.accountRowAccName.requestFocus();
                            b.accountRowAccName.dismissDropDown();
                            if (focused)
                                Misc.showSoftKeyboard((NewTransactionActivity) b.getRoot()
                                                                                .getContext());
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
                b.accountRowAccAmounts.setImeOptions(EditorInfo.IME_ACTION_DONE);
            else
                b.accountRowAccAmounts.setImeOptions(EditorInfo.IME_ACTION_NEXT);
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
                b.currency.setVisibility(View.VISIBLE);
                b.currencyButton.setVisibility(View.VISIBLE);
                String defaultCommodity = mProfile.getDefaultCommodity();
                item.setCurrency(
                        (defaultCommodity == null) ? null : Currency.loadByName(defaultCommodity));
            }
            else {
                b.currency.setVisibility(View.GONE);
                b.currencyButton.setVisibility(View.GONE);
                item.setCurrency(null);
            }
        };

        commentObserver = comment -> {
            final View focusedView = b.comment.findFocus();
            b.comment.setTypeface(null,
                    (focusedView == b.comment) ? Typeface.NORMAL : Typeface.ITALIC);
            b.comment.setVisibility(
                    ((focusedView != b.comment) && TextUtils.isEmpty(comment)) ? View.INVISIBLE
                                                                               : View.VISIBLE);
        };

        showCommentsObserver = show -> {
            ConstraintLayout.LayoutParams amountLayoutParams =
                    (ConstraintLayout.LayoutParams) b.amountLayout.getLayoutParams();
            ConstraintLayout.LayoutParams accountParams =
                    (ConstraintLayout.LayoutParams) b.accountRowAccName.getLayoutParams();
            if (show) {
                accountParams.endToStart = ConstraintLayout.LayoutParams.UNSET;
                accountParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;

                amountLayoutParams.topToTop = ConstraintLayout.LayoutParams.UNSET;
                amountLayoutParams.topToBottom = b.accountRowAccName.getId();

                b.commentLayout.setVisibility(View.VISIBLE);
            }
            else {
                accountParams.endToStart = b.amountLayout.getId();
                accountParams.endToEnd = ConstraintLayout.LayoutParams.UNSET;

                amountLayoutParams.topToBottom = ConstraintLayout.LayoutParams.UNSET;
                amountLayoutParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;

                b.commentLayout.setVisibility(View.GONE);
            }

            b.accountRowAccName.setLayoutParams(accountParams);
            b.amountLayout.setLayoutParams(amountLayoutParams);

            b.transactionCommentLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        };

        amountValidityObserver = valid -> {
            b.accountRowAccAmounts.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    valid ? 0 : R.drawable.ic_error_outline_black_24dp, 0, 0, 0);
            b.accountRowAccAmounts.setMinEms(valid ? 4 : 5);
        };
    }
    private void commentFocusChanged(TextView textView, boolean hasFocus) {
        @ColorInt int textColor;
        textColor = b.dummyText.getTextColors()
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
                (ConstraintLayout.LayoutParams) b.accountRowAccAmounts.getLayoutParams();
        ConstraintLayout.LayoutParams currencyLP =
                (ConstraintLayout.LayoutParams) b.currency.getLayoutParams();

        if (position == Currency.Position.before) {
            currencyLP.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            currencyLP.endToEnd = ConstraintLayout.LayoutParams.UNSET;

            amountLP.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            amountLP.endToStart = ConstraintLayout.LayoutParams.UNSET;
            amountLP.startToStart = ConstraintLayout.LayoutParams.UNSET;
            amountLP.startToEnd = b.currency.getId();

            b.currency.setGravity(Gravity.END);
        }
        else {
            currencyLP.startToStart = ConstraintLayout.LayoutParams.UNSET;
            currencyLP.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;

            amountLP.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            amountLP.startToEnd = ConstraintLayout.LayoutParams.UNSET;
            amountLP.endToEnd = ConstraintLayout.LayoutParams.UNSET;
            amountLP.endToStart = b.currency.getId();

            b.currency.setGravity(Gravity.START);
        }

        amountLP.resolveLayoutDirection(b.accountRowAccAmounts.getLayoutDirection());
        currencyLP.resolveLayoutDirection(b.currency.getLayoutDirection());

        b.accountRowAccAmounts.setLayoutParams(amountLP);
        b.currency.setLayoutParams(currencyLP);

        // distance between the amount and the currency symbol
        int gapSize = DimensionUtils.sp2px(b.currency.getContext(), 5);

        if (position == Currency.Position.before) {
            b.currency.setPaddingRelative(0, 0, hasGap ? gapSize : 0, 0);
        }
        else {
            b.currency.setPaddingRelative(hasGap ? gapSize : 0, 0, 0, 0);
        }
    }
    private void setCurrencyString(String currency) {
        @ColorInt int textColor = b.dummyText.getTextColors()
                                             .getDefaultColor();
        if ((currency == null) || currency.isEmpty()) {
            b.currency.setText(R.string.currency_symbol);
            int alpha = (textColor >> 24) & 0xff;
            alpha = alpha * 3 / 4;
            b.currency.setTextColor((alpha << 24) | (0x00ffffff & textColor));
        }
        else {
            b.currency.setText(currency);
            b.currency.setTextColor(textColor);
        }
    }
    private void setCurrency(Currency currency) {
        setCurrencyString((currency == null) ? null : currency.getName());
    }
    private void setEditable(Boolean editable) {
        b.newTransactionDate.setEnabled(editable);
        b.newTransactionDescription.setEnabled(editable);
        b.accountRowAccName.setEnabled(editable);
        b.accountRowAccAmounts.setEnabled(editable);
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
                    item.setDate(String.valueOf(b.newTransactionDate.getText()));
                    item.setDescription(String.valueOf(b.newTransactionDescription.getText()));
                    item.setTransactionComment(String.valueOf(b.transactionComment.getText()));
                    break;
                case transactionRow:
                    final LedgerTransactionAccount account = item.getAccount();
                    account.setAccountName(String.valueOf(b.accountRowAccName.getText()));

                    item.setComment(String.valueOf(b.comment.getText()));

                    String amount = String.valueOf(b.accountRowAccAmounts.getText());
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
                        final String curr = String.valueOf(b.currency.getText());
                        if (curr.equals(b.currency.getContext()
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
        picker.setCurrentDateFromText(b.newTransactionDate.getText());
        picker.show(((NewTransactionActivity) b.getRoot()
                                               .getContext()).getSupportFragmentManager(), null);
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
                    b.newTransactionDate.setText(item.getFormattedDate());
                    b.newTransactionDescription.setText(item.getDescription());
                    b.transactionComment.setText(item.getTransactionComment());
                    b.ntrData.setVisibility(View.VISIBLE);
                    b.ntrAccount.setVisibility(View.GONE);
                    b.ntrPadding.setVisibility(View.GONE);
                    setEditable(true);
                    break;
                case transactionRow:
                    LedgerTransactionAccount acc = item.getAccount();
                    b.accountRowAccName.setText(acc.getAccountName());
                    b.comment.setText(acc.getComment());
                    if (acc.isAmountSet()) {
                        b.accountRowAccAmounts.setText(String.format("%1.2f", acc.getAmount()));
                    }
                    else {
                        b.accountRowAccAmounts.setText("");
//                        tvAmount.setHint(R.string.zero_amount);
                    }
                    b.accountRowAccAmounts.setHint(item.getAmountHint());
                    setCurrencyString(acc.getCurrency());
                    b.ntrData.setVisibility(View.GONE);
                    b.ntrAccount.setVisibility(View.VISIBLE);
                    b.ntrPadding.setVisibility(View.GONE);
                    setEditable(true);
                    break;
                case bottomFiller:
                    b.ntrData.setVisibility(View.GONE);
                    b.ntrAccount.setVisibility(View.GONE);
                    b.ntrPadding.setVisibility(View.VISIBLE);
                    setEditable(false);
                    break;
            }
            if (this.item == null) { // was null or has changed
                this.item = item;
                final NewTransactionActivity activity = (NewTransactionActivity) b.getRoot()
                                                                                  .getContext();

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
        boolean focused = b.newTransactionDescription.requestFocus();
        if (focused)
            Misc.showSoftKeyboard((NewTransactionActivity) b.getRoot()
                                                            .getContext());

    }
    @Override
    public void descriptionSelected(String description) {
        b.accountRowAccName.setText(description);
        b.accountRowAccAmounts.requestFocus(View.FOCUS_FORWARD);
    }
}
