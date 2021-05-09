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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.dao.TemplateHeaderDAO;
import net.ktnx.mobileledger.databinding.FragmentTemplateListBinding;
import net.ktnx.mobileledger.db.DB;
import net.ktnx.mobileledger.db.TemplateHeader;
import net.ktnx.mobileledger.ui.FabManager;
import net.ktnx.mobileledger.ui.HelpDialog;
import net.ktnx.mobileledger.utils.Logger;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link TemplateListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TemplateListFragment extends Fragment {
    private FragmentTemplateListBinding b;
    private OnTemplateListFragmentInteractionListener mListener;
    public TemplateListFragment() {
        // Required empty public constructor
    }
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment TemplateListFragment.
     */
    public static TemplateListFragment newInstance() {
        TemplateListFragment fragment = new TemplateListFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.template_list_menu, menu);
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_item_template_list_help) {
            HelpDialog.show(requireContext(), R.string.template_list_help_title,
                    R.array.template_list_help_text);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Logger.debug("flow", "PatternListFragment.onCreateView()");
        b = FragmentTemplateListBinding.inflate(inflater);
        FragmentActivity activity = requireActivity();

        if (activity instanceof FabManager.FabHandler)
            FabManager.handle((FabManager.FabHandler) activity, b.templateList);

        TemplatesRecyclerViewAdapter modelAdapter = new TemplatesRecyclerViewAdapter();

        b.templateList.setAdapter(modelAdapter);
        TemplateHeaderDAO pDao = DB.get()
                                   .getTemplateDAO();
        LiveData<List<TemplateHeader>> templates = pDao.getTemplates();
        templates.observe(getViewLifecycleOwner(), modelAdapter::setTemplates);
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        llm.setOrientation(RecyclerView.VERTICAL);
        b.templateList.setLayoutManager(llm);
        DividerItemDecoration did =
                new TemplateListDivider(activity, DividerItemDecoration.VERTICAL);
        b.templateList.addItemDecoration(did);

        return b.getRoot();
    }
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnTemplateListFragmentInteractionListener) {
            mListener = (OnTemplateListFragmentInteractionListener) context;
        }
        else {
            throw new RuntimeException(
                    context.toString() + " must implement OnFragmentInteractionListener");
        }

        final LifecycleEventObserver observer = new LifecycleEventObserver() {
            @Override
            public void onStateChanged(@NonNull LifecycleOwner source,
                                       @NonNull Lifecycle.Event event) {
                if (event.getTargetState() == Lifecycle.State.CREATED) {
//                    getActivity().setActionBar(b.toolbar);
                    getLifecycle().removeObserver(this);
                }
            }
        };
        getLifecycle().addObserver(observer);
    }
    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnTemplateListFragmentInteractionListener {
        void onSaveTemplate();

        void onEditTemplate(Long id);

        void onDuplicateTemplate(long id);
    }
}