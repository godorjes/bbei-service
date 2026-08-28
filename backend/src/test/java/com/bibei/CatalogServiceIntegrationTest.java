package com.bibei;

import com.bibei.dto.CatalogModels.ItemView;
import com.bibei.dto.CatalogModels.SaveItemRequest;
import com.bibei.dto.CatalogModels.SaveSceneRequest;
import com.bibei.dto.CatalogModels.SaveSectionRequest;
import com.bibei.dto.CatalogModels.SceneDetail;
import com.bibei.dto.CatalogModels.SectionView;
import com.bibei.service.ItemService;
import com.bibei.service.SceneService;
import com.bibei.service.SectionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CatalogServiceIntegrationTest {
    @Autowired
    private SceneService sceneService;

    @Autowired
    private SectionService sectionService;

    @Autowired
    private ItemService itemService;

    @Test
    void sharedItemAppearsOnceInEarliestSceneSection() {
        SectionView first = sectionService.create(section("测试随身", Collections.emptyList()));
        SectionView second = sectionService.create(section("测试数码", Collections.emptyList()));
        itemService.create(item("测试充电器", Arrays.asList(first.id, second.id)));

        SceneDetail detail = sceneService.create(scene("测试周末出游", Arrays.asList(first.id, second.id)));

        assertThat(detail.sections).extracting(section -> section.name).containsExactly("测试随身", "测试数码");
        assertThat(detail.sections.get(0).items).extracting(entry -> entry.name).containsExactly("测试充电器");
        assertThat(detail.sections.get(1).items).isEmpty();
        assertThat(detail.totalCount).isEqualTo(1);
    }

    @Test
    void checkedStatePersistsAndResetClearsIt() {
        SectionView section = sectionService.create(section("测试证件", Collections.emptyList()));
        ItemView item = itemService.create(item("测试身份证", Collections.singletonList(section.id)));
        SceneDetail scene = sceneService.create(scene("测试商务出差", Collections.singletonList(section.id)));

        sceneService.setChecked(scene.id, item.id, true);
        assertThat(sceneService.get(scene.id).checkedCount).isEqualTo(1);

        sceneService.reset(scene.id);
        assertThat(sceneService.get(scene.id).checkedCount).isZero();
    }

    @Test
    void deletingSectionKeepsItemAsUnbound() {
        SectionView section = sectionService.create(section("临时分区", Collections.emptyList()));
        ItemView item = itemService.create(item("雨伞", Collections.singletonList(section.id)));

        sectionService.delete(section.id);

        assertThat(itemService.get(item.id).sectionIds).isEmpty();
    }

    @Test
    void sectionBindingAddsItToSceneWithoutReorderingExistingSections() {
        SectionView first = sectionService.create(section("测试证件顺序", Collections.emptyList()));
        SectionView second = sectionService.create(section("测试衣物顺序", Collections.emptyList()));
        SectionView appended = sectionService.create(section("测试健康顺序", Collections.emptyList()));
        SceneDetail scene = sceneService.create(scene("测试一周旅行", Arrays.asList(first.id, second.id)));

        sectionService.update(appended.id, section("测试健康顺序", Collections.singletonList(scene.id)));

        assertThat(sceneService.get(scene.id).sectionIds).containsExactly(first.id, second.id, appended.id);
    }

    @Test
    void normalizedDuplicateNamesAreRejected() {
        sectionService.create(section("测试重复分区", Collections.emptyList()));

        assertThatThrownBy(() -> sectionService.create(section("  测试重复分区  ", Collections.emptyList())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("已经存在");
    }

    private SaveSceneRequest scene(String name, java.util.List<Long> sectionIds) {
        SaveSceneRequest request = new SaveSceneRequest();
        request.name = name;
        request.sectionIds = sectionIds;
        return request;
    }

    private SaveSectionRequest section(String name, java.util.List<Long> sceneIds) {
        SaveSectionRequest request = new SaveSectionRequest();
        request.name = name;
        request.sceneIds = sceneIds;
        return request;
    }

    private SaveItemRequest item(String name, java.util.List<Long> sectionIds) {
        SaveItemRequest request = new SaveItemRequest();
        request.name = name;
        request.sectionIds = sectionIds;
        return request;
    }
}
