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

package net.ktnx.mobileledger.ui.transaction_list;

import android.app.Activity;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import net.ktnx.mobileledger.App;
import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.model.LedgerTransactionAccount;
import net.ktnx.mobileledger.model.TransactionListItem;
import net.ktnx.mobileledger.ui.MainModel;
import net.ktnx.mobileledger.utils.Colors;
import net.ktnx.mobileledger.utils.Globals;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.Misc;
import net.ktnx.mobileledger.utils.SimpleDate;

import java.text.DateFormat;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class TransactionListAdapter extends RecyclerView.Adapter<TransactionRowHolder> {
    private final MainModel model;
    private final AsyncListDiffer<TransactionListItem> listDiffer;
    public TransactionListAdapter(MainModel model) {
        super();
        this.model = model;

        listDiffer = new AsyncListDiffer<>(this, new DiffUtil.ItemCallback<TransactionListItem>() {
            @Override
            public boolean areItemsTheSame(@NonNull TransactionListItem oldItem,
                                           @NonNull TransactionListItem newItem) {
                if (oldItem.getType() != newItem.getType())
                    return false;
                switch (oldItem.getType()) {
                    case DELIMITER:
                        return (oldItem.getDate()
                                       .equals(newItem.getDate()));
                    case TRANSACTION:
                        return oldItem.getTransaction()
                                      .getLedgerId() == newItem.getTransaction()
                                                               .getLedgerId();
                    case HEADER:
                        return true;    // there can be only one header
                    default:
                        throw new IllegalStateException(
                                String.format(Locale.US, "Unexpected transaction item type %s",
                                        oldItem.getType()));
                }
            }
            @Override
            public boolean areContentsTheSame(@NonNull TransactionListItem oldItem,
                                              @NonNull TransactionListItem newItem) {
                switch (oldItem.getType()) {
                    case DELIMITER:
                        // Delimiters items are "same" for same dates and the contents are the date
                        return true;
                    case TRANSACTION:
                        return oldItem.getTransaction()
                                      .equals(newItem.getTransaction());
                    case HEADER:
                        // headers don't differ in their contents. they observe the last update
                        // date and react to its changes
                        return true;
                    default:
                        throw new IllegalStateException(
                                String.format(Locale.US, "Unexpected transaction item type %s",
                                        oldItem.getType()));

                }
            }
        });
    }
    public void onBindViewHolder(@NonNull TransactionRowHolder holder, int position) {
        TransactionListItem item = listDiffer.getCurrentList()
                                             .get(position);

        // in a race when transaction value is reduced, but the model hasn't been notified yet
        // the view will disappear when the notifications reaches the model, so by simply omitting
        // the out-of-range get() call nothing bad happens - just a to-be-deleted view remains
        // a bit longer
        if (item == null)
            return;

        final TransactionListItem.Type newType = item.getType();
        holder.setType(newType);

        switch (newType) {
            case TRANSACTION:
                LedgerTransaction tr = item.getTransaction();

                //        debug("transactions", String.format("Filling position %d with %d
                //        accounts", position,
                //                tr.getAccounts().size()));

                TransactionLoader loader = new TransactionLoader();
                loader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                        new TransactionLoaderParams(tr, holder, position, model.getAccountFilter()
                                                                               .getValue()));

                // WORKAROUND what seems to be a bug in CardHolder somewhere
                // when a view that was previously holding a delimiter is re-purposed
                // occasionally it stays too short (not high enough)
                holder.vTransaction.measure(
                        View.MeasureSpec.makeMeasureSpec(holder.itemView.getWidth(),
                                View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                break;
            case DELIMITER:
                SimpleDate date = item.getDate();
                holder.tvDelimiterDate.setText(DateFormat.getDateInstance()
                                                         .format(date.toDate()));
                if (item.isMonthShown()) {
                    GregorianCalendar cal = new GregorianCalendar(TimeZone.getDefault());
                    cal.setTime(date.toDate());
                    App.prepareMonthNames();
                    holder.tvDelimiterMonth.setText(
                            Globals.monthNames[cal.get(GregorianCalendar.MONTH)]);
                    holder.tvDelimiterMonth.setVisibility(View.VISIBLE);
                    //                holder.vDelimiterLine.setBackgroundResource(R.drawable
                    //                .dashed_border_8dp);
                    holder.vDelimiterThick.setVisibility(View.VISIBLE);
                }
                else {
                    holder.tvDelimiterMonth.setVisibility(View.GONE);
                    //                holder.vDelimiterLine.setBackgroundResource(R.drawable
                    //                .dashed_border_1dp);
                    holder.vDelimiterThick.setVisibility(View.GONE);
                }
                break;
            case HEADER:
                holder.setLastUpdateText(Data.lastTransactionsUpdateText.getValue());

                break;
            default:
                throw new IllegalStateException("Unexpected value: " + newType);
        }
    }
    @NonNull
    @Override
    public TransactionRowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        debug("perf", "onCreateViewHolder called");
        View row = LayoutInflater.from(parent.getContext())
                                 .inflate(R.layout.transaction_list_row, parent, false);
        return new TransactionRowHolder(row);
    }

    @Override
    public int getItemCount() {
        return listDiffer.getCurrentList()
                         .size();
    }
    public void setTransactions(List<TransactionListItem> newList) {
        Logger.debug("transactions",
                String.format(Locale.US, "Got new transaction list (%d items)", newList.size()));
        listDiffer.submitList(newList);
    }
    enum LoaderStep {HEAD, ACCOUNTS, DONE}

    private static class TransactionLoader
            extends AsyncTask<TransactionLoaderParams, TransactionLoaderStep, Void> {
        @Override
        protected Void doInBackground(TransactionLoaderParams... p) {
            LedgerTransaction tr = p[0].transaction;

            SQLiteDatabase db = App.getDatabase();
            tr.loadData(db);

            publishProgress(new TransactionLoaderStep(p[0].holder, p[0].position, tr));

            int rowIndex = 0;
            // FIXME ConcurrentModificationException in ArrayList$ltr.next (ArrayList.java:831)
            for (LedgerTransactionAccount acc : tr.getAccounts()) {
//                debug(c.getAccountName(), acc.getAmount()));
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
                    holder.tvDescription.setText(step.getTransaction()
                                                     .getDescription());
                    String trComment = Misc.emptyIsNull(step.getTransaction()
                                                            .getComment());
                    if (trComment == null)
                        holder.tvComment.setVisibility(View.GONE);
                    else {
                        holder.tvComment.setText(trComment);
                        holder.tvComment.setVisibility(View.VISIBLE);
                    }

//                    if (step.isOdd())
//                        holder.row.setBackgroundColor(Colors.tableRowDarkBG);
//                    else
//                        holder.row.setBackgroundColor(Colors.tableRowLightBG);

                    break;
                case ACCOUNTS:
                    int rowIndex = step.getAccountPosition();
                    Context ctx = holder.row.getContext();
                    LinearLayout row = (LinearLayout) holder.tableAccounts.getChildAt(rowIndex);
                    if (row == null) {
                        row = new LinearLayout(ctx);
                        LayoutInflater inflater = ((Activity) ctx).getLayoutInflater();
                        inflater.inflate(R.layout.transaction_list_row_accounts_table_row, row);
                        holder.tableAccounts.addView(row);
                    }
                    TextView dummyText = row.findViewById(R.id.dummy_text);
                    TextView accName = row.findViewById(R.id.transaction_list_acc_row_acc_name);
                    TextView accComment =
                            row.findViewById(R.id.transaction_list_acc_row_acc_comment);
                    TextView accAmount = row.findViewById(R.id.transaction_list_acc_row_acc_amount);
                    LedgerTransactionAccount acc = step.getAccount();


//                    debug("tmp", String.format("showing acc row %d: %s %1.2f", rowIndex,
//                            acc.getAccountName(), acc.getAmount()));

                    String boldAccountName = step.getBoldAccountName();
                    if ((boldAccountName != null) && acc.getAccountName()
                                                        .startsWith(boldAccountName))
                    {
                        accName.setTextColor(Colors.secondary);
                        accAmount.setTextColor(Colors.secondary);

                        SpannableString ss = new SpannableString(acc.getAccountName());
                        ss.setSpan(new StyleSpan(Typeface.BOLD), 0, boldAccountName.length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        accName.setText(ss);
                    }
                    else {
                        @ColorInt int textColor = dummyText.getTextColors()
                                                           .getDefaultColor();
                        accName.setTextColor(textColor);
                        accAmount.setTextColor(textColor);
                        accName.setText(acc.getAccountName());
                    }

                    String comment = acc.getComment();
                    if (comment != null && !comment.isEmpty()) {
                        accComment.setText(comment);
                        accComment.setVisibility(View.VISIBLE);
                    }
                    else {
                        accComment.setVisibility(View.GONE);
                    }
                    accAmount.setText(acc.toString());

                    break;
                case DONE:
                    int accCount = step.getAccountCount();
                    if (holder.tableAccounts.getChildCount() > accCount) {
                        holder.tableAccounts.removeViews(accCount,
                                holder.tableAccounts.getChildCount() - accCount);
                    }

//                    debug("transactions",
//                            String.format("Position %d fill done", step.getPosition()));
            }
        }
    }

    private static class TransactionLoaderParams {
        final LedgerTransaction transaction;
        final TransactionRowHolder holder;
        final int position;
        final String boldAccountName;
        TransactionLoaderParams(LedgerTransaction transaction, TransactionRowHolder holder,
                                int position, String boldAccountName) {
            this.transaction = transaction;
            this.holder = holder;
            this.position = position;
            this.boldAccountName = boldAccountName;
        }
    }
}