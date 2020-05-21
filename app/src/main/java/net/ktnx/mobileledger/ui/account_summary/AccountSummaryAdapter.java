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

package net.ktnx.mobileledger.ui.account_summary;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerAccount;
import net.ktnx.mobileledger.ui.activity.MainActivity;
import net.ktnx.mobileledger.utils.LockHolder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

public class AccountSummaryAdapter
        extends RecyclerView.Adapter<AccountSummaryAdapter.LedgerRowHolder> {
    public static final int AMOUNT_LIMIT = 3;

    AccountSummaryAdapter() { }

    public void onBindViewHolder(@NonNull LedgerRowHolder holder, int position) {
        try (LockHolder lh = Data.accounts.lockForReading()) {
            if (position < Data.accounts.size()) {
                LedgerAccount acc = Data.accounts.get(position);
                Context ctx = holder.row.getContext();
                Resources rm = ctx.getResources();

                holder.row.setTag(acc);
                holder.row.setVisibility(View.VISIBLE);
                holder.vTrailer.setVisibility(View.GONE);
                holder.tvAccountName.setText(acc.getShortName());
                ConstraintLayout.LayoutParams lp =
                        (ConstraintLayout.LayoutParams) holder.tvAccountName.getLayoutParams();
                lp.setMarginStart(
                        acc.getLevel() * rm.getDimensionPixelSize(R.dimen.thumb_row_height) / 2);
                holder.expanderContainer
                        .setVisibility(acc.hasSubAccounts() ? View.VISIBLE : View.INVISIBLE);
                holder.expanderContainer.setRotation(acc.isExpanded() ? 0 : 180);
                int amounts = acc.getAmountCount();
                if ((amounts > AMOUNT_LIMIT) && !acc.amountsExpanded()) {
                    holder.tvAccountAmounts.setText(acc.getAmountsString(AMOUNT_LIMIT));
                    holder.accountExpanderContainer.setVisibility(View.VISIBLE);
                }
                else {
                    holder.tvAccountAmounts.setText(acc.getAmountsString());
                    holder.accountExpanderContainer.setVisibility(View.GONE);
                }

                if (acc.isHiddenByStar()) {
                    holder.tvAccountName.setTypeface(null, Typeface.ITALIC);
                    holder.tvAccountAmounts.setTypeface(null, Typeface.ITALIC);
                }
                else {
                    holder.tvAccountName.setTypeface(null, Typeface.NORMAL);
                    holder.tvAccountAmounts.setTypeface(null, Typeface.NORMAL);
                }

                holder.row.setTag(R.id.POS, position);
            }
            else {
                holder.vTrailer.setVisibility(View.VISIBLE);
                holder.row.setVisibility(View.GONE);
            }
        }
    }

    @NonNull
    @Override
    public LedgerRowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.account_summary_row, parent, false);
        return new LedgerRowHolder(row);
    }

    @Override
    public int getItemCount() {
        return Data.accounts.size();
    }
    public void selectItem(int position) {
        try (LockHolder lh = Data.accounts.lockForWriting()) {
            LedgerAccount acc = Data.accounts.get(position);
            acc.toggleHiddenToBe();
            toggleChildrenOf(acc, acc.isHiddenByStarToBe(), position);
            notifyItemChanged(position);
        }
    }
    private void toggleChildrenOf(LedgerAccount parent, boolean hiddenToBe, int parentPosition) {
        int i = parentPosition + 1;
        try (LockHolder lh = Data.accounts.lockForWriting()) {
            for (int j = 0; j < Data.accounts.size(); j++) {
                LedgerAccount acc = Data.accounts.get(j);
                if (acc.getName().startsWith(parent.getName() + ":")) {
                    acc.setHiddenByStarToBe(hiddenToBe);
                    notifyItemChanged(i);
                    toggleChildrenOf(acc, hiddenToBe, i);
                    i++;
                }
            }
        }
    }

    static class LedgerRowHolder extends RecyclerView.ViewHolder {
        TextView tvAccountName, tvAccountAmounts;
        ConstraintLayout row;
        View vTrailer;
        FrameLayout expanderContainer;
        ImageView expander;
        FrameLayout accountExpanderContainer;
        public LedgerRowHolder(@NonNull View itemView) {
            super(itemView);
            this.row = itemView.findViewById(R.id.account_summary_row);
            this.tvAccountName = itemView.findViewById(R.id.account_row_acc_name);
            this.tvAccountAmounts = itemView.findViewById(R.id.account_row_acc_amounts);
            this.vTrailer = itemView.findViewById(R.id.account_summary_trailer);
            this.expanderContainer = itemView.findViewById(R.id.account_expander_container);
            this.expander = itemView.findViewById(R.id.account_expander);
            this.accountExpanderContainer =
                    itemView.findViewById(R.id.account_row_amounts_expander_container);

            expanderContainer.addOnLayoutChangeListener(
                    (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                        int w = right - left;
                        int h = bottom - top;
                        if (h > w) {
                            int p = (h - w) / 2;
                            v.setPadding(0, p, 0, p);
                        }
                        else v.setPadding(0, 0, 0, 0);
                    });

            itemView.setOnLongClickListener(this::onItemLongClick);
            tvAccountName.setOnLongClickListener(this::onItemLongClick);
            tvAccountAmounts.setOnLongClickListener(this::onItemLongClick);
            expanderContainer.setOnLongClickListener(this::onItemLongClick);
            expander.setOnLongClickListener(this::onItemLongClick);
        }
        private boolean onItemLongClick(View v) {
            MainActivity activity = (MainActivity) v.getContext();
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            View row;
            int id = v.getId();
            switch (id) {
                case R.id.account_summary_row:
                    row = v;
                    break;
                case R.id.account_root:
                    row = v.findViewById(R.id.account_summary_row);
                    break;
                case R.id.account_row_acc_name:
                case R.id.account_row_acc_amounts:
                case R.id.account_expander_container:
                    row = (View) v.getParent();
                    break;
                case R.id.account_expander:
                    row = (View) v.getParent().getParent();
                    break;
                default:
                    Log.e("error", String.format("Don't know how to handle long click on id %d", id));
                    return false;
            }
            LedgerAccount acc = (LedgerAccount) row.findViewById(R.id.account_summary_row).getTag();
            builder.setTitle(acc.getName());
            builder.setItems(R.array.acc_ctx_menu, (dialog, which) -> {
                switch (which) {
                    case 0:
                        // show transactions
                        activity.showAccountTransactions(acc);
                        break;
                }
                dialog.dismiss();
            });
            builder.show();
            return true;
        }
    }
}
