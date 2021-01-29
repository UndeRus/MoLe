/*
 * Copyright © 2019 Damyan Ivanov.
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

package net.ktnx.mobileledger.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import net.ktnx.mobileledger.databinding.FragmentPatternDetailSourceSelectorBinding;
import net.ktnx.mobileledger.model.PatternDetailSource;

import org.jetbrains.annotations.NotNull;

/**
 * {@link RecyclerView.Adapter} that can display a {@link PatternDetailSource} and makes a call
 * to the
 * specified {@link OnSourceSelectedListener}.
 */
public class PatternDetailSourceSelectorRecyclerViewAdapter extends
        ListAdapter<PatternDetailSource,
                PatternDetailSourceSelectorRecyclerViewAdapter.ViewHolder> {

    private OnSourceSelectedListener sourceSelectedListener;
    public PatternDetailSourceSelectorRecyclerViewAdapter() {
        super(PatternDetailSource.DIFF_CALLBACK);
    }
    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        FragmentPatternDetailSourceSelectorBinding b =
                FragmentPatternDetailSourceSelectorBinding.inflate(
                        LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(b);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.bindTo(getItem(position));
    }
    public void setSourceSelectedListener(OnSourceSelectedListener listener) {
        this.sourceSelectedListener = listener;
    }
    public void resetSourceSelectedListener() {
        sourceSelectedListener = null;
    }
    public void notifySourceSelected(PatternDetailSource item) {
        if (null != sourceSelectedListener)
            sourceSelectedListener.onSourceSelected(false, item.getGroupNumber());
    }
    public void notifyLiteralSelected() {
        if (null != sourceSelectedListener)
            sourceSelectedListener.onSourceSelected(true, (short) -1);
    }
    public class ViewHolder extends RecyclerView.ViewHolder {
        private final FragmentPatternDetailSourceSelectorBinding b;
        private PatternDetailSource mItem;

        ViewHolder(FragmentPatternDetailSourceSelectorBinding binding) {
            super(binding.getRoot());
            b = binding;

            b.getRoot()
             .setOnClickListener(v -> notifySourceSelected(mItem));
        }

        @NotNull
        @Override
        public String toString() {
            return super.toString() + " " + b.groupNumber.getText() + ": '" +
                   b.matchedText.getText() + "'";
        }
        void bindTo(PatternDetailSource item) {
            mItem = item;
            b.groupNumber.setText(String.valueOf(item.getGroupNumber()));
            b.matchedText.setText(item.getMatchedText());
        }
    }
}
