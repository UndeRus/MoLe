/*
 * Copyright © 2018 Damyan Ivanov.
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

package net.ktnx.mobileledger;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.FontsContract;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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
import android.widget.FilterQueryProvider;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import net.ktnx.mobileledger.async.SaveTransactionTask;
import net.ktnx.mobileledger.async.TaskCallback;
import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.model.LedgerTransactionItem;
import net.ktnx.mobileledger.ui.DatePickerFragment;
import net.ktnx.mobileledger.utils.MobileLedgerDatabase;

import java.util.Date;
import java.util.Objects;

/*
 * TODO: nicer progress while transaction is submitted
 * TODO: latest transactions, maybe with browsing further in the past?
 * TODO: reports
 * TODO: get rid of the custom session/cookie and auth code?
 *         (the last problem with the POST was the missing content-length header)
 * TODO: app icon
 * TODO: nicer swiping removal with visual feedback
 * TODO: setup wizard
 * TODO: update accounts/check settings upon change of backend settings
 *  */

public class NewTransactionActivity extends AppCompatActivity implements TaskCallback {
    private TableLayout table;
    private ProgressBar progress;
    private TextView text_date;
    private AutoCompleteTextView text_descr;
    private static SaveTransactionTask saver;
    private MenuItem mSave;
    private MobileLedgerDatabase dbh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_transaction);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        dbh = new MobileLedgerDatabase(this);

        text_date = findViewById(R.id.new_transaction_date);
        text_date.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public
            void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) pickTransactionDate(v);
            }
        });
        text_descr = findViewById(R.id.new_transaction_description);
        hook_autocompletion_adapter(text_descr, MobileLedgerDatabase
                .DESCRIPTION_HISTORY_TABLE, "description");
        hook_text_change_listener(text_descr);

        progress = findViewById(R.id.save_transaction_progress);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        table = findViewById(R.id.new_transaction_accounts_table);
        for (int i = 0; i < table.getChildCount(); i++) {
            TableRow row = (TableRow) table.getChildAt(i);
            AutoCompleteTextView acc_name_view = (AutoCompleteTextView) row.getChildAt(0);
            TextView amount_view = (TextView) row.getChildAt(1);
            hook_swipe_listener(row);
            hook_autocompletion_adapter(acc_name_view, MobileLedgerDatabase.ACCOUNTS_TABLE, "name");
            hook_text_change_listener(acc_name_view);
            hook_text_change_listener(amount_view);
//            Log.d("swipe", "hooked to row "+i);
        }
    }

    @Override
    protected
    void onStart() {
        super.onStart();
        if (text_descr.getText().toString().isEmpty()) text_descr.requestFocus();
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

    public void save_transaction() {
        if (mSave != null) mSave.setVisible(false);
        toggle_all_editing(false);
        progress.setVisibility(View.VISIBLE);

        saver = new SaveTransactionTask(this);

        saver.setPref(PreferenceManager.getDefaultSharedPreferences(this));
        String date = text_date.getText().toString();
        if (date.isEmpty()) date = String.valueOf(new Date().getDate());
        LedgerTransaction tr = new LedgerTransaction(date, text_descr.getText().toString());

        TableLayout table = findViewById(R.id.new_transaction_accounts_table);
        for ( int i = 0; i < table.getChildCount(); i++ ) {
            TableRow row = (TableRow) table.getChildAt(i);
            String acc = ((TextView) row.getChildAt(0)).getText().toString();
            String amt = ((TextView) row.getChildAt(1)).getText().toString();
            LedgerTransactionItem item =
                    amt.length() > 0
                    ? new LedgerTransactionItem( acc, Float.parseFloat(amt))
                    : new LedgerTransactionItem( acc );

            tr.add_item(item);
        }
        saver.execute(tr);
    }

    private void toggle_all_editing(boolean enabled) {
        text_date.setEnabled(enabled);
        text_descr.setEnabled(enabled);
        TableLayout table = findViewById(R.id.new_transaction_accounts_table);
        for (int i = 0; i < table.getChildCount(); i++) {
            TableRow row = (TableRow) table.getChildAt(i);
            for (int j = 0; j < row.getChildCount(); j++) {
                row.getChildAt(j).setEnabled(enabled);
            }
        }
    }

    private void hook_swipe_listener(final TableRow row) {
        row.getChildAt(0).setOnTouchListener(new OnSwipeTouchListener(this) {
            public void onSwipeLeft() {
//                Log.d("swipe", "LEFT" + row.getId());
                if (table.getChildCount() > 2) {
                    TableRow prev_row = (TableRow) table.getChildAt(table.indexOfChild(row) - 1);
                    TableRow next_row = (TableRow) table.getChildAt(table.indexOfChild(row) + 1);
                    TextView prev_amt =
                            (prev_row != null) ? (TextView) prev_row.getChildAt(1) : text_descr;
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
                    Snackbar.make(table, R.string.msg_at_least_two_accounts_are_required, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
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

    private void hook_text_change_listener(final TextView view) {
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

    @TargetApi(Build.VERSION_CODES.N)
    private void hook_autocompletion_adapter(final AutoCompleteTextView view, final String table, final String field) {
        String[] from = {field};
        int[] to = {android.R.id.text1};
        SimpleCursorAdapter adapter =
                new SimpleCursorAdapter(this, android.R.layout.simple_dropdown_item_1line, null,
                        from, to, 0);
        adapter.setStringConversionColumn(1);

        FilterQueryProvider provider = new FilterQueryProvider() {
            @Override
            public
            Cursor runQuery(CharSequence constraint) {
                if (constraint == null) return null;

                String str = constraint.toString().toUpperCase();
                Log.d("autocompletion", "Looking for " + str);
                String[] col_names = {FontsContract.Columns._ID, field};
                MatrixCursor c = new MatrixCursor(col_names);

                try (SQLiteDatabase db = dbh.getReadableDatabase()) {

                    try (Cursor matches = db.rawQuery(String.format(
                            "SELECT %s as a, case when %s_upper LIKE ?||'%%' then 1 "
                                    + "WHEN %s_upper LIKE '%%:'||?||'%%' then 2 "
                                    + "WHEN %s_upper LIKE '%% '||?||'%%' then 3 " + "else 9 end "
                                    + "FROM %s " + "WHERE %s_upper LIKE '%%'||?||'%%' "
                                    + "ORDER BY 2, 1;", field, field, field, field, table, field),
                            new String[]{str, str, str, str}))
                    {
                        int i = 0;
                        while (matches.moveToNext()) {
                            String match = matches.getString(0);
                            int order = matches.getInt(1);
                            Log.d("autocompletion", String.format("match: %s |%d", match, order));
                            c.newRow().add(i++).add(match);
                        }
                    }

                    return c;
                }

            }
        };

        adapter.setFilterQueryProvider(provider);

        view.setAdapter(adapter);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.new_transaction, menu);
        mSave = menu.findItem(R.id.action_submit_transaction);
        if (mSave == null) throw new AssertionError();

        check_transaction_submittable();

        return true;
    }

    public void pickTransactionDate(View view) {
        DialogFragment picker = new DatePickerFragment();
        picker.show(getSupportFragmentManager(), "datePicker");
    }

    public int dp2px(float dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
    }

    private void do_add_account_row(boolean focus) {
        final AutoCompleteTextView acc = new AutoCompleteTextView(this);
        acc.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 9f));
        acc.setHint(R.string.new_transaction_account_hint);
        acc.setWidth(0);
        acc.setImeOptions(EditorInfo.IME_ACTION_NEXT | EditorInfo.IME_FLAG_NO_ENTER_ACTION
                | EditorInfo.IME_FLAG_NAVIGATE_NEXT);
        acc.setSingleLine(true);

        final EditText amt = new EditText(this);
        amt.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.MATCH_PARENT, 1f));
        amt.setHint(R.string.new_transaction_amount_hint);
        amt.setWidth(0);
        amt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL );
        amt.setMinWidth(dp2px(40));
        amt.setTextAlignment(EditText.TEXT_ALIGNMENT_VIEW_END);
        amt.setImeOptions(EditorInfo.IME_ACTION_DONE);

        // forward navigation support
        final TableRow last_row = (TableRow) table.getChildAt(table.getChildCount() - 1);
        final TextView last_amt = (TextView) last_row.getChildAt(1);
        last_amt.setNextFocusForwardId(acc.getId());
        last_amt.setNextFocusRightId(acc.getId());
        last_amt.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        acc.setNextFocusForwardId(amt.getId());
        acc.setNextFocusRightId(amt.getId());

        final TableRow row = new TableRow(this);
        row.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT));
        row.setGravity(Gravity.BOTTOM);
        row.addView(acc);
        row.addView(amt);
        table.addView(row);

        if (focus) acc.requestFocus();

        hook_swipe_listener(row);
        hook_autocompletion_adapter(acc, MobileLedgerDatabase.ACCOUNTS_TABLE, "name");
        hook_text_change_listener(acc);
        hook_text_change_listener(amt);
    }

    public void addTransactionAccountFromMenu(MenuItem item) {
        do_add_account_row(true);
    }

    public
    void resetTransactionFromMenu(MenuItem item) {
        reset_form();
    }

    public void saveTransactionFromMenu(MenuItem item) {
        save_transaction();
    }

    private
    boolean is_zero(float f) {
        return (f < 0.005) && (f > -0.005);
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

            if ((empty_rows == 0) && ((table.getChildCount() == accounts) || (table.getChildCount()
                    == amounts)))
            {
                do_add_account_row(false);
            }

            Log.d("submittable", String.format("accounts=%d, accounts_with_values=%s, "
                            + "amounts_with_accounts=%d, amounts=%d, running_total=%1.2f, "
                            + "single_empty_with_acc=%s", accounts, accounts_with_values,
                    amounts_with_accounts, amounts, running_total,
                    (single_empty_amount && single_empty_amount_has_account) ? "true" : "false"));

            if (have_description && (accounts >= 2) && (accounts_with_values >= (accounts - 1)) && (
                    amounts_with_accounts == amounts) && (
                    single_empty_amount && single_empty_amount_has_account || is_zero(
                            running_total)))
            {
                if (mSave != null) mSave.setVisible(true);
            }
            else if (mSave != null) mSave.setVisible(false);

            if (single_empty_amount) {
                empty_amount
                        .setHint(String.format("%1.2f", (running_total > 0) ? -running_total : 0f));
            }

        }
        catch (NumberFormatException e) {
            if (mSave != null) mSave.setVisible(false);
        }
        catch (Exception e) {
            e.printStackTrace();
            if (mSave != null) mSave.setVisible(false);
        }
    }

    @Override
    public
    void done(String error) {
        progress.setVisibility(View.INVISIBLE);
        Log.d("visuals", "hiding progress");

        if (error == null) reset_form();
        else Snackbar.make(findViewById(R.id.new_transaction_accounts_table), error,
                BaseTransientBottomBar.LENGTH_LONG).show();

        toggle_all_editing(true);
        check_transaction_submittable();
    }

    private void reset_form() {
        text_date.setText("");
        text_descr.setText("");

        text_descr.requestFocus();

        while(table.getChildCount() > 2) {
            table.removeViewAt(2);
        }
        for( int i = 0; i < 2; i++ ) {
            TableRow tr = (TableRow) table.getChildAt(i);
            if ( tr == null) break;

            ((TextView)tr.getChildAt(0)).setText("");
            ((TextView)tr.getChildAt(1)).setText("");
        }
    }
}
