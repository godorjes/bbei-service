package com.bibei.dto;

import com.bibei.model.ChecklistSection;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class ApiModels {
    private ApiModels() {
    }

    public static class TemplateView {
        public Long id;
        public String name;
        public String icon;
        public boolean pinned;
        public boolean archived;
        public List<ChecklistSection> sections = new ArrayList<>();
        public int itemCount;
        public LocalDateTime createdAt;
        public LocalDateTime updatedAt;
    }

    public static class SaveTemplateRequest {
        @NotBlank(message = "模板名称不能为空")
        public String name;
        public String icon = "suitcase";
        public boolean pinned;
        public List<ChecklistSection> sections = new ArrayList<>();
    }

    public static class CreatePackingListRequest {
        public Long templateId;
        public String title;
    }

    public static class SavePackingListRequest {
        @NotBlank(message = "清单名称不能为空")
        public String title;
        public List<ChecklistSection> sections = new ArrayList<>();
    }

    public static class CompletePackingListRequest {
        public List<String> promoteItemIds = new ArrayList<>();
        public String saveAsTemplateName;
    }

    public static class PackingListView {
        public Long id;
        public String title;
        public Long sourceTemplateId;
        public String sourceTemplateName;
        public String status;
        public List<ChecklistSection> sections = new ArrayList<>();
        public int totalCount;
        public int checkedCount;
        public PackingListChanges changes = new PackingListChanges();
        public LocalDateTime createdAt;
        public LocalDateTime updatedAt;
        public LocalDateTime completedAt;
    }

    public static class PackingListChanges {
        public List<ItemChangeView> added = new ArrayList<>();
        public List<ItemChangeView> removed = new ArrayList<>();
        public List<ItemChangeView> modified = new ArrayList<>();
    }

    public static class ItemChangeView {
        public String itemId;
        public String name;
        public String sectionTitle;
        public Integer quantity;
        public String note;
        public String description;
    }

    public static class ItemSuggestionView {
        public String name;
        public Integer quantity;
        public String note;
        public String sectionTitle;
        public String source;
    }
}
