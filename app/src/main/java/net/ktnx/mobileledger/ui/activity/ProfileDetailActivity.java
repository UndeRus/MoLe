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

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
import net.ktnx.mobileledger.ui.profiles.ProfileDetailFragment;
import net.ktnx.mobileledger.utils.Colors;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

/**
 * An activity representing a single Profile detail screen. This
 * activity is only used on narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a ProfileListActivity (not really).
 */
public class ProfileDetailActivity extends CrashReportingActivity {
    private MobileLedgerProfile profile = null;
    private ProfileDetailFragment mFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final int index = getIntent().getIntExtra(ProfileDetailFragment.ARG_ITEM_ID, -1);

        if (index != -1) {
            profile = Data.profiles.get(index);
            if (profile == null) throw new AssertionError(
                    String.format("Can't get profile " + "(index:%d) from the global list", index));

            Log.d("profiles", String.format("Editing profile %s (%s); hue=%d", profile.getName(),
                    profile.getUuid(), profile.getThemeId()));
        }

        super.onCreate(savedInstanceState);
        Colors.setupTheme(this, profile);
        setContentView(R.layout.activity_profile_detail);
        Toolbar toolbar = findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putInt(ProfileDetailFragment.ARG_ITEM_ID, index);
            mFragment = new ProfileDetailFragment();
            mFragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.profile_detail_container, mFragment).commit();
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        Log.d("profiles", "[activity] Creating profile details options menu");
        if (mFragment != null) mFragment.onCreateOptionsMenu(menu, getMenuInflater());

        return true;
    }

}
