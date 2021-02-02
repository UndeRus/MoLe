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

package net.ktnx.mobileledger.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.databinding.FragmentTemplateDetailSourceSelectorListBinding;
import net.ktnx.mobileledger.model.TemplateDetailSource;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.Misc;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnSourceSelectedListener}
 * interface.
 */
public class TemplateDetailSourceSelectorFragment extends AppCompatDialogFragment
        implements OnSourceSelectedListener {

    public static final int DEFAULT_COLUMN_COUNT = 1;
    public static final String ARG_COLUMN_COUNT = "column-count";
    public static final String ARG_PATTERN = "pattern";
    public static final String ARG_TEST_TEXT = "test-text";
    private int mColumnCount = DEFAULT_COLUMN_COUNT;
    private ArrayList<TemplateDetailSource> mSources;
    private TemplateDetailSourceSelectorModel model;
    private OnSourceSelectedListener onSourceSelectedListener;
    private @StringRes
    int mPatternProblem;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public TemplateDetailSourceSelectorFragment() {
    }
    @SuppressWarnings("unused")
    public static TemplateDetailSourceSelectorFragment newInstance() {
        return newInstance(DEFAULT_COLUMN_COUNT, null, null);
    }
    public static TemplateDetailSourceSelectorFragment newInstance(int columnCount,
                                                                   @Nullable String pattern,
                                                                   @Nullable String testText) {
        TemplateDetailSourceSelectorFragment fragment = new TemplateDetailSourceSelectorFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        if (pattern != null)
            args.putString(ARG_PATTERN, pattern);
        if (testText != null)
            args.putString(ARG_TEST_TEXT, testText);
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT, DEFAULT_COLUMN_COUNT);
            final String patternText = getArguments().getString(ARG_PATTERN);
            final String testText = getArguments().getString(ARG_TEST_TEXT);
            if (Misc.emptyIsNull(patternText) == null) {
                mPatternProblem = R.string.missing_pattern_error;
            }
            else {
                if (Misc.emptyIsNull(testText) == null) {
                    mPatternProblem = R.string.missing_test_text;
                }
                else {
                    Pattern pattern = Pattern.compile(patternText);
                    Matcher matcher = pattern.matcher(testText);
                    Logger.debug("templates",
                            String.format("Trying to match pattern '%s' against text '%s'",
                                    patternText, testText));
                    if (matcher.matches()) {
                        if (matcher.groupCount() >= 0) {
                            ArrayList<TemplateDetailSource> list = new ArrayList<>();
                            for (short g = 1; g <= matcher.groupCount(); g++) {
                                list.add(new TemplateDetailSource(g, matcher.group(g)));
                            }
                            mSources = list;
                        }
                        else {
                            mPatternProblem = R.string.pattern_without_groups;
                        }
                    }
                    else {
                        mPatternProblem = R.string.pattern_does_not_match;
                    }
                }
            }
        }
    }
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Context context = requireContext();
        Dialog csd = new Dialog(context);
        FragmentTemplateDetailSourceSelectorListBinding b =
                FragmentTemplateDetailSourceSelectorListBinding.inflate(
                        LayoutInflater.from(context));
        csd.setContentView(b.getRoot());
        csd.setTitle(R.string.choose_template_detail_source_label);

        if (mSources != null && !mSources.isEmpty()) {
            RecyclerView recyclerView = b.list;

            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            }
            else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }
            model = new ViewModelProvider(this).get(TemplateDetailSourceSelectorModel.class);
            if (onSourceSelectedListener != null)
                model.setOnSourceSelectedListener(onSourceSelectedListener);
            model.setSourcesList(mSources);

            TemplateDetailSourceSelectorRecyclerViewAdapter adapter =
                    new TemplateDetailSourceSelectorRecyclerViewAdapter();
            model.groups.observe(this, adapter::submitList);

            recyclerView.setAdapter(adapter);
            adapter.setSourceSelectedListener(this);
        }
        else {
            b.list.setVisibility(View.GONE);
            b.templateError.setText(mPatternProblem);
            b.templateError.setVisibility(View.VISIBLE);
        }

        b.literalButton.setOnClickListener(v -> onSourceSelected(true, (short) -1));

        return csd;
    }
    public void setOnSourceSelectedListener(OnSourceSelectedListener listener) {
        onSourceSelectedListener = listener;

        if (model != null)
            model.setOnSourceSelectedListener(listener);
    }
    public void resetOnSourceSelectedListener() {
        model.resetOnSourceSelectedListener();
    }
    @Override
    public void onSourceSelected(boolean literal, short group) {
        if (model != null)
            model.triggerOnSourceSelectedListener(literal, group);
        if (onSourceSelectedListener != null)
            onSourceSelectedListener.onSourceSelected(literal, group);

        dismiss();
    }
}