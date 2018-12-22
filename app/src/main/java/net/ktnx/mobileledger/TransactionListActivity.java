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
import net.ktnx.mobileledger.ui.transaction_list.TransactionListViewModel;
import net.ktnx.mobileledger.utils.MLDB;

import java.lang.ref.WeakReference;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class TransactionListActivity extends AppCompatActivity {
    public TransactionListViewModel model;
    private SwipeRefreshLayout swiper;
    private RecyclerView root;
    private ProgressBar progressBar;
    private TextView tvLastUpdate;
    private TransactionListAdapter modelAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.transaction_list_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setupActionBar();

        swiper = findViewById(R.id.transaction_swipe);
        if (swiper == null) throw new RuntimeException("Can't get hold on the swipe layout");
        root = findViewById(R.id.transaction_root);
        if (root == null) throw new RuntimeException("Can't get hold on the transaction list view");
        progressBar = findViewById(R.id.transaction_progress_bar);
        if (progressBar == null)
            throw new RuntimeException("Can't get hold on the transaction list progress bar");
        tvLastUpdate = findViewById(R.id.transactions_last_update);
        updateLastUpdateText();
        model = ViewModelProviders.of(this).get(TransactionListViewModel.class);
        modelAdapter = new TransactionListAdapter(model);

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

        updateLastUpdateText();
        long last_update = MLDB.get_option_value(this, MLDB.OPT_TRANSACTION_LIST_STAMP, 0L);
        Log.d("transactions", String.format("Last update = %d", last_update));
        if (last_update == 0) {
            update_transactions();
        }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) progressBar.setProgress(0, false);
        else progressBar.setProgress(0);
        progressBar.setVisibility(View.VISIBLE);
    }
    public void onRetrieveProgress(RetrieveTransactionsTask.Progress progress) {
        if ((progress.getTotal() == RetrieveTransactionsTask.Progress.INDETERMINATE) ||
            (progress.getTotal() == 0))
        {
            progressBar.setIndeterminate(true);
        }
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                progressBar.setMin(0);
            }
            progressBar.setMax(progress.getTotal());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                progressBar.setProgress(progress.getProgress(), true);
            }
            else progressBar.setProgress(progress.getProgress());
            progressBar.setIndeterminate(false);
        }
    }

    public void onRetrieveDone(boolean success) {
        progressBar.setVisibility(View.INVISIBLE);
        swiper.setRefreshing(false);
        updateLastUpdateText();
        if (success) {
            Log.d("transactions", "calling notifyDataSetChanged()");
            modelAdapter.notifyDataSetChanged();
        }
    }
    private void updateLastUpdateText() {
        {
            long last_update = MLDB.get_option_value(this, MLDB.OPT_TRANSACTION_LIST_STAMP, 0L);
            Log.d("transactions", String.format("Last update = %d", last_update));
            if (last_update == 0) {
                tvLastUpdate.setText(getString(R.string.transaction_last_update_never));
            }
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
    }
}
