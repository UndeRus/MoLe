package net.ktnx.mobileledger;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;

public class AccountSummary extends AppCompatActivity {
    DrawerLayout drawer;

    private static long account_list_last_updated;
    private static boolean account_list_needs_update = true;
    MenuItem mShowHiddenAccounts;
    SharedPreferences.OnSharedPreferenceChangeListener sBindPreferenceSummaryToValueListener;
    private MobileLedgerDatabase dbh;
    private AccountSummaryViewModel model;
    private AccountSummaryAdapter modelAdapter;
    private Menu optMenu;

    public static void preferences_changed() {
        account_list_needs_update = true;
    }

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

        model = ViewModelProviders.of(this).get(AccountSummaryViewModel.class);
        List<LedgerAccount> accounts = model.getAccounts();
        modelAdapter = new AccountSummaryAdapter(accounts);

        RecyclerView root = findViewById(R.id.account_root);
        root.setAdapter(modelAdapter);

        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        root.setLayoutManager(llm);

        root.addOnItemTouchListener(new RecyclerItemListener(this, root, new RecyclerItemListener.RecyclerTouchListener() {
            @Override
            public void onClickItem(View v, int position) {
                Log.d("list", String.format("item %d clicked", position));
                if (modelAdapter.isSelectionActive()) {
                    modelAdapter.selectItem(position);
                }
            }

            @Override
            public void onLongClickItem(View v, int position) {
                Log.d("list", String.format("item %d long-clicked", position));
                modelAdapter.startSelection();
                if (optMenu != null) {
                    optMenu.findItem(R.id.menu_acc_summary_cancel_selection).setVisible(true);
                    optMenu.findItem(R.id.menu_acc_summary_hide_selected).setVisible(true);
                }
            }
        }));

        root.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy < 0) ((FloatingActionButton) findViewById(R.id.btn_add_transaction)).show();
                if (dy > 0) ((FloatingActionButton) findViewById(R.id.btn_add_transaction)).hide();
            }
        });
        ((SwipeRefreshLayout) findViewById(R.id.account_swiper)).setOnRefreshListener(() -> {
            Log.d("ui", "refreshing accounts via swipe");
            update_accounts(true);
        });
        prepare_db();
//        update_account_table();
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
        Log.w("app", "exiting");
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
        optMenu = menu;

        mShowHiddenAccounts = menu.findItem(R.id.menu_acc_summary_only_starred);
        if (mShowHiddenAccounts == null) throw new AssertionError();

        sBindPreferenceSummaryToValueListener = (preference, value) -> mShowHiddenAccounts
                .setChecked(preference.getBoolean("show_hidden_accounts", false));
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
    void onShowOnlyStarredClicked(MenuItem mi) {
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
        RetrieveAccountsTask task = new RetrieveAccountsTask(new WeakReference<>(this));

        task.setPref(PreferenceManager.getDefaultSharedPreferences(this));
        task.execute();

    }
    void onAccountRefreshDone(int error) {
        SwipeRefreshLayout srl = findViewById(R.id.account_swiper);
        srl.setRefreshing(false);
        if (error != 0) {
            String err_text = getResources().getString(error);
            Log.d("visual", String.format("showing snackbar: %s", err_text));
            Snackbar.make(drawer, err_text, Snackbar.LENGTH_LONG ).show();
        }
        else {
            dbh.set_option_value("last_refresh", new Date().getTime() );
            update_account_table();
        }
    }
    private void update_account_table() {
        model.reloadAccounts();
        modelAdapter.notifyDataSetChanged();
    }
    public void onCancelAccSelection(MenuItem item) {
        modelAdapter.stopSelection();
        if (optMenu != null) {
            optMenu.findItem(R.id.menu_acc_summary_cancel_selection).setVisible(false);
            optMenu.findItem(R.id.menu_acc_summary_hide_selected).setVisible(false);
        }
    }
}
