/*
 * Copyright © 2020 Damyan Ivanov.
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

import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.async.TransactionDateFinder;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.ui.DatePickerFragment;
import net.ktnx.mobileledger.ui.MobileLedgerListFragment;
import net.ktnx.mobileledger.ui.activity.MainActivity;
import net.ktnx.mobileledger.utils.Colors;
import net.ktnx.mobileledger.utils.Globals;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.MLDB;
import net.ktnx.mobileledger.utils.SimpleDate;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

import static android.content.Context.INPUT_METHOD_SERVICE;
import static net.ktnx.mobileledger.utils.Logger.debug;

// TODO: support transaction-level comment

public class TransactionListFragment extends MobileLedgerListFragment
        implements DatePickerFragment.DatePickedListener {
    private MenuItem menuTransactionListFilter;
    private View vAccountFilter;
    private AutoCompleteTextView accNameFilter;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
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
        debug("flow", "TransactionListFragment.onResume()");
    }
    @Override
    public void onStop() {
        super.onStop();
        debug("flow", "TransactionListFragment.onStop()");
    }
    @Override
    public void onPause() {
        super.onPause();
        debug("flow", "TransactionListFragment.onPause()");
    }
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        debug("flow", "TransactionListFragment.onActivityCreated called");
        super.onActivityCreated(savedInstanceState);

        Data.backgroundTasksRunning.observe(getViewLifecycleOwner(),
                this::onBackgroundTaskRunningChanged);

        MainActivity mainActivity = getMainActivity();

        refreshLayout = mainActivity.findViewById(R.id.transaction_swipe);
        if (refreshLayout == null)
            throw new RuntimeException("Can't get hold on the swipe layout");
        root = mainActivity.findViewById(R.id.transaction_root);
        if (root == null)
            throw new RuntimeException("Can't get hold on the transaction value view");
        modelAdapter = new TransactionListAdapter();
        root.setAdapter(modelAdapter);

        mainActivity.fabShouldShow();

        manageFabOnScroll();

        LinearLayoutManager llm = new LinearLayoutManager(mainActivity);

        llm.setOrientation(RecyclerView.VERTICAL);
        root.setLayoutManager(llm);

        refreshLayout.setOnRefreshListener(() -> {
            debug("ui", "refreshing transactions via swipe");
            mainActivity.scheduleDataRetrieval();
        });

        Colors.themeWatch.observe(getViewLifecycleOwner(), this::themeChanged);

        vAccountFilter = mainActivity.findViewById(R.id.transaction_list_account_name_filter);
        accNameFilter = mainActivity.findViewById(R.id.transaction_filter_account_name);

        MLDB.hookAutocompletionAdapter(mainActivity, accNameFilter, "accounts", "name");
        accNameFilter.setOnItemClickListener((parent, view, position, id) -> {
//                debug("tmp", "direct onItemClick");
            Cursor c = (Cursor) parent.getItemAtPosition(position);
            Data.accountFilter.setValue(c.getString(1));
            Globals.hideSoftKeyboard(mainActivity);
        });

        Data.accountFilter.observe(getViewLifecycleOwner(), this::onAccountNameFilterChanged);

        TransactionListViewModel.updating.addObserver(
                (o, arg) -> refreshLayout.setRefreshing(TransactionListViewModel.updating.get()));
        TransactionListViewModel.updateError.addObserver((o, arg) -> {
            String err = TransactionListViewModel.updateError.get();
            if (err == null)
                return;

            Snackbar.make(this.root, err, Snackbar.LENGTH_LONG)
                    .show();
            TransactionListViewModel.updateError.set(null);
        });
        Data.transactions.addObserver(
                (o, arg) -> mainActivity.runOnUiThread(() -> modelAdapter.notifyDataSetChanged()));

        mainActivity.findViewById(R.id.clearAccountNameFilter)
                    .setOnClickListener(v -> {
                        Data.accountFilter.setValue(null);
                        vAccountFilter.setVisibility(View.GONE);
                        menuTransactionListFilter.setVisible(true);
                        Globals.hideSoftKeyboard(mainActivity);
                    });

        Data.foundTransactionItemIndex.observe(getViewLifecycleOwner(), pos -> {
            Logger.debug("go-to-date", String.format(Locale.US, "Found pos %d", pos));
            if (pos != null) {
                root.scrollToPosition(pos);
                // reset the value to avoid re-notification upon reconfiguration or app restart
                Data.foundTransactionItemIndex.setValue(null);
            }
        });
    }
    private void onAccountNameFilterChanged(String accName) {
        final String fieldText = accNameFilter.getText()
                                              .toString();
        if ((accName == null) && (fieldText.equals("")))
            return;

        if (accNameFilter != null) {
            accNameFilter.setText(accName, false);
        }
        final boolean filterActive = (accName != null) && !accName.isEmpty();
        if (vAccountFilter != null) {
            vAccountFilter.setVisibility(filterActive ? View.VISIBLE : View.GONE);
        }
        if (menuTransactionListFilter != null)
            menuTransactionListFilter.setVisible(!filterActive);

        TransactionListViewModel.scheduleTransactionListReload();

    }
    @Override
    public void onCreateOptionsMenu(@NotNull Menu menu, @NotNull MenuInflater inflater) {
        inflater.inflate(R.menu.transaction_list, menu);

        menuTransactionListFilter = menu.findItem(R.id.menu_transaction_list_filter);
        if ((menuTransactionListFilter == null))
            throw new AssertionError();

        if ((Data.accountFilter.getValue() != null) ||
            (vAccountFilter.getVisibility() == View.VISIBLE))
        {
            menuTransactionListFilter.setVisible(false);
        }

        super.onCreateOptionsMenu(menu, inflater);

        menuTransactionListFilter.setOnMenuItemClickListener(item -> {
            vAccountFilter.setVisibility(View.VISIBLE);
            if (menuTransactionListFilter != null)
                menuTransactionListFilter.setVisible(false);
            accNameFilter.requestFocus();
            InputMethodManager imm =
                    (InputMethodManager) getMainActivity().getSystemService(INPUT_METHOD_SERVICE);
            imm.showSoftInput(accNameFilter, 0);

            return true;
        });

        menu.findItem(R.id.menu_go_to_date)
            .setOnMenuItemClickListener(item -> {
                DatePickerFragment picker = new DatePickerFragment();
                picker.setOnDatePickedListener(this);
                picker.setDateRange(Data.earliestTransactionDate.getValue(),
                        Data.latestTransactionDate.getValue());
                picker.show(requireActivity().getSupportFragmentManager(), null);
                return true;
            });
    }
    @Override
    public void onDatePicked(int year, int month, int day) {
        RecyclerView list = requireActivity().findViewById(R.id.transaction_root);
        AsyncTask<SimpleDate, Void, Integer> finder = new TransactionDateFinder();

        finder.execute(new SimpleDate(year, month + 1, day));
    }
}
