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

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import net.ktnx.mobileledger.BuildConfig;
import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.async.DescriptionSelectedCallback;
import net.ktnx.mobileledger.async.SendTransactionTask;
import net.ktnx.mobileledger.async.TaskCallback;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.model.LedgerTransactionAccount;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
import net.ktnx.mobileledger.ui.DatePickerFragment;
import net.ktnx.mobileledger.ui.OnSwipeTouchListener;
import net.ktnx.mobileledger.utils.Globals;
import net.ktnx.mobileledger.utils.LockHolder;
import net.ktnx.mobileledger.utils.MLDB;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;

/*
 * TODO: nicer progress while transaction is submitted
 * TODO: reports
 * TODO: get rid of the custom session/cookie and auth code?
 *         (the last problem with the POST was the missing content-length header)
 *  */

public class NewTransactionActivity extends ProfileThemedActivity
        implements TaskCallback, DescriptionSelectedCallback {
    private static SendTransactionTask saver;
    private TableLayout table;
    private ProgressBar progress;
    private FloatingActionButton fab;
    private TextView tvDate;
    private AutoCompleteTextView tvDescription;
    private static boolean isZero(float f) {
        return (f < 0.005) && (f > -0.005);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_new_transaction);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setSubtitle(mProfile.getName());

        tvDate = findViewById(R.id.new_transaction_date);
        tvDate.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) pickTransactionDate(v);
        });
        tvDescription = findViewById(R.id.new_transaction_description);
        MLDB.hookAutocompletionAdapter(this, tvDescription, MLDB.DESCRIPTION_HISTORY_TABLE,
                "description", false, findViewById(R.id.new_transaction_acc_1), this);
        hookTextChangeListener(tvDescription);

        progress = findViewById(R.id.save_transaction_progress);
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> saveTransaction());

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        table = findViewById(R.id.new_transaction_accounts_table);
        for (int i = 0; i < table.getChildCount(); i++) {
            TableRow row = (TableRow) table.getChildAt(i);
            AutoCompleteTextView tvAccountName = (AutoCompleteTextView) row.getChildAt(0);
            TextView tvAmount = (TextView) row.getChildAt(1);
            hookSwipeListener(row);
            MLDB.hookAutocompletionAdapter(this, tvAccountName, MLDB.ACCOUNTS_TABLE, "name", true,
                    tvAmount, null);
            hookTextChangeListener(tvAccountName);
            hookTextChangeListener(tvAmount);
//            Log.d("swipe", "hooked to row "+i);
        }
    }
    @Override
    protected void initProfile() {
        String profileUUID = getIntent().getStringExtra("profile_uuid");

        if (profileUUID != null) {
            mProfile = Data.getProfile(profileUUID);
            if (mProfile == null) finish();
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
        if (tvDescription.getText().toString().isEmpty()) tvDescription.requestFocus();
    }
    public void saveTransaction() {
        if (fab != null) fab.setEnabled(false);
        toggleAllEditing(false);
        progress.setVisibility(View.VISIBLE);
        try {

            saver = new SendTransactionTask(this, mProfile);

            String dateString = tvDate.getText().toString();
            Date date;
            if (dateString.isEmpty()) date = new Date();
            else date = Globals.parseLedgerDate(dateString);
            LedgerTransaction tr = new LedgerTransaction(date, tvDescription.getText().toString());

            TableLayout table = findViewById(R.id.new_transaction_accounts_table);
            LedgerTransactionAccount emptyAmountAccount = null;
            float emptyAmountAccountBalance = 0;
            for (int i = 0; i < table.getChildCount(); i++) {
                TableRow row = (TableRow) table.getChildAt(i);
                String acc = ((TextView) row.getChildAt(0)).getText().toString();
                if (acc.isEmpty()) continue;

                String amt = ((TextView) row.getChildAt(1)).getText().toString();
                LedgerTransactionAccount item;
                if (amt.length() > 0) {
                    final float amount = Float.parseFloat(amt);
                    item = new LedgerTransactionAccount(acc, amount);
                    emptyAmountAccountBalance += amount;
                }
                else {
                    item = new LedgerTransactionAccount(acc);
                    emptyAmountAccount = item;
                }

                tr.addAccount(item);
            }

            if (emptyAmountAccount != null)
                emptyAmountAccount.setAmount(-emptyAmountAccountBalance);
            saver.execute(tr);
        }
        catch (ParseException e) {
            Log.d("new-transaction", "Parse error", e);
            Toast.makeText(this, getResources().getString(R.string.error_invalid_date),
                    Toast.LENGTH_LONG).show();
            tvDate.requestFocus();

            progress.setVisibility(View.GONE);
            toggleAllEditing(true);
            if (fab != null) fab.setEnabled(true);
        }
        catch (Exception e) {
            Log.d("new-transaction", "Unknown error", e);

            progress.setVisibility(View.GONE);
            toggleAllEditing(true);
            if (fab != null) fab.setEnabled(true);
        }
    }
    private void toggleAllEditing(boolean enabled) {
        tvDate.setEnabled(enabled);
        tvDescription.setEnabled(enabled);
        TableLayout table = findViewById(R.id.new_transaction_accounts_table);
        for (int i = 0; i < table.getChildCount(); i++) {
            TableRow row = (TableRow) table.getChildAt(i);
            for (int j = 0; j < row.getChildCount(); j++) {
                row.getChildAt(j).setEnabled(enabled);
            }
        }
    }
    private void hookSwipeListener(final TableRow row) {
        row.getChildAt(0).setOnTouchListener(new OnSwipeTouchListener(this) {
            private void onSwipeAside() {
                if (table.getChildCount() > 2) {
                    TableRow prev_row = (TableRow) table.getChildAt(table.indexOfChild(row) - 1);
                    TableRow next_row = (TableRow) table.getChildAt(table.indexOfChild(row) + 1);
                    TextView prev_amt =
                            (prev_row != null) ? (TextView) prev_row.getChildAt(1) : tvDescription;
                    TextView next_acc =
                            (next_row != null) ? (TextView) next_row.getChildAt(0) : null;

                    if (next_acc == null) {
                        prev_amt.setNextFocusRightId(R.id.none);
                        prev_amt.setNextFocusForwardId(R.id.none);
                        prev_amt.setImeOptions(EditorInfo.IME_ACTION_DONE);
                    }
                    else {
                        prev_amt.setNextFocusRightId(next_acc.getId());
                        prev_amt.setNextFocusForwardId(next_acc.getId());
                        prev_amt.setImeOptions(EditorInfo.IME_ACTION_NEXT);
                    }

                    if (row.hasFocus()) {
                        if (next_acc != null) next_acc.requestFocus();
                        else prev_amt.requestFocus();
                    }

                    table.removeView(row);
                    check_transaction_submittable();
//                    Toast.makeText(NewTransactionActivity.this, "LEFT", Toast.LENGTH_LONG).show();
                }
                else {
                    Snackbar.make(table, R.string.msg_at_least_two_accounts_are_required,
                            Snackbar.LENGTH_LONG).setAction("Action", null).show();
                }
            }
            public void onSwipeLeft() {
                onSwipeAside();
            }
            public void onSwipeRight() {
                onSwipeAside();
            }
            //            @Override
//            public boolean performClick(View view, MotionEvent m) {
//                return true;
//            }
            public boolean onTouch(View view, MotionEvent m) {
                return gestureDetector.onTouchEvent(m);
            }
        });
    }

    public boolean simulateCrash(MenuItem item) {
        Log.d("crash", "Will crash intentionally");
        new AsyncCrasher().execute();
        return true;
    }
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.new_transaction, menu);

        if (BuildConfig.DEBUG) {
            menu.findItem(R.id.action_simulate_crash).setVisible(true);
        }
        check_transaction_submittable();

        return true;
    }

    public void pickTransactionDate(View view) {
        DialogFragment picker = new DatePickerFragment();
        picker.show(getSupportFragmentManager(), "datePicker");
    }

    public int dp2px(float dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics()));
    }
    private void hookTextChangeListener(final TextView view) {
        view.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
//                Log.d("input", "text changed");
                check_transaction_submittable();
            }
        });

    }
    private TableRow doAddAccountRow(boolean focus) {
        final AutoCompleteTextView acc = new AutoCompleteTextView(this);
        acc.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT, 9f));
        acc.setHint(R.string.new_transaction_account_hint);
        acc.setWidth(0);
        acc.setImeOptions(EditorInfo.IME_ACTION_NEXT | EditorInfo.IME_FLAG_NO_ENTER_ACTION |
                          EditorInfo.IME_FLAG_NAVIGATE_NEXT);
        acc.setSingleLine(true);

        final EditText amt = new EditText(this);
        amt.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.MATCH_PARENT, 1f));
        amt.setHint(R.string.new_transaction_amount_hint);
        amt.setWidth(0);
        amt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED |
                         InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amt.setMinWidth(dp2px(40));
        amt.setTextAlignment(EditText.TEXT_ALIGNMENT_VIEW_END);
        amt.setImeOptions(EditorInfo.IME_ACTION_DONE);
        amt.setSelectAllOnFocus(true);

        // forward navigation support
        final TableRow last_row = (TableRow) table.getChildAt(table.getChildCount() - 1);
        final TextView last_amt = (TextView) last_row.getChildAt(1);
        last_amt.setNextFocusForwardId(acc.getId());
        last_amt.setNextFocusRightId(acc.getId());
        last_amt.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        acc.setNextFocusForwardId(amt.getId());
        acc.setNextFocusRightId(amt.getId());

        final TableRow row = new TableRow(this);
        row.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.MATCH_PARENT));
        row.setGravity(Gravity.BOTTOM);
        row.addView(acc);
        row.addView(amt);
        table.addView(row);

        if (focus) acc.requestFocus();

        hookSwipeListener(row);
        MLDB.hookAutocompletionAdapter(this, acc, MLDB.ACCOUNTS_TABLE, "name", true, amt, null);
        hookTextChangeListener(acc);
        hookTextChangeListener(amt);

        return row;
    }
    public void addTransactionAccountFromMenu(MenuItem item) {
        doAddAccountRow(true);
    }
    public void resetTransactionFromMenu(MenuItem item) {
        resetForm();
    }
    public void saveTransactionFromMenu(MenuItem item) {
        saveTransaction();
    }
    // rules:
    // 1) at least two account names
    // 2) each amount must have account name
    // 3) amounts must balance to 0, or
    // 3a) there must be exactly one empty amount
    // 4) empty accounts with empty amounts are ignored
    // 5) a row with an empty account name or empty amount is guaranteed to exist
    @SuppressLint("DefaultLocale")
    private void check_transaction_submittable() {
        TableLayout table = findViewById(R.id.new_transaction_accounts_table);
        int accounts = 0;
        int accounts_with_values = 0;
        int amounts = 0;
        int amounts_with_accounts = 0;
        int empty_rows = 0;
        TextView empty_amount = null;
        boolean single_empty_amount = false;
        boolean single_empty_amount_has_account = false;
        float running_total = 0f;
        boolean have_description =
                !((TextView) findViewById(R.id.new_transaction_description)).getText().toString()
                        .isEmpty();

        try {
            for (int i = 0; i < table.getChildCount(); i++) {
                TableRow row = (TableRow) table.getChildAt(i);

                TextView acc_name_v = (TextView) row.getChildAt(0);
                TextView amount_v = (TextView) row.getChildAt(1);
                String amt = String.valueOf(amount_v.getText());
                String acc_name = String.valueOf(acc_name_v.getText());
                acc_name = acc_name.trim();

                if (!acc_name.isEmpty()) {
                    accounts++;

                    if (!amt.isEmpty()) {
                        accounts_with_values++;
                    }
                }
                else empty_rows++;

                if (amt.isEmpty()) {
                    amount_v.setHint(String.format("%1.2f", 0f));
                    if (empty_amount == null) {
                        empty_amount = amount_v;
                        single_empty_amount = true;
                        single_empty_amount_has_account = !acc_name.isEmpty();
                    }
                    else if (!acc_name.isEmpty()) single_empty_amount = false;
                }
                else {
                    amounts++;
                    if (!acc_name.isEmpty()) amounts_with_accounts++;
                    running_total += Float.valueOf(amt);
                }
            }

            if ((empty_rows == 0) &&
                ((table.getChildCount() == accounts) || (table.getChildCount() == amounts)))
            {
                doAddAccountRow(false);
            }

            Log.d("submittable", String.format("accounts=%d, accounts_with_values=%s, " +
                                               "amounts_with_accounts=%d, amounts=%d, running_total=%1.2f, " +
                                               "single_empty_with_acc=%s", accounts,
                    accounts_with_values, amounts_with_accounts, amounts, running_total,
                    (single_empty_amount && single_empty_amount_has_account) ? "true" : "false"));

            if (have_description && (accounts >= 2) && (accounts_with_values >= (accounts - 1)) &&
                (amounts_with_accounts == amounts) &&
                (single_empty_amount && single_empty_amount_has_account || isZero(running_total)))
            {
                if (fab != null) {
                    fab.show();
                    fab.setEnabled(true);
                }
            }
            else {
                if (fab != null) fab.hide();
            }

            if (single_empty_amount) {
                empty_amount.setHint(String.format("%1.2f",
                        (Math.abs(running_total) > 0.005) ? -running_total : 0f));
            }

        }
        catch (NumberFormatException e) {
            if (fab != null) fab.hide();
        }
        catch (Exception e) {
            e.printStackTrace();
            if (fab != null) fab.hide();
        }
    }

    @Override
    public void done(String error) {
        progress.setVisibility(View.INVISIBLE);
        Log.d("visuals", "hiding progress");

        if (error == null) resetForm();
        else Snackbar.make(findViewById(R.id.new_transaction_accounts_table), error,
                BaseTransientBottomBar.LENGTH_LONG).show();

        toggleAllEditing(true);
        check_transaction_submittable();
    }

    private void resetForm() {
        tvDate.setText("");
        tvDescription.setText("");

        tvDescription.requestFocus();

        while (table.getChildCount() > 2) {
            table.removeViewAt(2);
        }
        for (int i = 0; i < 2; i++) {
            TableRow tr = (TableRow) table.getChildAt(i);
            if (tr == null) break;

            ((TextView) tr.getChildAt(0)).setText("");
            ((TextView) tr.getChildAt(1)).setText("");
        }
    }
    @Override
    public void descriptionSelected(String description) {
        Log.d("descr selected", description);
        if (!inputStateIsInitial()) return;

        MobileLedgerProfile currentProfile = Data.profile.get();
        String accFilter = currentProfile.getPreferredAccountsFilter();

        ArrayList<String> params = new ArrayList<>();
        StringBuilder sb = new StringBuilder(
                "select t.profile, t.id from transactions t where t.description=?");
        params.add(description);

        if (accFilter != null) {
            sb.append(" AND EXISTS (").append("SELECT 1 FROM transaction_accounts ta ")
                    .append("WHERE ta.profile = t.profile").append(" AND ta.transaction_id = t.id")
                    .append(" AND UPPER(ta.account_name) LIKE '%'||?||'%')");
            params.add(accFilter.toUpperCase());
        }

        sb.append(" ORDER BY date desc limit 1");

        final String sql = sb.toString();
        Log.d("descr", sql);
        Log.d("descr", params.toString());

        try (Cursor c = MLDB.getDatabase().rawQuery(sql, params.toArray(new String[]{}))) {
            if (!c.moveToNext()) return;

            String profileUUID = c.getString(0);
            int transactionId = c.getInt(1);
            LedgerTransaction tr;
            try (LockHolder lh = Data.profiles.lockForReading()) {
                MobileLedgerProfile profile = null;
                for (int i = 0; i < Data.profiles.size(); i++) {
                    MobileLedgerProfile p = Data.profiles.get(i);
                    if (p.getUuid().equals(profileUUID)) {
                        profile = p;
                        break;
                    }
                }
                if (profile == null) throw new RuntimeException(String.format(
                        "Unable to find profile %s, which is supposed to contain " +
                        "transaction %d with description %s", profileUUID, transactionId,
                        description));

                tr = profile.loadTransaction(transactionId);
            }
            int i = 0;
            table = findViewById(R.id.new_transaction_accounts_table);
            ArrayList<LedgerTransactionAccount> accounts = tr.getAccounts();
            TableRow firstNegative = null;
            int negativeCount = 0;
            for (i = 0; i < accounts.size(); i++) {
                LedgerTransactionAccount acc = accounts.get(i);
                TableRow row = (TableRow) table.getChildAt(i);
                if (row == null) row = doAddAccountRow(false);

                ((TextView) row.getChildAt(0)).setText(acc.getAccountName());
                ((TextView) row.getChildAt(1))
                        .setText(String.format(Locale.US, "%1.2f", acc.getAmount()));

                if (acc.getAmount() < 0.005) {
                    if (firstNegative == null) firstNegative = row;
                    negativeCount++;
                }
            }

            if (negativeCount == 1) {
                ((TextView) firstNegative.getChildAt(1)).setText(null);
            }

            check_transaction_submittable();

            EditText firstAmount = (EditText) ((TableRow) table.getChildAt(0)).getChildAt(1);
            String amtString = String.valueOf(firstAmount.getText());
            firstAmount.requestFocus();
            firstAmount.setSelection(0, amtString.length());
        }

    }
    private boolean inputStateIsInitial() {
        table = findViewById(R.id.new_transaction_accounts_table);

        if (table.getChildCount() != 2) return false;

        for (int i = 0; i < 2; i++) {
            TableRow row = (TableRow) table.getChildAt(i);
            if (((TextView) row.getChildAt(0)).getText().length() > 0) return false;
            if (((TextView) row.getChildAt(1)).getText().length() > 0) return false;
        }

        return true;
    }
    private class AsyncCrasher extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            throw new RuntimeException("Simulated crash");
        }
    }
}
