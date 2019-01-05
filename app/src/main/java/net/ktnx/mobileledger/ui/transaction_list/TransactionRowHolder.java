/*
 * Copyright © 2019 Damyan Ivanov.
 * This file is part of Mobile-Ledger.
 * Mobile-Ledger is free software: you can distribute it and/or modify it
 * under the term of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your opinion), any later version.
 *
 * Mobile-Ledger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License terms for details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mobile-Ledger. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ktnx.mobileledger.ui.transaction_list;

import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.ktnx.mobileledger.R;

class TransactionRowHolder extends RecyclerView.ViewHolder {
    TextView tvDescription, tvDate;
    LinearLayout tableAccounts;
    ConstraintLayout row;
    public TransactionRowHolder(@NonNull View itemView) {
        super(itemView);
        this.row = itemView.findViewById(R.id.transaction_row);
        this.tvDescription = itemView.findViewById(R.id.transaction_row_description);
        this.tvDate = itemView.findViewById(R.id.transaction_row_date);
        this.tableAccounts = itemView.findViewById(R.id.transaction_row_acc_amounts);
    }
}