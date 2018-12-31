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

package net.ktnx.mobileledger.ui.transaction_list;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.database.MatrixCursor;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.async.RetrieveTransactionsTask;
import net.ktnx.mobileledger.ui.activity.MainActivity;
import net.ktnx.mobileledger.utils.Globals;
import net.ktnx.mobileledger.utils.MLDB;

import java.lang.ref.WeakReference;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static android.content.Context.INPUT_METHOD_SERVICE;

public class TransactionListFragment extends Fragment {
    public static final String BUNDLE_KEY_FILTER_ACCOUNT_NAME = "filter_account_name";
    public TransactionListViewModel model;
    private String mShowOnlyAccountName;
    private MainActivity mActivity;
    private View bTransactionListCancelDownload;
    private MenuItem menuTransactionListFilter;
    private View vAccountFilter;
    private SwipeRefreshLayout swiper;
    private RecyclerView root;
    private ProgressBar progressBar;
    private LinearLayout progressLayout;
    private TextView tvLastUpdate;
    private TransactionListAdapter modelAdapter;
    private RetrieveTransactionsTask retrieveTransactionsTask;
    private AutoCompleteTextView accNameFilter;
    public void setShowOnlyAccountName(String mShowOnlyAccountName) {
        this.mShowOnlyAccountName = mShowOnlyAccountName;
        if (modelAdapter != null) {
            modelAdapter.setBoldAccountName(mShowOnlyAccountName);
        }
        if (accNameFilter != null) {
            accNameFilter.setText(mShowOnlyAccountName, false);
        }
        if (vAccountFilter != null) {
            vAccountFilter.setVisibility(
                    ((mShowOnlyAccountName != null) && !mShowOnlyAccountName.isEmpty())
                    ? View.VISIBLE : View.GONE);
        }
    }
    @Override
    public void setArguments(@Nullable Bundle args) {
        super.setArguments(args);
        mShowOnlyAccountName = args.getString(BUNDLE_KEY_FILTER_ACCOUNT_NAME);
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (MainActivity) context;
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.transaction_list_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Log.d("flow", "TransactionListFragment.onActivityCreated called");
        super.onActivityCreated(savedInstanceState);

        mActivity.markDrawerItemCurrent(R.id.nav_latest_transactions);

        swiper = mActivity.findViewById(R.id.transaction_swipe);
        if (swiper == null) throw new RuntimeException("Can't get hold on the swipe layout");
        root = mActivity.findViewById(R.id.transaction_root);
        if (root == null) throw new RuntimeException("Can't get hold on the transaction list view");
        progressBar = mActivity.findViewById(R.id.transaction_list_progress_bar);
        if (progressBar == null)
            throw new RuntimeException("Can't get hold on the transaction list progress bar");
        progressLayout = mActivity.findViewById(R.id.transaction_progress_layout);
        if (progressLayout == null) throw new RuntimeException(
                "Can't get hold on the transaction list progress bar layout");
        tvLastUpdate = mActivity.findViewById(R.id.transactions_last_update);
        updateLastUpdateText();
        model = ViewModelProviders.of(this).get(TransactionListViewModel.class);
        modelAdapter = new TransactionListAdapter(model);

        modelAdapter.setBoldAccountName(mShowOnlyAccountName);

        RecyclerView root = mActivity.findViewById(R.id.transaction_root);
        root.setAdapter(modelAdapter);

        LinearLayoutManager llm = new LinearLayoutManager(mActivity);

        llm.setOrientation(LinearLayoutManager.VERTICAL);
        root.setLayoutManager(llm);

        swiper.setOnRefreshListener(() -> {
            Log.d("ui", "refreshing transactions via swipe");
            update_transactions();
        });

        swiper.setColorSchemeResources(R.color.colorPrimary, R.color.colorAccent);

        vAccountFilter = mActivity.findViewById(R.id.transaction_list_account_name_filter);
        accNameFilter = mActivity.findViewById(R.id.transaction_filter_account_name);
        bTransactionListCancelDownload =
                mActivity.findViewById(R.id.transaction_list_cancel_download);

        TransactionListFragment me = this;
        MLDB.hook_autocompletion_adapter(mActivity, accNameFilter, "accounts", "name");
        accNameFilter.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d("tmp", "direct onItemClick");
                model.reloadTransactions(me);
                MatrixCursor mc = (MatrixCursor) parent.getItemAtPosition(position);
                modelAdapter.setBoldAccountName(mc.getString(1));
                modelAdapter.notifyDataSetChanged();
                Globals.hideSoftKeyboard(mActivity);
            }
        });

        updateLastUpdateText();
        long last_update = MLDB.get_option_value(mActivity, MLDB.OPT_TRANSACTION_LIST_STAMP, 0L);
        Log.d("transactions", String.format("Last update = %d", last_update));

        if (mShowOnlyAccountName != null) {
            accNameFilter.setText(mShowOnlyAccountName, false);
            onShowFilterClick(null);
            Log.d("flow", String.format("Account filter set to '%s'", mShowOnlyAccountName));
        }

        if (last_update == 0) {
            update_transactions();
        }
        else {
            model.reloadTransactions(this);
        }
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.transaction_list, menu);

        menuTransactionListFilter = menu.findItem(R.id.menu_transaction_list_filter);
        if ((menuTransactionListFilter == null)) throw new AssertionError();

        if (mShowOnlyAccountName != null) {
            menuTransactionListFilter.setVisible(false);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }
    private void update_transactions() {
        retrieveTransactionsTask = new RetrieveTransactionsTask(new WeakReference<>(this));

        RetrieveTransactionsTask.Params params = new RetrieveTransactionsTask.Params(
                PreferenceManager.getDefaultSharedPreferences(mActivity));

        retrieveTransactionsTask.execute(params);
        bTransactionListCancelDownload.setEnabled(true);
    }
    public void onRetrieveStart() {
        progressBar.setIndeterminate(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) progressBar.setProgress(0, false);
        else progressBar.setProgress(0);
        progressLayout.setVisibility(View.VISIBLE);
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
        progressLayout.setVisibility(View.GONE);
        swiper.setRefreshing(false);
        updateLastUpdateText();
        if (success) {
            Log.d("transactions", "calling notifyDataSetChanged()");
            modelAdapter.notifyDataSetChanged();
        }
    }
    private void updateLastUpdateText() {
        {
            long last_update =
                    MLDB.get_option_value(mActivity, MLDB.OPT_TRANSACTION_LIST_STAMP, 0L);
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
    public void onClearAccountNameClick(View view) {
        vAccountFilter.setVisibility(View.GONE);
        if (menuTransactionListFilter != null) menuTransactionListFilter.setVisible(true);
        accNameFilter.setText(null);
        mShowOnlyAccountName = null;
        model.reloadTransactions(this);
        modelAdapter.resetBoldAccountName();
        modelAdapter.notifyDataSetChanged();
        Globals.hideSoftKeyboard(mActivity);
    }
    public void onShowFilterClick(MenuItem menuItem) {
        vAccountFilter.setVisibility(View.VISIBLE);
        if (menuTransactionListFilter != null) menuTransactionListFilter.setVisible(false);
        if (menuItem != null) {
            accNameFilter.requestFocus();
            InputMethodManager imm =
                    (InputMethodManager) mActivity.getSystemService(INPUT_METHOD_SERVICE);
            imm.showSoftInput(accNameFilter, 0);
        }
    }
    public void onStopTransactionRefreshClick(View view) {
        Log.d("interactive", "Cancelling transactions refresh");
        if (retrieveTransactionsTask != null) retrieveTransactionsTask.cancel(false);
        bTransactionListCancelDownload.setEnabled(false);
    }
}
