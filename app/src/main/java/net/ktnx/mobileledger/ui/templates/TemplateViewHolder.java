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

package net.ktnx.mobileledger.ui.templates;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.ktnx.mobileledger.databinding.TemplateListTemplateItemBinding;
import net.ktnx.mobileledger.db.TemplateHeader;

class TemplateViewHolder extends RecyclerView.ViewHolder {
    final TemplateListTemplateItemBinding b;
    public TemplateViewHolder(@NonNull TemplateListTemplateItemBinding binding) {
        super(binding.getRoot());
        b = binding;
    }
    public void bindToItem(TemplateHeader item) {
        b.templateName.setText(item.getName());
        b.editButton.setOnClickListener(v -> {
            ((TemplatesActivity) v.getContext()).onEditTemplate(item.getId());
        });
    }
}
