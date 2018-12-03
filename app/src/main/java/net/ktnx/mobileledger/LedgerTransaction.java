package net.ktnx.mobileledger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class LedgerTransaction {
    private String date;
    private String description;
    private List<LedgerTransactionItem> items;

    LedgerTransaction(String date, String description) {
        this.date = date;
        this.description = description;
        this.items = new ArrayList<LedgerTransactionItem>();
    }

    void add_item(LedgerTransactionItem item) {
        items.add(item);
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Iterator<LedgerTransactionItem> getItemsIterator() {
        return new Iterator<LedgerTransactionItem>() {
            private int pointer = 0;
            @Override
            public boolean hasNext() {
                return pointer < items.size();
            }

            @Override
            public LedgerTransactionItem next() {
                return hasNext() ? items.get(pointer++) : null;
            }
        };
    }
}
