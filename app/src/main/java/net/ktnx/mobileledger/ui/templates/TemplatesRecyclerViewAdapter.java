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

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import net.ktnx.mobileledger.databinding.TemplateListTemplateItemBinding;
import net.ktnx.mobileledger.db.TemplateHeader;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TemplatesRecyclerViewAdapter extends RecyclerView.Adapter<TemplateViewHolder> {
    private final AsyncListDiffer<TemplateHeader> listDiffer;
    public TemplatesRecyclerViewAdapter() {
        listDiffer = new AsyncListDiffer<>(this, new DiffUtil.ItemCallback<TemplateHeader>() {
            @Override
            public boolean areItemsTheSame(@NotNull TemplateHeader oldItem,
                                           @NotNull TemplateHeader newItem) {
                return oldItem.getId()
                              .equals(newItem.getId());
            }
            @Override
            public boolean areContentsTheSame(@NotNull TemplateHeader oldItem,
                                              @NotNull TemplateHeader newItem) {
                return oldItem.equals(newItem);
            }
        });
    }
    @NonNull
    @Override
    public TemplateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TemplateListTemplateItemBinding b =
                TemplateListTemplateItemBinding.inflate(LayoutInflater.from(parent.getContext()),
                        parent, false);

        return new TemplateViewHolder(b);
    }
    @Override
    public void onBindViewHolder(@NonNull TemplateViewHolder holder, int position) {
        holder.bindToItem(listDiffer.getCurrentList()
                                    .get(position));
    }
    @Override
    public int getItemCount() {
        return listDiffer.getCurrentList()
                         .size();
    }
    public void setTemplates(List<TemplateHeader> newList) {
        listDiffer.submitList(newList);
    }
}
