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

package net.ktnx.mobileledger;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.ktnx.mobileledger.async.RetrieveTransactionsTask;
import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.ui.transaction_list.TransactionListViewModel;
import net.ktnx.mobileledger.utils.MobileLedgerDatabase;

import java.lang.ref.WeakReference;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

public class TransactionListActivity extends AppCompatActivity {
    private SwipeRefreshLayout swiper;
    private RecyclerView root;
    private ProgressBar progressBar;
    private TransactionListViewModel model;
    private TextView tvLastUpdate;
    private TransactionListAdapter modelAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.transaction_list_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setupActionBar();

        MobileLedgerDatabase dbh = new MobileLedgerDatabase(this);

        swiper = findViewById(R.id.transaction_swipe);
        if (swiper == null) throw new RuntimeException("Can't get hold on the swipe layout");
        root = findViewById(R.id.transaction_root);
        if (root == null) throw new RuntimeException("Can't get hold on the transaction list view");
        progressBar = findViewById(R.id.transaction_progress_bar);
        if (progressBar == null)
            throw new RuntimeException("Can't get hold on the transaction list progress bar");
        tvLastUpdate = findViewById(R.id.transactions_last_update);
        {
            long last_update = dbh.get_option_value("transaction_list_last_update", 0L);
            Log.d("transactions", String.format("Last update = %d", last_update));
            if (last_update == 0) tvLastUpdate.setText("never");
            else {
                Date date = new Date(last_update);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    tvLastUpdate.setText(date.toInstant().atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                }
                else {
                    tvLastUpdate.setText(date.toLocaleString());
                }
            }
        }
        model = ViewModelProviders.of(this).get(TransactionListViewModel.class);
        List<LedgerTransaction> transactions = model.getTransactions(dbh);
        modelAdapter = new TransactionListAdapter(transactions);

        RecyclerView root = findViewById(R.id.transaction_root);
        root.setAdapter(modelAdapter);

        LinearLayoutManager llm = new LinearLayoutManager(this);

        llm.setOrientation(LinearLayoutManager.VERTICAL);
        root.setLayoutManager(llm);

        swiper.setOnRefreshListener(() -> {
            Log.d("ui", "refreshing transactions via swipe");
            update_transactions();
        });

        swiper.setColorSchemeResources(R.color.colorPrimary, R.color.colorAccent);

//        update_transactions();
    }
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }
    @Override
    public void finish() {
        super.finish();
        Log.d("visuals", "finishing");
        overridePendingTransition(R.anim.dummy, R.anim.slide_out_right);
    }
    private void update_transactions() {
        RetrieveTransactionsTask task = new RetrieveTransactionsTask(new WeakReference<>(this));

        RetrieveTransactionsTask.Params params = new RetrieveTransactionsTask.Params(
                PreferenceManager.getDefaultSharedPreferences(this));

        task.execute(params);
    }

    public void onRetrieveStart() {
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.VISIBLE);
    }
    public void onRetrieveProgress(RetrieveTransactionsTask.Progress progress) {
        if ((progress.getTotal() == RetrieveTransactionsTask.Progress.INDETERMINATE) ||
            (progress.getTotal() == 0))
        {
            progressBar.setIndeterminate(true);
        }
        else {
            progressBar.setIndeterminate(false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                progressBar.setMin(0);
            }
            progressBar.setMax(progress.getTotal());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                progressBar.setProgress(progress.getProgress(), true);
            }
            else progressBar.setProgress(progress.getProgress());
        }
    }

    public void onRetrieveDone(boolean success) {
        progressBar.setVisibility(View.GONE);
        swiper.setRefreshing(false);
        if (success) {
            MobileLedgerDatabase dbh = new MobileLedgerDatabase(this);
            Date now = new Date();
            dbh.set_option_value("transaction_list_last_update", now.getTime());
            updateLastUpdateText(now);
            modelAdapter.notifyDataSetChanged();
        }
    }
    private void updateLastUpdateText(Date now) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            tvLastUpdate.setText(now.toInstant().atZone(ZoneId.systemDefault()).toString());
        }
        else {
            tvLastUpdate.setText(now.toLocaleString());
        }
    }
}
