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
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.databinding.NewTransactionAccountRowBinding;
import net.ktnx.mobileledger.db.AccountAutocompleteAdapter;
import net.ktnx.mobileledger.model.Currency;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.ui.CurrencySelectorFragment;
import net.ktnx.mobileledger.ui.TextViewClearHelper;
import net.ktnx.mobileledger.utils.DimensionUtils;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.Misc;

import java.text.DecimalFormatSymbols;

class NewTransactionAccountRowItemHolder extends NewTransactionItemViewHolder {
    private final String decimalDot = ".";
    private final NewTransactionAccountRowBinding b;
    private boolean ignoreFocusChanges = false;
    private String decimalSeparator;
    private boolean inUpdate = false;
    private boolean syncingData = false;
    //TODO multiple amounts with different currencies per posting?
    NewTransactionAccountRowItemHolder(@NonNull NewTransactionAccountRowBinding b,
                                       NewTransactionItemsAdapter adapter) {
        super(b.getRoot());
        this.b = b;
        new TextViewClearHelper().attachToTextView(b.comment);

        b.accountRowAccName.setNextFocusForwardId(View.NO_ID);
        b.accountRowAccAmounts.setNextFocusForwardId(View.NO_ID); // magic!

        b.accountCommentButton.setOnClickListener(v -> {
            b.comment.setVisibility(View.VISIBLE);
            b.comment.requestFocus();
        });

        @SuppressLint("DefaultLocale") View.OnFocusChangeListener focusMonitor = (v, hasFocus) -> {
            final int id = v.getId();
            if (hasFocus) {
                boolean wasSyncing = syncingData;
                syncingData = true;
                try {
                    final int pos = getBindingAdapterPosition();
                    if (id == R.id.account_row_acc_name) {
                        adapter.noteFocusIsOnAccount(pos);
                    }
                    else if (id == R.id.account_row_acc_amounts) {
                        adapter.noteFocusIsOnAmount(pos);
                    }
                    else if (id == R.id.comment) {
                        adapter.noteFocusIsOnComment(pos);
                    }
                    else
                        throw new IllegalStateException("Where is the focus? " + id);
                }
                finally {
                    syncingData = wasSyncing;
                }
            }
            else {  // lost focus
                if (id == R.id.account_row_acc_amounts) {
                    try {
                        String input = String.valueOf(b.accountRowAccAmounts.getText());
                        input = input.replace(decimalSeparator, decimalDot);
                        final String newText = String.format("%4.2f", Float.parseFloat(input));
                        if (!newText.equals(input)) {
                            boolean wasSyncingData = syncingData;
                            syncingData = true;
                            try {
                                b.accountRowAccAmounts.setText(newText);
                            }
                            finally {
                                syncingData = wasSyncingData;
                            }
                        }
                    }
                    catch (NumberFormatException ex) {
                        // ignored
                    }
                }
            }

            if (id == R.id.comment) {
                commentFocusChanged(b.comment, hasFocus);
            }
        };

        b.accountRowAccName.setOnFocusChangeListener(focusMonitor);
        b.accountRowAccAmounts.setOnFocusChangeListener(focusMonitor);
        b.comment.setOnFocusChangeListener(focusMonitor);

        NewTransactionActivity activity = (NewTransactionActivity) b.getRoot()
                                                                    .getContext();

        b.accountRowAccName.setAdapter(new AccountAutocompleteAdapter(b.getRoot()
                                                                       .getContext(), mProfile));

        decimalSeparator = "";
        Data.locale.observe(activity, locale -> decimalSeparator = String.valueOf(
                DecimalFormatSymbols.getInstance(locale)
                                    .getMonetaryDecimalSeparator()));

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
                if (syncData()) {
                    Logger.debug("textWatcher",
                            "syncData() returned, checking if transaction is submittable");
                    adapter.model.checkTransactionSubmittable(null);
                }
                Logger.debug("textWatcher", "done");
            }
        };
        final TextWatcher amountWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                checkAmountValid(s.toString());

                if (syncData())
                    adapter.model.checkTransactionSubmittable(null);
            }
        };
        b.accountRowAccName.addTextChangedListener(tw);
        monitorComment(b.comment);
        b.accountRowAccAmounts.addTextChangedListener(amountWatcher);

        b.currencyButton.setOnClickListener(v -> {
            CurrencySelectorFragment cpf = new CurrencySelectorFragment();
            cpf.showPositionAndPadding();
            cpf.setOnCurrencySelectedListener(
                    c -> adapter.setItemCurrency(getBindingAdapterPosition(), c));
            cpf.show(activity.getSupportFragmentManager(), "currency-selector");
        });

        commentFocusChanged(b.comment, false);

        adapter.model.getFocusInfo()
                     .observe(activity, this::applyFocus);

        Data.currencyGap.observe(activity,
                hasGap -> updateCurrencyPositionAndPadding(Data.currencySymbolPosition.getValue(),
                        hasGap));

        Data.currencySymbolPosition.observe(activity,
                position -> updateCurrencyPositionAndPadding(position,
                        Data.currencyGap.getValue()));

        adapter.model.getShowCurrency()
                     .observe(activity, showCurrency -> {
                         if (showCurrency) {
                             b.currency.setVisibility(View.VISIBLE);
                             b.currencyButton.setVisibility(View.VISIBLE);
                             setCurrencyString(mProfile.getDefaultCommodity());
                         }
                         else {
                             b.currency.setVisibility(View.GONE);
                             b.currencyButton.setVisibility(View.GONE);
                             setCurrencyString(null);
                         }
                     });

        adapter.model.getShowComments()
                     .observe(activity, show -> b.commentLayout.setVisibility(
                             show ? View.VISIBLE : View.GONE));
    }
    private void applyFocus(NewTransactionModel.FocusInfo focusInfo) {
        if (ignoreFocusChanges) {
            Logger.debug("new-trans", "Ignoring focus change");
            return;
        }
        ignoreFocusChanges = true;
        try {
            if (((focusInfo == null) || (focusInfo.element == null) ||
                 focusInfo.position != getBindingAdapterPosition()))
                return;

            final NewTransactionModel.Item item = getItem();
            if (item == null)
                return;

            NewTransactionModel.TransactionAccount acc = item.toTransactionAccount();
            switch (focusInfo.element) {
                case Amount:
                    b.accountRowAccAmounts.requestFocus();
                    break;
                case Comment:
                    b.comment.setVisibility(View.VISIBLE);
                    b.comment.requestFocus();
                    break;
                case Account:
                    boolean focused = b.accountRowAccName.requestFocus();
//                                         b.accountRowAccName.dismissDropDown();
                    if (focused)
                        Misc.showSoftKeyboard((NewTransactionActivity) b.getRoot()
                                                                        .getContext());
                    break;
            }
        }
        finally {
            ignoreFocusChanges = false;
        }
    }
    public void checkAmountValid(String s) {
        boolean valid = true;
        try {
            if (s.length() > 0) {
                float ignored = Float.parseFloat(s.replace(decimalSeparator, decimalDot));
            }
        }
        catch (NumberFormatException ex) {
            valid = false;
        }

        displayAmountValidity(valid);
    }
    private void displayAmountValidity(boolean valid) {
        b.accountRowAccAmounts.setCompoundDrawablesRelativeWithIntrinsicBounds(
                valid ? 0 : R.drawable.ic_error_outline_black_24dp, 0, 0, 0);
        b.accountRowAccAmounts.setMinEms(valid ? 4 : 5);
    }
    private void monitorComment(EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
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
                styleComment(editText, s.toString());
                Logger.debug("textWatcher", "done");
            }
        });
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
        if (TextUtils.isEmpty(currency)) {
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
        if (syncingData) {
            Logger.debug("new-trans", "skipping syncData() loop");
            return false;
        }

        if (getBindingAdapterPosition() == RecyclerView.NO_POSITION) {
            // probably the row was swiped out
            Logger.debug("new-trans", "Ignoring request to suncData(): adapter position negative");
            return false;
        }

        final NewTransactionModel.Item item = getItem();
        if (item == null)
            return false;

        syncingData = true;

        boolean significantChange = false;

        try {
            NewTransactionModel.TransactionAccount acc = item.toTransactionAccount();

            // having account name is important
            final Editable incomingAccountName = b.accountRowAccName.getText();
            if (TextUtils.isEmpty(acc.getAccountName()) != TextUtils.isEmpty(incomingAccountName))
                significantChange = true;

            acc.setAccountName(String.valueOf(incomingAccountName));
            final int accNameSelEnd = b.accountRowAccName.getSelectionEnd();
            final int accNameSelStart = b.accountRowAccName.getSelectionStart();
            acc.setAccountNameCursorPosition(accNameSelEnd);

            acc.setComment(String.valueOf(b.comment.getText()));

            String amount = String.valueOf(b.accountRowAccAmounts.getText());
            amount = amount.trim();

            if (amount.isEmpty()) {
                if (acc.isAmountSet())
                    significantChange = true;
                acc.resetAmount();
                acc.setAmountValid(true);
            }
            else {
                try {
                    amount = amount.replace(decimalSeparator, decimalDot);
                    final float parsedAmount = Float.parseFloat(amount);
                    if (!acc.isAmountSet() || !Misc.equalFloats(parsedAmount, acc.getAmount()))
                        significantChange = true;
                    acc.setAmount(parsedAmount);
                    acc.setAmountValid(true);
                }
                catch (NumberFormatException e) {
                    Logger.debug("new-trans", String.format(
                            "assuming amount is not set due to number format exception. " +
                            "input was '%s'", amount));
                    if (acc.isAmountValid())
                        significantChange = true;
                    acc.setAmountValid(false);
                }
                final String curr = String.valueOf(b.currency.getText());
                final String currValue;
                if (curr.equals(b.currency.getContext()
                                          .getResources()
                                          .getString(R.string.currency_symbol)) || curr.isEmpty())
                    currValue = null;
                else
                    currValue = curr;

                if (!significantChange && !Misc.equalStrings(acc.getCurrency(), currValue))
                    significantChange = true;
                acc.setCurrency(currValue);
            }

            return significantChange;
        }
        finally {
            syncingData = false;
        }
    }
    /**
     * bind
     *
     * @param item updates the UI elements with the data from the model item
     */
    @SuppressLint("DefaultLocale")
    public void bind(@NonNull NewTransactionModel.Item item) {
        beginUpdates();
        try {
            syncingData = true;
            try {
                NewTransactionModel.TransactionAccount acc = item.toTransactionAccount();

                final String incomingAccountName = acc.getAccountName();
                final String presentAccountName = String.valueOf(b.accountRowAccName.getText());
                if (!Misc.equalStrings(incomingAccountName, presentAccountName)) {
                    Logger.debug("bind",
                            String.format("Setting account name from '%s' to '%s' (| @ %d)",
                                    presentAccountName, incomingAccountName,
                                    acc.getAccountNameCursorPosition()));
                    // avoid triggering completion pop-up
                    AccountAutocompleteAdapter a =
                            (AccountAutocompleteAdapter) b.accountRowAccName.getAdapter();
                    try {
                        b.accountRowAccName.setAdapter(null);
                        b.accountRowAccName.setText(incomingAccountName);
                        b.accountRowAccName.setSelection(acc.getAccountNameCursorPosition());
                    }
                    finally {
                        b.accountRowAccName.setAdapter(a);
                    }
                }

                final String amountHint = acc.getAmountHint();
                if (amountHint == null) {
                    b.accountRowAccAmounts.setHint(R.string.zero_amount);
                }
                else {
                    b.accountRowAccAmounts.setHint(amountHint);
                }

                b.accountRowAccAmounts.setImeOptions(
                        acc.isLast() ? EditorInfo.IME_ACTION_DONE : EditorInfo.IME_ACTION_NEXT);

                setCurrencyString(acc.getCurrency());
                b.accountRowAccAmounts.setText(
                        acc.isAmountSet() ? String.format("%4.2f", acc.getAmount()) : null);
                displayAmountValidity(true);

                final String comment = acc.getComment();
                b.comment.setText(comment);
                styleComment(b.comment, comment);

                setEditable(true);

                NewTransactionItemsAdapter adapter =
                        (NewTransactionItemsAdapter) getBindingAdapter();
                if (adapter != null)
                    applyFocus(adapter.model.getFocusInfo()
                                            .getValue());
            }
            finally {
                syncingData = false;
            }
        }
        finally {
            endUpdates();
        }
    }
    private void styleComment(EditText editText, String comment) {
        final View focusedView = editText.findFocus();
        editText.setTypeface(null, (focusedView == editText) ? Typeface.NORMAL : Typeface.ITALIC);
        editText.setVisibility(
                ((focusedView != editText) && TextUtils.isEmpty(comment)) ? View.INVISIBLE
                                                                          : View.VISIBLE);
    }
}
