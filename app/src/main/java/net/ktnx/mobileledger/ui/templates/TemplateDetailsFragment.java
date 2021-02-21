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

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.navigation.NavController;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.databinding.TemplateDetailsFragmentBinding;
import net.ktnx.mobileledger.utils.Logger;

public class TemplateDetailsFragment extends Fragment {
    static final String ARG_TEMPLATE_ID = "pattern-id";
    private static final String ARG_COLUMN_COUNT = "column-count";
    private TemplateDetailsFragmentBinding b;
    private TemplateDetailsViewModel mViewModel;
    private int mColumnCount = 1;
    private Long mPatternId;
    private InteractionListener interactionListener;
    public TemplateDetailsFragment() {
    }
    public static TemplateDetailsFragment newInstance(int columnCount, int patternId) {
        final TemplateDetailsFragment fragment = new TemplateDetailsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        if (patternId > 0)
            args.putInt(ARG_TEMPLATE_ID, patternId);
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.template_details_menu, menu);
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.delete_template) {
            signalDeleteTemplateInteraction();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    private void signalDeleteTemplateInteraction() {
        if (interactionListener != null)
            interactionListener.onDeleteTemplate(mPatternId);
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle args = getArguments();
        if (args != null) {
            mColumnCount = args.getInt(ARG_COLUMN_COUNT, 1);
            mPatternId = args.getLong(ARG_TEMPLATE_ID, -1);
            if (mPatternId == -1)
                mPatternId = null;
        }

        setHasOptionsMenu(mPatternId != null);
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        if (!(getActivity() instanceof InteractionListener))
            throw new IllegalStateException(
                    "Containing activity must implement TemplateDetailsFragment" +
                    ".InteractionListener");
        interactionListener = (InteractionListener) getActivity();

        NavController controller = ((TemplatesActivity) requireActivity()).getNavController();
        final ViewModelStoreOwner viewModelStoreOwner =
                controller.getViewModelStoreOwner(R.id.template_list_navigation);
        mViewModel = new ViewModelProvider(viewModelStoreOwner).get(TemplateDetailsViewModel.class);
        mViewModel.setDefaultPatternName(getString(R.string.unnamed_template));
        Logger.debug("flow", "PatternDetailsFragment.onCreateView(): model=" + mViewModel);

        b = TemplateDetailsFragmentBinding.inflate(inflater);
        Context context = b.patternDetailsRecyclerView.getContext();
        if (mColumnCount <= 1) {
            b.patternDetailsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        }
        else {
            b.patternDetailsRecyclerView.setLayoutManager(
                    new GridLayoutManager(context, mColumnCount));
        }


        TemplateDetailsAdapter adapter = new TemplateDetailsAdapter(mViewModel);
        b.patternDetailsRecyclerView.setAdapter(adapter);
        mViewModel.getItems(mPatternId)
                  .observe(getViewLifecycleOwner(), adapter::setItems);

        return b.getRoot();
    }
    interface InteractionListener {
        void onDeleteTemplate(@NonNull Long templateId);
    }
}