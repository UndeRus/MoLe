/*
 * Copyright © 2018 Damyan Ivanov.
 * This file is part of Mobile-Ledger.
 * Mobile-Ledger is free software: you can distribute it and/or modify it
 * under the term of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your opinion), any later version.
 *
 * Mobile-Ledger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License terms for details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mobile-Ledger. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ktnx.mobileledger;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.model.LedgerTransactionAccount;
import net.ktnx.mobileledger.ui.transaction_list.TransactionListViewModel;
import net.ktnx.mobileledger.utils.Globals;
import net.ktnx.mobileledger.utils.MLDB;

import static net.ktnx.mobileledger.utils.DimensionUtils.dp2px;

class TransactionListAdapter
        extends RecyclerView.Adapter<TransactionListAdapter.TransactionRowHolder> {
    TransactionListViewModel model;
    private String boldAccountName;
    public TransactionListAdapter(TransactionListViewModel model) {
        this.model = model;
    }
    public void onBindViewHolder(@NonNull TransactionRowHolder holder, int position) {
        LedgerTransaction tr = model.getTransaction(position);
        // in a race when transaction list is reduced, but the model hasn't been notified yet
        // the view will disappear when the notifications reaches the model, so by simply omitting
        // the out-of-range get() call nothing bad happens - just a to-be-deleted view remains
        // a bit longer
        if (tr == null) return;

        Context ctx = holder.row.getContext();

        try (SQLiteDatabase db = MLDB.getReadableDatabase(ctx)) {
            tr.loadData(db);
            holder.tvDescription.setText(tr.getDescription());
            holder.tvDate.setText(tr.getDate());

            int rowIndex = 0;
            for (LedgerTransactionAccount acc : tr.getAccounts()) {
                LinearLayout row = (LinearLayout) holder.tableAccounts.getChildAt(rowIndex++);
                TextView accName, accAmount;
                if (row == null) {
                    row = new LinearLayout(ctx);
                    row.setLayoutParams(
                            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT));
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setPaddingRelative(dp2px(ctx, 8), 0, 0, 0);
                    accName = new AppCompatTextView(ctx);
                    accName.setLayoutParams(
                            new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT,
                                    5f));
                    accName.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                    row.addView(accName);
                    accAmount = new AppCompatTextView(ctx);
                    LinearLayout.LayoutParams llp =
                            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT);
                    llp.setMarginEnd(0);
                    accAmount.setLayoutParams(llp);
                    accAmount.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
                    accAmount.setMinWidth(dp2px(ctx, 60));
                    row.addView(accAmount);
                    holder.tableAccounts.addView(row);
                }
                else {
                    accName = (TextView) row.getChildAt(0);
                    accAmount = (TextView) row.getChildAt(1);
                }
                accName.setText(acc.getAccountName());
                accAmount.setText(acc.toString());

                if ((boldAccountName != null) && boldAccountName.equals(acc.getAccountName())) {
                    accName.setTypeface(null, Typeface.BOLD);
                    accAmount.setTypeface(null, Typeface.BOLD);
                    accName.setTextColor(Globals.primaryDark);
                    accAmount.setTextColor(Globals.primaryDark);
                }
                else {
                    accName.setTypeface(null, Typeface.NORMAL);
                    accAmount.setTypeface(null, Typeface.NORMAL);
                    accName.setTextColor(Globals.defaultTextColor);
                    accAmount.setTextColor(Globals.defaultTextColor);
                }

            }
            if (holder.tableAccounts.getChildCount() > rowIndex) {
                holder.tableAccounts
                        .removeViews(rowIndex, holder.tableAccounts.getChildCount() - rowIndex);
            }

            if (position % 2 == 0) {
                holder.row.setBackgroundColor(Globals.table_row_even_bg);
            }
            else {
                holder.row.setBackgroundColor(Globals.table_row_odd_bg);
            }

            Log.d("transactions", String.format("Filled position %d", position));
        }
    }

    @NonNull
    @Override
    public TransactionRowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d("perf", "onCreateViewHolder called");
        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.transaction_list_row, parent, false);
        return new TransactionRowHolder(row);
    }

    @Override
    public int getItemCount() {
        return model.getTransactionCount();
    }
    public void setBoldAccountName(String boldAccountName) {
        this.boldAccountName = boldAccountName;
    }
    public void resetBoldAccountName() {
        this.boldAccountName = null;
    }
    class TransactionRowHolder extends RecyclerView.ViewHolder {
        TextView tvDescription, tvDate;
        LinearLayout tableAccounts;
        ConstraintLayout row;
        public TransactionRowHolder(@NonNull View itemView) {
            super(itemView);
            this.row = itemView.findViewById(R.id.transaction_row);
            this.tvDescription = itemView.findViewById(R.id.transaction_row_description);
            this.tvDate = itemView.findViewById(R.id.transaction_row_date);
            this.tableAccounts = itemView.findViewById(R.id.transaction_row_acc_amounts);
        }
    }
}