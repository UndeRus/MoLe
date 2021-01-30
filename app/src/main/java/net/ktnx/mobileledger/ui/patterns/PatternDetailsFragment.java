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

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.navigation.NavController;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.databinding.PatternDetailsFragmentBinding;
import net.ktnx.mobileledger.ui.QRScanAbleFragment;
import net.ktnx.mobileledger.utils.Logger;

public class PatternDetailsFragment extends QRScanAbleFragment {
    static final String ARG_PATTERN_ID = "pattern-id";
    private static final String ARG_COLUMN_COUNT = "column-count";
    PatternDetailsFragmentBinding b;
    private PatternDetailsViewModel mViewModel;
    private int mColumnCount = 1;
    private Long mPatternId;
    public PatternDetailsFragment() {
    }
    public static PatternDetailsFragment newInstance(int columnCount, int patternId) {
        final PatternDetailsFragment fragment = new PatternDetailsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        if (patternId > 0)
            args.putInt(ARG_PATTERN_ID, patternId);
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle args = getArguments();
        if (args != null) {
            mColumnCount = args.getInt(ARG_COLUMN_COUNT, 1);
            mPatternId = args.getLong(ARG_PATTERN_ID, -1);
            if (mPatternId == -1)
                mPatternId = null;
        }
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = PatternDetailsFragmentBinding.inflate(inflater);
        Context context = b.patternDetailsRecyclerView.getContext();
        if (mColumnCount <= 1) {
            b.patternDetailsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        }
        else {
            b.patternDetailsRecyclerView.setLayoutManager(
                    new GridLayoutManager(context, mColumnCount));
        }


        PatternDetailsAdapter adapter = new PatternDetailsAdapter();
        b.patternDetailsRecyclerView.setAdapter(adapter);
        mViewModel.getItems(mPatternId)
                  .observe(getViewLifecycleOwner(), adapter::setItems);
        return b.getRoot();
    }
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        NavController controller = ((PatternsActivity) context).getNavController();
        final ViewModelStoreOwner viewModelStoreOwner =
                controller.getViewModelStoreOwner(R.id.pattern_list_navigation);
        mViewModel = new ViewModelProvider(viewModelStoreOwner).get(PatternDetailsViewModel.class);
        mViewModel.setDefaultPatternName(getString(R.string.unnamed_pattern));
        Logger.debug("flow", "PatternDetailsFragment.onAttach(): model=" + mViewModel);

    }
    @Override
    protected void onQrScanned(String text) {
        Logger.debug("PatDet_fr", String.format("Got scanned text '%s'", text));
        if (text != null)
            mViewModel.setTestText(text);
    }
    public void onSavePattern() {
        mViewModel.onSavePattern();
        final Snackbar snackbar = Snackbar.make(b.getRoot(),
                "One Save pattern action coming up soon in a fragment near you",
                Snackbar.LENGTH_INDEFINITE);
//        snackbar.setAction("Action", v -> snackbar.dismiss());
        snackbar.show();
    }
}