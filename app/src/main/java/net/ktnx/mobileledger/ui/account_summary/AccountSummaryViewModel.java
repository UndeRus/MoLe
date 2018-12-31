/*
 * Copyright © 2018 Damyan Ivanov.
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

package net.ktnx.mobileledger.ui.account_summary;

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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.model.LedgerAccount;
import net.ktnx.mobileledger.utils.MLDB;

import java.util.ArrayList;
import java.util.List;

import static net.ktnx.mobileledger.ui.activity.SettingsActivity.PREF_KEY_SHOW_ONLY_STARRED_ACCOUNTS;

class AccountSummaryViewModel extends AndroidViewModel {
    private List<LedgerAccount> accounts;

    public AccountSummaryViewModel(@NonNull Application application) {
        super(application);
    }

    List<LedgerAccount> getAccounts(Context context) {
        if (accounts == null) {
            accounts = new ArrayList<>();
            reloadAccounts(context);
        }

        return accounts;
    }

    void reloadAccounts(Context context) {
        accounts.clear();
        boolean showingOnlyStarred =
                PreferenceManager.getDefaultSharedPreferences(getApplication())
                        .getBoolean(PREF_KEY_SHOW_ONLY_STARRED_ACCOUNTS, false);
        String sql = "SELECT name, hidden FROM accounts";
        if (showingOnlyStarred) sql += " WHERE hidden = 0";
        sql += " ORDER BY name";

        try (SQLiteDatabase db = MLDB.getReadableDatabase()) {
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
    void commitSelections(Context context) {
        try(SQLiteDatabase db = MLDB.getWritableDatabase(context)) {
            db.beginTransaction();
            try {
                for (LedgerAccount acc : accounts) {
                    Log.d("db", String.format("Setting %s to %s", acc.getName(),
                            acc.isHidden() ? "hidden" : "starred"));
                    db.execSQL("UPDATE accounts SET hidden=? WHERE name=?",
                            new Object[]{acc.isHiddenToBe() ? 1 : 0, acc.getName()});
                }
                db.setTransactionSuccessful();
                for (LedgerAccount acc : accounts ) { acc.setHidden(acc.isHiddenToBe()); }
            }
            finally { db.endTransaction(); }
        }
    }
}

class AccountSummaryAdapter extends RecyclerView.Adapter<AccountSummaryAdapter
.LedgerRowHolder> {
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
                acc.getLevel() * rm.getDimensionPixelSize(R.dimen.activity_horizontal_margin) / 2,
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
        holder.selectionCb.setChecked(!acc.isHiddenToBe());

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
        for( LedgerAccount acc : accounts ) acc.setHiddenToBe(acc.isHidden());
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
        LedgerAccount acc = accounts.get(position);
        acc.toggleHiddenToBe();
        toggleChildrenOf(acc, acc.isHiddenToBe());
        notifyDataSetChanged();
    }
    void toggleChildrenOf(LedgerAccount parent, boolean hiddenToBe) {
        for (LedgerAccount acc : accounts) {
            String acc_parent = acc.getParentName();
            if ((acc_parent != null) && acc.getParentName().equals(parent.getName())) {
                acc.setHiddenToBe(hiddenToBe);
                toggleChildrenOf(acc, hiddenToBe);
            }
        }
    }
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