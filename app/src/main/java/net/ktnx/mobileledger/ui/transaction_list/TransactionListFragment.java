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
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.ui.MobileLedgerListFragment;
import net.ktnx.mobileledger.ui.activity.MainActivity;
import net.ktnx.mobileledger.utils.Globals;
import net.ktnx.mobileledger.utils.MLDB;
import net.ktnx.mobileledger.utils.ObservableValue;

import java.util.Observable;
import java.util.Observer;

import static android.content.Context.INPUT_METHOD_SERVICE;

public class TransactionListFragment extends MobileLedgerListFragment {
    public static final String BUNDLE_KEY_FILTER_ACCOUNT_NAME = "filter_account_name";
    public static ObservableValue<String> accountFilter = new ObservableValue<>();
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
                    mActivity.runOnUiThread(() -> {
                        int cnt = Data.backgroundTaskCount.get();
                        Log.d("trl", String.format("background task count changed to %d", cnt));
                        swiper.setRefreshing(cnt > 0);
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
    public void onResume() {
        super.onResume();
        Log.d("flow", "TransactionListFragment.onResume()");
    }
    @Override
    public void onStop() {
        super.onStop();
        Log.d("flow", "TransactionListFragment.onStop()");
    }
    @Override
    public void onPause() {
        super.onPause();
        Log.d("flow", "TransactionListFragment.onPause()");
    }
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Log.d("flow", "TransactionListFragment.onActivityCreated called");
        super.onActivityCreated(savedInstanceState);

        swiper = mActivity.findViewById(R.id.transaction_swipe);
        if (swiper == null) throw new RuntimeException("Can't get hold on the swipe layout");
        root = mActivity.findViewById(R.id.transaction_root);
        if (root == null)
            throw new RuntimeException("Can't get hold on the transaction value view");
        model = ViewModelProviders.of(this).get(TransactionListViewModel.class);
        modelAdapter = new TransactionListAdapter();

        modelAdapter.setBoldAccountName(mShowOnlyAccountName);
        root.setAdapter(modelAdapter);

        FloatingActionButton fab = mActivity.findViewById(R.id.btn_add_transaction);

        fab.show();
        root.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy < 0) fab.show();
                if (dy > 0) fab.hide();
            }
        });

        LinearLayoutManager llm = new LinearLayoutManager(mActivity);

        llm.setOrientation(LinearLayoutManager.VERTICAL);
        root.setLayoutManager(llm);

        swiper.setOnRefreshListener(() -> {
            Log.d("ui", "refreshing transactions via swipe");
            mActivity.scheduleTransactionListRetrieval();
        });

        swiper.setColorSchemeResources(R.color.colorPrimary, R.color.colorAccent);

        vAccountFilter = mActivity.findViewById(R.id.transaction_list_account_name_filter);
        accNameFilter = mActivity.findViewById(R.id.transaction_filter_account_name);

        TransactionListFragment me = this;
        MLDB.hookAutocompletionAdapter(mActivity, accNameFilter, "accounts", "name", true);
        accNameFilter.setOnItemClickListener((parent, view, position, id) -> {
//                Log.d("tmp", "direct onItemClick");
            TransactionListViewModel.scheduleTransactionListReload();
            MatrixCursor mc = (MatrixCursor) parent.getItemAtPosition(position);
            accountFilter.set(mc.getString(1));
            Globals.hideSoftKeyboard(mActivity);
        });

        accountFilter.addObserver((o, arg) -> {
            String accountName = accountFilter.get();
            modelAdapter.setBoldAccountName(accountName);
            setShowOnlyAccountName(accountName);
            TransactionListViewModel.scheduleTransactionListReload();
            if (menuTransactionListFilter != null) menuTransactionListFilter.setVisible(false);
        });

        Data.profile.addObserver((o, arg) -> mActivity.runOnUiThread(() -> {
            Log.d("transactions", "requesting list reload");
            TransactionListViewModel.scheduleTransactionListReload();
        }));

        TransactionListViewModel.scheduleTransactionListReload();
        TransactionListViewModel.updating.addObserver(
                (o, arg) -> swiper.setRefreshing(TransactionListViewModel.updating.get()));
        TransactionListViewModel.updateError.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                String err = TransactionListViewModel.updateError.get();
                if (err == null) return;

                Toast.makeText(mActivity, err, Toast.LENGTH_SHORT).show();
                TransactionListViewModel.updateError.set(null);
            }
        });
        Data.transactions.addObserver(
                (o, arg) -> mActivity.runOnUiThread(() -> modelAdapter.notifyDataSetChanged()));

        mActivity.findViewById(R.id.clearAccountNameFilter).setOnClickListener(v -> {
            vAccountFilter.setVisibility(View.GONE);
            if (menuTransactionListFilter != null) menuTransactionListFilter.setVisible(true);
            accountFilter.set(null);
            accNameFilter.setText(null);
            TransactionListViewModel.scheduleTransactionListReload();
            Globals.hideSoftKeyboard(mActivity);
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

        menuTransactionListFilter.setOnMenuItemClickListener(item -> {
            vAccountFilter.setVisibility(View.VISIBLE);
            if (menuTransactionListFilter != null) menuTransactionListFilter.setVisible(false);
            accNameFilter.requestFocus();
            InputMethodManager imm =
                    (InputMethodManager) mActivity.getSystemService(INPUT_METHOD_SERVICE);
            imm.showSoftInput(accNameFilter, 0);

            return true;
        });
    }
}