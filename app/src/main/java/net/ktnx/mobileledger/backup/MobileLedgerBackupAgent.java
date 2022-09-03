/*
 * Copyright © 2022 Damyan Ivanov.
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

import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.os.ParcelFileDescriptor;

import net.ktnx.mobileledger.db.DB;
import net.ktnx.mobileledger.utils.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MobileLedgerBackupAgent extends BackupAgent {
    private static final int READ_BUF_LEN = 10;
    public static String SETTINGS_KEY = "settings";
    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
                         ParcelFileDescriptor newState) throws IOException {
        Logger.debug("backup", "onBackup()");
        backupSettings(data);
        newState.close();
    }
    private void backupSettings(BackupDataOutput data) throws IOException {
        Logger.debug ("backup", "Starting cloud backup");
        ByteArrayOutputStream output = new ByteArrayOutputStream(4096);
        RawConfigWriter saver = new RawConfigWriter(output);
        saver.writeConfig();
        byte[] bytes = output.toByteArray();
        data.writeEntityHeader(SETTINGS_KEY, bytes.length);
        data.writeEntityData(bytes, bytes.length);
        Logger.debug("backup", "Done writing backup data");
    }
    @Override
    public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState)
            throws IOException {
        Logger.debug("restore", "Starting cloud restore");
        if (data.readNextHeader()) {
            String key = data.getKey();
            if (key.equals(SETTINGS_KEY)) {
                restoreSettings(data);
            }
        }
    }
    private void restoreSettings(BackupDataInput data) throws IOException {
        byte[] bytes = new byte[data.getDataSize()];
        data.readEntityData(bytes, 0, bytes.length);
        RawConfigReader reader = new RawConfigReader(new ByteArrayInputStream(bytes));
        reader.readConfig();
        Logger.debug("restore", "Successfully read restore data. Wiping database");
        DB.get().deleteAllSync();
        Logger.debug("restore", "Database wiped");
        reader.restoreAll();
        Logger.debug("restore", "All data restored from the cloud");
    }
}
