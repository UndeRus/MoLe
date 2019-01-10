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

package net.ktnx.mobileledger.ui.profiles;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
import net.ktnx.mobileledger.ui.activity.ProfileListActivity;

/**
 * A fragment representing a single Profile detail screen.
 * This fragment is either contained in a {@link ProfileListActivity}
 * in two-pane mode (on tablets) or a {@link ProfileDetailActivity}
 * on handsets.
 */
public class ProfileDetailFragment extends Fragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";

    /**
     * The dummy content this fragment is presenting.
     */
    private MobileLedgerProfile mProfile;
    private TextView url;
    private LinearLayout authParams;
    private Switch useAuthentication;
    private TextView userName;
    private TextView password;
    private FloatingActionButton fab;
    private TextView profileName;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ProfileDetailFragment() {
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.d("profiles", "[fragment] Creating profile details options menu");
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.profile_details, menu);
        final MenuItem menuDeleteProfile = menu.findItem(R.id.menuDelete);
        menuDeleteProfile.setOnMenuItemClickListener(item -> {
            Log.d("profiles", String.format("[fragment] removing profile %s", mProfile.getUuid()));
            mProfile.removeFromDB();
            Data.profiles.remove(mProfile);
            if (Data.profile.get().getUuid().equals(mProfile.getUuid())) {
                Data.profile.set(Data.profiles.get(0));
            }
            return false;
        });
        menuDeleteProfile.setVisible((mProfile != null) && (Data.profiles.size() > 1));
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if ((getArguments() != null) && getArguments().containsKey(ARG_ITEM_ID)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            String uuid = getArguments().getString(ARG_ITEM_ID);
            if (uuid != null) mProfile =
                    MobileLedgerProfile.loadUUIDFromDB(getArguments().getString(ARG_ITEM_ID));

            Activity activity = this.getActivity();
            if (activity == null) throw new AssertionError();
            CollapsingToolbarLayout appBarLayout = activity.findViewById(R.id.toolbar_layout);
            if (appBarLayout != null) {
                if (mProfile != null) appBarLayout.setTitle(mProfile.getName());
                else appBarLayout.setTitle(getResources().getString(R.string.new_profile_title));
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        fab = ((Activity) context).findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            if (mProfile != null) {
                mProfile.setName(profileName.getText());
                mProfile.setUrl(url.getText());
                mProfile.setAuthEnabled(useAuthentication.isChecked());
                mProfile.setAuthUserName(userName.getText());
                mProfile.setAuthPassword(password.getText());
                mProfile.storeInDB();


                if (mProfile.getUuid().equals(Data.profile.get().getUuid())) {
                    Data.profile.set(mProfile);
                }
            }
            else {
                mProfile = new MobileLedgerProfile(profileName.getText(), url.getText(),
                        useAuthentication.isChecked(), userName.getText(), password.getText());
                mProfile.storeInDB();
                Data.profiles.add(mProfile);
                MobileLedgerProfile.storeProfilesOrder();
            }

            Activity activity = getActivity();
            if (activity != null) activity.finish();
        });
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.profile_detail, container, false);

        profileName = rootView.findViewById(R.id.profile_name);
        url = rootView.findViewById(R.id.url);
        authParams = rootView.findViewById(R.id.auth_params);
        useAuthentication = rootView.findViewById(R.id.enable_http_auth);
        userName = rootView.findViewById(R.id.auth_user_name);
        password = rootView.findViewById(R.id.password);

        useAuthentication.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d("profiles", isChecked ? "auth enabled " : "auth disabled");
            authParams.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        if (mProfile != null) {
            profileName.setText(mProfile.getName());
            url.setText(mProfile.getUrl());
            useAuthentication.setChecked(mProfile.isAuthEnabled());
            authParams.setVisibility(mProfile.isAuthEnabled() ? View.VISIBLE : View.GONE);
            userName.setText(mProfile.isAuthEnabled() ? mProfile.getAuthUserName() : "");
            password.setText(mProfile.isAuthEnabled() ? mProfile.getAuthPassword() : "");
        }
        else {
            profileName.setText("");
            url.setText("");
            useAuthentication.setChecked(false);
            authParams.setVisibility(View.GONE);
            userName.setText("");
            password.setText("");
        }

        return rootView;
    }
}
