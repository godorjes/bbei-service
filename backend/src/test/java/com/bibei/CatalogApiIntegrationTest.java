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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CatalogApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SceneService sceneService;

    @Autowired
    private SectionService sectionService;

    @Autowired
    private ItemService itemService;

    @Test
    void sceneResourceSupportsCreateReadUpdateAndDelete() throws Exception {
        String response = mockMvc.perform(post("/api/scenes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"骑行\",\"sectionIds\":[]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("骑行"))
                .andReturn().getResponse().getContentAsString();

        long id = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).get("id").asLong();
        mockMvc.perform(put("/api/scenes/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"周末骑行\",\"sectionIds\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("周末骑行"));
        mockMvc.perform(delete("/api/scenes/{id}", id)).andExpect(status().isNoContent());
    }

    @Test
    void checklistCheckedAndResetEndpointsPersistState() throws Exception {
        SaveSectionRequest sectionRequest = new SaveSectionRequest();
        sectionRequest.name = "接口测试分区";
        SectionView section = sectionService.create(sectionRequest);
        SaveItemRequest itemRequest = new SaveItemRequest();
        itemRequest.name = "接口测试物品";
        itemRequest.sectionIds = Collections.singletonList(section.id);
        ItemView item = itemService.create(itemRequest);
        SaveSceneRequest sceneRequest = new SaveSceneRequest();
        sceneRequest.name = "接口测试场景";
        sceneRequest.sectionIds = Collections.singletonList(section.id);
        SceneDetail scene = sceneService.create(sceneRequest);

        mockMvc.perform(put("/api/scenes/{sceneId}/items/{itemId}/checked", scene.id, item.id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"checked\":true}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/scenes/{id}", scene.id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkedCount").value(1));
        mockMvc.perform(post("/api/scenes/{id}/reset", scene.id))
                .andExpect(status().isNoContent());
    }

    @Test
    void sectionAndItemResourcesExposeBindingsAndSearch() throws Exception {
        mockMvc.perform(post("/api/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"露营装备\",\"sceneIds\":[]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("露营装备"));

        mockMvc.perform(get("/api/items").param("query", "充电"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").exists());
    }
}
