package net.ktnx.mobileledger;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

class AccountSummaryViewModel extends AndroidViewModel {
    private MobileLedgerDatabase dbh;
    private List<LedgerAccount> accounts;

    public AccountSummaryViewModel(@NonNull Application application) {
        super(application);
        dbh = new MobileLedgerDatabase(application);
    }

    List<LedgerAccount> getAccounts() {
        if (accounts == null) {
            accounts = new ArrayList<>();
            reloadAccounts();
        }

        return accounts;
    }

    void reloadAccounts() {
        accounts.clear();
        boolean showingHiddenAccounts =
                PreferenceManager.getDefaultSharedPreferences(getApplication())
                        .getBoolean("show_hidden_accounts", false);
        String sql = "SELECT name, hidden FROM accounts";
        if (!showingHiddenAccounts) sql += " WHERE hidden = 0";
        sql += " ORDER BY name";

        try (SQLiteDatabase db = dbh.getReadableDatabase()) {
            try (Cursor cursor = db
                    .rawQuery(sql,null))
            {
                while (cursor.moveToNext()) {
                    LedgerAccount acc = new LedgerAccount(cursor.getString(0));
                    acc.setHidden(cursor.getInt(1) == 1);
                    try (Cursor c2 = db.rawQuery(
                            "SELECT value, currency FROM account_values " + "WHERE account = ?",
                            new String[]{acc.getName()}))
                    {
                        while (c2.moveToNext()) {
                            acc.addAmount(c2.getFloat(0), c2.getString(1));
                        }
                    }
                    accounts.add(acc);
                }
            }
        }
    }
}

class AccountSummaryAdapter extends RecyclerView.Adapter<AccountSummaryAdapter.LedgerRowHolder> {
    private List<LedgerAccount> accounts;
    private boolean selectionActive;

    AccountSummaryAdapter(List<LedgerAccount> accounts) {
        this.accounts = accounts;
        this.selectionActive = false;
    }

    public void onBindViewHolder(@NonNull LedgerRowHolder holder, int position) {
        LedgerAccount acc = accounts.get(position);
        Context ctx = holder.row.getContext();
        Resources rm = ctx.getResources();

        holder.tvAccountName.setText(acc.getShortName());
        holder.tvAccountName.setPadding(
                acc.getLevel() * rm.getDimensionPixelSize(R.dimen.activity_horizontal_margin)/2,
                0, 0,
                0);
        holder.tvAccountAmounts.setText(acc.getAmountsString());

        if (acc.isHidden()) {
            holder.tvAccountName.setTypeface(null, Typeface.ITALIC);
            holder.tvAccountAmounts.setTypeface(null, Typeface.ITALIC);
        }
        else {
            holder.tvAccountName.setTypeface(null, Typeface.NORMAL);
            holder.tvAccountAmounts.setTypeface(null, Typeface.NORMAL);
        }

        if (position % 2 == 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) holder.row
                    .setBackgroundColor(rm.getColor(R.color.table_row_even_bg, ctx.getTheme()));
            else holder.row.setBackgroundColor(rm.getColor(R.color.table_row_even_bg));
        }
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) holder.row
                    .setBackgroundColor(rm.getColor(R.color.drawer_background, ctx.getTheme()));
            else holder.row.setBackgroundColor(rm.getColor(R.color.drawer_background));
        }

        holder.selectionCb.setVisibility( selectionActive ? View.VISIBLE : View.GONE);
        holder.selectionCb.setChecked(acc.isSelected());

        holder.row.setTag(R.id.POS, position);
    }

    @NonNull
    @Override
    public LedgerRowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.account_summary_row, parent, false);
        return new LedgerRowHolder(row);
    }

    @Override
    public int getItemCount() {
        return accounts.size();
    }

    public void startSelection() {
        for( LedgerAccount acc : accounts ) acc.setSelected(false);
        this.selectionActive = true;
        notifyDataSetChanged();
    }

    public void stopSelection() {
        this.selectionActive = false;
        notifyDataSetChanged();
    }

    public boolean isSelectionActive() {
        return selectionActive;
    }

    public void selectItem(int position) {
        accounts.get(position).toggleSelected();
        notifyItemChanged(position);
    }

//    @NonNull
//    @Override
//    public View getView(int position, @Nullable View row, @NonNull ViewGroup parent) {
//        LedgerAccount acc = getItem(position);
//        LedgerRowHolder holder;
//        if (row == null) {
//            holder = new LedgerRowHolder();
//            LayoutInflater vi = getSystemService(this.getContext(), LayoutInflater.class);
//            MenuInflater mi = getSystemService(this.getContext(), MenuInflater.class);
//
//            if (vi == null)
//                throw new IllegalStateException("Unable to instantiate the inflater " + "service");
//            row = vi.inflate(R.layout.account_summary_row, parent, false);
//            holder.tvAccountName = row.findViewById(R.id.account_row_acc_name);
//            holder.tvAccountAmounts = row.findViewById(R.id.account_row_acc_amounts);
//            row.setTag(R.id.VH, holder);
//
//            row.setPadding(context.getResources()
//                            .getDimensionPixelSize(R.dimen.activity_horizontal_margin)/2, dp2px
//                            (context, 3),
//                    context.getResources()
//                            .getDimensionPixelSize(R.dimen.activity_horizontal_margin)/2,
//                    dp2px(context, 4));
//            View.OnCreateContextMenuListener ccml = new View.OnCreateContextMenuListener() {
//                @Override
//                public void onCreateContextMenu(ContextMenu menu, View v,
//                                                ContextMenu.ContextMenuInfo menuInfo) {
//                    final ListView parent = (ListView) v.getParent();
//                    int pos = parent.getPositionForView(v);
//                    parent.setItemChecked(pos, true);
//                    Log.d("list", String.format("checking pos %d", pos));
//                }
//            };
//            row.setOnCreateContextMenuListener(ccml);
//
//        }
//        else holder = (LedgerRowHolder) row.getTag(R.id.VH);
//
//        holder.tvAccountName.setText(acc.getShortName());
//        holder.tvAccountName.setPadding(acc.getLevel() * context.getResources()
//                .getDimensionPixelSize(R.dimen.activity_horizontal_margin), 0, 0, 0);
//        holder.tvAccountAmounts.setText(acc.getAmountsString());
//
//        if (acc.isHidden()) {
//            holder.tvAccountName.setTypeface(null, Typeface.ITALIC);
//            holder.tvAccountAmounts.setTypeface(null, Typeface.ITALIC);
//        }
//        else {
//            holder.tvAccountName.setTypeface(null, Typeface.NORMAL);
//            holder.tvAccountAmounts.setTypeface(null, Typeface.NORMAL);
//        }
//
//        int real_pos = ((ListView)parent).getPositionForView(row);
//        Log.d("model", String.format("%s: real_pos=%d, position=%d", acc.getName(), real_pos,
//                position));
//        if (real_pos == -1) real_pos = position+1;
//        else real_pos = real_pos + 1;
//
//        if ( real_pos % 2 == 0) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                row.setBackgroundColor(context.getResources()
//                        .getColor(R.color.table_row_even_bg, context.getTheme()));
//            }
//            else {
//                row.setBackgroundColor(
//                        context.getResources().getColor(R.color.table_row_even_bg));
//            }
//        }
//        else {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                row.setBackgroundColor(context.getResources()
//                        .getColor(R.color.drawer_background, context.getTheme()));
//            }
//            else {
//                row.setBackgroundColor(context.getResources().getColor(R.color.drawer_background));
//            }
//        }
//
//        row.setTag(R.id.POS, position);
//
//        return row;
//    }

    class LedgerRowHolder extends RecyclerView.ViewHolder {
        CheckBox selectionCb;
        TextView tvAccountName, tvAccountAmounts;
        LinearLayout row;

        public LedgerRowHolder(@NonNull View itemView) {
            super(itemView);
            this.row = (LinearLayout) itemView;
            this.tvAccountName = itemView.findViewById(R.id.account_row_acc_name);
            this.tvAccountAmounts = itemView.findViewById(R.id.account_row_acc_amounts);
            this.selectionCb = itemView.findViewById(R.id.account_row_check);
        }
    }
}