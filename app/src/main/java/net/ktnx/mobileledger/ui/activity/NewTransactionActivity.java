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

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import net.ktnx.mobileledger.BuildConfig;
import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.async.SendTransactionTask;
import net.ktnx.mobileledger.async.TaskCallback;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.model.LedgerTransactionAccount;

import java.util.Date;
import java.util.Objects;

import static net.ktnx.mobileledger.utils.Logger.debug;

/*
 * TODO: nicer progress while transaction is submitted
 * TODO: reports
 * TODO: get rid of the custom session/cookie and auth code?
 *         (the last problem with the POST was the missing content-length header)
 *  */

public class NewTransactionActivity extends ProfileThemedActivity implements TaskCallback {
    private static SendTransactionTask saver;
    private ProgressBar progress;
    private FloatingActionButton fab;
    private NewTransactionItemsAdapter listAdapter;
    private NewTransactionModel viewModel;
    private RecyclerView list;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_new_transaction);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Data.profile.observe(this,
                mobileLedgerProfile -> toolbar.setSubtitle(mobileLedgerProfile.getName()));

        progress = findViewById(R.id.save_transaction_progress);
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> saveTransaction());

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        list = findViewById(R.id.new_transaction_accounts);
        viewModel = ViewModelProviders.of(this).get(NewTransactionModel.class);
        listAdapter = new NewTransactionItemsAdapter(viewModel, mProfile);
        list.setAdapter(listAdapter);
        list.setLayoutManager(new LinearLayoutManager(this));
        Data.profile.observe(this, profile -> listAdapter.setProfile(profile));
        listAdapter.notifyDataSetChanged();
        new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView,
                                        @NonNull RecyclerView.ViewHolder viewHolder) {
                int flags = makeFlag(ItemTouchHelper.ACTION_STATE_IDLE, ItemTouchHelper.END);
                // the top item is always there (date and description)
                if (viewHolder.getAdapterPosition() > 0) {
                    if (viewModel.getAccountCount() > 2) {
                        flags |= makeFlag(ItemTouchHelper.ACTION_STATE_SWIPE,
                                ItemTouchHelper.START | ItemTouchHelper.END);
                    }
                }

                return flags;
            }
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                if (viewModel.getAccountCount() == 2)
                    Snackbar.make(list, R.string.msg_at_least_two_accounts_are_required,
                            Snackbar.LENGTH_LONG).setAction("Action", null).show();
                else {
                    int pos = viewHolder.getAdapterPosition();
                    viewModel.removeItem(pos - 1);
                    listAdapter.notifyItemRemoved(pos);
                    viewModel.sendCountNotifications(); // needed after items re-arrangement
                    viewModel.checkTransactionSubmittable(listAdapter);
                }
            }
        }).attachToRecyclerView(list);

        viewModel.isSubmittable().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isSubmittable) {
                if (isSubmittable) {
                    if (fab != null) {
                        fab.show();
                        fab.setEnabled(true);
                    }
                }
                else {
                    if (fab != null) {
                        fab.hide();
                    }
                }
            }
        });
        viewModel.checkTransactionSubmittable(listAdapter);
    }
    @Override
    protected void initProfile() {
        String profileUUID = getIntent().getStringExtra("profile_uuid");

        if (profileUUID != null) {
            mProfile = Data.getProfile(profileUUID);
            if (mProfile == null) finish();
            Data.setCurrentProfile(mProfile);
        }
        else super.initProfile();
    }
    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.dummy, R.anim.slide_out_right);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    protected void onStart() {
        super.onStart();
        // FIXME if (tvDescription.getText().toString().isEmpty()) tvDescription.requestFocus();
    }
    public void saveTransaction() {
        if (fab != null) fab.setEnabled(false);
        listAdapter.toggleAllEditing(false);
        progress.setVisibility(View.VISIBLE);
        try {

            saver = new SendTransactionTask(this, mProfile);

            Date date = viewModel.getDate();
            LedgerTransaction tr =
                    new LedgerTransaction(null, date, viewModel.getDescription(),
                            mProfile);

            LedgerTransactionAccount emptyAmountAccount = null;
            float emptyAmountAccountBalance = 0;
            for (int i = 0; i < viewModel.getAccountCount(); i++) {
                LedgerTransactionAccount acc = viewModel.getAccount(i);
                if (acc.getAccountName().trim().isEmpty()) continue;

                if (acc.isAmountSet()) {
                    emptyAmountAccountBalance += acc.getAmount();
                }
                else {
                    emptyAmountAccount = acc;
                }

                tr.addAccount(acc);
            }

            if (emptyAmountAccount != null)
                emptyAmountAccount.setAmount(-emptyAmountAccountBalance);
            saver.execute(tr);
        }
        catch (Exception e) {
            debug("new-transaction", "Unknown error", e);

            progress.setVisibility(View.GONE);
            listAdapter.toggleAllEditing(true);
            if (fab != null) fab.setEnabled(true);
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
            menu.findItem(R.id.action_simulate_crash).setVisible(true);
        }

        return true;
    }


    public int dp2px(float dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics()));
    }
    public void resetTransactionFromMenu(MenuItem item) {
        resetForm();
    }
    @Override
    public void done(String error) {
        progress.setVisibility(View.INVISIBLE);
        debug("visuals", "hiding progress");

        if (error == null) resetForm();
        else Snackbar.make(list, error, BaseTransientBottomBar.LENGTH_LONG).show();

        listAdapter.toggleAllEditing(true);
        viewModel.checkTransactionSubmittable(listAdapter);
    }

    private void resetForm() {
        listAdapter.reset();
    }
    private class AsyncCrasher extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            throw new RuntimeException("Simulated crash");
        }
    }

}
