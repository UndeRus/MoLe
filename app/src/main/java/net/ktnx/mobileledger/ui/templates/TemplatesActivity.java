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

package net.ktnx.mobileledger.ui.templates;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.dao.TemplateHeaderDAO;
import net.ktnx.mobileledger.databinding.ActivityTemplatesBinding;
import net.ktnx.mobileledger.db.DB;
import net.ktnx.mobileledger.db.TemplateWithAccounts;
import net.ktnx.mobileledger.ui.activity.CrashReportingActivity;
import net.ktnx.mobileledger.utils.Logger;

import java.util.Objects;

public class TemplatesActivity extends CrashReportingActivity
        implements TemplateListFragment.OnTemplateListFragmentInteractionListener,
        TemplateDetailsFragment.InteractionListener {
    public static final String ARG_ADD_TEMPLATE = "add-template";
    private ActivityTemplatesBinding b;
    private NavController navController;
    //    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        super.onCreateOptionsMenu(menu);
//        getMenuInflater().inflate(R.menu.template_list_menu, menu);
//
//        return true;
//    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityTemplatesBinding.inflate(getLayoutInflater());
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
            if (destination.getId() == R.id.templateListFragment) {
                b.fabAdd.show();
                b.fabSave.hide();
                b.toolbarLayout.setTitle(getString(R.string.title_activity_templates));
            }
            if (destination.getId() == R.id.templateDetailsFragment) {
                b.fabAdd.hide();
                b.fabSave.show();
            }
        });

        b.toolbarLayout.setTitle(getString(R.string.title_activity_templates));

        b.fabAdd.setOnClickListener(v -> onEditTemplate(null));
        b.fabSave.setOnClickListener(v -> onSaveTemplate());
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            final NavDestination currentDestination = navController.getCurrentDestination();
            if (currentDestination != null &&
                currentDestination.getId() == R.id.templateDetailsFragment)
                navController.popBackStack();
            else
                finish();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onDuplicateTemplate(long id) {
        DB.get()
          .getTemplateDAO()
          .duplicateTemplateWitAccounts(id, null);
    }
    @Override
    public void onEditTemplate(Long id) {
        if (id == null) {
            navController.navigate(R.id.action_templateListFragment_to_templateDetailsFragment);
            b.toolbarLayout.setTitle(getString(R.string.title_new_template));
        }
        else {
            Bundle bundle = new Bundle();
            bundle.putLong(TemplateDetailsFragment.ARG_TEMPLATE_ID, id);
            navController.navigate(R.id.action_templateListFragment_to_templateDetailsFragment,
                    bundle);
            b.toolbarLayout.setTitle(getString(R.string.title_edit_template));
        }
    }
    @Override
    public void onSaveTemplate() {
        final ViewModelStoreOwner viewModelStoreOwner =
                navController.getViewModelStoreOwner(R.id.template_list_navigation);
        TemplateDetailsViewModel model =
                new ViewModelProvider(viewModelStoreOwner).get(TemplateDetailsViewModel.class);
        Logger.debug("flow", "TemplatesActivity.onSavePattern(): model=" + model);
        model.onSaveTemplate();
        navController.navigateUp();
    }
    public NavController getNavController() {
        return navController;
    }
    @Override
    public void onDeleteTemplate(@NonNull Long templateId) {
        Objects.requireNonNull(templateId);
        TemplateHeaderDAO dao = DB.get()
                                  .getTemplateDAO();

        dao.getTemplateWitAccountsAsync(templateId, template -> {
            TemplateWithAccounts copy = TemplateWithAccounts.from(template);
            dao.deleteAsync(template.header, () -> {
                navController.popBackStack(R.id.templateListFragment, false);

                Snackbar.make(b.getRoot(), String.format(
                        TemplatesActivity.this.getString(R.string.template_xxx_deleted),
                        template.header.getName()), BaseTransientBottomBar.LENGTH_LONG)
                        .setAction(R.string.action_undo, v -> dao.insertAsync(copy, null))
                        .show();
            });
        });
    }
}