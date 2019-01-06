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

package net.ktnx.mobileledger.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.async.RetrieveTransactionsTask;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerAccount;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
import net.ktnx.mobileledger.ui.MobileLedgerListFragment;
import net.ktnx.mobileledger.ui.account_summary.AccountSummaryFragment;
import net.ktnx.mobileledger.ui.transaction_list.TransactionListFragment;
import net.ktnx.mobileledger.utils.MLDB;

import java.lang.ref.WeakReference;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    public MobileLedgerListFragment currentFragment = null;
    DrawerLayout drawer;
    private AccountSummaryFragment accountSummaryFragment;
    private TransactionListFragment transactionListFragment;
    private FragmentManager fragmentManager;
    private TextView tvLastUpdate;
    private RetrieveTransactionsTask retrieveTransactionsTask;
    private View bTransactionListCancelDownload;
    private ProgressBar progressBar;
    private LinearLayout progressLayout;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;

    @Override
    protected void onStart() {
        super.onStart();

        Data.lastUpdateDate.set(null);
        updateLastUpdateTextFromDB();
        Date lastUpdate = Data.lastUpdateDate.get();

        long now = new Date().getTime();
        if ((lastUpdate == null) || (now > (lastUpdate.getTime() + (24 * 3600 * 1000)))) {
            if (lastUpdate == null) Log.d("db::", "WEB data never fetched. scheduling a fetch");
            else Log.d("db",
                    String.format("WEB data last fetched at %1.3f and now is %1.3f. re-fetching",
                            lastUpdate.getTime() / 1000f, now / 1000f));

            scheduleTransactionListRetrieval();
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Data.profile.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                MobileLedgerProfile profile = Data.profile.get();
                runOnUiThread(() -> {
                    if (profile == null) toolbar.setSubtitle("");
                    else toolbar.setSubtitle(profile.getName());
                });
            }
        });

        String profileUUID = MLDB.get_option_value(MLDB.OPT_PROFILE_UUID, null);
        if (profileUUID == null) {
            SharedPreferences backend = getSharedPreferences("backend", MODE_PRIVATE);
            Log.d("profiles", "Migrating from preferences to profiles");
            // migration to multiple profiles
            profileUUID = UUID.randomUUID().toString();
            MobileLedgerProfile profile = new MobileLedgerProfile(profileUUID, "default",
                    backend.getString("backend_url", ""),
                    backend.getBoolean("backend_use_http_auth", false),
                    backend.getString("backend_auth_user", null),
                    backend.getString("backend_auth_password", null));
            profile.storeInDB();
            SharedPreferences.Editor editor = backend.edit();
            editor.clear();
            editor.apply();
            Data.profile.set(profile);
            MLDB.set_option_value(MLDB.OPT_PROFILE_UUID, profileUUID);
        }
        else {
            MobileLedgerProfile profile = MobileLedgerProfile.loadUUIDFromDB(profileUUID);
            Data.profile.set(profile);
        }

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

        tvLastUpdate = findViewById(R.id.transactions_last_update);

        bTransactionListCancelDownload = findViewById(R.id.transaction_list_cancel_download);
        progressBar = findViewById(R.id.transaction_list_progress_bar);
        if (progressBar == null)
            throw new RuntimeException("Can't get hold on the transaction value progress bar");
        progressLayout = findViewById(R.id.transaction_progress_layout);
        if (progressLayout == null) throw new RuntimeException(
                "Can't get hold on the transaction value progress bar layout");

        fragmentManager = getSupportFragmentManager();
        mSectionsPagerAdapter = new SectionsPagerAdapter(fragmentManager);

        mViewPager = findViewById(R.id.root_frame);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        markDrawerItemCurrent(R.id.nav_account_summary);
                        break;
                    case 1:
                        markDrawerItemCurrent(R.id.nav_latest_transactions);
                        break;
                    default:
                        Log.e("MainActivity", String.format("Unexpected page index %d", position));
                }

                super.onPageSelected(position);
            }
        });

        Data.lastUpdateDate.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                Log.d("main", "lastUpdateDate changed");
                runOnUiThread(() -> {
                    Date date = Data.lastUpdateDate.get();
                    if (date == null) {
                        tvLastUpdate.setText(R.string.transaction_last_update_never);
                    }
                    else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            tvLastUpdate.setText(date.toInstant().atZone(ZoneId.systemDefault())
                                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                        }
                        else {
                            tvLastUpdate.setText(date.toLocaleString());
                        }
                    }
                });
            }
        });
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
        TextView item = drawer.findViewById(id);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            item.setBackgroundColor(getResources().getColor(R.color.table_row_even_bg, getTheme()));
        }
        else {
            item.setBackgroundColor(getResources().getColor(R.color.table_row_even_bg));
        }

        setTitle(item.getText());

        @ColorInt int transparent = getResources().getColor(android.R.color.transparent);

        LinearLayout actions = drawer.findViewById(R.id.nav_actions);
        for (int i = 0; i < actions.getChildCount(); i++) {
            View view = actions.getChildAt(i);
            if (view.getId() != id) {
                view.setBackgroundColor(transparent);
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

        showAccountSummaryFragment();
    }
    private void showAccountSummaryFragment() {
        mViewPager.setCurrentItem(0, true);
//        FragmentTransaction ft = fragmentManager.beginTransaction();
//        accountSummaryFragment = new AccountSummaryFragment();
//        ft.replace(R.id.root_frame, accountSummaryFragment);
//        ft.commit();
//        currentFragment = accountSummaryFragment;
    }
    public void onLatestTransactionsClicked(View view) {
        drawer.closeDrawers();

        showTransactionsFragment(null);
    }
    private void resetFragmentBackStack() {
//        fragmentManager.popBackStack(0, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }
    private void showTransactionsFragment(LedgerAccount account) {
        mViewPager.setCurrentItem(1, true);
//        FragmentTransaction ft = fragmentManager.beginTransaction();
//        if (transactionListFragment == null) {
//            Log.d("flow", "MainActivity creating TransactionListFragment");
//            transactionListFragment = new TransactionListFragment();
//        }
//        Bundle bundle = new Bundle();
//        if (account != null) {
//            bundle.putString(TransactionListFragment.BUNDLE_KEY_FILTER_ACCOUNT_NAME,
//                    account.getName());
//        }
//        transactionListFragment.setArguments(bundle);
//        ft.replace(R.id.root_frame, transactionListFragment);
//        if (account != null)
//            ft.addToBackStack(getResources().getString(R.string.title_activity_transaction_list));
//        ft.commit();
//
//        currentFragment = transactionListFragment;
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
    public void updateLastUpdateTextFromDB() {
        {
            long last_update = MLDB.get_option_value(MLDB.OPT_TRANSACTION_LIST_STAMP, 0L);

            Log.d("transactions", String.format("Last update = %d", last_update));
            if (last_update == 0) {
                Data.lastUpdateDate.set(null);
            }
            else {
                Data.lastUpdateDate.set(new Date(last_update));
            }
        }
    }
    public void scheduleTransactionListRetrieval() {
        retrieveTransactionsTask = new RetrieveTransactionsTask(new WeakReference<>(this));

        retrieveTransactionsTask.execute();
        bTransactionListCancelDownload.setEnabled(true);
    }
    public void onStopTransactionRefreshClick(View view) {
        Log.d("interactive", "Cancelling transactions refresh");
        if (retrieveTransactionsTask != null) retrieveTransactionsTask.cancel(false);
        bTransactionListCancelDownload.setEnabled(false);
    }
    public void onRetrieveDone(boolean success) {
        progressLayout.setVisibility(View.GONE);
        updateLastUpdateTextFromDB();
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
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Log.d("main", String.format("Switching to gragment %d", position));
            switch (position) {
                case 0:
                    return new AccountSummaryFragment();
                case 1:
                    return new TransactionListFragment();
                default:
                    throw new IllegalStateException(
                            String.format("Unexpected fragment index: " + "%d", position));
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    }

}
