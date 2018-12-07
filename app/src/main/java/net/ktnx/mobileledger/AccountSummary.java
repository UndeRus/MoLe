package net.ktnx.mobileledger;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Date;

import static android.view.View.GONE;
import static net.ktnx.mobileledger.MobileLedgerDB.db;
import static net.ktnx.mobileledger.MobileLedgerDB.set_option_value;

public class AccountSummary extends AppCompatActivity {
    DrawerLayout drawer;

    private static long account_list_last_updated;
    private static boolean account_list_needs_update = true;
    public static void preferences_changed() {
        account_list_needs_update = true;
    }
    MenuItem mRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_latest_transactions);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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
        update_accounts(false);
    }

    public void fab_new_transaction_clicked(View view) {
        Intent intent = new Intent(this, NewTransactionActivity.class);
        startActivity(intent);
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
        mRefresh = (MenuItem) menu.findItem(R.id.menu_acc_summary_refresh);
        assert mRefresh != null;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        //if (id == R.id.action_settings) {
        //    return true;
        // }

        return super.onOptionsItemSelected(item);
    }

    public void onRefreshAccountSummaryClicked(MenuItem mi) {
        update_accounts(true);
    }

    private void prepare_db() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            MobileLedgerDB.setDb_filename(this.getApplicationInfo().deviceProtectedDataDir + "/" + MobileLedgerDB.DATABASE_NAME);
        }
        else {
            MobileLedgerDB.setDb_filename(MobileLedgerDB.DATABASE_NAME);
        }
        MobileLedgerDB.initDB();

        MobileLedgerDB.applyRevisions(getResources(), getPackageName());

        account_list_last_updated = MobileLedgerDB.get_option_value("last_refresh", (long) 0);

    }

    private void update_accounts(boolean force) {
        long now = new Date().getTime();
        if ((now > (account_list_last_updated + (24 * 3600*1000))) || force) {
            Log.d("db", "accounts last updated at " + account_list_last_updated+" and now is " + now+". re-fetching");
            update_accounts();
        }
    }

    private void update_accounts() {
        mRefresh.setVisible(false);
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
                mRefresh.setVisible(true);
                if (this.error != 0) {
                    String err_text = rm.getString(this.error);
                    Log.d("visual", String.format("showing snackbar: %s", err_text));
                    Snackbar.make(drawer, err_text, Snackbar.LENGTH_LONG ).show();
                } else
                    set_option_value("last_refresh", new Date().getTime() );
            }
        };

        task.setPref(PreferenceManager.getDefaultSharedPreferences(this));
        task.execute(db);

    }
}
