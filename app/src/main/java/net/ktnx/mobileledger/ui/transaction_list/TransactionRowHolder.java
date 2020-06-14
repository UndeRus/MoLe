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

package net.ktnx.mobileledger.ui.transaction_list;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.ktnx.mobileledger.R;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

class TransactionRowHolder extends RecyclerView.ViewHolder {
    TextView tvDescription;
    TextView tvComment;
    LinearLayout tableAccounts;
    ConstraintLayout row;
    ConstraintLayout vDelimiter;
    CardView vTransaction;
    TextView tvDelimiterMonth, tvDelimiterDate;
    View vDelimiterLine, vDelimiterThick;
    public TransactionRowHolder(@NonNull View itemView) {
        super(itemView);
        this.row = itemView.findViewById(R.id.transaction_row);
        this.tvDescription = itemView.findViewById(R.id.transaction_row_description);
        this.tvComment = itemView.findViewById(R.id.transaction_comment);
        this.tableAccounts = itemView.findViewById(R.id.transaction_row_acc_amounts);
        this.vDelimiter = itemView.findViewById(R.id.transaction_delimiter);
        this.vTransaction = itemView.findViewById(R.id.transaction_card_view);
        this.tvDelimiterDate = itemView.findViewById(R.id.transaction_delimiter_date);
        this.tvDelimiterMonth = itemView.findViewById(R.id.transaction_delimiter_month);
        this.vDelimiterLine = itemView.findViewById(R.id.transaction_delimiter_line);
        this.vDelimiterThick = itemView.findViewById(R.id.transaction_delimiter_thick);
    }
}
