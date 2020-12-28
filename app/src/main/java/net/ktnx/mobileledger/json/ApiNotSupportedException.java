/*
 * Copyright © 2020 Damyan Ivanov.
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

package net.ktnx.mobileledger.json;

import androidx.annotation.Nullable;

public class ApiNotSupportedException extends Throwable {
    public ApiNotSupportedException() {
    }
    public ApiNotSupportedException(@Nullable String message) {
        super(message);
    }
    public ApiNotSupportedException(@Nullable String message, @Nullable Throwable cause) {
        super(message, cause);
    }
    public ApiNotSupportedException(@Nullable Throwable cause) {
        super(cause);
    }
    public ApiNotSupportedException(@Nullable String message, @Nullable Throwable cause,
                                    boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
