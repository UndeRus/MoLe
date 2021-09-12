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

import net.ktnx.mobileledger.utils.Misc;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class ConfigWriter extends ConfigIO {
    private final OnDoneListener onDoneListener;
    private RawConfigWriter w;
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
        w = new RawConfigWriter(new FileOutputStream(pfd.getFileDescriptor()));
    }
    @Override
    protected void processStream() throws IOException {
        w.writeConfig();

        if (onDoneListener != null)
            Misc.onMainThread(onDoneListener::done);
    }
    abstract static public class OnDoneListener {
        public abstract void done();
    }
}
