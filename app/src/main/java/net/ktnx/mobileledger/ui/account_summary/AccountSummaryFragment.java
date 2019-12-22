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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.ui.MobileLedgerListFragment;
import net.ktnx.mobileledger.ui.activity.MainActivity;
import net.ktnx.mobileledger.utils.Colors;

import org.jetbrains.annotations.NotNull;

import static net.ktnx.mobileledger.utils.Logger.debug;

public class AccountSummaryFragment extends MobileLedgerListFragment {
    public AccountSummaryAdapter modelAdapter;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        debug("flow", "AccountSummaryFragment.onCreate()");
        setHasOptionsMenu(true);

        Data.backgroundTasksRunning.observe(this.getViewLifecycleOwner(),
                this::onBackgroundTaskRunningChanged);
    }
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        debug("flow", "AccountSummaryFragment.onAttach()");
        mActivity = (MainActivity) context;
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        debug("flow", "AccountSummaryFragment.onCreateView()");
        return inflater.inflate(R.layout.account_summary_fragment, container, false);
    }

    @Override

    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        debug("flow", "AccountSummaryFragment.onActivityCreated()");
        super.onActivityCreated(savedInstanceState);

        modelAdapter = new AccountSummaryAdapter();

        mActivity.mAccountSummaryFragment = this;
        root = mActivity.findViewById(R.id.account_root);
        LinearLayoutManager llm = new LinearLayoutManager(mActivity);
        llm.setOrientation(RecyclerView.VERTICAL);
        root.setLayoutManager(llm);
        root.setAdapter(modelAdapter);
        DividerItemDecoration did =
                new DividerItemDecoration(mActivity, DividerItemDecoration.VERTICAL);
        root.addItemDecoration(did);

        mActivity.fabShouldShow();

        manageFabOnScroll();

        swiper = mActivity.findViewById(R.id.account_swiper);
        Colors.themeWatch.observe(getViewLifecycleOwner(), this::themeChanged);
        swiper.setOnRefreshListener(() -> {
            debug("ui", "refreshing accounts via swipe");
            Data.scheduleTransactionListRetrieval(mActivity);
        });

        Data.accounts.addObserver(
                (o, arg) -> mActivity.runOnUiThread(() -> modelAdapter.notifyDataSetChanged()));
    }
/*
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
    public void onCreateOptionsMenu(@NotNull Menu menu, @NotNull MenuInflater inflater) {
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
            debug("pref", String.format("pref change came (%s)", newValue ? "true" : "false"));
            mShowOnlyStarred.setChecked(newValue);
            update_account_table();
        });

        mShowOnlyStarred.setChecked(Data.optShowOnlyStarred.get());

        debug("menu", "Accounts: onCreateOptionsMenu called");

        mShowOnlyStarred.setOnMenuItemClickListener(item -> {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mActivity);
            SharedPreferences.Editor editor = pref.edit();
            boolean flag = item.isChecked();
            editor.putBoolean(PREF_KEY_SHOW_ONLY_STARRED_ACCOUNTS, !flag);
            debug("pref",
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
*/
}
