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
import net.ktnx.mobileledger.databinding.TemplatesFallbackDividerBinding;
import net.ktnx.mobileledger.db.TemplateHeader;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TemplatesRecyclerViewAdapter extends RecyclerView.Adapter<BaseTemplateViewHolder> {
    private static final int ITEM_TYPE_TEMPLATE = 1;
    private static final int ITEM_TYPE_DIVIDER = 2;
    private final AsyncListDiffer<BaseTemplateItem> listDiffer;

    public TemplatesRecyclerViewAdapter() {
        listDiffer = new AsyncListDiffer<>(this, new DiffUtil.ItemCallback<BaseTemplateItem>() {
            @Override
            public boolean areItemsTheSame(
                    @NotNull TemplatesRecyclerViewAdapter.BaseTemplateItem oldItem,
                    @NotNull TemplatesRecyclerViewAdapter.BaseTemplateItem newItem) {
                return oldItem.getId() == newItem.getId();
            }
            @Override
            public boolean areContentsTheSame(
                    @NotNull TemplatesRecyclerViewAdapter.BaseTemplateItem oldItem,
                    @NotNull TemplatesRecyclerViewAdapter.BaseTemplateItem newItem) {
                return oldItem.equals(newItem);
            }
        });
    }
    @NonNull
    @Override
    public BaseTemplateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case ITEM_TYPE_TEMPLATE:
                TemplateListTemplateItemBinding b =
                        TemplateListTemplateItemBinding.inflate(inflater, parent, false);
                return new BaseTemplateViewHolder.TemplateViewHolder(b);
            case ITEM_TYPE_DIVIDER:
                return new BaseTemplateViewHolder.TemplateDividerViewHolder(
                        TemplatesFallbackDividerBinding.inflate(inflater, parent, false));
            default:
                throw new RuntimeException("Can't handle " + viewType);
        }
    }
    @Override
    public void onBindViewHolder(@NonNull BaseTemplateViewHolder holder, int position) {
        holder.bindToItem(listDiffer.getCurrentList()
                                    .get(position));
    }
    @Override
    public int getItemViewType(int position) {
        BaseTemplateItem item = getItem(position);
        if (item instanceof TemplateItem)
            return ITEM_TYPE_TEMPLATE;
        if (item instanceof TemplateDivider)
            return ITEM_TYPE_DIVIDER;

        throw new RuntimeException("Can't handle " + item);
    }
    @Override
    public int getItemCount() {
        return listDiffer.getCurrentList()
                         .size();
    }
    public void setTemplates(List<TemplateHeader> newList) {
        List<BaseTemplateItem> itemList = new ArrayList<>();

        boolean reachedFallbackItems = false;

        for (TemplateHeader item : newList) {
            if (!reachedFallbackItems && item.isFallback()) {
                itemList.add(new TemplateDivider());
                reachedFallbackItems = true;
            }
            itemList.add(new TemplateItem(item));
        }

        listDiffer.submitList(itemList);
    }
    public BaseTemplateItem getItem(int position) {
        return listDiffer.getCurrentList()
                         .get(position);
    }

    static abstract class BaseTemplateItem {
        abstract long getId();

        abstract boolean equals(BaseTemplateItem other);
    }

    static class TemplateItem extends BaseTemplateItem {
        final TemplateHeader template;
        TemplateItem(TemplateHeader template) {this.template = template;}
        @Override
        long getId() {
            return template.getId();
        }
        @Override
        boolean equals(BaseTemplateItem other) {
            return template.equals(((TemplateItem) other).template);
        }
    }

    static class TemplateDivider extends BaseTemplateItem {
        @Override
        long getId() {
            return -1;
        }
        @Override
        boolean equals(BaseTemplateItem other) {
            return true;
        }
    }
}
