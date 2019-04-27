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
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Icon;
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
import net.ktnx.mobileledger.async.DbOpQueue;
import net.ktnx.mobileledger.async.RefreshDescriptionsTask;
import net.ktnx.mobileledger.async.RetrieveTransactionsTask;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerAccount;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
import net.ktnx.mobileledger.ui.account_summary.AccountSummaryAdapter;
import net.ktnx.mobileledger.ui.account_summary.AccountSummaryFragment;
import net.ktnx.mobileledger.ui.account_summary.AccountSummaryViewModel;
import net.ktnx.mobileledger.ui.profiles.ProfileDetailFragment;
import net.ktnx.mobileledger.ui.profiles.ProfilesRecyclerViewAdapter;
import net.ktnx.mobileledger.ui.transaction_list.TransactionListFragment;
import net.ktnx.mobileledger.ui.transaction_list.TransactionListViewModel;
import net.ktnx.mobileledger.utils.Colors;
import net.ktnx.mobileledger.utils.LockHolder;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.MLDB;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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

import static net.ktnx.mobileledger.utils.Logger.debug;

public class MainActivity extends ProfileThemedActivity {
    public static final String STATE_CURRENT_PAGE = "current_page";
    public static final String BUNDLE_SAVED_STATE = "bundle_savedState";
    public static final String STATE_ACC_FILTER = "account_filter";
    public AccountSummaryFragment mAccountSummaryFragment;
    DrawerLayout drawer;
    private LinearLayout profileListContainer;
    private View profileListHeadArrow, profileListHeadMore, profileListHeadCancel;
    private FragmentManager fragmentManager;
    private RetrieveTransactionsTask retrieveTransactionsTask;
    private View bTransactionListCancelDownload;
    private ProgressBar progressBar;
    private LinearLayout progressLayout;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private FloatingActionButton fab;
    private boolean profileListExpanded = false;
    private ProfilesRecyclerViewAdapter mProfileListAdapter;
    private int mCurrentPage;
    private String mAccountFilter;
    private boolean mBackMeansToAccountList = false;
    private Observer profileObserver;
    private Observer profilesObserver;
    private Toolbar mToolbar;
    private DrawerLayout.SimpleDrawerListener drawerListener;
    private ActionBarDrawerToggle barDrawerToggle;
    private ViewPager.SimpleOnPageChangeListener pageChangeListener;
    private Observer editingProfilesObserver;
    @Override
    protected void onStart() {
        super.onStart();

        debug("flow", "MainActivity.onStart()");
        mViewPager.setCurrentItem(mCurrentPage, false);
        if (mAccountFilter != null) showTransactionsFragment(mAccountFilter);
        else Data.accountFilter.setValue(null);

    }
    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_CURRENT_PAGE, mViewPager.getCurrentItem());
        if (mAccountFilter != null) outState.putString(STATE_ACC_FILTER, mAccountFilter);
    }
    @Override
    protected void onDestroy() {
        mSectionsPagerAdapter = null;
        Data.profile.deleteObserver(profileObserver);
        profileObserver = null;
        Data.profiles.deleteObserver(profilesObserver);
        profilesObserver = null;
        RecyclerView root = findViewById(R.id.nav_profile_list);
        if (root != null) root.setAdapter(null);
        if (drawer != null) drawer.removeDrawerListener(drawerListener);
        drawerListener = null;
        if (drawer != null) drawer.removeDrawerListener(barDrawerToggle);
        barDrawerToggle = null;
        if (mViewPager != null) mViewPager.removeOnPageChangeListener(pageChangeListener);
        pageChangeListener = null;
        if (mProfileListAdapter != null)
            mProfileListAdapter.deleteEditingProfilesObserver(editingProfilesObserver);
        editingProfilesObserver = null;
        super.onDestroy();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        debug("flow", "MainActivity.onCreate()");
        int profileColor = Data.retrieveCurrentThemeIdFromDb();
        Colors.setupTheme(this, profileColor);
        Colors.profileThemeId = profileColor;
        setContentView(R.layout.activity_main);

        fab = findViewById(R.id.btn_add_transaction);
        profileListContainer = findViewById(R.id.nav_profile_list_container);
        profileListHeadArrow = findViewById(R.id.nav_profiles_arrow);
        profileListHeadMore = findViewById(R.id.nav_profiles_start_edit);
        profileListHeadCancel = findViewById(R.id.nav_profiles_cancel_edit);
        LinearLayout profileListHeadMoreAndCancel =
                findViewById(R.id.nav_profile_list_head_buttons);
        drawer = findViewById(R.id.drawer_layout);
        bTransactionListCancelDownload = findViewById(R.id.transaction_list_cancel_download);
        progressBar = findViewById(R.id.transaction_list_progress_bar);
        progressLayout = findViewById(R.id.transaction_progress_layout);
        fragmentManager = getSupportFragmentManager();
        mSectionsPagerAdapter = new SectionsPagerAdapter(fragmentManager);
        mViewPager = findViewById(R.id.root_frame);

        Bundle extra = getIntent().getBundleExtra(BUNDLE_SAVED_STATE);
        if (extra != null && savedInstanceState == null) savedInstanceState = extra;


        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        if (profileObserver == null) {
            profileObserver = (o, arg) -> onProfileChanged(arg);
            Data.profile.addObserver(profileObserver);
        }

        if (profilesObserver == null) {
            profilesObserver = (o, arg) -> onProfileListChanged(arg);
            Data.profiles.addObserver(profilesObserver);
        }

        if (barDrawerToggle == null) {
            barDrawerToggle = new ActionBarDrawerToggle(this, drawer, mToolbar,
                    R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawer.addDrawerListener(barDrawerToggle);
        }
        barDrawerToggle.syncState();

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

        if (pageChangeListener == null) {
            pageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
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
                            Log.e("MainActivity",
                                    String.format("Unexpected page index %d", position));
                    }

                    super.onPageSelected(position);
                }
            };
            mViewPager.addOnPageChangeListener(pageChangeListener);
        }

        mCurrentPage = 0;
        if (savedInstanceState != null) {
            int currentPage = savedInstanceState.getInt(STATE_CURRENT_PAGE, -1);
            if (currentPage != -1) {
                mCurrentPage = currentPage;
            }
            mAccountFilter = savedInstanceState.getString(STATE_ACC_FILTER, null);
        }
        else mAccountFilter = null;

        Data.lastUpdateDate.observe(this, this::updateLastUpdateDisplay);

        findViewById(R.id.btn_no_profiles_add)
                .setOnClickListener(v -> startEditProfileActivity(null));

        findViewById(R.id.btn_add_transaction).setOnClickListener(this::fabNewTransactionClicked);

        findViewById(R.id.nav_new_profile_button)
                .setOnClickListener(v -> startEditProfileActivity(null));

        RecyclerView root = findViewById(R.id.nav_profile_list);
        if (root == null)
            throw new RuntimeException("Can't get hold on the transaction value view");

        if (mProfileListAdapter == null) mProfileListAdapter = new ProfilesRecyclerViewAdapter();
        root.setAdapter(mProfileListAdapter);

        if (editingProfilesObserver == null) {
            editingProfilesObserver = (o, arg) -> {
                if (mProfileListAdapter.isEditingProfiles()) {
                    profileListHeadMore.setVisibility(View.GONE);
                    profileListHeadMore
                            .startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));
                    profileListHeadCancel.setVisibility(View.VISIBLE);
                    profileListHeadCancel
                            .startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
                }
                else {
                    profileListHeadCancel.setVisibility(View.GONE);
                    profileListHeadCancel
                            .startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));
                    profileListHeadMore.setVisibility(View.GONE);
                    if (profileListExpanded) {
                        profileListHeadMore.setVisibility(View.VISIBLE);
                        profileListHeadMore
                                .startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
                    }
                    else profileListHeadMore.setVisibility(View.GONE);
                }
            };
            mProfileListAdapter.addEditingProfilesObserver(editingProfilesObserver);
        }

        LinearLayoutManager llm = new LinearLayoutManager(this);

        llm.setOrientation(RecyclerView.VERTICAL);
        root.setLayoutManager(llm);

        profileListHeadMore.setOnClickListener((v) -> mProfileListAdapter.flipEditingProfiles());
        profileListHeadCancel.setOnClickListener((v) -> mProfileListAdapter.flipEditingProfiles());
        profileListHeadMoreAndCancel
                .setOnClickListener((v) -> mProfileListAdapter.flipEditingProfiles());

        if (drawerListener == null) {
            drawerListener = new DrawerLayout.SimpleDrawerListener() {
                @Override
                public void onDrawerClosed(View drawerView) {
                    super.onDrawerClosed(drawerView);
                    collapseProfileList();
                }
            };
            drawer.addDrawerListener(drawerListener);
        }

        findViewById(R.id.nav_profile_list_head_layout)
                .setOnClickListener(this::navProfilesHeadClicked);
        findViewById(R.id.nav_profiles_label).setOnClickListener(this::navProfilesHeadClicked);
        boolean initialStart = Data.profile.get() == null;
        setupProfile();
        if (!initialStart) onProfileChanged(null);

        updateLastUpdateTextFromDB();
    }
    private void scheduleDataRetrievalIfStale(Date lastUpdate) {
        long now = new Date().getTime();
        if ((lastUpdate == null) || (now > (lastUpdate.getTime() + (24 * 3600 * 1000)))) {
            if (lastUpdate == null) debug("db::", "WEB data never fetched. scheduling a fetch");
            else debug("db", String.format(Locale.ENGLISH,
                    "WEB data last fetched at %1.3f and now is %1.3f. re-fetching",
                    lastUpdate.getTime() / 1000f, now / 1000f));

            scheduleTransactionListRetrieval();
        }
    }
    private void createShortcuts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        List<ShortcutInfo> shortcuts = new ArrayList<>();
        try (LockHolder ignored = Data.profiles.lockForReading()) {
            for (int i = 0; i < Data.profiles.size(); i++) {
                MobileLedgerProfile p = Data.profiles.get(i);
                if (!p.isPostingPermitted()) continue;

                ShortcutInfo si = new ShortcutInfo.Builder(this, "new_transaction_" + p.getUuid())
                        .setShortLabel(p.getName())
                        .setIcon(Icon.createWithResource(this, R.drawable.svg_thick_plus_white))
                        .setIntent(new Intent(Intent.ACTION_VIEW, null, this,
                                NewTransactionActivity.class).putExtra("profile_uuid", p.getUuid()))
                        .setRank(i).build();
                shortcuts.add(si);
            }
        }
        ShortcutManager sm = getSystemService(ShortcutManager.class);
        sm.setDynamicShortcuts(shortcuts);
    }
    private void onProfileListChanged(Object arg) {
        findViewById(R.id.nav_profile_list).setMinimumHeight(
                (int) (getResources().getDimension(R.dimen.thumb_row_height) *
                       Data.profiles.size()));

        debug("profiles", "profile list changed");
        if (arg == null) mProfileListAdapter.notifyDataSetChanged();
        else mProfileListAdapter.notifyItemChanged((int) arg);

        createShortcuts();
    }
    private void onProfileChanged(Object arg) {
        MobileLedgerProfile profile = Data.profile.get();
        MainActivity.this.runOnUiThread(() -> {

            boolean haveProfile = profile != null;
            findViewById(R.id.no_profiles_layout)
                    .setVisibility(haveProfile ? View.GONE : View.VISIBLE);
            findViewById(R.id.pager_layout)
                    .setVisibility(haveProfile ? View.VISIBLE : View.VISIBLE);

            if (profile == null) MainActivity.this.setTitle(R.string.app_name);
            else MainActivity.this.setTitle(profile.getName());
            MainActivity.this.updateLastUpdateTextFromDB();
            int old_index = -1;
            int new_index = -1;
            if (arg != null) {
                MobileLedgerProfile old = (MobileLedgerProfile) arg;
                old_index = Data.getProfileIndex(old);
                new_index = Data.getProfileIndex(profile);
            }

            if ((old_index != -1) && (new_index != -1)) {
                mProfileListAdapter.notifyItemChanged(old_index);
                mProfileListAdapter.notifyItemChanged(new_index);
            }
            else mProfileListAdapter.notifyDataSetChanged();

            MainActivity.this.collapseProfileList();

            int newProfileTheme = (profile == null) ? -1 : profile.getThemeId();
            if (newProfileTheme != Colors.profileThemeId) {
                debug("profiles", String.format("profile theme %d → %d", Colors.profileThemeId,
                        newProfileTheme));
                MainActivity.this.profileThemeChanged();
                Colors.profileThemeId = newProfileTheme;
                // profileThemeChanged would restart the activity, so no need to reload the
                // data sets below
                return;
            }
            drawer.closeDrawers();

            Data.transactions.clear();
            debug("transactions", "requesting list reload");
            TransactionListViewModel.scheduleTransactionListReload();

            Data.accounts.clear();
            AccountSummaryViewModel.scheduleAccountListReload();

            if (profile == null) {
                mToolbar.setSubtitle(null);
                fab.hide();
            }
            else {
                if (profile.isPostingPermitted()) {
                    mToolbar.setSubtitle(null);
                    fab.show();
                }
                else {
                    mToolbar.setSubtitle(R.string.profile_subitlte_read_only);
                    fab.hide();
                }
            }

            updateLastUpdateTextFromDB();
        });
    }
    private void updateLastUpdateDisplay(Date newValue) {
        LinearLayout l = findViewById(R.id.transactions_last_update_layout);
        TextView v = findViewById(R.id.transactions_last_update);
        if (newValue == null) {
            l.setVisibility(View.INVISIBLE);
            debug("main", "no last update date :(");
        }
        else {
            final String text = DateFormat.getDateTimeInstance().format(newValue);
            v.setText(text);
            l.setVisibility(View.VISIBLE);
            debug("main", String.format("Date formatted: %s", text));
        }

        scheduleDataRetrievalIfStale(newValue);
    }
    @Override
    public void finish() {
        if (profilesObserver != null) {
            Data.profiles.deleteObserver(profilesObserver);
            profilesObserver = null;
        }

        if (profileObserver != null) {
            Data.profile.deleteObserver(profileObserver);
            profileObserver = null;
        }

        super.finish();
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

        profile = Data.getProfile(profileUUID);

        if (Data.profiles.isEmpty()) {
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
        Data.accountFilter.setValue(null);
    }
    public void onLatestTransactionsClicked(View view) {
        drawer.closeDrawers();

        showTransactionsFragment((String) null);
    }
    private void showTransactionsFragment(String accName) {
        Data.accountFilter.setValue(accName);
        mViewPager.setCurrentItem(1, true);
    }
    private void showTransactionsFragment(LedgerAccount account) {
        showTransactionsFragment((account == null) ? null : account.getName());
    }
    public void showAccountTransactions(LedgerAccount account) {
        mBackMeansToAccountList = true;
        showTransactionsFragment(account);
    }
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
        else {
            if (mBackMeansToAccountList && (mViewPager.getCurrentItem() == 1)) {
                Data.accountFilter.setValue(null);
                showAccountSummaryFragment();
                mBackMeansToAccountList = false;
            }
            else {
                debug("fragments", String.format(Locale.ENGLISH, "manager stack: %d",
                        fragmentManager.getBackStackEntryCount()));

                super.onBackPressed();
            }
        }
    }
    public void updateLastUpdateTextFromDB() {
        final MobileLedgerProfile profile = Data.profile.get();
        long last_update = (profile != null) ? profile.getLongOption(MLDB.OPT_LAST_SCRAPE, 0L) : 0;

        debug("transactions", String.format(Locale.ENGLISH, "Last update = %d", last_update));
        if (last_update == 0) {
            Data.lastUpdateDate.postValue(null);
        }
        else {
            Data.lastUpdateDate.postValue(new Date(last_update));
        }
    }
    public void scheduleTransactionListRetrieval() {
        if (Data.profile.get() == null) return;

        retrieveTransactionsTask = new RetrieveTransactionsTask(new WeakReference<>(this));

        retrieveTransactionsTask.execute();
    }
    public void onStopTransactionRefreshClick(View view) {
        debug("interactive", "Cancelling transactions refresh");
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
    public void fabHide() {
        fab.hide();
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
        boolean wasExpanded = profileListExpanded;
        profileListExpanded = false;

        if (wasExpanded) {
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
            final Animation moreAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out);
            moreAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }
                @Override
                public void onAnimationEnd(Animation animation) {
                    profileListHeadMore.setVisibility(View.GONE);
                }
                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            profileListHeadMore.startAnimation(moreAnimation);
        }
        else {
            profileListContainer.setVisibility(View.GONE);
            profileListHeadArrow.setRotation(0f);
            profileListHeadMore.setVisibility(View.GONE);
        }
    }
    public void onAccountSummaryRowViewClicked(View view) {
        ViewGroup row;
        if (view.getId() == R.id.account_expander) row = (ViewGroup) view.getParent().getParent();
        else row = (ViewGroup) view.getParent();

        LedgerAccount acc = (LedgerAccount) row.getTag();
        switch (view.getId()) {
            case R.id.account_row_acc_name:
            case R.id.account_expander:
            case R.id.account_expander_container:
                debug("accounts", "Account expander clicked");
                if (!acc.hasSubAccounts()) return;

                boolean wasExpanded = acc.isExpanded();

                View arrow = row.findViewById(R.id.account_expander_container);

                arrow.clearAnimation();
                ViewPropertyAnimator animator = arrow.animate();

                acc.toggleExpanded();
                DbOpQueue.add("update accounts set expanded=? where name=? and profile=?",
                        new Object[]{acc.isExpanded(), acc.getName(), Data.profile.get().getUuid()
                        });

                if (wasExpanded) {
                    debug("accounts", String.format("Collapsing account '%s'", acc.getName()));
                    arrow.setRotation(0);
                    animator.rotationBy(180);

                    // removing all child accounts from the view
                    int start = -1, count = 0;
                    try (LockHolder ignored = Data.accounts.lockForWriting()) {
                        for (int i = 0; i < Data.accounts.size(); i++) {
                            if (acc.isParentOf(Data.accounts.get(i))) {
//                                debug("accounts", String.format("Found a child '%s' at position %d",
//                                        Data.accounts.get(i).getName(), i));
                                if (start == -1) {
                                    start = i;
                                }
                                count++;
                            }
                            else {
                                if (start != -1) {
//                                    debug("accounts",
//                                            String.format("Found a non-child '%s' at position %d",
//                                                    Data.accounts.get(i).getName(), i));
                                    break;
                                }
                            }
                        }

                        if (start != -1) {
                            for (int j = 0; j < count; j++) {
//                                debug("accounts", String.format("Removing item %d: %s", start + j,
//                                        Data.accounts.get(start).getName()));
                                Data.accounts.removeQuietly(start);
                            }

                            mAccountSummaryFragment.modelAdapter
                                    .notifyItemRangeRemoved(start, count);
                        }
                    }
                }
                else {
                    debug("accounts", String.format("Expanding account '%s'", acc.getName()));
                    arrow.setRotation(180);
                    animator.rotationBy(-180);
                    List<LedgerAccount> children =
                            Data.profile.get().loadVisibleChildAccountsOf(acc);
                    try (LockHolder ignored = Data.accounts.lockForWriting()) {
                        int parentPos = Data.accounts.indexOf(acc);
                        if (parentPos != -1) {
                            // may have disappeared in a concurrent refresh operation
                            Data.accounts.addAllQuietly(parentPos + 1, children);
                            mAccountSummaryFragment.modelAdapter
                                    .notifyItemRangeInserted(parentPos + 1, children.size());
                        }
                    }
                }
                break;
            case R.id.account_row_acc_amounts:
                if (acc.getAmountCount() > AccountSummaryAdapter.AMOUNT_LIMIT) {
                    acc.toggleAmountsExpanded();
                    DbOpQueue
                            .add("update accounts set amounts_expanded=? where name=? and profile=?",
                                    new Object[]{acc.amountsExpanded(), acc.getName(),
                                                 Data.profile.get().getUuid()
                                    });
                    Data.accounts.triggerItemChangedNotification(acc);
                }
                break;
        }
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @NotNull
        @Override
        public Fragment getItem(int position) {
            debug("main", String.format(Locale.ENGLISH, "Switching to fragment %d", position));
            switch (position) {
                case 0:
//                    debug("flow", "Creating account summary fragment");
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
