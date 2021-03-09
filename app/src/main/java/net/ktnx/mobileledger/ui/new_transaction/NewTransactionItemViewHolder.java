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

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.MobileLedgerProfile;

abstract class NewTransactionItemViewHolder extends RecyclerView.ViewHolder {
    final NewTransactionItemsAdapter mAdapter;
    final MobileLedgerProfile mProfile;
    public NewTransactionItemViewHolder(@NonNull View itemView,
                                        NewTransactionItemsAdapter adapter) {
        super(itemView);
        mAdapter = adapter;
        mProfile = Data.getProfile();
    }
    NewTransactionModel.Item getItem() {
        return mAdapter.getItem(getAdapterPosition());
//        return Objects.requireNonNull(mAdapter.model.getItems()
//                                                    .getValue())
//                      .get(getAdapterPosition());
    }
    abstract public void bind(NewTransactionModel.Item item);
}
