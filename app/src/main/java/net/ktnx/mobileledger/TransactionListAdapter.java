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
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import net.ktnx.mobileledger.model.LedgerTransaction;

import java.util.List;

class TransactionListAdapter
        extends RecyclerView.Adapter<TransactionListAdapter.TransactionRowHolder> {
    private List<LedgerTransaction> transactions;

    TransactionListAdapter(List<LedgerTransaction> transactions) {
        this.transactions = transactions;
    }

    public void onBindViewHolder(@NonNull TransactionRowHolder holder, int position) {
        LedgerTransaction tr = transactions.get(position);
        Context ctx = holder.row.getContext();
        Resources rm = ctx.getResources();

        holder.tvDescription.setText(String.format("%s\n%s", tr.getDescription(), tr.getDate()));
        TableLayout tbl = holder.row.findViewById(R.id.transaction_row_acc_amounts);
        tbl.removeAllViews();
        for (Iterator<LedgerTransactionItem> it = tr.getItemsIterator(); it.hasNext(); ) {
            LedgerTransactionItem acc = it.next();
            TableRow row = new TableRow(holder.row.getContext());
            TextView child = new TextView(ctx);
            child.setText(acc.getShortAccountName());
            row.addView(child);
            child = new TextView(ctx);
            child.setText(acc.toString());
            row.addView(child);
            tbl.addView(row);
        }

        if (position % 2 == 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) holder.row
                    .setBackgroundColor(rm.getColor(R.color.table_row_even_bg, ctx.getTheme()));
            else holder.row.setBackgroundColor(rm.getColor(R.color.table_row_even_bg));
        }
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) holder.row
                    .setBackgroundColor(rm.getColor(R.color.drawer_background, ctx.getTheme()));
            else holder.row.setBackgroundColor(rm.getColor(R.color.drawer_background));
        }

        holder.row.setTag(R.id.POS, position);
    }

    @NonNull
    @Override
    public TransactionRowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.transaction_list_row, parent, false);
        return new TransactionRowHolder(row);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }
    class TransactionRowHolder extends RecyclerView.ViewHolder {
        TextView tvDescription;
        TableLayout tableAccounts;
        LinearLayout row;
        public TransactionRowHolder(@NonNull View itemView) {
            super(itemView);
            this.row = (LinearLayout) itemView;
            this.tvDescription = itemView.findViewById(R.id.transaction_row_description);
            this.tableAccounts = itemView.findViewById(R.id.transaction_row_acc_amounts);
        }
    }
}