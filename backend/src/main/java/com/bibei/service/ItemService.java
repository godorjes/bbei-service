package com.bibei.service;

import com.bibei.dto.CatalogModels.ItemView;
import com.bibei.dto.CatalogModels.SaveItemRequest;
import com.bibei.entity.PackingItemEntity;
import com.bibei.exception.NotFoundException;
import com.bibei.mapper.ItemMapper;
import com.bibei.mapper.SceneMapper;
import com.bibei.mapper.SectionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class ItemService {
    private final ItemMapper itemMapper;
    private final SectionMapper sectionMapper;
    private final SceneMapper sceneMapper;

    public ItemService(ItemMapper itemMapper, SectionMapper sectionMapper, SceneMapper sceneMapper) {
        this.itemMapper = itemMapper;
        this.sectionMapper = sectionMapper;
        this.sceneMapper = sceneMapper;
    }

    public List<ItemView> list(String query) {
        String cleanedQuery = query == null ? null : query.trim();
        List<ItemView> result = new ArrayList<>();
        for (PackingItemEntity entity : itemMapper.findAll(cleanedQuery)) {
            result.add(toView(entity));
        }
        return result;
    }

    public ItemView get(long id) {
        return toView(require(id));
    }

    @Transactional
    public ItemView create(SaveItemRequest request) {
        String name = cleanName(request.name);
        ensureUniqueName(name, null);
        List<Long> sectionIds = validSectionIds(request.sectionIds);
        LocalDateTime now = LocalDateTime.now();
        PackingItemEntity entity = new PackingItemEntity();
        entity.name = name;
        entity.createdAt = now;
        entity.updatedAt = now;
        itemMapper.insert(entity);
        replaceSections(entity.id, sectionIds);
        return get(entity.id);
    }

    @Transactional
    public ItemView update(long id, SaveItemRequest request) {
        PackingItemEntity entity = require(id);
        String name = cleanName(request.name);
        ensureUniqueName(name, id);
        List<Long> sectionIds = validSectionIds(request.sectionIds);
        entity.name = name;
        entity.updatedAt = LocalDateTime.now();
        itemMapper.update(entity);
        replaceSections(id, sectionIds);
        sceneMapper.deleteInvisibleStates();
        return get(id);
    }

    @Transactional
    public void delete(long id) {
        require(id);
        itemMapper.delete(id);
    }

    private void replaceSections(long itemId, List<Long> sectionIds) {
        itemMapper.deleteSectionBindings(itemId);
        for (Long sectionId : sectionIds) {
            itemMapper.insertSectionBinding(sectionId, itemId);
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

    private ItemView toView(PackingItemEntity entity) {
        ItemView view = new ItemView();
        view.id = entity.id;
        view.name = entity.name;
        view.sectionIds = itemMapper.findSectionIds(entity.id);
        view.sectionNames = itemMapper.findSectionNames(entity.id);
        return view;
    }

    private PackingItemEntity require(long id) {
        PackingItemEntity entity = itemMapper.findById(id);
        if (entity == null) {
            throw new NotFoundException("没有找到这个物品");
        }
        return entity;
    }

    private void ensureUniqueName(String name, Long excludeId) {
        if (itemMapper.countByName(name, excludeId) > 0) {
            throw new IllegalArgumentException("“" + name + "”物品已经存在");
        }
    }

    private String cleanName(String name) {
        String value = name == null ? "" : name.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("请输入物品名称");
        }
        return value;
    }
}
