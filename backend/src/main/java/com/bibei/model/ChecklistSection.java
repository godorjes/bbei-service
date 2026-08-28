package com.bibei.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChecklistSection {
    public String id;
    public String title;
    public List<ChecklistItem> items = new ArrayList<>();

    public ChecklistSection() {
    }

    public ChecklistSection(String title) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
    }

    public ChecklistSection copyForTrip() {
        ChecklistSection copy = new ChecklistSection(title);
        for (ChecklistItem item : items) {
            copy.items.add(item.copyForTrip());
        }
        return copy;
    }
}
