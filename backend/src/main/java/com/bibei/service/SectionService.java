package com.bibei.service;

import com.bibei.dto.CatalogModels.SaveSectionRequest;
import com.bibei.dto.CatalogModels.SectionView;
import com.bibei.entity.PackingSectionEntity;
import com.bibei.exception.NotFoundException;
import com.bibei.mapper.SceneMapper;
import com.bibei.mapper.SectionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class SectionService {
    private final SectionMapper sectionMapper;
    private final SceneMapper sceneMapper;

    public SectionService(SectionMapper sectionMapper, SceneMapper sceneMapper) {
        this.sectionMapper = sectionMapper;
        this.sceneMapper = sceneMapper;
    }

    public List<SectionView> list() {
        List<SectionView> result = new ArrayList<>();
        for (PackingSectionEntity entity : sectionMapper.findAll()) {
            result.add(toView(entity));
        }
        return result;
    }

    public SectionView get(long id) {
        return toView(require(id));
    }

    @Transactional
    public SectionView create(SaveSectionRequest request) {
        String name = cleanName(request.name);
        ensureUniqueName(name, null);
        List<Long> sceneIds = validSceneIds(request.sceneIds);
        LocalDateTime now = LocalDateTime.now();
        PackingSectionEntity entity = new PackingSectionEntity();
        entity.name = name;
        entity.createdAt = now;
        entity.updatedAt = now;
        sectionMapper.insert(entity);
        updateSceneBindings(entity.id, sceneIds);
        return get(entity.id);
    }

    @Transactional
    public SectionView update(long id, SaveSectionRequest request) {
        PackingSectionEntity entity = require(id);
        String name = cleanName(request.name);
        ensureUniqueName(name, id);
        List<Long> sceneIds = validSceneIds(request.sceneIds);
        entity.name = name;
        entity.updatedAt = LocalDateTime.now();
        sectionMapper.update(entity);
        updateSceneBindings(id, sceneIds);
        return get(id);
    }

    @Transactional
    public void delete(long id) {
        require(id);
        sectionMapper.delete(id);
        sceneMapper.deleteInvisibleStates();
    }

    private void updateSceneBindings(long sectionId, List<Long> desiredIds) {
        Set<Long> desired = new LinkedHashSet<>(desiredIds);
        Set<Long> current = new LinkedHashSet<>(sectionMapper.findSceneIds(sectionId));
        for (Long sceneId : current) {
            if (!desired.contains(sceneId)) {
                sectionMapper.deleteSceneBinding(sceneId, sectionId);
            }
        }
        for (Long sceneId : desired) {
            if (!current.contains(sceneId)) {
                sectionMapper.insertSceneBinding(sceneId, sectionId, sectionMapper.nextSortOrder(sceneId));
            }
        }
        sceneMapper.deleteInvisibleStates();
    }

    private List<Long> validSceneIds(List<Long> requestedIds) {
        LinkedHashSet<Long> unique = new LinkedHashSet<>();
        if (requestedIds != null) {
            for (Long sceneId : requestedIds) {
                if (sceneId == null || sceneMapper.findById(sceneId) == null) {
                    throw new IllegalArgumentException("选择的场景不存在");
                }
                unique.add(sceneId);
            }
        }
        return new ArrayList<>(unique);
    }

    private SectionView toView(PackingSectionEntity entity) {
        SectionView view = new SectionView();
        view.id = entity.id;
        view.name = entity.name;
        view.itemCount = sectionMapper.countItems(entity.id);
        view.sceneIds = sectionMapper.findSceneIds(entity.id);
        view.sceneNames = sectionMapper.findSceneNames(entity.id);
        return view;
    }

    private PackingSectionEntity require(long id) {
        PackingSectionEntity entity = sectionMapper.findById(id);
        if (entity == null) {
            throw new NotFoundException("没有找到这个分区");
        }
        return entity;
    }

    private void ensureUniqueName(String name, Long excludeId) {
        if (sectionMapper.countByName(name, excludeId) > 0) {
            throw new IllegalArgumentException("“" + name + "”分区已经存在");
        }
    }

    private String cleanName(String name) {
        String value = name == null ? "" : name.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("请输入分区名称");
        }
        return value;
    }
}
