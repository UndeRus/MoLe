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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.backup.BackupManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;

import net.ktnx.mobileledger.BuildConfig;
import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.dao.BaseDAO;
import net.ktnx.mobileledger.dao.ProfileDAO;
import net.ktnx.mobileledger.databinding.ProfileDetailBinding;
import net.ktnx.mobileledger.db.DB;
import net.ktnx.mobileledger.db.Profile;
import net.ktnx.mobileledger.json.API;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.FutureDates;
import net.ktnx.mobileledger.ui.CurrencySelectorFragment;
import net.ktnx.mobileledger.ui.HueRingDialog;
import net.ktnx.mobileledger.utils.Colors;
import net.ktnx.mobileledger.utils.Misc;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;

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

    private boolean defaultCommoditySet;
    private boolean syncingModelFromUI = false;
    private ProfileDetailBinding binding;
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
        final List<Profile> profiles = Data.profiles.getValue();

        final MenuItem menuWipeProfileData = menu.findItem(R.id.menuWipeData);
        if (BuildConfig.DEBUG)
            menuWipeProfileData.setOnMenuItemClickListener(ignored -> onWipeDataMenuClicked());

        getModel().getProfileId()
                  .observe(getViewLifecycleOwner(), id -> {
                      menuDeleteProfile.setVisible(id > 0);
                      if (BuildConfig.DEBUG)
                          menuWipeProfileData.setVisible(id > 0);
                  });
    }
    private boolean onDeleteProfile() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        @NotNull ProfileDetailModel model = getModel();
        builder.setTitle(model.getProfileName());
        builder.setMessage(R.string.remove_profile_dialog_message);
        builder.setPositiveButton(R.string.Remove, (dialog, which) -> {
            final long profileId = Objects.requireNonNull(model.getProfileId()
                                                               .getValue());
            debug("profiles", String.format("[fragment] removing profile %s", profileId));
            ProfileDAO dao = DB.get()
                               .getProfileDAO();
            dao.getById(profileId)
               .observe(getViewLifecycleOwner(), profile -> {
                   if (profile != null)
                       BaseDAO.runAsync(() -> DB.get()
                                                .runInTransaction(() -> {
                                                    dao.deleteSync(profile);
                                                    dao.updateOrderSync(dao.getAllOrderedSync());
                                                }));
               });

            final FragmentActivity activity = getActivity();
            if (activity != null)
                activity.finish();
        });
        builder.show();
        return false;
    }
    private boolean onWipeDataMenuClicked() {
        // this is a development option, so no confirmation
        DB.get()
          .getProfileDAO()
          .getById(Objects.requireNonNull(getModel().getProfileId()
                                                    .getValue()))
          .observe(getViewLifecycleOwner(), profile -> {
              if (profile != null)
                  profile.wipeAllData();
          });
        return true;
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
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = ProfileDetailBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }
    @Override
    public void onViewCreated(@NotNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Activity context = getActivity();
        if (context == null)
            return;

        final LifecycleOwner viewLifecycleOwner = getViewLifecycleOwner();
        final ProfileDetailModel model = getModel();

        model.observeDefaultCommodity(viewLifecycleOwner, c -> {
            if (c != null)
                setDefaultCommodity(c);
            else
                resetDefaultCommodity();
        });

        FloatingActionButton fab = context.findViewById(R.id.fabAdd);
        fab.setOnClickListener(v -> onSaveFabClicked());

        hookTextChangeSyncRoutine(binding.profileName, model::setProfileName);
        model.observeProfileName(viewLifecycleOwner, pn -> {
            if (!Misc.equalStrings(pn, Misc.nullIsEmpty(binding.profileName.getText())))
                binding.profileName.setText(pn);
        });

        hookTextChangeSyncRoutine(binding.url, model::setUrl);
        model.observeUrl(viewLifecycleOwner, u -> {
            if (!Misc.equalStrings(u, Misc.nullIsEmpty(binding.url.getText())))
                binding.url.setText(u);
        });

        binding.defaultCommodityLayout.setOnClickListener(v -> {
            CurrencySelectorFragment cpf = CurrencySelectorFragment.newInstance(
                    CurrencySelectorFragment.DEFAULT_COLUMN_COUNT, false);
            cpf.setOnCurrencySelectedListener(model::setDefaultCommodity);
            final AppCompatActivity activity = (AppCompatActivity) v.getContext();
            cpf.show(activity.getSupportFragmentManager(), "currency-selector");
        });

        binding.profileShowCommodity.setOnCheckedChangeListener(
                (buttonView, isChecked) -> model.setShowCommodityByDefault(isChecked));
        model.observeShowCommodityByDefault(viewLifecycleOwner,
                binding.profileShowCommodity::setChecked);

        model.observePostingPermitted(viewLifecycleOwner, isChecked -> {
            binding.profilePermitPosting.setChecked(isChecked);
            binding.postingSubItems.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
        binding.profilePermitPosting.setOnCheckedChangeListener(
                ((buttonView, isChecked) -> model.setPostingPermitted(isChecked)));

        model.observeShowCommentsByDefault(viewLifecycleOwner,
                binding.profileShowComments::setChecked);
        binding.profileShowComments.setOnCheckedChangeListener(
                ((buttonView, isChecked) -> model.setShowCommentsByDefault(isChecked)));

        binding.futureDatesLayout.setOnClickListener(v -> {
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
                v -> binding.futureDatesText.setText(v.getText(getResources())));

        model.observeApiVersion(viewLifecycleOwner,
                apiVer -> binding.apiVersionText.setText(apiVer.getDescription(getResources())));
        binding.apiVersionLabel.setOnClickListener(this::chooseAPIVersion);
        binding.apiVersionText.setOnClickListener(this::chooseAPIVersion);

        binding.serverVersionLabel.setOnClickListener(v -> model.triggerVersionDetection());
        model.observeDetectedVersion(viewLifecycleOwner, ver -> {
            if (ver == null)
                binding.detectedServerVersionText.setText(context.getResources()
                                                                 .getString(
                                                                         R.string.server_version_unknown_label));
            else if (ver.isPre_1_20_1())
                binding.detectedServerVersionText.setText(context.getResources()
                                                                 .getString(
                                                                         R.string.detected_server_pre_1_20_1));
            else
                binding.detectedServerVersionText.setText(ver.toString());
        });
        binding.detectedServerVersionText.setOnClickListener(v -> model.triggerVersionDetection());
        binding.serverVersionDetectButton.setOnClickListener(v -> model.triggerVersionDetection());
        model.observeDetectingHledgerVersion(viewLifecycleOwner,
                running -> binding.serverVersionDetectButton.setVisibility(
                        running ? View.VISIBLE : View.INVISIBLE));

        binding.enableHttpAuth.setOnCheckedChangeListener((buttonView, isChecked) -> {
            boolean wasOn = model.getUseAuthentication();
            model.setUseAuthentication(isChecked);
            if (!wasOn && isChecked)
                binding.authUserName.requestFocus();
        });
        model.observeUseAuthentication(viewLifecycleOwner, isChecked -> {
            binding.enableHttpAuth.setChecked(isChecked);
            binding.authParams.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            checkInsecureSchemeWithAuth();
        });

        model.observeUserName(viewLifecycleOwner, text -> {
            if (!Misc.equalStrings(text, Misc.nullIsEmpty(binding.authUserName.getText())))
                binding.authUserName.setText(text);
        });
        hookTextChangeSyncRoutine(binding.authUserName, model::setAuthUserName);

        model.observePassword(viewLifecycleOwner, text -> {
            if (!Misc.equalStrings(text, Misc.nullIsEmpty(binding.password.getText())))
                binding.password.setText(text);
        });
        hookTextChangeSyncRoutine(binding.password, model::setAuthPassword);

        model.observeThemeId(viewLifecycleOwner, themeId -> {
            final int hue = (themeId == -1) ? Colors.DEFAULT_HUE_DEG : themeId;
            final int profileColor = Colors.getPrimaryColorForHue(hue);
            binding.btnPickRingColor.setBackgroundColor(profileColor);
            binding.btnPickRingColor.setTag(hue);
        });

        model.observePreferredAccountsFilter(viewLifecycleOwner, text -> {
            if (!Misc.equalStrings(text,
                    Misc.nullIsEmpty(binding.preferredAccountsFilter.getText())))
                binding.preferredAccountsFilter.setText(text);
        });
        hookTextChangeSyncRoutine(binding.preferredAccountsFilter,
                model::setPreferredAccountsFilter);

        hookClearErrorOnFocusListener(binding.profileName, binding.profileNameLayout);
        hookClearErrorOnFocusListener(binding.url, binding.urlLayout);
        hookClearErrorOnFocusListener(binding.authUserName, binding.authUserNameLayout);
        hookClearErrorOnFocusListener(binding.password, binding.passwordLayout);

        binding.url.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                checkInsecureSchemeWithAuth();
            }
        });

        binding.btnPickRingColor.setOnClickListener(v -> {
            HueRingDialog d = new HueRingDialog(ProfileDetailFragment.this.requireContext(),
                    model.initialThemeHue, (Integer) v.getTag());
            d.show();
            d.setColorSelectedListener(model::setThemeId);
        });

        binding.profileName.requestFocus();
    }
    private void chooseAPIVersion(View v) {
        Activity context = getActivity();
        ProfileDetailModel model = getModel();
        MenuInflater mi = new MenuInflater(context);
        PopupMenu menu = new PopupMenu(context, v);
        menu.inflate(R.menu.api_version);
        menu.setOnMenuItemClickListener(item -> {
            API apiVer;
            int itemId = item.getItemId();
            if (itemId == R.id.api_version_menu_html) {
                apiVer = API.html;
            }
            else if (itemId == R.id.api_version_menu_1_23) {
                apiVer = API.v1_23;
            }
            else if (itemId == R.id.api_version_menu_1_19_1) {
                apiVer = API.v1_19_1;
            }
            else if (itemId == R.id.api_version_menu_1_15) {
                apiVer = API.v1_15;
            }
            else if (itemId == R.id.api_version_menu_1_14) {
                apiVer = API.v1_14;
            }
            else {
                apiVer = API.auto;
            }
            model.setApiVersion(apiVer);
            binding.apiVersionText.setText(apiVer.getDescription(getResources()));
            return true;
        });
        menu.show();
    }
    private FutureDates futureDatesSettingFromMenuItemId(int itemId) {
        if (itemId == R.id.menu_future_dates_7) {
            return FutureDates.OneWeek;
        }
        else if (itemId == R.id.menu_future_dates_14) {
            return FutureDates.TwoWeeks;
        }
        else if (itemId == R.id.menu_future_dates_30) {
            return FutureDates.OneMonth;
        }
        else if (itemId == R.id.menu_future_dates_60) {
            return FutureDates.TwoMonths;
        }
        else if (itemId == R.id.menu_future_dates_90) {
            return FutureDates.ThreeMonths;
        }
        else if (itemId == R.id.menu_future_dates_180) {
            return FutureDates.SixMonths;
        }
        else if (itemId == R.id.menu_future_dates_365) {
            return FutureDates.OneYear;
        }
        else if (itemId == R.id.menu_future_dates_all) {
            return FutureDates.All;
        }
        return FutureDates.None;
    }
    @NotNull
    private ProfileDetailModel getModel() {
        return new ViewModelProvider(requireActivity()).get(ProfileDetailModel.class);
    }
    private void onSaveFabClicked() {
        if (!checkValidity())
            return;

        ProfileDetailModel model = getModel();
        ProfileDAO dao = DB.get()
                           .getProfileDAO();

        Profile profile = new Profile();
        model.updateProfile(profile);
        if (profile.getId() > 0) {
            dao.update(profile);
            debug("profiles", "profile stored in DB");
//                debug("profiles", String.format("Selected item is %d", mProfile.getThemeHue()));
        }
        else {
            dao.insertLast(profile, null);
        }

        BackupManager.dataChanged(BuildConfig.APPLICATION_ID);

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
            binding.urlLayout.setError(getResources().getText(R.string.err_profile_url_empty));
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
                binding.urlLayout.setError(getResources().getText(R.string.err_invalid_url));
            }
        }
        catch (MalformedURLException e) {
            valid = false;
            binding.urlLayout.setError(getResources().getText(R.string.err_invalid_url));
        }

        return valid;
    }
    private void checkInsecureSchemeWithAuth() {
        boolean showWarning = false;

        final ProfileDetailModel model = getModel();

        if (model.getUseAuthentication()) {
            String urlText = model.getUrl();
            if (urlText.startsWith("http://") ||
                urlText.length() >= 8 && !urlText.startsWith("https://"))
                showWarning = true;
        }

        if (showWarning)
            binding.insecureSchemeText.setVisibility(View.VISIBLE);
        else
            binding.insecureSchemeText.setVisibility(View.GONE);
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

            model.setProfileName(binding.profileName.getText());
            model.setUrl(binding.url.getText());
            model.setPreferredAccountsFilter(binding.preferredAccountsFilter.getText());
            model.setAuthUserName(binding.authUserName.getText());
            model.setAuthPassword(binding.password.getText());
        }
        finally {
            syncingModelFromUI = false;
        }
    }
    private boolean checkValidity() {
        boolean valid = true;

        String val = String.valueOf(binding.profileName.getText());
        if (val.trim()
               .isEmpty())
        {
            valid = false;
            binding.profileNameLayout.setError(
                    getResources().getText(R.string.err_profile_name_empty));
        }

        if (!checkUrlValidity())
            valid = false;

        if (binding.enableHttpAuth.isChecked()) {
            val = String.valueOf(binding.authUserName.getText());
            if (val.trim()
                   .isEmpty())
            {
                valid = false;
                binding.authUserNameLayout.setError(
                        getResources().getText(R.string.err_profile_user_name_empty));
            }

            val = String.valueOf(binding.password.getText());
            if (val.trim()
                   .isEmpty())
            {
                valid = false;
                binding.passwordLayout.setError(
                        getResources().getText(R.string.err_profile_password_empty));
            }
        }

        return valid;
    }
    private void resetDefaultCommodity() {
        defaultCommoditySet = false;
        binding.defaultCommodityText.setText(R.string.btn_no_currency);
        binding.defaultCommodityText.setTypeface(binding.defaultCommodityText.getTypeface(),
                Typeface.ITALIC);
    }
    private void setDefaultCommodity(@NonNull @NotNull String name) {
        defaultCommoditySet = true;
        binding.defaultCommodityText.setText(name);
        binding.defaultCommodityText.setTypeface(Typeface.DEFAULT);
    }
    interface TextChangeSyncRoutine {
        void onTextChanged(String text);
    }
}
