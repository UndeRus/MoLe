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

import net.ktnx.mobileledger.databinding.LastUpdateLayoutBinding;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.ui.activity.MainActivity;

class TransactionListLastUpdateRowHolder extends TransactionRowHolderBase {
    private final LastUpdateLayoutBinding b;
    TransactionListLastUpdateRowHolder(LastUpdateLayoutBinding binding) {
        super(binding.getRoot());
        b = binding;
    }
    void setLastUpdateText(String text) {
        b.lastUpdateText.setText(text);
    }
    public void bind() {
        Data.lastTransactionsUpdateText.observe((MainActivity) b.lastUpdateText.getContext(),
                b.lastUpdateText::setText);
    }
}
