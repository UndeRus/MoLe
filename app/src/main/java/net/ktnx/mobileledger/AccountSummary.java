package net.ktnx.mobileledger;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.view.View.GONE;

public class AccountSummary extends AppCompatActivity {
    DrawerLayout drawer;

    private static long account_list_last_updated;
    private static boolean account_list_needs_update = true;
    MenuItem mShowHiddenAccounts;
    SharedPreferences.OnSharedPreferenceChangeListener sBindPreferenceSummaryToValueListener;
    private AccountRowLayout clickedAccountRow;
    private MobileLedgerDatabase dbh;

    public static void preferences_changed() {
        account_list_needs_update = true;
    }
    MenuItem mRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_summary);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        dbh = new MobileLedgerDatabase(this);

        drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        android.widget.TextView ver = drawer.findViewById(R.id.drawer_version_text);

        try {
            PackageInfo pi = getApplicationContext().getPackageManager().getPackageInfo(getPackageName(), 0);
            ver.setText(pi.versionName);
        } catch (Exception e) {
            e.printStackTrace();
        }

        prepare_db();
        update_account_table();
        update_accounts(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        LinearLayout grp = drawer.findViewById(R.id.nav_actions);
        for (int i = 0; i < grp.getChildCount(); i++) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                grp.getChildAt(i).setBackgroundColor(
                        getResources().getColor(R.color.drawer_background, getTheme()));
            }
            else {
                grp.getChildAt(i)
                        .setBackgroundColor(getResources().getColor(R.color.drawer_background));
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            drawer.findViewById(R.id.nav_account_summary).setBackgroundColor(
                    getResources().getColor(R.color.table_row_even_bg, getTheme()));
        }
        else {
            drawer.findViewById(R.id.nav_account_summary)
                    .setBackgroundColor(getResources().getColor(R.color.table_row_even_bg));
        }
    }

    public void fab_new_transaction_clicked(View view) {
        Intent intent = new Intent(this, NewTransactionActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.dummy);
    }

    public void nav_exit_clicked(View view) {
        Log.w("mobileledger", "exiting");
        finish();
    }

    public void nav_settings_clicked(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.account_summary, menu);
        mRefresh = menu.findItem(R.id.menu_acc_summary_refresh);
        if (mRefresh == null) throw new AssertionError();

        mShowHiddenAccounts = menu.findItem(R.id.menu_acc_summary_show_hidden);
        if (mShowHiddenAccounts == null) throw new AssertionError();

        sBindPreferenceSummaryToValueListener =
                new SharedPreferences.OnSharedPreferenceChangeListener() {
                    @Override
                    public
                    void onSharedPreferenceChanged(SharedPreferences preference, String value) {
                        mShowHiddenAccounts
                                .setChecked(preference.getBoolean("show_hidden_accounts", false));
                    }
                };
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        pref.registerOnSharedPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        mShowHiddenAccounts.setChecked(pref.getBoolean("show_hidden_accounts", false));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        //if (id == R.id.action_settings) {
        //    return true;
        // }

        return super.onOptionsItemSelected(item);
    }

    public void onRefreshAccountSummaryClicked(MenuItem mi) {
        update_accounts(true);
    }

    public
    void onShowHiddenAccountsClicked(MenuItem mi) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean flag = pref.getBoolean("show_hidden_accounts", false);

        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("show_hidden_accounts", !flag);
        Log.d("pref", "Setting show_hidden_accounts to " + (flag ? "false" : "true"));
        editor.apply();

        update_account_table();
    }

    private void prepare_db() {
        account_list_last_updated = dbh.get_option_value("last_refresh", (long) 0);
    }

    private void update_accounts(boolean force) {
        long now = new Date().getTime();
        if ((now > (account_list_last_updated + (24 * 3600*1000))) || force) {
            Log.d("db", "accounts last updated at " + account_list_last_updated+" and now is " + now+". re-fetching");
            update_accounts();
        }
    }

    private void update_accounts() {
        if (mRefresh != null) mRefresh.setVisible(false);
        Resources rm = getResources();

        ProgressBar pb = findViewById(R.id.progressBar);
        pb.setVisibility(View.VISIBLE);
        TextView pt = findViewById(R.id.textProgress);
        pt.setVisibility(View.VISIBLE);
        pb.setIndeterminate(true);

        RetrieveAccountsTask task = new RetrieveAccountsTask() {
            @Override
            protected void onProgressUpdate(Integer... values) {
                if ( values[0] == 0 )
                    pt.setText(R.string.progress_connecting);
                else
                    pt.setText(String.format(getResources().getString(R.string.progress_N_accounts_loaded), values[0]));
            }

            @Override
            protected void onPostExecute(Void result) {
                pb.setVisibility(GONE);
                pt.setVisibility(GONE);
                if (mRefresh != null) mRefresh.setVisible(true);
                if (this.error != 0) {
                    String err_text = rm.getString(this.error);
                    Log.d("visual", String.format("showing snackbar: %s", err_text));
                    Snackbar.make(drawer, err_text, Snackbar.LENGTH_LONG ).show();
                }
                else {
                    dbh.set_option_value("last_refresh", new Date().getTime() );
                    update_account_table();
                }
            }
        };

        task.setPref(PreferenceManager.getDefaultSharedPreferences(this));
        task.execute();

    }

    public int dp2px(float dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
    }

    Pattern higher_account = Pattern.compile("^[^:]+:");

    private String strip_higher_accounts(String acc_name, int[] count) {
        count[0] = 0;
        while (true) {
            Matcher m = higher_account.matcher(acc_name);
            if (m.find()) {
                count[0]++;
                acc_name = m.replaceFirst("");
            }
            else break;
        }

        return acc_name;
    }

    public void hideAccountClicked(MenuItem item) {
        try(SQLiteDatabase db = dbh.getWritableDatabase()) {
            db.execSQL("update accounts set hidden=1 where name=?", new Object[]{clickedAccountRow.getAccountName()});
        }
        update_account_table();
    }

    @SuppressLint("DefaultLocale")
    private void update_account_table() {
        LinearLayout root = findViewById(R.id.account_root);
        root.removeAllViewsInLayout();

        View.OnCreateContextMenuListener ccml = new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                clickedAccountRow = (AccountRowLayout) v;
                getMenuInflater().inflate(R.menu.account_summary_account_menu, menu);
            }
        };

        int rowHeight =
                (int) (getTheme().obtainStyledAttributes(new int[]{android.R.attr.actionBarSize})
                        .getDimensionPixelSize(0, dp2px(56)) * 0.75);

        boolean showingHiddenAccounts = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("show_hidden_accounts", false);
        Log.d("pref", "show_hidden_accounts is " + (showingHiddenAccounts ? "true" : "false"));

        try(SQLiteDatabase db = dbh.getReadableDatabase()) {
            try (Cursor cursor = db
                    .rawQuery("SELECT name, hidden FROM accounts ORDER BY name;", null))
            {
                boolean even = false;
                String skippingAccountName = null;
                while (cursor.moveToNext()) {
                    String acc_name = cursor.getString(0);
                    if (skippingAccountName != null) {
                        if (acc_name.startsWith(skippingAccountName + ":")) continue;

                        skippingAccountName = null;
                    }

                    boolean is_hidden = cursor.getInt(1) == 1;

                    if (!showingHiddenAccounts && is_hidden) {
                        skippingAccountName = acc_name;
                        continue;
                    }

                    LinearLayout r = new AccountRowLayout(this, acc_name);
                    r.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
                    r.setGravity(Gravity.CENTER_VERTICAL);
                    r.setPadding(getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin), dp2px(3),
                            getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin),
                            dp2px(4));
                    r.setMinimumHeight(rowHeight);

                    if (even) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            r.setBackgroundColor(
                                    getResources().getColor(R.color.table_row_even_bg, getTheme()));
                        }
                        else {
                            r.setBackgroundColor(getResources().getColor(R.color.table_row_even_bg));
                        }
                    }
                    even = !even;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        r.setContextClickable(true);
                    }
                    r.setOnCreateContextMenuListener(ccml);


                    TextView acc_tv = new TextView(this, null, R.style.account_summary_account_name);
                    acc_tv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.MATCH_PARENT, 5f));
                    acc_tv.setGravity(Gravity.CENTER_VERTICAL);
                    int[] indent_level = new int[]{0};
                    String short_acc_name = strip_higher_accounts(acc_name, indent_level);
                    acc_tv.setPadding(indent_level[0] * getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin) / 2, 0, 0,
                            0);
                    acc_tv.setText(short_acc_name);
                    if (is_hidden) acc_tv.setTypeface(null, Typeface.ITALIC);
                    r.addView(acc_tv);

                    TextView amt_tv = new TextView(this, null, R.style.account_summary_amounts);
                    amt_tv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.MATCH_PARENT, 1f));
                    amt_tv.setTextAlignment(EditText.TEXT_ALIGNMENT_VIEW_END);
                    amt_tv.setGravity(Gravity.CENTER_VERTICAL);
//                amt_tv.setGravity(Gravity.CENTER);
                    amt_tv.setMinWidth(dp2px(60f));
                    StringBuilder amt_text = new StringBuilder();
                    try (Cursor cAmounts = db.rawQuery(
                            "SELECT currency, value FROM account_values WHERE account = ?", new String[]{acc_name}))
                    {
                        while (cAmounts.moveToNext()) {
                            String curr = cAmounts.getString(0);
                            Float amt = cAmounts.getFloat(1);
                            if (amt_text.length() != 0) amt_text.append('\n');
                            amt_text.append(String.format("%s %,1.2f", curr, amt));
                        }
                    }
                    amt_tv.setText(amt_text.toString());
                    if (is_hidden) amt_tv.setTypeface(null, Typeface.ITALIC);

                    r.addView(amt_tv);

                    root.addView(r);
                }
            }
        }
    }
}
