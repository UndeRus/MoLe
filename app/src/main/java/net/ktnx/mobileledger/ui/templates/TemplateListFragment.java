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
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.ktnx.mobileledger.dao.PatternHeaderDAO;
import net.ktnx.mobileledger.databinding.FragmentTemplateListBinding;
import net.ktnx.mobileledger.db.DB;
import net.ktnx.mobileledger.db.TemplateHeader;
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
    // TODO: Rename and change types and number of parameters
    public static TemplateListFragment newInstance() {
        TemplateListFragment fragment = new TemplateListFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        if (getArguments() != null) {
//            mParam1 = getArguments().getString(ARG_PARAM1);
//            mParam2 = getArguments().getString(ARG_PARAM2);
//        }
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Logger.debug("flow", "PatternListFragment.onCreateView()");
        b = FragmentTemplateListBinding.inflate(inflater);

        TemplatesRecyclerViewAdapter modelAdapter = new TemplatesRecyclerViewAdapter();

        b.templateList.setAdapter(modelAdapter);
        PatternHeaderDAO pDao = DB.get()
                                  .getPatternDAO();
        LiveData<List<TemplateHeader>> templates = pDao.getPatterns();
        templates.observe(getViewLifecycleOwner(), modelAdapter::setTemplates);
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        llm.setOrientation(RecyclerView.VERTICAL);
        b.templateList.setLayoutManager(llm);
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
    }
}