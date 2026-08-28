package com.bibei.model;

import java.util.UUID;

public class ChecklistItem {
    public String id;
    public String name;
    public Integer quantity = 1;
    public String note;
    public boolean checked;
    public boolean temporary;
    public String sourceItemId;

    public ChecklistItem() {
    }

    public ChecklistItem(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
    }

    public ChecklistItem copyForTrip() {
        ChecklistItem copy = new ChecklistItem();
        copy.id = UUID.randomUUID().toString();
        copy.name = name;
        copy.quantity = quantity == null ? 1 : quantity;
        copy.note = note;
        copy.checked = false;
        copy.temporary = false;
        copy.sourceItemId = id;
        return copy;
    }

    public ChecklistItem copyForTemplate() {
        ChecklistItem copy = new ChecklistItem();
        copy.id = UUID.randomUUID().toString();
        copy.name = name;
        copy.quantity = quantity == null ? 1 : quantity;
        copy.note = note;
        copy.checked = false;
        copy.temporary = false;
        copy.sourceItemId = null;
        return copy;
    }
}
