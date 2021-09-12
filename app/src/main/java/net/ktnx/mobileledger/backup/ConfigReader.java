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

import android.content.Context;
import android.net.Uri;

import net.ktnx.mobileledger.dao.ProfileDAO;
import net.ktnx.mobileledger.db.DB;
import net.ktnx.mobileledger.db.Profile;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.utils.Misc;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class ConfigReader extends ConfigIO {
    private final OnDoneListener onDoneListener;
    private RawConfigReader r;
    public ConfigReader(Context context, Uri uri, OnErrorListener onErrorListener,
                        OnDoneListener onDoneListener) throws FileNotFoundException {
        super(context, uri, onErrorListener);

        this.onDoneListener = onDoneListener;
    }
    @Override
    protected String getStreamMode() {
        return "r";
    }
    @Override
    protected void initStream() {
        RawConfigReader r = new RawConfigReader(new FileInputStream(pfd.getFileDescriptor()));
    }
    @Override
    protected void processStream() throws IOException {
        r.readConfig();
        r.restoreAll();
        String currentProfile = r.getCurrentProfile();

        if (Data.getProfile() == null) {
            Profile p = null;
            final ProfileDAO dao = DB.get()
                                     .getProfileDAO();
            if (currentProfile != null)
                p = dao.getByUuidSync(currentProfile);

            if (p == null)
                dao.getAnySync();

            if (p != null)
                Data.postCurrentProfile(p);
        }

        if (onDoneListener != null)
            Misc.onMainThread(onDoneListener::done);
    }
    abstract static public class OnDoneListener {
        public abstract void done();
    }
}
