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

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;

import net.ktnx.mobileledger.BuildConfig;
import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.async.SendTransactionTask;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
import net.ktnx.mobileledger.ui.HueRingDialog;
import net.ktnx.mobileledger.ui.activity.ProfileDetailActivity;
import net.ktnx.mobileledger.utils.Colors;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;

import static net.ktnx.mobileledger.utils.Logger.debug;

/**
 * A fragment representing a single Profile detail screen.
 * a {@link ProfileDetailActivity}
 * on handsets.
 */
public class ProfileDetailFragment extends Fragment implements HueRingDialog.HueSelectedListener {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";
    public static final String ARG_HUE = "hue";
    @NonNls
    private static final String HTTPS_URL_START = "https://";

    /**
     * The dummy content this fragment is presenting.
     */
    private MobileLedgerProfile mProfile;
    private TextView url;
    private Switch postingPermitted;
    private Switch showCommodityByDefault;
    private TextInputLayout urlLayout;
    private LinearLayout authParams;
    private Switch useAuthentication;
    private TextView userName;
    private TextInputLayout userNameLayout;
    private TextView password;
    private TextInputLayout passwordLayout;
    private TextView profileName;
    private TextInputLayout profileNameLayout;
    private TextView preferredAccountsFilter;
    private TextInputLayout preferredAccountsFilterLayout;
    private View huePickerView;
    private View insecureWarningText;
    private TextView futureDatesText;
    private MobileLedgerProfile.FutureDates futureDates;
    private View futureDatesLayout;
    private TextView apiVersionText;
    private SendTransactionTask.API apiVersion;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ProfileDetailFragment() {
    }
    @Override
    public void onCreateOptionsMenu(@NotNull Menu menu, @NotNull MenuInflater inflater) {
        debug("profiles", "[fragment] Creating profile details options menu");
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.profile_details, menu);
        final MenuItem menuDeleteProfile = menu.findItem(R.id.menuDelete);
        menuDeleteProfile.setOnMenuItemClickListener(item -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(mProfile.getName());
            builder.setMessage(R.string.remove_profile_dialog_message);
            builder.setPositiveButton(R.string.Remove, (dialog, which) -> {
                debug("profiles",
                        String.format("[fragment] removing profile %s", mProfile.getUuid()));
                mProfile.removeFromDB();
                ArrayList<MobileLedgerProfile> oldList = Data.profiles.getValue();
                if (oldList == null)
                    throw new AssertionError();
                ArrayList<MobileLedgerProfile> newList = new ArrayList<>(oldList);
                newList.remove(mProfile);
                Data.profiles.setValue(newList);
                if (mProfile.equals(Data.profile.getValue())) {
                    debug("profiles", "[fragment] setting current profile to 0");
                    Data.setCurrentProfile(newList.get(0));
                }

                final FragmentActivity activity = getActivity();
                if (activity != null)
                    activity.finish();
            });
            builder.show();
            return false;
        });
        final ArrayList<MobileLedgerProfile> profiles = Data.profiles.getValue();
        menuDeleteProfile.setVisible(
                (mProfile != null) && (profiles != null) && (profiles.size() > 1));

        if (BuildConfig.DEBUG) {
            final MenuItem menuWipeProfileData = menu.findItem(R.id.menuWipeData);
            menuWipeProfileData.setOnMenuItemClickListener(ignored -> onWipeDataMenuClicked());
            menuWipeProfileData.setVisible(mProfile != null);
        }
    }
    private boolean onWipeDataMenuClicked() {
        // this is a development option, so no confirmation
        mProfile.wipeAllData();
        if (mProfile.equals(Data.profile.getValue()))
            triggerProfileChange();
        return true;
    }
    private void triggerProfileChange() {
        int index = Data.getProfileIndex(mProfile);
        MobileLedgerProfile newProfile = new MobileLedgerProfile(mProfile);
        final ArrayList<MobileLedgerProfile> profiles = Data.profiles.getValue();
        if (profiles == null)
            throw new AssertionError();
        profiles.set(index, newProfile);

        ProfilesRecyclerViewAdapter viewAdapter = ProfilesRecyclerViewAdapter.getInstance();
        if (viewAdapter != null)
            viewAdapter.notifyItemChanged(index);

        if (mProfile.equals(Data.profile.getValue()))
            Data.profile.setValue(newProfile);
    }
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Activity context = getActivity();
        if (context == null)
            return;

        if ((getArguments() != null) && getArguments().containsKey(ARG_ITEM_ID)) {
            int index = getArguments().getInt(ARG_ITEM_ID, -1);
            ArrayList<MobileLedgerProfile> profiles = Data.profiles.getValue();
            if ((profiles != null) && (index != -1) && (index < profiles.size()))
                mProfile = profiles.get(index);

            Activity activity = this.getActivity();
            if (activity == null)
                throw new AssertionError();
            CollapsingToolbarLayout appBarLayout = activity.findViewById(R.id.toolbar_layout);
            if (appBarLayout != null) {
                if (mProfile != null)
                    appBarLayout.setTitle(mProfile.getName());
                else
                    appBarLayout.setTitle(getResources().getString(R.string.new_profile_title));
            }
        }

        FloatingActionButton fab = context.findViewById(R.id.fab);
        fab.setOnClickListener(v -> onSaveFabClicked());
        profileName = context.findViewById(R.id.profile_name);
        profileNameLayout = context.findViewById(R.id.profile_name_layout);
        url = context.findViewById(R.id.url);
        urlLayout = context.findViewById(R.id.url_layout);
        postingPermitted = context.findViewById(R.id.profile_permit_posting);
        showCommodityByDefault = context.findViewById(R.id.profile_show_commodity);
        futureDatesLayout = context.findViewById(R.id.future_dates_layout);
        futureDatesText = context.findViewById(R.id.future_dates_text);
        context.findViewById(R.id.future_dates_layout)
               .setOnClickListener(v -> {
                   MenuInflater mi = new MenuInflater(context);
                   PopupMenu menu = new PopupMenu(context, v);
                   menu.inflate(R.menu.future_dates);
                   menu.setOnMenuItemClickListener(item -> {
                       switch (item.getItemId()) {
                           case R.id.menu_future_dates_7:
                               futureDates = MobileLedgerProfile.FutureDates.OneWeek;
                               break;
                           case R.id.menu_future_dates_14:
                               futureDates = MobileLedgerProfile.FutureDates.TwoWeeks;
                               break;
                           case R.id.menu_future_dates_30:
                               futureDates = MobileLedgerProfile.FutureDates.OneMonth;
                               break;
                           case R.id.menu_future_dates_60:
                               futureDates = MobileLedgerProfile.FutureDates.TwoMonths;
                               break;
                           case R.id.menu_future_dates_90:
                               futureDates = MobileLedgerProfile.FutureDates.ThreeMonths;
                               break;
                           case R.id.menu_future_dates_180:
                               futureDates = MobileLedgerProfile.FutureDates.SixMonths;
                               break;
                           case R.id.menu_future_dates_365:
                               futureDates = MobileLedgerProfile.FutureDates.OneYear;
                               break;
                           case R.id.menu_future_dates_all:
                               futureDates = MobileLedgerProfile.FutureDates.All;
                               break;
                           default:
                               futureDates = MobileLedgerProfile.FutureDates.None;
                       }
                       futureDatesText.setText(futureDates.getText(getResources()));
                       return true;
                   });
                   menu.show();
               });
        apiVersionText = context.findViewById(R.id.api_version_text);
        context.findViewById(R.id.api_version_layout)
               .setOnClickListener(v -> {
                   MenuInflater mi = new MenuInflater(context);
                   PopupMenu menu = new PopupMenu(context, v);
                   menu.inflate(R.menu.api_version);
                   menu.setOnMenuItemClickListener(item -> {
                       switch (item.getItemId()) {
                           case R.id.api_version_menu_html:
                               apiVersion = SendTransactionTask.API.html;
                               break;
                           case R.id.api_version_menu_post_1_14:
                               apiVersion = SendTransactionTask.API.post_1_14;
                               break;
                           case R.id.api_version_menu_pre_1_15:
                               apiVersion = SendTransactionTask.API.pre_1_15;
                               break;
                           case R.id.api_version_menu_auto:
                           default:
                               apiVersion = SendTransactionTask.API.auto;
                       }
                       apiVersionText.setText(apiVersion.getDescription(getResources()));
                       return true;
                   });
                   menu.show();
               });
        authParams = context.findViewById(R.id.auth_params);
        useAuthentication = context.findViewById(R.id.enable_http_auth);
        userName = context.findViewById(R.id.auth_user_name);
        userNameLayout = context.findViewById(R.id.auth_user_name_layout);
        password = context.findViewById(R.id.password);
        passwordLayout = context.findViewById(R.id.password_layout);
        huePickerView = context.findViewById(R.id.btn_pick_ring_color);
        preferredAccountsFilter = context.findViewById(R.id.preferred_accounts_filter_filter);
        preferredAccountsFilterLayout =
                context.findViewById(R.id.preferred_accounts_accounts_filter_layout);
        insecureWarningText = context.findViewById(R.id.insecure_scheme_text);

        useAuthentication.setOnCheckedChangeListener((buttonView, isChecked) -> {
            debug("profiles", isChecked ? "auth enabled " : "auth disabled");
            authParams.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (isChecked)
                userName.requestFocus();
            checkInsecureSchemeWithAuth();
        });

        postingPermitted.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            preferredAccountsFilterLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            futureDatesLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        }));

        hookClearErrorOnFocusListener(profileName, profileNameLayout);
        hookClearErrorOnFocusListener(url, urlLayout);
        hookClearErrorOnFocusListener(userName, userNameLayout);
        hookClearErrorOnFocusListener(password, passwordLayout);

        final int profileThemeId;
        if (mProfile != null) {
            profileName.setText(mProfile.getName());
            postingPermitted.setChecked(mProfile.isPostingPermitted());
            showCommodityByDefault.setChecked(mProfile.getShowCommodityByDefault());
            futureDates = mProfile.getFutureDates();
            futureDatesText.setText(futureDates.getText(getResources()));
            apiVersion = mProfile.getApiVersion();
            apiVersionText.setText(apiVersion.getDescription(getResources()));
            url.setText(mProfile.getUrl());
            useAuthentication.setChecked(mProfile.isAuthEnabled());
            authParams.setVisibility(mProfile.isAuthEnabled() ? View.VISIBLE : View.GONE);
            userName.setText(mProfile.isAuthEnabled() ? mProfile.getAuthUserName() : "");
            password.setText(mProfile.isAuthEnabled() ? mProfile.getAuthPassword() : "");
            preferredAccountsFilter.setText(mProfile.getPreferredAccountsFilter());
            profileThemeId = mProfile.getThemeHue();
        }
        else {
            profileName.setText("");
            url.setText(HTTPS_URL_START);
            postingPermitted.setChecked(true);
            showCommodityByDefault.setChecked(false);
            futureDates = MobileLedgerProfile.FutureDates.None;
            futureDatesText.setText(futureDates.getText(getResources()));
            apiVersion = SendTransactionTask.API.auto;
            apiVersionText.setText(apiVersion.getDescription(getResources()));
            useAuthentication.setChecked(false);
            authParams.setVisibility(View.GONE);
            userName.setText("");
            password.setText("");
            preferredAccountsFilter.setText(null);
            profileThemeId = getArguments().getInt(ARG_HUE, -1);
        }

        checkInsecureSchemeWithAuth();

        url.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
            @Override
            public void afterTextChanged(Editable s) {
                checkInsecureSchemeWithAuth();
            }
        });

        final int hue = (profileThemeId == -1) ? Colors.DEFAULT_HUE_DEG : profileThemeId;
        final int profileColor = Colors.getPrimaryColorForHue(hue);

        huePickerView.setBackgroundColor(profileColor);
        huePickerView.setTag(profileThemeId);
        huePickerView.setOnClickListener(v -> {
            HueRingDialog d = new HueRingDialog(
                    Objects.requireNonNull(ProfileDetailFragment.this.getContext()), profileThemeId,
                    (Integer) v.getTag());
            d.show();
            d.setColorSelectedListener(this);
        });

        profileName.requestFocus();
    }
    private void onSaveFabClicked() {
        if (!checkValidity())
            return;

        if (mProfile != null) {
            updateProfileFromUI();
//                debug("profiles", String.format("Selected item is %d", mProfile.getThemeHue()));
            mProfile.storeInDB();
            debug("profiles", "profile stored in DB");
            triggerProfileChange();
        }
        else {
            mProfile = new MobileLedgerProfile();
            updateProfileFromUI();
            mProfile.storeInDB();
            final ArrayList<MobileLedgerProfile> profiles = Data.profiles.getValue();
            if (profiles == null)
                throw new AssertionError();
            ArrayList<MobileLedgerProfile> newList = new ArrayList<>(profiles);
            newList.add(mProfile);
            Data.profiles.setValue(newList);
            MobileLedgerProfile.storeProfilesOrder();

            // first profile ever?
            if (newList.size() == 1)
                Data.profile.setValue(mProfile);
        }

        Activity activity = getActivity();
        if (activity != null)
            activity.finish();
    }
    private void updateProfileFromUI() {
        mProfile.setName(profileName.getText());
        mProfile.setUrl(url.getText());
        mProfile.setPostingPermitted(postingPermitted.isChecked());
        mProfile.setShowCommodityByDefault(showCommodityByDefault.isChecked());
        mProfile.setPreferredAccountsFilter(preferredAccountsFilter.getText());
        mProfile.setAuthEnabled(useAuthentication.isChecked());
        mProfile.setAuthUserName(userName.getText());
        mProfile.setAuthPassword(password.getText());
        mProfile.setThemeHue(huePickerView.getTag());
        mProfile.setFutureDates(futureDates);
        mProfile.setApiVersion(apiVersion);
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.profile_detail, container, false);
    }
    private boolean checkUrlValidity() {
        boolean valid = true;

        String val = String.valueOf(url.getText())
                           .trim();
        if (val.isEmpty()) {
            valid = false;
            urlLayout.setError(getResources().getText(R.string.err_profile_url_empty));
        }
        try {
            URL url = new URL(val);
            String host = url.getHost();
            if (host == null || host.isEmpty())
                throw new MalformedURLException("Missing host");
            String protocol = url.getProtocol()
                                 .toUpperCase();
            if (!protocol.equals("HTTP") && !protocol.equals("HTTPS")) {
                valid = false;
                urlLayout.setError(getResources().getText(R.string.err_invalid_url));
            }
        }
        catch (MalformedURLException e) {
            valid = false;
            urlLayout.setError(getResources().getText(R.string.err_invalid_url));
        }

        return valid;
    }
    private void checkInsecureSchemeWithAuth() {
        boolean showWarning = false;

        if (useAuthentication.isChecked()) {
            String urlText = url.getText()
                                .toString();
            if (urlText.startsWith("http") && !urlText.startsWith("https"))
                showWarning = true;
        }

        if (showWarning)
            insecureWarningText.setVisibility(View.VISIBLE);
        else
            insecureWarningText.setVisibility(View.GONE);
    }
    private void hookClearErrorOnFocusListener(TextView view, TextInputLayout layout) {
        view.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus)
                layout.setError(null);
        });
        view.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                layout.setError(null);
            }
            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }
    private boolean checkValidity() {
        boolean valid = true;

        String val = String.valueOf(profileName.getText());
        if (val.trim()
               .isEmpty())
        {
            valid = false;
            profileNameLayout.setError(getResources().getText(R.string.err_profile_name_empty));
        }

        if (!checkUrlValidity())
            valid = false;

        if (useAuthentication.isChecked()) {
            val = String.valueOf(userName.getText());
            if (val.trim()
                   .isEmpty())
            {
                valid = false;
                userNameLayout.setError(
                        getResources().getText(R.string.err_profile_user_name_empty));
            }

            val = String.valueOf(password.getText());
            if (val.trim()
                   .isEmpty())
            {
                valid = false;
                passwordLayout.setError(
                        getResources().getText(R.string.err_profile_password_empty));
            }
        }

        return valid;
    }
    @Override
    public void onHueSelected(int hue) {
        huePickerView.setBackgroundColor(Colors.getPrimaryColorForHue(hue));
        huePickerView.setTag(hue);
    }
}
