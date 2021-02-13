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
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import net.ktnx.mobileledger.R;
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
        b.templateName.setOnClickListener(
                v -> ((TemplatesActivity) v.getContext()).onEditTemplate(item.getId()));
        b.templateName.setOnLongClickListener((v) -> {
            TemplatesActivity activity = (TemplatesActivity) v.getContext();
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            final String templateName = item.getName();
            builder.setTitle(templateName);
            builder.setItems(R.array.templates_ctx_menu, (dialog, which) -> {
                if (which == 0) { // edit
                    activity.onEditTemplate(item.getId());
                }
                else if (which == 1) { // duplicate
                    activity.onDuplicateTemplate(item.getId());
                }
                else if (which == 2) { // delete
                    activity.onDeleteTemplate(item.getId());
                }
                else {
                    throw new RuntimeException(String.format("Unknown menu item id (%d)", which));
                }
                dialog.dismiss();
            });
            builder.show();
            return true;
        });
    }
}
