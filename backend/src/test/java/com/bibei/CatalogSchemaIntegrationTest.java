package com.bibei;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CatalogSchemaIntegrationTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void normalizedCatalogTablesAreCreated() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'",
                String.class
        );
        assertThat(tables).contains(
                "SCENE",
                "PACKING_SECTION",
                "PACKING_ITEM",
                "SCENE_SECTION",
                "SECTION_ITEM",
                "SCENE_ITEM_STATE"
        );
    }
}
