package com.bibei.service;

import com.bibei.dto.CatalogModels.ItemView;
import com.bibei.dto.CatalogModels.SaveSceneRequest;
import com.bibei.dto.CatalogModels.SceneDetail;
import com.bibei.dto.CatalogModels.SceneSectionView;
import com.bibei.dto.CatalogModels.SceneSummary;
import com.bibei.entity.SceneEntity;
import com.bibei.entity.SceneItemRow;
import com.bibei.exception.NotFoundException;
import com.bibei.mapper.SceneMapper;
import com.bibei.mapper.SectionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SceneService {
    private final SceneMapper sceneMapper;
    private final SectionMapper sectionMapper;

    public SceneService(SceneMapper sceneMapper, SectionMapper sectionMapper) {
        this.sceneMapper = sceneMapper;
        this.sectionMapper = sectionMapper;
    }

    public List<SceneSummary> list() {
        List<SceneSummary> result = new ArrayList<>();
        for (SceneEntity entity : sceneMapper.findAll()) {
            result.add(get(entity.id));
        }
        return result;
    }

    public SceneDetail get(long id) {
        SceneEntity entity = require(id);
        SceneDetail detail = new SceneDetail();
        detail.id = entity.id;
        detail.name = entity.name;
        detail.sectionIds = sceneMapper.findSectionIds(id);

        Map<Long, SceneSectionView> sections = new LinkedHashMap<>();
        Set<Long> seenItems = new HashSet<>();
        for (SceneItemRow row : sceneMapper.findChecklistRows(id)) {
            SceneSectionView section = sections.computeIfAbsent(row.sectionId, key -> {
                SceneSectionView value = new SceneSectionView();
                value.id = row.sectionId;
                value.name = row.sectionName;
                return value;
            });
            if (row.itemId == null || !seenItems.add(row.itemId)) {
                continue;
            }
            ItemView item = new ItemView();
            item.id = row.itemId;
            item.name = row.itemName;
            item.checked = Boolean.TRUE.equals(row.checked);
            section.items.add(item);
            detail.totalCount++;
            if (item.checked) {
                detail.checkedCount++;
            }
        }
        detail.sections.addAll(sections.values());
        return detail;
    }

    @Transactional
    public SceneDetail create(SaveSceneRequest request) {
        String name = cleanName(request.name);
        ensureUniqueName(name, null);
        List<Long> sectionIds = validSectionIds(request.sectionIds);
        LocalDateTime now = LocalDateTime.now();
        SceneEntity entity = new SceneEntity();
        entity.name = name;
        entity.createdAt = now;
        entity.updatedAt = now;
        sceneMapper.insert(entity);
        replaceSections(entity.id, sectionIds);
        return get(entity.id);
    }

    @Transactional
    public SceneDetail update(long id, SaveSceneRequest request) {
        SceneEntity entity = require(id);
        String name = cleanName(request.name);
        ensureUniqueName(name, id);
        List<Long> sectionIds = validSectionIds(request.sectionIds);
        entity.name = name;
        entity.updatedAt = LocalDateTime.now();
        sceneMapper.update(entity);
        replaceSections(id, sectionIds);
        sceneMapper.deleteInvisibleStates();
        return get(id);
    }

    @Transactional
    public void delete(long id) {
        require(id);
        sceneMapper.delete(id);
    }

    @Transactional
    public void setChecked(long sceneId, long itemId, boolean checked) {
        require(sceneId);
        if (sceneMapper.countVisibleItem(sceneId, itemId) == 0) {
            throw new IllegalArgumentException("物品不在当前场景中");
        }
        sceneMapper.deleteCheckedState(sceneId, itemId);
        if (checked) {
            sceneMapper.insertCheckedState(sceneId, itemId);
        }
    }

    @Transactional
    public void reset(long sceneId) {
        require(sceneId);
        sceneMapper.resetCheckedState(sceneId);
    }

    private void replaceSections(long sceneId, List<Long> sectionIds) {
        sceneMapper.deleteSectionBindings(sceneId);
        for (int index = 0; index < sectionIds.size(); index++) {
            sceneMapper.insertSectionBinding(sceneId, sectionIds.get(index), index);
        }
    }

    private List<Long> validSectionIds(List<Long> requestedIds) {
        LinkedHashSet<Long> unique = new LinkedHashSet<>();
        if (requestedIds != null) {
            for (Long sectionId : requestedIds) {
                if (sectionId == null || sectionMapper.findById(sectionId) == null) {
                    throw new IllegalArgumentException("选择的分区不存在");
                }
                unique.add(sectionId);
            }
        }
        return new ArrayList<>(unique);
    }

    private SceneEntity require(long id) {
        SceneEntity entity = sceneMapper.findById(id);
        if (entity == null) {
            throw new NotFoundException("没有找到这个场景");
        }
        return entity;
    }

    private void ensureUniqueName(String name, Long excludeId) {
        if (sceneMapper.countByName(name, excludeId) > 0) {
            throw new IllegalArgumentException("“" + name + "”场景已经存在");
        }
    }

    private String cleanName(String name) {
        String value = name == null ? "" : name.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("请输入场景名称");
        }
        return value;
    }
}
