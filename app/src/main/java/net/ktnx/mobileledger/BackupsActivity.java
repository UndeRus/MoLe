/*
 * Copyright © 2021 Damyan Ivanov.
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

package net.ktnx.mobileledger;

import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import net.ktnx.mobileledger.async.ConfigReader;
import net.ktnx.mobileledger.async.ConfigWriter;
import net.ktnx.mobileledger.databinding.FragmentBackupsBinding;
import net.ktnx.mobileledger.model.Data;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BackupsActivity extends AppCompatActivity {
    private FragmentBackupsBinding b;
    private ActivityResultLauncher<String> backupChooserLauncher;
    private ActivityResultLauncher<String[]> restoreChooserLauncher;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = FragmentBackupsBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        setSupportActionBar(b.toolbar);
        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        b.backupButton.setOnClickListener(this::backupClicked);
        b.restoreButton.setOnClickListener(this::restoreClicked);


        backupChooserLauncher =
                registerForActivityResult(new ActivityResultContracts.CreateDocument(),
                        this::storeConfig);
        restoreChooserLauncher =
                registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                        this::readConfig);

        Data.observeProfile(this, p -> {
            if (p == null) {
                b.backupButton.setEnabled(false);
                b.backupExplanationText.setEnabled(false);
            }
            else {
                b.backupButton.setEnabled(true);
                b.backupExplanationText.setEnabled(true);
            }
        });
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void storeConfig(Uri result) {
        if (result == null)
            return;

        try {
            ConfigWriter saver =
                    new ConfigWriter(getBaseContext(), result, new ConfigWriter.OnErrorListener() {
                        @Override
                        public void error(Exception e) {
                            Snackbar.make(b.backupButton, e.toString(),
                                    BaseTransientBottomBar.LENGTH_LONG)
                                    .show();
                        }
                    }, new ConfigWriter.OnDoneListener() {
                        public void done() {
                            Snackbar.make(b.backupButton, R.string.config_saved,
                                    Snackbar.LENGTH_LONG)
                                    .show();
                        }
                    });
            saver.start();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }
    private void readConfig(Uri result) {
        if (result == null)
            return;

        try {
            ConfigReader reader =
                    new ConfigReader(getBaseContext(), result, new ConfigWriter.OnErrorListener() {
                        @Override
                        public void error(Exception e) {
                            Snackbar.make(b.backupButton, e.toString(),
                                    BaseTransientBottomBar.LENGTH_LONG)
                                    .show();
                        }
                    }, new ConfigReader.OnDoneListener() {
                        public void done() {
                            Snackbar.make(b.backupButton, R.string.config_restored,
                                    Snackbar.LENGTH_LONG)
                                    .show();
                        }
                    });
            reader.start();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }
    private void backupClicked(View view) {
        final Date now = new Date();
        DateFormat df = new SimpleDateFormat("y-MM-dd HH:mm", Locale.getDefault());
        df.format(now);
        backupChooserLauncher.launch(String.format("MoLe-%s.json", df.format(now)));
    }
    private void restoreClicked(View view) {
        restoreChooserLauncher.launch(new String[]{"application/json"});
    }

}