package com.bibei;

import com.bibei.config.CatalogDataInitializer;
import com.bibei.service.ItemService;
import com.bibei.service.SceneService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CatalogMigrationIntegrationTest {
    @Autowired
    private CatalogDataInitializer initializer;

    @Autowired
    private SceneService sceneService;

    @Autowired
    private ItemService itemService;

    @Test
    void activeTemplatesMigrateOnceAndMergeItemsByName() {
        int firstSceneCount = sceneService.list().size();
        long chargerCount = itemService.list("手机充电器").stream()
                .filter(item -> item.name.equals("手机充电器"))
                .count();

        initializer.initialize();

        assertThat(firstSceneCount).isEqualTo(3);
        assertThat(sceneService.list()).hasSize(firstSceneCount);
        assertThat(chargerCount).isEqualTo(1);
    }
}
