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

package net.ktnx.mobileledger.ui.profiles;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.CollapsingToolbarLayout;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.db.DB;
import net.ktnx.mobileledger.db.Profile;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.ui.activity.CrashReportingActivity;
import net.ktnx.mobileledger.utils.Colors;
import net.ktnx.mobileledger.utils.Logger;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

import static net.ktnx.mobileledger.utils.Logger.debug;

/**
 * An activity representing a single Profile detail screen. This
 * activity is only used on narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a ProfileListActivity (not really).
 */
public class ProfileDetailActivity extends CrashReportingActivity {
    private static final String TAG = "profile-det-act";
    private ProfileDetailFragment mFragment;
    public static void start(Context context, @Nullable Profile profile) {
        Intent starter = new Intent(context, ProfileDetailActivity.class);
        if (profile != null) {
            starter.putExtra(ProfileDetailFragment.ARG_ITEM_ID, profile.getId());
            starter.putExtra(ProfileDetailFragment.ARG_HUE, profile.getTheme());
            Logger.debug(TAG,
                    String.format(Locale.ROOT, "Starting profile editor for profile %d, theme %d",
                            profile.getId(), profile.getTheme()));
        }
        else
            Logger.debug(TAG, "Starting empty profile editor");
        context.startActivity(starter);
    }
    @NotNull
    private ProfileDetailModel getModel() {
        return new ViewModelProvider(this).get(ProfileDetailModel.class);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final long id = getIntent().getLongExtra(ProfileDetailFragment.ARG_ITEM_ID, -1);

        DB.get()
          .getProfileDAO()
          .getById(id)
          .observe(this, this::setProfile);

        int themeHue = getIntent().getIntExtra(ProfileDetailFragment.ARG_HUE, -1);

        super.onCreate(savedInstanceState);
        if (themeHue == -1) {
            themeHue = Colors.getNewProfileThemeHue(Data.profiles.getValue());
        }
        Colors.setupTheme(this, themeHue);
        final ProfileDetailModel model = getModel();
        model.initialThemeHue = themeHue;
        model.setThemeId(themeHue);
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
            arguments.putInt(ProfileDetailFragment.ARG_HUE, themeHue);
            mFragment = new ProfileDetailFragment();
            mFragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                                       .add(R.id.profile_detail_container, mFragment)
                                       .commit();
        }
    }
    private void setProfile(Profile profile) {
        ProfileDetailModel model = new ViewModelProvider(this).get(ProfileDetailModel.class);
        CollapsingToolbarLayout appBarLayout = findViewById(R.id.toolbar_layout);
        if (appBarLayout != null) {
            if (profile != null)
                appBarLayout.setTitle(profile.getName());
            else
                appBarLayout.setTitle(getResources().getString(R.string.new_profile_title));
        }
        model.setValuesFromProfile(profile);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        debug("profiles", "[activity] Creating profile details options menu");
        if (mFragment != null)
            mFragment.onCreateOptionsMenu(menu, getMenuInflater());

        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
