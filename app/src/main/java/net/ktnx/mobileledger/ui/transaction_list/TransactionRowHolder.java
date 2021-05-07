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

package net.ktnx.mobileledger.ui.transaction_list;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.databinding.TransactionListRowBinding;
import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.model.LedgerTransactionAccount;
import net.ktnx.mobileledger.model.TransactionListItem;
import net.ktnx.mobileledger.utils.Colors;
import net.ktnx.mobileledger.utils.Misc;

import java.util.Observer;

class TransactionRowHolder extends TransactionRowHolderBase {
    private final TransactionListRowBinding b;
    TransactionListItem.Type lastType;
    private Observer lastUpdateObserver;
    public TransactionRowHolder(@NonNull TransactionListRowBinding binding) {
        super(binding.getRoot());
        b = binding;
    }
    public void bind(@NonNull TransactionListItem item, @Nullable String boldAccountName) {
        LedgerTransaction tr = item.getTransaction();
        b.transactionRowDescription.setText(tr.getDescription());
        String trComment = Misc.emptyIsNull(tr.getComment());
        if (trComment == null)
            b.transactionComment.setVisibility(View.GONE);
        else {
            b.transactionComment.setText(trComment);
            b.transactionComment.setVisibility(View.VISIBLE);
        }

        if (Misc.emptyIsNull(item.getRunningTotal()) != null) {
            b.transactionRunningTotal.setText(item.getRunningTotal());
            b.transactionRunningTotal.setVisibility(View.VISIBLE);
            b.transactionRunningTotalDivider.setVisibility(View.VISIBLE);
        }
        else {
            b.transactionRunningTotal.setVisibility(View.GONE);
            b.transactionRunningTotalDivider.setVisibility(View.GONE);
        }

        int rowIndex = 0;
        Context ctx = b.getRoot()
                       .getContext();
        LayoutInflater inflater = ((Activity) ctx).getLayoutInflater();
        for (LedgerTransactionAccount acc : tr.getAccounts()) {
            LinearLayout row = (LinearLayout) b.transactionRowAccAmounts.getChildAt(rowIndex);
            if (row == null) {
                row = new LinearLayout(ctx);
                inflater.inflate(R.layout.transaction_list_row_accounts_table_row, row);
                b.transactionRowAccAmounts.addView(row);
            }

            TextView dummyText = row.findViewById(R.id.dummy_text);
            TextView accName = row.findViewById(R.id.transaction_list_acc_row_acc_name);
            TextView accComment = row.findViewById(R.id.transaction_list_acc_row_acc_comment);
            TextView accAmount = row.findViewById(R.id.transaction_list_acc_row_acc_amount);

            if ((boldAccountName != null) && acc.getAccountName()
                                                .startsWith(boldAccountName))
            {
                accName.setTextColor(Colors.primary);
                accAmount.setTextColor(Colors.primary);

                SpannableString ss = new SpannableString(acc.getAccountName());
                ss.setSpan(new StyleSpan(Typeface.BOLD), 0, boldAccountName.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                accName.setText(ss);
            }
            else {
                @ColorInt int textColor = dummyText.getTextColors()
                                                   .getDefaultColor();
                accName.setTextColor(textColor);
                accAmount.setTextColor(textColor);
                accName.setText(acc.getAccountName());
            }

            String comment = acc.getComment();
            if (comment != null && !comment.isEmpty()) {
                accComment.setText(comment);
                accComment.setVisibility(View.VISIBLE);
            }
            else {
                accComment.setVisibility(View.GONE);
            }
            accAmount.setText(acc.toString());

            rowIndex++;
        }

        if (b.transactionRowAccAmounts.getChildCount() > rowIndex) {
            b.transactionRowAccAmounts.removeViews(rowIndex,
                    b.transactionRowAccAmounts.getChildCount() - rowIndex);
        }
    }
}
