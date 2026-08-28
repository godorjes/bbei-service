package com.bibei.service;

import com.bibei.dto.ApiModels.ItemSuggestionView;
import com.bibei.dto.ApiModels.TemplateView;
import com.bibei.entity.PackingListEntity;
import com.bibei.mapper.PackingListMapper;
import com.bibei.model.ChecklistItem;
import com.bibei.model.ChecklistSection;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ItemSearchService {
    private static final int MAX_RESULTS = 8;

    private final TemplateService templateService;
    private final PackingListMapper packingListMapper;

    public ItemSearchService(TemplateService templateService, PackingListMapper packingListMapper) {
        this.templateService = templateService;
        this.packingListMapper = packingListMapper;
    }

    public List<ItemSuggestionView> search(String query, Long excludeListId) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, ItemSuggestionView> unique = new LinkedHashMap<>();
        for (TemplateView template : templateService.list(false)) {
            collect(unique, template.sections, normalizedQuery, "模板 · " + template.name);
        }
        for (PackingListEntity list : packingListMapper.findAll()) {
            if (!"COMPLETED".equals(list.listStatus) || (excludeListId != null && excludeListId.equals(list.id))) {
                continue;
            }
            collect(unique, templateService.readSections(list.contentJson), normalizedQuery, "历史 · " + list.title);
        }

        List<ItemSuggestionView> result = new ArrayList<>(unique.values());
        result.sort(Comparator.comparingInt(item -> matchScore(item.name, normalizedQuery)));
        if (result.size() > MAX_RESULTS) {
            return new ArrayList<>(result.subList(0, MAX_RESULTS));
        }
        return result;
    }

    private void collect(
            Map<String, ItemSuggestionView> target,
            List<ChecklistSection> sections,
            String normalizedQuery,
            String source
    ) {
        for (ChecklistSection section : sections) {
            for (ChecklistItem item : section.items) {
                String normalizedName = normalize(item.name);
                if (!normalizedName.contains(normalizedQuery) || target.containsKey(normalizedName)) {
                    continue;
                }
                ItemSuggestionView suggestion = new ItemSuggestionView();
                suggestion.name = item.name;
                suggestion.quantity = item.quantity == null ? 1 : item.quantity;
                suggestion.note = item.note;
                suggestion.sectionTitle = section.title;
                suggestion.source = source;
                target.put(normalizedName, suggestion);
            }
        }
    }

    private int matchScore(String name, String normalizedQuery) {
        String normalizedName = normalize(name);
        if (normalizedName.equals(normalizedQuery)) return 0;
        if (normalizedName.startsWith(normalizedQuery)) return 1;
        return 2;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
