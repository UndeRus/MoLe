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

import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import net.ktnx.mobileledger.BuildConfig;
import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.async.SendTransactionTask;
import net.ktnx.mobileledger.async.TaskCallback;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerTransaction;

import java.util.Objects;

import static net.ktnx.mobileledger.utils.Logger.debug;

public class NewTransactionActivity extends ProfileThemedActivity implements TaskCallback,
        NewTransactionFragment.OnNewTransactionFragmentInteractionListener {
    private NavController navController;
    private NewTransactionModel model;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_new_transaction);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Data.observeProfile(this,
                mobileLedgerProfile -> toolbar.setSubtitle(mobileLedgerProfile.getName()));

        NavHostFragment navHostFragment = (NavHostFragment) Objects.requireNonNull(
                getSupportFragmentManager().findFragmentById(R.id.new_transaction_nav));
        navController = navHostFragment.getNavController();

        Objects.requireNonNull(getSupportActionBar())
               .setDisplayHomeAsUpEnabled(true);

        model = new ViewModelProvider(this).get(NewTransactionModel.class);
    }
    @Override
    protected void initProfile() {
        String profileUUID = getIntent().getStringExtra("profile_uuid");

        if (profileUUID != null) {
            mProfile = Data.getProfile(profileUUID);
            if (mProfile == null)
                finish();
            Data.setCurrentProfile(mProfile);
        }
        else
            super.initProfile();
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
                    new SendTransactionTask(this, mProfile, model.getSimulateSave());
            saver.execute(tr);
        }
        catch (Exception e) {
            debug("new-transaction", "Unknown error", e);

            Bundle b = new Bundle();
            b.putString("error", "unknown error");
            navController.navigate(R.id.newTransactionFragment, b);
        }
    }
    public void simulateCrash(MenuItem item) {
        debug("crash", "Will crash intentionally");
        new AsyncCrasher().execute();
    }
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.new_transaction, menu);

        if (BuildConfig.DEBUG) {
            menu.findItem(R.id.action_simulate_crash)
                .setVisible(true);
            menu.findItem(R.id.action_simulate_save)
                .setVisible(true);
        }

        model.observeSimulateSave(this, state -> {
            menu.findItem(R.id.action_simulate_save)
                .setChecked(state);
            findViewById(R.id.simulationLabel).setVisibility(state ? View.VISIBLE : View.GONE);
        });

        return true;
    }


    public int dp2px(float dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics()));
    }
    @Override
    public void done(String error) {
        Bundle b = new Bundle();
        if (error != null) {
            b.putString("error", error);
            navController.navigate(R.id.action_newTransactionSavingFragment_Failure, b);
        }
        else
            navController.navigate(R.id.action_newTransactionSavingFragment_Success, b);
    }
    public void toggleSimulateSave(MenuItem item) {
        model.toggleSimulateSave();
    }

}
