package com.bibei;

import com.bibei.dto.ApiModels.CompletePackingListRequest;
import com.bibei.dto.ApiModels.CreatePackingListRequest;
import com.bibei.dto.ApiModels.PackingListView;
import com.bibei.dto.ApiModels.SavePackingListRequest;
import com.bibei.dto.ApiModels.TemplateView;
import com.bibei.model.ChecklistItem;
import com.bibei.service.ItemSearchService;
import com.bibei.service.PackingListService;
import com.bibei.service.TemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class PackingListFlowIntegrationTest {
    @Autowired
    private TemplateService templateService;

    @Autowired
    private PackingListService packingListService;

    @Autowired
    private ItemSearchService itemSearchService;

    @Test
    void temporaryItemOnlyEntersTemplateAfterExplicitPromotion() {
        TemplateView template = templateService.list(false).get(0);
        int originalCount = template.itemCount;

        CreatePackingListRequest createRequest = new CreatePackingListRequest();
        createRequest.templateId = template.id;
        PackingListView list = packingListService.create(createRequest);

        ChecklistItem temporary = new ChecklistItem("测试临时物品");
        temporary.temporary = true;
        list.sections.get(0).items.add(temporary);

        SavePackingListRequest saveRequest = new SavePackingListRequest();
        saveRequest.title = list.title;
        saveRequest.sections = list.sections;
        PackingListView saved = packingListService.save(list.id, saveRequest);

        CompletePackingListRequest completeRequest = new CompletePackingListRequest();
        completeRequest.promoteItemIds = Collections.singletonList(temporary.id);
        packingListService.complete(saved.id, completeRequest);

        TemplateView updated = templateService.get(template.id);
        assertThat(updated.itemCount).isEqualTo(originalCount + 1);
        assertThat(updated.sections)
                .flatExtracting(section -> section.items)
                .extracting(item -> item.name)
                .contains("测试临时物品");
    }

    @Test
    void changeSummaryTracksAddedRemovedAndModifiedItems() {
        TemplateView template = templateService.list(false).get(0);
        CreatePackingListRequest createRequest = new CreatePackingListRequest();
        createRequest.templateId = template.id;
        PackingListView list = packingListService.create(createRequest);

        ChecklistItem modified = list.sections.get(0).items.get(0);
        modified.quantity = modified.quantity + 1;
        ChecklistItem removed = list.sections.get(1).items.remove(0);
        ChecklistItem added = new ChecklistItem("测试雨伞");
        added.temporary = true;
        list.sections.get(0).items.add(added);

        SavePackingListRequest saveRequest = new SavePackingListRequest();
        saveRequest.title = list.title;
        saveRequest.sections = list.sections;
        PackingListView saved = packingListService.save(list.id, saveRequest);

        assertThat(saved.changes.added).extracting(item -> item.name).containsExactly("测试雨伞");
        assertThat(saved.changes.removed).extracting(item -> item.name).contains(removed.name);
        assertThat(saved.changes.modified).extracting(item -> item.name).contains(modified.name);
        assertThat(saved.changes.modified.get(0).description).contains("数量");
    }

    @Test
    void itemSearchOnlyReturnsMatchesAndActiveListCanBeAbandoned() {
        assertThat(itemSearchService.search("充电", null))
                .isNotEmpty()
                .allMatch(item -> item.name.contains("充电"));

        PackingListView list = packingListService.create(new CreatePackingListRequest());
        packingListService.abandon(list.id);

        assertThatThrownBy(() -> packingListService.get(list.id))
                .hasMessageContaining("没有找到");
    }
}
