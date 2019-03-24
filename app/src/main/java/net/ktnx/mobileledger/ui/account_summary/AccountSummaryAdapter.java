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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerAccount;
import net.ktnx.mobileledger.utils.LockHolder;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

public class AccountSummaryAdapter
        extends RecyclerView.Adapter<AccountSummaryAdapter.LedgerRowHolder> {
    private boolean selectionActive;

    AccountSummaryAdapter() {
        this.selectionActive = false;
    }

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
                holder.tvAccountAmounts.setText(acc.getAmountsString());

                if (acc.isHiddenByStar()) {
                    holder.tvAccountName.setTypeface(null, Typeface.ITALIC);
                    holder.tvAccountAmounts.setTypeface(null, Typeface.ITALIC);
                }
                else {
                    holder.tvAccountName.setTypeface(null, Typeface.NORMAL);
                    holder.tvAccountAmounts.setTypeface(null, Typeface.NORMAL);
                }

                holder.selectionCb.setVisibility(selectionActive ? View.VISIBLE : View.GONE);
                holder.selectionCb.setChecked(!acc.isHiddenByStarToBe());

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
        return Data.accounts.size() + 1;
    }
    public void startSelection() {
        try (LockHolder lh = Data.accounts.lockForWriting()) {
            for (int i = 0; i < Data.accounts.size(); i++) {
                LedgerAccount acc = Data.accounts.get(i);
                acc.setHiddenByStarToBe(acc.isHiddenByStar());
            }
            this.selectionActive = true;
            lh.downgrade();
            notifyDataSetChanged();
        }
    }

    public void stopSelection() {
        this.selectionActive = false;
        notifyDataSetChanged();
    }

    public boolean isSelectionActive() {
        return selectionActive;
    }

    public void selectItem(int position) {
        try (LockHolder lh = Data.accounts.lockForWriting()) {
            LedgerAccount acc = Data.accounts.get(position);
            acc.toggleHiddenToBe();
            toggleChildrenOf(acc, acc.isHiddenByStarToBe(), position);
            notifyItemChanged(position);
        }
    }
    void toggleChildrenOf(LedgerAccount parent, boolean hiddenToBe, int parentPosition) {
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

    class LedgerRowHolder extends RecyclerView.ViewHolder {
        CheckBox selectionCb;
        TextView tvAccountName, tvAccountAmounts;
        ConstraintLayout row;
        View vTrailer;
        FrameLayout expanderContainer;
        ImageView expander;
        public LedgerRowHolder(@NonNull View itemView) {
            super(itemView);
            this.row = itemView.findViewById(R.id.account_summary_row);
            this.tvAccountName = itemView.findViewById(R.id.account_row_acc_name);
            this.tvAccountAmounts = itemView.findViewById(R.id.account_row_acc_amounts);
            this.selectionCb = itemView.findViewById(R.id.account_row_check);
            this.vTrailer = itemView.findViewById(R.id.account_summary_trailer);
            this.expanderContainer = itemView.findViewById(R.id.account_expander_container);
            this.expander = itemView.findViewById(R.id.account_expander);

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
        }
    }
}
