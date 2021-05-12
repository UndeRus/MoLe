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
import android.view.View;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.databinding.NewTransactionHeaderRowBinding;
import net.ktnx.mobileledger.db.TransactionDescriptionAutocompleteAdapter;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.FutureDates;
import net.ktnx.mobileledger.ui.DatePickerFragment;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.Misc;
import net.ktnx.mobileledger.utils.SimpleDate;

import java.text.DecimalFormatSymbols;
import java.text.ParseException;

class NewTransactionHeaderItemHolder extends NewTransactionItemViewHolder
        implements DatePickerFragment.DatePickedListener {
    private final NewTransactionHeaderRowBinding b;
    private boolean ignoreFocusChanges = false;
    private String decimalSeparator;
    private boolean inUpdate = false;
    private boolean syncingData = false;
    NewTransactionHeaderItemHolder(@NonNull NewTransactionHeaderRowBinding b,
                                   NewTransactionItemsAdapter adapter) {
        super(b.getRoot());
        this.b = b;

        b.newTransactionDescription.setNextFocusForwardId(View.NO_ID);

        b.newTransactionDate.setOnClickListener(v -> pickTransactionDate());

        b.transactionCommentButton.setOnClickListener(v -> {
            b.transactionComment.setVisibility(View.VISIBLE);
            b.transactionComment.requestFocus();
        });

        @SuppressLint("DefaultLocale") View.OnFocusChangeListener focusMonitor = (v, hasFocus) -> {
            final int id = v.getId();
            if (hasFocus) {
                boolean wasSyncing = syncingData;
                syncingData = true;
                try {
                    final int pos = getBindingAdapterPosition();
                    if (id == R.id.transaction_comment) {
                        adapter.noteFocusIsOnTransactionComment(pos);
                    }
                    else if (id == R.id.new_transaction_description) {
                        adapter.noteFocusIsOnDescription(pos);
                    }
                    else
                        throw new IllegalStateException("Where is the focus? " + id);
                }
                finally {
                    syncingData = wasSyncing;
                }
            }

            if (id == R.id.transaction_comment) {
                commentFocusChanged(b.transactionComment, hasFocus);
            }
        };

        b.newTransactionDescription.setOnFocusChangeListener(focusMonitor);
        b.transactionComment.setOnFocusChangeListener(focusMonitor);

        NewTransactionActivity activity = (NewTransactionActivity) b.getRoot()
                                                                    .getContext();

        b.newTransactionDescription.setAdapter(
                new TransactionDescriptionAutocompleteAdapter(activity));
        b.newTransactionDescription.setOnItemClickListener(
                (parent, view, position, id) -> activity.onDescriptionSelected(
                        parent.getItemAtPosition(position)
                              .toString()));

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
        b.newTransactionDescription.addTextChangedListener(tw);
        monitorComment(b.transactionComment);

        commentFocusChanged(b.transactionComment, false);

        adapter.model.getFocusInfo()
                     .observe(activity, this::applyFocus);

        adapter.model.getShowComments()
                     .observe(activity, show -> b.transactionCommentLayout.setVisibility(
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

            NewTransactionModel.Item head = item.toTransactionHead();
            // bad idea - double pop-up, and not really necessary.
            // the user can tap the input to get the calendar
            //if (!tvDate.hasFocus()) tvDate.requestFocus();
            switch (focusInfo.element) {
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
        }
        finally {
            ignoreFocusChanges = false;
        }
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
    private void setEditable(Boolean editable) {
        b.newTransactionDate.setEnabled(editable);
        b.newTransactionDescription.setEnabled(editable);
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
            Logger.debug("new-trans", "Ignoring request to syncData(): adapter position negative");
            return false;
        }


        boolean significantChange = false;

        syncingData = true;
        try {
            final NewTransactionModel.Item item = getItem();
            if (item == null)
                return false;

            NewTransactionModel.TransactionHead head = item.toTransactionHead();

            head.setDate(String.valueOf(b.newTransactionDate.getText()));

            // transaction description is required
            if (TextUtils.isEmpty(head.getDescription()) !=
                TextUtils.isEmpty(b.newTransactionDescription.getText()))
                significantChange = true;

            head.setDescription(String.valueOf(b.newTransactionDescription.getText()));
            head.setComment(String.valueOf(b.transactionComment.getText()));

            return significantChange;
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
        picker.setFutureDates(FutureDates.valueOf(mProfile.getFutureDates()));
        picker.setOnDatePickedListener(this);
        picker.setCurrentDateFromText(b.newTransactionDate.getText());
        picker.show(((NewTransactionActivity) b.getRoot()
                                               .getContext()).getSupportFragmentManager(), null);
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
                NewTransactionModel.TransactionHead head = item.toTransactionHead();
                b.newTransactionDate.setText(head.getFormattedDate());

                // avoid triggering completion pop-up
                ListAdapter a = b.newTransactionDescription.getAdapter();
                try {
                    b.newTransactionDescription.setAdapter(null);
                    b.newTransactionDescription.setText(head.getDescription());
                }
                finally {
                    b.newTransactionDescription.setAdapter(
                            (TransactionDescriptionAutocompleteAdapter) a);
                }

                final String comment = head.getComment();
                b.transactionComment.setText(comment);
                styleComment(b.transactionComment, comment); // would hide or make it visible

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
    @Override
    public void onDatePicked(int year, int month, int day) {
        final NewTransactionModel.Item item = getItem();
        if (item == null)
            return;

        final NewTransactionModel.TransactionHead head = item.toTransactionHead();
        head.setDate(new SimpleDate(year, month + 1, day));
        b.newTransactionDate.setText(head.getFormattedDate());

        boolean focused = b.newTransactionDescription.requestFocus();
        if (focused)
            Misc.showSoftKeyboard((NewTransactionActivity) b.getRoot()
                                                            .getContext());

    }
}
