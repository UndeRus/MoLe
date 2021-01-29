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

package net.ktnx.mobileledger.model;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import java.io.Serializable;

public class PatternDetailSource implements Serializable {
    public static final DiffUtil.ItemCallback<PatternDetailSource> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<PatternDetailSource>() {
                @Override
                public boolean areItemsTheSame(@NonNull PatternDetailSource oldItem,
                                               @NonNull PatternDetailSource newItem) {
                    return oldItem.groupNumber == newItem.groupNumber;
                }
                @Override
                public boolean areContentsTheSame(@NonNull PatternDetailSource oldItem,
                                                  @NonNull PatternDetailSource newItem) {
                    return oldItem.matchedText.equals(newItem.matchedText);
                }
            };

    private short groupNumber;
    private String matchedText;
    public PatternDetailSource() {
    }
    public PatternDetailSource(short groupNumber, String matchedText) {
        this.groupNumber = groupNumber;
        this.matchedText = matchedText;
    }
    public short getGroupNumber() {
        return groupNumber;
    }
    public void setGroupNumber(short groupNumber) {
        this.groupNumber = groupNumber;
    }
    public String getMatchedText() {
        return matchedText;
    }
    public void setMatchedText(String matchedText) {
        this.matchedText = matchedText;
    }
}
