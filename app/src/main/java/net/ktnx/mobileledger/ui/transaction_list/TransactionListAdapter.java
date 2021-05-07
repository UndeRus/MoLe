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

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import net.ktnx.mobileledger.databinding.LastUpdateLayoutBinding;
import net.ktnx.mobileledger.databinding.TransactionDelimiterBinding;
import net.ktnx.mobileledger.databinding.TransactionListRowBinding;
import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.model.TransactionListItem;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.Misc;

import java.util.List;
import java.util.Locale;

public class TransactionListAdapter extends RecyclerView.Adapter<TransactionRowHolderBase> {
    private final AsyncListDiffer<TransactionListItem> listDiffer;
    public TransactionListAdapter() {
        super();

        setHasStableIds(true);

        listDiffer = new AsyncListDiffer<>(this, new DiffUtil.ItemCallback<TransactionListItem>() {
            @Override
            public boolean areItemsTheSame(@NonNull TransactionListItem oldItem,
                                           @NonNull TransactionListItem newItem) {
                if (oldItem.getType() != newItem.getType())
                    return false;
                switch (oldItem.getType()) {
                    case DELIMITER:
                        return (oldItem.getDate()
                                       .equals(newItem.getDate()));
                    case TRANSACTION:
                        return oldItem.getTransaction()
                                      .getLedgerId() == newItem.getTransaction()
                                                               .getLedgerId();
                    case HEADER:
                        return true;    // there can be only one header
                    default:
                        throw new IllegalStateException(
                                String.format(Locale.US, "Unexpected transaction item type %s",
                                        oldItem.getType()));
                }
            }
            @Override
            public boolean areContentsTheSame(@NonNull TransactionListItem oldItem,
                                              @NonNull TransactionListItem newItem) {
                switch (oldItem.getType()) {
                    case DELIMITER:
                        return oldItem.isMonthShown() == newItem.isMonthShown();
                    case TRANSACTION:
                        return oldItem.getTransaction()
                                      .equals(newItem.getTransaction()) &&
                               Misc.equalStrings(oldItem.getBoldAccountName(),
                                       newItem.getBoldAccountName());
                    case HEADER:
                        // headers don't differ in their contents. they observe the last update
                        // date and react to its changes
                        return true;
                    default:
                        throw new IllegalStateException(
                                String.format(Locale.US, "Unexpected transaction item type %s",
                                        oldItem.getType()));

                }
            }
        });
    }
    @Override
    public long getItemId(int position) {
        TransactionListItem item = listDiffer.getCurrentList()
                                             .get(position);
        switch (item.getType()) {
            case HEADER:
                return -1;
            case TRANSACTION:
                return item.getTransaction()
                           .getLedgerId();
            case DELIMITER:
                return -item.getDate()
                            .toDate()
                            .getTime();
            default:
                throw new IllegalStateException("Unexpected value: " + item.getType());
        }
    }
    @Override
    public int getItemViewType(int position) {
        return listDiffer.getCurrentList()
                         .get(position)
                         .getType()
                         .ordinal();
    }
    public void onBindViewHolder(@NonNull TransactionRowHolderBase holder, int position) {
        TransactionListItem item = listDiffer.getCurrentList()
                                             .get(position);

        // in a race when transaction value is reduced, but the model hasn't been notified yet
        // the view will disappear when the notifications reaches the model, so by simply omitting
        // the out-of-range get() call nothing bad happens - just a to-be-deleted view remains
        // a bit longer
        if (item == null)
            return;

        final TransactionListItem.Type newType = item.getType();

        switch (newType) {
            case TRANSACTION:
                holder.asTransaction()
                      .bind(item, item.getBoldAccountName());

                break;
            case DELIMITER:
                holder.asDelimiter()
                      .bind(item);
                break;
            case HEADER:
                holder.asHeader()
                      .bind();

                break;
            default:
                throw new IllegalStateException("Unexpected value: " + newType);
        }
    }
    @NonNull
    @Override
    public TransactionRowHolderBase onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        debug("perf", "onCreateViewHolder called");
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (TransactionListItem.Type.valueOf(viewType)) {
            case TRANSACTION:
                return new TransactionRowHolder(
                        TransactionListRowBinding.inflate(inflater, parent, false));
            case DELIMITER:
                return new TransactionListDelimiterRowHolder(
                        TransactionDelimiterBinding.inflate(inflater, parent, false));
            case HEADER:
                return new TransactionListLastUpdateRowHolder(
                        LastUpdateLayoutBinding.inflate(inflater, parent, false));
            default:
                throw new IllegalStateException("Unexpected value: " + viewType);
        }
    }

    @Override
    public int getItemCount() {
        return listDiffer.getCurrentList()
                         .size();
    }
    public void setTransactions(List<TransactionListItem> newList) {
        Logger.debug("transactions",
                String.format(Locale.US, "Got new transaction list (%d items)", newList.size()));
        listDiffer.submitList(newList);
    }
}