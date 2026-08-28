package com.bibei.config;

import com.bibei.dto.ApiModels.TemplateView;
import com.bibei.dto.CatalogModels.ItemView;
import com.bibei.dto.CatalogModels.SaveItemRequest;
import com.bibei.dto.CatalogModels.SaveSceneRequest;
import com.bibei.dto.CatalogModels.SaveSectionRequest;
import com.bibei.dto.CatalogModels.SceneSummary;
import com.bibei.dto.CatalogModels.SectionView;
import com.bibei.model.ChecklistItem;
import com.bibei.model.ChecklistSection;
import com.bibei.service.ItemService;
import com.bibei.service.SceneService;
import com.bibei.service.SectionService;
import com.bibei.service.TemplateService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@Order(2)
public class CatalogDataInitializer implements ApplicationRunner {
    private static final String VERSION = "option-b-catalog-v1";

    private final JdbcTemplate jdbcTemplate;
    private final TemplateService templateService;
    private final SceneService sceneService;
    private final SectionService sectionService;
    private final ItemService itemService;

    public CatalogDataInitializer(
            JdbcTemplate jdbcTemplate,
            TemplateService templateService,
            SceneService sceneService,
            SectionService sectionService,
            ItemService itemService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.templateService = templateService;
        this.sceneService = sceneService;
        this.sectionService = sectionService;
        this.itemService = itemService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        initialize();
    }

    @Transactional
    public void initialize() {
        if (isApplied()) {
            return;
        }
        List<TemplateView> templates = templateService.list(false);
        if (templates.isEmpty()) {
            seedCatalog();
        } else {
            migrateTemplates(templates);
        }
        jdbcTemplate.update(
                "INSERT INTO app_migration(version, applied_at) VALUES(?, CURRENT_TIMESTAMP)",
                VERSION
        );
    }

    private void migrateTemplates(List<TemplateView> templates) {
        Map<String, SectionView> sectionsByName = new LinkedHashMap<>();
        for (SectionView section : sectionService.list()) {
            sectionsByName.put(key(section.name), section);
        }
        Map<String, ItemView> itemsByName = new LinkedHashMap<>();
        for (ItemView item : itemService.list(null)) {
            itemsByName.put(key(item.name), item);
        }
        Map<String, SceneSummary> scenesByName = new LinkedHashMap<>();
        for (SceneSummary scene : sceneService.list()) {
            scenesByName.put(key(scene.name), scene);
        }

        for (TemplateView template : templates) {
            List<Long> sceneSectionIds = new ArrayList<>();
            for (ChecklistSection legacySection : template.sections) {
                String sectionName = clean(legacySection.title, "未分区");
                SectionView section = sectionsByName.get(key(sectionName));
                if (section == null) {
                    section = sectionService.create(sectionRequest(sectionName));
                    sectionsByName.put(key(sectionName), section);
                }
                sceneSectionIds.add(section.id);

                for (ChecklistItem legacyItem : legacySection.items) {
                    String itemName = clean(legacyItem.name, "");
                    if (itemName.isEmpty()) {
                        continue;
                    }
                    ItemView item = itemsByName.get(key(itemName));
                    if (item == null) {
                        item = itemService.create(itemRequest(itemName, Collections.singletonList(section.id)));
                    } else if (!item.sectionIds.contains(section.id)) {
                        LinkedHashSet<Long> sectionIds = new LinkedHashSet<>(item.sectionIds);
                        sectionIds.add(section.id);
                        item = itemService.update(item.id, itemRequest(item.name, new ArrayList<>(sectionIds)));
                    }
                    itemsByName.put(key(itemName), item);
                }
            }

            String sceneName = clean(template.name, "未命名场景");
            SceneSummary scene = scenesByName.get(key(sceneName));
            SaveSceneRequest request = sceneRequest(sceneName, sceneSectionIds);
            if (scene == null) {
                scene = sceneService.create(request);
            } else {
                scene = sceneService.update(scene.id, request);
            }
            scenesByName.put(key(sceneName), scene);
        }
    }

    private void seedCatalog() {
        createSeedScene("周末旅行", new String[][]{
                {"证件", "身份证"},
                {"数码", "手机充电器", "充电宝", "耳机"},
                {"衣物", "换洗衣物", "睡衣"},
                {"洗漱", "牙刷", "洗面奶"}
        });
        createSeedScene("商务出差", new String[][]{
                {"证件", "身份证"},
                {"数码", "笔记本电脑", "电脑充电器", "手机充电器"},
                {"衣物", "衬衫", "换洗衣物"},
                {"工作", "名片", "会议资料"}
        });
        createSeedScene("一周旅行", new String[][]{
                {"证件", "身份证", "银行卡"},
                {"数码", "手机充电器", "充电宝", "耳机"},
                {"衣物", "上衣", "裤子", "换洗衣物"},
                {"洗漱", "牙刷", "洗面奶", "毛巾"},
                {"健康", "常用药", "创可贴"}
        });
    }

    private void createSeedScene(String sceneName, String[][] sectionItems) {
        Map<String, SectionView> existingSections = new LinkedHashMap<>();
        for (SectionView section : sectionService.list()) {
            existingSections.put(key(section.name), section);
        }
        Map<String, ItemView> existingItems = new LinkedHashMap<>();
        for (ItemView item : itemService.list(null)) {
            existingItems.put(key(item.name), item);
        }
        List<Long> sectionIds = new ArrayList<>();
        for (String[] values : sectionItems) {
            SectionView section = existingSections.get(key(values[0]));
            if (section == null) {
                section = sectionService.create(sectionRequest(values[0]));
                existingSections.put(key(values[0]), section);
            }
            sectionIds.add(section.id);
            for (String name : Arrays.copyOfRange(values, 1, values.length)) {
                ItemView item = existingItems.get(key(name));
                if (item == null) {
                    item = itemService.create(itemRequest(name, Collections.singletonList(section.id)));
                } else if (!item.sectionIds.contains(section.id)) {
                    List<Long> boundSections = new ArrayList<>(item.sectionIds);
                    boundSections.add(section.id);
                    item = itemService.update(item.id, itemRequest(item.name, boundSections));
                }
                existingItems.put(key(name), item);
            }
        }
        sceneService.create(sceneRequest(sceneName, sectionIds));
    }

    private SaveSceneRequest sceneRequest(String name, List<Long> sectionIds) {
        SaveSceneRequest request = new SaveSceneRequest();
        request.name = name;
        request.sectionIds = new ArrayList<>(new LinkedHashSet<>(sectionIds));
        return request;
    }

    private SaveSectionRequest sectionRequest(String name) {
        SaveSectionRequest request = new SaveSectionRequest();
        request.name = name;
        request.sceneIds = new ArrayList<>();
        return request;
    }

    private SaveItemRequest itemRequest(String name, List<Long> sectionIds) {
        SaveItemRequest request = new SaveItemRequest();
        request.name = name;
        request.sectionIds = sectionIds;
        return request;
    }

    private boolean isApplied() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_migration WHERE version = ?",
                Integer.class,
                VERSION
        );
        return count != null && count > 0;
    }

    private String key(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String clean(String value, String fallback) {
        String cleaned = value == null ? "" : value.trim();
        return cleaned.isEmpty() ? fallback : cleaned;
    }
}
