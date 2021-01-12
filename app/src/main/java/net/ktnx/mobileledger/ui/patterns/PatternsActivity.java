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

import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.databinding.ActivityPatternsBinding;
import net.ktnx.mobileledger.ui.activity.CrashReportingActivity;

import java.util.Objects;

public class PatternsActivity extends CrashReportingActivity
        implements PatternListFragment.OnPatternListFragmentInteractionListener {
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
        setContentView(b.fragmentContainer);

        NavHostFragment navHostFragment = (NavHostFragment) Objects.requireNonNull(
                getSupportFragmentManager().findFragmentById(R.id.fragment_container));
        navController = navHostFragment.getNavController();
    }
    @Override
    public void onNewPattern() {
//        navController.navigate
        final Snackbar snackbar =
                Snackbar.make(b.fragmentContainer, "New pattern action coming up soon",
                        Snackbar.LENGTH_INDEFINITE);
//        snackbar.setAction("Action", v -> snackbar.dismiss());
        snackbar.show();
    }
    @Override
    public void onEditPattern(int id) {
        final Snackbar snackbar =
                Snackbar.make(b.fragmentContainer, "One Edit pattern action coming up soon",
                        Snackbar.LENGTH_INDEFINITE);
//        snackbar.setAction("Action", v -> snackbar.dismiss());
        snackbar.show();
    }
}