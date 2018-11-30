package net.ktnx.mobileledger;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;

import java.util.Objects;

public class NewTransactionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_transaction);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.new_transaction, menu);

        return true;
    }

    public void pickTransactionDate(View view) {
        DialogFragment picker = new DatePickerFragment();
        picker.show(getSupportFragmentManager(), "datePicker");
//        Snackbar.make(view, "Date editing not yet ready", Snackbar.LENGTH_LONG)
//                .setAction("Action", null).show();
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
        amt.setMinWidth(64);
        amt.setTextAlignment(EditText.TEXT_ALIGNMENT_VIEW_END);

        final TableLayout table = findViewById(R.id.new_transaction_accounts_table);
        final TableRow row = new TableRow(this);
        row.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT));
        row.addView(acc);
        row.addView(amt);
        table.addView(row);

        acc.requestFocus();
    }

}
