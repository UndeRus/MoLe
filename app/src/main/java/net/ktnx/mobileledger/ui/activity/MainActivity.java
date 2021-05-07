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

package net.ktnx.mobileledger.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.snackbar.Snackbar;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.async.RetrieveTransactionsTask;
import net.ktnx.mobileledger.async.TransactionAccumulator;
import net.ktnx.mobileledger.databinding.ActivityMainBinding;
import net.ktnx.mobileledger.db.DB;
import net.ktnx.mobileledger.db.Option;
import net.ktnx.mobileledger.db.Profile;
import net.ktnx.mobileledger.db.TransactionWithAccounts;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.ui.FabManager;
import net.ktnx.mobileledger.ui.MainModel;
import net.ktnx.mobileledger.ui.account_summary.AccountSummaryFragment;
import net.ktnx.mobileledger.ui.new_transaction.NewTransactionActivity;
import net.ktnx.mobileledger.ui.profiles.ProfileDetailActivity;
import net.ktnx.mobileledger.ui.profiles.ProfilesRecyclerViewAdapter;
import net.ktnx.mobileledger.ui.templates.TemplatesActivity;
import net.ktnx.mobileledger.ui.transaction_list.TransactionListFragment;
import net.ktnx.mobileledger.utils.Colors;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.Misc;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/*
 * TODO: reports
 *  */

public class MainActivity extends ProfileThemedActivity implements FabManager.FabHandler {
    public static final String TAG = "main-act";
    public static final String STATE_CURRENT_PAGE = "current_page";
    public static final String BUNDLE_SAVED_STATE = "bundle_savedState";
    public static final String STATE_ACC_FILTER = "account_filter";
    private static final boolean FAB_HIDDEN = false;
    private static final boolean FAB_SHOWN = true;
    private ConverterThread converterThread = null;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ProfilesRecyclerViewAdapter mProfileListAdapter;
    private int mCurrentPage;
    private boolean mBackMeansToAccountList = false;
    private DrawerLayout.SimpleDrawerListener drawerListener;
    private ActionBarDrawerToggle barDrawerToggle;
    private ViewPager2.OnPageChangeCallback pageChangeCallback;
    private Profile profile;
    private MainModel mainModel;
    private ActivityMainBinding b;
    private int fabVerticalOffset;
    private FabManager fabManager;
    @Override
    protected void onStart() {
        super.onStart();

        Logger.debug(TAG, "onStart()");

        b.mainPager.setCurrentItem(mCurrentPage, false);
    }
    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_CURRENT_PAGE, b.mainPager.getCurrentItem());
        if (mainModel.getAccountFilter()
                     .getValue() != null)
            outState.putString(STATE_ACC_FILTER, mainModel.getAccountFilter()
                                                          .getValue());
    }
    @Override
    protected void onDestroy() {
        mSectionsPagerAdapter = null;
        b.navProfileList.setAdapter(null);
        b.drawerLayout.removeDrawerListener(drawerListener);
        drawerListener = null;
        b.drawerLayout.removeDrawerListener(barDrawerToggle);
        barDrawerToggle = null;
        b.mainPager.unregisterOnPageChangeCallback(pageChangeCallback);
        pageChangeCallback = null;
        super.onDestroy();
    }
    @Override
    protected void onResume() {
        super.onResume();
        fabShouldShow();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logger.debug(TAG, "onCreate()/entry");
        super.onCreate(savedInstanceState);
        Logger.debug(TAG, "onCreate()/after super");
        b = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        mainModel = new ViewModelProvider(this).get(MainModel.class);

        mSectionsPagerAdapter = new SectionsPagerAdapter(this);

        Bundle extra = getIntent().getBundleExtra(BUNDLE_SAVED_STATE);
        if (extra != null && savedInstanceState == null)
            savedInstanceState = extra;


        setSupportActionBar(b.toolbar);

        Data.observeProfile(this, this::onProfileChanged);

        Data.profiles.observe(this, this::onProfileListChanged);

        Data.backgroundTaskProgress.observe(this, this::onRetrieveProgress);
        Data.backgroundTasksRunning.observe(this, this::onRetrieveRunningChanged);

        if (barDrawerToggle == null) {
            barDrawerToggle = new ActionBarDrawerToggle(this, b.drawerLayout, b.toolbar,
                    R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            b.drawerLayout.addDrawerListener(barDrawerToggle);
        }
        barDrawerToggle.syncState();

        try {
            PackageInfo pi = getApplicationContext().getPackageManager()
                                                    .getPackageInfo(getPackageName(), 0);
            ((TextView) b.navUpper.findViewById(R.id.drawer_version_text)).setText(pi.versionName);
            ((TextView) b.noProfilesLayout.findViewById(R.id.drawer_version_text)).setText(
                    pi.versionName);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        markDrawerItemCurrent(R.id.nav_account_summary);

        b.mainPager.setAdapter(mSectionsPagerAdapter);
        b.mainPager.setOffscreenPageLimit(1);

        if (pageChangeCallback == null) {
            pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
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
                            Log.e(TAG, String.format("Unexpected page index %d", position));
                    }

                    super.onPageSelected(position);
                }
            };
            b.mainPager.registerOnPageChangeCallback(pageChangeCallback);
        }

        mCurrentPage = 0;
        if (savedInstanceState != null) {
            int currentPage = savedInstanceState.getInt(STATE_CURRENT_PAGE, -1);
            if (currentPage != -1) {
                mCurrentPage = currentPage;
            }
            mainModel.getAccountFilter()
                     .setValue(savedInstanceState.getString(STATE_ACC_FILTER, null));
        }

        b.btnNoProfilesAdd.setOnClickListener(v -> ProfileDetailActivity.start(this, null));

        b.btnAddTransaction.setOnClickListener(this::fabNewTransactionClicked);

        b.navNewProfileButton.setOnClickListener(v -> ProfileDetailActivity.start(this, null));

        b.transactionListCancelDownload.setOnClickListener(this::onStopTransactionRefreshClick);

        if (mProfileListAdapter == null)
            mProfileListAdapter = new ProfilesRecyclerViewAdapter();
        b.navProfileList.setAdapter(mProfileListAdapter);

        mProfileListAdapter.editingProfiles.observe(this, newValue -> {
            if (newValue) {
                b.navProfilesStartEdit.setVisibility(View.GONE);
                b.navProfilesCancelEdit.setVisibility(View.VISIBLE);
                b.navNewProfileButton.setVisibility(View.VISIBLE);
                if (b.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    b.navProfilesStartEdit.startAnimation(
                            AnimationUtils.loadAnimation(MainActivity.this, R.anim.fade_out));
                    b.navProfilesCancelEdit.startAnimation(
                            AnimationUtils.loadAnimation(MainActivity.this, R.anim.fade_in));
                    b.navNewProfileButton.startAnimation(
                            AnimationUtils.loadAnimation(MainActivity.this, R.anim.fade_in));
                }
            }
            else {
                b.navProfilesCancelEdit.setVisibility(View.GONE);
                b.navProfilesStartEdit.setVisibility(View.VISIBLE);
                b.navNewProfileButton.setVisibility(View.GONE);
                if (b.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    b.navProfilesCancelEdit.startAnimation(
                            AnimationUtils.loadAnimation(MainActivity.this, R.anim.fade_out));
                    b.navProfilesStartEdit.startAnimation(
                            AnimationUtils.loadAnimation(MainActivity.this, R.anim.fade_in));
                    b.navNewProfileButton.startAnimation(
                            AnimationUtils.loadAnimation(MainActivity.this, R.anim.fade_out));
                }
            }

            mProfileListAdapter.notifyDataSetChanged();
        });

        fabManager = new FabManager(b.btnAddTransaction);

        LinearLayoutManager llm = new LinearLayoutManager(this);

        llm.setOrientation(RecyclerView.VERTICAL);
        b.navProfileList.setLayoutManager(llm);

        b.navProfilesStartEdit.setOnClickListener((v) -> mProfileListAdapter.flipEditingProfiles());
        b.navProfilesCancelEdit.setOnClickListener(
                (v) -> mProfileListAdapter.flipEditingProfiles());
        b.navProfileListHeadButtons.setOnClickListener(
                (v) -> mProfileListAdapter.flipEditingProfiles());
        if (drawerListener == null) {
            drawerListener = new DrawerLayout.SimpleDrawerListener() {
                @Override
                public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
                    if (slideOffset > 0.2)
                        fabManager.hideFab();
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
                    fabManager.hideFab();
                }
            };
            b.drawerLayout.addDrawerListener(drawerListener);
        }

        Data.drawerOpen.observe(this, open -> {
            if (open)
                b.drawerLayout.open();
            else
                b.drawerLayout.close();
        });

        mainModel.getUpdateError()
                 .observe(this, (error) -> {
                     if (error == null)
                         return;

                     Snackbar.make(b.mainPager, error, Snackbar.LENGTH_INDEFINITE)
                             .show();
                     mainModel.clearUpdateError();
                 });
        Data.locale.observe(this, l -> refreshLastUpdateInfo());
        Data.lastUpdateDate.observe(this, date -> refreshLastUpdateInfo());
        Data.lastUpdateTransactionCount.observe(this, date -> refreshLastUpdateInfo());
        Data.lastUpdateAccountCount.observe(this, date -> refreshLastUpdateInfo());
        b.navAccountSummary.setOnClickListener(this::onAccountSummaryClicked);
        b.navLatestTransactions.setOnClickListener(this::onLatestTransactionsClicked);
        b.navPatterns.setOnClickListener(this::onPatternsClick);
    }
    private void onPatternsClick(View view) {
        Intent intent = new Intent(this, TemplatesActivity.class);
        startActivity(intent);
    }
    private void scheduleDataRetrievalIfStale(long lastUpdate) {
        long now = new Date().getTime();
        if ((lastUpdate == 0) || (now > (lastUpdate + (24 * 3600 * 1000)))) {
            if (lastUpdate == 0)
                Logger.debug("db::", "WEB data never fetched. scheduling a fetch");
            else
                Logger.debug("db", String.format(Locale.ENGLISH,
                        "WEB data last fetched at %1.3f and now is %1.3f. re-fetching",
                        lastUpdate / 1000f, now / 1000f));

            mainModel.scheduleTransactionListRetrieval();
        }
    }
    private void createShortcuts(List<Profile> list) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1)
            return;

        ShortcutManager sm = getSystemService(ShortcutManager.class);
        List<ShortcutInfo> shortcuts = new ArrayList<>();
        int i = 0;
        for (Profile p : list) {
            if (shortcuts.size() >= sm.getMaxShortcutCountPerActivity())
                break;

            if (!p.permitPosting())
                continue;

            final ShortcutInfo.Builder builder =
                    new ShortcutInfo.Builder(this, "new_transaction_" + p.getId());
            ShortcutInfo si = builder.setShortLabel(p.getName())
                                     .setIcon(Icon.createWithResource(this,
                                             R.drawable.thick_plus_icon))
                                     .setIntent(new Intent(Intent.ACTION_VIEW, null, this,
                                             NewTransactionActivity.class).putExtra(
                                             ProfileThemedActivity.PARAM_PROFILE_ID, p.getId())
                                                                          .putExtra(
                                                                                  ProfileThemedActivity.PARAM_THEME,
                                                                                  p.getTheme()))
                                     .setRank(i)
                                     .build();
            shortcuts.add(si);
            i++;
        }
        sm.setDynamicShortcuts(shortcuts);
    }
    private void onProfileListChanged(List<Profile> newList) {
        if ((newList == null) || newList.isEmpty()) {
            b.noProfilesLayout.setVisibility(View.VISIBLE);
            b.mainAppLayout.setVisibility(View.GONE);
            return;
        }

        b.mainAppLayout.setVisibility(View.VISIBLE);
        b.noProfilesLayout.setVisibility(View.GONE);

        b.navProfileList.setMinimumHeight(
                (int) (getResources().getDimension(R.dimen.thumb_row_height) * newList.size()));

        Logger.debug("profiles", "profile list changed");
        mProfileListAdapter.setProfileList(newList);

        createShortcuts(newList);

        final Profile currentProfile = Data.getProfile();
        Profile replacementProfile = null;
        if (currentProfile != null) {
            for (Profile p : newList) {
                if (p.getId() == currentProfile.getId()) {
                    replacementProfile = p;
                    break;
                }
            }
        }

        if (replacementProfile == null) {
            Logger.debug(TAG, "Switching profile because the current is no longer available");
            Data.setCurrentProfile(newList.get(0));
        }
        else {
            Data.setCurrentProfile(replacementProfile);
        }
    }
    /**
     * called when the current profile has changed
     */
    private void onProfileChanged(@Nullable Profile newProfile) {
        if (this.profile != null) {
            if (this.profile.equals(newProfile))
                return;
        }

        boolean haveProfile = newProfile != null;

        if (haveProfile)
            setTitle(newProfile.getName());
        else
            setTitle(R.string.app_name);

        int newProfileTheme = haveProfile ? newProfile.getTheme() : Colors.DEFAULT_HUE_DEG;
        if (newProfileTheme != Colors.profileThemeId) {
            Logger.debug("profiles",
                    String.format(Locale.ENGLISH, "profile theme %d → %d", Colors.profileThemeId,
                            newProfileTheme));
            Colors.profileThemeId = newProfileTheme;
            profileThemeChanged();
            // profileThemeChanged would restart the activity, so no need to reload the
            // data sets below
            return;
        }

        final boolean sameProfileId = (newProfile != null) && (this.profile != null) &&
                                      this.profile.getId() == newProfile.getId();

        this.profile = newProfile;

        b.noProfilesLayout.setVisibility(haveProfile ? View.GONE : View.VISIBLE);
        b.pagerLayout.setVisibility(haveProfile ? View.VISIBLE : View.VISIBLE);

        mProfileListAdapter.notifyDataSetChanged();

        if (haveProfile) {
            if (newProfile.permitPosting()) {
                b.toolbar.setSubtitle(null);
                b.btnAddTransaction.show();
            }
            else {
                b.toolbar.setSubtitle(R.string.profile_subtitle_read_only);
                b.btnAddTransaction.hide();
            }
        }
        else {
            b.toolbar.setSubtitle(null);
            b.btnAddTransaction.hide();
        }

        updateLastUpdateTextFromDB();

        if (sameProfileId) {
            Logger.debug(TAG, String.format(Locale.ROOT, "Short-cut profile 'changed' to %d",
                    newProfile.getId()));
            return;
        }

        mainModel.getAccountFilter()
                 .observe(this, this::onAccountFilterChanged);

        mainModel.stopTransactionsRetrieval();
        mainModel.clearTransactions();
    }
    private void onAccountFilterChanged(String accFilter) {
        Logger.debug(TAG, "account filter changed, reloading transactions");
//                     mainModel.scheduleTransactionListReload();
        LiveData<List<TransactionWithAccounts>> transactions =
                new MutableLiveData<>(new ArrayList<>());
        if (profile != null) {
            if (accFilter == null || accFilter.isEmpty()) {
                transactions = DB.get()
                                 .getTransactionDAO()
                                 .getAllWithAccounts(profile.getId());
            }
            else {
                transactions = DB.get()
                                 .getTransactionDAO()
                                 .getAllWithAccountsFiltered(profile.getId(), accFilter);
            }
        }

        transactions.observe(this, list -> {
            Logger.debug(TAG,
                    String.format(Locale.ROOT, "got transaction list from DB (%d transactions)",
                            list.size()));

            if (converterThread != null)
                converterThread.interrupt();
            converterThread = new ConverterThread(mainModel, list, accFilter);
            converterThread.start();
        });
    }
    private void profileThemeChanged() {
        // un-hook all observed LiveData
        Data.removeProfileObservers(this);
        Data.profiles.removeObservers(this);
        Data.lastUpdateTransactionCount.removeObservers(this);
        Data.lastUpdateAccountCount.removeObservers(this);
        Data.lastUpdateDate.removeObservers(this);

        Logger.debug(TAG, "profileThemeChanged(): recreating activity");
        recreate();
    }
    public void fabNewTransactionClicked(View view) {
        Intent intent = new Intent(this, NewTransactionActivity.class);
        intent.putExtra(ProfileThemedActivity.PARAM_PROFILE_ID, profile.getId());
        intent.putExtra(ProfileThemedActivity.PARAM_THEME, profile.getTheme());
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_up, R.anim.dummy);
    }
    public void markDrawerItemCurrent(int id) {
        TextView item = b.drawerLayout.findViewById(id);
        item.setBackgroundColor(Colors.tableRowDarkBG);

        for (int i = 0; i < b.navActions.getChildCount(); i++) {
            View view = b.navActions.getChildAt(i);
            if (view.getId() != id) {
                view.setBackgroundColor(Color.TRANSPARENT);
            }
        }
    }
    public void onAccountSummaryClicked(View view) {
        b.drawerLayout.closeDrawers();

        showAccountSummaryFragment();
    }
    private void showAccountSummaryFragment() {
        b.mainPager.setCurrentItem(0, true);
        mainModel.getAccountFilter()
                 .setValue(null);
    }
    public void onLatestTransactionsClicked(View view) {
        b.drawerLayout.closeDrawers();

        showTransactionsFragment(null);
    }
    public void showTransactionsFragment(String accName) {
        mainModel.getAccountFilter()
                 .setValue(accName);
        b.mainPager.setCurrentItem(1, true);
    }
    public void showAccountTransactions(String accountName) {
        mBackMeansToAccountList = true;
        showTransactionsFragment(accountName);
    }
    @Override
    public void onBackPressed() {
        if (b.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            b.drawerLayout.closeDrawer(GravityCompat.START);
        }
        else {
            if (mBackMeansToAccountList && (b.mainPager.getCurrentItem() == 1)) {
                mainModel.getAccountFilter()
                         .setValue(null);
                showAccountSummaryFragment();
                mBackMeansToAccountList = false;
            }
            else {
                Logger.debug(TAG, String.format(Locale.ENGLISH, "manager stack: %d",
                        getSupportFragmentManager().getBackStackEntryCount()));

                super.onBackPressed();
            }
        }
    }
    public void updateLastUpdateTextFromDB() {
        if (profile == null)
            return;

        DB.get()
          .getOptionDAO()
          .load(profile.getId(), Option.OPT_LAST_SCRAPE)
          .observe(this, opt -> {
              long lastUpdate = 0;
              if (opt != null) {
                  try {
                      lastUpdate = Long.parseLong(opt.getValue());
                  }
                  catch (NumberFormatException ex) {
                      Logger.debug(TAG, String.format("Error parsing '%s' as long", opt.getValue()),
                              ex);
                  }
              }

              if (lastUpdate == 0) {
                  Data.lastUpdateDate.postValue(null);
              }
              else {
                  Data.lastUpdateDate.postValue(new Date(lastUpdate));
              }

              scheduleDataRetrievalIfStale(lastUpdate);
          });
    }
    private void refreshLastUpdateInfo() {
        final int formatFlags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR |
                                DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_NUMERIC_DATE;
        String templateForTransactions =
                getResources().getString(R.string.transaction_count_summary);
        String templateForAccounts = getResources().getString(R.string.account_count_summary);
        Integer accountCount = Data.lastUpdateAccountCount.getValue();
        Integer transactionCount = Data.lastUpdateTransactionCount.getValue();
        Date lastUpdate = Data.lastUpdateDate.getValue();
        if (lastUpdate == null) {
            Data.lastTransactionsUpdateText.setValue("----");
            Data.lastAccountsUpdateText.setValue("----");
        }
        else {
            Data.lastTransactionsUpdateText.setValue(
                    String.format(Objects.requireNonNull(Data.locale.getValue()),
                            templateForTransactions,
                            transactionCount == null ? 0 : transactionCount,
                            DateUtils.formatDateTime(this, lastUpdate.getTime(), formatFlags)));
            Data.lastAccountsUpdateText.setValue(
                    String.format(Objects.requireNonNull(Data.locale.getValue()),
                            templateForAccounts, accountCount == null ? 0 : accountCount,
                            DateUtils.formatDateTime(this, lastUpdate.getTime(), formatFlags)));
        }
    }
    public void onStopTransactionRefreshClick(View view) {
        Logger.debug(TAG, "Cancelling transactions refresh");
        mainModel.stopTransactionsRetrieval();
        b.transactionListCancelDownload.setEnabled(false);
    }
    public void onRetrieveRunningChanged(Boolean running) {
        if (running) {
            b.transactionListCancelDownload.setEnabled(true);
            ColorStateList csl = Colors.getColorStateList();
            b.transactionListProgressBar.setIndeterminateTintList(csl);
            b.transactionListProgressBar.setProgressTintList(csl);
            b.transactionListProgressBar.setIndeterminate(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                b.transactionListProgressBar.setProgress(0, false);
            }
            else {
                b.transactionListProgressBar.setProgress(0);
            }
            b.transactionProgressLayout.setVisibility(View.VISIBLE);
        }
        else {
            b.transactionProgressLayout.setVisibility(View.GONE);
        }
    }
    public void onRetrieveProgress(@Nullable RetrieveTransactionsTask.Progress progress) {
        if (progress == null ||
            progress.getState() == RetrieveTransactionsTask.ProgressState.FINISHED)
        {
            Logger.debug(TAG, "progress: Done");
            b.transactionProgressLayout.setVisibility(View.GONE);

            mainModel.transactionRetrievalDone();

            String error = (progress == null) ? null : progress.getError();
            if (error != null) {
                if (error.equals(RetrieveTransactionsTask.Result.ERR_JSON_PARSER_ERROR))
                    error = getResources().getString(R.string.err_json_parser_error);

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(error);
                builder.setPositiveButton(R.string.btn_profile_options, (dialog, which) -> {
                    Logger.debug(TAG, "will start profile editor");
                    ProfileDetailActivity.start(this, profile);
                });
                builder.create()
                       .show();
                return;
            }

            return;
        }


        b.transactionListCancelDownload.setEnabled(true);
//        ColorStateList csl = Colors.getColorStateList();
//        progressBar.setIndeterminateTintList(csl);
//        progressBar.setProgressTintList(csl);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
//            progressBar.setProgress(0, false);
//        else
//            progressBar.setProgress(0);
        b.transactionProgressLayout.setVisibility(View.VISIBLE);

        if (progress.isIndeterminate() || (progress.getTotal() <= 0)) {
            b.transactionListProgressBar.setIndeterminate(true);
            Logger.debug(TAG, "progress: indeterminate");
        }
        else {
            if (b.transactionListProgressBar.isIndeterminate()) {
                b.transactionListProgressBar.setIndeterminate(false);
            }
//            Logger.debug(TAG,
//                    String.format(Locale.US, "progress: %d/%d", progress.getProgress(),
//                    progress.getTotal
//                    ()));
            b.transactionListProgressBar.setMax(progress.getTotal());
            // for some reason animation doesn't work - no progress is shown (stick at 0)
            // on lineageOS 14.1 (Nougat, 7.1.2)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                b.transactionListProgressBar.setProgress(progress.getProgress(), false);
            else
                b.transactionListProgressBar.setProgress(progress.getProgress());
        }
    }
    public void fabShouldShow() {
        if ((profile != null) && profile.permitPosting() && !b.drawerLayout.isOpen())
            fabManager.showFab();
    }
    @Override
    public Context getContext() {
        return this;
    }
    @Override
    public void showManagedFab() {
        fabShouldShow();
    }
    @Override
    public void hideManagedFab() {
        fabManager.hideFab();
    }
    public static class SectionsPagerAdapter extends FragmentStateAdapter {

        public SectionsPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }
        @NotNull
        @Override
        public Fragment createFragment(int position) {
            Logger.debug(TAG, String.format(Locale.ENGLISH, "Switching to fragment %d", position));
            switch (position) {
                case 0:
//                    debug(TAG, "Creating account summary fragment");
                    return new AccountSummaryFragment();
                case 1:
                    return new TransactionListFragment();
                default:
                    throw new IllegalStateException(
                            String.format("Unexpected fragment index: " + "%d", position));
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }

    static private class ConverterThread extends Thread {
        private final List<TransactionWithAccounts> list;
        private final MainModel model;
        private final String accFilter;
        public ConverterThread(@NonNull MainModel model,
                               @NonNull List<TransactionWithAccounts> list, String accFilter) {
            this.model = model;
            this.list = list;
            this.accFilter = accFilter;
        }
        @Override
        public void run() {
            TransactionAccumulator accumulator = new TransactionAccumulator(accFilter, accFilter);

            for (TransactionWithAccounts tr : list) {
                if (isInterrupted()) {
                    Logger.debug(TAG, "ConverterThread bailing out on interrupt");
                    return;
                }
                accumulator.put(new LedgerTransaction(tr));
            }

            if (isInterrupted()) {
                Logger.debug(TAG, "ConverterThread bailing out on interrupt");
                return;
            }

            Logger.debug(TAG, "ConverterThread publishing results");

            Misc.onMainThread(() -> accumulator.publishResults(model));
        }
    }
}
