/*
 * Copyright © 2019 Damyan Ivanov.
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

package net.ktnx.mobileledger.ui.account_summary;

import android.arch.lifecycle.ViewModel;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.async.CommitAccountsTask;
import net.ktnx.mobileledger.async.CommitAccountsTaskParams;
import net.ktnx.mobileledger.async.UpdateAccountsTask;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerAccount;

import java.util.ArrayList;

class AccountSummaryViewModel extends ViewModel {
    static void commitSelections(Context context) {
        CAT task = new CAT();
        task.execute(
                new CommitAccountsTaskParams(Data.accounts.get(), Data.optShowOnlyStarred.get()));
    }
    void scheduleAccountListReload() {
        UAT task = new UAT();
        task.execute();

    }

    private static class UAT extends UpdateAccountsTask {
        @Override
        protected void onPostExecute(ArrayList<LedgerAccount> list) {
            super.onPostExecute(list);
            if (list != null) {
                Log.d("acc", "setting updated account list");
                Data.accounts.set(list);
            }
        }
    }

    private static class CAT extends CommitAccountsTask {
        @Override
        protected void onPostExecute(ArrayList<LedgerAccount> list) {
            super.onPostExecute(list);
            if (list != null) {
                Log.d("acc", "setting new account list");
                Data.accounts.set(list);
            }
        }
    }
}

class AccountSummaryAdapter extends RecyclerView.Adapter<AccountSummaryAdapter.LedgerRowHolder> {
    private boolean selectionActive;

    AccountSummaryAdapter() {
        this.selectionActive = false;
    }

    public void onBindViewHolder(@NonNull LedgerRowHolder holder, int position) {
        LedgerAccount acc = Data.accounts.get().get(position);
        Context ctx = holder.row.getContext();
        Resources rm = ctx.getResources();

        holder.tvAccountName.setText(acc.getShortName());
        holder.tvAccountName.setPadding(
                acc.getLevel() * rm.getDimensionPixelSize(R.dimen.activity_horizontal_margin) / 2,
                0, 0, 0);
        holder.tvAccountAmounts.setText(acc.getAmountsString());

        if (acc.isHidden()) {
            holder.tvAccountName.setTypeface(null, Typeface.ITALIC);
            holder.tvAccountAmounts.setTypeface(null, Typeface.ITALIC);
        }
        else {
            holder.tvAccountName.setTypeface(null, Typeface.NORMAL);
            holder.tvAccountAmounts.setTypeface(null, Typeface.NORMAL);
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

        holder.selectionCb.setVisibility(selectionActive ? View.VISIBLE : View.GONE);
        holder.selectionCb.setChecked(!acc.isHiddenToBe());

        holder.row.setTag(R.id.POS, position);
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
        return Data.accounts.get().size();
    }
    public void startSelection() {
        for (LedgerAccount acc : Data.accounts.get()) acc.setHiddenToBe(acc.isHidden());
        this.selectionActive = true;
        notifyDataSetChanged();
    }

    public void stopSelection() {
        this.selectionActive = false;
        notifyDataSetChanged();
    }

    public boolean isSelectionActive() {
        return selectionActive;
    }

    public void selectItem(int position) {
        LedgerAccount acc = Data.accounts.get().get(position);
        acc.toggleHiddenToBe();
        toggleChildrenOf(acc, acc.isHiddenToBe(), position);
        notifyItemChanged(position);
    }
    void toggleChildrenOf(LedgerAccount parent, boolean hiddenToBe, int parentPosition) {
        int i = parentPosition + 1;
        for (LedgerAccount acc : Data.accounts.get()) {
            if (acc.getName().startsWith(parent.getName() + ":")) {
                acc.setHiddenToBe(hiddenToBe);
                notifyItemChanged(i);
                toggleChildrenOf(acc, hiddenToBe, i);
                i++;
            }
        }
    }

    class LedgerRowHolder extends RecyclerView.ViewHolder {
        CheckBox selectionCb;
        TextView tvAccountName, tvAccountAmounts;
        LinearLayout row;
        public LedgerRowHolder(@NonNull View itemView) {
            super(itemView);
            this.row = (LinearLayout) itemView;
            this.tvAccountName = itemView.findViewById(R.id.account_row_acc_name);
            this.tvAccountAmounts = itemView.findViewById(R.id.account_row_acc_amounts);
            this.selectionCb = itemView.findViewById(R.id.account_row_check);
        }
    }
}