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
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.async.RefreshDescriptionsTask;
import net.ktnx.mobileledger.async.RetrieveTransactionsTask;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerAccount;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
import net.ktnx.mobileledger.ui.account_summary.AccountSummaryFragment;
import net.ktnx.mobileledger.ui.profiles.ProfileDetailFragment;
import net.ktnx.mobileledger.ui.profiles.ProfilesRecyclerViewAdapter;
import net.ktnx.mobileledger.ui.transaction_list.TransactionListFragment;
import net.ktnx.mobileledger.ui.transaction_list.TransactionListViewModel;
import net.ktnx.mobileledger.utils.Colors;
import net.ktnx.mobileledger.utils.MLDB;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

public class MainActivity extends ProfileThemedActivity {
    private static final String STATE_CURRENT_PAGE = "current_page";
    private static final String BUNDLE_SAVED_STATE = "bundle_savedState";
    public AccountSummaryFragment mAccountSummaryFragment;
    DrawerLayout drawer;
    private LinearLayout profileListContainer;
    private View profileListHeadArrow, profileListHeadMore, profileListHeadCancel;
    private LinearLayout profileListHeadMoreAndCancel;
    private FragmentManager fragmentManager;
    private TextView tvLastUpdate;
    private RetrieveTransactionsTask retrieveTransactionsTask;
    private View bTransactionListCancelDownload;
    private ProgressBar progressBar;
    private LinearLayout progressLayout;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private FloatingActionButton fab;
    private boolean profileModificationEnabled = false;
    private boolean profileListExpanded = false;
    private ProfilesRecyclerViewAdapter mProfileListAdapter;
    @Override
    protected void onStart() {
        super.onStart();

        setupProfile();

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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_CURRENT_PAGE, mViewPager.getCurrentItem());
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        fab = findViewById(R.id.btn_add_transaction);
        profileListContainer = findViewById(R.id.nav_profile_list_container);
        profileListHeadArrow = findViewById(R.id.nav_profiles_arrow);
        profileListHeadMore = findViewById(R.id.nav_profiles_start_edit);
        profileListHeadCancel = findViewById(R.id.nav_profiles_cancel_edit);
        profileListHeadMoreAndCancel = findViewById(R.id.nav_profile_list_head_buttons);
        drawer = findViewById(R.id.drawer_layout);
        tvLastUpdate = findViewById(R.id.transactions_last_update);
        bTransactionListCancelDownload = findViewById(R.id.transaction_list_cancel_download);
        progressBar = findViewById(R.id.transaction_list_progress_bar);
        progressLayout = findViewById(R.id.transaction_progress_layout);
        fragmentManager = getSupportFragmentManager();
        mSectionsPagerAdapter = new SectionsPagerAdapter(fragmentManager);
        mViewPager = findViewById(R.id.root_frame);

        Bundle extra = getIntent().getBundleExtra(BUNDLE_SAVED_STATE);
        if (extra != null && savedInstanceState == null) savedInstanceState = extra;


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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

                int newProfileTheme = profile.getThemeId();
                if (newProfileTheme != Colors.profileThemeId) {
                    Log.d("profiles", String.format("profile theme %d → %d", Colors.profileThemeId,
                            newProfileTheme));
                    profileThemeChanged();
                    Colors.profileThemeId = newProfileTheme;
                }
            });
        });

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

        if (progressBar == null)
            throw new RuntimeException("Can't get hold on the transaction value progress bar");
        if (progressLayout == null) throw new RuntimeException(
                "Can't get hold on the transaction value progress bar layout");

        markDrawerItemCurrent(R.id.nav_account_summary);

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

        if (savedInstanceState != null) {
            int currentPage = savedInstanceState.getInt(STATE_CURRENT_PAGE, -1);
            if (currentPage != -1) {
                mViewPager.setCurrentItem(currentPage, false);
            }
        }

        Data.lastUpdateDate.addObserver((o, arg) -> {
            Log.d("main", "lastUpdateDate changed");
            runOnUiThread(this::updateLastUpdateDisplay);
        });

        updateLastUpdateDisplay();

        findViewById(R.id.btn_no_profiles_add)
                .setOnClickListener(v -> startEditProfileActivity(null));

        findViewById(R.id.btn_add_transaction).setOnClickListener(this::fabNewTransactionClicked);

        findViewById(R.id.nav_new_profile_button)
                .setOnClickListener(v -> startEditProfileActivity(null));

        RecyclerView root = findViewById(R.id.nav_profile_list);
        if (root == null)
            throw new RuntimeException("Can't get hold on the transaction value view");

        mProfileListAdapter = new ProfilesRecyclerViewAdapter();
        root.setAdapter(mProfileListAdapter);

        mProfileListAdapter.addEditingProfilesObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                if (mProfileListAdapter.isEditingProfiles()) {
                    profileListHeadArrow.clearAnimation();
                    profileListHeadArrow.setVisibility(View.GONE);
                    profileListHeadMore.setVisibility(View.GONE);
                    profileListHeadCancel.setVisibility(View.VISIBLE);
                }
                else {
                    profileListHeadArrow.setRotation(180f);
                    profileListHeadArrow.setVisibility(View.VISIBLE);
                    profileListHeadCancel.setVisibility(View.GONE);
                    profileListHeadMore.setVisibility(View.GONE);
                    profileListHeadMore
                            .setVisibility(profileListExpanded ? View.VISIBLE : View.GONE);
                }
            }
        });

        LinearLayoutManager llm = new LinearLayoutManager(this);

        llm.setOrientation(RecyclerView.VERTICAL);
        root.setLayoutManager(llm);

        profileListHeadMore.setOnClickListener((v) -> mProfileListAdapter.flipEditingProfiles());
        profileListHeadCancel.setOnClickListener((v) -> mProfileListAdapter.flipEditingProfiles());
        profileListHeadMoreAndCancel
                .setOnClickListener((v) -> mProfileListAdapter.flipEditingProfiles());

        drawer.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                collapseProfileList();
            }
        });
    }
    private void updateLastUpdateDisplay() {
        TextView v = findViewById(R.id.transactions_last_update);
        Date date = Data.lastUpdateDate.get();
        if (date == null) {
            v.setText(R.string.transaction_last_update_never);
            Log.d("main", "no last update date :(");
        }
        else {
            final String text = DateFormat.getDateTimeInstance().format(date);
            v.setText(text);
            Log.d("main", String.format("Date formatted: %s", text));
        }
    }
    private void profileThemeChanged() {
        setupProfileColors();

        Bundle bundle = new Bundle();
        onSaveInstanceState(bundle);
        // restart activity to reflect theme change
        finish();
        Intent intent = new Intent(this, this.getClass());
        intent.putExtra(BUNDLE_SAVED_STATE, bundle);
        startActivity(intent);
    }
    public void startEditProfileActivity(MobileLedgerProfile profile) {
        Intent intent = new Intent(this, ProfileDetailActivity.class);
        Bundle args = new Bundle();
        if (profile != null) {
            int index = Data.getProfileIndex(profile);
            if (index != -1) intent.putExtra(ProfileDetailFragment.ARG_ITEM_ID, index);
        }
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

            new RefreshDescriptionsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            TransactionListViewModel.scheduleTransactionListReload();
        }
        else Toast.makeText(this, error, Toast.LENGTH_LONG).show();
    }
    public void onRetrieveStart() {
        bTransactionListCancelDownload.setEnabled(true);
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
    public void fabShouldShow() {
        MobileLedgerProfile profile = Data.profile.get();
        if ((profile != null) && profile.isPostingPermitted()) fab.show();
    }
    public void navProfilesHeadClicked(View view) {
        if (profileListExpanded) {
            collapseProfileList();
        }
        else {
            expandProfileList();
        }
    }
    private void expandProfileList() {
        profileListExpanded = true;


        profileListContainer.setVisibility(View.VISIBLE);
        profileListContainer.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_down));
        profileListHeadArrow.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rotate_180));
        profileListHeadMore.setVisibility(View.VISIBLE);
        profileListHeadMore.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        findViewById(R.id.nav_profile_list).setMinimumHeight(
                (int) (getResources().getDimension(R.dimen.thumb_row_height) *
                       Data.profiles.size()));
    }
    private void collapseProfileList() {
        profileListExpanded = false;

        final Animation animation = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }
            @Override
            public void onAnimationEnd(Animation animation) {
                profileListContainer.setVisibility(View.GONE);
            }
            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mProfileListAdapter.stopEditingProfiles();

        profileListContainer.startAnimation(animation);
        profileListHeadArrow.setRotation(0f);
        profileListHeadArrow
                .startAnimation(AnimationUtils.loadAnimation(this, R.anim.rotate_180_back));
        profileListHeadMore.setVisibility(View.GONE);
    }
    public void onProfileRowClicked(View v) {
        Data.setCurrentProfile((MobileLedgerProfile) v.getTag());
    }
    public void enableProfileModifications() {
        profileModificationEnabled = true;
        ViewGroup profileList = findViewById(R.id.nav_profile_list);
        for (int i = 0; i < profileList.getChildCount(); i++) {
            View aRow = profileList.getChildAt(i);
            aRow.findViewById(R.id.profile_list_edit_button).setVisibility(View.VISIBLE);
            aRow.findViewById(R.id.profile_list_rearrange_handle).setVisibility(View.VISIBLE);
        }
        // FIXME enable rearranging

    }
    public void disableProfileModifications() {
        profileModificationEnabled = false;
        ViewGroup profileList = findViewById(R.id.nav_profile_list);
        for (int i = 0; i < profileList.getChildCount(); i++) {
            View aRow = profileList.getChildAt(i);
            aRow.findViewById(R.id.profile_list_edit_button).setVisibility(View.GONE);
            aRow.findViewById(R.id.profile_list_rearrange_handle).setVisibility(View.INVISIBLE);
        }
        // FIXME disable rearranging

    }
    public void onAccountSummaryRowViewClicked(View view) {
        ViewGroup row = (ViewGroup) view.getParent();
        if (view.getId() == R.id.account_expander_container) {
            Log.d("accounts", "Account expander clicked");
            LedgerAccount acc = (LedgerAccount) row.getTag();
            if (!acc.hasSubAccounts()) return;

            boolean wasExpanded = acc.isExpanded();

            view.clearAnimation();
            ViewPropertyAnimator animator = view.animate();

            acc.toggleExpanded();
            Data.profile.get().storeAccount(MLDB.getWritableDatabase(), acc);

            if (wasExpanded) {
                Log.d("accounts", String.format("Collapsing account '%s'", acc.getName()));
                animator.rotationBy(180);

                // removing all child accounts from the view
                int start = -1, count = 0;
                int i = 0;
                final ArrayList<LedgerAccount> accountList = Data.accounts.get();
                for (LedgerAccount a : accountList) {
                    if (acc.isParentOf(a)) {
                        if (start == -1) {
                            start = i;
                        }
                        count++;
                    }
                    else {
                        if (start != -1) {
                            break;
                        }
                    }
                    i++;
                }

                if (start != -1) {
                    for (int j = 0; j < count; j++) {
                        Log.d("accounts", String.format("Removing item %d: %s", start + j,
                                accountList.get(start).getName()));
                        accountList.remove(start);
                    }

                    mAccountSummaryFragment.modelAdapter.notifyItemRangeRemoved(start, count);
                }
            }
            else {
                Log.d("accounts", String.format("Expanding account '%s'", acc.getName()));
                animator.rotationBy(-180);
                ArrayList<LedgerAccount> accounts = Data.accounts.get();
                List<LedgerAccount> children = Data.profile.get().loadVisibleChildAccountsOf(acc);
                int parentPos = accounts.indexOf(acc);
                if (parentPos == -1) throw new RuntimeException(
                        "Can't find index of clicked account " + acc.getName());
                accounts.addAll(parentPos + 1, children);
                mAccountSummaryFragment.modelAdapter
                        .notifyItemRangeInserted(parentPos + 1, children.size());
            }
        }
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
//                    Log.d("flow", "Creating account summary fragment");
                    return mAccountSummaryFragment = new AccountSummaryFragment();
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
