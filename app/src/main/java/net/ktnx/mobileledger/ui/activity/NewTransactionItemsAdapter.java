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

package net.ktnx.mobileledger.ui.activity;

import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import net.ktnx.mobileledger.App;
import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.async.DescriptionSelectedCallback;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.model.LedgerTransactionAccount;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
import net.ktnx.mobileledger.utils.Logger;

import java.util.ArrayList;
import java.util.Locale;

import static net.ktnx.mobileledger.utils.Logger.debug;

class NewTransactionItemsAdapter extends RecyclerView.Adapter<NewTransactionItemHolder>
        implements DescriptionSelectedCallback {
    NewTransactionModel model;
    private MobileLedgerProfile mProfile;
    private ItemTouchHelper touchHelper;
    private RecyclerView recyclerView;
    NewTransactionItemsAdapter(NewTransactionModel viewModel, MobileLedgerProfile profile) {
        super();
        model = viewModel;
        mProfile = profile;
        int size = model.getAccountCount();
        while (size < 2) {
            Logger.debug("new-transaction",
                    String.format(Locale.US, "%d accounts is too little, Calling addRow()", size));
            size = addRow();
        }

        NewTransactionItemsAdapter adapter = this;

        touchHelper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public boolean isLongPressDragEnabled() {
                return true;
            }
            @Override
            public boolean canDropOver(@NonNull RecyclerView recyclerView,
                                       @NonNull RecyclerView.ViewHolder current,
                                       @NonNull RecyclerView.ViewHolder target) {
                final int adapterPosition = target.getAdapterPosition();

                // first and last items are immovable
                if (adapterPosition == 0)
                    return false;
                if (adapterPosition == adapter.getItemCount() - 1)
                    return false;

                return super.canDropOver(recyclerView, current, target);
            }
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView,
                                        @NonNull RecyclerView.ViewHolder viewHolder) {
                int flags = makeFlag(ItemTouchHelper.ACTION_STATE_IDLE, ItemTouchHelper.END);
                // the top item is always there (date and description)
                if (viewHolder.getAdapterPosition() > 0) {
                    flags |= makeFlag(ItemTouchHelper.ACTION_STATE_DRAG,
                            ItemTouchHelper.UP | ItemTouchHelper.DOWN);

                    if (viewModel.getAccountCount() > 2) {
                        flags |= makeFlag(ItemTouchHelper.ACTION_STATE_SWIPE,
                                ItemTouchHelper.START | ItemTouchHelper.END);
                    }
                }

                return flags;
            }
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {

                model.swapItems(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                notifyItemMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                return true;
            }
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                if (viewModel.getAccountCount() == 2)
                    Snackbar.make(recyclerView, R.string.msg_at_least_two_accounts_are_required,
                            Snackbar.LENGTH_LONG)
                            .setAction("Action", null)
                            .show();
                else {
                    int pos = viewHolder.getAdapterPosition();
                    viewModel.removeItem(pos - 1);
                    notifyItemRemoved(pos);
                    viewModel.sendCountNotifications(); // needed after items re-arrangement
                    viewModel.checkTransactionSubmittable(adapter);
                }
            }
        });
    }
    public void setProfile(MobileLedgerProfile profile) {
        mProfile = profile;
    }
    int addRow() {
        final int newAccountCount = model.addAccount(new LedgerTransactionAccount(""));
        Logger.debug("new-transaction",
                String.format(Locale.US, "invoking notifyItemInserted(%d)", newAccountCount));
        // the header is at position 0
        notifyItemInserted(newAccountCount);
        model.sendCountNotifications(); // needed after holders' positions have changed
        return newAccountCount;
    }
    @NonNull
    @Override
    public NewTransactionItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout row = (LinearLayout) LayoutInflater.from(parent.getContext())
                                                        .inflate(R.layout.new_transaction_row,
                                                                parent, false);

        return new NewTransactionItemHolder(row, this);
    }
    @Override
    public void onBindViewHolder(@NonNull NewTransactionItemHolder holder, int position) {
        Logger.debug("bind", String.format(Locale.US, "Binding item at position %d", position));
        NewTransactionModel.Item item = model.getItem(position);
        holder.setData(item);
        Logger.debug("bind", String.format(Locale.US, "Bound %s item at position %d", item.getType()
                                                                                          .toString(),
                position));
    }
    @Override
    public int getItemCount() {
        return model.getAccountCount() + 2;
    }
    boolean accountListIsEmpty() {
        for (int i = 0; i < model.getAccountCount(); i++) {
            LedgerTransactionAccount acc = model.getAccount(i);
            if (!acc.getAccountName()
                    .isEmpty())
                return false;
            if (acc.isAmountSet())
                return false;
        }

        return true;
    }
    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
        touchHelper.attachToRecyclerView(recyclerView);
    }
    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        touchHelper.attachToRecyclerView(null);
        super.onDetachedFromRecyclerView(recyclerView);
        this.recyclerView = null;
    }
    public void descriptionSelected(String description) {
        debug("descr selected", description);
        if (!accountListIsEmpty())
            return;

        String accFilter = mProfile.getPreferredAccountsFilter();

        ArrayList<String> params = new ArrayList<>();
        StringBuilder sb = new StringBuilder(
                "select t.profile, t.id from transactions t where t.description=?");
        params.add(description);

        if (accFilter != null) {
            sb.append(" AND EXISTS (")
              .append("SELECT 1 FROM transaction_accounts ta ")
              .append("WHERE ta.profile = t.profile")
              .append(" AND ta.transaction_id = t.id")
              .append(" AND UPPER(ta.account_name) LIKE '%'||?||'%')");
            params.add(accFilter.toUpperCase());
        }

        sb.append(" ORDER BY date desc limit 1");

        final String sql = sb.toString();
        debug("descr", sql);
        debug("descr", params.toString());

        try (Cursor c = App.getDatabase()
                           .rawQuery(sql, params.toArray(new String[]{})))
        {
            if (!c.moveToNext())
                return;

            String profileUUID = c.getString(0);
            int transactionId = c.getInt(1);
            LedgerTransaction tr;
            MobileLedgerProfile profile = Data.getProfile(profileUUID);
            if (profile == null)
                throw new RuntimeException(String.format(
                        "Unable to find profile %s, which is supposed to contain " +
                        "transaction %d with description %s", profileUUID, transactionId,
                        description));

            tr = profile.loadTransaction(transactionId);
            ArrayList<LedgerTransactionAccount> accounts = tr.getAccounts();
            NewTransactionModel.Item firstNegative = null;
            boolean singleNegative = false;
            int negativeCount = 0;
            for (int i = 0; i < accounts.size(); i++) {
                LedgerTransactionAccount acc = accounts.get(i);
                NewTransactionModel.Item item;
                if (model.getAccountCount() < i + 1) {
                    model.addAccount(acc);
                    notifyItemInserted(i + 1);
                }
                item = model.getItem(i + 1);

                item.getAccount()
                    .setAccountName(acc.getAccountName());
                if (acc.isAmountSet()) {
                    item.getAccount()
                        .setAmount(acc.getAmount());
                    if (acc.getAmount() < 0) {
                        if (firstNegative == null) {
                            firstNegative = item;
                            singleNegative = true;
                        }
                        else
                            singleNegative = false;
                    }
                }
                else
                    item.getAccount()
                        .resetAmount();
                notifyItemChanged(i + 1);
            }

            if (singleNegative) {
                firstNegative.getAccount()
                             .resetAmount();
            }
        }
        model.checkTransactionSubmittable(this);
        model.setFocusedItem(1);
    }
    public void toggleAllEditing(boolean editable) {
        // item 0 is the header
        for (int i = 0; i <= model.getAccountCount(); i++) {
            model.getItem(i)
                 .setEditable(editable);
            notifyItemChanged(i);
            // TODO perhaps do only one notification about the whole range (notifyDatasetChanged)?
        }
    }
    public void reset() {
        int presentItemCount = model.getAccountCount();
        model.reset();
        notifyItemChanged(0);       // header changed
        notifyItemRangeChanged(1, 2);    // the two empty rows
        if (presentItemCount > 2)
            notifyItemRangeRemoved(3, presentItemCount - 2); // all the rest are gone
    }
    public void updateFocusedItem(int position) {
        model.updateFocusedItem(position);
    }
    public void noteFocusIsOnAccount(int position) {
        model.noteFocusIsOnAccount(position);
    }
    public void noteFocusIsOnAmount(int position) {
        model.noteFocusIsOnAmount(position);
    }
}
