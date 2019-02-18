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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.model.LedgerTransactionAccount;
import net.ktnx.mobileledger.model.TransactionListItem;
import net.ktnx.mobileledger.utils.Colors;
import net.ktnx.mobileledger.utils.Globals;
import net.ktnx.mobileledger.utils.MLDB;

import java.text.DateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static net.ktnx.mobileledger.utils.DimensionUtils.dp2px;

public class TransactionListAdapter extends RecyclerView.Adapter<TransactionRowHolder> {
    private String boldAccountName;
    public void onBindViewHolder(@NonNull TransactionRowHolder holder, int position) {
        TransactionListItem item = TransactionListViewModel.getTransactionListItem(position);

        // in a race when transaction value is reduced, but the model hasn't been notified yet
        // the view will disappear when the notifications reaches the model, so by simply omitting
        // the out-of-range get() call nothing bad happens - just a to-be-deleted view remains
        // a bit longer
        if (item == null) return;

        switch (item.getType()) {
            case TRANSACTION:
                holder.vTransaction.setVisibility(View.VISIBLE);
                holder.vDelimiter.setVisibility(View.GONE);
                holder.vTrailer.setVisibility(View.GONE);
                LedgerTransaction tr = item.getTransaction();

                //        Log.d("transactions", String.format("Filling position %d with %d accounts", position,
                //                tr.getAccounts().size()));

                TransactionLoader loader = new TransactionLoader();
                loader.execute(new TransactionLoaderParams(tr, holder, position, boldAccountName,
                        item.isOdd()));

                // WORKAROUND what seems to be a bug in CardHolder somewhere
                // when a view that was previously holding a delimiter is re-purposed
                // occasionally it stays too short (not high enough)
                holder.vTransaction.measure(View.MeasureSpec
                                .makeMeasureSpec(holder.itemView.getWidth(), View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                break;
            case DELIMITER:
                Date date = item.getDate();
                holder.vTransaction.setVisibility(View.GONE);
                holder.vTrailer.setVisibility(View.GONE);
                holder.vDelimiter.setVisibility(View.VISIBLE);
                holder.tvDelimiterDate.setText(DateFormat.getDateInstance().format(date));
                if (item.isMonthShown()) {
                    GregorianCalendar cal = new GregorianCalendar(TimeZone.getDefault());
                    cal.setTime(date);
                    holder.tvDelimiterMonth
                            .setText(Globals.monthNames[cal.get(GregorianCalendar.MONTH)]);
                    holder.tvDelimiterMonth.setVisibility(View.VISIBLE);
                    //                holder.vDelimiterLine.setBackgroundResource(R.drawable.dashed_border_8dp);
                    holder.vDelimiterLine.setVisibility(View.GONE);
                    holder.vDelimiterThick.setVisibility(View.VISIBLE);
                }
                else {
                    holder.tvDelimiterMonth.setVisibility(View.GONE);
                    //                holder.vDelimiterLine.setBackgroundResource(R.drawable.dashed_border_1dp);
                    holder.vDelimiterLine.setVisibility(View.VISIBLE);
                    holder.vDelimiterThick.setVisibility(View.GONE);
                }
                break;
            case TRAILER:
                holder.vTransaction.setVisibility(View.GONE);
                holder.vTrailer.setVisibility(View.VISIBLE);
                holder.vDelimiter.setVisibility(View.GONE);

                break;
        }
    }

    @NonNull
    @Override
    public TransactionRowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        Log.d("perf", "onCreateViewHolder called");
        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.transaction_list_row, parent, false);
        return new TransactionRowHolder(row);
    }

    @Override
    public int getItemCount() {
        return TransactionListViewModel.getTransactionCount() + 1;
    }
    public void setBoldAccountName(String boldAccountName) {
        this.boldAccountName = boldAccountName;
    }
    public void resetBoldAccountName() {
        this.boldAccountName = null;
    }

    enum LoaderStep {HEAD, ACCOUNTS, DONE}

    private static class TransactionLoader
            extends AsyncTask<TransactionLoaderParams, TransactionLoaderStep, Void> {
        @Override
        protected Void doInBackground(TransactionLoaderParams... p) {
            LedgerTransaction tr = p[0].transaction;
            boolean odd = p[0].odd;

            SQLiteDatabase db = MLDB.getReadableDatabase();
            tr.loadData(db);

            publishProgress(new TransactionLoaderStep(p[0].holder, p[0].position, tr, odd));

            int rowIndex = 0;
            for (LedgerTransactionAccount acc : tr.getAccounts()) {
//                Log.d(c.getAccountName(), acc.getAmount()));
                publishProgress(new TransactionLoaderStep(p[0].holder, acc, rowIndex++,
                        p[0].boldAccountName));
            }

            publishProgress(new TransactionLoaderStep(p[0].holder, p[0].position, rowIndex));

            return null;
        }
        @Override
        protected void onProgressUpdate(TransactionLoaderStep... values) {
            super.onProgressUpdate(values);
            TransactionLoaderStep step = values[0];
            TransactionRowHolder holder = step.getHolder();

            switch (step.getStep()) {
                case HEAD:
                    holder.tvDescription.setText(step.getTransaction().getDescription());

                    if (step.isOdd()) holder.row.setBackgroundColor(Colors.tableRowDarkBG);
                    else holder.row.setBackgroundColor(Colors.tableRowLightBG);

                    break;
                case ACCOUNTS:
                    int rowIndex = step.getAccountPosition();
                    Context ctx = holder.row.getContext();
                    LinearLayout row = (LinearLayout) holder.tableAccounts.getChildAt(rowIndex);
                    TextView accName, accAmount;
                    if (row == null) {
                        row = new LinearLayout(ctx);
                        row.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT));
                        row.setGravity(Gravity.CENTER_VERTICAL);
                        row.setOrientation(LinearLayout.HORIZONTAL);
                        row.setPaddingRelative(dp2px(ctx, 8), 0, 0, 0);
                        accName = new AppCompatTextView(ctx);
                        accName.setLayoutParams(new LinearLayout.LayoutParams(0,
                                LinearLayout.LayoutParams.WRAP_CONTENT, 5f));
                        accName.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                        row.addView(accName);
                        accAmount = new AppCompatTextView(ctx);
                        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        llp.setMarginEnd(0);
                        accAmount.setLayoutParams(llp);
                        accAmount.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
                        accAmount.setMinWidth(dp2px(ctx, 60));
                        row.addView(accAmount);
                        holder.tableAccounts.addView(row);
                    }
                    else {
                        accName = (TextView) row.getChildAt(0);
                        accAmount = (TextView) row.getChildAt(1);
                    }
                    LedgerTransactionAccount acc = step.getAccount();


//                    Log.d("tmp", String.format("showing acc row %d: %s %1.2f", rowIndex,
//                            acc.getAccountName(), acc.getAmount()));

                    String boldAccountName = step.getBoldAccountName();
                    if ((boldAccountName != null) &&
                        acc.getAccountName().startsWith(boldAccountName))
                    {
                        accName.setTextColor(Colors.accent);
                        accAmount.setTextColor(Colors.accent);

                        SpannableString ss = new SpannableString(acc.getAccountName());
                        ss.setSpan(new StyleSpan(Typeface.BOLD), 0, boldAccountName.length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        accName.setText(ss);
                    }
                    else {
                        accName.setTextColor(Colors.defaultTextColor);
                        accAmount.setTextColor(Colors.defaultTextColor);
                        accName.setText(acc.getAccountName());
                    }
                    accAmount.setText(acc.toString());

                    break;
                case DONE:
                    int accCount = step.getAccountCount();
                    if (holder.tableAccounts.getChildCount() > accCount) {
                        holder.tableAccounts.removeViews(accCount,
                                holder.tableAccounts.getChildCount() - accCount);
                    }

//                    Log.d("transactions",
//                            String.format("Position %d fill done", step.getPosition()));
            }
        }
    }

    private class TransactionLoaderParams {
        LedgerTransaction transaction;
        TransactionRowHolder holder;
        int position;
        String boldAccountName;
        boolean odd;
        TransactionLoaderParams(LedgerTransaction transaction, TransactionRowHolder holder,
                                int position, String boldAccountName, boolean odd) {
            this.transaction = transaction;
            this.holder = holder;
            this.position = position;
            this.boldAccountName = boldAccountName;
            this.odd = odd;
        }
    }
}