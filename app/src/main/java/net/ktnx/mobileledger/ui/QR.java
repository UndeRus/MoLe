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

package net.ktnx.mobileledger.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class QR {
    private static final String SCAN_APP_NAME = "com.google.zxing.client.android.SCAN";
    public static ActivityResultLauncher<Void> registerLauncher(ActivityResultCaller activity,
                                                                QRScanResultReceiver resultReceiver) {
        return activity.registerForActivityResult(new ActivityResultContract<Void, String>() {
            @NonNull
            @Override
            public Intent createIntent(@NonNull Context context, Void input) {
                final Intent intent = new Intent(SCAN_APP_NAME);
                intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
                return intent;
            }
            @Override
            public String parseResult(int resultCode, @Nullable Intent intent) {
                if (resultCode == Activity.RESULT_CANCELED || intent == null)
                    return null;
                return intent.getStringExtra("SCAN_RESULT");
            }
        }, resultReceiver::onQRScanResult);
    }
    public interface QRScanResultReceiver {
        void onQRScanResult(String scanned);
    }

    public interface QRScanTrigger {
        void triggerQRScan();
    }
}
