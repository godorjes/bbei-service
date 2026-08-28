package com.bibei.service;

import com.bibei.dto.ApiModels.CompletePackingListRequest;
import com.bibei.dto.ApiModels.CreatePackingListRequest;
import com.bibei.dto.ApiModels.ItemChangeView;
import com.bibei.dto.ApiModels.PackingListChanges;
import com.bibei.dto.ApiModels.PackingListView;
import com.bibei.dto.ApiModels.SavePackingListRequest;
import com.bibei.dto.ApiModels.TemplateView;
import com.bibei.entity.PackingListEntity;
import com.bibei.exception.NotFoundException;
import com.bibei.mapper.PackingListMapper;
import com.bibei.model.ChecklistItem;
import com.bibei.model.ChecklistSection;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class PackingListService {
    private static final String ACTIVE = "ACTIVE";
    private static final String COMPLETED = "COMPLETED";
    private static final DateTimeFormatter DEFAULT_TITLE_DATE = DateTimeFormatter.ofPattern("M月d日");

    private final PackingListMapper mapper;
    private final TemplateService templateService;
    private final ObjectMapper objectMapper;

    public PackingListService(
            PackingListMapper mapper,
            TemplateService templateService,
            ObjectMapper objectMapper
    ) {
        this.mapper = mapper;
        this.templateService = templateService;
        this.objectMapper = objectMapper;
    }

    public List<PackingListView> activeLists() {
        return toViews(mapper.findByStatus(ACTIVE));
    }

    public List<PackingListView> history() {
        return toViews(mapper.findByStatus(COMPLETED));
    }

    public PackingListView get(long id) {
        return toView(requireEntity(id));
    }

    @Transactional
    public PackingListView create(CreatePackingListRequest request) {
        LocalDateTime now = LocalDateTime.now();
        PackingListEntity entity = new PackingListEntity();
        List<ChecklistSection> sections = new ArrayList<>();

        if (request != null && request.templateId != null) {
            TemplateView template = templateService.get(request.templateId);
            entity.sourceTemplateId = template.id;
            entity.sourceTemplateName = template.name;
            entity.sourceContentJson = writeSections(template.sections);
            for (ChecklistSection section : template.sections) {
                sections.add(section.copyForTrip());
            }
            entity.title = hasText(request.title)
                    ? request.title.trim()
                    : template.name + " · " + DEFAULT_TITLE_DATE.format(now);
        } else {
            sections.add(new ChecklistSection("未分组"));
            entity.title = request != null && hasText(request.title)
                    ? request.title.trim()
                    : "空白清单 · " + DEFAULT_TITLE_DATE.format(now);
        }

        entity.listStatus = ACTIVE;
        entity.contentJson = writeSections(sections);
        entity.createdAt = now;
        entity.updatedAt = now;
        mapper.insert(entity);
        return toView(entity);
    }

    @Transactional
    public PackingListView save(long id, SavePackingListRequest request) {
        PackingListEntity entity = requireEntity(id);
        requireActive(entity);

        List<ChecklistSection> oldSections = templateService.readSections(entity.contentJson);
        Map<String, Boolean> temporaryState = collectTemporaryState(oldSections);
        List<ChecklistSection> cleaned = templateService.sanitizeSections(request.sections, false);

        for (ChecklistSection section : cleaned) {
            for (ChecklistItem item : section.items) {
                Boolean wasTemporary = temporaryState.get(item.id);
                item.temporary = wasTemporary == null || wasTemporary;
            }
        }

        entity.title = hasText(request.title) ? request.title.trim() : entity.title;
        entity.contentJson = writeSections(cleaned);
        entity.updatedAt = LocalDateTime.now();
        mapper.update(entity);
        return toView(entity);
    }

    @Transactional
    public PackingListView complete(long id, CompletePackingListRequest request) {
        PackingListEntity entity = requireEntity(id);
        requireActive(entity);
        List<ChecklistSection> sections = templateService.readSections(entity.contentJson);

        if (entity.sourceTemplateId != null && request != null) {
            Set<String> selectedIds = request.promoteItemIds == null
                    ? new HashSet<>()
                    : new HashSet<>(request.promoteItemIds);
            templateService.promoteItems(entity.sourceTemplateId, sections, selectedIds);
        } else if (entity.sourceTemplateId == null && request != null && hasText(request.saveAsTemplateName)) {
            templateService.createFromPackingList(request.saveAsTemplateName.trim(), sections);
        }

        entity.listStatus = COMPLETED;
        entity.completedAt = LocalDateTime.now();
        entity.updatedAt = entity.completedAt;
        mapper.update(entity);
        return toView(entity);
    }

    @Transactional
    public PackingListView reuse(long id) {
        PackingListEntity previous = requireEntity(id);
        if (!COMPLETED.equals(previous.listStatus)) {
            throw new IllegalStateException("只有已完成的清单可以再次使用");
        }

        PackingListEntity entity = new PackingListEntity();
        entity.title = previous.title + " · 再次使用";
        entity.sourceTemplateId = previous.sourceTemplateId;
        entity.sourceTemplateName = previous.sourceTemplateName;
        entity.sourceContentJson = previous.sourceContentJson;
        entity.listStatus = ACTIVE;
        entity.createdAt = LocalDateTime.now();
        entity.updatedAt = entity.createdAt;

        List<ChecklistSection> copiedSections = new ArrayList<>();
        for (ChecklistSection oldSection : templateService.readSections(previous.contentJson)) {
            ChecklistSection section = new ChecklistSection(oldSection.title);
            for (ChecklistItem oldItem : oldSection.items) {
                ChecklistItem item = oldItem.copyForTrip();
                item.temporary = oldItem.temporary;
                item.sourceItemId = oldItem.sourceItemId;
                section.items.add(item);
            }
            copiedSections.add(section);


        }
        entity.contentJson = writeSections(copiedSections);
        mapper.insert(entity);
        return toView(entity);
    }

    @Transactional
    public void abandon(long id) {
        PackingListEntity entity = requireEntity(id);
        requireActive(entity);
        mapper.delete(id);
    }


    private Map<String, Boolean> collectTemporaryState(List<ChecklistSection> sections) {
        Map<String, Boolean> result = new HashMap<>();
        for (ChecklistSection section : sections) {
            for (ChecklistItem item : section.items) {
                result.put(item.id, item.temporary);
            }
        }
        return result;
    }

    private List<PackingListView> toViews(List<PackingListEntity> entities) {
        List<PackingListView> result = new ArrayList<>();
        for (PackingListEntity entity : entities) {
            result.add(toView(entity));
        }
        return result;
    }

    private PackingListView toView(PackingListEntity entity) {
        PackingListView view = new PackingListView();
        view.id = entity.id;
        view.title = entity.title;
        view.sourceTemplateId = entity.sourceTemplateId;
        view.sourceTemplateName = entity.sourceTemplateName;
        view.status = entity.listStatus;
        view.sections = templateService.readSections(entity.contentJson);
        for (ChecklistSection section : view.sections) {
            view.totalCount += section.items.size();
            view.checkedCount += (int) section.items.stream().filter(item -> item.checked).count();
        }
        view.createdAt = entity.createdAt;
        view.changes = calculateChanges(entity, view.sections);
        view.updatedAt = entity.updatedAt;
        view.completedAt = entity.completedAt;
        return view;
    }

    private PackingListChanges calculateChanges(PackingListEntity entity, List<ChecklistSection> currentSections) {
        PackingListChanges changes = new PackingListChanges();

        for (ChecklistSection section : currentSections) {
            for (ChecklistItem item : section.items) {
                if (item.temporary) {
                    changes.added.add(toChange(item, section.title, "本次新增"));
                }
            }
        }

        if (!hasText(entity.sourceContentJson)) {
            return changes;
        }

        Map<String, ItemLocation> baselineItems = flattenByItemId(templateService.readSections(entity.sourceContentJson));
        Map<String, ItemLocation> currentBySourceId = new HashMap<>();
        for (ChecklistSection section : currentSections) {
            for (ChecklistItem item : section.items) {
                if (hasText(item.sourceItemId)) {
                    currentBySourceId.put(item.sourceItemId, new ItemLocation(item, section.title));
                }
            }
        }

        for (Map.Entry<String, ItemLocation> entry : baselineItems.entrySet()) {
            ItemLocation before = entry.getValue();
            ItemLocation after = currentBySourceId.get(entry.getKey());
            if (after == null) {
                changes.removed.add(toChange(before.item, before.sectionTitle, "仅从本次清单移除"));
                continue;
            }

            String description = modificationDescription(before, after);
            if (description != null) {
                changes.modified.add(toChange(after.item, after.sectionTitle, description));
            }
        }
        return changes;
    }

    private Map<String, ItemLocation> flattenByItemId(List<ChecklistSection> sections) {
        Map<String, ItemLocation> result = new HashMap<>();
        for (ChecklistSection section : sections) {
            for (ChecklistItem item : section.items) {
                if (hasText(item.id)) {
                    result.put(item.id, new ItemLocation(item, section.title));
                }
            }
        }
        return result;
    }

    private ItemChangeView toChange(ChecklistItem item, String sectionTitle, String description) {
        ItemChangeView change = new ItemChangeView();
        change.itemId = item.id;
        change.name = item.name;
        change.sectionTitle = sectionTitle;
        change.quantity = item.quantity == null ? 1 : item.quantity;
        change.note = item.note;
        change.description = description;
        return change;
    }

    private String modificationDescription(ItemLocation before, ItemLocation after) {
        List<String> details = new ArrayList<>();
        if (!Objects.equals(before.item.name, after.item.name)) {
            details.add("名称“" + before.item.name + "”→“" + after.item.name + "”");
        }
        int beforeQuantity = before.item.quantity == null ? 1 : before.item.quantity;
        int afterQuantity = after.item.quantity == null ? 1 : after.item.quantity;
        if (beforeQuantity != afterQuantity) {
            details.add("数量 " + beforeQuantity + " → " + afterQuantity);
        }
        if (!Objects.equals(trimToEmpty(before.item.note), trimToEmpty(after.item.note))) {
            details.add("备注已修改");
        }
        if (!Objects.equals(before.sectionTitle, after.sectionTitle)) {
            details.add("分组“" + before.sectionTitle + "”→“" + after.sectionTitle + "”");
        }
        return details.isEmpty() ? null : String.join(" · ", details);
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static class ItemLocation {
        private final ChecklistItem item;
        private final String sectionTitle;

        private ItemLocation(ChecklistItem item, String sectionTitle) {
            this.item = item;
            this.sectionTitle = sectionTitle;
        }
    }
    private PackingListEntity requireEntity(long id) {
        PackingListEntity entity = mapper.findById(id);
        if (entity == null) {
            throw new NotFoundException("没有找到该出行清单");
        }
        return entity;
    }

    private void requireActive(PackingListEntity entity) {
        if (!ACTIVE.equals(entity.listStatus)) {
            throw new IllegalStateException("已完成的清单不能修改");
        }
    }

    private String writeSections(List<ChecklistSection> sections) {
        try {
            return objectMapper.writeValueAsString(sections);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("清单数据保存失败", exception);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
