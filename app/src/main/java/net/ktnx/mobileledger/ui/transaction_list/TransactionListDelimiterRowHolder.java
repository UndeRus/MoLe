/*
 * Copyright © 2024 Damyan Ivanov.
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

import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;

import net.ktnx.mobileledger.App;
import net.ktnx.mobileledger.databinding.TransactionDelimiterBinding;
import net.ktnx.mobileledger.model.TransactionListItem;
import net.ktnx.mobileledger.utils.DimensionUtils;
import net.ktnx.mobileledger.utils.Globals;
import net.ktnx.mobileledger.utils.SimpleDate;

import java.text.DateFormat;
import java.util.GregorianCalendar;
import java.util.TimeZone;

class TransactionListDelimiterRowHolder extends TransactionRowHolderBase {
    private final TransactionDelimiterBinding b;
    TransactionListDelimiterRowHolder(TransactionDelimiterBinding binding) {
        super(binding.getRoot());
        b = binding;
    }
    public void bind(TransactionListItem item) {
        SimpleDate date = item.getDate();
        b.transactionDelimiterDate.setText(DateFormat.getDateInstance()
                                                     .format(date.toDate()));
        if (item.isMonthShown()) {
            GregorianCalendar cal = new GregorianCalendar(TimeZone.getDefault());
            cal.setTime(date.toDate());
            App.prepareMonthNames();
            b.transactionDelimiterMonth.setText(
                    Globals.monthNames[cal.get(GregorianCalendar.MONTH)]);
            b.transactionDelimiterMonth.setVisibility(View.VISIBLE);
            b.transactionDelimiterThick.setVisibility(View.VISIBLE);
            ConstraintLayout.LayoutParams lp =
                    (ConstraintLayout.LayoutParams) b.transactionDelimiterThick.getLayoutParams();
            lp.height = DimensionUtils.dp2px(b.getRoot()
                                              .getContext(), 4);
            b.transactionDelimiterThick.setLayoutParams(lp);
        }
        else {
            b.transactionDelimiterMonth.setVisibility(View.GONE);
            ConstraintLayout.LayoutParams lp =
                    (ConstraintLayout.LayoutParams) b.transactionDelimiterThick.getLayoutParams();
            lp.height = DimensionUtils.dp2px(b.getRoot()
                                              .getContext(), 1.3f);
            b.transactionDelimiterThick.setLayoutParams(lp);
            b.transactionDelimiterThick.setVisibility(View.VISIBLE);
        }

    }
}
