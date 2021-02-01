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

package net.ktnx.mobileledger.ui.patterns;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.databinding.ActivityPatternsBinding;
import net.ktnx.mobileledger.ui.activity.CrashReportingActivity;
import net.ktnx.mobileledger.utils.Logger;

import java.util.Objects;

public class PatternsActivity extends CrashReportingActivity
        implements PatternListFragment.OnPatternListFragmentInteractionListener {
    public static final String ARG_ADD_PATTERN = "add-pattern";
    private ActivityPatternsBinding b;
    private NavController navController;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.pattern_list_menu, menu);

        return true;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityPatternsBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setSupportActionBar(b.toolbar);
        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        NavHostFragment navHostFragment = (NavHostFragment) Objects.requireNonNull(
                getSupportFragmentManager().findFragmentById(R.id.fragment_container));
        navController = navHostFragment.getNavController();

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.patternListFragment) {
                b.fabAdd.show();
                b.fabSave.hide();
                b.toolbarLayout.setTitle(getString(R.string.title_activity_patterns));
            }
            if (destination.getId() == R.id.patternDetailsFragment) {
                b.fabAdd.hide();
                b.fabSave.show();
            }
        });

        b.toolbarLayout.setTitle(getString(R.string.title_activity_patterns));

        b.fabAdd.setOnClickListener(v -> onEditPattern(null));
        b.fabSave.setOnClickListener(v -> onSavePattern());
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (navController.getCurrentDestination()
                             .getId() == R.id.patternDetailsFragment)
                navController.popBackStack();
            else
                finish();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onEditPattern(Long id) {
        if (id == null) {
            navController.navigate(R.id.action_patternListFragment_to_patternDetailsFragment);
            b.toolbarLayout.setTitle(getString(R.string.title_new_pattern));
        }
        else {
            Bundle bundle = new Bundle();
            bundle.putLong(PatternDetailsFragment.ARG_PATTERN_ID, id);
            navController.navigate(R.id.action_patternListFragment_to_patternDetailsFragment,
                    bundle);
            b.toolbarLayout.setTitle(getString(R.string.title_edit_pattern));
        }
    }
    @Override
    public void onSavePattern() {
        final ViewModelStoreOwner viewModelStoreOwner =
                navController.getViewModelStoreOwner(R.id.pattern_list_navigation);
        PatternDetailsViewModel model =
                new ViewModelProvider(viewModelStoreOwner).get(PatternDetailsViewModel.class);
        Logger.debug("flow", "PatternsActivity.onSavePattern(): model=" + model);
        model.onSavePattern();
        navController.navigateUp();
    }
    public NavController getNavController() {
        return navController;
    }
}