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
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.databinding.ActivityPatternsBinding;
import net.ktnx.mobileledger.ui.activity.CrashReportingActivity;

public class PatternsActivity extends CrashReportingActivity {

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.pattern_list_menu, menu);

        return true;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityPatternsBinding b = ActivityPatternsBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setSupportActionBar(b.toolbar);
        b.toolbarLayout.setTitle(getTitle());

        b.fab.setOnClickListener(this::fabClicked);

        PatternsRecyclerViewAdapter modelAdapter = new PatternsRecyclerViewAdapter();

        b.patternList.setAdapter(modelAdapter);
        PatternsModel.retrievePatterns(modelAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(RecyclerView.VERTICAL);
        b.patternList.setLayoutManager(llm);
    }
    private void fabClicked(View view) {
        Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_INDEFINITE)
                .setAction("Action", null)
                .show();
    }
}