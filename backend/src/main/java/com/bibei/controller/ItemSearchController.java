package com.bibei.controller;

import com.bibei.dto.ApiModels.ItemSuggestionView;
import com.bibei.service.ItemSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ItemSearchController {
    private final ItemSearchService service;

    public ItemSearchController(ItemSearchService service) {
        this.service = service;
    }

    @GetMapping("/search")
    public List<ItemSuggestionView> search(
            @RequestParam String q,
            @RequestParam(required = false) Long excludeListId
    ) {
        return service.search(q, excludeListId);
    }
}
