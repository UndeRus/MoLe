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

package net.ktnx.mobileledger.ui.account_summary;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.ui.MobileLedgerListFragment;
import net.ktnx.mobileledger.ui.activity.MainActivity;
import net.ktnx.mobileledger.utils.Colors;

import java.util.Observer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static net.ktnx.mobileledger.ui.activity.SettingsActivity.PREF_KEY_SHOW_ONLY_STARRED_ACCOUNTS;

public class AccountSummaryFragment extends MobileLedgerListFragment {

    MenuItem mShowOnlyStarred;
    public AccountSummaryAdapter modelAdapter;
    private Menu optMenu;
    private FloatingActionButton fab;
    private Observer backgroundTaskCountObserver;
    @Override
    public void onDestroy() {
        if (backgroundTaskCountObserver != null) {
            Log.d("acc", "destroying background task count observer");
            Data.backgroundTaskCount.deleteObserver(backgroundTaskCountObserver);
        }
        super.onDestroy();
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("flow", "AccountSummaryFragment.onCreate()");
        setHasOptionsMenu(true);

        if (backgroundTaskCountObserver == null) {
            Log.d("acc", "creating background task count observer");
            Data.backgroundTaskCount.addObserver(backgroundTaskCountObserver = (o, arg) -> {
                if (mActivity == null) return;
                if (swiper == null) return;
                mActivity.runOnUiThread(() -> {
                    int cnt = Data.backgroundTaskCount.get();
                    Log.d("acc", String.format("background task count changed to %d", cnt));
                    swiper.setRefreshing(cnt > 0);
                });
            });
        }
    }
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d("flow", "AccountSummaryFragment.onAttach()");
        mActivity = (MainActivity) context;
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d("flow", "AccountSummaryFragment.onCreateView()");
        return inflater.inflate(R.layout.account_summary_fragment, container, false);
    }

    @Override

    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Log.d("flow", "AccountSummaryFragment.onActivityCreated()");
        super.onActivityCreated(savedInstanceState);

        modelAdapter = new AccountSummaryAdapter();

        mActivity.mAccountSummaryFragment = this;
        root = mActivity.findViewById(R.id.account_root);
        LinearLayoutManager llm = new LinearLayoutManager(mActivity);
        llm.setOrientation(RecyclerView.VERTICAL);
        root.setLayoutManager(llm);
        root.setAdapter(modelAdapter);
        DividerItemDecoration did = new DividerItemDecoration(mActivity, DividerItemDecoration.VERTICAL);
        root.addItemDecoration(did);

        fab = mActivity.findViewById(R.id.btn_add_transaction);

//        root.addOnItemTouchListener(new RecyclerItemListener(mActivity, root,
//                new RecyclerItemListener.RecyclerTouchListener() {
//                    @Override
//                    public void onClickItem(View v, int position) {
//                        Log.d("value", String.format("item %d clicked", position));
//                        if (modelAdapter.isSelectionActive()) {
//                            modelAdapter.selectItem(position);
//                        }
//                        else {
//                            List<LedgerAccount> accounts = Data.accounts.get();
//                            if (accounts != null) {
//                                LedgerAccount account = accounts.get(position);
//
//                                mActivity.showAccountTransactions(account);
//                            }
//                        }
//                    }
//
//                    @Override
//                    public void onLongClickItem(View v, int position) {
//                        Log.d("value", String.format("item %d long-clicked", position));
//                        modelAdapter.startSelection();
//                        if (optMenu != null) {
//                            optMenu.findItem(R.id.menu_acc_summary_cancel_selection)
//                                    .setVisible(true);
//                            optMenu.findItem(R.id.menu_acc_summary_confirm_selection)
//                                    .setVisible(true);
//                            optMenu.findItem(R.id.menu_acc_summary_only_starred).setVisible(false);
//                        }
//                        {
//                            if (fab != null) fab.hide();
//                        }
//                    }
//                }));

        mActivity.fabShouldShow();
        root.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (fab != null) {
                    if (dy < 0) mActivity.fabShouldShow();
                    if (dy > 0) fab.hide();
                }
            }
        });
        swiper = mActivity.findViewById(R.id.account_swiper);
        Colors.themeWatch.addObserver((o, arg) -> swiper.setColorSchemeColors(Colors.primary));
        swiper.setColorSchemeColors(Colors.primary);
        swiper.setOnRefreshListener(() -> {
            Log.d("ui", "refreshing accounts via swipe");
            mActivity.scheduleTransactionListRetrieval();
        });

        Data.accounts.addObserver(
                (o, arg) -> mActivity.runOnUiThread(() -> modelAdapter.notifyDataSetChanged()));
    }
    private void update_account_table() {
        if (this.getContext() == null) return;

        AccountSummaryViewModel.scheduleAccountListReload();
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
        AccountSummaryViewModel.commitSelections(mActivity);
        stopSelection();
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.account_summary, menu);
        optMenu = menu;

        mShowOnlyStarred = menu.findItem(R.id.menu_acc_summary_only_starred);
        if (mShowOnlyStarred == null) throw new AssertionError();
        MenuItem mCancelSelection = menu.findItem(R.id.menu_acc_summary_cancel_selection);
        if (mCancelSelection == null) throw new AssertionError();
        MenuItem mConfirmSelection = menu.findItem(R.id.menu_acc_summary_confirm_selection);
        if (mConfirmSelection == null) throw new AssertionError();

        Data.optShowOnlyStarred.addObserver((o, arg) -> {
            boolean newValue = Data.optShowOnlyStarred.get();
            Log.d("pref", String.format("pref change came (%s)", newValue ? "true" : "false"));
            mShowOnlyStarred.setChecked(newValue);
            update_account_table();
        });

        mShowOnlyStarred.setChecked(Data.optShowOnlyStarred.get());

        Log.d("menu", "Accounts: onCreateOptionsMenu called");

        mShowOnlyStarred.setOnMenuItemClickListener(item -> {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mActivity);
            SharedPreferences.Editor editor = pref.edit();
            boolean flag = item.isChecked();
            editor.putBoolean(PREF_KEY_SHOW_ONLY_STARRED_ACCOUNTS, !flag);
            Log.d("pref",
                    "Setting show only starred accounts pref to " + (flag ? "false" : "true"));
            editor.apply();

            return true;
        });

        mCancelSelection.setOnMenuItemClickListener(item -> {
            stopSelection();
            return true;
        });

        mConfirmSelection.setOnMenuItemClickListener(item -> {
            AccountSummaryViewModel.commitSelections(mActivity);
            stopSelection();

            return true;
        });
    }
}
