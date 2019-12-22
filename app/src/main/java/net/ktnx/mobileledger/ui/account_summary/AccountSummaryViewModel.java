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

package net.ktnx.mobileledger.ui.account_summary;

import android.os.AsyncTask;

import androidx.lifecycle.ViewModel;

import net.ktnx.mobileledger.async.UpdateAccountsTask;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerAccount;

import java.util.ArrayList;

import static net.ktnx.mobileledger.utils.Logger.debug;

public class AccountSummaryViewModel extends ViewModel {
    static public void scheduleAccountListReload() {
        if (Data.profile.getValue() == null) return;

        UAT task = new UAT();
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }

    private static class UAT extends UpdateAccountsTask {
        @Override
        protected void onPostExecute(ArrayList<LedgerAccount> list) {
            super.onPostExecute(list);
            if (list != null) {
                debug("acc", "setting updated account list");
                Data.accounts.setList(list);
            }
        }
    }

}

