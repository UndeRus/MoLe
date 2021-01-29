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

package net.ktnx.mobileledger.ui.patterns;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.ktnx.mobileledger.databinding.PatternLayoutBinding;
import net.ktnx.mobileledger.model.PatternEntry;

class PatternViewHolder extends RecyclerView.ViewHolder {
    final PatternLayoutBinding b;
    public PatternViewHolder(@NonNull PatternLayoutBinding binding) {
        super(binding.getRoot());
        b = binding;
    }
    public void bindToItem(PatternEntry item) {
        b.patternName.setText(item.getName());
        b.editButon.setOnClickListener(v -> {
            ((PatternsActivity) v.getContext()).onEditPattern(item.getId());
        });
    }
}
