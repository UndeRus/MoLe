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

package net.ktnx.mobileledger.async;

import android.content.Context;
import android.net.Uri;
import android.util.JsonWriter;

import net.ktnx.mobileledger.db.Currency;
import net.ktnx.mobileledger.db.DB;
import net.ktnx.mobileledger.db.Profile;
import net.ktnx.mobileledger.db.TemplateAccount;
import net.ktnx.mobileledger.db.TemplateWithAccounts;
import net.ktnx.mobileledger.json.API;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.utils.Misc;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

public class ConfigWriter extends ConfigIO {
    private final OnDoneListener onDoneListener;
    private JsonWriter w;
    public ConfigWriter(Context context, Uri uri, OnErrorListener onErrorListener,
                        OnDoneListener onDoneListener) throws FileNotFoundException {
        super(context, uri, onErrorListener);

        this.onDoneListener = onDoneListener;
    }
    @Override
    protected String getStreamMode() {
        return "w";
    }
    @Override
    protected void initStream() {
        w = new JsonWriter(new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(pfd.getFileDescriptor()))));
        w.setIndent("  ");
    }
    @Override
    protected void processStream() throws IOException {
        w.beginObject();
        writeCommodities(w);
        writeProfiles(w);
        writeCurrentProfile(w);
        writeConfigTemplates(w);
        w.endObject();
        w.flush();

        if (onDoneListener != null)
            Misc.onMainThread(onDoneListener::done);
    }
    private void writeKey(JsonWriter w, String key, String value) throws IOException {
        if (value != null)
            w.name(key)
             .value(value);
    }
    private void writeKey(JsonWriter w, String key, Integer value) throws IOException {
        if (value != null)
            w.name(key)
             .value(value);
    }
    private void writeKey(JsonWriter w, String key, Long value) throws IOException {
        if (value != null)
            w.name(key)
             .value(value);
    }
    private void writeKey(JsonWriter w, String key, Float value) throws IOException {
        if (value != null)
            w.name(key)
             .value(value);
    }
    private void writeKey(JsonWriter w, String key, Boolean value) throws IOException {
        if (value != null)
            w.name(key)
             .value(value);
    }
    private void writeConfigTemplates(JsonWriter w) throws IOException {
        List<TemplateWithAccounts> templates = DB.get()
                                                 .getTemplateDAO()
                                                 .getAllTemplatesWithAccountsSync();

        if (templates.isEmpty())
            return;

        w.name("templates")
         .beginArray();
        for (TemplateWithAccounts t : templates) {
            w.name(Keys.UUID)
             .value(t.header.getUuid());
            w.name(Keys.NAME)
             .value(t.header.getName());
            w.name(Keys.REGEX)
             .value(t.header.getRegularExpression());
            writeKey(w, Keys.TEST_TEXT, t.header.getTestText());
            writeKey(w, Keys.DATE_YEAR, t.header.getDateYear());
            writeKey(w, Keys.DATE_YEAR_GROUP, t.header.getDateYearMatchGroup());
            writeKey(w, Keys.DATE_MONTH, t.header.getDateMonth());
            writeKey(w, Keys.DATE_MONTH_GROUP, t.header.getDateMonthMatchGroup());
            writeKey(w, Keys.DATE_DAY, t.header.getDateDay());
            writeKey(w, Keys.DATE_DAY_GROUP, t.header.getDateDayMatchGroup());
            writeKey(w, Keys.TRANSACTION, t.header.getTransactionDescription());
            writeKey(w, Keys.TRANSACTION_GROUP, t.header.getTransactionDescriptionMatchGroup());
            writeKey(w, Keys.COMMENT, t.header.getTransactionComment());
            writeKey(w, Keys.COMMENT_GROUP, t.header.getTransactionCommentMatchGroup());
            w.name(Keys.IS_FALLBACK)
             .value(t.header.isFallback());
            if (t.accounts.size() > 0) {
                w.name(Keys.ACCOUNTS)
                 .beginArray();
                for (TemplateAccount a : t.accounts) {
                    writeKey(w, Keys.NAME, a.getAccountName());
                    writeKey(w, Keys.NAME_GROUP, a.getAccountNameMatchGroup());
                    writeKey(w, Keys.COMMENT, a.getAccountComment());
                    writeKey(w, Keys.COMMENT_GROUP, a.getAccountCommentMatchGroup());
                    writeKey(w, Keys.AMOUNT, a.getAmount());
                    writeKey(w, Keys.AMOUNT_GROUP, a.getAmountMatchGroup());
                    writeKey(w, Keys.NEGATE_AMOUNT, a.getNegateAmount());
                    writeKey(w, Keys.CURRENCY, a.getCurrency());
                    writeKey(w, Keys.CURRENCY_GROUP, a.getCurrencyMatchGroup());
                }
                w.endArray();
            }
        }
        w.endArray();
    }
    private void writeCommodities(JsonWriter w) throws IOException {
        List<Currency> list = DB.get()
                                .getCurrencyDAO()
                                .getAllSync();
        if (list.isEmpty())
            return;
        w.name(Keys.COMMODITIES)
         .beginArray();
        for (Currency c : list) {
            w.beginObject();
            writeKey(w, Keys.NAME, c.getName());
            writeKey(w, Keys.POSITION, c.getPosition());
            writeKey(w, Keys.HAS_GAP, c.getHasGap());
            w.endObject();
        }
        w.endArray();
    }
    private void writeProfiles(JsonWriter w) throws IOException {
        List<Profile> profiles = DB.get()
                                   .getProfileDAO()
                                   .getAllOrderedSync();

        if (profiles.isEmpty())
            return;

        w.name(Keys.PROFILES)
         .beginArray();
        for (Profile p : profiles) {
            w.beginObject();

            w.name(Keys.NAME)
             .value(p.getName());
            w.name(Keys.UUID)
             .value(p.getUuid());
            w.name(Keys.URL)
             .value(p.getUrl());
            w.name(Keys.USE_AUTH)
             .value(p.useAuthentication());
            if (p.useAuthentication()) {
                w.name(Keys.AUTH_USER)
                 .value(p.getAuthUser());
                w.name(Keys.AUTH_PASS)
                 .value(p.getAuthPassword());
            }
            if (p.getApiVersion() != API.auto.toInt())
                w.name(Keys.API_VER)
                 .value(p.getApiVersion());
            w.name(Keys.CAN_POST)
             .value(p.permitPosting());
            if (p.permitPosting()) {
                String defaultCommodity = p.getDefaultCommodity();
                if (!defaultCommodity.isEmpty())
                    w.name(Keys.DEFAULT_COMMODITY)
                     .value(defaultCommodity);
                w.name(Keys.SHOW_COMMODITY)
                 .value(p.getShowCommodityByDefault());
                w.name(Keys.SHOW_COMMENTS)
                 .value(p.getShowCommentsByDefault());
                w.name(Keys.FUTURE_DATES)
                 .value(p.getFutureDates());
                w.name(Keys.PREF_ACCOUNT)
                 .value(p.getPreferredAccountsFilter());
            }
            w.name(Keys.COLOUR)
             .value(p.getTheme());

            w.endObject();
        }
        w.endArray();
    }
    private void writeCurrentProfile(JsonWriter w) throws IOException {
        Profile currentProfile = Data.getProfile();
        if (currentProfile == null)
            return;

        w.name(Keys.CURRENT_PROFILE)
         .value(currentProfile.getUuid());
    }

    abstract static public class OnDoneListener {
        public abstract void done();
    }
}
