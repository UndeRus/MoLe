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
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.model.Currency;

import org.jetbrains.annotations.NotNull;

/**
 * {@link RecyclerView.Adapter} that can display a {@link Currency} and makes a call to the
 * specified {@link OnCurrencySelectedListener}.
 */
public class CurrencySelectorRecyclerViewAdapter
        extends ListAdapter<Currency, CurrencySelectorRecyclerViewAdapter.ViewHolder> {

    private OnCurrencySelectedListener currencySelectedListener;
    private OnCurrencyLongClickListener currencyLongClickListener;
    public CurrencySelectorRecyclerViewAdapter() {
        super(Currency.DIFF_CALLBACK);
    }
    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                                  .inflate(R.layout.fragment_currency_selector, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.bindTo(getItem(position));
    }
    public void setCurrencySelectedListener(OnCurrencySelectedListener listener) {
        this.currencySelectedListener = listener;
    }
    public void resetCurrencySelectedListener() {
        currencySelectedListener = null;
    }
    public void notifyCurrencySelected(Currency currency) {
        if (null != currencySelectedListener)
            currencySelectedListener.onCurrencySelected(currency);
    }
    public void setCurrencyLongClickListener(OnCurrencyLongClickListener listener) {
        this.currencyLongClickListener = listener;
    }
    public void resetCurrencyLockClickListener() { currencyLongClickListener = null; }
    private void notifyCurrencyLongClicked(Currency mItem) {
        if (null != currencyLongClickListener)
            currencyLongClickListener.onCurrencyLongClick(mItem);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView mNameView;
        private Currency mItem;

        ViewHolder(View view) {
            super(view);
            mNameView = view.findViewById(R.id.content);

            view.setOnClickListener(v -> notifyCurrencySelected(mItem));
            view.setOnLongClickListener(v -> {
                notifyCurrencyLongClicked(mItem);
                return false;
            });
        }

        @NotNull
        @Override
        public String toString() {
            return super.toString() + " '" + mNameView.getText() + "'";
        }
        void bindTo(Currency item) {
            mItem = item;
            mNameView.setText(item.getName());
        }
    }
}
