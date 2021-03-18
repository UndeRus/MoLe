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

package net.ktnx.mobileledger.ui.account_summary;

import android.content.Context;
import android.content.res.Resources;
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
import net.ktnx.mobileledger.model.AccountListItem;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerAccount;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
import net.ktnx.mobileledger.ui.MainModel;
import net.ktnx.mobileledger.ui.activity.MainActivity;
import net.ktnx.mobileledger.utils.Locker;
import net.ktnx.mobileledger.utils.Misc;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Observer;

import static net.ktnx.mobileledger.utils.Logger.debug;

public class AccountSummaryAdapter
        extends RecyclerView.Adapter<AccountSummaryAdapter.LedgerRowHolder> {
    public static final int AMOUNT_LIMIT = 3;
    private final AsyncListDiffer<AccountListItem> listDiffer;
    private final MainModel model;
    AccountSummaryAdapter(MainModel model) {
        this.model = model;

        listDiffer = new AsyncListDiffer<>(this, new DiffUtil.ItemCallback<AccountListItem>() {
            @Override
            public boolean areItemsTheSame(@NotNull AccountListItem oldItem,
                                           @NotNull AccountListItem newItem) {
                final AccountListItem.Type oldType = oldItem.getType();
                final AccountListItem.Type newType = newItem.getType();
                if (oldType == AccountListItem.Type.HEADER) {
                    return newType == AccountListItem.Type.HEADER;
                }
                if (oldType != newType)
                    return false;

                return Misc.equalStrings(oldItem.getAccount()
                                                .getName(), newItem.getAccount()
                                                                   .getName());
            }
            @Override
            public boolean areContentsTheSame(@NotNull AccountListItem oldItem,
                                              @NotNull AccountListItem newItem) {
                if (oldItem.getType()
                           .equals(AccountListItem.Type.HEADER))
                    return true;
                return oldItem.getAccount()
                              .equals(newItem.getAccount());
            }
        });
    }

    public void onBindViewHolder(@NonNull LedgerRowHolder holder, int position) {
        holder.bindToAccount(listDiffer.getCurrentList()
                                       .get(position));
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
        return listDiffer.getCurrentList()
                         .size();
    }
    public void setAccounts(List<AccountListItem> newList) {
        listDiffer.submitList(newList);
    }
    class LedgerRowHolder extends RecyclerView.ViewHolder {
        private final TextView tvAccountName, tvAccountAmounts;
        private final ConstraintLayout row;
        private final View expanderContainer;
        private final View amountExpanderContainer;
        private final View lLastUpdate;
        private final TextView tvLastUpdate;
        private final View vAccountNameLayout;
        LedgerAccount mAccount;
        private AccountListItem.Type lastType;
        private Observer lastUpdateObserver;
        public LedgerRowHolder(@NonNull View itemView) {
            super(itemView);

            row = itemView.findViewById(R.id.account_summary_row);
            vAccountNameLayout = itemView.findViewById(R.id.account_name_layout);
            tvAccountName = itemView.findViewById(R.id.account_row_acc_name);
            tvAccountAmounts = itemView.findViewById(R.id.account_row_acc_amounts);
            expanderContainer = itemView.findViewById(R.id.account_expander_container);
            ImageView expander = itemView.findViewById(R.id.account_expander);
            amountExpanderContainer =
                    itemView.findViewById(R.id.account_row_amounts_expander_container);
            lLastUpdate = itemView.findViewById(R.id.last_update_container);
            tvLastUpdate = itemView.findViewById(R.id.last_update_text);

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
        private void toggleAccountExpanded() {
            if (!mAccount.hasSubAccounts())
                return;
            debug("accounts", "Account expander clicked");

            // make sure we use the same object as the one in the allAccounts list
            MobileLedgerProfile profile = mAccount.getProfile();
            if (profile == null) {
                return;
            }
            try (Locker ignored = model.lockAccountsForWriting()) {
                LedgerAccount realAccount = model.locateAccount(mAccount.getName());
                if (realAccount == null)
                    return;

                mAccount = realAccount;
                mAccount.toggleExpanded();
            }
            expanderContainer.animate()
                             .rotation(mAccount.isExpanded() ? 0 : 180);
            model.updateDisplayedAccounts();

            DbOpQueue.add("update accounts set expanded=? where name=? and profile=?",
                    new Object[]{mAccount.isExpanded(), mAccount.getName(), profile.getId()
                    });

        }
        private void toggleAmountsExpanded() {
            if (mAccount.getAmountCount() <= AMOUNT_LIMIT)
                return;

            mAccount.toggleAmountsExpanded();
            if (mAccount.amountsExpanded()) {
                tvAccountAmounts.setText(mAccount.getAmountsString());
                amountExpanderContainer.setVisibility(View.GONE);
            }
            else {
                tvAccountAmounts.setText(mAccount.getAmountsString(AMOUNT_LIMIT));
                amountExpanderContainer.setVisibility(View.VISIBLE);
            }

            MobileLedgerProfile profile = mAccount.getProfile();
            if (profile == null)
                return;

            DbOpQueue.add("update accounts set amounts_expanded=? where name=? and profile=?",
                    new Object[]{mAccount.amountsExpanded(), mAccount.getName(), profile.getId()
                    });

        }
        private boolean onItemLongClick(View v) {
            MainActivity activity = (MainActivity) v.getContext();
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            final String accountName = mAccount.getName();
            builder.setTitle(accountName);
            builder.setItems(R.array.acc_ctx_menu, (dialog, which) -> {
                if (which == 0) {// show transactions
                    activity.showAccountTransactions(accountName);
                }
                else {
                    throw new RuntimeException(String.format("Unknown menu item id (%d)", which));
                }
                dialog.dismiss();
            });
            builder.show();
            return true;
        }
        public void bindToAccount(AccountListItem item) {
            final AccountListItem.Type newType = item.getType();
            setType(newType);

            switch (newType) {
                case ACCOUNT:
                    LedgerAccount acc = item.getAccount();

                    debug("accounts", String.format(Locale.US, "Binding to '%s'", acc.getName()));
                    Context ctx = row.getContext();
                    Resources rm = ctx.getResources();
                    mAccount = acc;

                    row.setTag(acc);

                    tvAccountName.setText(acc.getShortName());

                    ConstraintLayout.LayoutParams lp =
                            (ConstraintLayout.LayoutParams) tvAccountName.getLayoutParams();
                    lp.setMarginStart(
                            acc.getLevel() * rm.getDimensionPixelSize(R.dimen.thumb_row_height) /
                            3);

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
                        amountExpanderContainer.setVisibility(View.VISIBLE);
                    }
                    else {
                        tvAccountAmounts.setText(acc.getAmountsString());
                        amountExpanderContainer.setVisibility(View.GONE);
                    }

                    break;
                case HEADER:
                    setLastUpdateText(Data.lastAccountsUpdateText.get());
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + newType);
            }

        }
        void setLastUpdateText(String text) {
            tvLastUpdate.setText(text);
        }
        private void initLastUpdateObserver() {
            if (lastUpdateObserver != null)
                return;

            lastUpdateObserver = (o, arg) -> setLastUpdateText(Data.lastAccountsUpdateText.get());

            Data.lastAccountsUpdateText.addObserver(lastUpdateObserver);
        }
        private void dropLastUpdateObserver() {
            if (lastUpdateObserver == null)
                return;

            Data.lastAccountsUpdateText.deleteObserver(lastUpdateObserver);
            lastUpdateObserver = null;
        }
        private void setType(AccountListItem.Type newType) {
            if (newType == lastType)
                return;

            switch (newType) {
                case ACCOUNT:
                    row.setLongClickable(true);
                    amountExpanderContainer.setVisibility(View.VISIBLE);
                    vAccountNameLayout.setVisibility(View.VISIBLE);
                    tvAccountAmounts.setVisibility(View.VISIBLE);
                    lLastUpdate.setVisibility(View.GONE);
                    dropLastUpdateObserver();
                    break;
                case HEADER:
                    row.setLongClickable(false);
                    tvAccountAmounts.setVisibility(View.GONE);
                    amountExpanderContainer.setVisibility(View.GONE);
                    vAccountNameLayout.setVisibility(View.GONE);
                    lLastUpdate.setVisibility(View.VISIBLE);
                    initLastUpdateObserver();
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + newType);
            }

            lastType = newType;
        }
    }
}
