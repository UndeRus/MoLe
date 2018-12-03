package net.ktnx.mobileledger;

import android.annotation.TargetApi;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.FontsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.SimpleCursorAdapter;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.Objects;

public class NewTransactionActivity extends AppCompatActivity implements TaskCallback {
    private TableLayout table;
    private FloatingActionButton fab;
    private TextView text_date;
    private TextView text_descr;
    private static SaveTransactionTask saver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_transaction);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        text_date = findViewById(R.id.new_transaction_date);
        text_descr = findViewById(R.id.new_transaction_description);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            text_descr.setAutofillHints("");
        }

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new_transaction_save_clicked(view);
            }
        });
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        table = findViewById(R.id.new_transaction_accounts_table);
        for (int i = 0; i < table.getChildCount(); i++) {
            hook_swipe_listener((TableRow)table.getChildAt(i));
            hook_autocompletion_adapter((TableRow)table.getChildAt(i));
//            Log.d("swipe", "hooked to row "+i);
        }
    }

    public void new_transaction_save_clicked(View view) {
        fab.setEnabled(false);

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

    @TargetApi(Build.VERSION_CODES.N)
    private void hook_autocompletion_adapter(final TableRow row) {
        String[] from = {"name"};
        int[] to = {android.R.id.text1};
        SQLiteDatabase db = MobileLedgerDB.db;

        AutoCompleteTextView acc = (AutoCompleteTextView) row.getChildAt(0);
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, android.R.layout.simple_dropdown_item_1line, null, from, to, 0);
        adapter.setStringConversionColumn(1);

        FilterQueryProvider provider = new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {
                if (constraint == null) return null;

                String str = constraint.toString().toUpperCase();
                Log.d("autocompletion", "Looking for "+str);
                String[] col_names = {FontsContract.Columns._ID, "name"};
                MatrixCursor c = new MatrixCursor(col_names);

                Cursor matches = db.rawQuery("SELECT name FROM accounts WHERE UPPER(name) LIKE '%'||?||'%' ORDER BY name;", new String[]{str});

                try {
                    int i = 0;
                    while (matches.moveToNext()) {
                        String name = matches.getString(0);
                        Log.d("autocompletion-match", name);
                        c.newRow().add(i++).add(name);
                    }
                }
                finally {
                    matches.close();
                }

                return c;

            }
        };

        adapter.setFilterQueryProvider(provider);

        acc.setAdapter(adapter);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.new_transaction, menu);

        return true;
    }

    public void pickTransactionDate(View view) {
        DialogFragment picker = new DatePickerFragment();
        picker.show(getSupportFragmentManager(), "datePicker");
    }

    public int dp2px(float dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
    }

    public void addTransactionAccountFromMenu(MenuItem item) {
        final AutoCompleteTextView acc = new AutoCompleteTextView(this);
        acc.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 9f));
        acc.setHint(R.string.new_transaction_account_hint);
        acc.setWidth(0);

        final EditText amt = new EditText(this);
        amt.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT, 1f));
        amt.setHint(R.string.new_transaction_amount_hint);
        amt.setWidth(0);
        amt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL );
        amt.setMinWidth(dp2px(40));
        amt.setTextAlignment(EditText.TEXT_ALIGNMENT_VIEW_END);

        final TableRow row = new TableRow(this);
        row.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT));
        row.addView(acc);
        row.addView(amt);
        table.addView(row);

        acc.requestFocus();

        hook_swipe_listener(row);
        hook_autocompletion_adapter(row);
    }

    @Override
    public void done() {
        fab.setEnabled(true);
        reset_form();
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
