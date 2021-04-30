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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.async.TransactionDateFinder;
import net.ktnx.mobileledger.db.AccountAutocompleteAdapter;
import net.ktnx.mobileledger.db.Profile;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.ui.DatePickerFragment;
import net.ktnx.mobileledger.ui.FabManager;
import net.ktnx.mobileledger.ui.MainModel;
import net.ktnx.mobileledger.ui.MobileLedgerListFragment;
import net.ktnx.mobileledger.ui.activity.MainActivity;
import net.ktnx.mobileledger.utils.Colors;
import net.ktnx.mobileledger.utils.Globals;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.SimpleDate;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

import static android.content.Context.INPUT_METHOD_SERVICE;
import static net.ktnx.mobileledger.utils.Logger.debug;

public class TransactionListFragment extends MobileLedgerListFragment
        implements DatePickerFragment.DatePickedListener {
    private MenuItem menuTransactionListFilter;
    private MenuItem menuGoToDate;
    private View vAccountFilter;
    private AutoCompleteTextView accNameFilter;
    private MainModel model;
    private boolean fragmentActive = false;
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
        fragmentActive = true;
        toggleMenuItems();
        debug("flow", "TransactionListFragment.onResume()");
    }
    private void toggleMenuItems() {
        if (menuGoToDate != null)
            menuGoToDate.setVisible(fragmentActive);
        if (menuTransactionListFilter != null) {
            final int filterVisibility = vAccountFilter.getVisibility();
            menuTransactionListFilter.setVisible(
                    fragmentActive && filterVisibility != View.VISIBLE);
        }
    }
    @Override
    public void onStop() {
        super.onStop();
        fragmentActive = false;
        toggleMenuItems();
        debug("flow", "TransactionListFragment.onStop()");
    }
    @Override
    public void onPause() {
        super.onPause();
        fragmentActive = false;
        toggleMenuItems();
        debug("flow", "TransactionListFragment.onPause()");
    }
    @Override
    public void onViewCreated(@NotNull View view, @Nullable Bundle savedInstanceState) {
        debug("flow", "TransactionListFragment.onActivityCreated called");
        super.onViewCreated(view, savedInstanceState);

        Data.backgroundTasksRunning.observe(getViewLifecycleOwner(),
                this::onBackgroundTaskRunningChanged);

        MainActivity mainActivity = getMainActivity();

        model = new ViewModelProvider(requireActivity()).get(MainModel.class);

        refreshLayout = view.findViewById(R.id.transaction_swipe);
        if (refreshLayout == null)
            throw new RuntimeException("Can't get hold on the swipe layout");
        root = view.findViewById(R.id.transaction_root);
        if (root == null)
            throw new RuntimeException("Can't get hold on the transaction value view");
        modelAdapter = new TransactionListAdapter();
        root.setAdapter(modelAdapter);

        mainActivity.fabShouldShow();

        if (mainActivity instanceof FabManager.FabHandler)
            FabManager.handle(mainActivity, root);

        LinearLayoutManager llm = new LinearLayoutManager(mainActivity);

        llm.setOrientation(RecyclerView.VERTICAL);
        root.setLayoutManager(llm);

        refreshLayout.setOnRefreshListener(() -> {
            debug("ui", "refreshing transactions via swipe");
            model.scheduleTransactionListRetrieval();
        });

        Colors.themeWatch.observe(getViewLifecycleOwner(), this::themeChanged);

        vAccountFilter = view.findViewById(R.id.transaction_list_account_name_filter);
        accNameFilter = view.findViewById(R.id.transaction_filter_account_name);

        Data.observeProfile(getViewLifecycleOwner(), this::onProfileChanged);

        accNameFilter.setOnItemClickListener((parent, v, position, id) -> {
//                debug("tmp", "direct onItemClick");
            model.getAccountFilter()
                 .setValue(parent.getItemAtPosition(position)
                                 .toString());
            Globals.hideSoftKeyboard(mainActivity);
        });

        model.getAccountFilter()
             .observe(getViewLifecycleOwner(), this::onAccountNameFilterChanged);

        model.getUpdatingFlag()
             .observe(getViewLifecycleOwner(), (flag) -> refreshLayout.setRefreshing(flag));
        model.getDisplayedTransactions()
             .observe(getViewLifecycleOwner(), list -> modelAdapter.setTransactions(list));

        view.findViewById(R.id.clearAccountNameFilter)
            .setOnClickListener(v -> {
                model.getAccountFilter()
                     .setValue(null);
                Globals.hideSoftKeyboard(mainActivity);
            });

        model.foundTransactionItemIndex.observe(getViewLifecycleOwner(), pos -> {
            Logger.debug("go-to-date", String.format(Locale.US, "Found pos %d", pos));
            if (pos != null) {
                root.scrollToPosition(pos);
                // reset the value to avoid re-notification upon reconfiguration or app restart
                model.foundTransactionItemIndex.setValue(null);
            }
        });
    }
    private void onProfileChanged(Profile profile) {
        if (profile == null)
            return;

        accNameFilter.setAdapter(new AccountAutocompleteAdapter(getContext(), profile));
    }
    private void onAccountNameFilterChanged(String accName) {
        accNameFilter.setText(accName, false);

        boolean filterActive = (accName != null) && !accName.isEmpty();
        if (vAccountFilter != null) {
            vAccountFilter.setVisibility(filterActive ? View.VISIBLE : View.GONE);
        }
        if (menuTransactionListFilter != null)
            menuTransactionListFilter.setVisible(!filterActive);
    }
    @Override
    public void onCreateOptionsMenu(@NotNull Menu menu, @NotNull MenuInflater inflater) {
        inflater.inflate(R.menu.transaction_list, menu);

        menuTransactionListFilter = menu.findItem(R.id.menu_transaction_list_filter);
        if ((menuTransactionListFilter == null))
            throw new AssertionError();
        menuGoToDate = menu.findItem(R.id.menu_go_to_date);
        if ((menuGoToDate == null))
            throw new AssertionError();

        model.getAccountFilter()
             .observe(this, v -> menuTransactionListFilter.setVisible(v == null));

        super.onCreateOptionsMenu(menu, inflater);

        menuTransactionListFilter.setOnMenuItemClickListener(item -> {
            vAccountFilter.setVisibility(View.VISIBLE);
            menuTransactionListFilter.setVisible(false);
            accNameFilter.requestFocus();
            InputMethodManager imm =
                    (InputMethodManager) getMainActivity().getSystemService(INPUT_METHOD_SERVICE);
            imm.showSoftInput(accNameFilter, 0);

            return true;
        });

        menuGoToDate.setOnMenuItemClickListener(item -> {
            DatePickerFragment picker = new DatePickerFragment();
            picker.setOnDatePickedListener(this);
            picker.setDateRange(model.getFirstTransactionDate(), model.getLastTransactionDate());
            picker.show(requireActivity().getSupportFragmentManager(), null);
            return true;
        });

        toggleMenuItems();
    }
    @Override
    public void onDatePicked(int year, int month, int day) {
        RecyclerView list = requireActivity().findViewById(R.id.transaction_root);
        AsyncTask<TransactionDateFinder.Params, Void, Integer> finder = new TransactionDateFinder();

        finder.execute(
                new TransactionDateFinder.Params(model, new SimpleDate(year, month + 1, day)));
    }
}
