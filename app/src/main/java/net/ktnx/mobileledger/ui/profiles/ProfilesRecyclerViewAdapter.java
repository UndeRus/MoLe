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

package net.ktnx.mobileledger.ui.profiles;

import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.db.DB;
import net.ktnx.mobileledger.db.Profile;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.utils.Colors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.ktnx.mobileledger.utils.Logger.debug;

public class ProfilesRecyclerViewAdapter
        extends RecyclerView.Adapter<ProfilesRecyclerViewAdapter.ProfileListViewHolder> {
    public final MutableLiveData<Boolean> editingProfiles = new MutableLiveData<>(false);
    private final ItemTouchHelper rearrangeHelper;
    private final AsyncListDiffer<Profile> listDiffer;
    private RecyclerView recyclerView;
    private boolean animationsEnabled = true;

    public ProfilesRecyclerViewAdapter() {
        debug("flow", "ProfilesRecyclerViewAdapter.new()");

        setHasStableIds(true);
        listDiffer = new AsyncListDiffer<>(this, new DiffUtil.ItemCallback<Profile>() {
            @Override
            public boolean areItemsTheSame(@NonNull Profile oldItem, @NonNull Profile newItem) {
                return oldItem.getId() == newItem.getId();
            }
            @Override
            public boolean areContentsTheSame(@NonNull Profile oldItem, @NonNull Profile newItem) {
                return oldItem.equals(newItem);
            }
        });

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
                final List<Profile> profiles = new ArrayList<>(listDiffer.getCurrentList());
                Collections.swap(profiles, viewHolder.getAdapterPosition(),
                        target.getAdapterPosition());
                DB.get()
                  .getProfileDAO()
                  .updateOrder(profiles, null);
//                notifyItemMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                return true;
            }
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
            }
        };
        rearrangeHelper = new ItemTouchHelper(cb);
    }
    @Override
    public long getItemId(int position) {
        return listDiffer.getCurrentList()
                         .get(position)
                         .getId();
    }
    public void setProfileList(List<Profile> list) {
        listDiffer.submitList(list);
    }
    public void setAnimationsEnabled(boolean animationsEnabled) {
        this.animationsEnabled = animationsEnabled;
    }
    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        rearrangeHelper.attachToRecyclerView(null);
        super.onDetachedFromRecyclerView(recyclerView);
        this.recyclerView = null;
    }
    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
        if (editingProfiles())
            rearrangeHelper.attachToRecyclerView(recyclerView);
    }
    public boolean editingProfiles() {
        final Boolean b = editingProfiles.getValue();
        if (b == null)
            return false;
        return b;
    }
    public void startEditingProfiles() {
        if (editingProfiles())
            return;
        this.editingProfiles.setValue(true);
        rearrangeHelper.attachToRecyclerView(recyclerView);
    }
    public void stopEditingProfiles() {
        if (!editingProfiles())
            return;
        this.editingProfiles.setValue(false);
        rearrangeHelper.attachToRecyclerView(null);
    }
    public void flipEditingProfiles() {
        if (editingProfiles())
            stopEditingProfiles();
        else
            startEditingProfiles();
    }
    @NonNull
    @Override
    public ProfileListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                                  .inflate(R.layout.profile_list_content, parent, false);
        return new ProfileListViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull final ProfileListViewHolder holder, int position) {
        final Profile profile = listDiffer.getCurrentList()
                                          .get(position);
        final Profile currentProfile = Data.getProfile();
//        debug("profiles", String.format(Locale.ENGLISH, "pos %d: %s, current: %s", position,
//                profile.getUuid(), currentProfile.getUuid()));

        int hue = profile.getTheme();
        if (hue == -1)
            holder.mColorTag.setBackgroundColor(
                    Colors.getPrimaryColorForHue(Colors.DEFAULT_HUE_DEG));
        else
            holder.mColorTag.setBackgroundColor(Colors.getPrimaryColorForHue(hue));

        holder.mTitle.setText(profile.getName());
//            holder.mSubTitle.setText(profile.getUrl());

        holder.mEditButton.setOnClickListener(view -> {
            Profile p = listDiffer.getCurrentList()
                                  .get(holder.getAdapterPosition());
            ProfileDetailActivity.start(view.getContext(), p);
        });

        final boolean sameProfile =
                currentProfile != null && currentProfile.getId() == profile.getId();
        holder.itemView.setBackground(
                sameProfile ? new ColorDrawable(Colors.tableRowDarkBG) : null);
        if (editingProfiles()) {
            boolean wasHidden = holder.mEditButton.getVisibility() == View.GONE;
            holder.mRearrangeHandle.setVisibility(View.VISIBLE);
            holder.mEditButton.setVisibility(View.VISIBLE);
            if (wasHidden && animationsEnabled) {
                Animation a = AnimationUtils.loadAnimation(holder.mRearrangeHandle.getContext(),
                        R.anim.fade_in);
                holder.mRearrangeHandle.startAnimation(a);
                holder.mEditButton.startAnimation(a);
            }
        }
        else {
            boolean wasShown = holder.mEditButton.getVisibility() == View.VISIBLE;
            holder.mRearrangeHandle.setVisibility(View.INVISIBLE);
            holder.mEditButton.setVisibility(View.GONE);
            if (wasShown && animationsEnabled) {
                Animation a = AnimationUtils.loadAnimation(holder.mRearrangeHandle.getContext(),
                        R.anim.fade_out);
                holder.mRearrangeHandle.startAnimation(a);
                holder.mEditButton.startAnimation(a);
            }
        }
    }
    @Override
    public int getItemCount() {
        return listDiffer.getCurrentList()
                         .size();
    }
    class ProfileListViewHolder extends RecyclerView.ViewHolder {
        final TextView mEditButton;
        final TextView mTitle, mColorTag;
        final LinearLayout tagAndHandleLayout;
        final ImageView mRearrangeHandle;
        final ConstraintLayout mRow;

        ProfileListViewHolder(View view) {
            super(view);
            mEditButton = view.findViewById(R.id.profile_list_edit_button);
            mTitle = view.findViewById(R.id.title);
            mColorTag = view.findViewById(R.id.colorTag);
            mRearrangeHandle = view.findViewById(R.id.profile_list_rearrange_handle);
            tagAndHandleLayout = view.findViewById(R.id.handle_and_tag);
            mRow = (ConstraintLayout) view;


            mRow.setOnClickListener(this::onProfileRowClicked);
            mTitle.setOnClickListener(v -> {
                View row = (View) v.getParent();
                onProfileRowClicked(row);
            });
            mColorTag.setOnClickListener(v -> {
                View row = (View) v.getParent()
                                   .getParent();
                onProfileRowClicked(row);
            });
            mTitle.setOnLongClickListener(v -> {
                flipEditingProfiles();
                return true;
            });

            View.OnTouchListener dragStarter = (v, event) -> {
                if (rearrangeHelper != null && editingProfiles()) {
                    rearrangeHelper.startDrag(this);
                    return true;
                }
                return false;
            };

            tagAndHandleLayout.setOnTouchListener(dragStarter);
        }
        private void onProfileRowClicked(View v) {
            if (editingProfiles())
                return;
            Profile profile = listDiffer.getCurrentList()
                                        .get(getAdapterPosition());
            if (Data.getProfile() != profile) {
                debug("profiles", "Setting profile to " + profile.getName());
                Data.drawerOpen.setValue(false);
                Data.setCurrentProfile(profile);
            }
            else
                debug("profiles",
                        "Not setting profile to the current profile " + profile.getName());
        }

    }
}
