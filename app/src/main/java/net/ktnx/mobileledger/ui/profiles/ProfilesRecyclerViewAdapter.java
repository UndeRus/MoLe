/*
 * Copyright © 2019 Damyan Ivanov.
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

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
import net.ktnx.mobileledger.ui.activity.ProfileDetailActivity;
import net.ktnx.mobileledger.utils.Colors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import static net.ktnx.mobileledger.utils.Logger.debug;

public class ProfilesRecyclerViewAdapter
        extends RecyclerView.Adapter<ProfilesRecyclerViewAdapter.ProfileListViewHolder> {
    private final View.OnClickListener mOnClickListener = view -> {
        MobileLedgerProfile profile = (MobileLedgerProfile) ((View) view.getParent()).getTag();
        editProfile(view, profile);
    };
    public MutableLiveData<Boolean> editingProfiles = new MutableLiveData<>(false);
    private RecyclerView recyclerView;
    private ItemTouchHelper rearrangeHelper;
    public ProfilesRecyclerViewAdapter() {
        debug("flow", "ProfilesRecyclerViewAdapter.new()");

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
                final ArrayList<MobileLedgerProfile> profiles = Data.profiles.getValue();
                assert profiles != null;
                Collections.swap(profiles, viewHolder.getAdapterPosition(),
                        target.getAdapterPosition());
                MobileLedgerProfile.storeProfilesOrder();
                notifyItemMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                return true;
            }
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
            }
        };
        rearrangeHelper = new ItemTouchHelper(cb);
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
        if (editingProfiles.getValue()) rearrangeHelper.attachToRecyclerView(recyclerView);
    }
    public void startEditingProfiles() {
        if (editingProfiles.getValue()) return;
        this.editingProfiles.setValue(true);
        notifyDataSetChanged();
        rearrangeHelper.attachToRecyclerView(recyclerView);
    }
    public void stopEditingProfiles() {
        if (!editingProfiles.getValue()) return;
        this.editingProfiles.setValue(false);
        notifyDataSetChanged();
        rearrangeHelper.attachToRecyclerView(null);
    }
    public void flipEditingProfiles() {
        if (editingProfiles.getValue()) stopEditingProfiles();
        else startEditingProfiles();
    }
    private void editProfile(View view, MobileLedgerProfile profile) {
        int index = Data.getProfileIndex(profile);
        Context context = view.getContext();
        Intent intent = new Intent(context, ProfileDetailActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        if (index != -1) intent.putExtra(ProfileDetailFragment.ARG_ITEM_ID, index);

        context.startActivity(intent);
    }
    private void onProfileRowClicked(View v) {
        if (editingProfiles.getValue()) return;
        MobileLedgerProfile profile = (MobileLedgerProfile) v.getTag();
        if (profile == null)
            throw new IllegalStateException("Profile row without associated profile");
        debug("profiles", "Setting profile to " + profile.getName());
        Data.setCurrentProfile(profile);
    }
    @NonNull
    @Override
    public ProfileListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.profile_list_content, parent, false);
        ProfileListViewHolder holder = new ProfileListViewHolder(view);

        holder.mRow.setOnClickListener(this::onProfileRowClicked);
        holder.mTitle.setOnClickListener(v -> {
            View row = (View) v.getParent();
            onProfileRowClicked(row);
        });
        holder.mColorTag.setOnClickListener(v -> {
            View row = (View) v.getParent().getParent();
            onProfileRowClicked(row);
        });
        holder.mTitle.setOnLongClickListener(v -> {
            flipEditingProfiles();
            return true;
        });

        View.OnTouchListener dragStarter = (v, event) -> {
            if (rearrangeHelper != null && editingProfiles.getValue()) {
                rearrangeHelper.startDrag(holder);
                return true;
            }
            return false;
        };

        holder.tagAndHandleLayout.setOnTouchListener(dragStarter);
        return holder;
    }
    @Override
    public void onBindViewHolder(@NonNull final ProfileListViewHolder holder, int position) {
        final ArrayList<MobileLedgerProfile> profiles = Data.profiles.getValue();
        assert profiles != null;
        final MobileLedgerProfile profile = profiles.get(position);
        final MobileLedgerProfile currentProfile = Data.profile.getValue();
        debug("profiles", String.format(Locale.ENGLISH,"pos %d: %s, current: %s", position, profile.getUuid(),
                (currentProfile == null) ? "<NULL>" : currentProfile.getUuid()));
        holder.itemView.setTag(profile);

        int hue = profile.getThemeId();
        if (hue == -1) holder.mColorTag
                .setBackgroundColor(Colors.getPrimaryColorForHue(Colors.DEFAULT_HUE_DEG));
        else holder.mColorTag.setBackgroundColor(Colors.getPrimaryColorForHue(hue));

        holder.mTitle.setText(profile.getName());
//            holder.mSubTitle.setText(profile.getUrl());

        holder.mEditButton.setOnClickListener(mOnClickListener);

        final boolean sameProfile = (currentProfile != null) && currentProfile.equals(profile);
        holder.itemView
                .setBackground(sameProfile ? new ColorDrawable(Colors.tableRowDarkBG) : null);
        if (editingProfiles.getValue()) {
            boolean wasHidden = holder.mEditButton.getVisibility() == View.GONE;
            holder.mRearrangeHandle.setVisibility(View.VISIBLE);
            holder.mEditButton.setVisibility(View.VISIBLE);
            if (wasHidden) {
                Animation a = AnimationUtils
                        .loadAnimation(holder.mRearrangeHandle.getContext(), R.anim.fade_in);
                holder.mRearrangeHandle.startAnimation(a);
                holder.mEditButton.startAnimation(a);
            }
        }
        else {
            boolean wasShown = holder.mEditButton.getVisibility() == View.VISIBLE;
            holder.mRearrangeHandle.setVisibility(View.INVISIBLE);
            holder.mEditButton.setVisibility(View.GONE);
            if (wasShown) {
                Animation a = AnimationUtils
                        .loadAnimation(holder.mRearrangeHandle.getContext(), R.anim.fade_out);
                holder.mRearrangeHandle.startAnimation(a);
                holder.mEditButton.startAnimation(a);
            }
        }
    }
    @Override
    public int getItemCount() {
        final ArrayList<MobileLedgerProfile> profiles = Data.profiles.getValue();
        return profiles != null ? profiles.size() : 0;
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
        }
    }
}
