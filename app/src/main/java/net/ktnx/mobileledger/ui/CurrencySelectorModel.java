/*
 * Copyright © 2020 Damyan Ivanov.
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

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import net.ktnx.mobileledger.model.Currency;

import java.util.ArrayList;
import java.util.List;

public class CurrencySelectorModel extends ViewModel {
    public final MutableLiveData<List<Currency>> currencies;
    private final MutableLiveData<Boolean> positionAndPaddingVisible = new MutableLiveData<>(true);
    private OnCurrencySelectedListener selectionListener;
    public CurrencySelectorModel() {
        this.currencies = new MutableLiveData<>(new ArrayList<>());
    }
    public void showPositionAndPadding() {
        positionAndPaddingVisible.postValue(true);
    }
    public void hidePositionAndPadding() {
        positionAndPaddingVisible.postValue(false);
    }
    public void observePositionAndPaddingVisible(LifecycleOwner activity,
                                                 Observer<Boolean> observer) {
        positionAndPaddingVisible.observe(activity, observer);
    }
    void setOnCurrencySelectedListener(OnCurrencySelectedListener listener) {
        selectionListener = listener;
    }
    void resetOnCurrencySelectedListener() {
        selectionListener = null;
    }
    void triggerOnCurrencySelectedListener(Currency c) {
        if (selectionListener != null)
            selectionListener.onCurrencySelected(c);
    }
}
