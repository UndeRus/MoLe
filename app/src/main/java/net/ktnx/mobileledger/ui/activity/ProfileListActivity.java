/*
 * Copyright © 2019 Damyan Ivanov.
 * This file is part of Mobile-Ledger.
 * Mobile-Ledger is free software: you can distribute it and/or modify it
 * under the term of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your opinion), any later version.
 *
 * Mobile-Ledger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License terms for details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mobile-Ledger. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ktnx.mobileledger.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
import net.ktnx.mobileledger.ui.profiles.ProfileDetailActivity;
import net.ktnx.mobileledger.ui.profiles.ProfileDetailFragment;
import net.ktnx.mobileledger.utils.MLDB;

import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * An activity representing a list of Profiles. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link ProfileDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class ProfileListActivity extends AppCompatActivity {

    public static final String ARG_ACTION = "action";
    public static final String ARG_PROFILE_INDEX = "profile_uuid";
    public static final int ACTION_EDIT_PROFILE = 1;
    public static final int ACTION_INVALID = -1;
    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        RecyclerView recyclerView = findViewById(R.id.profile_list);
        if (recyclerView == null) throw new AssertionError();
        setupRecyclerView(recyclerView);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ProfilesRecyclerViewAdapter adapter =
                        (ProfilesRecyclerViewAdapter) recyclerView.getAdapter();
                if (adapter != null) adapter.editProfile(recyclerView, null);
            }
        });

        if (findViewById(R.id.profile_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }

        int action = getIntent().getIntExtra(ARG_ACTION, ACTION_INVALID);
        if (action == ACTION_EDIT_PROFILE) {
            Log.d("profiles", "got edit profile action");
            int index = getIntent().getIntExtra(ARG_PROFILE_INDEX, -1);
            if (index >= 0) {
                List<MobileLedgerProfile> list = MobileLedgerProfile.loadAllFromDB();
                if (index < list.size()) {
                    ProfilesRecyclerViewAdapter adapter =
                            (ProfilesRecyclerViewAdapter) recyclerView.getAdapter();
                    if (adapter != null) adapter.editProfile(recyclerView, list.get(index));
                }
            }
        }
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        final ProfilesRecyclerViewAdapter adapter = new ProfilesRecyclerViewAdapter(this, mTwoPane);
        recyclerView.setAdapter(adapter);
        ItemTouchHelper.Callback cb = new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView,
                                        @NonNull RecyclerView.ViewHolder viewHolder) {
                return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
            }
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                Collections.swap(Data.profiles.getList(), viewHolder.getAdapterPosition(),
                        target.getAdapterPosition());
                MobileLedgerProfile.storeProfilesOrder();
                adapter.notifyItemMoved(viewHolder.getAdapterPosition(),
                        target.getAdapterPosition());
                return true;
            }
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {

            }
        };
        new ItemTouchHelper(cb).attachToRecyclerView(recyclerView);
    }

    public static class ProfilesRecyclerViewAdapter
            extends RecyclerView.Adapter<ProfilesRecyclerViewAdapter.ProfileListViewHolder> {

        private final ProfileListActivity mParentActivity;
        private final boolean mTwoPane;
        private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MobileLedgerProfile item = (MobileLedgerProfile) ((View) view.getParent()).getTag();
                editProfile(view, item);
            }
        };
        ProfilesRecyclerViewAdapter(ProfileListActivity parent, boolean twoPane) {
            mParentActivity = parent;
            mTwoPane = twoPane;
            Data.profiles.addObserver((o, arg) -> {
                Log.d("profiles", "profile list changed");
                notifyDataSetChanged();
            });
        }
        private void editProfile(View view, MobileLedgerProfile item) {
            if (mTwoPane) {
                Bundle arguments = new Bundle();
                arguments.putString(ProfileDetailFragment.ARG_ITEM_ID, item.getUuid());
                ProfileDetailFragment fragment = new ProfileDetailFragment();
                fragment.setArguments(arguments);
                mParentActivity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.profile_detail_container, fragment).commit();
            }
            else {
                Context context = view.getContext();
                Intent intent = new Intent(context, ProfileDetailActivity.class);
                intent.putExtra(ProfileDetailFragment.ARG_ITEM_ID,
                        (item == null) ? null : item.getUuid());

                context.startActivity(intent);
            }
        }
        @NonNull
        @Override
        public ProfileListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.profile_list_content, parent, false);
            ProfileListViewHolder holder = new ProfileListViewHolder(view);
            Data.profile.addObserver(new Observer() {
                @Override
                public void update(Observable o, Object arg) {
                    MobileLedgerProfile newProfile = Data.profile.get();
                    MobileLedgerProfile profile = (MobileLedgerProfile) holder.itemView.getTag();
                    holder.mRadioView.setChecked(
                            newProfile != null && newProfile.getUuid().equals(profile.getUuid()));
                }
            });
            return holder;
        }
        @Override
        public void onBindViewHolder(@NonNull final ProfileListViewHolder holder, int position) {
            final MobileLedgerProfile profile = Data.profiles.get(position);
            final MobileLedgerProfile currentProfile = Data.profile.get();
            Log.d("profiles", String.format("pos %d: %s, current: %s", position, profile.getUuid(),
                    currentProfile.getUuid()));
            holder.mRadioView.setText(profile.getName());
            holder.mRadioView.setChecked(profile.getUuid().equals(currentProfile.getUuid()));
            holder.mRadioView
                    .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            if (!isChecked) return;
                            MLDB.set_option_value(MLDB.OPT_PROFILE_UUID, profile.getUuid());
                            Data.profile.set(profile);
                        }
                    });

            holder.itemView.setTag(profile);
            holder.mEditButton.setOnClickListener(mOnClickListener);

        }
        @Override
        public int getItemCount() {
            return Data.profiles.size();
        }
        class ProfileListViewHolder extends RecyclerView.ViewHolder {
            final RadioButton mRadioView;
            final TextView mEditButton;

            ProfileListViewHolder(View view) {
                super(view);
                mRadioView = view.findViewById(R.id.profile_list_radio);
                mEditButton = view.findViewById(R.id.profile_list_edit_button);
            }
        }
    }
}
