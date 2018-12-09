package net.ktnx.mobileledger;

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
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.Objects;

/*
 * TODO: auto-fill of transaction description
 *       if Android O's implementation won't work, add a custom one
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_transaction);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        text_date = findViewById(R.id.new_transaction_date);
        text_descr = findViewById(R.id.new_transaction_description);
        hook_autocompletion_adapter(text_descr, MobileLedgerDB.DESCRIPTION_HISTORY_TABLE, "description");

        progress = findViewById(R.id.save_transaction_progress);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        table = findViewById(R.id.new_transaction_accounts_table);
        for (int i = 0; i < table.getChildCount(); i++) {
            TableRow row = (TableRow) table.getChildAt(i);
            AutoCompleteTextView acc_name_view = (AutoCompleteTextView) row.getChildAt(0);
            TextView amount_view = (TextView) row.getChildAt(1);
            hook_swipe_listener(row);
            hook_autocompletion_adapter(acc_name_view, MobileLedgerDB.ACCOUNTS_TABLE, "name");
            hook_text_change_listener(acc_name_view);
            hook_text_change_listener(amount_view);
//            Log.d("swipe", "hooked to row "+i);
        }
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
        LedgerTransaction tr = new LedgerTransaction(text_date.getText().toString(), text_descr.getText().toString());

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
                    table.removeView(row);
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
        SQLiteDatabase db = MobileLedgerDB.db;

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, android.R.layout.simple_dropdown_item_1line, null, from, to, 0);
        adapter.setStringConversionColumn(1);

        FilterQueryProvider provider = new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {
                if (constraint == null) return null;

                String str = constraint.toString().toUpperCase();
                Log.d("autocompletion", "Looking for "+str);
                String[] col_names = {FontsContract.Columns._ID, field};
                MatrixCursor c = new MatrixCursor(col_names);

                Cursor matches = db.rawQuery(String.format(
                        "SELECT %s as a, case when %s_upper LIKE ?||'%%' then 1 " +
                                "WHEN %s_upper LIKE '%%:'||?||'%%' then 2 " +
                                "WHEN %s_upper LIKE '%% '||?||'%%' then 3 " + "else 9 end " +
                                "FROM %s " + "WHERE %s_upper LIKE '%%'||?||'%%' " +
                                "ORDER BY 2, 1;", field, field, field, field, table, field),
                        new String[]{str, str, str, str});

                try {
                    int i = 0;
                    while (matches.moveToNext()) {
                        String match = matches.getString(0);
                        int order = matches.getInt(1);
                        Log.d("autocompletion", String.format("match: %s |%d", match, order));
                        c.newRow().add(i++).add(match);
                    }
                }
                finally {
                    matches.close();
                }

                return c;

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

        final EditText amt = new EditText(this);
        amt.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.MATCH_PARENT, 1f));
        amt.setHint(R.string.new_transaction_amount_hint);
        amt.setWidth(0);
        amt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL );
        amt.setMinWidth(dp2px(40));
        amt.setTextAlignment(EditText.TEXT_ALIGNMENT_VIEW_END);

        final TableRow row = new TableRow(this);
        row.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT));
        row.setGravity(Gravity.BOTTOM);
        row.addView(acc);
        row.addView(amt);
        table.addView(row);

        if (focus) acc.requestFocus();

        hook_swipe_listener(row);
        hook_autocompletion_adapter(acc, MobileLedgerDB.ACCOUNTS_TABLE, "name");
        hook_text_change_listener(acc);
        hook_text_change_listener(amt);
    }

    public void addTransactionAccountFromMenu(MenuItem item) {
        do_add_account_row(true);
    }

    public void saveTransactionFromMenu(MenuItem item) {
        save_transaction();
    }

    private void check_transaction_submittable() {
        TableLayout table = findViewById(R.id.new_transaction_accounts_table);
        int accounts = 0;
        int accounts_with_values = 0;
        int empty_rows = 0;
        for(int i = 0; i < table.getChildCount(); i++ ) {
            TableRow row = (TableRow) table.getChildAt(i);

            TextView acc_name_v = (TextView) row.getChildAt(0);

            String acc_name = String.valueOf(acc_name_v.getText());
            acc_name = acc_name.trim();
            if (!acc_name.isEmpty()) {
                accounts++;

                TextView amount_v = (TextView) row.getChildAt(1);
                String amt = String.valueOf(amount_v.getText());

                if (!amt.isEmpty()) accounts_with_values++;
            } else empty_rows++;
        }

        if (accounts_with_values == accounts && empty_rows == 0) {
            do_add_account_row(false);
        }

        if ((accounts >= 2) && (accounts_with_values >= (accounts - 1))) {
            if (mSave != null) mSave.setVisible(true);
        } else {
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
        while(table.getChildCount() > 2) {
            table.removeViewAt(2);
        }
        for( int i = 0; i < 2; i++ ) {
            TableRow tr = (TableRow) table.getChildAt(i);
            if ( tr == null) break;

            ((TextView)tr.getChildAt(0)).setText("");
            ((TextView)tr.getChildAt(1)).setText("");
        }

        text_descr.requestFocus();
    }
}
