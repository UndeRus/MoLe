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

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
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

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.ui.MobileLedgerListFragment;
import net.ktnx.mobileledger.ui.activity.MainActivity;
import net.ktnx.mobileledger.utils.Globals;
import net.ktnx.mobileledger.utils.MLDB;

import java.util.Observable;
import java.util.Observer;

import static android.content.Context.INPUT_METHOD_SERVICE;

public class TransactionListFragment extends MobileLedgerListFragment {
    public static final String BUNDLE_KEY_FILTER_ACCOUNT_NAME = "filter_account_name";
    private String mShowOnlyAccountName;
    private MenuItem menuTransactionListFilter;
    private View vAccountFilter;
    private AutoCompleteTextView accNameFilter;
    private Observer backgroundTaskCountObserver;
    private static void update(Observable o, Object arg) {
    }
    @Override
    public void onDestroy() {
        if (backgroundTaskCountObserver != null) {
            Log.d("rtl", "destroying background task count observer");
            Data.backgroundTaskCount.deleteObserver(backgroundTaskCountObserver);
        }
        super.onDestroy();
    }
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
        if (backgroundTaskCountObserver == null) {
            Log.d("rtl", "creating background task count observer");
            Data.backgroundTaskCount.addObserver(backgroundTaskCountObserver = new Observer() {
                @Override
                public void update(Observable o, Object arg) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            int cnt = Data.backgroundTaskCount.get();
                            Log.d("trl", String.format("background task count changed to %d", cnt));
                            swiper.setRefreshing(cnt > 0);
                        }
                    });
                }
            });
        }
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
        if (root == null)
            throw new RuntimeException("Can't get hold on the transaction value view");
        model = ViewModelProviders.of(this).get(TransactionListViewModel.class);
        modelAdapter = new TransactionListAdapter();

        modelAdapter.setBoldAccountName(mShowOnlyAccountName);

        FloatingActionButton fab = mActivity.findViewById(R.id.btn_add_transaction);

        RecyclerView root = mActivity.findViewById(R.id.transaction_root);
        root.setAdapter(modelAdapter);

        fab.show();
        root.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (fab != null) {
                    if (dy < 0) fab.show();
                    if (dy > 0) fab.hide();
                }
            }
        });

        LinearLayoutManager llm = new LinearLayoutManager(mActivity);

        llm.setOrientation(LinearLayoutManager.VERTICAL);
        root.setLayoutManager(llm);

        swiper.setOnRefreshListener(() -> {
            Log.d("ui", "refreshing transactions via swipe");
            mActivity.update_transactions();
        });

        swiper.setColorSchemeResources(R.color.colorPrimary, R.color.colorAccent);

        vAccountFilter = mActivity.findViewById(R.id.transaction_list_account_name_filter);
        accNameFilter = mActivity.findViewById(R.id.transaction_filter_account_name);

        TransactionListFragment me = this;
        MLDB.hook_autocompletion_adapter(mActivity, accNameFilter, "accounts", "name");
        accNameFilter.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d("tmp", "direct onItemClick");
                ((TransactionListViewModel) model).scheduleTransactionListReload(mActivity);
                MatrixCursor mc = (MatrixCursor) parent.getItemAtPosition(position);
                modelAdapter.setBoldAccountName(mc.getString(1));
                modelAdapter.notifyDataSetChanged();
                Globals.hideSoftKeyboard(mActivity);
            }
        });

        if (mShowOnlyAccountName != null) {
            accNameFilter.setText(mShowOnlyAccountName, false);
            onShowFilterClick(null);
            Log.d("flow", String.format("Account filter set to '%s'", mShowOnlyAccountName));
        }

        TransactionListViewModel.scheduleTransactionListReload(mActivity);
        TransactionListViewModel.updating.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                swiper.setRefreshing(TransactionListViewModel.updating.get());
            }
        });

        Data.transactions.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        modelAdapter.notifyDataSetChanged();
                    }
                });
            }
        });

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

    public void onClearAccountNameClick(View view) {
        vAccountFilter.setVisibility(View.GONE);
        if (menuTransactionListFilter != null) menuTransactionListFilter.setVisible(true);
        accNameFilter.setText(null);
        mShowOnlyAccountName = null;
        modelAdapter.resetBoldAccountName();
        TransactionListViewModel.scheduleTransactionListReload(mActivity);
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
}