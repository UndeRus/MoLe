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

package net.ktnx.mobileledger.ui.activity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.model.LedgerAccount;
import net.ktnx.mobileledger.ui.account_summary.AccountSummaryFragment;
import net.ktnx.mobileledger.ui.transaction_list.TransactionListFragment;

public class MainActivity extends AppCompatActivity {
    DrawerLayout drawer;
    private AccountSummaryFragment accountSummaryFragment;
    private TransactionListFragment transactionListFragment;
    private Fragment currentFragment = null;
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle =
                new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open,
                        R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        android.widget.TextView ver = drawer.findViewById(R.id.drawer_version_text);

        try {
            PackageInfo pi =
                    getApplicationContext().getPackageManager().getPackageInfo(getPackageName(), 0);
            ver.setText(pi.versionName);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        fragmentManager = getSupportFragmentManager();

        onAccountSummaryClicked(null);
    }
    public void fab_new_transaction_clicked(View view) {
        Intent intent = new Intent(this, NewTransactionActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.dummy);
    }

    public void nav_exit_clicked(View view) {
        Log.w("app", "exiting");
        finish();
    }

    public void nav_settings_clicked(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
    public void markDrawerItemCurrent(int id) {
        View item = drawer.findViewById(id);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            item.setBackgroundColor(getResources().getColor(R.color.table_row_even_bg, getTheme()));
        }
        else {
            item.setBackgroundColor(getResources().getColor(R.color.table_row_even_bg));
        }

        LinearLayout actions = drawer.findViewById(R.id.nav_actions);
        for (int i = 0; i < actions.getChildCount(); i++) {
            View view = actions.getChildAt(i);
            if (view.getId() != id) {
                view.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            }
        }
    }
    public void onOptionsMenuClicked(MenuItem menuItem) {
        ContextMenu.ContextMenuInfo info = menuItem.getMenuInfo();
        switch (menuItem.getItemId()) {
            case R.id.menu_acc_summary_cancel_selection:
                if (accountSummaryFragment != null)
                    accountSummaryFragment.onCancelAccSelection(menuItem);
                break;
            case R.id.menu_acc_summary_confirm_selection:
                if (accountSummaryFragment != null)
                    accountSummaryFragment.onConfirmAccSelection(menuItem);
                break;
            case R.id.menu_acc_summary_only_starred:
                if (accountSummaryFragment != null)
                    accountSummaryFragment.onShowOnlyStarredClicked(menuItem);
                break;
            case R.id.menu_transaction_list_filter:
                if (transactionListFragment != null)
                    transactionListFragment.onShowFilterClick(menuItem);
                break;
            default:
                Log.e("menu", String.format("Menu item %d not handled", menuItem.getItemId()));
        }
    }
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.clearAccountNameFilter:
                if (transactionListFragment != null)
                    transactionListFragment.onClearAccountNameClick(view);
                break;
            default:
                Log.e("click", String.format("View %d click not handled", view.getId()));
        }
    }
    public void onAccountSummaryClicked(View view) {
        drawer.closeDrawers();

        resetFragmentBackStack();

        showAccountSummaryFragment();
    }
    private void showAccountSummaryFragment() {
        FragmentTransaction ft = fragmentManager.beginTransaction();
        accountSummaryFragment = new AccountSummaryFragment();
        ft.replace(R.id.root_frame, accountSummaryFragment);
        ft.commit();
        currentFragment = accountSummaryFragment;
    }
    public void onLatestTransactionsClicked(View view) {
        drawer.closeDrawers();

        resetFragmentBackStack();

        showTransactionsFragment(null);
    }
    private void resetFragmentBackStack() {
//        fragmentManager.popBackStack(0, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }
    private void showTransactionsFragment(LedgerAccount account) {
        FragmentTransaction ft = fragmentManager.beginTransaction();
        if (transactionListFragment == null) {
            Log.d("flow", "MainActivity creating TransactionListFragment");
            transactionListFragment = new TransactionListFragment();
        }
        Bundle bundle = new Bundle();
        if (account != null) {
            bundle.putString(TransactionListFragment.BUNDLE_KEY_FILTER_ACCOUNT_NAME,
                    account.getName());
        }
        transactionListFragment.setArguments(bundle);
        ft.replace(R.id.root_frame, transactionListFragment);
        if (account != null)
            ft.addToBackStack(getResources().getString(R.string.title_activity_transaction_list));
        ft.commit();

        currentFragment = transactionListFragment;
    }
    public void showAccountTransactions(LedgerAccount account) {
        showTransactionsFragment(account);
    }
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
        else {
            Log.d("fragments",
                    String.format("manager stack: %d", fragmentManager.getBackStackEntryCount()));

            super.onBackPressed();
        }
    }

}
