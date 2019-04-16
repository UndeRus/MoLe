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
import android.database.MatrixCursor;
import android.os.Bundle;
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

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.ui.MobileLedgerListFragment;
import net.ktnx.mobileledger.ui.activity.MainActivity;
import net.ktnx.mobileledger.utils.Colors;
import net.ktnx.mobileledger.utils.Globals;
import net.ktnx.mobileledger.utils.MLDB;

import org.jetbrains.annotations.NotNull;

import java.util.Observable;
import java.util.Observer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static android.content.Context.INPUT_METHOD_SERVICE;

public class TransactionListFragment extends MobileLedgerListFragment {
    private MenuItem menuTransactionListFilter;
    private View vAccountFilter;
    private AutoCompleteTextView accNameFilter;
    private Observer backgroundTaskCountObserver;
    @Override
    public void onDestroy() {
        if (backgroundTaskCountObserver != null) {
            Log.d("rtl", "destroying background task count observer");
            Data.backgroundTaskCount.deleteObserver(backgroundTaskCountObserver);
        }
        super.onDestroy();
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
    public void onAttach(@NotNull Context context) {
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
        modelAdapter = new TransactionListAdapter();
        root.setAdapter(modelAdapter);

        FloatingActionButton fab = mActivity.findViewById(R.id.btn_add_transaction);

        mActivity.fabShouldShow();
        root.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy < 0) mActivity.fabShouldShow();
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

        Colors.themeWatch.observe(this, this::themeChanged);
        swiper.setColorSchemeColors(Colors.primary);

        vAccountFilter = mActivity.findViewById(R.id.transaction_list_account_name_filter);
        accNameFilter = mActivity.findViewById(R.id.transaction_filter_account_name);

        MLDB.hookAutocompletionAdapter(mActivity, accNameFilter, "accounts", "name", true);
        accNameFilter.setOnItemClickListener((parent, view, position, id) -> {
//                Log.d("tmp", "direct onItemClick");
            MatrixCursor mc = (MatrixCursor) parent.getItemAtPosition(position);
            Data.accountFilter.setValue(mc.getString(1));
            Globals.hideSoftKeyboard(mActivity);
        });

        Data.accountFilter.observe(this, this::onAccountNameFilterChanged);

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
            Data.accountFilter.setValue(null);
            vAccountFilter.setVisibility(View.GONE);
            menuTransactionListFilter.setVisible(true);
            Globals.hideSoftKeyboard(mActivity);
        });
    }
    private void onAccountNameFilterChanged(String accName) {
        if (accNameFilter != null) {
            accNameFilter.setText(accName, false);
        }
        final boolean filterActive = (accName != null) && !accName.isEmpty();
        if (vAccountFilter != null) {
            vAccountFilter.setVisibility(filterActive ? View.VISIBLE : View.GONE);
        }
        if (menuTransactionListFilter != null) menuTransactionListFilter.setVisible(!filterActive);

        TransactionListViewModel.scheduleTransactionListReload();

    }
    @Override
    public void onCreateOptionsMenu(@NotNull Menu menu, @NotNull MenuInflater inflater) {
        inflater.inflate(R.menu.transaction_list, menu);

        menuTransactionListFilter = menu.findItem(R.id.menu_transaction_list_filter);
        if ((menuTransactionListFilter == null)) throw new AssertionError();

        if (Data.accountFilter.getValue() != null) {
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