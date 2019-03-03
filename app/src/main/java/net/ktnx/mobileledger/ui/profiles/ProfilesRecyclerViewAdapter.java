/*
 * Copyright © 2019 Damyan Ivanov.
 *  This file is part of MoLe.
 *  MoLe is free software: you can distribute it and/or modify it
 *  under the term of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your opinion), any later version.
 *
 *  MoLe is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License terms for details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Mobile-Ledger. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ktnx.mobileledger.ui.profiles;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
import net.ktnx.mobileledger.ui.activity.ProfileDetailActivity;
import net.ktnx.mobileledger.utils.Colors;
import net.ktnx.mobileledger.utils.ObservableValue;

import java.util.Collections;
import java.util.Observer;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class ProfilesRecyclerViewAdapter
        extends RecyclerView.Adapter<ProfilesRecyclerViewAdapter.ProfileListViewHolder> {
    private final View.OnClickListener mOnClickListener = view -> {
        MobileLedgerProfile profile = (MobileLedgerProfile) ((View) view.getParent()).getTag();
        editProfile(view, profile);
    };
    private ObservableValue<Boolean> editingProfiles = new ObservableValue<>(false);
    public void addEditingProfilesObserver(Observer o) {
        editingProfiles.addObserver(o);
    }
    public void deleteEditingProfilesObserver(Observer o) {
        editingProfiles.deleteObserver(o);
    }
    private RecyclerView recyclerView;
    private ItemTouchHelper rearrangeHelper;
    public ProfilesRecyclerViewAdapter() {
        Data.profiles.addObserver((o, arg) -> {
            Log.d("profiles", "profile list changed");
            if (arg == null) notifyDataSetChanged();
            else notifyItemChanged((int) arg);
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
                Collections.swap(Data.profiles.getList(), viewHolder.getAdapterPosition(),
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
        if (editingProfiles.get()) rearrangeHelper.attachToRecyclerView(recyclerView);
    }
    public void startEditingProfiles() {
        if (editingProfiles.get()) return;
        this.editingProfiles.set(true);
        notifyDataSetChanged();
        rearrangeHelper.attachToRecyclerView(recyclerView);
    }
    public void stopEditingProfiles() {
        if (!editingProfiles.get()) return;
        this.editingProfiles.set(false);
        notifyDataSetChanged();
        rearrangeHelper.attachToRecyclerView(null);
    }
    public void flipEditingProfiles() {
        if (editingProfiles.get()) stopEditingProfiles();
        else startEditingProfiles();
    }
    private void editProfile(View view, MobileLedgerProfile profile) {
        int index = Data.profiles.indexOf(profile);
        Context context = view.getContext();
        Intent intent = new Intent(context, ProfileDetailActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        if (index != -1) intent.putExtra(ProfileDetailFragment.ARG_ITEM_ID, index);

        context.startActivity(intent);
    }
    @NonNull
    @Override
    public ProfileListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.profile_list_content, parent, false);
        ProfileListViewHolder holder = new ProfileListViewHolder(view);

        holder.mTitle.setOnClickListener(v -> {
            View row = (View) v.getParent();
            MobileLedgerProfile profile = (MobileLedgerProfile) row.getTag();
            if (profile == null)
                throw new IllegalStateException("Profile row without associated profile");
            Log.d("profiles", "Setting profile to " + profile.getName());
            Data.setCurrentProfile(profile);
        });
        holder.mTitle.setOnLongClickListener(v -> {
            flipEditingProfiles();
            return true;
        });
        Data.profile.addObserver((o, arg) -> {
            MobileLedgerProfile myProfile = (MobileLedgerProfile) holder.itemView.getTag();
            final MobileLedgerProfile currentProfile = Data.profile.get();
            final boolean sameProfile = currentProfile.equals(myProfile);
            view.setAlpha(sameProfile ? 1 : 0.5f);
        });

        View.OnTouchListener dragStarter = (v, event) -> {
            if (rearrangeHelper != null && editingProfiles.get()) {
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
        final MobileLedgerProfile profile = Data.profiles.get(position);
        final MobileLedgerProfile currentProfile = Data.profile.get();
        Log.d("profiles", String.format("pos %d: %s, current: %s", position, profile.getUuid(),
                (currentProfile == null) ? "<NULL>" : currentProfile.getUuid()));
        holder.itemView.setTag(profile);

        int hue = profile.getThemeId();
        if (hue == -1) holder.mColorTag
                .setBackgroundColor(Colors.getPrimaryColorForHue(Colors.DEFAULT_HUE_DEG));
        else holder.mColorTag.setBackgroundColor(Colors.getPrimaryColorForHue(hue));

        holder.mTitle.setText(profile.getName());
//            holder.mSubTitle.setText(profile.getUrl());

        holder.mEditButton.setOnClickListener(mOnClickListener);

        final boolean sameProfile = currentProfile.equals(profile);
        holder.itemView.setAlpha(sameProfile ? 1 : 0.5f);
        holder.itemView
                .setBackground(sameProfile ? new ColorDrawable(Colors.tableRowDarkBG) : null);
        if (editingProfiles.get()) {
            holder.mRearrangeHandle.setVisibility(View.VISIBLE);
            holder.mEditButton.setVisibility(View.VISIBLE);
        }
        else {
            holder.mRearrangeHandle.setVisibility(View.INVISIBLE);
            holder.mEditButton.setVisibility(View.GONE);
        }
    }
    @Override
    public int getItemCount() {
        return Data.profiles.size();
    }
    public boolean isEditingProfiles() {
        return editingProfiles.get();
    }
    class ProfileListViewHolder extends RecyclerView.ViewHolder {
        final TextView mEditButton;
        final TextView mTitle, mColorTag;
        final LinearLayout tagAndHandleLayout;
        final ImageView mRearrangeHandle;

        ProfileListViewHolder(View view) {
            super(view);
            mEditButton = view.findViewById(R.id.profile_list_edit_button);
            mTitle = view.findViewById(R.id.title);
            mColorTag = view.findViewById(R.id.colorTag);
            mRearrangeHandle = view.findViewById(R.id.profile_list_rearrange_handle);
            tagAndHandleLayout = view.findViewById(R.id.handle_and_tag);
        }
    }
}
