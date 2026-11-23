
package com.feat.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.feat.dto.RepositoryImportResponse;
import com.feat.service.RepositoryImportService;

@RestController
@RequestMapping("/api/projects")
public class RepositoryImportController {

    private final RepositoryImportService importService;

    public RepositoryImportController(
            RepositoryImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/{id}/import")
    @ResponseStatus(HttpStatus.OK)
    public RepositoryImportResponse importRepository(
            @PathVariable Long id) {

        return importService.importRepository(id);
    }
}
