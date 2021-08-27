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

package net.ktnx.mobileledger.ui.new_transaction;

import android.content.Context;
import android.content.Intent;
import android.database.AbstractCursor;
import android.os.Bundle;
import android.os.ParcelFormatException;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.view.MenuCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import net.ktnx.mobileledger.BuildConfig;
import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.async.DescriptionSelectedCallback;
import net.ktnx.mobileledger.async.GeneralBackgroundTasks;
import net.ktnx.mobileledger.async.SendTransactionTask;
import net.ktnx.mobileledger.async.TaskCallback;
import net.ktnx.mobileledger.dao.BaseDAO;
import net.ktnx.mobileledger.dao.TransactionDAO;
import net.ktnx.mobileledger.databinding.ActivityNewTransactionBinding;
import net.ktnx.mobileledger.db.DB;
import net.ktnx.mobileledger.db.TemplateHeader;
import net.ktnx.mobileledger.db.TransactionWithAccounts;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.model.MatchedTemplate;
import net.ktnx.mobileledger.ui.FabManager;
import net.ktnx.mobileledger.ui.QR;
import net.ktnx.mobileledger.ui.activity.ProfileThemedActivity;
import net.ktnx.mobileledger.ui.activity.SplashActivity;
import net.ktnx.mobileledger.ui.templates.TemplatesActivity;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.Misc;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.ktnx.mobileledger.utils.Logger.debug;

public class NewTransactionActivity extends ProfileThemedActivity
        implements TaskCallback, NewTransactionFragment.OnNewTransactionFragmentInteractionListener,
        QR.QRScanTrigger, QR.QRScanResultReceiver, DescriptionSelectedCallback,
        FabManager.FabHandler {
    final String TAG = "new-t-a";
    private NavController navController;
    private NewTransactionModel model;
    private ActivityResultLauncher<Void> qrScanLauncher;
    private ActivityNewTransactionBinding b;
    private FabManager fabManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        b = ActivityNewTransactionBinding.inflate(getLayoutInflater(), null, false);
        setContentView(b.getRoot());
        setSupportActionBar(b.toolbar);
        Data.observeProfile(this, profile -> {
            if (profile == null) {
                Logger.debug("new-t-act", "no active profile. Redirecting to SplashActivity");
                Intent intent = new Intent(this, SplashActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
            else
                b.toolbar.setSubtitle(profile.getName());
        });

        NavHostFragment navHostFragment = (NavHostFragment) Objects.requireNonNull(
                getSupportFragmentManager().findFragmentById(R.id.new_transaction_nav));
        navController = navHostFragment.getNavController();

        Objects.requireNonNull(getSupportActionBar())
               .setDisplayHomeAsUpEnabled(true);

        model = new ViewModelProvider(this).get(NewTransactionModel.class);

        qrScanLauncher = QR.registerLauncher(this, this);

        fabManager = new FabManager(b.fabAdd);

        model.isSubmittable()
             .observe(this, isSubmittable -> {
                 if (isSubmittable) {
                     fabManager.showFab();
                 }
                 else {
                     fabManager.hideFab();
                 }
             });
//        viewModel.checkTransactionSubmittable(listAdapter);

        b.fabAdd.setOnClickListener(v -> onFabPressed());
    }
    @Override
    protected void initProfile() {
        long profileId = getIntent().getLongExtra(PARAM_PROFILE_ID, 0);
        int profileHue = getIntent().getIntExtra(PARAM_THEME, -1);

        if (profileHue < 0) {
            Logger.debug(TAG, "Started with invalid/missing theme; quitting");
            finish();
            return;
        }

        if (profileId <= 0) {
            Logger.debug(TAG, "Started with invalid/missing profile_id; quitting");
            finish();
            return;
        }

        setupProfileColors(profileHue);
        initProfile(profileId);
    }
    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.dummy, R.anim.slide_out_down);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    public void onTransactionSave(LedgerTransaction tr) {
        navController.navigate(R.id.action_newTransactionFragment_to_newTransactionSavingFragment);
        try {

            SendTransactionTask saver =
                    new SendTransactionTask(this, mProfile, tr, model.getSimulateSaveFlag());
            saver.start();
        }
        catch (Exception e) {
            debug("new-transaction", "Unknown error: " + e);

            Bundle b = new Bundle();
            b.putString("error", "unknown error");
            navController.navigate(R.id.newTransactionFragment, b);
        }
    }
    public boolean onSimulateCrashMenuItemClicked(MenuItem item) {
        debug("crash", "Will crash intentionally");
        GeneralBackgroundTasks.run(() -> { throw new RuntimeException("Simulated crash");});
        return true;
    }
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        if (!BuildConfig.DEBUG)
            return true;

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.new_transaction, menu);

        MenuCompat.setGroupDividerEnabled(menu, true);

        menu.findItem(R.id.action_simulate_save)
            .setOnMenuItemClickListener(this::onToggleSimulateSaveMenuItemClicked);
        menu.findItem(R.id.action_simulate_crash)
            .setOnMenuItemClickListener(this::onSimulateCrashMenuItemClicked);

        model.getSimulateSave()
             .observe(this, state -> {
                 menu.findItem(R.id.action_simulate_save)
                     .setChecked(state);
                 b.simulationLabel.setVisibility(state ? View.VISIBLE : View.GONE);
             });

        return true;
    }


    public int dp2px(float dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics()));
    }
    @Override
    public void onTransactionSaveDone(String error, Object arg) {
        Bundle b = new Bundle();
        if (error != null) {
            b.putString("error", error);
            navController.navigate(R.id.action_newTransactionSavingFragment_Failure, b);
        }
        else {
            navController.navigate(R.id.action_newTransactionSavingFragment_Success, b);

            BaseDAO.runAsync(() -> commitToDb((LedgerTransaction) arg));
        }
    }
    public void commitToDb(LedgerTransaction tr) {
        TransactionWithAccounts dbTransaction = tr.toDBO();
        DB.get()
          .getTransactionDAO()
          .appendSync(dbTransaction);
    }
    public boolean onToggleSimulateSaveMenuItemClicked(MenuItem item) {
        model.toggleSimulateSave();
        return true;
    }

    @Override
    public void triggerQRScan() {
        qrScanLauncher.launch(null);
    }
    private void startNewPatternActivity(String scanned) {
        Intent intent = new Intent(this, TemplatesActivity.class);
        Bundle args = new Bundle();
        args.putString(TemplatesActivity.ARG_ADD_TEMPLATE, scanned);
        startActivity(intent, args);
    }
    private void alertNoTemplateMatch(String scanned) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setCancelable(true)
               .setMessage(R.string.no_template_matches)
               .setPositiveButton(R.string.add_button,
                       (dialog, which) -> startNewPatternActivity(scanned))
               .create()
               .show();
    }
    public void onQRScanResult(String text) {
        Logger.debug("qr", String.format("Got QR scan result [%s]", text));

        if (Misc.emptyIsNull(text) == null)
            return;

        LiveData<List<TemplateHeader>> allTemplates = DB.get()
                                                        .getTemplateDAO()
                                                        .getTemplates();
        allTemplates.observe(this, templateHeaders -> {
            ArrayList<MatchedTemplate> matchingFallbackTemplates = new ArrayList<>();
            ArrayList<MatchedTemplate> matchingTemplates = new ArrayList<>();

            for (TemplateHeader ph : templateHeaders) {
                String patternSource = ph.getRegularExpression();
                if (Misc.emptyIsNull(patternSource) == null)
                    continue;
                try {
                    Pattern pattern = Pattern.compile(patternSource);
                    Matcher matcher = pattern.matcher(text);
                    if (!matcher.matches())
                        continue;

                    Logger.debug("pattern",
                            String.format("Pattern '%s' [%s] matches '%s'", ph.getName(),
                                    patternSource, text));
                    if (ph.isFallback())
                        matchingFallbackTemplates.add(
                                new MatchedTemplate(ph, matcher.toMatchResult()));
                    else
                        matchingTemplates.add(new MatchedTemplate(ph, matcher.toMatchResult()));
                }
                catch (ParcelFormatException e) {
                    // ignored
                    Logger.debug("pattern",
                            String.format("Error compiling regular expression '%s'", patternSource),
                            e);
                }
            }

            if (matchingTemplates.isEmpty())
                matchingTemplates = matchingFallbackTemplates;

            if (matchingTemplates.isEmpty())
                alertNoTemplateMatch(text);
            else if (matchingTemplates.size() == 1)
                model.applyTemplate(matchingTemplates.get(0), text);
            else
                chooseTemplate(matchingTemplates, text);
        });
    }
    private void chooseTemplate(ArrayList<MatchedTemplate> matchingTemplates, String matchedText) {
        final String templateNameColumn = "name";
        AbstractCursor cursor = new AbstractCursor() {
            @Override
            public int getCount() {
                return matchingTemplates.size();
            }
            @Override
            public String[] getColumnNames() {
                return new String[]{"_id", templateNameColumn};
            }
            @Override
            public String getString(int column) {
                if (column == 0)
                    return String.valueOf(getPosition());
                return matchingTemplates.get(getPosition()).templateHead.getName();
            }
            @Override
            public short getShort(int column) {
                if (column == 0)
                    return (short) getPosition();
                return -1;
            }
            @Override
            public int getInt(int column) {
                return getShort(column);
            }
            @Override
            public long getLong(int column) {
                return getShort(column);
            }
            @Override
            public float getFloat(int column) {
                return getShort(column);
            }
            @Override
            public double getDouble(int column) {
                return getShort(column);
            }
            @Override
            public boolean isNull(int column) {
                return false;
            }
            @Override
            public int getColumnCount() {
                return 2;
            }
        };

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setCancelable(true)
               .setTitle(R.string.choose_template_to_apply)
               .setIcon(R.drawable.ic_baseline_auto_graph_24)
               .setSingleChoiceItems(cursor, -1, templateNameColumn, (dialog, which) -> {
                   model.applyTemplate(matchingTemplates.get(which), matchedText);
                   dialog.dismiss();
               })
               .create()
               .show();
    }
    public void onDescriptionSelected(String description) {
        debug("description selected", description);
        if (!model.accountListIsEmpty())
            return;

        BaseDAO.runAsync(() -> {
            String accFilter = mProfile.getPreferredAccountsFilter();

            TransactionDAO trDao = DB.get()
                                     .getTransactionDAO();

            TransactionWithAccounts tr = null;

            if (Misc.emptyIsNull(accFilter) != null)
                tr = trDao.getFirstByDescriptionHavingAccountSync(description, accFilter);
            if (tr == null)
                tr = trDao.getFirstByDescriptionSync(description);

            if (tr != null)
                model.loadTransactionIntoModel(tr);
        });
    }
    private void onFabPressed() {
        fabManager.hideFab();
        Misc.hideSoftKeyboard(this);

        LedgerTransaction tr = model.constructLedgerTransaction();

        onTransactionSave(tr);
    }
    @Override
    public Context getContext() {
        return this;
    }
    @Override
    public void showManagedFab() {
        if (Objects.requireNonNull(model.isSubmittable()
                                        .getValue()))
            fabManager.showFab();
    }
    @Override
    public void hideManagedFab() {
        fabManager.hideFab();
    }
}
