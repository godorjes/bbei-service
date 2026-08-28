package com.bibei.controller;

import com.bibei.dto.ApiModels.SaveTemplateRequest;
import com.bibei.dto.ApiModels.TemplateView;
import com.bibei.service.TemplateService;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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

@Validated
@RestController
@RequestMapping("/api/templates")
public class TemplateController {
    private final TemplateService service;

    public TemplateController(TemplateService service) {
        this.service = service;
    }

    @GetMapping
    public List<TemplateView> list(@RequestParam(defaultValue = "false") boolean archived) {
        return service.list(archived);
    }

    @GetMapping("/{id}")
    public TemplateView get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TemplateView create(@Valid @RequestBody SaveTemplateRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public TemplateView update(@PathVariable long id, @Valid @RequestBody SaveTemplateRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable long id, @RequestParam(defaultValue = "true") boolean archived) {
        service.setArchived(id, archived);
    }
}
