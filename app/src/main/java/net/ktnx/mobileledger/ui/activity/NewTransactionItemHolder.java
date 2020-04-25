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
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

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
import net.ktnx.mobileledger.utils.Colors;
import net.ktnx.mobileledger.utils.DimensionUtils;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.MLDB;
import net.ktnx.mobileledger.utils.Misc;

import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormatSymbols;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import static net.ktnx.mobileledger.ui.activity.NewTransactionModel.ItemType;

class NewTransactionItemHolder extends RecyclerView.ViewHolder
        implements DatePickerFragment.DatePickedListener, DescriptionSelectedCallback {
    private final String decimalSeparator;
    private final String decimalDot;
    private final TextView tvCurrency;
    private NewTransactionModel.Item item;
    private TextView tvDate;
    private AutoCompleteTextView tvDescription;
    private AutoCompleteTextView tvAccount;
    private TextView tvComment;
    private EditText tvAmount;
    private LinearLayout lHead;
    private ViewGroup lAccount;
    private FrameLayout lPadding;
    private MobileLedgerProfile mProfile;
    private Date date;
    private Observer<Date> dateObserver;
    private Observer<String> descriptionObserver;
    private Observer<String> hintObserver;
    private Observer<Integer> focusedAccountObserver;
    private Observer<Integer> accountCountObserver;
    private Observer<Boolean> editableObserver;
    private Observer<Boolean> commentVisibleObserver;
    private Observer<String> commentObserver;
    private Observer<Currency.Position> currencyPositionObserver;
    private Observer<Boolean> currencyGapObserver;
    private Observer<Locale> localeObserver;
    private Observer<Currency> currencyObserver;
    private Observer<Boolean> showCurrencyObserver;
    private boolean inUpdate = false;
    private boolean syncingData = false;
    private View commentButton;
    //TODO multiple amounts with different currencies per posting
    NewTransactionItemHolder(@NonNull View itemView, NewTransactionItemsAdapter adapter) {
        super(itemView);
        tvAccount = itemView.findViewById(R.id.account_row_acc_name);
        tvComment = itemView.findViewById(R.id.comment);
        new TextViewClearHelper().attachToTextView((EditText) tvComment);
        commentButton = itemView.findViewById(R.id.comment_button);
        tvAmount = itemView.findViewById(R.id.account_row_acc_amounts);
        tvCurrency = itemView.findViewById(R.id.currency);
        tvDate = itemView.findViewById(R.id.new_transaction_date);
        tvDescription = itemView.findViewById(R.id.new_transaction_description);
        lHead = itemView.findViewById(R.id.ntr_data);
        lAccount = itemView.findViewById(R.id.ntr_account);
        lPadding = itemView.findViewById(R.id.ntr_padding);

        tvDescription.setNextFocusForwardId(View.NO_ID);
        tvAccount.setNextFocusForwardId(View.NO_ID);
        tvAmount.setNextFocusForwardId(View.NO_ID); // magic!

        tvDate.setOnClickListener(v -> pickTransactionDate());

        mProfile = Data.profile.getValue();
        if (mProfile == null)
            throw new AssertionError();

        View.OnFocusChangeListener focusMonitor = (v, hasFocus) -> {
            if (hasFocus) {
                boolean wasSyncing = syncingData;
                syncingData = true;
                try {
                    final int pos = getAdapterPosition();
                    adapter.updateFocusedItem(pos);
                    switch (v.getId()) {
                        case R.id.account_row_acc_name:
                            adapter.noteFocusIsOnAccount(pos);
                            break;
                        case R.id.account_row_acc_amounts:
                            adapter.noteFocusIsOnAmount(pos);
                            break;
                        case R.id.comment:
                            adapter.noteFocusIsOnComment(pos);
                            break;
                    }
                }
                finally {
                    syncingData = wasSyncing;
                }
            }
        };

        tvDescription.setOnFocusChangeListener(focusMonitor);
        tvAccount.setOnFocusChangeListener(focusMonitor);
        tvAmount.setOnFocusChangeListener(focusMonitor);

        itemView.findViewById(R.id.comment_button)
                .setOnClickListener(v -> {
                    final int pos = getAdapterPosition();
                    adapter.toggleComment(pos);
                });
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
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    // only one decimal separator is allowed
                    // plus and minus are allowed only at the beginning
                    String allowed = "0123456789";
                    String val = s.toString();
                    Logger.debug("input", val);
                    if (val.isEmpty() || (tvAmount.getSelectionStart() == 0))
                        allowed += "-";
                    if (!val.contains(decimalSeparator) && !val.contains(decimalDot))
                        allowed += decimalSeparator + decimalDot;

                    tvAmount.setKeyListener(DigitsKeyListener.getInstance(allowed));
                }

                if (syncData())
                    adapter.checkTransactionSubmittable();
            }
        };
        tvDescription.addTextChangedListener(tw);
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            tvAmount.setKeyListener(
                    DigitsKeyListener.getInstance(Data.locale.getValue(), true, true));
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
        commentVisibleObserver = this::setCommentVisible;
        commentObserver = this::setComment;
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
                        // do nothing if a row element already has the focus
                        if (!itemView.hasFocus()) {
                            switch (item.getFocusedElement()) {
                                case Amount:
                                    tvAmount.requestFocus();
                                    break;
                                case Comment:
                                    tvComment.requestFocus();
                                    break;
                                case Account:
                                    focused = tvAccount.requestFocus();
                                    tvAccount.dismissDropDown();
                                    if (focused)
                                        Misc.showSoftKeyboard(
                                                (NewTransactionActivity) tvAccount.getContext());
                                    break;
                            }
                        }

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

        localeObserver = locale -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                tvAmount.setKeyListener(DigitsKeyListener.getInstance(locale, true, true));
        };

        currencyObserver = currency -> {
            setCurrency(currency);
            adapter.checkTransactionSubmittable();
        };

        currencyGapObserver = hasGap -> {
            updateCurrencyPositionAndPadding(Data.currencySymbolPosition.getValue(), hasGap);
        };

        currencyPositionObserver = position -> {
            updateCurrencyPositionAndPadding(position, Data.currencyGap.getValue());
        };

        showCurrencyObserver = showCurrency -> {
            if (showCurrency) {
                tvCurrency.setVisibility(View.VISIBLE);
            }
            else {
                tvCurrency.setVisibility(View.GONE);
                item.setCurrency(null);
            }
        };
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
        if ((currency == null) || currency.isEmpty()) {
            tvCurrency.setText(R.string.currency_symbol);
            tvCurrency.setTextColor(0x7f000000 + (0x00ffffff & Colors.defaultTextColor));
        }
        else {
            tvCurrency.setText(currency);
            tvCurrency.setTextColor(Colors.defaultTextColor);
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
    private void setCommentVisible(@NotNull Boolean visible) {
        if (visible) {
            // showing; show the comment view and align the comment button to it
            tvComment.setVisibility(View.VISIBLE);
            tvComment.requestFocus();
            ConstraintLayout.LayoutParams lp =
                    (ConstraintLayout.LayoutParams) commentButton.getLayoutParams();
            lp.bottomToBottom = R.id.comment;

            commentButton.setLayoutParams(lp);
        }
        else {
            // hiding; hide the comment view and align the comment bottom to the amount
            tvComment.setVisibility(View.GONE);
            ConstraintLayout.LayoutParams lp =
                    (ConstraintLayout.LayoutParams) commentButton.getLayoutParams();
            lp.bottomToBottom = R.id.amount_layout;   // R.id.parent doesn't work here

            commentButton.setLayoutParams(lp);
        }
    }
    private void setComment(String comment) {
        if ((comment != null) && !comment.isEmpty())
            commentButton.setBackgroundResource(R.drawable.ic_comment_black_24dp);
        else
            commentButton.setBackgroundResource(R.drawable.ic_comment_gray_24dp);
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
                    break;
                case transactionRow:
                    final LedgerTransactionAccount account = item.getAccount();
                    account.setAccountName(String.valueOf(tvAccount.getText()));

                    item.setComment(String.valueOf(tvComment.getText()));

                    String amount = String.valueOf(tvAmount.getText());
                    amount = amount.trim();

                    if (amount.isEmpty()) {
                        account.resetAmount();
//                        account.setCurrency(null);
                    }
                    else {
                        try {
                            amount = amount.replace(decimalSeparator, decimalDot);
                            account.setAmount(Float.parseFloat(amount));
                        }
                        catch (NumberFormatException e) {
                            Logger.debug("new-trans", String.format(
                                    "assuming amount is not set due to number format exception. " +
                                    "input was '%s'", amount));
                            account.resetAmount();
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
        finally {
            syncingData = false;
        }
    }
    private void pickTransactionDate() {
        DatePickerFragment picker = new DatePickerFragment();
        picker.setFutureDates(mProfile.getFutureDates());
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
                this.item.stopObservingCommentVisible(commentVisibleObserver);
                this.item.stopObservingComment(commentObserver);
                this.item.getModel()
                         .stopObservingFocusedItem(focusedAccountObserver);
                this.item.getModel()
                         .stopObservingAccountCount(accountCountObserver);
                Data.currencySymbolPosition.removeObserver(currencyPositionObserver);
                Data.currencyGap.removeObserver(currencyGapObserver);
                Data.locale.removeObserver(localeObserver);
                this.item.stopObservingCurrency(currencyObserver);
                this.item.getModel().showCurrency.removeObserver(showCurrencyObserver);

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

                if (!item.isOfType(ItemType.bottomFiller)) {
                    item.observeEditableFlag(activity, editableObserver);
                    item.getModel()
                        .observeFocusedItem(activity, focusedAccountObserver);
                }
                switch (item.getType()) {
                    case generalData:
                        item.observeDate(activity, dateObserver);
                        item.observeDescription(activity, descriptionObserver);
                        break;
                    case transactionRow:
                        item.observeAmountHint(activity, hintObserver);
                        item.observeCommentVisible(activity, commentVisibleObserver);
                        item.observeComment(activity, commentObserver);
                        Data.currencySymbolPosition.observe(activity, currencyPositionObserver);
                        Data.currencyGap.observe(activity, currencyGapObserver);
                        Data.locale.observe(activity, localeObserver);
                        item.observeCurrency(activity, currencyObserver);
                        item.getModel().showCurrency.observe(activity, showCurrencyObserver);
                        item.getModel()
                            .observeAccountCount(activity, accountCountObserver);
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
