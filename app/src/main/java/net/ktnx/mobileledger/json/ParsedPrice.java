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

package net.ktnx.mobileledger.json;

class ParsedPrice {
    private String tag;
    private Contents contents;
    public ParsedPrice() {
        tag = "NoPrice";
    }
    public Contents getContents() {
        return contents;
    }
    public void setContents(Contents contents) {
        this.contents = contents;
    }
    public String getTag() {
        return tag;
    }
    public void setTag(String tag) {
        this.tag = tag;
    }
    private class Contents {
        private ParsedPrice aprice;
        private ParsedQuantity aquantity;
        private String acommodity;
        private boolean aismultiplier;
        private ParsedStyle astyle;
        public Contents() {
        }
        public ParsedPrice getAprice() {
            return aprice;
        }
        public void setAprice(ParsedPrice aprice) {
            this.aprice = aprice;
        }
        public ParsedQuantity getAquantity() {
            return aquantity;
        }
        public void setAquantity(ParsedQuantity aquantity) {
            this.aquantity = aquantity;
        }
        public String getAcommodity() {
            return acommodity;
        }
        public void setAcommodity(String acommodity) {
            this.acommodity = acommodity;
        }
        public boolean isAismultiplier() {
            return aismultiplier;
        }
        public void setAismultiplier(boolean aismultiplier) {
            this.aismultiplier = aismultiplier;
        }
        public ParsedStyle getAstyle() {
            return astyle;
        }
        public void setAstyle(ParsedStyle astyle) {
            this.astyle = astyle;
        }
    }
}
