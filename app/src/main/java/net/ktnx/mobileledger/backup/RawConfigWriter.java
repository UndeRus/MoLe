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

package net.ktnx.mobileledger.backup;

import android.util.JsonWriter;

import net.ktnx.mobileledger.backup.ConfigIO.Keys;
import net.ktnx.mobileledger.db.Currency;
import net.ktnx.mobileledger.db.DB;
import net.ktnx.mobileledger.db.Profile;
import net.ktnx.mobileledger.db.TemplateAccount;
import net.ktnx.mobileledger.db.TemplateWithAccounts;
import net.ktnx.mobileledger.json.API;
import net.ktnx.mobileledger.model.Data;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

public class RawConfigWriter {
    private final JsonWriter w;
    public RawConfigWriter(OutputStream outputStream) {
        w = new JsonWriter(new BufferedWriter(new OutputStreamWriter(outputStream)));
        w.setIndent("  ");
    }
    public void writeConfig() throws IOException {
        w.beginObject();
        writeCommodities();
        writeProfiles();
        writeCurrentProfile();
        writeConfigTemplates();
        w.endObject();
        w.flush();
    }
    private void writeKey(String key, String value) throws IOException {
        if (value != null)
            w.name(key)
             .value(value);
    }
    private void writeKey(String key, Integer value) throws IOException {
        if (value != null)
            w.name(key)
             .value(value);
    }
    private void writeKey(String key, Long value) throws IOException {
        if (value != null)
            w.name(key)
             .value(value);
    }
    private void writeKey(String key, Float value) throws IOException {
        if (value != null)
            w.name(key)
             .value(value);
    }
    private void writeKey(String key, Boolean value) throws IOException {
        if (value != null)
            w.name(key)
             .value(value);
    }
    private void writeConfigTemplates() throws IOException {
        List<TemplateWithAccounts> templates = DB.get()
                                                 .getTemplateDAO()
                                                 .getAllTemplatesWithAccountsSync();

        if (templates.isEmpty())
            return;

        w.name("templates")
         .beginArray();
        for (TemplateWithAccounts t : templates) {
            w.beginObject();

            w.name(Keys.UUID)
             .value(t.header.getUuid());
            w.name(Keys.NAME)
             .value(t.header.getName());
            w.name(Keys.REGEX)
             .value(t.header.getRegularExpression());
            writeKey(Keys.TEST_TEXT, t.header.getTestText());
            writeKey(ConfigIO.Keys.DATE_YEAR, t.header.getDateYear());
            writeKey(Keys.DATE_YEAR_GROUP, t.header.getDateYearMatchGroup());
            writeKey(Keys.DATE_MONTH, t.header.getDateMonth());
            writeKey(Keys.DATE_MONTH_GROUP, t.header.getDateMonthMatchGroup());
            writeKey(Keys.DATE_DAY, t.header.getDateDay());
            writeKey(Keys.DATE_DAY_GROUP, t.header.getDateDayMatchGroup());
            writeKey(Keys.TRANSACTION, t.header.getTransactionDescription());
            writeKey(Keys.TRANSACTION_GROUP, t.header.getTransactionDescriptionMatchGroup());
            writeKey(Keys.COMMENT, t.header.getTransactionComment());
            writeKey(Keys.COMMENT_GROUP, t.header.getTransactionCommentMatchGroup());
            w.name(Keys.IS_FALLBACK)
             .value(t.header.isFallback());
            if (t.accounts.size() > 0) {
                w.name(Keys.ACCOUNTS)
                 .beginArray();
                for (TemplateAccount a : t.accounts) {
                    w.beginObject();

                    writeKey(Keys.NAME, a.getAccountName());
                    writeKey(Keys.NAME_GROUP, a.getAccountNameMatchGroup());
                    writeKey(Keys.COMMENT, a.getAccountComment());
                    writeKey(Keys.COMMENT_GROUP, a.getAccountCommentMatchGroup());
                    writeKey(Keys.AMOUNT, a.getAmount());
                    writeKey(Keys.AMOUNT_GROUP, a.getAmountMatchGroup());
                    writeKey(Keys.NEGATE_AMOUNT, a.getNegateAmount());
                    writeKey(Keys.CURRENCY, a.getCurrency());
                    writeKey(Keys.CURRENCY_GROUP, a.getCurrencyMatchGroup());

                    w.endObject();
                }
                w.endArray();
            }

            w.endObject();
        }
        w.endArray();
    }
    private void writeCommodities() throws IOException {
        List<Currency> list = DB.get()
                                .getCurrencyDAO()
                                .getAllSync();
        if (list.isEmpty())
            return;
        w.name(Keys.COMMODITIES)
         .beginArray();
        for (Currency c : list) {
            w.beginObject();
            writeKey(Keys.NAME, c.getName());
            writeKey(Keys.POSITION, c.getPosition());
            writeKey(Keys.HAS_GAP, c.getHasGap());
            w.endObject();
        }
        w.endArray();
    }
    private void writeProfiles() throws IOException {
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
    private void writeCurrentProfile() throws IOException {
        Profile currentProfile = Data.getProfile();
        if (currentProfile == null)
            return;

        w.name(Keys.CURRENT_PROFILE)
         .value(currentProfile.getUuid());
    }
}
