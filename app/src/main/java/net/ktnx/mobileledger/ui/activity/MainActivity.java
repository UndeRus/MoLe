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

package net.ktnx.mobileledger.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
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
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.async.RefreshDescriptionsTask;
import net.ktnx.mobileledger.async.RetrieveTransactionsTask;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
import net.ktnx.mobileledger.ui.account_summary.AccountSummaryFragment;
import net.ktnx.mobileledger.ui.profiles.ProfileDetailFragment;
import net.ktnx.mobileledger.ui.profiles.ProfilesRecyclerViewAdapter;
import net.ktnx.mobileledger.ui.transaction_list.TransactionListFragment;
import net.ktnx.mobileledger.ui.transaction_list.TransactionListViewModel;
import net.ktnx.mobileledger.utils.Colors;
import net.ktnx.mobileledger.utils.GetOptCallback;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.MLDB;

import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static net.ktnx.mobileledger.utils.Logger.debug;

/*
 * TODO: reports
 *  */

public class MainActivity extends ProfileThemedActivity {
    public static final String STATE_CURRENT_PAGE = "current_page";
    public static final String BUNDLE_SAVED_STATE = "bundle_savedState";
    public static final String STATE_ACC_FILTER = "account_filter";
    private static final String PREF_THEME_ID = "themeId";
    DrawerLayout drawer;
    private View profileListHeadMore, profileListHeadCancel, profileListHeadAddProfile;
    private View bTransactionListCancelDownload;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private FloatingActionButton fab;
    private ProfilesRecyclerViewAdapter mProfileListAdapter;
    private int mCurrentPage;
    private boolean mBackMeansToAccountList = false;
    private Toolbar mToolbar;
    private DrawerLayout.SimpleDrawerListener drawerListener;
    private ActionBarDrawerToggle barDrawerToggle;
    private ViewPager.SimpleOnPageChangeListener pageChangeListener;
    private MobileLedgerProfile profile;
    @Override
    protected void onStart() {
        super.onStart();

        Logger.debug("MainActivity", "onStart()");

        mViewPager.setCurrentItem(mCurrentPage, false);
    }
    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_CURRENT_PAGE, mViewPager.getCurrentItem());
        if (Data.accountFilter.getValue() != null)
            outState.putString(STATE_ACC_FILTER, Data.accountFilter.getValue());
    }
    @Override
    protected void onDestroy() {
        mSectionsPagerAdapter = null;
        RecyclerView root = findViewById(R.id.nav_profile_list);
        if (root != null)
            root.setAdapter(null);
        if (drawer != null)
            drawer.removeDrawerListener(drawerListener);
        drawerListener = null;
        if (drawer != null)
            drawer.removeDrawerListener(barDrawerToggle);
        barDrawerToggle = null;
        if (mViewPager != null)
            mViewPager.removeOnPageChangeListener(pageChangeListener);
        pageChangeListener = null;
        super.onDestroy();
    }
    @Override
    protected void setupProfileColors() {
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        int profileColor = prefs.getInt(PREF_THEME_ID, -2);
        if (profileColor == -2)
            profileColor = Data.retrieveCurrentThemeIdFromDb();
        Colors.setupTheme(this, profileColor);
        Colors.profileThemeId = profileColor;
        storeThemeIdInPrefs(profileColor);
    }
    @Override
    protected void onResume() {
        super.onResume();
        fabShouldShow();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        debug("MainActivity", "onCreate()/entry");
        super.onCreate(savedInstanceState);
        debug("MainActivity", "onCreate()/after super");
        setContentView(R.layout.activity_main);

        fab = findViewById(R.id.btn_add_transaction);
        profileListHeadMore = findViewById(R.id.nav_profiles_start_edit);
        profileListHeadCancel = findViewById(R.id.nav_profiles_cancel_edit);
        LinearLayout profileListHeadMoreAndCancel =
                findViewById(R.id.nav_profile_list_head_buttons);
        profileListHeadAddProfile = findViewById(R.id.nav_new_profile_button);
        drawer = findViewById(R.id.drawer_layout);
        bTransactionListCancelDownload = findViewById(R.id.transaction_list_cancel_download);
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager = findViewById(R.id.root_frame);

        Bundle extra = getIntent().getBundleExtra(BUNDLE_SAVED_STATE);
        if (extra != null && savedInstanceState == null)
            savedInstanceState = extra;


        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        Data.profile.observe(this, this::onProfileChanged);

        Data.profiles.observe(this, this::onProfileListChanged);

        if (barDrawerToggle == null) {
            barDrawerToggle = new ActionBarDrawerToggle(this, drawer, mToolbar,
                    R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawer.addDrawerListener(barDrawerToggle);
        }
        barDrawerToggle.syncState();

        try {
            PackageInfo pi = getApplicationContext().getPackageManager()
                                                    .getPackageInfo(getPackageName(), 0);
            ((TextView) findViewById(R.id.nav_upper).findViewById(
                    R.id.drawer_version_text)).setText(pi.versionName);
            ((TextView) findViewById(R.id.no_profiles_layout).findViewById(
                    R.id.drawer_version_text)).setText(pi.versionName);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        markDrawerItemCurrent(R.id.nav_account_summary);

        mViewPager.setAdapter(mSectionsPagerAdapter);

        if (pageChangeListener == null) {
            pageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
                @Override
                public void onPageSelected(int position) {
                    mCurrentPage = position;
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
            Data.accountFilter.setValue(savedInstanceState.getString(STATE_ACC_FILTER, null));
        }

        Data.lastUpdateDate.observe(this, this::updateLastUpdateDisplay);

        findViewById(R.id.btn_no_profiles_add).setOnClickListener(
                v -> startEditProfileActivity(null));

        findViewById(R.id.btn_add_transaction).setOnClickListener(this::fabNewTransactionClicked);

        findViewById(R.id.nav_new_profile_button).setOnClickListener(
                v -> startEditProfileActivity(null));

        RecyclerView root = findViewById(R.id.nav_profile_list);
        if (root == null)
            throw new RuntimeException("Can't get hold on the transaction value view");

        if (mProfileListAdapter == null)
            mProfileListAdapter = new ProfilesRecyclerViewAdapter();
        root.setAdapter(mProfileListAdapter);

        mProfileListAdapter.editingProfiles.observe(this, newValue -> {
            if (newValue) {
                profileListHeadMore.setVisibility(View.GONE);
                profileListHeadCancel.setVisibility(View.VISIBLE);
                profileListHeadAddProfile.setVisibility(View.VISIBLE);
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    profileListHeadMore.startAnimation(
                            AnimationUtils.loadAnimation(MainActivity.this, R.anim.fade_out));
                    profileListHeadCancel.startAnimation(
                            AnimationUtils.loadAnimation(MainActivity.this, R.anim.fade_in));
                    profileListHeadAddProfile.startAnimation(
                            AnimationUtils.loadAnimation(MainActivity.this, R.anim.fade_in));
                }
            }
            else {
                profileListHeadCancel.setVisibility(View.GONE);
                profileListHeadMore.setVisibility(View.VISIBLE);
                profileListHeadAddProfile.setVisibility(View.GONE);
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    profileListHeadCancel.startAnimation(
                            AnimationUtils.loadAnimation(MainActivity.this, R.anim.fade_out));
                    profileListHeadMore.startAnimation(
                            AnimationUtils.loadAnimation(MainActivity.this, R.anim.fade_in));
                    profileListHeadAddProfile.startAnimation(
                            AnimationUtils.loadAnimation(MainActivity.this, R.anim.fade_out));
                }
            }

            mProfileListAdapter.notifyDataSetChanged();
        });

        LinearLayoutManager llm = new LinearLayoutManager(this);

        llm.setOrientation(RecyclerView.VERTICAL);
        root.setLayoutManager(llm);

        profileListHeadMore.setOnClickListener((v) -> mProfileListAdapter.flipEditingProfiles());
        profileListHeadCancel.setOnClickListener((v) -> mProfileListAdapter.flipEditingProfiles());
        profileListHeadMoreAndCancel.setOnClickListener(
                (v) -> mProfileListAdapter.flipEditingProfiles());
        if (drawerListener == null) {
            drawerListener = new DrawerLayout.SimpleDrawerListener() {
                @Override
                public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
                    if (slideOffset > 0.2)
                        fabHide();
                }
                @Override
                public void onDrawerClosed(View drawerView) {
                    super.onDrawerClosed(drawerView);
                    mProfileListAdapter.setAnimationsEnabled(false);
                    mProfileListAdapter.editingProfiles.setValue(false);
                    Data.drawerOpen.setValue(false);
                    fabShouldShow();
                }
                @Override
                public void onDrawerOpened(View drawerView) {
                    super.onDrawerOpened(drawerView);
                    mProfileListAdapter.setAnimationsEnabled(true);
                    Data.drawerOpen.setValue(true);
                    fabHide();
                }
            };
            drawer.addDrawerListener(drawerListener);
        }

        Data.drawerOpen.observe(this, open -> {
            if (open)
                drawer.open();
            else
                drawer.close();
        });
    }
    private void scheduleDataRetrievalIfStale(Date lastUpdate) {
        long now = new Date().getTime();
        if ((lastUpdate == null) || (now > (lastUpdate.getTime() + (24 * 3600 * 1000)))) {
            if (lastUpdate == null)
                debug("db::", "WEB data never fetched. scheduling a fetch");
            else
                debug("db", String.format(Locale.ENGLISH,
                        "WEB data last fetched at %1.3f and now is %1.3f. re-fetching",
                        lastUpdate.getTime() / 1000f, now / 1000f));

            Data.scheduleTransactionListRetrieval(this);
        }
    }
    private void createShortcuts(List<MobileLedgerProfile> list) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1)
            return;

        ShortcutManager sm = getSystemService(ShortcutManager.class);
        List<ShortcutInfo> shortcuts = new ArrayList<>();
        int i = 0;
        for (MobileLedgerProfile p : list) {
            if (shortcuts.size() >= sm.getMaxShortcutCountPerActivity())
                break;

            if (!p.isPostingPermitted())
                continue;

            final ShortcutInfo.Builder builder =
                    new ShortcutInfo.Builder(this, "new_transaction_" + p.getUuid());
            ShortcutInfo si = builder.setShortLabel(p.getName())
                                     .setIcon(Icon.createWithResource(this,
                                             R.drawable.thick_plus_icon))
                                     .setIntent(new Intent(Intent.ACTION_VIEW, null, this,
                                             NewTransactionActivity.class).putExtra("profile_uuid",
                                             p.getUuid()))
                                     .setRank(i)
                                     .build();
            shortcuts.add(si);
            i++;
        }
        sm.setDynamicShortcuts(shortcuts);
    }
    private void onProfileListChanged(List<MobileLedgerProfile> newList) {
        if ((newList == null) || newList.isEmpty()) {
            findViewById(R.id.no_profiles_layout).setVisibility(View.VISIBLE);
            findViewById(R.id.main_app_layout).setVisibility(View.GONE);
            return;
        }

        findViewById(R.id.main_app_layout).setVisibility(View.VISIBLE);
        findViewById(R.id.no_profiles_layout).setVisibility(View.GONE);

        findViewById(R.id.nav_profile_list).setMinimumHeight(
                (int) (getResources().getDimension(R.dimen.thumb_row_height) * newList.size()));

        debug("profiles", "profile list changed");
        mProfileListAdapter.notifyDataSetChanged();

        createShortcuts(newList);
    }
    /**
     * called when the current profile has changed
     */
    private void onProfileChanged(MobileLedgerProfile profile) {
        if (this.profile == null) {
            if (profile == null)
                return;
        }
        else {
            if (this.profile.equals(profile))
                return;
        }

        boolean haveProfile = profile != null;

        if (haveProfile)
            setTitle(profile.getName());
        else
            setTitle(R.string.app_name);

        if (this.profile != null)
            this.profile.getDisplayedAccounts()
                        .removeObservers(this);

        this.profile = profile;

        int newProfileTheme = haveProfile ? profile.getThemeHue() : -1;
        if (newProfileTheme != Colors.profileThemeId) {
            debug("profiles",
                    String.format(Locale.ENGLISH, "profile theme %d → %d", Colors.profileThemeId,
                            newProfileTheme));
            Colors.profileThemeId = newProfileTheme;
            profileThemeChanged();
            // profileThemeChanged would restart the activity, so no need to reload the
            // data sets below
            return;
        }

        findViewById(R.id.no_profiles_layout).setVisibility(haveProfile ? View.GONE : View.VISIBLE);
        findViewById(R.id.pager_layout).setVisibility(haveProfile ? View.VISIBLE : View.VISIBLE);

        mProfileListAdapter.notifyDataSetChanged();

        Data.transactions.clear();
        debug("transactions", "requesting list reload");
        TransactionListViewModel.scheduleTransactionListReload();

        if (haveProfile) {
            profile.scheduleAccountListReload();

            if (profile.isPostingPermitted()) {
                mToolbar.setSubtitle(null);
                fab.show();
            }
            else {
                mToolbar.setSubtitle(R.string.profile_subtitle_read_only);
                fab.hide();
            }
        }
        else {
            mToolbar.setSubtitle(null);
            fab.hide();
        }

        updateLastUpdateTextFromDB();
    }
    private void updateLastUpdateDisplay(Date newValue) {
        LinearLayout l = findViewById(R.id.transactions_last_update_layout);
        TextView v = findViewById(R.id.transactions_last_update);
        if (newValue == null) {
            l.setVisibility(View.INVISIBLE);
            debug("main", "no last update date :(");
        }
        else {
            final String text = DateFormat.getDateTimeInstance()
                                          .format(newValue);
            v.setText(text);
            l.setVisibility(View.VISIBLE);
            debug("main", String.format("Date formatted: %s", text));
        }

        scheduleDataRetrievalIfStale(newValue);
    }
    private void profileThemeChanged() {
        storeThemeIdInPrefs(profile.getThemeHue());

        // un-hook all observed LiveData
        Data.profile.removeObservers(this);
        Data.profiles.removeObservers(this);
        Data.lastUpdateDate.removeObservers(this);

        recreate();
    }
    private void storeThemeIdInPrefs(int themeId) {
        // store the new theme id in the preferences
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor e = prefs.edit();
        e.putInt(PREF_THEME_ID, themeId);
        e.apply();
    }
    public void startEditProfileActivity(MobileLedgerProfile profile) {
        Intent intent = new Intent(this, ProfileDetailActivity.class);
        Bundle args = new Bundle();
        if (profile != null) {
            int index = Data.getProfileIndex(profile);
            if (index != -1)
                intent.putExtra(ProfileDetailFragment.ARG_ITEM_ID, index);
        }
        intent.putExtras(args);
        startActivity(intent, args);
    }
    private void setupProfile() {
        MLDB.getOption(MLDB.OPT_PROFILE_UUID, null, new GetOptCallback() {
            @Override
            protected void onResult(String profileUUID) {
                MobileLedgerProfile startupProfile;

                startupProfile = Data.getProfile(profileUUID);
                Data.setCurrentProfile(startupProfile);
            }
        });
    }
    public void fabNewTransactionClicked(View view) {
        Intent intent = new Intent(this, NewTransactionActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_up, R.anim.dummy);
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

        showTransactionsFragment(null);
    }
    public void showTransactionsFragment(String accName) {
        Data.accountFilter.setValue(accName);
        mViewPager.setCurrentItem(1, true);
    }
    public void showAccountTransactions(String accountName) {
        mBackMeansToAccountList = true;
        showTransactionsFragment(accountName);
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
                        getSupportFragmentManager().getBackStackEntryCount()));

                super.onBackPressed();
            }
        }
    }
    public void updateLastUpdateTextFromDB() {
        if (profile == null)
            return;

        long last_update = profile.getLongOption(MLDB.OPT_LAST_SCRAPE, 0L);

        debug("transactions", String.format(Locale.ENGLISH, "Last update = %d", last_update));
        if (last_update == 0) {
            Data.lastUpdateDate.postValue(null);
        }
        else {
            Data.lastUpdateDate.postValue(new Date(last_update));
        }
    }
    public void onStopTransactionRefreshClick(View view) {
        debug("interactive", "Cancelling transactions refresh");
        Data.stopTransactionsRetrieval();
        bTransactionListCancelDownload.setEnabled(false);
    }
    public void onRetrieveDone(String error) {
        Data.transactionRetrievalDone();
        findViewById(R.id.transaction_progress_layout).setVisibility(View.GONE);

        if (error == null) {
            updateLastUpdateTextFromDB();

            new RefreshDescriptionsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            TransactionListViewModel.scheduleTransactionListReload();
        }
        else
            Snackbar.make(mViewPager, error, Snackbar.LENGTH_LONG)
                    .show();
    }
    public void onRetrieveStart() {
        ProgressBar progressBar = findViewById(R.id.transaction_list_progress_bar);
        bTransactionListCancelDownload.setEnabled(true);
        ColorStateList csl = Colors.getColorStateList();
        progressBar.setIndeterminateTintList(csl);
        progressBar.setProgressTintList(csl);
        progressBar.setIndeterminate(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            progressBar.setProgress(0, false);
        else
            progressBar.setProgress(0);
        findViewById(R.id.transaction_progress_layout).setVisibility(View.VISIBLE);
    }
    public void onRetrieveProgress(RetrieveTransactionsTask.Progress progress) {
        ProgressBar progressBar = findViewById(R.id.transaction_list_progress_bar);
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
            else
                progressBar.setProgress(progress.getProgress());
            progressBar.setIndeterminate(false);
        }
    }
    public void fabShouldShow() {
        if ((profile != null) && profile.isPostingPermitted() && !drawer.isOpen())
            fab.show();
        else
            fabHide();
    }
    public void fabHide() {
        fab.hide();
    }

    public static class SectionsPagerAdapter extends FragmentPagerAdapter {

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NotNull
        @Override
        public Fragment getItem(int position) {
            debug("main", String.format(Locale.ENGLISH, "Switching to fragment %d", position));
            switch (position) {
                case 0:
//                    debug("flow", "Creating account summary fragment");
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
