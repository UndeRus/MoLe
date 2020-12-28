/*
 * Copyright © 2020 Damyan Ivanov.
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
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputLayout;

import net.ktnx.mobileledger.BuildConfig;
import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.json.API;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
import net.ktnx.mobileledger.ui.CurrencySelectorFragment;
import net.ktnx.mobileledger.ui.HueRingDialog;
import net.ktnx.mobileledger.ui.activity.ProfileDetailActivity;
import net.ktnx.mobileledger.utils.Colors;
import net.ktnx.mobileledger.utils.Misc;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

import static net.ktnx.mobileledger.utils.Logger.debug;

/**
 * A fragment representing a single Profile detail screen.
 * a {@link ProfileDetailActivity}
 * on handsets.
 */
public class ProfileDetailFragment extends Fragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";
    public static final String ARG_HUE = "hue";
    @NonNls

    private MobileLedgerProfile mProfile;
    private TextView url;
    private TextView defaultCommodity;
    private boolean defaultCommoditySet;
    private TextInputLayout urlLayout;
    private LinearLayout authParams;
    private SwitchMaterial useAuthentication;
    private TextView userName;
    private TextInputLayout userNameLayout;
    private TextView password;
    private TextInputLayout passwordLayout;
    private TextView profileName;
    private TextInputLayout profileNameLayout;
    private TextView preferredAccountsFilter;
    private View huePickerView;
    private View insecureWarningText;
    private TextView futureDatesText;
    private TextView apiVersionText;
    private boolean syncingModelFromUI = false;
    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ProfileDetailFragment() {
        super(R.layout.profile_detail);
    }
    @Override
    public void onCreateOptionsMenu(@NotNull Menu menu, @NotNull MenuInflater inflater) {
        debug("profiles", "[fragment] Creating profile details options menu");
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.profile_details, menu);
        final MenuItem menuDeleteProfile = menu.findItem(R.id.menuDelete);
        menuDeleteProfile.setOnMenuItemClickListener(item -> onDeleteProfile());
        final ArrayList<MobileLedgerProfile> profiles = Data.profiles.getValue();

        if (BuildConfig.DEBUG) {
            final MenuItem menuWipeProfileData = menu.findItem(R.id.menuWipeData);
            menuWipeProfileData.setOnMenuItemClickListener(ignored -> onWipeDataMenuClicked());
            menuWipeProfileData.setVisible(mProfile != null);
        }
    }
    private boolean onDeleteProfile() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(mProfile.getName());
        builder.setMessage(R.string.remove_profile_dialog_message);
        builder.setPositiveButton(R.string.Remove, (dialog, which) -> {
            debug("profiles", String.format("[fragment] removing profile %s", mProfile.getUuid()));
            mProfile.removeFromDB();
            ArrayList<MobileLedgerProfile> oldList = Data.profiles.getValue();
            if (oldList == null)
                throw new AssertionError();
            ArrayList<MobileLedgerProfile> newList = new ArrayList<>(oldList);
            newList.remove(mProfile);
            Data.profiles.setValue(newList);
            if (mProfile.equals(Data.getProfile())) {
                debug("profiles", "[fragment] setting current profile to 0");
                Data.setCurrentProfile(newList.get(0));
            }

            final FragmentActivity activity = getActivity();
            if (activity != null)
                activity.finish();
        });
        builder.show();
        return false;
    }
    private boolean onWipeDataMenuClicked() {
        // this is a development option, so no confirmation
        mProfile.wipeAllData();
        if (mProfile.equals(Data.getProfile()))
            triggerProfileChange();
        return true;
    }
    private void triggerProfileChange() {
        int index = Data.getProfileIndex(mProfile);
        MobileLedgerProfile newProfile = new MobileLedgerProfile(mProfile);
        final ArrayList<MobileLedgerProfile> profiles =
                Objects.requireNonNull(Data.profiles.getValue());
        profiles.set(index, newProfile);

        ProfilesRecyclerViewAdapter viewAdapter = ProfilesRecyclerViewAdapter.getInstance();
        if (viewAdapter != null)
            viewAdapter.notifyItemChanged(index);

        if (mProfile.equals(Data.getProfile()))
            Data.setCurrentProfile(newProfile);
    }
    private void hookTextChangeSyncRoutine(TextView view, TextChangeSyncRoutine syncRoutine) {
        view.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) { syncRoutine.onTextChanged(s.toString());}
        });
    }
    @Override
    public void onViewCreated(@NotNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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

        final LifecycleOwner viewLifecycleOwner = getViewLifecycleOwner();
        final ProfileDetailModel model = getModel();

        model.observeDefaultCommodity(viewLifecycleOwner, c -> {
            if (c != null)
                setDefaultCommodity(c.getName());
            else
                resetDefaultCommodity();
        });

        FloatingActionButton fab = context.findViewById(R.id.fab);
        fab.setOnClickListener(v -> onSaveFabClicked());

        profileName = context.findViewById(R.id.profile_name);
        hookTextChangeSyncRoutine(profileName, model::setProfileName);
        model.observeProfileName(viewLifecycleOwner, pn -> {
            if (!Misc.equalStrings(pn, profileName.getText()))
                profileName.setText(pn);
        });

        profileNameLayout = context.findViewById(R.id.profile_name_layout);

        url = context.findViewById(R.id.url);
        hookTextChangeSyncRoutine(url, model::setUrl);
        model.observeUrl(viewLifecycleOwner, u -> {
            if (!Misc.equalStrings(u, url.getText()))
                url.setText(u);
        });

        urlLayout = context.findViewById(R.id.url_layout);

        context.findViewById(R.id.default_commodity_layout)
               .setOnClickListener(v -> {
                   CurrencySelectorFragment cpf = CurrencySelectorFragment.newInstance(
                           CurrencySelectorFragment.DEFAULT_COLUMN_COUNT, false);
                   cpf.setOnCurrencySelectedListener(model::setDefaultCommodity);
                   final AppCompatActivity activity = (AppCompatActivity) v.getContext();
                   cpf.show(activity.getSupportFragmentManager(), "currency-selector");
               });

        SwitchMaterial showCommodityByDefault = context.findViewById(R.id.profile_show_commodity);
        showCommodityByDefault.setOnCheckedChangeListener(
                (buttonView, isChecked) -> model.setShowCommodityByDefault(isChecked));
        model.observeShowCommodityByDefault(viewLifecycleOwner, showCommodityByDefault::setChecked);

        View postingSubItems = context.findViewById(R.id.posting_sub_items);

        SwitchMaterial postingPermitted = context.findViewById(R.id.profile_permit_posting);
        model.observePostingPermitted(viewLifecycleOwner, isChecked -> {
            postingPermitted.setChecked(isChecked);
            postingSubItems.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
        postingPermitted.setOnCheckedChangeListener(
                ((buttonView, isChecked) -> model.setPostingPermitted(isChecked)));

        SwitchMaterial showCommentsByDefault = context.findViewById(R.id.profile_show_comments);
        model.observeShowCommentsByDefault(viewLifecycleOwner, showCommentsByDefault::setChecked);
        showCommentsByDefault.setOnCheckedChangeListener(
                ((buttonView, isChecked) -> model.setShowCommentsByDefault(isChecked)));

        defaultCommodity = context.findViewById(R.id.default_commodity_text);

        futureDatesText = context.findViewById(R.id.future_dates_text);
        context.findViewById(R.id.future_dates_layout)
               .setOnClickListener(v -> {
                   MenuInflater mi = new MenuInflater(context);
                   PopupMenu menu = new PopupMenu(context, v);
                   menu.inflate(R.menu.future_dates);
                   menu.setOnMenuItemClickListener(item -> {
                       model.setFutureDates(futureDatesSettingFromMenuItemId(item.getItemId()));
                       return true;
                   });
                   menu.show();
               });
        model.observeFutureDates(viewLifecycleOwner,
                v -> futureDatesText.setText(v.getText(getResources())));

        apiVersionText = context.findViewById(R.id.api_version_text);
        model.observeApiVersion(viewLifecycleOwner,
                apiVer -> apiVersionText.setText(apiVer.getDescription(getResources())));
        context.findViewById(R.id.api_version_label)
               .setOnClickListener(this::chooseAPIVersion);
        context.findViewById(R.id.api_version_text)
               .setOnClickListener(this::chooseAPIVersion);

        context.findViewById(R.id.server_version_label)
               .setOnClickListener(v -> model.triggerVersionDetection());
        TextView detectedServerVersion = context.findViewById(R.id.detected_server_version_text);
        model.observeDetectedVersion(viewLifecycleOwner, ver -> {
            if (ver == null)
                detectedServerVersion.setText(context.getResources()
                                                     .getString(
                                                             R.string.server_version_unknown_label));
            else if (ver.isPre_1_20_1())
                detectedServerVersion.setText(context.getResources()
                                                     .getString(
                                                             R.string.detected_server_pre_1_20_1));
            else
                detectedServerVersion.setText(ver.toString());
        });
        detectedServerVersion.setOnClickListener(v -> model.triggerVersionDetection());
        final View detectButton = context.findViewById(R.id.server_version_detect_button);
        detectButton.setOnClickListener(v -> model.triggerVersionDetection());
        model.observeDetectingHledgerVersion(viewLifecycleOwner, running -> {
            detectButton.setVisibility(running ? View.VISIBLE : View.INVISIBLE);
        });

        authParams = context.findViewById(R.id.auth_params);

        useAuthentication = context.findViewById(R.id.enable_http_auth);
        useAuthentication.setOnCheckedChangeListener((buttonView, isChecked) -> {
            model.setUseAuthentication(isChecked);
            if (isChecked)
                userName.requestFocus();
        });
        model.observeUseAuthentication(viewLifecycleOwner, isChecked -> {
            useAuthentication.setChecked(isChecked);
            authParams.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            checkInsecureSchemeWithAuth();
        });

        userName = context.findViewById(R.id.auth_user_name);
        model.observeUserName(viewLifecycleOwner, text -> {
            if (!Misc.equalStrings(text, userName.getText()))
                userName.setText(text);
        });
        hookTextChangeSyncRoutine(userName, model::setAuthUserName);
        userNameLayout = context.findViewById(R.id.auth_user_name_layout);

        password = context.findViewById(R.id.password);
        model.observePassword(viewLifecycleOwner, text -> {
            if (!Misc.equalStrings(text, password.getText()))
                password.setText(text);
        });
        hookTextChangeSyncRoutine(password, model::setAuthPassword);
        passwordLayout = context.findViewById(R.id.password_layout);

        huePickerView = context.findViewById(R.id.btn_pick_ring_color);
        model.observeThemeId(viewLifecycleOwner, themeId -> {
            final int hue = (themeId == -1) ? Colors.DEFAULT_HUE_DEG : themeId;
            final int profileColor = Colors.getPrimaryColorForHue(hue);
            huePickerView.setBackgroundColor(profileColor);
            huePickerView.setTag(hue);
        });

        preferredAccountsFilter = context.findViewById(R.id.preferred_accounts_filter_filter);
        model.observePreferredAccountsFilter(viewLifecycleOwner, text -> {
            if (!Misc.equalStrings(text, preferredAccountsFilter.getText()))
                preferredAccountsFilter.setText(text);
        });
        hookTextChangeSyncRoutine(preferredAccountsFilter, model::setPreferredAccountsFilter);

        insecureWarningText = context.findViewById(R.id.insecure_scheme_text);

        hookClearErrorOnFocusListener(profileName, profileNameLayout);
        hookClearErrorOnFocusListener(url, urlLayout);
        hookClearErrorOnFocusListener(userName, userNameLayout);
        hookClearErrorOnFocusListener(password, passwordLayout);

        if (savedInstanceState == null) {
            model.setValuesFromProfile(mProfile, getArguments().getInt(ARG_HUE, -1));
        }
        checkInsecureSchemeWithAuth();

        url.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                checkInsecureSchemeWithAuth();
            }
        });

        huePickerView.setOnClickListener(v -> {
            HueRingDialog d = new HueRingDialog(ProfileDetailFragment.this.requireContext(),
                    model.initialThemeHue, (Integer) v.getTag());
            d.show();
            d.setColorSelectedListener(model::setThemeId);
        });

        profileName.requestFocus();
    }
    private void chooseAPIVersion(View v) {
        Activity context = getActivity();
        ProfileDetailModel model = getModel();
        MenuInflater mi = new MenuInflater(context);
        PopupMenu menu = new PopupMenu(context, v);
        menu.inflate(R.menu.api_version);
        menu.setOnMenuItemClickListener(item -> {
            API apiVer;
            switch (item.getItemId()) {
                case R.id.api_version_menu_html:
                    apiVer = API.html;
                    break;
                case R.id.api_version_menu_1_19_1:
                    apiVer = API.v1_19_1;
                    break;
                case R.id.api_version_menu_1_15:
                    apiVer = API.v1_15;
                    break;
                case R.id.api_version_menu_1_14:
                    apiVer = API.v1_14;
                    break;
                case R.id.api_version_menu_auto:
                default:
                    apiVer = API.auto;
            }
            model.setApiVersion(apiVer);
            apiVersionText.setText(apiVer.getDescription(getResources()));
            return true;
        });
        menu.show();
    }
    private MobileLedgerProfile.FutureDates futureDatesSettingFromMenuItemId(int itemId) {
        switch (itemId) {
            case R.id.menu_future_dates_7:
                return MobileLedgerProfile.FutureDates.OneWeek;
            case R.id.menu_future_dates_14:
                return MobileLedgerProfile.FutureDates.TwoWeeks;
            case R.id.menu_future_dates_30:
                return MobileLedgerProfile.FutureDates.OneMonth;
            case R.id.menu_future_dates_60:
                return MobileLedgerProfile.FutureDates.TwoMonths;
            case R.id.menu_future_dates_90:
                return MobileLedgerProfile.FutureDates.ThreeMonths;
            case R.id.menu_future_dates_180:
                return MobileLedgerProfile.FutureDates.SixMonths;
            case R.id.menu_future_dates_365:
                return MobileLedgerProfile.FutureDates.OneYear;
            case R.id.menu_future_dates_all:
                return MobileLedgerProfile.FutureDates.All;
            default:
                return MobileLedgerProfile.FutureDates.None;
        }
    }
    @NotNull
    private ProfileDetailModel getModel() {
        return new ViewModelProvider(requireActivity()).get(ProfileDetailModel.class);
    }
    private void onSaveFabClicked() {
        if (!checkValidity())
            return;

        ProfileDetailModel model = getModel();
        final ArrayList<MobileLedgerProfile> profiles =
                Objects.requireNonNull(Data.profiles.getValue());

        if (mProfile != null) {
            int pos = Data.profiles.getValue()
                                   .indexOf(mProfile);
            mProfile = new MobileLedgerProfile(mProfile);
            model.updateProfile(mProfile);
            mProfile.storeInDB();
            debug("profiles", "profile stored in DB");
            profiles.set(pos, mProfile);
//                debug("profiles", String.format("Selected item is %d", mProfile.getThemeHue()));

            final MobileLedgerProfile currentProfile = Data.getProfile();
            if (mProfile.getUuid()
                        .equals(currentProfile.getUuid()))
            {
                Data.setCurrentProfile(mProfile);
            }

            ProfilesRecyclerViewAdapter viewAdapter = ProfilesRecyclerViewAdapter.getInstance();
            if (viewAdapter != null)
                viewAdapter.notifyItemChanged(pos);
        }
        else {
            mProfile = new MobileLedgerProfile(String.valueOf(UUID.randomUUID()));
            model.updateProfile(mProfile);
            mProfile.storeInDB();
            final ArrayList<MobileLedgerProfile> newList = new ArrayList<>(profiles);
            newList.add(mProfile);
            Data.profiles.setValue(newList);
            MobileLedgerProfile.storeProfilesOrder();

            // first profile ever?
            if (newList.size() == 1)
                Data.setCurrentProfile(mProfile);
        }

        Activity activity = getActivity();
        if (activity != null)
            activity.finish();
    }
    private boolean checkUrlValidity() {
        boolean valid = true;

        ProfileDetailModel model = getModel();

        String val = model.getUrl()
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

        final ProfileDetailModel model = getModel();

        if (model.getUseAuthentication()) {
            String urlText = model.getUrl();
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
    private void syncModelFromUI() {
        if (syncingModelFromUI)
            return;

        syncingModelFromUI = true;

        try {
            ProfileDetailModel model = getModel();

            model.setProfileName(profileName.getText());
            model.setUrl(url.getText());
            model.setPreferredAccountsFilter(preferredAccountsFilter.getText());
            model.setAuthUserName(userName.getText());
            model.setAuthPassword(password.getText());
        }
        finally {
            syncingModelFromUI = false;
        }
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
    private void resetDefaultCommodity() {
        defaultCommoditySet = false;
        defaultCommodity.setText(R.string.btn_no_currency);
        defaultCommodity.setTypeface(defaultCommodity.getTypeface(), Typeface.ITALIC);
    }
    private void setDefaultCommodity(@NonNull @NotNull String name) {
        defaultCommoditySet = true;
        defaultCommodity.setText(name);
        defaultCommodity.setTypeface(Typeface.DEFAULT);
    }
    interface TextChangeSyncRoutine {
        void onTextChanged(String text);
    }
}
