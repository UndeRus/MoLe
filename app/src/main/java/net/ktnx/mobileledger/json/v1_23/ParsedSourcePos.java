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

package net.ktnx.mobileledger.json.v1_23;

class ParsedSourcePos {
    private String sourceName = "";
    private int sourceLine = 1;
    private int sourceColumn = 1;
    public ParsedSourcePos() {
    }
    public String getSourceName() {
        return sourceName;
    }
    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }
    public int getSourceLine() {
        return sourceLine;
    }
    public void setSourceLine(int sourceLine) {
        this.sourceLine = sourceLine;
    }
    public int getSourceColumn() {
        return sourceColumn;
    }
    public void setSourceColumn(int sourceColumn) {
        this.sourceColumn = sourceColumn;
    }
}
