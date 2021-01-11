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

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import net.ktnx.mobileledger.databinding.PatternLayoutBinding;
import net.ktnx.mobileledger.model.PatternEntry;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class PatternsRecyclerViewAdapter extends RecyclerView.Adapter<PatternViewHolder> {
    private final AsyncListDiffer<PatternEntry> listDiffer;
    public PatternsRecyclerViewAdapter() {
        listDiffer = new AsyncListDiffer<>(this, new DiffUtil.ItemCallback<PatternEntry>() {
            @Override
            public boolean areItemsTheSame(@NotNull PatternEntry oldItem,
                                           @NotNull PatternEntry newItem) {
                return oldItem.getId() == newItem.getId();
            }
            @Override
            public boolean areContentsTheSame(@NotNull PatternEntry oldItem,
                                              @NotNull PatternEntry newItem) {
                return Objects.equals(oldItem.getName(), newItem.getName());
            }
        });
    }
    @NonNull
    @Override
    public PatternViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        PatternLayoutBinding b =
                PatternLayoutBinding.inflate(LayoutInflater.from(parent.getContext()), parent,
                        false);

        return new PatternViewHolder(b);
    }
    @Override
    public void onBindViewHolder(@NonNull PatternViewHolder holder, int position) {
        holder.bindToItem(listDiffer.getCurrentList()
                                    .get(position));
    }
    @Override
    public int getItemCount() {
        return listDiffer.getCurrentList()
                         .size();
    }
    public void setPatterns(List<PatternEntry> newList) {
        listDiffer.submitList(newList);
    }
}
