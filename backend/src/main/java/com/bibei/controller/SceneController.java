package com.bibei.controller;

import com.bibei.dto.CatalogModels.CheckedRequest;
import com.bibei.dto.CatalogModels.SaveSceneRequest;
import com.bibei.dto.CatalogModels.SceneDetail;
import com.bibei.dto.CatalogModels.SceneSummary;
import com.bibei.service.SceneService;
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
@RequestMapping("/api/scenes")
public class SceneController {
    private final SceneService service;

    public SceneController(SceneService service) {
        this.service = service;
    }

    @GetMapping
    public List<SceneSummary> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public SceneDetail get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SceneDetail create(@Valid @RequestBody SaveSceneRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public SceneDetail update(@PathVariable long id, @Valid @RequestBody SaveSceneRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.delete(id);
    }

    @PutMapping("/{sceneId}/items/{itemId}/checked")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setChecked(
            @PathVariable long sceneId,
            @PathVariable long itemId,
            @RequestBody CheckedRequest request
    ) {
        service.setChecked(sceneId, itemId, request.checked);
    }

    @PostMapping("/{id}/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@PathVariable long id) {
        service.reset(id);
    }
}
