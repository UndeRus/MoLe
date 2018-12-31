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

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
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

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.ui.RecyclerItemListener;
import net.ktnx.mobileledger.async.RetrieveAccountsTask;
import net.ktnx.mobileledger.model.LedgerAccount;
import net.ktnx.mobileledger.ui.activity.MainActivity;
import net.ktnx.mobileledger.utils.MLDB;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;

import static net.ktnx.mobileledger.ui.activity.SettingsActivity.PREF_KEY_SHOW_ONLY_STARRED_ACCOUNTS;

public class AccountSummaryFragment extends Fragment {

    private static long account_list_last_updated;
    private static boolean account_list_needs_update = true;
    MenuItem mShowHiddenAccounts;
    SharedPreferences.OnSharedPreferenceChangeListener sBindPreferenceSummaryToValueListener;
    private AccountSummaryViewModel model;
    private AccountSummaryAdapter modelAdapter;
    private Menu optMenu;
    private MainActivity mActivity;
    private FloatingActionButton fab;
    private SwipeRefreshLayout swiper;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (MainActivity) context;
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.account_summary_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mActivity.markDrawerItemCurrent(R.id.nav_account_summary);

        model = ViewModelProviders.of(this).get(AccountSummaryViewModel.class);
        List<LedgerAccount> accounts = model.getAccounts(this.getContext());
        modelAdapter = new AccountSummaryAdapter(accounts);

        RecyclerView root = mActivity.findViewById(R.id.account_root);
        root.setAdapter(modelAdapter);

        LinearLayoutManager llm = new LinearLayoutManager(mActivity);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        root.setLayoutManager(llm);

        fab = mActivity.findViewById(R.id.btn_add_transaction);

        root.addOnItemTouchListener(new RecyclerItemListener(mActivity, root,
                new RecyclerItemListener.RecyclerTouchListener() {
                    @Override
                    public void onClickItem(View v, int position) {
                        Log.d("list", String.format("item %d clicked", position));
                        if (modelAdapter.isSelectionActive()) {
                            modelAdapter.selectItem(position);
                        }
                        else {
                            List<LedgerAccount> accounts = model.getAccounts(mActivity);
                            if (accounts != null) {
                                LedgerAccount account = accounts.get(position);

                                mActivity.showAccountTransactions(account);
                            }
                        }
                    }

                    @Override
                    public void onLongClickItem(View v, int position) {
                        Log.d("list", String.format("item %d long-clicked", position));
                        modelAdapter.startSelection();
                        if (optMenu != null) {
                            optMenu.findItem(R.id.menu_acc_summary_cancel_selection)
                                    .setVisible(true);
                            optMenu.findItem(R.id.menu_acc_summary_confirm_selection)
                                    .setVisible(true);
                            optMenu.findItem(R.id.menu_acc_summary_only_starred).setVisible(false);
                        }
                        {
                            if (fab != null) fab.hide();
                        }
                    }
                }));

        root.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (fab != null) {
                    if (dy < 0) fab.show();
                    if (dy > 0) fab.hide();
                }
            }
        });
        swiper = mActivity.findViewById(R.id.account_swiper);
        swiper.setColorSchemeResources(R.color.colorPrimary, R.color.colorAccent);
        swiper.setOnRefreshListener(() -> {
            Log.d("ui", "refreshing accounts via swipe");
            update_accounts(true);
        });
        prepare_db();
//        update_account_table();
        update_accounts(false);

    }
    private void prepare_db() {
        account_list_last_updated = MLDB.get_option_value(mActivity, "last_refresh", (long) 0);
    }

    private void update_accounts(boolean force) {
        long now = new Date().getTime();
        if ((now > (account_list_last_updated + (24 * 3600 * 1000))) || force) {
            Log.d("db",
                    "accounts last updated at " + account_list_last_updated + " and now is " + now +
                    ". re-fetching");
            update_accounts();
        }
    }

    private void update_accounts() {
        RetrieveAccountsTask task = new RetrieveAccountsTask(new WeakReference<>(this));

        task.setPref(PreferenceManager.getDefaultSharedPreferences(mActivity));
        task.execute();

    }
    public void onAccountRefreshDone(int error) {
        swiper.setRefreshing(false);
        if (error != 0) {
            String err_text = getResources().getString(error);
            Log.d("visual", String.format("showing snackbar: %s", err_text));
            Snackbar.make(swiper, err_text, Snackbar.LENGTH_LONG).show();
        }
        else {
            MLDB.set_option_value(mActivity, "last_refresh", new Date().getTime());
            update_account_table();
        }
    }
    private void update_account_table() {
        if (this.getContext() == null) return;

        model.reloadAccounts(this.getContext());
        modelAdapter.notifyDataSetChanged();
    }
    public void onRefreshAccountSummaryClicked(MenuItem mi) {
        update_accounts(true);
    }

    public void onShowOnlyStarredClicked(MenuItem mi) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mActivity);
        boolean flag = pref.getBoolean(PREF_KEY_SHOW_ONLY_STARRED_ACCOUNTS, false);

        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(PREF_KEY_SHOW_ONLY_STARRED_ACCOUNTS, !flag);
        Log.d("pref", "Setting show only starred accounts pref to " + (flag ? "false" : "true"));
        editor.apply();

        update_account_table();
    }

    void stopSelection() {
        modelAdapter.stopSelection();
        if (optMenu != null) {
            optMenu.findItem(R.id.menu_acc_summary_cancel_selection).setVisible(false);
            optMenu.findItem(R.id.menu_acc_summary_confirm_selection).setVisible(false);
            optMenu.findItem(R.id.menu_acc_summary_only_starred).setVisible(true);
        }
        {
            if (fab != null) fab.show();
        }
    }
    public void onCancelAccSelection(MenuItem item) {
        stopSelection();
    }
    public void onConfirmAccSelection(MenuItem item) {
        model.commitSelections(mActivity);
        stopSelection();
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.account_summary, menu);
        optMenu = menu;

        mShowHiddenAccounts = menu.findItem(R.id.menu_acc_summary_only_starred);
        if (mShowHiddenAccounts == null) throw new AssertionError();

        sBindPreferenceSummaryToValueListener = (preference, value) -> mShowHiddenAccounts
                .setChecked(preference.getBoolean(PREF_KEY_SHOW_ONLY_STARRED_ACCOUNTS, false));
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mActivity);
        pref.registerOnSharedPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        mShowHiddenAccounts.setChecked(pref.getBoolean(PREF_KEY_SHOW_ONLY_STARRED_ACCOUNTS, false));

        Log.d("menu", "MainActivity: onCreateOptionsMenu called");
    }
}
