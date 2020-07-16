/*
 * Copyright © 2020 Damyan Ivanov.
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
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.async.DbOpQueue;
import net.ktnx.mobileledger.model.LedgerAccount;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
import net.ktnx.mobileledger.ui.activity.MainActivity;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import static net.ktnx.mobileledger.utils.Logger.debug;

public class AccountSummaryAdapter
        extends RecyclerView.Adapter<AccountSummaryAdapter.LedgerRowHolder> {
    public static final int AMOUNT_LIMIT = 3;
    private MobileLedgerProfile profile;
    private AsyncListDiffer<LedgerAccount> listDiffer;
    AccountSummaryAdapter() {
        listDiffer = new AsyncListDiffer<>(this, new DiffUtil.ItemCallback<LedgerAccount>() {
            @Override
            public boolean areItemsTheSame(@NotNull LedgerAccount oldItem,
                                           @NotNull LedgerAccount newItem) {
                return TextUtils.equals(oldItem.getName(), newItem.getName());
            }
            @Override
            public boolean areContentsTheSame(@NotNull LedgerAccount oldItem,
                                              @NotNull LedgerAccount newItem) {
                return (oldItem.isExpanded() == newItem.isExpanded()) &&
                       (oldItem.amountsExpanded() == newItem.amountsExpanded() &&
                        TextUtils.equals(oldItem.getAmountsString(), newItem.getAmountsString()));
            }
        });
    }

    public void onBindViewHolder(@NonNull LedgerRowHolder holder, int position) {
        holder.bindToAccount(listDiffer.getCurrentList().get(position));
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
        return listDiffer.getCurrentList().size();
    }
    public void setAccounts(MobileLedgerProfile profile, ArrayList<LedgerAccount> newList) {
        this.profile = profile;
        listDiffer.submitList(newList);
    }
    class LedgerRowHolder extends RecyclerView.ViewHolder {
        TextView tvAccountName, tvAccountAmounts;
        ConstraintLayout row;
        View expanderContainer;
        ImageView expander;
        View accountExpanderContainer;
        public LedgerRowHolder(@NonNull View itemView) {
            super(itemView);
            row = itemView.findViewById(R.id.account_summary_row);
            tvAccountName = itemView.findViewById(R.id.account_row_acc_name);
            tvAccountAmounts = itemView.findViewById(R.id.account_row_acc_amounts);
            expanderContainer = itemView.findViewById(R.id.account_expander_container);
            expander = itemView.findViewById(R.id.account_expander);
            accountExpanderContainer =
                    itemView.findViewById(R.id.account_row_amounts_expander_container);

            itemView.setOnLongClickListener(this::onItemLongClick);
            tvAccountName.setOnLongClickListener(this::onItemLongClick);
            tvAccountAmounts.setOnLongClickListener(this::onItemLongClick);
            expanderContainer.setOnLongClickListener(this::onItemLongClick);
            expander.setOnLongClickListener(this::onItemLongClick);
            row.setOnLongClickListener(this::onItemLongClick);

            tvAccountName.setOnClickListener(v -> toggleAccountExpanded());
            expanderContainer.setOnClickListener(v -> toggleAccountExpanded());
            expander.setOnClickListener(v -> toggleAccountExpanded());
            tvAccountAmounts.setOnClickListener(v -> toggleAmountsExpanded());
        }
        private @NonNull
        LedgerAccount getAccount() {
            final ArrayList<LedgerAccount> accountList = profile.getAccounts()
                                                                .getValue();
            if (accountList == null)
                throw new IllegalStateException("No account list");

            return accountList.get(getAdapterPosition());
        }
        private void toggleAccountExpanded() {
            LedgerAccount acc = getAccount();
            if (!acc.hasSubAccounts())
                return;
            debug("accounts", "Account expander clicked");

            acc.toggleExpanded();
            expanderContainer.animate()
                             .rotation(acc.isExpanded() ? 0 : 180);

            MobileLedgerProfile profile = acc.getProfile();
            if (profile == null)
                return;

            DbOpQueue.add("update accounts set expanded=? where name=? and profile=?",
                    new Object[]{acc.isExpanded(), acc.getName(), profile.getUuid()
                    }, profile::scheduleAccountListReload);

        }
        private void toggleAmountsExpanded() {
            LedgerAccount acc = getAccount();
            if (acc.getAmountCount() <= AMOUNT_LIMIT)
                return;

            acc.toggleAmountsExpanded();
            if (acc.amountsExpanded()) {
                tvAccountAmounts.setText(acc.getAmountsString());
                accountExpanderContainer.setVisibility(View.GONE);
            }
            else {
                tvAccountAmounts.setText(acc.getAmountsString(AMOUNT_LIMIT));
                accountExpanderContainer.setVisibility(View.VISIBLE);
            }

            MobileLedgerProfile profile = acc.getProfile();
            if (profile == null)
                return;

            DbOpQueue.add("update accounts set amounts_expanded=? where name=? and profile=?",
                    new Object[]{acc.amountsExpanded(), acc.getName(), profile.getUuid()
                    });

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
                case R.id.account_row_acc_amounts:
                case R.id.account_row_amounts_expander_container:
                    row = (View) v.getParent();
                    break;
                case R.id.account_row_acc_name:
                case R.id.account_expander_container:
                    row = (View) v.getParent()
                                  .getParent();
                    break;
                case R.id.account_expander:
                    row = (View) v.getParent()
                                  .getParent()
                                  .getParent();
                    break;
                default:
                    Log.e("error",
                            String.format("Don't know how to handle long click on id %d", id));
                    return false;
            }
            LedgerAccount acc = getAccount();
            builder.setTitle(acc.getName());
            builder.setItems(R.array.acc_ctx_menu, (dialog, which) -> {
                switch (which) {
                    case 0:
                        // show transactions
                        activity.showAccountTransactions(acc.getName());
                        break;
                }
                dialog.dismiss();
            });
            builder.show();
            return true;
        }
        public void bindToAccount(LedgerAccount acc) {
            Context ctx = row.getContext();
            Resources rm = ctx.getResources();

            row.setTag(acc);

            tvAccountName.setText(acc.getShortName());

            ConstraintLayout.LayoutParams lp =
                    (ConstraintLayout.LayoutParams) tvAccountName.getLayoutParams();
            lp.setMarginStart(
                    acc.getLevel() * rm.getDimensionPixelSize(R.dimen.thumb_row_height) / 3);

            if (acc.hasSubAccounts()) {
                expanderContainer.setVisibility(View.VISIBLE);
                expanderContainer.setRotation(acc.isExpanded() ? 0 : 180);
            }
            else {
                expanderContainer.setVisibility(View.GONE);
            }

            int amounts = acc.getAmountCount();
            if ((amounts > AMOUNT_LIMIT) && !acc.amountsExpanded()) {
                tvAccountAmounts.setText(acc.getAmountsString(AMOUNT_LIMIT));
                accountExpanderContainer.setVisibility(View.VISIBLE);
            }
            else {
                tvAccountAmounts.setText(acc.getAmountsString());
                accountExpanderContainer.setVisibility(View.GONE);
            }
        }
    }
}
