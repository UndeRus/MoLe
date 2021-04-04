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

import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.databinding.AccountListRowBinding;
import net.ktnx.mobileledger.databinding.AccountListSummaryRowBinding;
import net.ktnx.mobileledger.db.Account;
import net.ktnx.mobileledger.db.DB;
import net.ktnx.mobileledger.model.AccountListItem;
import net.ktnx.mobileledger.model.LedgerAccount;
import net.ktnx.mobileledger.ui.activity.MainActivity;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.Misc;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

import static net.ktnx.mobileledger.utils.Logger.debug;

public class AccountSummaryAdapter extends RecyclerView.Adapter<AccountSummaryAdapter.RowHolder> {
    public static final int AMOUNT_LIMIT = 3;
    private static final int ITEM_TYPE_HEADER = 1;
    private static final int ITEM_TYPE_ACCOUNT = 2;
    private final AsyncListDiffer<AccountListItem> listDiffer;
    AccountSummaryAdapter() {
        setHasStableIds(true);
        
        listDiffer = new AsyncListDiffer<>(this, new DiffUtil.ItemCallback<AccountListItem>() {
            @Override
            public boolean areItemsTheSame(@NotNull AccountListItem oldItem,
                                           @NotNull AccountListItem newItem) {
                final AccountListItem.Type oldType = oldItem.getType();
                final AccountListItem.Type newType = newItem.getType();
                if (oldType != newType)
                    return false;
                if (oldType == AccountListItem.Type.HEADER)
                    return true;

                return Misc.equalStrings(oldItem.getAccount()
                                                .getName(), newItem.getAccount()
                                                                   .getName());
            }
            @Override
            public boolean areContentsTheSame(@NotNull AccountListItem oldItem,
                                              @NotNull AccountListItem newItem) {
                return oldItem.sameContent(newItem);
            }
        });
    }
    @Override
    public long getItemId(int position) {
        if (position == 0)
            return 0;
        return listDiffer.getCurrentList()
                         .get(position)
                         .getAccount()
                         .getId();
    }
    public void onBindViewHolder(@NonNull RowHolder holder, int position) {
        holder.bind(listDiffer.getCurrentList()
                              .get(position));
    }

    @NonNull
    @Override
    public RowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        final RowHolder result;
        switch (viewType) {
            case ITEM_TYPE_HEADER:
                result = new HeaderRowHolder(
                        AccountListSummaryRowBinding.inflate(inflater, parent, false));
                break;
            case ITEM_TYPE_ACCOUNT:
                result = new AccountRowHolder(
                        AccountListRowBinding.inflate(inflater, parent, false));
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + viewType);
        }

        Logger.debug("acc-ui", "Creating " + result);
        return result;
    }

    @Override
    public int getItemCount() {
        return listDiffer.getCurrentList()
                         .size();
    }
    @Override
    public int getItemViewType(int position) {
        return (position == 0) ? ITEM_TYPE_HEADER : ITEM_TYPE_ACCOUNT;
    }
    public void setAccounts(List<AccountListItem> newList) {
        new Handler(Looper.getMainLooper()).post(() -> listDiffer.submitList(newList));
    }
    static abstract class RowHolder extends RecyclerView.ViewHolder {
        public RowHolder(@NonNull View itemView) {
            super(itemView);
        }
        public abstract void bind(AccountListItem accountListItem);
    }

    static class HeaderRowHolder extends RowHolder {
        private final AccountListSummaryRowBinding b;
        public HeaderRowHolder(@NonNull AccountListSummaryRowBinding binding) {
            super(binding.getRoot());
            b = binding;
        }
        @Override
        public void bind(AccountListItem item) {
            Resources r = itemView.getResources();
            Logger.debug("acc", itemView.getContext()
                                        .toString());
            ((AccountListItem.Header) item).getText()
                                           .observe((LifecycleOwner) itemView.getContext(),
                                                   b.lastUpdateText::setText);
        }
    }

    class AccountRowHolder extends AccountSummaryAdapter.RowHolder {
        private final AccountListRowBinding b;
        public AccountRowHolder(@NonNull AccountListRowBinding binding) {
            super(binding.getRoot());
            b = binding;

            itemView.setOnLongClickListener(this::onItemLongClick);
            b.accountRowAccName.setOnLongClickListener(this::onItemLongClick);
            b.accountRowAccAmounts.setOnLongClickListener(this::onItemLongClick);
            b.accountExpanderContainer.setOnLongClickListener(this::onItemLongClick);
            b.accountExpander.setOnLongClickListener(this::onItemLongClick);

            b.accountRowAccName.setOnClickListener(v -> toggleAccountExpanded());
            b.accountExpanderContainer.setOnClickListener(v -> toggleAccountExpanded());
            b.accountExpander.setOnClickListener(v -> toggleAccountExpanded());
            b.accountRowAccAmounts.setOnClickListener(v -> toggleAmountsExpanded());
        }
        private void toggleAccountExpanded() {
            LedgerAccount account = getAccount();
            if (!account.hasSubAccounts())
                return;
            debug("accounts", "Account expander clicked");

            AsyncTask.execute(() -> {
                Account dbo = account.toDBO();
                dbo.setExpanded(!dbo.isExpanded());
                Logger.debug("accounts",
                        String.format(Locale.ROOT, "%s (%d) → %s", account.getName(), dbo.getId(),
                                dbo.isExpanded() ? "expanded" : "collapsed"));
                DB.get()
                  .getAccountDAO()
                  .updateSync(dbo);
            });
        }
        @NotNull
        private LedgerAccount getAccount() {
            return listDiffer.getCurrentList()
                             .get(getAdapterPosition())
                             .getAccount();
        }
        private void toggleAmountsExpanded() {
            LedgerAccount account = getAccount();
            if (account.getAmountCount() <= AMOUNT_LIMIT)
                return;

            account.toggleAmountsExpanded();
            if (account.amountsExpanded()) {
                b.accountRowAccAmounts.setText(account.getAmountsString());
                b.accountRowAmountsExpanderContainer.setVisibility(View.GONE);
            }
            else {
                b.accountRowAccAmounts.setText(account.getAmountsString(AMOUNT_LIMIT));
                b.accountRowAmountsExpanderContainer.setVisibility(View.VISIBLE);
            }

            AsyncTask.execute(() -> {
                Account dbo = account.toDBO();
                DB.get()
                  .getAccountDAO()
                  .updateSync(dbo);
            });
        }
        private boolean onItemLongClick(View v) {
            MainActivity activity = (MainActivity) v.getContext();
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            final String accountName = getAccount().getName();
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
        @Override
        public void bind(AccountListItem item) {
            LedgerAccount acc = item.getAccount();

            debug("accounts",
                    String.format(Locale.US, "Binding to '%s' to %s", acc.getName(), this));
            Resources rm = b.getRoot()
                            .getContext()
                            .getResources();

            b.accountRowAccName.setText(acc.getShortName());

            ConstraintLayout.LayoutParams lp =
                    (ConstraintLayout.LayoutParams) b.accountNameLayout.getLayoutParams();
            lp.setMarginStart(
                    acc.getLevel() * rm.getDimensionPixelSize(R.dimen.thumb_row_height) / 3);

            if (acc.hasSubAccounts()) {
                b.accountExpanderContainer.setVisibility(View.VISIBLE);
                int wantedRotation = acc.isExpanded() ? 0 : 180;
                if (b.accountExpanderContainer.getRotation() != wantedRotation) {
                    Logger.debug("acc-ui",
                            String.format(Locale.ROOT, "Rotating %s to %d", acc.getName(),
                                    wantedRotation));
                    b.accountExpanderContainer.animate()
                                              .rotation(wantedRotation);
                }
            }
            else {
                b.accountExpanderContainer.setVisibility(View.GONE);
            }

            int amounts = acc.getAmountCount();
            if ((amounts > AMOUNT_LIMIT) && !acc.amountsExpanded()) {
                b.accountRowAccAmounts.setText(acc.getAmountsString(AMOUNT_LIMIT));
                b.accountRowAmountsExpanderContainer.setVisibility(View.VISIBLE);
            }
            else {
                b.accountRowAccAmounts.setText(acc.getAmountsString());
                b.accountRowAmountsExpanderContainer.setVisibility(View.GONE);
            }
        }
    }
}
