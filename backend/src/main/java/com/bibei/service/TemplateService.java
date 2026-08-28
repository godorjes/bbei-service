package com.bibei.service;

import com.bibei.dto.ApiModels.SaveTemplateRequest;
import com.bibei.dto.ApiModels.TemplateView;
import com.bibei.entity.SceneTemplateEntity;
import com.bibei.exception.NotFoundException;
import com.bibei.mapper.SceneTemplateMapper;
import com.bibei.model.ChecklistItem;
import com.bibei.model.ChecklistSection;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class TemplateService {
    private static final TypeReference<List<ChecklistSection>> SECTION_LIST_TYPE =
            new TypeReference<List<ChecklistSection>>() { };

    private final SceneTemplateMapper mapper;
    private final ObjectMapper objectMapper;

    public TemplateService(SceneTemplateMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    public List<TemplateView> list(boolean archived) {
        List<TemplateView> result = new ArrayList<>();
        for (SceneTemplateEntity entity : mapper.findAll(archived)) {
            result.add(toView(entity));
        }
        return result;
    }

    public TemplateView get(long id) {
        return toView(requireEntity(id));
    }

    @Transactional
    public TemplateView create(SaveTemplateRequest request) {
        SceneTemplateEntity entity = new SceneTemplateEntity();
        entity.name = requiredText(request.name, "模板名称不能为空");
        entity.icon = textOrDefault(request.icon, "suitcase");
        entity.pinned = request.pinned;
        entity.archived = false;
        entity.contentJson = writeSections(sanitizeSections(request.sections, true));
        entity.createdAt = LocalDateTime.now();
        entity.updatedAt = entity.createdAt;
        mapper.insert(entity);
        return toView(entity);
    }

    @Transactional
    public TemplateView update(long id, SaveTemplateRequest request) {
        SceneTemplateEntity entity = requireEntity(id);
        entity.name = requiredText(request.name, "模板名称不能为空");
        entity.icon = textOrDefault(request.icon, "suitcase");
        entity.pinned = request.pinned;
        entity.contentJson = writeSections(sanitizeSections(request.sections, true));
        entity.updatedAt = LocalDateTime.now();
        mapper.update(entity);
        return toView(entity);
    }

    @Transactional
    public void setArchived(long id, boolean archived) {
        requireEntity(id);
        mapper.setArchived(id, archived);
    }

    @Transactional
    public void promoteItems(long templateId, List<ChecklistSection> tripSections, Set<String> selectedIds) {
        if (selectedIds == null || selectedIds.isEmpty()) {
            return;
        }
        SceneTemplateEntity entity = requireEntity(templateId);
        List<ChecklistSection> templateSections = readSections(entity.contentJson);
        Set<String> existingNames = collectNormalizedNames(templateSections);

        for (ChecklistSection tripSection : safeSections(tripSections)) {
            for (ChecklistItem item : safeItems(tripSection.items)) {
                if (!item.temporary || !selectedIds.contains(item.id)) {
                    continue;
                }
                String normalizedName = normalizeName(item.name);
                if (normalizedName.isEmpty() || existingNames.contains(normalizedName)) {
                    continue;
                }
                ChecklistSection target = findOrCreateSection(templateSections, tripSection.title);
                target.items.add(item.copyForTemplate());
                existingNames.add(normalizedName);
            }
        }

        entity.contentJson = writeSections(sanitizeSections(templateSections, true));
        entity.updatedAt = LocalDateTime.now();
        mapper.update(entity);
    }

    public TemplateView createFromPackingList(String name, List<ChecklistSection> sections) {
        SaveTemplateRequest request = new SaveTemplateRequest();
        request.name = name;
        request.icon = "suitcase";
        request.sections = sections;
        return create(request);
    }

    public List<ChecklistSection> readSections(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, SECTION_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("清单数据读取失败", exception);
        }
    }

    public List<ChecklistSection> sanitizeSections(List<ChecklistSection> sections, boolean forTemplate) {
        List<ChecklistSection> cleaned = new ArrayList<>();
        Set<String> names = new HashSet<>();

        for (ChecklistSection source : safeSections(sections)) {
            ChecklistSection section = new ChecklistSection();
            section.id = textOrDefault(source.id, UUID.randomUUID().toString());
            section.title = textOrDefault(source.title, "未分组");
            section.items = new ArrayList<>();

            for (ChecklistItem sourceItem : safeItems(source.items)) {
                String itemName = requiredText(sourceItem.name, "物品名称不能为空");
                String normalizedName = normalizeName(itemName);
                if (!names.add(normalizedName)) {
                    throw new IllegalArgumentException("物品“" + itemName + "”已存在");
                }

                ChecklistItem item = new ChecklistItem();
                item.id = textOrDefault(sourceItem.id, UUID.randomUUID().toString());
                item.name = itemName;
                item.quantity = sourceItem.quantity == null || sourceItem.quantity < 1 ? 1 : sourceItem.quantity;
                item.note = trimToNull(sourceItem.note);
                item.checked = forTemplate ? false : sourceItem.checked;
                item.temporary = forTemplate ? false : sourceItem.temporary;
                item.sourceItemId = forTemplate ? null : trimToNull(sourceItem.sourceItemId);
                section.items.add(item);
            }
            cleaned.add(section);
        }

        if (cleaned.isEmpty()) {
            cleaned.add(new ChecklistSection("未分组"));
        }
        return cleaned;
    }

    private ChecklistSection findOrCreateSection(List<ChecklistSection> sections, String title) {
        String targetTitle = textOrDefault(title, "未分组");
        for (ChecklistSection section : sections) {
            if (targetTitle.equals(section.title)) {
                return section;
            }
        }
        ChecklistSection section = new ChecklistSection(targetTitle);
        sections.add(section);
        return section;
    }

    private Set<String> collectNormalizedNames(List<ChecklistSection> sections) {
        Set<String> result = new HashSet<>();
        for (ChecklistSection section : safeSections(sections)) {
            for (ChecklistItem item : safeItems(section.items)) {
                result.add(normalizeName(item.name));
            }
        }
        return result;
    }

    private SceneTemplateEntity requireEntity(long id) {
        SceneTemplateEntity entity = mapper.findById(id);
        if (entity == null) {
            throw new NotFoundException("没有找到该场景模板");
        }
        return entity;
    }

    private TemplateView toView(SceneTemplateEntity entity) {
        TemplateView view = new TemplateView();
        view.id = entity.id;
        view.name = entity.name;
        view.icon = entity.icon;
        view.pinned = Boolean.TRUE.equals(entity.pinned);
        view.archived = Boolean.TRUE.equals(entity.archived);
        view.sections = readSections(entity.contentJson);
        view.itemCount = view.sections.stream().mapToInt(section -> safeItems(section.items).size()).sum();
        view.createdAt = entity.createdAt;
        view.updatedAt = entity.updatedAt;
        return view;
    }

    private String writeSections(List<ChecklistSection> sections) {
        try {
            return objectMapper.writeValueAsString(sections);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("清单数据保存失败", exception);
        }
    }

    private List<ChecklistSection> safeSections(List<ChecklistSection> sections) {
        return sections == null ? new ArrayList<>() : sections;
    }

    private List<ChecklistItem> safeItems(List<ChecklistItem> items) {
        return items == null ? new ArrayList<>() : items;
    }

    private String normalizeName(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private String requiredText(String value, String message) {
        String result = trimToNull(value);
        if (result == null) {
            throw new IllegalArgumentException(message);
        }
        return result;
    }

    private String textOrDefault(String value, String defaultValue) {
        String result = trimToNull(value);
        return result == null ? defaultValue : result;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String result = value.trim();
        return result.isEmpty() ? null : result;
    }
}
