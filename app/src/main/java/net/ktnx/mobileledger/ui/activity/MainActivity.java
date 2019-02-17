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

package net.ktnx.mobileledger.ui.activity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.async.RefreshDescriptionsTask;
import net.ktnx.mobileledger.async.RetrieveTransactionsTask;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerAccount;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
import net.ktnx.mobileledger.ui.account_summary.AccountSummaryFragment;
import net.ktnx.mobileledger.ui.transaction_list.TransactionListFragment;
import net.ktnx.mobileledger.utils.Colors;
import net.ktnx.mobileledger.utils.MLDB;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends CrashReportingActivity {
    DrawerLayout drawer;
    private FragmentManager fragmentManager;
    private TextView tvLastUpdate;
    private RetrieveTransactionsTask retrieveTransactionsTask;
    private View bTransactionListCancelDownload;
    private ProgressBar progressBar;
    private LinearLayout progressLayout;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private FloatingActionButton fab;

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
        fab = findViewById(R.id.btn_add_transaction);

        Data.profile.addObserver((o, arg) -> {
            MobileLedgerProfile profile = Data.profile.get();
            runOnUiThread(() -> {
                if (profile == null) setTitle(R.string.app_name);
                else setTitle(profile.getName());
                updateLastUpdateTextFromDB();
                if (profile.isPostingPermitted()) {
                    toolbar.setSubtitle(null);
                    fab.show();
                }
                else {
                    toolbar.setSubtitle(R.string.profile_subitlte_read_only);
                    fab.hide();
                }
            });
        });

        drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle =
                new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open,
                        R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        TextView ver = drawer.findViewById(R.id.drawer_version_text);

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

        markDrawerItemCurrent(R.id.nav_account_summary);

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

        Data.lastUpdateDate.addObserver((o, arg) -> {
            Log.d("main", "lastUpdateDate changed");
            runOnUiThread(() -> {
                Date date = Data.lastUpdateDate.get();
                if (date == null) {
                    tvLastUpdate.setText(R.string.transaction_last_update_never);
                }
                else {
                    final String text = DateFormat.getDateTimeInstance().format(date);
                    tvLastUpdate.setText(text);
                    Log.d("despair", String.format("Date formatted: %s", text));
                }
            });
        });

        findViewById(R.id.btn_no_profiles_add).setOnClickListener(v -> startAddProfileActivity());

        findViewById(R.id.btn_add_transaction).setOnClickListener(this::fabNewTransactionClicked);
    }
    @Override
    protected void onResume() {
        super.onResume();
        setupProfile();
    }
    private void startAddProfileActivity() {
        Intent intent = new Intent(this, ProfileListActivity.class);
        Bundle args = new Bundle();
        args.putInt(ProfileListActivity.ARG_ACTION, ProfileListActivity.ACTION_EDIT_PROFILE);
        args.putInt(ProfileListActivity.ARG_PROFILE_INDEX, ProfileListActivity.PROFILE_INDEX_NONE);
        intent.putExtras(args);
        startActivity(intent, args);
    }
    private void setupProfile() {
        String profileUUID = MLDB.getOption(MLDB.OPT_PROFILE_UUID, null);
        MobileLedgerProfile profile;

        profile = MobileLedgerProfile.loadAllFromDB(profileUUID);

        if (Data.profiles.getList().isEmpty()) {
            findViewById(R.id.no_profiles_layout).setVisibility(View.VISIBLE);
            findViewById(R.id.pager_layout).setVisibility(View.GONE);
            return;
        }

        findViewById(R.id.pager_layout).setVisibility(View.VISIBLE);
        findViewById(R.id.no_profiles_layout).setVisibility(View.GONE);

        if (profile == null) profile = Data.profiles.get(0);

        if (profile == null) throw new AssertionError("profile must have a value");

        Data.setCurrentProfile(profile);
    }
    public void fabNewTransactionClicked(View view) {
        Intent intent = new Intent(this, NewTransactionActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.dummy);
    }
    public void navSettingsClicked(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
        drawer.closeDrawers();
    }
    public void markDrawerItemCurrent(int id) {
        TextView item = drawer.findViewById(id);
        item.setBackgroundColor(Colors.tableRowDarkBG);

        LinearLayout actions = drawer.findViewById(R.id.nav_actions);
        for (int i = 0; i < actions.getChildCount(); i++) {
            View view = actions.getChildAt(i);
            if (view.getId() != id) {
                view.setBackgroundColor(Color.TRANSPARENT);
            }
        }
    }
    public void onAccountSummaryClicked(View view) {
        drawer.closeDrawers();

        showAccountSummaryFragment();
    }
    private void showAccountSummaryFragment() {
        mViewPager.setCurrentItem(0, true);
        TransactionListFragment.accountFilter.set(null);
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
        if (account != null) TransactionListFragment.accountFilter.set(account.getName());
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
            final MobileLedgerProfile profile = Data.profile.get();
            long last_update =
                    (profile != null) ? profile.getLongOption(MLDB.OPT_LAST_SCRAPE, 0L) : 0;

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
        if (Data.profile.get() == null) return;

        retrieveTransactionsTask = new RetrieveTransactionsTask(new WeakReference<>(this));

        retrieveTransactionsTask.execute();
        bTransactionListCancelDownload.setEnabled(true);
    }
    public void onStopTransactionRefreshClick(View view) {
        Log.d("interactive", "Cancelling transactions refresh");
        if (retrieveTransactionsTask != null) retrieveTransactionsTask.cancel(false);
        bTransactionListCancelDownload.setEnabled(false);
    }
    public void onRetrieveDone(String error) {
        progressLayout.setVisibility(View.GONE);

        if (error == null) {
            updateLastUpdateTextFromDB();

            new RefreshDescriptionsTask().execute();
        }
        else Toast.makeText(this, error, Toast.LENGTH_LONG).show();
    }
    public void onRetrieveStart() {
        progressBar.setIndeterminateTintList(ColorStateList.valueOf(Colors.primary));
        progressBar.setProgressTintList(ColorStateList.valueOf(Colors.primary));
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
    public void navProfilesClicked(View view) {
        drawer.closeDrawers();
        Intent intent = new Intent(this, ProfileListActivity.class);
        startActivity(intent);
    }
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Log.d("main", String.format("Switching to fragment %d", position));
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
    public void fabShouldShow() {
        MobileLedgerProfile profile = Data.profile.get();
        if ((profile != null) && profile.isPostingPermitted()) fab.show();
    }
}
