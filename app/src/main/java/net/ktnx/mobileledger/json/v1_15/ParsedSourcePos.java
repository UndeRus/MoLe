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

package net.ktnx.mobileledger.json.v1_15;

import java.util.ArrayList;
import java.util.List;

class ParsedSourcePos {
    private String tag = "JournalSourcePos";
    private List<Object> contents;
    public ParsedSourcePos() {
        contents = new ArrayList<>();
        contents.add("");
        contents.add(new Integer[]{1, 1});
    }
    public String getTag() {
        return tag;
    }
    public void setTag(String tag) {
        this.tag = tag;
    }
    public List<Object> getContents() {
        return contents;
    }
    public void setContents(List<Object> contents) {
        this.contents = contents;
    }
}
