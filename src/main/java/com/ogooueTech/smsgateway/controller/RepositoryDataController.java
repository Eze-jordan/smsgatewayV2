package com.ogooueTech.smsgateway.controller;

import com.ogooueTech.smsgateway.model.RepositoryData;
import com.ogooueTech.smsgateway.service.RepositoryDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import java.util.List;

@RestController
@RequestMapping("/api/v1/referentiel")
@Tag(name = "Repository", description = "API for managing repository entries")
public class RepositoryDataController {

    private final RepositoryDataService repositoryDataService;

    public RepositoryDataController(RepositoryDataService repositoryDataService) {
        this.repositoryDataService = repositoryDataService;
    }

    @GetMapping("/all")
    @Operation(summary = "Get all repository data", tags = "Repository")
    public ResponseEntity<List<RepositoryData>> getAll() {
        return repositoryDataService.getAll();
    }

    @GetMapping("/categorie/{refCategory}")
    @Operation(summary = "Get repository data by category", tags = "Repository")
    public ResponseEntity<List<RepositoryData>> getByCategory(@PathVariable String refCategory) {
        return repositoryDataService.getAllByCategory(refCategory);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a repository entry by ID", tags = "Repository")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return repositoryDataService.getById(id);
    }

    @GetMapping("/search")
    @Operation(summary = "Search repository data by keyword", tags = "Repository")
    public ResponseEntity<List<RepositoryData>> search(@RequestParam("q") String keyword) {
        return repositoryDataService.search(keyword);
    }

    @PostMapping
    @Operation(summary = "Create a new repository entry", tags = "Repository")
    public ResponseEntity<?> create(@RequestBody RepositoryData data) {
        return repositoryDataService.create(data);
    }

    @PostMapping("/batch")
    @Operation(summary = "Create multiple repository entries", tags = "Repository")
    public ResponseEntity<?> createBatch(@RequestBody List<RepositoryData> dataList) {
        return repositoryDataService.createBatch(dataList);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing repository entry", tags = "Repository")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody RepositoryData updated) {
        return repositoryDataService.update(id, updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a repository entry", tags = "Repository")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return repositoryDataService.delete(id);
    }

    @GetMapping("/categories")
    @Operation(summary = "Get all distinct refCategory values", tags = "Repository")
    public ResponseEntity<List<String>> getAllRefCategories() {
        return repositoryDataService.getAllDistinctRefCategories();
    }
}
