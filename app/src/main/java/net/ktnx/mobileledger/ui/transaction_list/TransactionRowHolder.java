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

package net.ktnx.mobileledger.ui.transaction_list;

import android.text.format.DateUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.TransactionListItem;

import java.util.Observer;

class TransactionRowHolder extends RecyclerView.ViewHolder {
    final TextView tvDescription;
    final TextView tvComment;
    final LinearLayout tableAccounts;
    final ConstraintLayout row;
    final ConstraintLayout vDelimiter;
    final CardView vTransaction;
    final TextView tvDelimiterMonth, tvDelimiterDate;
    final View vDelimiterThick;
    final View vHeader;
    final TextView tvLastUpdate;
    TransactionListItem.Type lastType;
    private Observer lastUpdateObserver;
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
        this.vDelimiterThick = itemView.findViewById(R.id.transaction_delimiter_thick);
        this.vHeader = itemView.findViewById(R.id.last_update_container);
        this.tvLastUpdate = itemView.findViewById(R.id.last_update_text);
    }
    private void initLastUpdateObserver() {
        if (lastUpdateObserver != null)
            return;

        lastUpdateObserver = (o, arg) -> setLastUpdateText(Data.lastUpdate.get());

        Data.lastUpdate.addObserver(lastUpdateObserver);
    }
    void setLastUpdateText(long lastUpdate) {
        final int formatFlags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR |
                                DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_NUMERIC_DATE;
        tvLastUpdate.setText((lastUpdate == 0) ? "----"
                                               : DateUtils.formatDateTime(tvLastUpdate.getContext(),
                                                       lastUpdate, formatFlags));
    }
    private void dropLastUpdateObserver() {
        if (lastUpdateObserver == null)
            return;

        Data.lastUpdate.deleteObserver(lastUpdateObserver);
        lastUpdateObserver = null;
    }
    void setType(TransactionListItem.Type newType) {
        if (newType == lastType)
            return;

        switch (newType) {
            case TRANSACTION:
                vHeader.setVisibility(View.GONE);
                vTransaction.setVisibility(View.VISIBLE);
                vDelimiter.setVisibility(View.GONE);
                dropLastUpdateObserver();
                break;
            case DELIMITER:
                vHeader.setVisibility(View.GONE);
                vTransaction.setVisibility(View.GONE);
                vDelimiter.setVisibility(View.VISIBLE);
                dropLastUpdateObserver();
                break;
            case HEADER:
                vHeader.setVisibility(View.VISIBLE);
                vTransaction.setVisibility(View.GONE);
                vDelimiter.setVisibility(View.GONE);
                initLastUpdateObserver();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + newType);
        }

        lastType = newType;
    }
}
