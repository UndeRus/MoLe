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
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.ktnx.mobileledger.db.Profile;
import net.ktnx.mobileledger.model.Data;

abstract class NewTransactionItemViewHolder extends RecyclerView.ViewHolder {
    final Profile mProfile;
    public NewTransactionItemViewHolder(@NonNull View itemView) {
        super(itemView);
        mProfile = Data.getProfile();
    }
    @Nullable
    NewTransactionModel.Item getItem() {
        NewTransactionItemsAdapter adapter = (NewTransactionItemsAdapter) getBindingAdapter();
        if (adapter == null)
            return null;
        return adapter.getItem(getBindingAdapterPosition());
    }
    abstract public void bind(NewTransactionModel.Item item);
}
