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

package net.ktnx.mobileledger.ui.new_transaction;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import net.ktnx.mobileledger.databinding.NewTransactionAccountRowBinding;
import net.ktnx.mobileledger.databinding.NewTransactionHeaderRowBinding;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
import net.ktnx.mobileledger.utils.Logger;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

class NewTransactionItemsAdapter extends RecyclerView.Adapter<NewTransactionItemViewHolder> {
    private static final int ITEM_VIEW_TYPE_HEADER = 1;
    private static final int ITEM_VIEW_TYPE_ACCOUNT = 2;
    final NewTransactionModel model;
    private final ItemTouchHelper touchHelper;
    private final AsyncListDiffer<NewTransactionModel.Item> differ =
            new AsyncListDiffer<>(this, new DiffUtil.ItemCallback<NewTransactionModel.Item>() {
                @Override
                public boolean areItemsTheSame(@NonNull NewTransactionModel.Item oldItem,
                                               @NonNull NewTransactionModel.Item newItem) {
//                    Logger.debug("new-trans",
//                            String.format("comparing ids of {%s} and {%s}", oldItem.toString(),
//                                    newItem.toString()));
                    return oldItem.getId() == newItem.getId();
                }
                @Override
                public boolean areContentsTheSame(@NonNull NewTransactionModel.Item oldItem,
                                                  @NonNull NewTransactionModel.Item newItem) {

//                    Logger.debug("new-trans",
//                            String.format("comparing contents of {%s} and {%s}", oldItem
//                            .toString(),
//                                    newItem.toString()));
                    return oldItem.equalContents(newItem);
                }
            });
    private MobileLedgerProfile mProfile;
    private int checkHoldCounter = 0;
    NewTransactionItemsAdapter(NewTransactionModel viewModel, MobileLedgerProfile profile) {
        super();
        setHasStableIds(true);
        model = viewModel;
        mProfile = profile;


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

                // first item is immovable
                if (adapterPosition == 0)
                    return false;

                return super.canDropOver(recyclerView, current, target);
            }
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView,
                                        @NonNull RecyclerView.ViewHolder viewHolder) {
                int flags = makeFlag(ItemTouchHelper.ACTION_STATE_IDLE, ItemTouchHelper.END);
                // the top (date and description) and the bottom (padding) items are always there
                final int adapterPosition = viewHolder.getAdapterPosition();
                if (adapterPosition > 0) {
                    flags |= makeFlag(ItemTouchHelper.ACTION_STATE_DRAG,
                            ItemTouchHelper.UP | ItemTouchHelper.DOWN) |
                             makeFlag(ItemTouchHelper.ACTION_STATE_SWIPE,
                                     ItemTouchHelper.START | ItemTouchHelper.END);
                }

                return flags;
            }
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {

                model.moveItem(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                return true;
            }
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                viewModel.removeItem(pos);
            }
        });
    }
    @Override
    public int getItemViewType(int position) {
        final ItemType type = differ.getCurrentList()
                                    .get(position)
                                    .getType();
        switch (type) {
            case generalData:
                return ITEM_VIEW_TYPE_HEADER;
            case transactionRow:
                return ITEM_VIEW_TYPE_ACCOUNT;
            default:
                throw new RuntimeException("Can't handle " + type);
        }
    }
    @Override
    public long getItemId(int position) {
        return differ.getCurrentList()
                     .get(position)
                     .getId();
    }
    public void setProfile(MobileLedgerProfile profile) {
        mProfile = profile;
    }
    @NonNull
    @Override
    public NewTransactionItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                           int viewType) {
        switch (viewType) {
            case ITEM_VIEW_TYPE_HEADER:
                NewTransactionHeaderRowBinding headerBinding =
                        NewTransactionHeaderRowBinding.inflate(
                                LayoutInflater.from(parent.getContext()), parent, false);
                final NewTransactionHeaderItemHolder headerHolder =
                        new NewTransactionHeaderItemHolder(headerBinding, this);
                Logger.debug("new-trans", "Creating new Header ViewHolder " +
                                          Integer.toHexString(headerHolder.hashCode()));
                return headerHolder;
            case ITEM_VIEW_TYPE_ACCOUNT:
                NewTransactionAccountRowBinding accBinding =
                        NewTransactionAccountRowBinding.inflate(
                                LayoutInflater.from(parent.getContext()), parent, false);
                final NewTransactionAccountRowItemHolder accHolder =
                        new NewTransactionAccountRowItemHolder(accBinding, this);
                Logger.debug("new-trans", "Creating new AccountRow ViewHolder " +
                                          Integer.toHexString(accHolder.hashCode()));
                return accHolder;
            default:
                throw new RuntimeException("Cant handle view type " + viewType);
        }
    }
    @Override
    public void onBindViewHolder(@NonNull NewTransactionItemViewHolder holder, int position) {
        Logger.debug("bind",
                String.format(Locale.US, "Binding item at position %d, holder %s", position,
                        Integer.toHexString(holder.hashCode())));
        NewTransactionModel.Item item = Objects.requireNonNull(differ.getCurrentList()
                                                                     .get(position));
        holder.bind(item);
        Logger.debug("bind", String.format(Locale.US, "Bound %s item at position %d", item.getType()
                                                                                          .toString(),
                position));
    }
    @Override
    public int getItemCount() {
        return differ.getCurrentList()
                     .size();
    }
    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        touchHelper.attachToRecyclerView(recyclerView);
    }
    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        touchHelper.attachToRecyclerView(null);
        super.onDetachedFromRecyclerView(recyclerView);
    }
    void noteFocusIsOnAccount(int position) {
        model.noteFocusChanged(position, FocusedElement.Account);
    }
    void noteFocusIsOnAmount(int position) {
        model.noteFocusChanged(position, FocusedElement.Amount);
    }
    void noteFocusIsOnComment(int position) {
        model.noteFocusChanged(position, FocusedElement.Comment);
    }
    void noteFocusIsOnTransactionComment(int position) {
        model.noteFocusChanged(position, FocusedElement.TransactionComment);
    }
    public void noteFocusIsOnDescription(int pos) {
        model.noteFocusChanged(pos, FocusedElement.Description);
    }
    private void holdSubmittableChecks() {
        checkHoldCounter++;
    }
    private void releaseSubmittableChecks() {
        if (checkHoldCounter == 0)
            throw new RuntimeException("Asymmetrical call to releaseSubmittableChecks");
        checkHoldCounter--;
    }
    void setItemCurrency(int position, String newCurrency) {
        model.setItemCurrency(position, newCurrency);
    }

    public void setItems(List<NewTransactionModel.Item> newList) {
        Logger.debug("new-trans", "adapter: submitting new item list");
        differ.submitList(newList);
    }
    public NewTransactionModel.Item getItem(int position) {
        return differ.getCurrentList()
                     .get(position);
    }
}
