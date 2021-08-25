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

import net.ktnx.mobileledger.utils.Misc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GeneralBackgroundTasks {
    private static final Executor runner = Executors.newCachedThreadPool();
    public static void run(@NotNull Runnable runnable) {
        runner.execute(runnable);
    }
    public static void run(@NotNull Runnable runnable, @NotNull Runnable onSuccess) {
        runner.execute(() -> {
            runnable.run();
            onSuccess.run();
        });
    }
    public static void run(@NotNull Runnable runnable, @Nullable Runnable onSuccess,
                           @Nullable ErrorCallback onError, @Nullable Runnable onDone) {
        runner.execute(() -> {
            try {
                runnable.run();
                if (onSuccess != null)
                    Misc.onMainThread(onSuccess);
            }
            catch (Exception e) {
                if (onError != null)
                    Misc.onMainThread(() -> onError.error(e));
                else
                    throw e;
            }
            finally {
                if (onDone != null)
                    Misc.onMainThread(onDone);
            }
        });
    }
    public static abstract class ErrorCallback {
        abstract void error(Exception e);
    }
}
