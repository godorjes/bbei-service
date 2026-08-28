package com.bibei.controller;

import com.bibei.dto.ApiModels.CompletePackingListRequest;
import com.bibei.dto.ApiModels.CreatePackingListRequest;
import com.bibei.dto.ApiModels.PackingListView;
import com.bibei.dto.ApiModels.SavePackingListRequest;
import com.bibei.service.PackingListService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/lists")
public class PackingListController {
    private final PackingListService service;

    public PackingListController(PackingListService service) {
        this.service = service;
    }

    @GetMapping
    public List<PackingListView> activeLists() {
        return service.activeLists();
    }

    @GetMapping("/history")
    public List<PackingListView> history() {
        return service.history();
    }

    @GetMapping("/{id}")
    public PackingListView get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PackingListView create(@RequestBody(required = false) CreatePackingListRequest request) {
        return service.create(request == null ? new CreatePackingListRequest() : request);
    }

    @PutMapping("/{id}")
    public PackingListView save(@PathVariable long id, @Valid @RequestBody SavePackingListRequest request) {
        return service.save(id, request);
    }

    @PostMapping("/{id}/complete")
    public PackingListView complete(
            @PathVariable long id,
            @RequestBody(required = false) CompletePackingListRequest request
    ) {
        return service.complete(id, request == null ? new CompletePackingListRequest() : request);
    }

    @PostMapping("/{id}/reuse")
    @ResponseStatus(HttpStatus.CREATED)
    public PackingListView reuse(@PathVariable long id) {
        return service.reuse(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void abandon(@PathVariable long id) {
        service.abandon(id);
    }
}
