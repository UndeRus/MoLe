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

import android.content.Context;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;

public abstract class QRScanCapableFragment extends Fragment {
    private static final MutableLiveData<Integer> qrScanTrigger = new MutableLiveData<>();
    protected final ActivityResultLauncher<Void> scanQrLauncher = QR.registerLauncher(this, this::onQrScanned);
    public static void triggerQRScan() {
        qrScanTrigger.setValue(1);
    }
    protected abstract void onQrScanned(String text);
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        qrScanTrigger.observe(this, ignored -> {
            scanQrLauncher.launch(null);
        });
    }
}
