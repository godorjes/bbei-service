package com.bibei.controller;

import com.bibei.dto.CatalogModels.ItemView;
import com.bibei.dto.CatalogModels.SaveItemRequest;
import com.bibei.service.ItemService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ItemController {
    private final ItemService service;

    public ItemController(ItemService service) {
        this.service = service;
    }

    @GetMapping
    public List<ItemView> list(@RequestParam(required = false) String query) {
        return service.list(query);
    }

    @GetMapping("/{id:\\d+}")
    public ItemView get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemView create(@Valid @RequestBody SaveItemRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id:\\d+}")
    public ItemView update(@PathVariable long id, @Valid @RequestBody SaveItemRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id:\\d+}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.delete(id);
    }
}
